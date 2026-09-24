package top.ribs.scguns.entity.projectile;


import net.minecraft.core.registries.BuiltInRegistries;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
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
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.init.ModDamageTypes;
import top.ribs.scguns.init.ModTags;
import top.ribs.scguns.interfaces.IDamageable;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.network.PacketHandler;
import top.ribs.scguns.network.message.S2CMessageBlood;
import top.ribs.scguns.network.message.S2CMessageProjectileHitBlock;
import top.ribs.scguns.particles.TrailData;

public class ShatterRoundProjectileEntity extends ProjectileEntity {
   private static final int SHRAPNEL_COUNT = 20;
   private static final float SHRAPNEL_RANGE = 5.0F;
   private static final float SHRAPNEL_DAMAGE_MULTIPLIER = 0.3F;
   private boolean hasDetonated = false;

   public ShatterRoundProjectileEntity(EntityType<? extends Entity> entityType, Level worldIn) {
      super(entityType, worldIn);
   }

   public ShatterRoundProjectileEntity(
      EntityType<? extends Entity> entityType, Level worldIn, LivingEntity shooter, ItemStack weapon, GunItem item, Gun modifiedGun
   ) {
      super(entityType, worldIn, shooter, weapon, item, modifiedGun);
   }

   @Override
   protected void onHitEntity(Entity entity, Vec3 hitVec, Vec3 startVec, Vec3 endVec, boolean headshot) {
      if (!this.hasDetonated) {
         super.onHitEntity(entity, hitVec, startVec, endVec, headshot);
         this.explode(hitVec);
      }
   }

