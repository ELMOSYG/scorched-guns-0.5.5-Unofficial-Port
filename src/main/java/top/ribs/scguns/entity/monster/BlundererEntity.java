package top.ribs.scguns.entity.monster;

import java.util.EnumSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import top.ribs.scguns.util.MobType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RestrictSunGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.ribs.scguns.config.EntityEquipmentConfig;
import top.ribs.scguns.entity.ai.AIType;
import top.ribs.scguns.entity.ai.GunAttackGoal;
import top.ribs.scguns.entity.throwable.ThrowableGrenadeEntity;
import top.ribs.scguns.item.GunItem;

public class BlundererEntity extends Raider implements RangedAttackMob {
   private static final EntityDataAccessor<Boolean> ATTACKING = SynchedEntityData.defineId(BlundererEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> ATTACK_TIMEOUT = SynchedEntityData.defineId(BlundererEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> CHARGING = SynchedEntityData.defineId(BlundererEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> TOSSING_GRENADE = SynchedEntityData.defineId(BlundererEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> GRENADE_TOSS_TIMEOUT = SynchedEntityData.defineId(BlundererEntity.class, EntityDataSerializers.INT);

   @NotNull
   public MobType getMobType() {
      return MobType.ILLAGER;
   }

   public BlundererEntity(EntityType<? extends Raider> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
      this.setPathfindingMalus(PathType.WATER, -1.0F);
   }

   public static Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, 50.0)
         .add(Attributes.FOLLOW_RANGE, 24.0)
         .add(Attributes.MOVEMENT_SPEED, 0.3)
         .add(Attributes.ARMOR_TOUGHNESS, 2.0)
         .add(Attributes.ARMOR, 8.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
         .add(Attributes.ATTACK_KNOCKBACK, 1.0)
         .add(Attributes.ATTACK_DAMAGE, 5.0);
   }

   public boolean hurt(DamageSource source, float amount) {
      if (source.is(DamageTypeTags.IS_EXPLOSION)) {
         amount *= 0.15F;
      }

      return super.hurt(source, amount);
   }

   public HumanoidArm getMainArm() {
      return HumanoidArm.LEFT;
   }

   @Override
   public SpawnGroupData finalizeSpawn(
      ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData
   ) {
      EntityEquipmentConfig.equipEntity(this, "scguns:blunderer");
      this.setCanJoinRaid(this.getType() != EntityType.WITCH || pReason != MobSpawnType.NATURAL);
      return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData);
   }

   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(ATTACKING, false);
      builder.define(ATTACK_TIMEOUT, 0);
      builder.define(CHARGING, false);
      builder.define(TOSSING_GRENADE, false);
      builder.define(GRENADE_TOSS_TIMEOUT, 0);
   }

   public void tick() {
      super.tick();
      if (!this.level().isClientSide) {
         if (this.isAttacking() && this.getAttackTimeout() > 0) {
            this.setAttackTimeout(this.getAttackTimeout() - 1);
            if (this.getAttackTimeout() == 6) {
               LivingEntity target = this.getTarget();
               if (target != null && this.distanceToSqr(target) <= (double)(this.getBbWidth() * 2.0F * this.getBbWidth() * 2.0F + target.getBbWidth())) {
                  this.doHurtTarget(target);
               }
            }

            if (this.getAttackTimeout() <= 0) {
               this.setAttacking(false);
            }
         }

         if (this.isTossingGrenade() && this.getGrenadeTossTimeout() > 0) {
            this.setGrenadeTossTimeout(this.getGrenadeTossTimeout() - 1);
            if (this.getGrenadeTossTimeout() == 6) {
               this.throwGrenade();
            }

            if (this.getGrenadeTossTimeout() <= 0) {
               this.setTossingGrenade(false);
            }
         }
      }
   }

   protected void customServerAiStep() {
      super.customServerAiStep();
      ItemStack mainHandItem = this.getMainHandItem();
      boolean hasGun = mainHandItem.getItem() instanceof GunItem;
      if (hasGun && this.getTarget() != null) {
         this.getLookControl().setLookAt(this.getTarget(), 30.0F, 30.0F);
      }
   }

   public void applyRaidBuffs(ServerLevel pLevel, int pWave, boolean pUnusedFalse) {
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

   protected void registerGoals() {
      super.registerGoals();
      ItemStack mainHandItem = this.getMainHandItem();
      boolean hasGun = mainHandItem.getItem() instanceof GunItem;
      if (hasGun) {
         this.goalSelector.addGoal(1, new GunAttackGoal(this, mainHandItem, 1.0F, AIType.TACTICAL, 3));
      } else {
         this.goalSelector.addGoal(1, new BlundererEntity.StampedeChargeGoal(this, 1.0, 16.0, 100));
         this.goalSelector.addGoal(2, new BlundererEntity.GrenadeTossGoal(this, 12.0, 200));
         this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, false) {
            @Override
            protected void checkAndPerformAttack(LivingEntity pEnemy) {
               double pDistToEnemySqr = this.mob.distanceToSqr(pEnemy);
               if (pDistToEnemySqr <= top.ribs.scguns.util.CombatHelper.attackReachSqr(this.mob, pEnemy) && this.getTicksUntilNextAttack() <= 0 && !BlundererEntity.this.isAttacking()) {
                  BlundererEntity.this.setAttacking(true);
                  this.resetAttackCooldown();
                  this.mob.swing(InteractionHand.MAIN_HAND);
               }
            }
         });
      }

      this.goalSelector.addGoal(4, new FloatGoal(this));
      this.goalSelector.addGoal(5, new RestrictSunGoal(this));
      this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.9));
      this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this, new Class[]{Raider.class}).setAlertOthers(new Class[]{Raider.class}));
      this.targetSelector.addGoal(2, new NearestAttackableTargetGoal(this, Player.class, true));
      this.targetSelector.addGoal(3, new NearestAttackableTargetGoal(this, AbstractVillager.class, false));
      this.targetSelector.addGoal(4, new NearestAttackableTargetGoal(this, IronGolem.class, true));
      this.goalSelector.addGoal(5, new NearestAttackableTargetGoal(this, Turtle.class, 10, true, false, Turtle.BABY_ON_LAND_SELECTOR));
   }

   @Nullable
   protected SoundEvent getAmbientSound() {
      return SoundEvents.PILLAGER_AMBIENT;
   }

   public float getVoicePitch() {
      return 0.7F;
   }

   @Nullable
   protected SoundEvent getHurtSound(@NotNull DamageSource pDamageSource) {
      return SoundEvents.PILLAGER_HURT;
   }

   @Nullable
   protected SoundEvent getDeathSound() {
      return SoundEvents.PILLAGER_DEATH;
   }

   public SoundEvent getCelebrateSound() {
      return SoundEvents.PILLAGER_CELEBRATE;
   }

   public void performRangedAttack(@NotNull LivingEntity target, float distanceFactor) {
      this.doHurtTarget(target);
   }

   public void setAttacking(boolean attacking) {
      this.entityData.set(ATTACKING, attacking);
      if (attacking) {
         this.setAttackTimeout(12);
      }
   }

   public void setAttackTimeout(int timeout) {
      this.entityData.set(ATTACK_TIMEOUT, timeout);
   }

   public int getAttackTimeout() {
      return (Integer)this.entityData.get(ATTACK_TIMEOUT);
   }

   public boolean isAttacking() {
      return (Boolean)this.entityData.get(ATTACKING);
   }

   public void setCharging(boolean charging) {
      this.entityData.set(CHARGING, charging);
   }

   public boolean isCharging() {
      return (Boolean)this.entityData.get(CHARGING);
   }

   public void setTossingGrenade(boolean tossing) {
      this.entityData.set(TOSSING_GRENADE, tossing);
      if (tossing) {
         this.setGrenadeTossTimeout(12);
      }
   }

   public boolean isTossingGrenade() {
      return (Boolean)this.entityData.get(TOSSING_GRENADE);
   }

   public void setGrenadeTossTimeout(int timeout) {
      this.entityData.set(GRENADE_TOSS_TIMEOUT, timeout);
   }

   public int getGrenadeTossTimeout() {
      return (Integer)this.entityData.get(GRENADE_TOSS_TIMEOUT);
   }

   private void throwGrenade() {
      LivingEntity target = this.getTarget();
      if (target != null) {
         Vec3 startPos = this.position().add(0.0, (double)this.getEyeHeight() * 0.9, 0.0);
         Vec3 targetPos = target.position().add(0.0, 0.5, 0.0);
         double dx = targetPos.x - startPos.x;
         double dy = targetPos.y - startPos.y;
         double dz = targetPos.z - startPos.z;
         double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
         if (!(horizontalDistance < 4.0)) {
            double timeToTarget = Math.sqrt(horizontalDistance / 6.0);
            double velocityXZ = horizontalDistance / timeToTarget;
            double velocityY = (dy + 4.905 * timeToTarget * timeToTarget * 0.08) / timeToTarget;
            double motionX = dx / horizontalDistance * velocityXZ * 0.08;
            double motionY = velocityY * 0.08;
            double motionZ = dz / horizontalDistance * velocityXZ * 0.08;
            ThrowableGrenadeEntity grenade = new ThrowableGrenadeEntity(this.level(), this, 40);
            grenade.setPos(startPos.x + dx / horizontalDistance * 1.5, startPos.y, startPos.z + dz / horizontalDistance * 1.5);
            grenade.setDeltaMovement(motionX, motionY, motionZ);
            this.level().addFreshEntity(grenade);
            this.playSound(SoundEvents.SNOWBALL_THROW, 1.0F, 0.8F);
         }
      }
   }

   public boolean removeWhenFarAway(double pDistanceToClosestPlayer) {
      return this.getCurrentRaid() == null && super.removeWhenFarAway(pDistanceToClosestPlayer);
   }

   public boolean requiresCustomPersistence() {
      return super.requiresCustomPersistence() || this.getCurrentRaid() != null;
   }

   public static class GrenadeTossGoal extends Goal {
      private final BlundererEntity mob;
      private final double tossRange;
      private final int tossCooldown;
      private int cooldownTicks;
      private int retreatTicks;
      private LivingEntity target;
      private Vec3 retreatDirection;

      public GrenadeTossGoal(BlundererEntity mob, double tossRange, int tossCooldown) {
         super();
         this.mob = mob;
         this.tossRange = tossRange;
         this.tossCooldown = tossCooldown;
         this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      public boolean canUse() {
         if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return false;
         } else {
            this.target = this.mob.getTarget();
            if (this.target == null) {
               return false;
            } else {
               double distanceToTarget = this.mob.distanceToSqr(this.target);
               return distanceToTarget <= this.tossRange * this.tossRange
                  && distanceToTarget > 25.0
                  && this.mob.hasLineOfSight(this.target)
                  && (double)this.mob.random.nextFloat() < 0.15;
            }
         }
      }

      public boolean canContinueToUse() {
         return this.mob.isTossingGrenade() || this.retreatTicks > 0;
      }

      public void start() {
         this.mob.setTossingGrenade(true);
         this.mob.getLookControl().setLookAt(this.target, 30.0F, 30.0F);
         this.retreatTicks = 0;
      }

      public void tick() {
         if (this.mob.isTossingGrenade()) {
            if (this.target != null) {
               this.mob.getLookControl().setLookAt(this.target, 30.0F, 30.0F);
               this.mob.getNavigation().stop();
            }
         } else if (this.retreatTicks == 0) {
            this.retreatTicks = 40;
            if (this.target != null) {
               double dx = this.mob.getX() - this.target.getX();
               double dz = this.mob.getZ() - this.target.getZ();
               double distance = Math.sqrt(dx * dx + dz * dz);
               if (distance > 0.1) {
                  this.retreatDirection = new Vec3(dx / distance, 0.0, dz / distance);
               }
            }
         } else {
            this.retreatTicks--;
            if (this.retreatDirection != null) {
               Vec3 retreatPos = this.mob.position().add(this.retreatDirection.x * 6.0, 0.0, this.retreatDirection.z * 6.0);
               this.mob.getNavigation().moveTo(retreatPos.x, retreatPos.y, retreatPos.z, 1.2);
            }
         }
      }

      public void stop() {
         this.cooldownTicks = this.tossCooldown;
         this.target = null;
         this.retreatTicks = 0;
         this.retreatDirection = null;
      }
   }

   public static class StampedeChargeGoal extends Goal {
      private final BlundererEntity mob;
      private final double speedModifier;
      private final double chargeRange;
      private final int chargeCooldown;
      private int cooldownTicks;
      private int chargeTicks;
      private LivingEntity target;
      private Vec3 chargeDirection;
      private int trampleCounter;

      public StampedeChargeGoal(BlundererEntity mob, double speedModifier, double chargeRange, int chargeCooldown) {
         super();
         this.mob = mob;
         this.speedModifier = speedModifier;
         this.chargeRange = chargeRange;
         this.chargeCooldown = chargeCooldown;
         this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      public boolean canUse() {
         if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return false;
         } else {
            this.target = this.mob.getTarget();
            if (this.target == null) {
               return false;
            } else {
               double distanceToTarget = this.mob.distanceToSqr(this.target);
               return distanceToTarget <= this.chargeRange * this.chargeRange && distanceToTarget > 9.0;
            }
         }
      }

      public boolean canContinueToUse() {
         return this.target != null && this.target.isAlive() && this.chargeTicks > 0;
      }

      public void start() {
         this.chargeTicks = 4;
         this.trampleCounter = 0;
         this.mob.setCharging(true);
         double dx = this.target.getX() - this.mob.getX();
         double dz = this.target.getZ() - this.mob.getZ();
         double distance = Math.sqrt(dx * dx + dz * dz);
         if (distance > 0.1) {
            this.chargeDirection = new Vec3(dx / distance, 0.0, dz / distance);
         } else {
            this.chargeDirection = this.mob.getLookAngle();
         }

         this.mob.playSound(SoundEvents.VINDICATOR_CELEBRATE, 1.0F, 0.7F);
      }

      public void tick() {
         if (this.chargeTicks > 0) {
            this.chargeTicks--;
            double speedMultiplier = this.speedModifier * (1.0 + (double)(30 - this.chargeTicks) * 0.02);
            this.mob.setDeltaMovement(this.chargeDirection.x * speedMultiplier, this.mob.getDeltaMovement().y, this.chargeDirection.z * speedMultiplier);
            this.mob
               .getLookControl()
               .setLookAt(
                  this.mob.getX() + this.chargeDirection.x * 5.0, this.mob.getY(), this.mob.getZ() + this.chargeDirection.z * 5.0
               );
            if (this.chargeTicks % 3 == 0) {
               this.mob.playSound(SoundEvents.RAVAGER_STEP, 0.5F, 0.9F);
            }

            for (LivingEntity entity : this.mob.level().getEntitiesOfClass(LivingEntity.class, this.mob.getBoundingBox().inflate(0.5, 0.2, 0.5))) {
               if (entity != this.mob && entity.isAlive()) {
                  this.trampleEntity(entity);
               }
            }
         }
      }

      public void stop() {
         this.mob.setCharging(false);
         this.cooldownTicks = this.chargeCooldown;
         this.target = null;
         this.chargeTicks = 0;
         this.chargeDirection = null;
      }

      private void trampleEntity(LivingEntity entity) {
         float damage = (float)this.mob.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.8F;
         entity.hurt(this.mob.damageSources().mobAttack(this.mob), damage);
         double knockbackX = this.chargeDirection.x * 0.6;
         double knockbackZ = this.chargeDirection.z * 0.6;
         entity.setDeltaMovement(entity.getDeltaMovement().add(knockbackX, 0.25, knockbackZ));
         this.trampleCounter++;
         if (this.trampleCounter % 2 == 0) {
            this.mob.playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 1.2F);
         }
      }
   }
}
