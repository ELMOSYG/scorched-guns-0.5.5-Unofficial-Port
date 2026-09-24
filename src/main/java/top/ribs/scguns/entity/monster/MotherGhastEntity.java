package top.ribs.scguns.entity.monster;

import java.util.EnumSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import top.ribs.scguns.entity.projectile.EnemyProjectileEntity;
import top.ribs.scguns.init.ModEntities;
import top.ribs.scguns.init.ModSounds;

public class MotherGhastEntity extends FlyingMob implements Enemy {
   private static final EntityDataAccessor<Boolean> DATA_IS_CHARGING = SynchedEntityData.defineId(MotherGhastEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> TURRET_FLASH_TIMER = SynchedEntityData.defineId(MotherGhastEntity.class, EntityDataSerializers.INT);
   private int explosionPower = 1;
   private int turretCooldown = 0;
   private int burstCounter = 0;
   private int burstPauseCooldown = 0;
   private static final int TURRET_COOLDOWN_TICKS = 3;
   private static final int BURST_SIZE = 12;
   private static final int BURST_PAUSE_TICKS = 40;
   private static final double TURRET_RANGE = 60.0;

   public MotherGhastEntity(EntityType<? extends MotherGhastEntity> entityType, Level level) {
      super(entityType, level);
      this.xpReward = 10;
      this.moveControl = new MotherGhastEntity.MotherGhastMoveControl(this);
      this.setPersistenceRequired();
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(1, new MotherGhastEntity.MotherGhastShootFireballGoal(this));
      this.goalSelector.addGoal(2, new MotherGhastEntity.AggressiveFloatGoal(this));
      this.goalSelector.addGoal(5, new MotherGhastEntity.RandomFloatAroundGoal(this));
      this.goalSelector.addGoal(7, new MotherGhastEntity.MotherGhastLookGoal(this));
      this.targetSelector
         .addGoal(
            1,
            new NearestAttackableTargetGoal<>(
               this,
               Player.class,
               10,
               true,
               false,
               entity -> !(entity instanceof Player player)
                     ? false
                     : !player.isCreative() && !player.isSpectator() && Math.abs(entity.getY() - this.getY()) <= 4.0
            )
         );
   }

   public boolean isCharging() {
      return (Boolean)this.entityData.get(DATA_IS_CHARGING);
   }

   public void setCharging(boolean charging) {
      this.entityData.set(DATA_IS_CHARGING, charging);
   }

   public int getExplosionPower() {
      return this.explosionPower;
   }

   protected boolean shouldDespawnInPeaceful() {
      return true;
   }

   private static boolean isReflectedFireball(DamageSource damageSource) {
      return damageSource.getDirectEntity() instanceof LargeFireball && damageSource.getEntity() instanceof Player;
   }

   public boolean isInvulnerableTo(DamageSource source) {
      return source.is(DamageTypeTags.IS_FIRE) || !isReflectedFireball(source) && super.isInvulnerableTo(source);
   }

   public boolean hurt(DamageSource source, float amount) {
      if (this.isInvulnerableTo(source)) {
         return false;
      } else {
         if (!this.level().isClientSide && source.getEntity() instanceof LivingEntity attacker) {
            boolean shouldTarget = true;
            if (attacker instanceof Player player) {
               shouldTarget = !player.isCreative() && !player.isSpectator();
            }

            if (shouldTarget && this.getTarget() == null) {
               this.setTarget(attacker);
            }
         }

         return isReflectedFireball(source) ? super.hurt(source, 25.0F) : super.hurt(source, amount);
      }
   }

   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(DATA_IS_CHARGING, false);
      builder.define(TURRET_FLASH_TIMER, 0);
   }

   public static Builder createAttributes() {
      return Mob.createMobAttributes()
         .add(Attributes.MAX_HEALTH, 120.0)
         .add(Attributes.ARMOR_TOUGHNESS, 2.0)
         .add(Attributes.ARMOR, 12.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
         .add(Attributes.ATTACK_KNOCKBACK, 0.8F)
         .add(Attributes.FOLLOW_RANGE, 100.0);
   }

   public SoundSource getSoundSource() {
      return SoundSource.HOSTILE;
   }

   protected SoundEvent getAmbientSound() {
      return SoundEvents.GHAST_AMBIENT;
   }

   protected SoundEvent getHurtSound(DamageSource damageSource) {
      return SoundEvents.GHAST_HURT;
   }

   protected SoundEvent getDeathSound() {
      return SoundEvents.GHAST_DEATH;
   }

   protected float getSoundVolume() {
      return 3.0F;
   }

   public float getVoicePitch() {
      return 0.7F;
   }

   public int getMaxSpawnClusterSize() {
      return 1;
   }

   public void addAdditionalSaveData(CompoundTag compound) {
      super.addAdditionalSaveData(compound);
      compound.putByte("ExplosionPower", (byte)this.explosionPower);
   }

   public void readAdditionalSaveData(CompoundTag compound) {
      super.readAdditionalSaveData(compound);
      if (compound.contains("ExplosionPower", 99)) {
         this.explosionPower = compound.getByte("ExplosionPower");
      }
   }

   protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
      return 4.0F;
   }

