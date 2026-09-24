package top.ribs.scguns.entity.monster;


import net.minecraft.core.Holder;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import top.ribs.scguns.entity.projectile.EnemyProjectileEntity;
import top.ribs.scguns.init.ModSounds;

public class SkyCarrierEntity extends FlyingMob implements Enemy {
   private static final EntityDataAccessor<Boolean> DATA_IS_CHARGING = SynchedEntityData.defineId(SkyCarrierEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> MUZZLE_FLASH_TIMER = SynchedEntityData.defineId(SkyCarrierEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> DATA_IS_PHASING = SynchedEntityData.defineId(SkyCarrierEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> DATA_IS_CHARGE_ATTACKING = SynchedEntityData.defineId(
      SkyCarrierEntity.class, EntityDataSerializers.BOOLEAN
   );
   public final AnimationState idleAnimationState = new AnimationState();
   private int idleAnimationTimeout = 0;
   private int shootCooldown = 0;
   private int chargeAttackCooldown = 0;
   private Vec3 initialTargetPosition = null;
   private int phasingTimer = 0;
   private static final int MAX_PHASING_TIME = 200;

   public SkyCarrierEntity(EntityType<? extends SkyCarrierEntity> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
      this.moveControl = new SkyCarrierEntity.SkyCarrierMoveControl(this, 5.0, 8.0, 2.0, 0.9, 0.15);
   }

   public void setInitialTarget(Vec3 targetPosition) {
      this.initialTargetPosition = targetPosition;
      this.phasingTimer = 200;
      this.setPhasing(true);
      Vec3 direction = targetPosition.subtract(this.position()).normalize();
      float targetYaw = (float)(Math.atan2(direction.z, direction.x) * (180.0 / Math.PI) - 90.0);
      this.setYRot(targetYaw);
      this.yBodyRot = targetYaw;
      this.yHeadRot = targetYaw;
   }

   public boolean isPhasing() {
      return (Boolean)this.entityData.get(DATA_IS_PHASING);
   }

   public void setPhasing(boolean phasing) {
      this.entityData.set(DATA_IS_PHASING, phasing);
      this.noPhysics = phasing;
   }

   public boolean isChargeAttacking() {
      return (Boolean)this.entityData.get(DATA_IS_CHARGE_ATTACKING);
   }

   public void setChargeAttacking(boolean charging) {
      this.entityData.set(DATA_IS_CHARGE_ATTACKING, charging);
   }

   public boolean canBeAffected(@NotNull MobEffectInstance pPotionEffect) {
      Holder<MobEffect> effect = pPotionEffect.getEffect();
      return effect != MobEffects.POISON
            && effect != MobEffects.WITHER
            && effect != MobEffects.HUNGER
            && effect != MobEffects.REGENERATION
            && effect != MobEffects.SATURATION
            && effect != MobEffects.CONFUSION
            && effect != MobEffects.BLINDNESS
            && effect != MobEffects.WEAKNESS
            && effect != MobEffects.MOVEMENT_SLOWDOWN
            && effect != MobEffects.DIG_SLOWDOWN
            && effect != MobEffects.HARM
            && effect != MobEffects.HEAL
         ? super.canBeAffected(pPotionEffect)
         : false;
   }

   public boolean shouldDespawnInPeaceful() {
      return true;
   }

   public void tick() {
      super.tick();
      if (!this.level().isClientSide() && this.isPhasing()) {
         if (this.getTarget() != null) {
            this.setPhasing(false);
            this.initialTargetPosition = null;
         } else {
            this.handlePhasing();
         }
      }

      if (this.level().isClientSide()) {
         this.setupAnimationStates();
         this.spawnSmokeParticles();
      } else {
         if (this.shootCooldown > 0) {
            this.shootCooldown--;
         } else {
            LivingEntity target = this.getTarget();
            if (target != null && this.distanceToSqr(target) < 625.0 && !this.isPhasing()) {
               this.fireProjectile();
               this.shootCooldown = 15;
            }
         }

         if (this.chargeAttackCooldown > 0) {
            this.chargeAttackCooldown--;
         }
      }

      int currentTimer = (Integer)this.entityData.get(MUZZLE_FLASH_TIMER);
      if (currentTimer > 0) {
         this.entityData.set(MUZZLE_FLASH_TIMER, currentTimer - 1);
      }
   }

   private void handlePhasing() {
      if (this.initialTargetPosition == null) {
         this.setPhasing(false);
      } else {
         double distanceToTarget = this.position().distanceTo(this.initialTargetPosition);
         this.phasingTimer--;
         if (!(distanceToTarget < 3.0) && this.phasingTimer > 0) {
            Vec3 direction = this.initialTargetPosition.subtract(this.position()).normalize();
            this.setDeltaMovement(direction.scale(0.3));
            float targetYaw = (float)(Math.atan2(direction.z, direction.x) * (180.0 / Math.PI) - 90.0);
            this.setYRot(targetYaw);
            this.yBodyRot = targetYaw;
            this.yHeadRot = targetYaw;
         } else {
            this.setPhasing(false);
            this.initialTargetPosition = null;
         }
      }
   }

   private void spawnSmokeParticles() {
      if (this.isMuzzleFlashVisible()) {
         double offsetX = 0.0;
         double offsetY = (double)this.getEyeHeight() - 0.5;
         double offsetZ = 0.0;
         double posX = this.getX() + offsetX;
         double posY = this.getY() + offsetY;
         double posZ = this.getZ() + offsetZ;
         RandomSource random = this.getRandom();

         for (int i = 0; i < 1; i++) {
            double particleOffsetX = random.nextGaussian() * 0.1;
            double particleOffsetY = random.nextGaussian() * 0.1;
            double particleOffsetZ = random.nextGaussian() * 0.1;
            this.level().addParticle(ParticleTypes.SMOKE, posX, posY, posZ, particleOffsetX, particleOffsetY, particleOffsetZ);
         }
      }
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(1, new SkyCarrierEntity.RandomFloatAroundGoal(this, 100));
      this.targetSelector.addGoal(1, new NearestAttackableTargetGoal(this, Player.class, true));
      this.goalSelector.addGoal(6, new SkyCarrierEntity.SkyCarrierFaceAndBackAwayFromTargetGoal(this));
      this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
   }

   public static Builder createAttributes() {
      return Mob.createLivingAttributes()
         .add(Attributes.MAX_HEALTH, 32.0)
         .add(Attributes.FOLLOW_RANGE, 50.0)
         .add(Attributes.ARMOR_TOUGHNESS, 0.1F)
         .add(Attributes.ATTACK_KNOCKBACK, 0.0)
         .add(Attributes.ATTACK_DAMAGE, 2.0);
   }

   private void setupAnimationStates() {
      if (this.idleAnimationTimeout <= 0) {
         this.idleAnimationTimeout = this.random.nextInt(40) + 80;
         this.idleAnimationState.start(this.tickCount);
      } else {
         this.idleAnimationTimeout--;
      }
   }

   protected void updateWalkAnimation(float pPartialTick) {
      float f = this.getPose() == Pose.STANDING ? Math.min(pPartialTick * 6.0F, 1.0F) : 0.0F;
      this.walkAnimation.update(f, 0.2F);
   }

   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(DATA_IS_CHARGING, false);
      builder.define(MUZZLE_FLASH_TIMER, 0);
      builder.define(DATA_IS_PHASING, false);
      builder.define(DATA_IS_CHARGE_ATTACKING, false);
   }

   protected SoundEvent getAmbientSound() {
      return SoundEvents.BEACON_AMBIENT;
   }

   protected SoundEvent getHurtSound(@NotNull DamageSource pDamageSource) {
      return SoundEvents.IRON_GOLEM_HURT;
   }

   protected SoundEvent getDeathSound() {
      return SoundEvents.IRON_GOLEM_DEATH;
   }

   public void triggerMuzzleFlash() {
      this.entityData.set(MUZZLE_FLASH_TIMER, 6);
   }

   public boolean isMuzzleFlashVisible() {
      return (Integer)this.entityData.get(MUZZLE_FLASH_TIMER) > 0;
   }

   private void fireProjectile() {
      LivingEntity target = this.getTarget();
      if (target != null) {
         double turretOffsetHeight = 0.4;
         double turretOffsetBack = 0.4;
         double spawnHeight = this.getY() + (double)this.getBbHeight() + turretOffsetHeight;
         double spawnX = this.getX() - Math.sin(Math.toRadians((double)this.getYRot())) * turretOffsetBack;
         double spawnZ = this.getZ() + Math.cos(Math.toRadians((double)this.getYRot())) * turretOffsetBack;
         EnemyProjectileEntity brassBolt = new EnemyProjectileEntity(this.level(), this);
         brassBolt.setPos(spawnX, spawnHeight, spawnZ);
         double dx = target.getX() - spawnX;
         double dy = target.getEyeY() - spawnHeight + 0.1;
         double dz = target.getZ() - spawnZ;
         brassBolt.shoot(dx, dy, dz, 3.0F, 2.0F);
         this.level().addFreshEntity(brassBolt);
         this.level()
            .playSound(
               null, this.getX(), this.getY(), this.getZ(), (SoundEvent)ModSounds.BRUISER_SILENCED_FIRE.get(), SoundSource.HOSTILE, 1.0F, 1.0F
            );
         this.triggerMuzzleFlash();
      }
   }

   @NotNull
   protected PathNavigation createNavigation(Level level) {
      return new FlyingPathNavigation(this, level);
   }

   public static class RandomFloatAroundGoal extends Goal {
      private final SkyCarrierEntity skyCarrier;
      private int tickDelay;

      public RandomFloatAroundGoal(SkyCarrierEntity skyCarrier, int initialDelay) {
         super();
         this.skyCarrier = skyCarrier;
         this.tickDelay = initialDelay;
         this.setFlags(EnumSet.of(Flag.MOVE));
      }

      public boolean canUse() {
         return !this.skyCarrier.isPhasing() && --this.tickDelay <= 0;
      }

      public void start() {
         this.setNewWanderTarget();
         this.tickDelay = 100;
      }

      private void setNewWanderTarget() {
         double x = this.skyCarrier.getX() + (this.skyCarrier.getRandom().nextDouble() * 20.0 - 10.0);
         double y = this.skyCarrier.getY() + (this.skyCarrier.getRandom().nextDouble() * 20.0 - 10.0);
         double z = this.skyCarrier.getZ() + (this.skyCarrier.getRandom().nextDouble() * 20.0 - 10.0);
         this.skyCarrier.getMoveControl().setWantedPosition(x, y, z, 1.0);
      }
   }

   private static class SkyCarrierFaceAndBackAwayFromTargetGoal extends Goal {
      private final SkyCarrierEntity skyCarrier;
      private float currentYaw;

      public SkyCarrierFaceAndBackAwayFromTargetGoal(SkyCarrierEntity skyCarrier) {
         super();
         this.skyCarrier = skyCarrier;
         this.currentYaw = skyCarrier.getYRot();
         this.setFlags(EnumSet.of(Flag.LOOK));
      }

      public boolean canUse() {
         return !this.skyCarrier.isPhasing() && this.skyCarrier.getTarget() != null;
      }

      public boolean requiresUpdateEveryTick() {
         return true;
      }

      public void tick() {
         LivingEntity target = this.skyCarrier.getTarget();
         if (target != null) {
            Vec3 targetPos = new Vec3(target.getX(), target.getY(), target.getZ());
            Vec3 ourPos = new Vec3(this.skyCarrier.getX(), this.skyCarrier.getY(), this.skyCarrier.getZ());
            Vec3 vectorToTarget = targetPos.subtract(ourPos).normalize();
            float targetYaw = -((float)Math.atan2(vectorToTarget.x, vectorToTarget.z)) * (180.0F / (float)Math.PI);
            targetYaw = Mth.wrapDegrees(targetYaw);
            float yawDiff = Mth.wrapDegrees(targetYaw - this.currentYaw);
            float turnSpeed = 4.0F;
            this.currentYaw = Mth.wrapDegrees(this.currentYaw + Mth.clamp(yawDiff, -turnSpeed, turnSpeed));
            this.skyCarrier.yHeadRot = this.currentYaw;
         }
      }
   }

   private static class SkyCarrierMoveControl extends MoveControl {
      private final SkyCarrierEntity skyCarrier;
      private final double minDistance;
      private final double maxDistance;
      private final double bufferZone;
      private final double maxSpeed;
      private final double backingSpeed;
      private Vec3 currentVelocity = Vec3.ZERO;
      private float currentYaw = 0.0F;
      private float targetYaw = 0.0F;
      private int chargeTicks = 0;
      private Vec3 chargeDirection = Vec3.ZERO;

      public SkyCarrierMoveControl(SkyCarrierEntity skyCarrier, double minDistance, double maxDistance, double bufferZone, double maxSpeed, double backingSpeed) {
         super(skyCarrier);
         this.skyCarrier = skyCarrier;
         this.minDistance = minDistance;
         this.maxDistance = maxDistance;
         this.bufferZone = bufferZone;
         this.maxSpeed = maxSpeed;
         this.backingSpeed = backingSpeed;
         this.currentYaw = skyCarrier.getYRot();
      }

      public void tick() {
         if (!this.skyCarrier.isPhasing()) {
            if (this.skyCarrier.isChargeAttacking()) {
               this.handleChargeAttack();
            } else {
               LivingEntity target = this.skyCarrier.getTarget();
               Vec3 desiredVelocity = Vec3.ZERO;
               AABB repulsionBox = this.skyCarrier.getBoundingBox().inflate(2.0);
               List<SkyCarrierEntity> nearbyCarriers = this.skyCarrier.level().getEntitiesOfClass(SkyCarrierEntity.class, repulsionBox, e -> e != this.skyCarrier);
               Vec3 repulsionVector = Vec3.ZERO;

               for (SkyCarrierEntity other : nearbyCarriers) {
                  Vec3 toOther = this.skyCarrier.position().subtract(other.position());
                  double distance = toOther.length();
                  if (distance < 2.0 && distance > 0.0) {
                     repulsionVector = repulsionVector.add(toOther.normalize().scale(0.5 / distance));
                  }
               }

               if (target != null) {
                  Vec3 targetPos = target.position();
                  Vec3 ourPos = this.skyCarrier.position();
                  Vec3 directionToTarget = targetPos.subtract(ourPos);
                  double distance = directionToTarget.length();
                  if (distance < this.minDistance) {
                     desiredVelocity = directionToTarget.normalize().reverse().scale(this.backingSpeed);
                     if (this.skyCarrier.chargeAttackCooldown <= 0 && this.skyCarrier.getRandom().nextFloat() < 0.08F) {
                        this.initiateChargeAttack(directionToTarget.normalize());
                        return;
                     }
                  } else if (distance > this.maxDistance + this.bufferZone) {
                     desiredVelocity = directionToTarget.normalize().scale(this.maxSpeed);
                  } else if (distance < this.minDistance - this.bufferZone) {
                     desiredVelocity = directionToTarget.normalize().reverse().scale(this.backingSpeed);
                  }

                  desiredVelocity = desiredVelocity.add(repulsionVector.scale(0.3));
                  double var22 = 0.15;
                  double var25 = 0.88;
                  this.currentVelocity = this.currentVelocity.scale(var25);
                  Vec3 accelerationVec = desiredVelocity.subtract(this.currentVelocity).scale(var22);
                  this.currentVelocity = this.currentVelocity.add(accelerationVec);
                  double currentSpeed = this.currentVelocity.length();
                  if (currentSpeed > this.maxSpeed) {
                     this.currentVelocity = this.currentVelocity.normalize().scale(this.maxSpeed);
                  }

                  this.skyCarrier.setDeltaMovement(this.currentVelocity);
                  double dx = target.getX() - this.skyCarrier.getX();
                  double dz = target.getZ() - this.skyCarrier.getZ();
                  this.targetYaw = (float)(Math.atan2(dz, dx) * (180.0 / Math.PI) - 90.0);
                  float yawDifference = Mth.wrapDegrees(this.targetYaw - this.currentYaw);
                  float maxTurnSpeed = 9.0F;
                  float turnAmount = Mth.clamp(yawDifference, -maxTurnSpeed, maxTurnSpeed);
                  this.currentYaw = Mth.wrapDegrees(this.currentYaw + turnAmount);
                  this.skyCarrier.setYRot(this.currentYaw);
                  this.skyCarrier.yBodyRot = this.currentYaw;
                  this.skyCarrier.yHeadRot = this.currentYaw;
               } else {
                  this.handleIdleMovement();
               }
            }
         }
      }

      private void initiateChargeAttack(Vec3 direction) {
         this.skyCarrier.setChargeAttacking(true);
         this.chargeDirection = direction;
         this.chargeTicks = 15;
         this.skyCarrier.chargeAttackCooldown = 100;
         this.skyCarrier
            .level()
            .playSound(
               null, this.skyCarrier.getX(), this.skyCarrier.getY(), this.skyCarrier.getZ(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.HOSTILE, 0.8F, 1.5F
            );
      }

      private void handleChargeAttack() {
         if (this.chargeTicks > 0) {
            this.chargeTicks--;
            double chargeSpeed = 1.2;
            this.skyCarrier.setDeltaMovement(this.chargeDirection.scale(chargeSpeed));
            LivingEntity target = this.skyCarrier.getTarget();
            if (target != null) {
               AABB collisionBox = this.skyCarrier.getBoundingBox().inflate(0.5);
               if (collisionBox.intersects(target.getBoundingBox())) {
                  this.performChargeImpact(target);
                  return;
               }
            }

            AABB nearbyBox = this.skyCarrier.getBoundingBox().inflate(1.5);

            for (LivingEntity entity : this.skyCarrier
               .level()
               .getEntitiesOfClass(LivingEntity.class, nearbyBox, entityx -> entityx != this.skyCarrier && entityx instanceof Player)) {
               if (this.skyCarrier.hasLineOfSight(entity)) {
                  this.performChargeImpact(entity);
                  return;
               }
            }

            if (this.chargeTicks <= 0) {
               this.skyCarrier.setChargeAttacking(false);
            }
         } else {
            this.skyCarrier.setChargeAttacking(false);
         }
      }

      private void performChargeImpact(LivingEntity target) {
         float damage = 4.0F;
         target.hurt(this.skyCarrier.damageSources().mobAttack(this.skyCarrier), damage);
         Vec3 targetPos = target.position();
         Vec3 carrierPos = this.skyCarrier.position();
         Vec3 knockbackDir = targetPos.subtract(carrierPos).normalize();
         double knockbackStrength = 1.5;
         target.setDeltaMovement(knockbackDir.x * knockbackStrength, 0.4, knockbackDir.z * knockbackStrength);
         target.hurtMarked = true;
         this.skyCarrier
            .level()
            .playSound(
               null, this.skyCarrier.getX(), this.skyCarrier.getY(), this.skyCarrier.getZ(), SoundEvents.IRON_GOLEM_DAMAGE, SoundSource.HOSTILE, 1.0F, 1.2F
            );
         this.skyCarrier.setChargeAttacking(false);
         this.chargeTicks = 0;
      }

      private void handleIdleMovement() {
         if (this.operation == MoveControl.Operation.MOVE_TO) {
            Vec3 direction = new Vec3(
               this.wantedX - this.skyCarrier.getX(), this.wantedY - this.skyCarrier.getY(), this.wantedZ - this.skyCarrier.getZ()
            );
            double distance = direction.length();
            if (distance < 1.0) {
               this.operation = MoveControl.Operation.WAIT;
               this.currentVelocity = this.currentVelocity.scale(0.85);
               this.skyCarrier.setDeltaMovement(this.currentVelocity);
            } else {
               Vec3 desiredVelocity = direction.normalize().scale(Math.min(this.maxSpeed * 0.7, distance * 0.2));
               this.currentVelocity = this.currentVelocity.scale(0.88).add(desiredVelocity.subtract(this.currentVelocity).scale(0.15));
               this.skyCarrier.setDeltaMovement(this.currentVelocity);
               if (this.currentVelocity.lengthSqr() > 0.001) {
                  this.targetYaw = (float)(Math.atan2(this.currentVelocity.z, this.currentVelocity.x) * (180.0 / Math.PI) - 90.0);
                  float yawDifference = Mth.wrapDegrees(this.targetYaw - this.currentYaw);
                  this.currentYaw = Mth.wrapDegrees(this.currentYaw + Mth.clamp(yawDifference, -9.0F, 9.0F));
                  this.skyCarrier.setYRot(this.currentYaw);
                  this.skyCarrier.yBodyRot = this.currentYaw;
                  this.skyCarrier.yHeadRot = this.currentYaw;
               }
            }
         }
      }
   }
}
