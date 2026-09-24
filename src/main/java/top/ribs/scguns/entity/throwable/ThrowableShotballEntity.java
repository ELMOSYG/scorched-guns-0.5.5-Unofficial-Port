package top.ribs.scguns.entity.throwable;


import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import top.ribs.scguns.init.ModEntities;
import top.ribs.scguns.init.ModItems;

public class ThrowableShotballEntity extends ThrowableItemEntity {
   private static final int MAX_BOUNCES = 3;
   private static final float BOUNCE_VELOCITY_RETENTION = 0.8F;
   private static final float MIN_BOUNCE_VELOCITY = 0.05F;
   private static final float BASE_DAMAGE = 9.0F;
   private static final float DAMAGE_REDUCTION_PER_BOUNCE = 0.85F;
   private int bouncesLeft;
   private float currentDamageMultiplier = 1.0F;

   public ThrowableShotballEntity(EntityType<? extends ThrowableItemEntity> entityType, Level worldIn) {
      super(entityType, worldIn);
      this.bouncesLeft = 3;
   }

   public ThrowableShotballEntity(Level world, LivingEntity entity) {
      super((EntityType<? extends ThrowableItemEntity>)ModEntities.THROWABLE_SHOTBALL.get(), world, entity);
      this.setShouldBounce(true);
      this.setGravityVelocity(0.04F);
      this.setItem(new ItemStack((ItemLike)ModItems.SHOTBALL.get()));
      this.setMaxLife(160);
      this.bouncesLeft = 3;
   }

   protected void defineSynchedData(SynchedEntityData.Builder builder) {
   }

   @Override
   public void tick() {
      super.tick();
      if (this.bouncesLeft <= 0 && this.getDeltaMovement().length() < 0.05F) {
         this.spawnDeathParticles(this.position());
         this.remove(RemovalReason.KILLED);
      }
   }