   public void tick() {
      super.tick();
      if (this.level().isClientSide && this.tickCount % 4 == 0) {
         this.spawnVentSmoke();
      }

      if (!this.level().isClientSide) {
         int flashTimer = (Integer)this.entityData.get(TURRET_FLASH_TIMER);
         if (flashTimer > 0) {
            this.entityData.set(TURRET_FLASH_TIMER, flashTimer - 1);
         }

         if (this.turretCooldown > 0) {
            this.turretCooldown--;
         }

         if (this.burstPauseCooldown > 0) {
            this.burstPauseCooldown--;
         }

         LivingEntity target = this.getTarget();
         if (target != null && this.turretCooldown <= 0 && this.burstPauseCooldown <= 0) {
            double distanceSq = this.distanceToSqr(target);
            if (distanceSq < 3600.0 && this.hasLineOfSight(target)) {
               this.fireTurret(target);
               this.turretCooldown = 3;
               this.burstCounter++;
               if (this.burstCounter >= 12) {
                  this.burstCounter = 0;
                  this.burstPauseCooldown = 40;
               }
            }
         }
      }
   }

   private void spawnVentSmoke() {
      float yaw = (float)Math.toRadians((double)this.getYRot());
      float cos = (float)Math.cos((double)yaw);
      float sin = (float)Math.sin((double)yaw);
      double leftX = -4.5;
      double leftZ = -6.0;
      double vent1X = this.getX() + (double)cos * leftX - (double)sin * leftZ;
      double vent1Y = this.getY() + 6.5;
      double vent1Z = this.getZ() + (double)sin * leftX + (double)cos * leftZ;
      this.level().addParticle(ParticleTypes.LARGE_SMOKE, vent1X, vent1Y, vent1Z, 0.0, 0.05, 0.0);
      double rightX = -4.5;
      double rightZ = -4.0;
      double vent2X = this.getX() + (double)cos * rightX - (double)sin * rightZ;
      double vent2Y = this.getY() + 6.5;
      double vent2Z = this.getZ() + (double)sin * rightX + (double)cos * rightZ;
      this.level().addParticle(ParticleTypes.LARGE_SMOKE, vent2X, vent2Y, vent2Z, 0.0, 0.05, 0.0);
   }

   private void fireTurret(LivingEntity target) {
      float yaw = this.getYRot() * (float) (Math.PI / 180.0);
      double rightOffsetX = Math.cos((double)yaw) * 3.5;
      double rightOffsetZ = -Math.sin((double)yaw) * 3.5;
      Vec3 turretPos = this.position().add(rightOffsetX, 4.0, rightOffsetZ);
      double deltaX = target.getX() - turretPos.x;
      double deltaY = target.getY(0.5) - turretPos.y;
      double deltaZ = target.getZ() - turretPos.z;
      double horizontalDist = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
      deltaY += horizontalDist * 0.02;
      Vec3 direction = new Vec3(deltaX, deltaY, deltaZ).normalize();
      EnemyProjectileEntity projectile = new EnemyProjectileEntity((EntityType<EnemyProjectileEntity>)ModEntities.ENEMY_PROJECTILE.get(), this.level(), this);
      projectile.setPos(turretPos.x, turretPos.y, turretPos.z);
      projectile.setDeltaMovement(direction.scale(6.5));
      projectile.setNoGravity(true);
      this.level().addFreshEntity(projectile);
      this.triggerTurretFlash();
      this.level()
         .playSound(
            null,
            this.getX(),
            this.getY(),
            this.getZ(),
            (SoundEvent)ModSounds.MACHINE_GUN_FIRE.get(),
            this.getSoundSource(),
            1.0F,
            1.0F + this.random.nextFloat() * 0.2F
         );
   }

   public void triggerTurretFlash() {
      this.entityData.set(TURRET_FLASH_TIMER, 2);
   }

   public boolean isTurretFlashVisible() {
      return (Integer)this.entityData.get(TURRET_FLASH_TIMER) > 0;
   }

   static class AggressiveFloatGoal extends Goal {
      private final MotherGhastEntity ghast;
      private static final double PREFERRED_DISTANCE = 35.0;
      private static final double MIN_DISTANCE = 25.0;
      private static final double MAX_DISTANCE = 50.0;

