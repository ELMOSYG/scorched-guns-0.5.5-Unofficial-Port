package top.ribs.scguns.entity.monster;


import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.Level.ExplosionInteraction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.ribs.scguns.Config;
import top.ribs.scguns.config.EntityEquipmentConfig;
import top.ribs.scguns.init.ModEntities;
import top.ribs.scguns.init.ModTags;

public class CogMinionEntity extends Monster {
   private static final EntityDataAccessor<Boolean> ATTACKING = SynchedEntityData.defineId(CogMinionEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> ATTACK_TIMEOUT = SynchedEntityData.defineId(CogMinionEntity.class, EntityDataSerializers.INT);
   public final AnimationState idleAnimationState = new AnimationState();
   public final AnimationState attackAnimationState = new AnimationState();

   public CogMinionEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
      this.setCanPickUpLoot(true);
   }

   private boolean isHoldingExplosiveBlock() {
      ItemStack mainHandItem = this.getMainHandItem();
      return mainHandItem.is(ModTags.Items.EXPLOSIVE_BLOCK);
   }

   private void explodeIfHoldingExplosive() {
      if (this.isHoldingExplosiveBlock() && !this.level().isClientSide) {
         this.level().explode(this, this.getX(), this.getY(), this.getZ(), 5.0F, false, ExplosionInteraction.MOB);
         this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
      }
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

   public static Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, 30.0)
         .add(Attributes.FOLLOW_RANGE, 24.0)
         .add(Attributes.MOVEMENT_SPEED, 0.25)
         .add(Attributes.ARMOR_TOUGHNESS, 0.5)
         .add(Attributes.ARMOR, 2.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
         .add(Attributes.ATTACK_KNOCKBACK, 0.8)
         .add(Attributes.ATTACK_DAMAGE, 4.0);
   }

   public void die(DamageSource source) {
      super.die(source);
      if (!this.level().isClientSide && source.getEntity() instanceof Player) {
         float spawnChance = ((Double)Config.COMMON.gameplay.cogBeaconSpawnChance.get()).floatValue();
         if (spawnChance > 0.0F && this.random.nextFloat() < spawnChance) {
            SignalBeaconEntity beacon = new SignalBeaconEntity((EntityType<? extends Mob>)ModEntities.SIGNAL_BEACON.get(), this.level());
            beacon.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
            this.level().addFreshEntity(beacon);
         }
      }
   }

   @Override
   public SpawnGroupData finalizeSpawn(
      ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData
   ) {
      super.finalizeSpawn(pLevel,  pDifficulty,  pReason,  pSpawnData);
      EntityEquipmentConfig.equipEntity(this, "scguns:cog_minion");
      return pSpawnData;
   }

   public void tick() {
      super.tick();
      if (!this.level().isClientSide() && this.isAttacking() && this.getAttackTimeout() > 0) {
         this.setAttackTimeout(this.getAttackTimeout() - 1);
         if (this.getAttackTimeout() == 6) {
            LivingEntity target = this.getTarget();
            if (target != null && this.distanceToSqr(target) <= (double)(this.getBbWidth() * 2.0F * this.getBbWidth() * 2.0F + target.getBbWidth())) {
               boolean didHurt = this.doHurtTarget(target);
               if (didHurt) {
                  this.explodeIfHoldingExplosive();
               }
            }
         }

         if (this.getAttackTimeout() <= 0) {
            this.setAttacking(false);
         }
      }

      if (this.level().isClientSide()) {
         this.setupAnimationStates();
      }
   }

   private void setupAnimationStates() {
      if (this.isAttacking()) {
         this.idleAnimationState.stop();
         this.attackAnimationState.startIfStopped(this.tickCount);
      } else {
         this.attackAnimationState.stop();
         this.idleAnimationState.startIfStopped(this.tickCount);
      }
   }

   public boolean wantsToPickUp(ItemStack pStack) {
      return this.canHoldItem(pStack);
   }

   public boolean canTakeItem(ItemStack stack) {
      EquipmentSlot slot = this.getEquipmentSlotForItem(stack);
      return !this.getItemBySlot(slot).isEmpty() ? false : slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.HEAD;
   }

   public boolean canReplaceCurrentItem(ItemStack candidate, ItemStack existing) {
      if (!existing.isEmpty()) {
         return false;
      } else {
         EquipmentSlot slot = this.getEquipmentSlotForItem(candidate);
         return slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.HEAD;
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

   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(ATTACKING, false);
      builder.define(ATTACK_TIMEOUT, 0);
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

   protected Vec3i getPickupReach() {
      return new Vec3i(3, 3, 3);
   }

   protected void pickUpItem(ItemEntity pItemEntity) {
      super.pickUpItem(pItemEntity);
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false) {
         @Override
         protected void checkAndPerformAttack(LivingEntity pEnemy) {
            double pDistToEnemySqr = this.mob.distanceToSqr(pEnemy);
            if (pDistToEnemySqr <= top.ribs.scguns.util.CombatHelper.attackReachSqr(this.mob, pEnemy) && this.getTicksUntilNextAttack() <= 0 && !CogMinionEntity.this.isAttacking()) {
               CogMinionEntity.this.setAttacking(true);
               this.resetAttackCooldown();
               this.mob.swing(InteractionHand.MAIN_HAND);
            }
         }

         protected void resetAttackCooldown() {
            this.adjustedTickDelay(25);
         }
      });
      this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this, new Class[0]).setAlertOthers(new Class[]{CogMinionEntity.class}));
      this.targetSelector.addGoal(2, new NearestAttackableTargetGoal(this, Player.class, true));
      this.goalSelector.addGoal(3, new MoveTowardsTargetGoal(this, 1.0, 30.0F));
      this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
      this.goalSelector.addGoal(4, new RandomStrollGoal(this, 1.0));
   }

   public boolean canHoldItem(ItemStack stack) {
      return true;
   }

   public HumanoidArm getMainArm() {
      return HumanoidArm.RIGHT;
   }

   @Nullable
   protected SoundEvent getAmbientSound() {
      return SoundEvents.IRON_GOLEM_STEP;
   }

   @Nullable
   protected SoundEvent getHurtSound(DamageSource pDamageSource) {
      return SoundEvents.IRON_GOLEM_HURT;
   }

   @Nullable
   protected SoundEvent getDeathSound() {
      return SoundEvents.IRON_GOLEM_DEATH;
   }
}