   @Override
   protected void onHit(HitResult result) {
      switch (result.getType()) {
         case BLOCK:
            BlockHitResult blockResult = (BlockHitResult)result;
            if (this.shouldBounce && this.bouncesLeft > 0) {
               BlockState state = this.level().getBlockState(blockResult.getBlockPos());
               double speed = this.getDeltaMovement().length();
               if (speed > 0.05F) {
                  this.level()
                     .playSound(
                        null,
                        blockResult.getLocation().x,
                        blockResult.getLocation().y,
                        blockResult.getLocation().z,
                        SoundEvents.STONE_HIT,
                        SoundSource.NEUTRAL,
                        0.8F,
                        1.2F + (this.random.nextFloat() - 0.5F) * 0.4F
                     );
                  this.spawnBounceParticles(blockResult.getLocation());
                  this.bounce(blockResult.getDirection());
                  this.bouncesLeft--;
                  this.currentDamageMultiplier *= 0.85F;
                  this.setDeltaMovement(
                     this.getDeltaMovement()
                        .add((this.random.nextDouble() - 0.5) * 0.1, (this.random.nextDouble() - 0.5) * 0.05, (this.random.nextDouble() - 0.5) * 0.1)
                  );
               } else {
                  this.spawnDeathParticles(this.position());
                  this.remove(RemovalReason.KILLED);
               }
            } else {
               this.spawnDeathParticles(blockResult.getLocation());
               this.remove(RemovalReason.KILLED);
            }
            break;
         case ENTITY:
            EntityHitResult entityResult = (EntityHitResult)result;
            Entity entity = entityResult.getEntity();
            if (this.shouldBounce && this.bouncesLeft > 0) {
               double speed = this.getDeltaMovement().length();
               if (speed > 0.1) {
                  float damage = 9.0F * this.currentDamageMultiplier * Math.min(1.0F, (float)(speed / 1.5));
                  entity.hurt(entity.damageSources().thrown(this, this.getOwner()), damage);
                  this.level()
                     .playSound(
                        null,
                        entityResult.getLocation().x,
                        entityResult.getLocation().y,
                        entityResult.getLocation().z,
                        SoundEvents.SLIME_BLOCK_HIT,
                        SoundSource.NEUTRAL,
                        0.6F,
                        1.0F + (this.random.nextFloat() - 0.5F) * 0.4F
                     );
                  this.spawnEntityHitParticles(entityResult.getLocation());
               }

               this.bounce(Direction.getNearest(this.getDeltaMovement().x(), this.getDeltaMovement().y(), this.getDeltaMovement().z()).getOpposite());
               this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, 0.8, 0.6));
               this.bouncesLeft--;
               this.currentDamageMultiplier *= 0.85F;
            } else {
               if (this.getDeltaMovement().length() > 0.1) {
                  float damage = 9.0F * this.currentDamageMultiplier;
                  entity.hurt(entity.damageSources().thrown(this, this.getOwner()), damage);
                  this.spawnEntityHitParticles(entityResult.getLocation());
               }

               this.remove(RemovalReason.KILLED);
            }
      }
   }

   @Override
   void bounce(Direction direction) {
      switch (direction.getAxis()) {
         case X:
            this.setDeltaMovement(this.getDeltaMovement().multiply(-0.8F, 0.85, 0.85));
            break;
         case Y:
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.85, -0.5600000083446502, 0.85));
            if (this.getDeltaMovement().y() < (double)(this.getGravity() * 2.0F)) {
               this.setDeltaMovement(this.getDeltaMovement().multiply(1.0, 0.1, 1.0));
            }
            break;
         case Z:
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.85, 0.85, -0.8F));
      }
   }

   @Override
   public void onDeath() {
      this.spawnFinalParticles(this.position());
      this.spawnDeathParticles(this.position());
   }

   public int getBouncesLeft() {
      return this.bouncesLeft;
   }

   public float getCurrentDamage() {
      return 9.0F * this.currentDamageMultiplier;
   }

   private void spawnBounceParticles(Vec3 position) {
      if (!this.level().isClientSide) {
         ServerLevel serverLevel = (ServerLevel)this.level();

         for (int i = 0; i < 4; i++) {
            double velocityX = (this.random.nextDouble() - 0.5) * 0.4;
            double velocityY = this.random.nextDouble() * 0.4;
            double velocityZ = (this.random.nextDouble() - 0.5) * 0.4;
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, position.x, position.y, position.z, 1, velocityX, velocityY, velocityZ, 0.04);
         }
      }
   }

   private void spawnEntityHitParticles(Vec3 position) {
      if (!this.level().isClientSide) {
         ServerLevel serverLevel = (ServerLevel)this.level();

         for (int i = 0; i < 5; i++) {
            double velocityX = (this.random.nextDouble() - 0.5) * 0.3;
            double velocityY = (this.random.nextDouble() - 0.5) * 0.3;
            double velocityZ = (this.random.nextDouble() - 0.5) * 0.3;
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, position.x, position.y, position.z, 1, velocityX, velocityY, velocityZ, 0.06);
         }
      }
   }

   private void spawnFinalParticles(Vec3 position) {
      if (!this.level().isClientSide) {
         ServerLevel serverLevel = (ServerLevel)this.level();

         for (int i = 0; i < 4; i++) {
            double velocityX = (this.random.nextDouble() - 0.5) * 0.2;
            double velocityY = this.random.nextDouble() * 0.2;
            double velocityZ = (this.random.nextDouble() - 0.5) * 0.2;
            serverLevel.sendParticles(ParticleTypes.CRIT, position.x, position.y, position.z, 1, velocityX, velocityY, velocityZ, 0.04);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, position.x, position.y, position.z, 1, velocityX, velocityY, velocityZ, 0.02);
         }

         if (this.getDeltaMovement().y() <= 0.01) {
            for (int i = 0; i < 3; i++) {
               double dustX = position.x + (this.random.nextDouble() - 0.5) * 0.4;
               double dustY = position.y;
               double dustZ = position.z + (this.random.nextDouble() - 0.5) * 0.4;
               serverLevel.sendParticles(ParticleTypes.POOF, dustX, dustY, dustZ, 1, 0.0, 0.05, 0.0, 0.01);
            }
         }
      }
   }

   private void spawnDeathParticles(Vec3 position) {
      if (!this.level().isClientSide) {
         ServerLevel serverLevel = (ServerLevel)this.level();

         for (int i = 0; i < 6; i++) {
            double velocityX = (this.random.nextDouble() - 0.5) * 0.3;
            double velocityY = this.random.nextDouble() * 0.4 + 0.1;
            double velocityZ = (this.random.nextDouble() - 0.5) * 0.3;
            serverLevel.sendParticles(
               ParticleTypes.LARGE_SMOKE,
               position.x + (this.random.nextDouble() - 0.5) * 0.3,
               position.y + (this.random.nextDouble() - 0.5) * 0.2,
               position.z + (this.random.nextDouble() - 0.5) * 0.3,
               1,
               velocityX,
               velocityY,
               velocityZ,
               0.02
            );
         }

         for (int i = 0; i < 4; i++) {
            double velocityX = (this.random.nextDouble() - 0.5) * 0.2;
            double velocityY = this.random.nextDouble() * 0.3 + 0.05;
            double velocityZ = (this.random.nextDouble() - 0.5) * 0.2;
            serverLevel.sendParticles(
               ParticleTypes.SMOKE,
               position.x + (this.random.nextDouble() - 0.5) * 0.4,
               position.y + (this.random.nextDouble() - 0.5) * 0.3,
               position.z + (this.random.nextDouble() - 0.5) * 0.4,
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
               0.3F,
               1.8F + (this.random.nextFloat() - 0.5F) * 0.4F
            );
      }
   }
}