      public AggressiveFloatGoal(MotherGhastEntity ghast) {
         super();
         this.ghast = ghast;
         this.setFlags(EnumSet.of(Flag.MOVE));
      }

      public boolean canUse() {
         LivingEntity target = this.ghast.getTarget();
         if (target == null) {
            return false;
         } else {
            MoveControl moveControl = this.ghast.getMoveControl();
            if (!moveControl.hasWanted()) {
               return true;
            } else {
               double deltaX = moveControl.getWantedX() - this.ghast.getX();
               double deltaY = moveControl.getWantedY() - this.ghast.getY();
               double deltaZ = moveControl.getWantedZ() - this.ghast.getZ();
               double distanceSq = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
               return distanceSq < 1.0 || distanceSq > 900.0;
            }
         }
      }

      public boolean canContinueToUse() {
         return false;
      }

      public void start() {
         LivingEntity target = this.ghast.getTarget();
         if (target != null) {
            RandomSource random = this.ghast.getRandom();
            double currentDistance = (double)this.ghast.distanceTo(target);
            double targetX;
            double targetY;
            double targetZ;
            if (currentDistance < 25.0) {
               double dx = this.ghast.getX() - target.getX();
               double dy = this.ghast.getY() - target.getY();
               double dz = this.ghast.getZ() - target.getZ();
               double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
               targetX = target.getX() + dx / length * 35.0;
               targetY = target.getY() + dy / length * 35.0 + (double)((random.nextFloat() * 2.0F - 1.0F) * 3.0F);
               targetZ = target.getZ() + dz / length * 35.0;
            } else if (currentDistance > 50.0) {
               double dx = target.getX() - this.ghast.getX();
               double dy = target.getY() - this.ghast.getY();
               double dz = target.getZ() - this.ghast.getZ();
               double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
               targetX = this.ghast.getX() + dx / length * (currentDistance - 35.0);
               targetY = this.ghast.getY() + dy / length * (currentDistance - 35.0);
               targetZ = this.ghast.getZ() + dz / length * (currentDistance - 35.0);
            } else {
               double angle = random.nextDouble() * Math.PI * 2.0;
               double distance = 17.5;
               targetX = target.getX() + Math.cos(angle) * distance;
               targetY = target.getY() + (double)(random.nextFloat() * 6.0F - 3.0F);
               targetZ = target.getZ() + Math.sin(angle) * distance;
            }

            this.ghast.getMoveControl().setWantedPosition(targetX, targetY, targetZ, 1.0);
         }
      }
   }

   static class MotherGhastLookGoal extends Goal {
      private final MotherGhastEntity ghast;

      public MotherGhastLookGoal(MotherGhastEntity ghast) {
         super();
         this.ghast = ghast;
         this.setFlags(EnumSet.of(Flag.LOOK));
      }

      public boolean canUse() {
         return true;
      }

      public boolean requiresUpdateEveryTick() {
         return true;
      }

      public void tick() {
         if (this.ghast.getTarget() == null) {
            Vec3 deltaMovement = this.ghast.getDeltaMovement();
            this.ghast.setYRot(-((float)Mth.atan2(deltaMovement.x, deltaMovement.z)) * (180.0F / (float)Math.PI));
            this.ghast.yBodyRot = this.ghast.getYRot();
         } else {
            LivingEntity target = this.ghast.getTarget();
            if (target.distanceToSqr(this.ghast) < 4096.0) {
               double deltaX = target.getX() - this.ghast.getX();
               double deltaZ = target.getZ() - this.ghast.getZ();
               this.ghast.setYRot(-((float)Mth.atan2(deltaX, deltaZ)) * (180.0F / (float)Math.PI));
               this.ghast.yBodyRot = this.ghast.getYRot();
            }
         }
      }
   }

   static class MotherGhastMoveControl extends MoveControl {
      private final MotherGhastEntity ghast;
      private int floatDuration;

      public MotherGhastMoveControl(MotherGhastEntity ghast) {
         super(ghast);
         this.ghast = ghast;
      }

      public void tick() {
         if (this.operation == MoveControl.Operation.MOVE_TO && this.floatDuration-- <= 0) {
            this.floatDuration = this.floatDuration + this.ghast.getRandom().nextInt(5) + 2;
            Vec3 vec3 = new Vec3(this.wantedX - this.ghast.getX(), this.wantedY - this.ghast.getY(), this.wantedZ - this.ghast.getZ());
            double distance = vec3.length();
            vec3 = vec3.normalize();
            if (this.canReach(vec3, Mth.ceil(distance))) {
               this.ghast.setDeltaMovement(this.ghast.getDeltaMovement().add(vec3.scale(0.1)));
            } else {
               this.operation = MoveControl.Operation.WAIT;
            }
         }
      }

