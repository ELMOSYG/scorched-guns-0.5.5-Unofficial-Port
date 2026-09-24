package top.ribs.scguns.entity.projectile;


import net.minecraft.core.registries.BuiltInRegistries;
import com.mrcrayfish.framework.api.network.LevelLocation;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.jetbrains.annotations.NotNull;
import top.ribs.scguns.Config;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.init.ModDamageTypes;
import top.ribs.scguns.init.ModTags;
import top.ribs.scguns.interfaces.IDamageable;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.network.PacketHandler;
import top.ribs.scguns.network.message.S2CMessageBlood;
import top.ribs.scguns.network.message.S2CMessageBulletTrail;
import top.ribs.scguns.network.message.S2CMessageProjectileHitBlock;
import top.ribs.scguns.network.message.S2CMessageProjectileHitEntity;
import top.ribs.scguns.util.GunEnchantmentHelper;

public class ShotballProjectileEntity extends ProjectileEntity {
   private static final int MAX_BOUNCES = 3;
   private static final float FIRST_BOUNCE_VELOCITY_BOOST = 1.15F;
   private static final float FIRST_BOUNCE_DAMAGE_BOOST = 1.1F;
   private static final float BOUNCE_VELOCITY_RETENTION = 0.7F;
   private static final float DAMAGE_REDUCTION_PER_BOUNCE = 0.85F;
   private static final float MIN_BOUNCE_VELOCITY = 0.01F;
   private static final int RIDER_IMMUNITY_TICKS = 3;
   private int immunityTicks;
   private static final float BASE_KNOCKBACK = 3.0F;
   private static final float KNOCKBACK_MULTIPLIER_PER_BOUNCE = 0.9F;
   private static final float VERTICAL_KNOCKBACK_BOOST = 0.4F;
   private static final float SPLASH_KNOCKBACK_RADIUS = 5.0F;
   private static final float SPLASH_KNOCKBACK_FALLOFF = 0.5F;
   private static final float WOOD_BREAK_CHANCE_BASE = 0.95F;
   private static final float WOOD_BREAK_CHANCE_PER_BOUNCE = 0.25F;
   private int bouncesLeft;
   private float currentDamageMultiplier = 1.0F;

   public ShotballProjectileEntity(EntityType<? extends Entity> entityType, Level worldIn) {
      super(entityType, worldIn);
      this.bouncesLeft = 3;
   }

   public ShotballProjectileEntity(
      EntityType<? extends Entity> entityType, Level worldIn, LivingEntity shooter, ItemStack weapon, GunItem item, Gun modifiedGun
   ) {
      super(entityType, worldIn, shooter, weapon, item, modifiedGun);
      this.bouncesLeft = 3;
      this.immunityTicks = 3;
   }

