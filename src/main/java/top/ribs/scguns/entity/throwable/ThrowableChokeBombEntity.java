package top.ribs.scguns.entity.throwable;


import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.particles.ParticleTypes;
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
import top.ribs.scguns.init.ModEntities;
import top.ribs.scguns.init.ModItems;

public class ThrowableChokeBombEntity extends ThrowableGrenadeEntity {
   private final float explosionRadius;
   private int cloudTicks;
   private final int cloudDuration;
   private final int activationDelay;
   private boolean cloudActive = false;

   public ThrowableChokeBombEntity(EntityType<? extends ThrowableGrenadeEntity> entityType, Level worldIn) {
      super(entityType, worldIn);
      this.explosionRadius = 4.0F;
      this.cloudDuration = 400;
      this.activationDelay = 40;
      this.cloudTicks = 0;
   }

   public ThrowableChokeBombEntity(Level world, LivingEntity entity, int timeLeft, float radius) {
      super((EntityType<? extends ThrowableItemEntity>)ModEntities.THROWABLE_CHOKE_BOMB.get(), world, entity);
      this.setShouldBounce(true);
      this.setItem(new ItemStack((ItemLike)ModItems.CHOKE_BOMB.get()));
      this.setMaxLife(timeLeft);
      this.explosionRadius = radius;
      this.cloudDuration = 400;
      this.activationDelay = 40;
      this.cloudTicks = 0;
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.cloudActive && this.tickCount >= this.activationDelay) {
         this.cloudActive = true;
         this.setMaxLife(this.tickCount + this.cloudDuration);
         this.onCloudActivation();
      }

      if (this.cloudActive) {
         this.cloudTicks++;
         this.emitChokeCloudParticles();
         this.applyChokeEffects();
         if (this.cloudTicks >= this.cloudDuration) {
            this.remove(RemovalReason.KILLED);
         }
      }
   }

   @Override
   public void particleTick() {
      if (this.level().isClientSide && !this.cloudActive) {
         this.level().addParticle(ParticleTypes.WHITE_ASH, true, this.getX(), this.getY() + 0.35, this.getZ(), 0.0, 0.0, 0.0);
         this.level().addParticle(ParticleTypes.SNOWFLAKE, true, this.getX(), this.getY() + 0.35, this.getZ(), 0.0, 0.0, 0.0);
      }
   }

   private void onCloudActivation() {
      double y = this.getY() + (double)this.getType().getDimensions().height() * 0.5;
      this.level().playSound(null, this.getX(), y, this.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 2.0F, 1.0F);
   }

   private void emitChokeCloudParticles() {
      Vec3 center = this.position();
      float intensity = 0.8F;
      if (!this.level().isClientSide) {
         ChokeBombCloud.spawnChokeCloudParticles(this.level(), center, (double)this.explosionRadius, intensity, this.random);
      }
   }

   private void applyChokeEffects() {
      if (!this.level().isClientSide) {
         Vec3 center = this.position();
         ChokeBombCloud.applyChokeEffects(this.level(), center, (double)this.explosionRadius);
         ChokeBombCloud.extinguishFireInArea(this.level(), center, (double)this.explosionRadius);
      }
   }

   @Override
   public void onDeath() {
      if (!this.cloudActive) {
         this.cloudActive = true;
         this.onCloudActivation();
      }
   }
}