      private boolean canReach(Vec3 pos, int length) {
         AABB aabb = this.ghast.getBoundingBox();

         for (int i = 1; i < length; i++) {
            aabb = aabb.move(pos);
            if (!this.ghast.level().noCollision(this.ghast, aabb)) {
               return false;
            }
         }

         return true;
      }
   }

   static class MotherGhastShootFireballGoal extends Goal {
      private final MotherGhastEntity ghast;
      public int chargeTime;
      private int fireballBurstCount = 0;
      private int fireballsToShoot = 0;

      public MotherGhastShootFireballGoal(MotherGhastEntity ghast) {
         super();
         this.ghast = ghast;
      }

      public boolean canUse() {
         return this.ghast.getTarget() != null;
      }

      public void start() {
         this.chargeTime = 0;
         this.fireballBurstCount = 0;
         this.fireballsToShoot = 0;
      }

      public void stop() {
         this.ghast.setCharging(false);
         this.fireballBurstCount = 0;
         this.fireballsToShoot = 0;
      }

      public boolean requiresUpdateEveryTick() {
         return true;
      }

      public void tick() {
         LivingEntity target = this.ghast.getTarget();
         if (target != null) {
            if (target.distanceToSqr(this.ghast) < 4096.0 && this.ghast.hasLineOfSight(target)) {
               Level level = this.ghast.level();
               this.chargeTime++;
               if (this.chargeTime == 10 && !this.ghast.isSilent()) {
                  level.levelEvent(null, 1015, this.ghast.blockPosition(), 0);
                  this.fireballsToShoot = 2 + this.ghast.getRandom().nextInt(3);
               }

               if ((this.chargeTime == 20 || this.fireballBurstCount > 0 && this.chargeTime % 5 == 0) && this.fireballBurstCount < this.fireballsToShoot) {
                  Vec3 viewVector = this.ghast.getViewVector(1.0F);
                  double d2 = target.getX() - (this.ghast.getX() + viewVector.x * 4.0);
                  double d3 = target.getY(0.5) - (0.5 + this.ghast.getY(0.5));
                  double d4 = target.getZ() - (this.ghast.getZ() + viewVector.z * 4.0);
                  if (!this.ghast.isSilent() && this.fireballBurstCount == 0) {
                     level.levelEvent(null, 1016, this.ghast.blockPosition(), 0);
                  }

                  double speed = 2.0;
                  LargeFireball fireball = new LargeFireball(level, this.ghast, new Vec3(d2 * speed, d3 * speed, d4 * speed), 0);
                  fireball.setPos(
                     this.ghast.getX() + viewVector.x * 4.0, this.ghast.getY(0.5) + 0.5, fireball.getZ() + viewVector.z * 4.0
                  );
                  level.addFreshEntity(fireball);
                  this.fireballBurstCount++;
                  if (this.fireballBurstCount >= this.fireballsToShoot) {
                     this.chargeTime = -60;
                     this.fireballBurstCount = 0;
                     this.fireballsToShoot = 0;
                  }
               }
            } else if (this.chargeTime > 0) {
               this.chargeTime--;
            }

            this.ghast.setCharging(this.chargeTime > 10);
         }
      }
   }

   static class RandomFloatAroundGoal extends Goal {
      private final MotherGhastEntity ghast;

      public RandomFloatAroundGoal(MotherGhastEntity ghast) {
         super();
         this.ghast = ghast;
         this.setFlags(EnumSet.of(Flag.MOVE));
      }

      public boolean canUse() {
         if (this.ghast.getTarget() != null) {
            return false;
         } else {
            MoveControl moveControl = this.ghast.getMoveControl();
            if (!moveControl.hasWanted()) {
               return true;
            } else {
               double deltaX = moveControl.getWantedX() - this.ghast.getX();
               double deltaY = moveControl.getWantedY() - this.ghast.getY();
               double deltaZ = moveControl.getWantedZ() - this.ghast.getZ();
               double distanceSq = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
               return distanceSq < 1.0 || distanceSq > 3600.0;
            }
         }
      }

      public boolean canContinueToUse() {
         return false;
      }

      public void start() {
         RandomSource random = this.ghast.getRandom();
         double d0 = this.ghast.getX() + (double)((random.nextFloat() * 2.0F - 1.0F) * 16.0F);
         double d1 = this.ghast.getY() + (double)((random.nextFloat() * 2.0F - 1.0F) * 16.0F);
         double d2 = this.ghast.getZ() + (double)((random.nextFloat() * 2.0F - 1.0F) * 16.0F);
         this.ghast.getMoveControl().setWantedPosition(d0, d1, d2, 1.0);
      }
   }
}