   @Override
   public void tick() {
      if (this.immunityTicks > 0) {
         this.immunityTicks--;
      }

      this.updateHeading();
      this.onProjectileTick();
      if (!this.level().isClientSide()) {
         Vec3 startVec = this.position();
         Vec3 endVec = startVec.add(this.getDeltaMovement());
         this.handleCustomCollisions(startVec, endVec);
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

   private float calculateWoodBreakChance() {
      int bouncesUsed = 3 - this.bouncesLeft;
      return 0.95F - (float)bouncesUsed * 0.25F;
   }

   private void tryBreakWoodenBlock(BlockState state, BlockPos pos) {
      if ((Boolean)Config.COMMON.gameplay.griefing.enableGlassBreaking.get()) {
         if (state.is(BlockTags.PLANKS)
            || state.is(BlockTags.WOODEN_DOORS)
            || state.is(BlockTags.WOODEN_TRAPDOORS)
            || state.is(BlockTags.WOODEN_FENCES)) {
            float breakChance = this.calculateWoodBreakChance();
            if (this.random.nextFloat() < breakChance) {
               this.level().destroyBlock(pos, true);
               this.level().playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0F, 0.8F);
            }
         }
      }
   }

   private void applyKnockback(Entity target, Vec3 hitPos) {
      if (target instanceof LivingEntity) {
         Vec3 knockbackDirection = target.position().subtract(this.position()).normalize();
         Vec3 knockbackVec = this.getVec3(knockbackDirection);
         target.push(knockbackVec.x, knockbackVec.y, knockbackVec.z);
         target.hurtMarked = true;
      }
   }

   @NotNull
   private Vec3 getVec3(Vec3 knockbackDirection) {
      float knockbackStrength = 3.0F * this.currentDamageMultiplier;
      int bouncesUsed = 3 - this.bouncesLeft;
      knockbackStrength *= (float)Math.pow(0.9F, (double)bouncesUsed);
      return new Vec3(
         knockbackDirection.x * (double)knockbackStrength,
         Math.max(knockbackDirection.y * (double)knockbackStrength, 0.0) + 0.4F,
         knockbackDirection.z * (double)knockbackStrength
      );
   }

   private void applySplashKnockback(Entity primaryTarget, Vec3 hitPos, float primaryKnockbackStrength) {
      AABB searchBox = new AABB(
         hitPos.x - 5.0, hitPos.y - 5.0, hitPos.z - 5.0, hitPos.x + 5.0, hitPos.y + 5.0, hitPos.z + 5.0
      );

      for (LivingEntity nearbyEntity : this.level()
         .getEntitiesOfClass(LivingEntity.class, searchBox, entity -> entity != primaryTarget && entity != this.shooter && entity.isAlive())) {
         double distance = nearbyEntity.position().distanceTo(hitPos);
         if (distance <= 5.0) {
            float distanceFalloff = (float)(1.0 - distance / 5.0);
            Vec3 splashDirection = nearbyEntity.position().subtract(hitPos).normalize();
            float splashStrength = primaryKnockbackStrength * 0.5F * distanceFalloff;
            Vec3 splashKnockback = new Vec3(
               splashDirection.x * (double)splashStrength,
               Math.max(splashDirection.y * (double)splashStrength, 0.0) + 0.28F,
               splashDirection.z * (double)splashStrength
            );
            nearbyEntity.push(splashKnockback.x, splashKnockback.y, splashKnockback.z);
            nearbyEntity.hurtMarked = true;
            if (distance <= 3.0) {
               float splashDamage = this.getDamage() * 0.15F * distanceFalloff;
               DamageSource source = ModDamageTypes.Sources.projectile(this.level().registryAccess(), this, this.shooter);
               nearbyEntity.hurt(source, splashDamage);
            }
         }
      }
   }

   private boolean isShooterRelatedEntity(Entity entity) {
      if (this.shooter == null) {
         return false;
      } else {
         if (this.immunityTicks > 0) {
            if (this.shooter.isPassenger() && this.shooter.getVehicle() == entity) {
               return true;
            }

            if (entity.isPassenger() && entity.getVehicle() == this.shooter) {
               return true;
            }

            Entity shooterVehicle = this.shooter.getVehicle();
            if (shooterVehicle != null && (shooterVehicle.getVehicle() == entity || entity.getVehicle() == shooterVehicle)) {
               return true;
            }
         }

         return false;
      }
   }

   private void handleCustomCollisions(Vec3 startVec, Vec3 endVec) {
      BlockHitResult blockResult = this.level().clip(new ClipContext(startVec, endVec, Block.COLLIDER, Fluid.NONE, this));
      List<ProjectileEntity.EntityResult> entityResults = this.findShotballEntitiesOnPath(startVec, endVec);
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

      if (blockDistance < entityDistance && blockResult.getType() != Type.MISS) {
         this.handleBlockCollision(blockResult);
      } else if (closestEntity != null) {
         this.handleEntityCollision(closestEntity);
      }
   }

   private List<ProjectileEntity.EntityResult> findShotballEntitiesOnPath(Vec3 startVec, Vec3 endVec) {
      List<ProjectileEntity.EntityResult> hitEntities = new ArrayList<>();

      for (Entity entity : this.level().getEntities(this, this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0), PROJECTILE_TARGETS)) {
         if (!this.isShooterRelatedEntity(entity)) {
            ProjectileEntity.EntityResult result = this.getHitResult(entity, startVec, endVec);
            if (result != null) {
               hitEntities.add(result);
            }
         }
      }

      return hitEntities;
   }

