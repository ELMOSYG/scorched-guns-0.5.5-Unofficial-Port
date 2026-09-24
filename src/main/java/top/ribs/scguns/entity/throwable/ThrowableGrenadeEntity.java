package top.ribs.scguns.entity.throwable;


import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import top.ribs.scguns.entity.projectile.ProjectileEntity;
import top.ribs.scguns.init.ModEntities;
import top.ribs.scguns.init.ModItems;

public class ThrowableGrenadeEntity extends ThrowableItemEntity {
   public float rotation;
   public float prevRotation;
   private float explosionRadius = 2.5F;
   private float explosionDamage = 19.0F;

   public ThrowableGrenadeEntity(EntityType<? extends ThrowableItemEntity> entityType, Level worldIn) {
      super(entityType, worldIn);
   }

   public ThrowableGrenadeEntity(EntityType<? extends ThrowableItemEntity> entityType, Level world, LivingEntity entity) {
      super(entityType, world, entity);
      this.setShouldBounce(true);
      this.setGravityVelocity(0.05F);
      this.setItem(new ItemStack((ItemLike)ModItems.GRENADE.get()));
      this.setMaxLife(4);
   }

   public ThrowableGrenadeEntity(Level world, LivingEntity entity, int timeLeft) {
      super((EntityType<? extends ThrowableItemEntity>)ModEntities.THROWABLE_GRENADE.get(), world, entity);
      this.setShouldBounce(true);
      this.setGravityVelocity(0.05F);
      this.setItem(new ItemStack((ItemLike)ModItems.GRENADE.get()));
      this.setMaxLife(timeLeft);
   }

   public void setExplosionRadius(float radius) {
      this.explosionRadius = radius;
   }

   public void setExplosionDamage(float damage) {
      this.explosionDamage = damage;
   }

   public float getExplosionRadius() {
      return this.explosionRadius;
   }

   public float getExplosionDamage() {
      return this.explosionDamage;
   }

   protected void defineSynchedData(SynchedEntityData.Builder builder) {
   }

   @Override
   public void tick() {
      super.tick();
      this.prevRotation = this.rotation;
      double speed = this.getDeltaMovement().length();
      this.particleTick();
   }

   public void particleTick() {
      if (this.level().isClientSide) {
         this.level().addParticle(ParticleTypes.SMOKE, true, this.getX(), this.getY() + 0.25, this.getZ(), 0.0, 0.0, 0.0);
      }
   }

   @Override
   public void onDeath() {
      ProjectileEntity.createRocketExplosion(this, this.explosionRadius, this.explosionDamage, false);
   }
}
