package top.ribs.scguns.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.ribs.scguns.entity.projectile.ProjectileEntity;

public class PlasmaExplosion extends CustomExplosion {
   private static final float OWNER_DAMAGE_REDUCTION = 0.1F;
   private final RandomSource random = RandomSource.create();
   private final Entity source;

   public PlasmaExplosion(
      Level pLevel,
      @Nullable Entity pSource,
      double pToBlowX,
      double pToBlowY,
      double pToBlowZ,
      float pRadius,
      boolean pFire,
      CustomExplosion.CustomBlockInteraction customBlockInteraction
   ) {
      super(pLevel, pSource, pToBlowX, pToBlowY, pToBlowZ, pRadius, pFire, customBlockInteraction);
      this.source = pSource;
   }

   @Override
   public void explode() {
      super.explode();
      if (this.level.isClientSide) {
         for (int i = 0; i < 100; i++) {
            double offsetX = (this.random.nextDouble() - 0.5) * 2.0;
            double offsetY = (this.random.nextDouble() - 0.5) * 2.0;
            double offsetZ = (this.random.nextDouble() - 0.5) * 2.0;
            this.level.addParticle(ParticleTypes.ELECTRIC_SPARK, this.x + offsetX, this.y + offsetY, this.z + offsetZ, 0.0, 0.0, 0.0);
         }
      }
   }

   @Override
   public void finalizeExplosion(boolean pSpawnParticles) {
      if (!this.level.isClientSide) {
         DamageSource damageSource = this.getDamageSource();
         LivingEntity shooter = null;
         if (this.source instanceof ProjectileEntity projectile) {
            shooter = projectile.getShooter();
         }

         for (LivingEntity entity : this.level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate((double)this.radius * 2.0))) {
            if (entity.isAlive()) {
               double distance = Math.sqrt(entity.distanceToSqr(this.x, this.y, this.z)) / (double)this.radius;
               if (!(distance > 1.0)) {
                  double exposure = (double)Explosion.getSeenPercent(this.getPosition(), entity);
                  if (!(exposure <= 0.0)) {
                     float damage = (float)((1.0 - distance) * exposure * (double)this.radius * 2.0);
                     if (entity == shooter) {
                        damage *= 0.1F;
                     }

                     entity.hurt(damageSource, damage);
                     double deltaX = entity.getX() - this.x;
                     double deltaZ = entity.getZ() - this.z;
                     double distance2D = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
                     if (distance2D > 0.0) {
                        entity.setDeltaMovement(entity.getDeltaMovement().add(deltaX / distance2D * exposure * 0.3, exposure * 0.3, deltaZ / distance2D * exposure * 0.3));
                     }
                  }
               }
            }
         }
      }

      super.finalizeExplosion(pSpawnParticles);
   }

   @NotNull
   public Vec3 getPosition() {
      return new Vec3(this.x, this.y, this.z);
   }

   private AABB getBoundingBox() {
      return new AABB(
         this.x - (double)this.radius,
         this.y - (double)this.radius,
         this.z - (double)this.radius,
         this.x + (double)this.radius,
         this.y + (double)this.radius,
         this.z + (double)this.radius
      );
   }
}