   private void handleBlockCollision(BlockHitResult result) {
      Vec3 hitVec = result.getLocation();
      BlockPos pos = result.getBlockPos();
      BlockState state = this.level().getBlockState(pos);
      Direction face = result.getDirection();
      this.tryBreakWoodenBlock(state, pos);
      if (state.getBlock() instanceof IDamageable damageable) {
         damageable.onBlockDamaged(
            this.level(),
            state,
            pos,
            this,
            this.getDamage() * this.currentDamageMultiplier,
            (int)Math.ceil((double)(this.getDamage() * this.currentDamageMultiplier) / 2.0) + 1
         );
      }

      if (this.bouncesLeft > 0 && this.canBounce() && !state.canBeReplaced()) {
         this.bounce(face, hitVec);
         this.bouncesLeft--;
         if (this.bouncesLeft == 1) {
            this.currentDamageMultiplier *= 1.1F;
         } else {
            this.currentDamageMultiplier *= 0.85F;
         }

         if (this.level() instanceof ServerLevel && this.projectile.isVisible()) {
            this.sendBounceTrailUpdate();
         }

         this.level()
            .playSound(
               null,
               hitVec.x,
               hitVec.y,
               hitVec.z,
               SoundEvents.STONE_HIT,
               SoundSource.NEUTRAL,
               0.8F,
               1.2F + (this.random.nextFloat() - 0.5F) * 0.4F
            );
         this.spawnBounceParticles(hitVec);
      } else {
         PacketHandler.getPlayChannel()
            .sendToTrackingChunk(
               () -> this.level().getChunkAt(pos), new S2CMessageProjectileHitBlock(hitVec.x, hitVec.y, hitVec.z, pos, face)
            );
         this.spawnDeathParticles(this.position());
         this.remove(RemovalReason.KILLED);
      }
   }

   private void handleEntityCollision(ProjectileEntity.EntityResult entityResult) {
      Entity entity = entityResult.getEntity();
      Vec3 hitVec = entityResult.getHitPos();
      boolean headshot = entityResult.isHeadshot();
      float damage = this.getDamage() * this.currentDamageMultiplier;
      float newDamage = this.getCriticalDamage(this.getWeapon(), this.random, damage);
      boolean critical = damage != newDamage;
      damage = newDamage * this.advantageMultiplier(entity);
      if (headshot) {
         damage = (float)((double)damage * (Double)Config.COMMON.gameplay.headShotDamageMultiplier.get());
      }

      if (entity instanceof LivingEntity livingTarget) {
         damage = this.applyProjectileProtection(livingTarget, damage);
         damage = this.calculateArmorBypassDamage(livingTarget, damage);
      }

      DamageSource source = ModDamageTypes.Sources.projectile(this.level().registryAccess(), this, this.shooter);
      boolean blocked = ProjectileEntity.ProjectileHelper.handleShieldHit(entity, this, damage);
      if (!blocked && (!entity.getType().is(ModTags.Entities.GHOST) || this.getProjectile().getAdvantage().equals(ModTags.Entities.UNDEAD.location()))) {
         if (damage > 0.0F) {
            entity.hurt(source, damage);
            float knockbackStrength = 3.0F * this.currentDamageMultiplier;
            int bouncesUsed = 3 - this.bouncesLeft;
            knockbackStrength *= (float)Math.pow(0.9F, (double)bouncesUsed);
            this.applyKnockback(entity, hitVec);
            this.applySplashKnockback(entity, hitVec, knockbackStrength);
         }

         if (entity instanceof LivingEntity livingEntity) {
            ResourceLocation effectLocation = this.projectile.getImpactEffect();
            if (effectLocation != null) {
               float effectChance = this.projectile.getImpactEffectChance();
               if (this.random.nextFloat() < effectChance) {
                  MobEffect effect = (MobEffect)BuiltInRegistries.MOB_EFFECT.get(effectLocation);
                  if (effect != null) {
                     livingEntity.addEffect(new MobEffectInstance(top.ribs.scguns.util.ScEffects.holder(effect), this.projectile.getImpactEffectDuration(), this.projectile.getImpactEffectAmplifier()));
                  }
               }
            }
         }
      }

      if (entity instanceof LivingEntity livingEntityx) {
         GunEnchantmentHelper.applyElementalPopEffect(this.getWeapon(), livingEntityx);
      }

      if (this.shooter instanceof Player) {
         int hitType = critical ? 2 : (headshot ? 1 : 0);
         PacketHandler.getPlayChannel()
            .sendToPlayer(
               () -> (ServerPlayer)this.shooter,
               new S2CMessageProjectileHitEntity(hitVec.x, hitVec.y, hitVec.z, hitType, entity instanceof Player)
            );
      }

      PacketHandler.getPlayChannel().sendToTrackingEntity(() -> entity, new S2CMessageBlood(hitVec.x, hitVec.y, hitVec.z, entity.getType()));
      if (this.bouncesLeft > 0 && this.canBounce()) {
         this.bounceOffEntity(entity, hitVec);
         this.bouncesLeft--;
         if (this.bouncesLeft == 1) {
            this.currentDamageMultiplier *= 1.1F;
         } else {
            this.currentDamageMultiplier *= 0.85F;
         }

         this.level()
            .playSound(
               null,
               hitVec.x,
               hitVec.y,
               hitVec.z,
               SoundEvents.SLIME_BLOCK_HIT,
               SoundSource.NEUTRAL,
               0.6F,
               1.0F + (this.random.nextFloat() - 0.5F) * 0.4F
            );
         this.spawnBounceParticles(hitVec);
      } else {
         this.spawnDeathParticles(this.position());
         this.remove(RemovalReason.KILLED);
      }

      entity.invulnerableTime = 0;
   }

