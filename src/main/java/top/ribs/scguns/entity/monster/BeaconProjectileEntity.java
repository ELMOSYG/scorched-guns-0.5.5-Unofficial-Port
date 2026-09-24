package top.ribs.scguns.entity.monster;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import top.ribs.scguns.init.ModEntities;

public class BeaconProjectileEntity extends Projectile {
   private static final EntityDataAccessor<Float> TARGET_X = SynchedEntityData.defineId(BeaconProjectileEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> TARGET_Z = SynchedEntityData.defineId(BeaconProjectileEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Boolean> HAS_LANDED = SynchedEntityData.defineId(BeaconProjectileEntity.class, EntityDataSerializers.BOOLEAN);
   private int ticksInFlight = 0;
   private static final int MAX_FLIGHT_TIME = 100;
   private boolean hasWarned = false;

   public BeaconProjectileEntity(EntityType<? extends BeaconProjectileEntity> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
   }

   public BeaconProjectileEntity(EntityType<? extends BeaconProjectileEntity> pEntityType, Level pLevel, LivingEntity pShooter) {
      this(pEntityType, pLevel);
      this.setOwner(pShooter);
      this.setPos(pShooter.getX(), pShooter.getEyeY() - 0.1, pShooter.getZ());
   }

   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      builder.define(TARGET_X, 0.0F);
      builder.define(TARGET_Z, 0.0F);
      builder.define(HAS_LANDED, false);
   }

   public void setLandingTarget(double targetX, double targetZ) {
      this.entityData.set(TARGET_X, (float)targetX);
      this.entityData.set(TARGET_Z, (float)targetZ);
   }

   public void tick() {
      super.tick();
      if (!(Boolean)this.entityData.get(HAS_LANDED)) {
         this.ticksInFlight++;
         Vec3 movement = this.getDeltaMovement();
         this.setDeltaMovement(movement.x, movement.y - 0.05, movement.z);
         if (!this.onGround() && this.ticksInFlight < 100 && !(this.getY() <= (double)this.level().getMinBuildHeight())) {
            if (!this.level().isClientSide) {
               Vec3 currentPos = this.position();
               Vec3 nextPos = currentPos.add(this.getDeltaMovement());
               BlockPos nextBlockPos = new BlockPos((int)nextPos.x, (int)nextPos.y, (int)nextPos.z);
               if (!this.level().getBlockState(nextBlockPos).isAir()) {
                  this.landAndSpawnBeacon();
                  return;
               }

               if (this.getDeltaMovement().y < 0.0) {
                  BlockPos belowPos = new BlockPos((int)currentPos.x, (int)(currentPos.y - 0.5), (int)currentPos.z);
                  if (!this.level().getBlockState(belowPos).isAir()) {
                     this.landAndSpawnBeacon();
                     return;
                  }
               }
            }

            Vec3 newMovement = this.getDeltaMovement();
            this.setPos(this.getX() + newMovement.x, this.getY() + newMovement.y, this.getZ() + newMovement.z);
            if (this.level().isClientSide) {
               this.level().addParticle(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
               if (this.random.nextInt(2) == 0) {
                  this.level()
                     .addParticle(
                        ParticleTypes.SMOKE,
                        this.getX() + (this.random.nextDouble() - 0.5) * 0.3,
                        this.getY() + (this.random.nextDouble() - 0.5) * 0.3,
                        this.getZ() + (this.random.nextDouble() - 0.5) * 0.3,
                        0.0,
                        0.05,
                        0.0
                     );
               }
            }

            if (!this.level().isClientSide && !this.hasWarned && this.ticksInFlight > 20) {
               float targetX = (Float)this.entityData.get(TARGET_X);
               float targetZ = (Float)this.entityData.get(TARGET_Z);
               double distanceToTarget = Math.sqrt(Math.pow(this.getX() - (double)targetX, 2.0) + Math.pow(this.getZ() - (double)targetZ, 2.0));
               if (distanceToTarget < 15.0 || this.getDeltaMovement().y < -0.3) {
                  this.showLandingWarning((double)targetX, (double)targetZ);
                  this.hasWarned = true;
               }
            }
         } else {
            this.landAndSpawnBeacon();
         }
      }
   }

   private void showLandingWarning(double targetX, double targetZ) {
      if (this.level() instanceof ServerLevel serverLevel) {
         double groundY = this.findGroundLevel(targetX, targetZ);

         for (int i = 0; i < 20; i++) {
            double angle = (double)i / 20.0 * Math.PI * 2.0;
            double radius = 3.0;
            double warningX = targetX + Math.cos(angle) * radius;
            double warningZ = targetZ + Math.sin(angle) * radius;
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, warningX, groundY + 0.5, warningZ, 2, 0.1, 0.5, 0.1, 0.05);
         }

         this.level().playSound(null, targetX, groundY, targetZ, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.HOSTILE, 1.0F, 1.5F);
      }
   }

   private double findGroundLevel(double x, double z) {
      for (int y = (int)this.getY(); y > this.level().getMinBuildHeight(); y--) {
         if (!this.level().getBlockState(new BlockPos((int)x, y, (int)z)).isAir()) {
            return (double)(y + 1);
         }
      }

      return this.getY();
   }

   private void landAndSpawnBeacon() {
      if (!(Boolean)this.entityData.get(HAS_LANDED)) {
         this.entityData.set(HAS_LANDED, true);
         if (!this.level().isClientSide) {
            double landX = this.getX();
            double landZ = this.getZ();
            double landY = this.findGroundLevel(landX, landZ);
            SignalBeaconEntity beacon = new SignalBeaconEntity((EntityType<? extends Mob>)ModEntities.SIGNAL_BEACON.get(), this.level());
            beacon.moveTo(landX, landY, landZ, this.random.nextFloat() * 360.0F, 0.0F);
            this.level().addFreshEntity(beacon);
            if (this.level() instanceof ServerLevel serverLevel) {
               for (int i = 0; i < 25; i++) {
                  double offsetX = (this.random.nextDouble() - 0.5) * 3.0;
                  double offsetY = this.random.nextDouble() * 2.0;
                  double offsetZ = (this.random.nextDouble() - 0.5) * 3.0;
                  serverLevel.sendParticles(ParticleTypes.EXPLOSION, landX + offsetX, landY + offsetY, landZ + offsetZ, 1, 0.0, 0.0, 0.0, 0.0);
               }

               for (int i = 0; i < 30; i++) {
                  double offsetX = (this.random.nextDouble() - 0.5) * 4.0;
                  double offsetY = this.random.nextDouble() * 1.5;
                  double offsetZ = (this.random.nextDouble() - 0.5) * 4.0;
                  serverLevel.sendParticles(ParticleTypes.POOF, landX + offsetX, landY + offsetY, landZ + offsetZ, 1, 0.2, 0.1, 0.2, 0.1);
               }
            }

            this.level().playSound(null, landX, landY, landZ, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.5F, 0.8F);
            this.level().playSound(null, landX, landY, landZ, SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.8F, 1.2F);
         }

         this.discard();
      }
   }

   protected void onHit(@NotNull HitResult pResult) {
      super.onHit(pResult);
      if (!this.level().isClientSide && !(Boolean)this.entityData.get(HAS_LANDED)) {
         this.landAndSpawnBeacon();
      }
   }

   public boolean isPickable() {
      return !(Boolean)this.entityData.get(HAS_LANDED);
   }

   public float getPickRadius() {
      return 1.0F;
   }

   public boolean isOnFire() {
      return false;
   }

   public boolean shouldRenderAtSqrDistance(double pDistance) {
      return pDistance < 16384.0;
   }

   protected void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
      super.addAdditionalSaveData(pCompound);
      pCompound.putInt("ticksInFlight", this.ticksInFlight);
      pCompound.putFloat("targetX", (Float)this.entityData.get(TARGET_X));
      pCompound.putFloat("targetZ", (Float)this.entityData.get(TARGET_Z));
      pCompound.putBoolean("hasLanded", (Boolean)this.entityData.get(HAS_LANDED));
      pCompound.putBoolean("hasWarned", this.hasWarned);
   }

   protected void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
      super.readAdditionalSaveData(pCompound);
      this.ticksInFlight = pCompound.getInt("ticksInFlight");
      this.entityData.set(TARGET_X, pCompound.getFloat("targetX"));
      this.entityData.set(TARGET_Z, pCompound.getFloat("targetZ"));
      this.entityData.set(HAS_LANDED, pCompound.getBoolean("hasLanded"));
      this.hasWarned = pCompound.getBoolean("hasWarned");
   }
}