   @Override
   protected void onHitBlock(BlockState state, BlockPos pos, Direction face, double x, double y, double z) {
      if (!this.hasDetonated) {
         PacketHandler.getPlayChannel().sendToTrackingChunk(() -> this.level().getChunkAt(pos), new S2CMessageProjectileHitBlock(x, y, z, pos, face));
         Block block = state.getBlock();
         this.primeTNT(state, pos);
         if (block instanceof DoorBlock) {
            boolean isOpen = (Boolean)state.getValue(DoorBlock.OPEN);
            if (!isOpen) {
               this.level().setBlock(pos, (BlockState)state.setValue(DoorBlock.OPEN, true), 10);
               this.level().playSound(null, pos, SoundEvents.WOODEN_DOOR_OPEN, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
         }

         if (block instanceof IDamageable) {
            ((IDamageable)block).onBlockDamaged(this.level(), state, pos, this, this.getDamage(), (int)Math.ceil((double)this.getDamage() / 2.0) + 1);
         }

         this.explode(new Vec3(x, y, z));
      }
   }

   @Override
   protected void onExpired() {
      if (!this.hasDetonated) {
         this.explode(this.position());
      }
   }

   private void explode(Vec3 explosionPos) {
      if (!this.hasDetonated && !this.level().isClientSide()) {
         this.hasDetonated = true;
         this.createCentralExplosion(explosionPos);
         this.createExplosionEffects(explosionPos);
         this.fireShrapnel(explosionPos);
         this.remove(RemovalReason.KILLED);
      }
   }

   private void createCentralExplosion(Vec3 pos) {
      if (!this.level().isClientSide()) {
         ServerLevel serverLevel = (ServerLevel)this.level();
         this.level()
            .playSound(null, pos.x, pos.y, pos.z, SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 2.0F, 0.8F + this.random.nextFloat() * 0.4F);
         this.level()
            .playSound(null, pos.x, pos.y, pos.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.NEUTRAL, 1.5F, 1.2F + this.random.nextFloat() * 0.3F);

         for (int i = 0; i < 40; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double pitch = (this.random.nextDouble() - 0.5) * Math.PI * 0.5;
            double speed = 0.3 + this.random.nextDouble() * 0.4;
            double offsetX = Math.cos(angle) * Math.cos(pitch) * speed;
            double offsetY = Math.sin(pitch) * speed;
            double offsetZ = Math.sin(angle) * Math.cos(pitch) * speed;
            serverLevel.sendParticles(ParticleTypes.CRIT, pos.x, pos.y, pos.z, 1, offsetX, offsetY, offsetZ, 0.02);
         }

         serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, pos.x, pos.y, pos.z, 1, 0.1, 0.1, 0.1, 0.0);
      }
   }

   private void createExplosionEffects(Vec3 pos) {
      this.level()
         .playSound(null, pos.x, pos.y, pos.z, SoundEvents.CALCITE_BREAK, SoundSource.NEUTRAL, 1.0F, 1.0F + this.random.nextFloat() * 0.4F);
      ServerLevel serverLevel = (ServerLevel)this.level();
   }

   private void fireShrapnel(Vec3 origin) {
      float shrapnelDamage = this.getDamage() * 0.3F;

      for (int i = 0; i < 20; i++) {
         Vec3 direction = this.generateRandomDirection();
         Vec3 endPos = origin.add(direction.scale(5.0));
         this.traceShrapnelRay(origin, endPos, shrapnelDamage, i);
      }
   }

   private Vec3 generateRandomDirection() {
      float x;
      float y;
      float z;
      float lengthSquared;
      do {
         x = this.random.nextFloat() * 2.0F - 1.0F;
         y = this.random.nextFloat() * 2.0F - 1.0F;
         z = this.random.nextFloat() * 2.0F - 1.0F;
         lengthSquared = x * x + y * y + z * z;
      } while (lengthSquared > 1.0F || lengthSquared < 0.001F);

      float length = Mth.sqrt(lengthSquared);
      return new Vec3((double)(x / length), (double)(y / length), (double)(z / length));
   }

   private void traceShrapnelRay(Vec3 start, Vec3 end, float damage, int rayIndex) {
      List<Entity> hitEntities = this.findEntitiesAlongRay(start, end);
      Vec3 traceEnd = end;
      boolean hitSomething = false;
      Entity closestEntity = null;
      double closestDistance = Double.MAX_VALUE;
      Vec3 closestHitPos = null;

      for (Entity entity : hitEntities) {
         if (this.isValidShrapnelTarget(entity)) {
            Vec3 hitPos = this.getEntityHitPosition(entity, start, end);
            if (hitPos != null) {
               double distance = start.distanceToSqr(hitPos);
               if (distance < closestDistance) {
                  closestDistance = distance;
                  closestEntity = entity;
                  closestHitPos = hitPos;
               }
            }
         }
      }

      if (closestEntity != null) {
         this.damageEntityWithShrapnel(closestEntity, closestHitPos, damage);
         traceEnd = closestHitPos;
         hitSomething = true;
      }

      if (!hitSomething) {
         BlockHitResult blockHit = this.level().clip(new ClipContext(start, end, net.minecraft.world.level.ClipContext.Block.COLLIDER, Fluid.NONE, this));
         if (blockHit.getType() != Type.MISS) {
            traceEnd = blockHit.getLocation();
         }
      }

      this.createShrapnelTracer(start, traceEnd);
   }

   private List<Entity> findEntitiesAlongRay(Vec3 start, Vec3 end) {
      AABB searchBox = new AABB(start, end).inflate(1.0);
      return this.level()
         .getEntities(
            this, searchBox, entity -> entity != null && entity.isPickable() && !entity.isSpectator() && entity != this.shooter && entity.getId() != this.shooterId
         );
   }

   private boolean isValidShrapnelTarget(Entity entity) {
      if (entity == this.shooter || entity.getId() == this.shooterId) {
         return false;
      } else if (this.shooter instanceof Player && entity instanceof Player && this.shooter.getUUID().equals(entity.getUUID())) {
         return false;
      } else if (this.shooter != null) {
         double distance = entity.position().distanceTo(this.shooter.position());
         return !(distance < 1.0);
      } else {
         return true;
      }
   }

   private Vec3 getEntityHitPosition(Entity entity, Vec3 start, Vec3 end) {
      AABB boundingBox = entity.getBoundingBox();
      return (Vec3)boundingBox.clip(start, end).orElse(null);
   }

   private void damageEntityWithShrapnel(Entity entity, Vec3 hitPos, float damage) {
      ResourceLocation advantage = this.getProjectile().getAdvantage();
      damage *= this.advantageMultiplier(entity);
      if (entity instanceof LivingEntity livingTarget) {
         damage = this.applyProjectileProtection(livingTarget, damage);
         damage = this.calculateArmorBypassDamage(livingTarget, damage);
      }

      DamageSource source = ModDamageTypes.Sources.projectile(this.level().registryAccess(), this, this.shooter);
      if (!entity.getType().is(ModTags.Entities.GHOST) || advantage.equals(ModTags.Entities.UNDEAD.location())) {
         entity.hurt(source, damage);
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

      PacketHandler.getPlayChannel().sendToTrackingEntity(() -> entity, new S2CMessageBlood(hitPos.x, hitPos.y, hitPos.z, entity.getType()));
      entity.invulnerableTime = Math.min(entity.invulnerableTime, 3);
   }

   private void createShrapnelTracer(Vec3 start, Vec3 end) {
      ServerLevel serverLevel = (ServerLevel)this.level();
      Vec3 direction = end.subtract(start);
      double distance = direction.length();
      if (!(distance < 0.1)) {
         direction = direction.normalize();
         boolean isEnchanted = this.getWeapon() != null && this.getWeapon().isEnchanted();
         TrailData trailData = new TrailData(isEnchanted);
         int maxSegments = Math.min(12, (int)(distance * 1.5));

         for (int i = 1; i <= maxSegments; i++) {
            double progress = (double)i / (double)maxSegments;
            Vec3 particlePos = start.add(direction.scale(distance * progress));
            double densityFactor = Math.max(0.2, 1.0 - progress * 0.8);
            int particlesAtThisPoint = Math.max(1, (int)(4.0 * densityFactor));
            double spreadRadius = 0.02 + progress * 0.1;

            for (int j = 0; j < particlesAtThisPoint; j++) {
               double offsetX = (this.random.nextDouble() - 0.5) * spreadRadius;
               double offsetY = (this.random.nextDouble() - 0.5) * spreadRadius;
               double offsetZ = (this.random.nextDouble() - 0.5) * spreadRadius;
               serverLevel.sendParticles(
                  trailData, particlePos.x + offsetX, particlePos.y + offsetY, particlePos.z + offsetZ, 1, 0.0, 0.0, 0.0, 0.0
               );
            }
         }
      }
   }

   @Override
   protected void onProjectileTick() {
      if (this.level().isClientSide && !this.hasDetonated && this.tickCount % 3 == 0) {
         double offsetX = (this.random.nextDouble() - 0.5) * 0.1;
         double offsetY = (this.random.nextDouble() - 0.5) * 0.1;
         double offsetZ = (this.random.nextDouble() - 0.5) * 0.1;
         this.level().addParticle(ParticleTypes.SMOKE, this.getX() + offsetX, this.getY() + offsetY, this.getZ() + offsetZ, 0.0, 0.0, 0.0);
      }
   }
}