   private void bounce(Direction face, Vec3 hitPos) {
      Vec3 velocity = this.getDeltaMovement();

      Vec3 newVelocity = switch (face.getAxis()) {
         case X -> new Vec3(-velocity.x, velocity.y, velocity.z);
         case Y -> new Vec3(velocity.x, -velocity.y, velocity.z);
         case Z -> new Vec3(velocity.x, velocity.y, -velocity.z);
         default -> throw new IncompatibleClassChangeError();
      };
      newVelocity = newVelocity.scale(0.7F);
      if (this.bouncesLeft == 2) {
         newVelocity = newVelocity.scale(1.15F);
      }

      newVelocity = newVelocity.add(
         (this.random.nextDouble() - 0.5) * 0.05, (this.random.nextDouble() - 0.5) * 0.05, (this.random.nextDouble() - 0.5) * 0.05
      );
      this.setDeltaMovement(newVelocity);
      Vec3 offset = Vec3.atLowerCornerOf(face.getNormal()).scale(0.2);
      this.setPos(hitPos.add(offset));
   }

   private void bounceOffEntity(Entity entity, Vec3 hitPos) {
      Vec3 velocity = this.getDeltaMovement();
      Vec3 entityCenter = entity.getBoundingBox().getCenter();
      Vec3 bounceDirection = this.position().subtract(entityCenter).normalize();
      Vec3 newVelocity = bounceDirection.scale(velocity.length() * 0.7F);
      if (this.bouncesLeft == 2) {
         newVelocity = newVelocity.scale(1.15F);
      }

      newVelocity = newVelocity.add(
         (this.random.nextDouble() - 0.5) * 0.1, (this.random.nextDouble() - 0.5) * 0.1, (this.random.nextDouble() - 0.5) * 0.1
      );
      this.setDeltaMovement(newVelocity);
      this.setPos(hitPos.add(bounceDirection.scale(0.3)));
   }

   private boolean canBounce() {
      double currentSpeed = this.getDeltaMovement().length();
      return currentSpeed > 0.01F;
   }

