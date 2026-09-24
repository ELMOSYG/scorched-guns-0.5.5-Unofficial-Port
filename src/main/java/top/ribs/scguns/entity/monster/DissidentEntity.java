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
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import top.ribs.scguns.util.MobType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.ribs.scguns.init.ModSounds;

public class DissidentEntity extends Monster {
   private static final EntityDataAccessor<Boolean> ATTACKING = SynchedEntityData.defineId(DissidentEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> LEAPING = SynchedEntityData.defineId(DissidentEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> LANDING = SynchedEntityData.defineId(DissidentEntity.class, EntityDataSerializers.BOOLEAN);
   public final AnimationState idleAnimationState = new AnimationState();
   public final AnimationState attackAnimationState = new AnimationState();
   public int attackAnimationTimeout = 0;
   public int landingAnimationTimeout = 0;
   private int idleAnimationTimeout = 0;
   private boolean wasInAir = false;

   public DissidentEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
      this.xpReward = 10;
   }

   public static Builder createAttributes() {
      return Animal.createLivingAttributes()
         .add(Attributes.MAX_HEALTH, 40.0)
         .add(Attributes.FOLLOW_RANGE, 24.0)
         .add(Attributes.MOVEMENT_SPEED, 0.31)
         .add(Attributes.ARMOR_TOUGHNESS, 0.5)
         .add(Attributes.ARMOR, 2.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
         .add(Attributes.ATTACK_KNOCKBACK, 0.8F)
         .add(Attributes.ATTACK_DAMAGE, 5.0);
   }

   @NotNull
   public MobType getMobType() {
      return MobType.UNDEAD;
   }

   public void tick() {
      super.tick();
      if (!this.level().isClientSide()) {
         if (this.wasInAir && this.onGround() && this.getDeltaMovement().y <= 0.0) {
            boolean wasLeaping = this.isLeaping();
            this.setLeaping(false);
            this.setLanding(true);
            this.landingAnimationTimeout = 4;
            if (wasLeaping) {
               this.level().broadcastEntityEvent(this, (byte)6);
            }
         }

         this.wasInAir = !this.onGround();
      }

      if (this.level().isClientSide()) {
         this.setupAnimationStates();
      }

      if (this.landingAnimationTimeout > 0) {
         this.landingAnimationTimeout--;
         if (this.landingAnimationTimeout <= 0) {
            this.setLanding(false);
         }
      }
   }

   private void setupAnimationStates() {
      if (this.idleAnimationTimeout <= 0) {
         this.idleAnimationTimeout = this.random.nextInt(40) + 80;
         this.idleAnimationState.start(this.tickCount);
      } else {
         this.idleAnimationTimeout--;
      }

      if (this.isAttacking()) {
         if (this.attackAnimationTimeout <= 0) {
            this.attackAnimationTimeout = 12;
            this.attackAnimationState.start(this.tickCount);
         }

         this.attackAnimationTimeout--;
      } else {
         this.attackAnimationState.stop();
      }
   }

   public double getPassengersRidingOffset() {
      return (double)this.getBbHeight() * 1.05;
   }

   @Nullable
   public LivingEntity getControllingPassenger() {
      Entity entity = this.getFirstPassenger();
      return entity instanceof Mob ? (Mob)entity : null;
   }

   protected void updateControlFlags() {
      boolean flag = !(this.getControllingPassenger() instanceof Mob);
      boolean flag1 = !(this.getVehicle() instanceof Boat);
      this.goalSelector.setControlFlag(Flag.MOVE, flag);
      this.goalSelector.setControlFlag(Flag.JUMP, flag && flag1);
      this.goalSelector.setControlFlag(Flag.LOOK, flag);
   }

   @Nullable
   @Override
   public SpawnGroupData finalizeSpawn(
      ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData
   ) {
      pSpawnData = super.finalizeSpawn(pLevel,  pDifficulty,  pReason,  pSpawnData);
      if (pLevel.getRandom().nextFloat() < 0.25F) {
         Zombie babyZombie = (Zombie)EntityType.ZOMBIE.create(pLevel.getLevel());
         if (babyZombie != null) {
            babyZombie.setBaby(true);
            babyZombie.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
            babyZombie.finalizeSpawn(pLevel, pDifficulty, pReason, null);
            if (pLevel.getRandom().nextFloat() < 0.5F) {
               babyZombie.addTag("MobGunner");
               babyZombie.addTag("ProgressionGunner");
            }

            babyZombie.startRiding(this);
            pLevel.addFreshEntity(babyZombie);
         }
      }

      return pSpawnData;
   }

   public void setAttacking(boolean attacking) {
      this.entityData.set(ATTACKING, attacking);
   }

   public boolean isAttacking() {
      return (Boolean)this.entityData.get(ATTACKING);
   }

   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(ATTACKING, false);
      builder.define(LEAPING, false);
      builder.define(LANDING, false);
   }

   protected void updateWalkAnimation(float pPartialTick) {
      float f;
      if (this.getPose() == Pose.STANDING) {
         f = Math.min(pPartialTick * 6.0F, 1.0F);
      } else {
         f = 0.0F;
      }

      this.walkAnimation.update(f, 0.2F);
   }

   public void setLeaping(boolean leaping) {
      this.entityData.set(LEAPING, leaping);
   }

   public boolean isLeaping() {
      return (Boolean)this.entityData.get(LEAPING);
   }

   public void setLanding(boolean landing) {
      this.entityData.set(LANDING, landing);
   }

   public boolean isLanding() {
      return (Boolean)this.entityData.get(LANDING);
   }

   protected void pickUpItem(ItemEntity pItemEntity) {
      super.pickUpItem(pItemEntity);
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(1, new DissidentEntity.LeapAttackGoal(this, 1.0, 10.0, 80));
      this.goalSelector.addGoal(2, new DissidentEntity.DissidentAttackGoal(this, 1.2, true));
      this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this, new Class[0]).setAlertOthers(new Class[]{DissidentEntity.class}));
      this.targetSelector.addGoal(2, new NearestAttackableTargetGoal(this, Player.class, true));
      this.goalSelector.addGoal(3, new MoveTowardsTargetGoal(this, 1.0, 30.0F));
      this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
      this.goalSelector.addGoal(4, new RandomStrollGoal(this, 1.0));
   }

   public void handleEntityEvent(byte pId) {
      if (pId == 5) {
         if (this.level().isClientSide) {
            for (int i = 0; i < 10; i++) {
               this.level()
                  .addParticle(
                     ParticleTypes.POOF,
                     this.getX() + (this.random.nextDouble() - 0.5) * (double)this.getBbWidth() * 2.0,
                     this.getY(),
                     this.getZ() + (this.random.nextDouble() - 0.5) * (double)this.getBbWidth() * 2.0,
                     (this.random.nextDouble() - 0.5) * 0.2,
                     this.random.nextDouble() * 0.1,
                     (this.random.nextDouble() - 0.5) * 0.2
                  );
            }
         }
      } else if (pId == 6) {
         if (this.level().isClientSide) {
            for (int i = 0; i < 30; i++) {
               double offsetX = (this.random.nextDouble() - 0.5) * (double)this.getBbWidth() * 2.5;
               double offsetZ = (this.random.nextDouble() - 0.5) * (double)this.getBbWidth() * 2.5;
               this.level()
                  .addParticle(
                     ParticleTypes.POOF,
                     this.getX() + offsetX,
                     this.getY() + 0.1,
                     this.getZ() + offsetZ,
                     (this.random.nextDouble() - 0.5) * 0.3,
                     this.random.nextDouble() * 0.2,
                     (this.random.nextDouble() - 0.5) * 0.3
                  );
            }
         }
      } else {
         super.handleEntityEvent(pId);
      }
   }

   public void aiStep() {
      super.aiStep();
      if (this.isLeaping() && !this.onGround() && this.getDeltaMovement().y < 0.0) {
         this.setDeltaMovement(this.getDeltaMovement().multiply(1.0, 0.8, 1.0));
      }
   }

   public boolean causeFallDamage(float pFallDistance, float pMultiplier, DamageSource pSource) {
      return this.isLeaping() ? false : super.causeFallDamage(pFallDistance, pMultiplier, pSource);
   }

   @Nullable
   protected SoundEvent getAmbientSound() {
      return (SoundEvent)ModSounds.DISSIDENT_IDLE.get();
   }

   @Nullable
   protected SoundEvent getHurtSound(DamageSource pDamageSource) {
      return (SoundEvent)ModSounds.DISSIDENT_HURT.get();
   }

   @Nullable
   protected SoundEvent getDeathSound() {
      return (SoundEvent)ModSounds.DISSIDENT_DIE.get();
   }

   public float getAttackSoundVolume() {
      return 1.0F;
   }

   public static class DissidentAttackGoal extends MeleeAttackGoal {
      private final DissidentEntity entity;
      private int attackDelay = 10;
      private int ticksUntilNextAttack = 10;
      private boolean shouldCountTillNextAttack = false;

      public DissidentAttackGoal(PathfinderMob pMob, double pSpeedModifier, boolean pFollowingTargetEvenIfNotSeen) {
         super(pMob, pSpeedModifier, pFollowingTargetEvenIfNotSeen);
         this.entity = (DissidentEntity)pMob;
      }

      public void start() {
         super.start();
         this.attackDelay = 10;
         this.ticksUntilNextAttack = 10;
      }

      @Override
      protected void checkAndPerformAttack(LivingEntity pEnemy) {
         double pDistToEnemySqr = this.mob.distanceToSqr(pEnemy);
         if (this.isEnemyWithinAttackDistance(pEnemy, pDistToEnemySqr)) {
            this.shouldCountTillNextAttack = true;
            if (this.isTimeToStartAttackAnimation()) {
               this.entity.setAttacking(true);
            }

            if (this.isTimeToAttack()) {
               this.mob.getLookControl().setLookAt(pEnemy.getX(), pEnemy.getEyeY(), pEnemy.getZ());
               this.performAttack(pEnemy);
            }
         } else {
            this.resetAttackCooldown();
            this.shouldCountTillNextAttack = false;
            this.entity.setAttacking(false);
            this.entity.attackAnimationTimeout = 0;
         }
      }

      private boolean isEnemyWithinAttackDistance(LivingEntity pEnemy, double pDistToEnemySqr) {
         double adjustedAttackDistance = top.ribs.scguns.util.CombatHelper.attackReachSqr(this.mob, pEnemy) * 1.1;
         return pDistToEnemySqr <= adjustedAttackDistance;
      }

      protected void resetAttackCooldown() {
         this.ticksUntilNextAttack = this.adjustedTickDelay(this.attackDelay * 2);
      }

      protected boolean isTimeToAttack() {
         return this.ticksUntilNextAttack <= 0;
      }

      protected boolean isTimeToStartAttackAnimation() {
         return this.ticksUntilNextAttack <= this.attackDelay;
      }

      protected int getTicksUntilNextAttack() {
         return this.ticksUntilNextAttack;
      }

      protected void performAttack(LivingEntity pEnemy) {
         this.resetAttackCooldown();
         this.mob.swing(InteractionHand.MAIN_HAND);
         this.mob.doHurtTarget(pEnemy);
         if (this.mob instanceof DissidentEntity dissident) {
            this.mob
               .level()
               .playSound(
                  null,
                  dissident.getX(),
                  dissident.getY(),
                  dissident.getZ(),
                  SoundEvents.HOGLIN_ATTACK,
                  SoundSource.HOSTILE,
                  dissident.getAttackSoundVolume(),
                  1.0F
               );
         }
      }

      public void tick() {
         super.tick();
         LivingEntity target = this.mob.getTarget();
         if (target != null) {
            double distanceToTarget = this.mob.distanceToSqr(target.getX(), target.getY(), target.getZ());
            if (this.shouldCountTillNextAttack) {
               this.ticksUntilNextAttack = Math.max(this.ticksUntilNextAttack - 1, 0);
            }

            if (!this.isEnemyWithinAttackDistance(target, distanceToTarget)) {
               this.mob.getNavigation().moveTo(target, 1.2);
               this.resetAttackCooldown();
               this.shouldCountTillNextAttack = false;
               this.entity.setAttacking(false);
               this.entity.attackAnimationTimeout = 0;
            }
         }
      }

      public void stop() {
         this.entity.setAttacking(false);
         super.stop();
      }
   }

   public static class LeapAttackGoal extends Goal {
      private final DissidentEntity mob;
      private final double leapStrength;
      private final double maxLeapDistance;
      private final int leapCooldown;
      private int cooldownTicks;
      private LivingEntity target;
      private boolean isLeaping;
      private int leapTicks;

      public LeapAttackGoal(DissidentEntity mob, double leapStrength, double maxLeapDistance, int leapCooldown) {
         super();
         this.mob = mob;
         this.leapStrength = leapStrength;
         this.maxLeapDistance = maxLeapDistance;
         this.leapCooldown = leapCooldown;
         this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
      }

      public boolean canUse() {
         if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return false;
         } else {
            this.target = this.mob.getTarget();
            if (this.target != null && this.target.isAlive()) {
               double distanceToTarget = this.mob.distanceToSqr(this.target);
               return distanceToTarget >= 16.0
                  && distanceToTarget <= this.maxLeapDistance * this.maxLeapDistance
                  && this.mob.onGround()
                  && this.mob.hasLineOfSight(this.target);
            } else {
               return false;
            }
         }
      }

      public boolean canContinueToUse() {
         return this.isLeaping && this.leapTicks > 0 && this.target != null && this.target.isAlive();
      }

      public void start() {
         this.isLeaping = true;
         this.leapTicks = 15;
         this.mob.setLeaping(true);
         double dx = this.target.getX() - this.mob.getX();
         double dy = this.target.getY() - this.mob.getY();
         double dz = this.target.getZ() - this.mob.getZ();
         double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
         if (horizontalDistance > 0.1) {
            dx /= horizontalDistance;
            dz /= horizontalDistance;
            double distanceRatio = Math.min(horizontalDistance / this.maxLeapDistance, 1.0);
            double horizontalVelocity = this.leapStrength * 1.8 * (0.7 + distanceRatio * 0.3);
            double verticalVelocity = 0.4 + distanceRatio * 0.3;
            if (dy > 0.0) {
               verticalVelocity += Math.min(dy * 0.3, 0.5);
            }

            this.mob.setDeltaMovement(dx * horizontalVelocity, verticalVelocity, dz * horizontalVelocity);
            this.mob.level().broadcastEntityEvent(this.mob, (byte)5);
            this.mob.playSound(SoundEvents.PARROT_IMITATE_GHAST, 1.2F, 0.8F);
         }
      }

      public void tick() {
         if (this.target != null && this.isLeaping) {
            this.leapTicks--;
            this.mob.getLookControl().setLookAt(this.target, 30.0F, 30.0F);
            if (this.mob.distanceToSqr(this.target) <= 4.5) {
               this.performLeapAttack();
            }

            if (this.mob.onGround() && this.mob.getDeltaMovement().y <= 0.1) {
               this.leapTicks = Math.min(this.leapTicks, 3);
               if (this.mob.distanceToSqr(this.target) <= 9.0) {
                  this.performLeapAttack();
               }
            }
         }
      }

      public void stop() {
         this.isLeaping = false;
         this.mob.setLeaping(false);
         this.cooldownTicks = this.leapCooldown;
         this.target = null;
         this.leapTicks = 0;
      }

      private void performLeapAttack() {
         if (this.target != null && this.mob.distanceToSqr(this.target) <= 12.0) {
            float leapDamage = (float)this.mob.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.3F;
            this.target.hurt(this.mob.damageSources().mobAttack(this.mob), leapDamage);
            this.target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            double knockbackStrength = 0.8;
            double dx = this.target.getX() - this.mob.getX();
            double dz = this.target.getZ() - this.mob.getZ();
            double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
            if (horizontalDistance > 0.1) {
               this.target
                  .setDeltaMovement(this.target.getDeltaMovement().add(dx / horizontalDistance * knockbackStrength, 0.4, dz / horizontalDistance * knockbackStrength));
            }

            this.mob.playSound(SoundEvents.RABBIT_HURT, 1.0F, 1.2F);
            this.mob.setAttacking(true);
         }

         this.leapTicks = 0;
      }
   }
}
