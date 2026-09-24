package top.ribs.scguns.entity.monster;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.ribs.scguns.config.EntityEquipmentConfig;
import top.ribs.scguns.entity.ai.AIType;
import top.ribs.scguns.entity.ai.GunAttackGoal;
import top.ribs.scguns.item.GunItem;

public class FinforcerEntity extends Monster implements RangedAttackMob {
   private static final EntityDataAccessor<Boolean> ATTACKING = SynchedEntityData.defineId(FinforcerEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> ATTACK_TIMEOUT = SynchedEntityData.defineId(FinforcerEntity.class, EntityDataSerializers.INT);
   private static final double ALLIANCE_RANGE = 32.0;
   private static final int ALERT_RANGE_Y = 10;
   private int ticksUntilNextAlert = 0;

   public FinforcerEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
   }

   public static Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, 35.0)
         .add(Attributes.FOLLOW_RANGE, 28.0)
         .add(Attributes.MOVEMENT_SPEED, 0.26)
         .add(Attributes.ARMOR_TOUGHNESS, 0.5)
         .add(Attributes.ARMOR, 3.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 0.4F)
         .add(Attributes.ATTACK_KNOCKBACK, 0.3F)
         .add(Attributes.ATTACK_DAMAGE, 4.0);
   }

   public HumanoidArm getMainArm() {
      return HumanoidArm.LEFT;
   }

   @Override
   public SpawnGroupData finalizeSpawn(
      ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData
   ) {
      EntityEquipmentConfig.equipEntity(this, "scguns:finforcer");
      return super.finalizeSpawn(pLevel,  pDifficulty,  pReason,  pSpawnData);
   }

   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(ATTACKING, false);
      builder.define(ATTACK_TIMEOUT, 0);
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

         if (this.getTarget() != null) {
            this.maybeAlertAllies();
         }
      }
   }

   protected void registerGoals() {
      ItemStack mainHandItem = this.getMainHandItem();
      boolean hasGun = mainHandItem.getItem() instanceof GunItem;
      if (hasGun) {
         this.goalSelector.addGoal(1, new GunAttackGoal(this, mainHandItem, 1.0F, AIType.TACTICAL, 2));
      } else {
         this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false) {
            @Override
            protected void checkAndPerformAttack(LivingEntity pEnemy) {
               double pDistToEnemySqr = this.mob.distanceToSqr(pEnemy);
               if (pDistToEnemySqr <= top.ribs.scguns.util.CombatHelper.attackReachSqr(this.mob, pEnemy) && this.getTicksUntilNextAttack() <= 0 && !FinforcerEntity.this.isAttacking()) {
                  FinforcerEntity.this.setAttacking(true);
                  this.resetAttackCooldown();
                  this.mob.swing(InteractionHand.MAIN_HAND);
               }
            }

            protected double getAttackReachSqr(LivingEntity pEnemy) {
               return top.ribs.scguns.util.CombatHelper.attackReachSqr(this.mob, pEnemy) * 1.2;
            }
         });
      }

      this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.9));
      this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, (new HurtByTargetGoal(this) {
         public boolean canUse() {
            return this.mob.getLastHurtByMob() instanceof FinforcerEntity ? false : super.canUse();
         }
      }).setAlertOthers(new Class[0]));
      this.targetSelector.addGoal(2, new NearestAttackableTargetGoal(this, Player.class, true, target -> !(target instanceof Player player) || (!player.isCreative() && !player.isSpectator())));
   }

   public boolean canAttack(LivingEntity target) {
      return target instanceof FinforcerEntity ? false : super.canAttack(target);
   }

   private void maybeAlertAllies() {
      if (this.ticksUntilNextAlert > 0) {
         this.ticksUntilNextAlert--;
      } else {
         if (this.getSensing().hasLineOfSight(this.getTarget())) {
            this.alertAllies();
         }

         this.ticksUntilNextAlert = 20 + this.random.nextInt(20);
      }
   }

   private void alertAllies() {
      AABB alertArea = AABB.unitCubeFromLowerCorner(this.position()).inflate(32.0, 10.0, 32.0);
      this.level()
         .getEntitiesOfClass(FinforcerEntity.class, alertArea, EntitySelector.NO_SPECTATORS)
         .stream()
         .filter(entity -> entity != this)
         .filter(entity -> entity.getTarget() == null)
         .filter(entity -> !entity.isAlliedTo(this.getTarget()))
         .forEach(entity -> entity.setTarget(this.getTarget()));
   }

   public void setTarget(@Nullable LivingEntity target) {
      if (!(target instanceof FinforcerEntity)) {
         if (this.getTarget() == null && target != null) {
            this.ticksUntilNextAlert = this.random.nextInt(20);
         }

         super.setTarget(target);
      }
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

   protected void updateWalkAnimation(float pPartialTick) {
      float f;
      if (this.getPose() == Pose.STANDING) {
         f = Math.min(pPartialTick * 6.0F, 1.0F);
      } else {
         f = 0.0F;
      }

      this.walkAnimation.update(f, 0.2F);
   }

   protected SoundEvent getAmbientSound() {
      return SoundEvents.DOLPHIN_AMBIENT;
   }

   @NotNull
   protected SoundEvent getHurtSound(@NotNull DamageSource pDamageSource) {
      return SoundEvents.DOLPHIN_HURT;
   }

   @NotNull
   protected SoundEvent getDeathSound() {
      return SoundEvents.DOLPHIN_DEATH;
   }

   public void performRangedAttack(@NotNull LivingEntity target, float distanceFactor) {
      this.doHurtTarget(target);
   }

   public void addAdditionalSaveData(@NotNull CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putInt("AlertCooldown", this.ticksUntilNextAlert);
   }

   public void readAdditionalSaveData(@NotNull CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.ticksUntilNextAlert = tag.getInt("AlertCooldown");
   }



   public boolean isPushedByFluid() {
      return false;
   }
}
