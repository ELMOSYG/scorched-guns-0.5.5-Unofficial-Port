package top.ribs.scguns.entity.projectile;


import top.ribs.scguns.util.ScEnchants;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.init.ModDamageTypes;
import top.ribs.scguns.init.ModParticleTypes;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.util.ExplosionHelper;

public class BouncyGrenadeRoundEntity extends ProjectileEntity {
   private static final float EXPLOSION_RADIUS = 2.0F;
   private static final int MAX_BOUNCES = 3;
   private static final float BOUNCE_VELOCITY_RETENTION = 0.75F;
   private static final float MIN_BOUNCE_VELOCITY = 0.05F;
   private int bouncesLeft = 3;

   public BouncyGrenadeRoundEntity(EntityType<? extends ProjectileEntity> entityType, Level worldIn) {
      super(entityType, worldIn);
   }

   public BouncyGrenadeRoundEntity(
      EntityType<? extends ProjectileEntity> entityType, Level worldIn, LivingEntity shooter, ItemStack weapon, GunItem item, Gun modifiedGun
   ) {
      super(entityType, worldIn, shooter, weapon, item, modifiedGun);
   }

   @Override
   public void tick() {
      this.updateHeading();
      this.onProjectileTick();
      if (!this.level().isClientSide()) {
         Vec3 startVec = this.position();
         Vec3 endVec = startVec.add(this.getDeltaMovement());
         BlockHitResult blockResult = this.level().clip(new ClipContext(startVec, endVec, Block.COLLIDER, Fluid.NONE, this));
         List<ProjectileEntity.EntityResult> entityResults = this.findEntitiesOnPath(startVec, endVec);
         double blockDistance = Double.MAX_VALUE;
         double entityDistance = Double.MAX_VALUE;
         if (blockResult.getType() != Type.MISS) {
            blockDistance = startVec.distanceToSqr(blockResult.getLocation());
         }

         ProjectileEntity.EntityResult closestEntity = null;
         if (!entityResults.isEmpty()) {
            for (ProjectileEntity.EntityResult entityResult : entityResults) {
               double dist = startVec.distanceToSqr(entityResult.getHitPos());
               if (dist < entityDistance) {
                  entityDistance = dist;
                  closestEntity = entityResult;
               }
            }
         }

         if (closestEntity != null && entityDistance < blockDistance) {
            this.handleEntityImpact(closestEntity.getEntity(), closestEntity.getHitPos());
         } else if (blockResult.getType() != Type.MISS) {
            this.handleBlockImpact(blockResult);
         }
      }

      double nextPosX = this.getX() + this.getDeltaMovement().x();
      double nextPosY = this.getY() + this.getDeltaMovement().y();
      double nextPosZ = this.getZ() + this.getDeltaMovement().z();
      this.setPos(nextPosX, nextPosY, nextPosZ);
      if (this.projectile.isGravity()) {
         this.setDeltaMovement(this.getDeltaMovement().add(0.0, this.modifiedGravity, 0.0));
      }

      if (this.tickCount >= this.life) {
         if (this.isAlive()) {
            this.onExpired();
         }

         this.remove(RemovalReason.KILLED);
      }
   }

   private void handleEntityImpact(Entity entity, Vec3 hitVec) {
      if (entity instanceof Player player) {
         ItemStack mainHandItem = player.getMainHandItem();
         ItemStack offHandItem = player.getOffhandItem();
         boolean isBlockingMainHand = player.isBlocking() && mainHandItem.getItem() instanceof ShieldItem;
         boolean isBlockingOffHand = player.isBlocking() && offHandItem.getItem() instanceof ShieldItem;
         if (isBlockingMainHand || isBlockingOffHand) {
            ItemStack shield = isBlockingMainHand ? mainHandItem : offHandItem;
            InteractionHand hand = isBlockingMainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            player.getCooldowns().addCooldown(shield.getItem(), 100);
            player.stopUsingItem();
            player.level().broadcastEntityEvent(player, (byte)30);
            player.level()
               .playSound(
                  null,
                  player.getX(),
                  player.getY(),
                  player.getZ(),
                  SoundEvents.SHIELD_BREAK,
                  SoundSource.PLAYERS,
                  1.0F,
                  0.8F + player.level().getRandom().nextFloat() * 0.4F
               );
            shield.hurtAndBreak(15, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(player.getUsedItemHand()));
         }
      }

      this.createBouncyGrenadeExplosion(this, 2.0F);
   }

   private void handleBlockImpact(BlockHitResult result) {
      Vec3 hitVec = result.getLocation();
      BlockPos pos = result.getBlockPos();
      BlockState state = this.level().getBlockState(pos);
      Direction face = result.getDirection();
      if (this.bouncesLeft > 0 && this.canBounce() && !state.canBeReplaced()) {
         this.bounce(face, hitVec);
         this.bouncesLeft--;
         this.level()
            .playSound(
               null,
               hitVec.x,
               hitVec.y,
               hitVec.z,
               SoundEvents.SLIME_BLOCK_HIT,
               SoundSource.NEUTRAL,
               0.8F,
               1.0F + (this.random.nextFloat() - 0.5F) * 0.4F
            );
         this.spawnBounceParticles(hitVec);
      } else {
         this.createBouncyGrenadeExplosion(this, 2.0F);
         this.remove(RemovalReason.KILLED);
      }
   }

