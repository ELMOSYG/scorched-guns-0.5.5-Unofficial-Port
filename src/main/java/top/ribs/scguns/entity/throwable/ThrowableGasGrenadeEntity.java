package top.ribs.scguns.entity.throwable;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import top.ribs.scguns.common.ChokeBombCloud;
import top.ribs.scguns.common.SulfurGasCloud;
import top.ribs.scguns.init.ModEntities;
import top.ribs.scguns.init.ModItems;

public class ThrowableGasGrenadeEntity extends ThrowableGrenadeEntity {
   private final float explosionRadius;
   private int remainingTicks;
   private final int delayTicks;

   public ThrowableGasGrenadeEntity(EntityType<? extends ThrowableGrenadeEntity> entityType, Level worldIn) {
      super(entityType, worldIn);
      this.explosionRadius = 6.0F;
      this.remainingTicks = 400;
      this.delayTicks = 30;
   }

   public ThrowableGasGrenadeEntity(Level world, LivingEntity entity, int timeLeft, float radius) {
      super((EntityType<? extends ThrowableItemEntity>)ModEntities.THROWABLE_GAS_GRENADE.get(), world, entity);
      this.setShouldBounce(true);
      this.setItem(new ItemStack((ItemLike)ModItems.GAS_GRENADE.get()));
      this.setMaxLife(400);
      this.explosionRadius = radius;
      this.remainingTicks = 400;
      this.delayTicks = 20;
   }

   @Override
   public void tick() {
      super.tick();
      if (this.remainingTicks > 0) {
         if (this.remainingTicks <= 400 - this.delayTicks
            && !ChokeBombCloud.isChokeBombActive(this.level(), this.position(), (double)this.explosionRadius * 1.5)) {
            this.emitGasCloudParticles();
            this.applyGasEffects();
            if (!this.level().isClientSide) {
               Vec3 center = this.position();
               SulfurGasCloud.checkAndHandleFireExplosion(this.level(), center, (double)this.explosionRadius);
            }
         }

         this.remainingTicks--;
      } else {
         this.remove(RemovalReason.KILLED);
      }
   }

   private void emitGasCloudParticles() {
      Vec3 center = this.position();
      float intensity = 0.8F;
      if (!this.level().isClientSide) {
         SulfurGasCloud.spawnEnhancedGasCloud(this.level(), center, (double)this.explosionRadius, intensity, this.random);
      }
   }

   private void applyGasEffects() {
      if (!this.level().isClientSide) {
         Vec3 center = this.position();
         SulfurGasCloud.applyGasEffects(this.level(), center, (double)this.explosionRadius, 100, 2);
      }
   }

   @Override
   public void onDeath() {
      double y = this.getY() + (double)this.getType().getDimensions().height() * 0.5;
      this.level().playSound(null, this.getX(), y, this.getZ(), SoundEvents.CAT_HISS, SoundSource.BLOCKS, 2.0F, 1.0F);
   }
}