   private void spawnBounceParticles(Vec3 position) {
      if (!this.level().isClientSide) {
         ServerLevel serverLevel = (ServerLevel)this.level();

         for (int i = 0; i < 8; i++) {
            double velocityX = (this.random.nextDouble() - 0.5) * 0.5;
            double velocityY = this.random.nextDouble() * 0.5;
            double velocityZ = (this.random.nextDouble() - 0.5) * 0.5;
            serverLevel.sendParticles(ParticleTypes.CRIT, position.x, position.y, position.z, 1, velocityX, velocityY, velocityZ, 0.1);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, position.x, position.y, position.z, 1, velocityX, velocityY, velocityZ, 0.05);
         }
      }
   }

   private void spawnDeathParticles(Vec3 position) {
      if (!this.level().isClientSide) {
         ServerLevel serverLevel = (ServerLevel)this.level();

         for (int i = 0; i < 5; i++) {
            double velocityX = (this.random.nextDouble() - 0.5) * 0.3;
            double velocityY = this.random.nextDouble() * 0.4 + 0.1;
            double velocityZ = (this.random.nextDouble() - 0.5) * 0.3;
            serverLevel.sendParticles(
               ParticleTypes.LARGE_SMOKE,
               position.x + (this.random.nextDouble() - 0.5) * 0.2,
               position.y + (this.random.nextDouble() - 0.5) * 0.15,
               position.z + (this.random.nextDouble() - 0.5) * 0.2,
               1,
               velocityX,
               velocityY,
               velocityZ,
               0.02
            );
         }

         for (int i = 0; i < 3; i++) {
            double velocityX = (this.random.nextDouble() - 0.5) * 0.2;
            double velocityY = this.random.nextDouble() * 0.3 + 0.05;
            double velocityZ = (this.random.nextDouble() - 0.5) * 0.2;
            serverLevel.sendParticles(
               ParticleTypes.SMOKE,
               position.x + (this.random.nextDouble() - 0.5) * 0.3,
               position.y + (this.random.nextDouble() - 0.5) * 0.2,
               position.z + (this.random.nextDouble() - 0.5) * 0.3,
               1,
               velocityX,
               velocityY,
               velocityZ,
               0.01
            );
         }

         this.level()
            .playSound(
               null,
               position.x,
               position.y,
               position.z,
               SoundEvents.FIRE_EXTINGUISH,
               SoundSource.NEUTRAL,
               0.2F,
               2.0F + (this.random.nextFloat() - 0.5F) * 0.3F
            );
      }
   }

   private void sendBounceTrailUpdate() {
      if (this.shooter instanceof ServerPlayer player) {
         Gun.Projectile projectileProps = this.getProjectile();
         ProjectileEntity[] bounceArray = new ProjectileEntity[]{this};
         ParticleOptions data = GunEnchantmentHelper.getParticle(player.getMainHandItem());
         S2CMessageBulletTrail messageBulletTrail = new S2CMessageBulletTrail(bounceArray, projectileProps, player.getId(), data, true);
         double radius = (Double)Config.COMMON.network.projectileTrackingRange.get();
         PacketHandler.getPlayChannel()
            .sendToNearbyPlayers(() -> LevelLocation.create((ServerLevel) this.level(), this.getX(), this.getY(), this.getZ(), radius), messageBulletTrail);
      }
   }

   @Override
   protected void onExpired() {
      this.spawnDeathParticles(this.position());
   }

   @Override
   public float getDamage() {
      return super.getDamage() * this.currentDamageMultiplier;
   }

   @Override
   protected void onHitBlock(BlockState state, BlockPos pos, Direction face, double x, double y, double z) {
   }

   @Override
   protected void onHitEntity(Entity entity, Vec3 hitVec, Vec3 startVec, Vec3 endVec, boolean headshot) {
   }

   @Override
   public void onHit(HitResult result, Vec3 startVec, Vec3 endVec) {
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag compound) {
      super.addAdditionalSaveData(compound);
      compound.putInt("BouncesLeft", this.bouncesLeft);
      compound.putFloat("CurrentDamageMultiplier", this.currentDamageMultiplier);
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag compound) {
      super.readAdditionalSaveData(compound);
      this.bouncesLeft = compound.getInt("BouncesLeft");
      this.currentDamageMultiplier = compound.getFloat("CurrentDamageMultiplier");
   }

   @Override
   public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
      super.writeSpawnData(buffer);
      buffer.writeInt(this.bouncesLeft);
      buffer.writeFloat(this.currentDamageMultiplier);
   }

   @Override
   public void readSpawnData(RegistryFriendlyByteBuf buffer) {
      super.readSpawnData(buffer);
      this.bouncesLeft = buffer.readInt();
      this.currentDamageMultiplier = buffer.readFloat();
   }
}