   private void bounce(Direction face, Vec3 hitPos) {
      Vec3 velocity = this.getDeltaMovement();

      Vec3 newVelocity = switch (face.getAxis()) {
         case X -> new Vec3(-velocity.x, velocity.y, velocity.z);
         case Y -> new Vec3(velocity.x, -velocity.y, velocity.z);
         case Z -> new Vec3(velocity.x, velocity.y, -velocity.z);
         default -> throw new IncompatibleClassChangeError();
      };
      newVelocity = newVelocity.scale(0.75);
      newVelocity = newVelocity.add(
         (this.random.nextDouble() - 0.5) * 0.05, (this.random.nextDouble() - 0.5) * 0.05, (this.random.nextDouble() - 0.5) * 0.05
      );
      this.setDeltaMovement(newVelocity);
      Vec3 offset = Vec3.atLowerCornerOf(face.getNormal()).scale(0.2);
      this.setPos(hitPos.add(offset));
   }

   private boolean canBounce() {
      double currentSpeed = this.getDeltaMovement().length();
      return currentSpeed > 0.05F;
   }

   private void spawnBounceParticles(Vec3 position) {
      if (!this.level().isClientSide) {
         ServerLevel serverLevel = (ServerLevel)this.level();

         for (int i = 0; i < 6; i++) {
            double velocityX = (this.random.nextDouble() - 0.5) * 0.4;
            double velocityY = this.random.nextDouble() * 0.4;
            double velocityZ = (this.random.nextDouble() - 0.5) * 0.4;
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, position.x, position.y, position.z, 1, velocityX, velocityY, velocityZ, 0.05);
         }
      }
   }

   @Override
   protected void onHitEntity(Entity entity, Vec3 hitVec, Vec3 startVec, Vec3 endVec, boolean headshot) {
   }

   @Override
   protected void onHitBlock(BlockState state, BlockPos pos, Direction face, double x, double y, double z) {
   }

   @Override
   public void onExpired() {
      this.createBouncyGrenadeExplosion(this, 2.0F);
   }

   private void createBouncyGrenadeExplosion(Entity entity, float radius) {
      Level world = entity.level();
      if (!world.isClientSide()) {
         Vec3 explosionPos = new Vec3(entity.getX(), entity.getY(), entity.getZ());
         this.playExplosionSound(world, explosionPos, radius);
         this.spawnExplosionParticles(world, explosionPos, radius);
         this.applyExplosionDamage(world, explosionPos, radius, this.getDamage(), entity);
      }
   }

   private void playExplosionSound(Level world, Vec3 pos, float radius) {
      float volume = Math.min(3.0F, radius * 0.5F);
      float pitch = 0.9F + world.random.nextFloat() * 0.3F;
      world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, volume, pitch);
   }

   private void spawnExplosionParticles(Level world, Vec3 explosionPos, float radius) {
      ServerLevel serverLevel = (ServerLevel)world;
      double sizeMultiplier = (double)radius / 3.5;
      BlockPos blockPos = BlockPos.containing(explosionPos.x, explosionPos.y, explosionPos.z);
      BlockState blockAtExplosion = world.getBlockState(blockPos);
      double adjustedY = blockAtExplosion.isAir() ? explosionPos.y + 0.2 : (double)blockPos.getY() + 1.0;
      double renderDistance = 128.0;
      List<ServerPlayer> nearbyPlayers = serverLevel.getEntitiesOfClass(
         ServerPlayer.class,
         new AABB(
            explosionPos.x - renderDistance,
            explosionPos.y - renderDistance,
            explosionPos.z - renderDistance,
            explosionPos.x + renderDistance,
            explosionPos.y + renderDistance,
            explosionPos.z + renderDistance
         )
      );

      for (ServerPlayer player : nearbyPlayers) {
         serverLevel.sendParticles(
            player,
            (SimpleParticleType)ModParticleTypes.GRENADE_EXPLOSION.get(),
            true,
            explosionPos.x,
            adjustedY,
            explosionPos.z,
            1,
            sizeMultiplier,
            0.0,
            0.0,
            0.0
         );
      }

      for (int burstWave = 0; burstWave < 2; burstWave++) {
         int particlesInBurst = 8 + burstWave * 4;
         double burstRadius = (double)radius * (0.3 + (double)burstWave * 0.15);

         for (int i = 0; i < particlesInBurst; i++) {
            double angle = (double)i / (double)particlesInBurst * 2.0 * Math.PI;
            double distance = burstRadius * (0.5 + world.random.nextDouble() * 0.5);
            double burstX = Math.cos(angle) * distance;
            double burstZ = Math.sin(angle) * distance;
            double burstY = (world.random.nextDouble() - 0.3) * (double)radius * 0.15;
            double speedX = Math.cos(angle) * (0.2 + world.random.nextDouble() * 0.3);
            double speedY = 0.15 + world.random.nextDouble() * 0.25;
            double speedZ = Math.sin(angle) * (0.2 + world.random.nextDouble() * 0.3);

            for (ServerPlayer player : nearbyPlayers) {
               serverLevel.sendParticles(
                  player,
                  ParticleTypes.FLAME,
                  true,
                  explosionPos.x + burstX,
                  adjustedY + burstY,
                  explosionPos.z + burstZ,
                  1,
                  speedX,
                  speedY,
                  speedZ,
                  0.08
               );
            }
         }
      }

      for (int scatter = 0; scatter < 12; scatter++) {
         double scatterRadius = (double)radius * 1.0;
         double scatterAngle = world.random.nextDouble() * 2.0 * Math.PI;
         double scatterDistance = world.random.nextDouble() * scatterRadius;
         double scatterX = Math.cos(scatterAngle) * scatterDistance;
         double scatterZ = Math.sin(scatterAngle) * scatterDistance;
         double scatterY = (world.random.nextDouble() - 0.5) * (double)radius * 0.25;
         double scatterSpeedX = (world.random.nextDouble() - 0.5) * 0.5;
         double scatterSpeedY = world.random.nextDouble() * 0.35;
         double scatterSpeedZ = (world.random.nextDouble() - 0.5) * 0.5;
         serverLevel.sendParticles(
            ParticleTypes.LARGE_SMOKE,
            explosionPos.x + scatterX,
            adjustedY + scatterY,
            explosionPos.z + scatterZ,
            1,
            scatterSpeedX,
            scatterSpeedY,
            scatterSpeedZ,
            0.04
         );
      }
   }

   private void applyExplosionDamage(Level world, Vec3 explosionPos, float radius, float baseDamage, Entity sourceEntity) {
      float damageRadius = radius * 1.5F;
      AABB searchBox = new AABB(
         explosionPos.x - (double)damageRadius - 1.0,
         explosionPos.y - (double)damageRadius - 1.0,
         explosionPos.z - (double)damageRadius - 1.0,
         explosionPos.x + (double)damageRadius + 1.0,
         explosionPos.y + (double)damageRadius + 1.0,
         explosionPos.z + (double)damageRadius + 1.0
      );
      List<Entity> entities = world.getEntitiesOfClass(Entity.class, searchBox);
      DamageSource damageSource = ModDamageTypes.Sources.projectile(world.registryAccess(), sourceEntity, this.getShooter());

      for (Entity entity : entities) {
         if (!ExplosionHelper.ignoresExplosion(entity)) {
            double distance = Math.sqrt(entity.distanceToSqr(explosionPos));
            if (!(distance >= (double)damageRadius)) {
               float damageMultiplier = 1.0F - (float)(distance / (double)damageRadius);
               float damage = baseDamage * damageMultiplier;
               if (entity instanceof LivingEntity livingEntity) {
                  damage = this.applyBlastProtection(livingEntity, damage);
               }

               if (damage > 0.0F) {
                  entity.hurt(damageSource, damage);
                  double deltaX = entity.getX() - explosionPos.x;
                  double deltaY = entity.getEyeY() - explosionPos.y;
                  double deltaZ = entity.getZ() - explosionPos.z;
                  double distanceToExplosion = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
                  if (distanceToExplosion != 0.0) {
                     deltaX /= distanceToExplosion;
                     deltaY /= distanceToExplosion;
                     deltaZ /= distanceToExplosion;
                  } else {
                     deltaX = 0.0;
                     deltaY = 1.0;
                     deltaZ = 0.0;
                  }

                  double knockbackStrength = Math.max(0.0, (1.0 - distance / (double)damageRadius) * 0.5);
                  entity.setDeltaMovement(entity.getDeltaMovement().add(deltaX * knockbackStrength, deltaY * knockbackStrength, deltaZ * knockbackStrength));
               }
            }
         }
      }
   }

   private float applyBlastProtection(LivingEntity target, float damage) {
      int protectionLevel = ScEnchants.level(target, Enchantments.BLAST_PROTECTION);
      if (protectionLevel > 0) {
         float reduction = (float)protectionLevel * 0.08F;
         reduction = Math.min(reduction, 0.8F);
         damage *= 1.0F - reduction;
      }

      return damage;
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag compound) {
      super.addAdditionalSaveData(compound);
      compound.putInt("BouncesLeft", this.bouncesLeft);
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag compound) {
      super.readAdditionalSaveData(compound);
      this.bouncesLeft = compound.getInt("BouncesLeft");
   }

   @Override
   public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
      super.writeSpawnData(buffer);
      buffer.writeInt(this.bouncesLeft);
   }

   @Override
   public void readSpawnData(RegistryFriendlyByteBuf buffer) {
      super.readSpawnData(buffer);
      this.bouncesLeft = buffer.readInt();
   }
}
