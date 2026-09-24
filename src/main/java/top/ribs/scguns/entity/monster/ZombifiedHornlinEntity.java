package top.ribs.scguns.entity.monster;


import net.minecraft.resources.ResourceLocation;
import java.lang.reflect.Field;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import top.ribs.scguns.util.MobType;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.ribs.scguns.config.EntityEquipmentConfig;
import top.ribs.scguns.entity.ai.ConsumeGoldGoal;
import top.ribs.scguns.entity.ai.GoldSeekingGoal;
import top.ribs.scguns.entity.util.IGoldConsumingEntity;

public class ZombifiedHornlinEntity extends Monster implements RangedAttackMob, NeutralMob, IGoldConsumingEntity {
   private static final double ALLIANCE_RANGE = 26.0;
   private static final int ALERT_RANGE_Y = 10;
   private static final EntityDataAccessor<Boolean> DATA_EATING = SynchedEntityData.defineId(ZombifiedHornlinEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> DATA_PREPARING_EAT = SynchedEntityData.defineId(
      ZombifiedHornlinEntity.class, EntityDataSerializers.BOOLEAN
   );
   private static final EntityDataAccessor<ItemStack> DATA_HELD_FOOD = SynchedEntityData.defineId(
      ZombifiedHornlinEntity.class, EntityDataSerializers.ITEM_STACK
   );
   private int goldEatingCooldown = 0;
   private int slagProductionCooldown = 0;
   private ItemEntity targetGoldItem = null;
   private float accumulatedGoldValue = 0.0F;
   private static final ResourceLocation SPEED_MODIFIER_ATTACKING_UUID = ResourceLocation.fromNamespaceAndPath("scguns", "49455a49-7ec5-45ba-b886-3b90b23a1718");
   private static final AttributeModifier SPEED_MODIFIER_ATTACKING = new AttributeModifier(SPEED_MODIFIER_ATTACKING_UUID, 0.05, Operation.ADD_VALUE);
   private static final UniformInt FIRST_ANGER_SOUND_DELAY = TimeUtil.rangeOfSeconds(0, 1);
   private int playFirstAngerSoundIn;
   private static final UniformInt PERSISTENT_ANGER_TIME = TimeUtil.rangeOfSeconds(20, 39);
   private int remainingPersistentAngerTime;
   @Nullable
   private UUID persistentAngerTarget;
   private static final UniformInt ALERT_INTERVAL = TimeUtil.rangeOfSeconds(1, 2);
   private int ticksUntilNextAlert;

   public ZombifiedHornlinEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
   }

   @NotNull
   public MobType getMobType() {
      return MobType.UNDEAD;
   }

   public static Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, 30.0)
         .add(Attributes.FOLLOW_RANGE, 26.0)
         .add(Attributes.MOVEMENT_SPEED, 0.23)
         .add(Attributes.ARMOR_TOUGHNESS, 0.5)
         .add(Attributes.ARMOR, 2.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
         .add(Attributes.ATTACK_KNOCKBACK, 0.8F)
         .add(Attributes.ATTACK_DAMAGE, 3.0)
         .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0);
   }

   public HumanoidArm getMainArm() {
      return HumanoidArm.RIGHT;
   }

   @Override
   public SpawnGroupData finalizeSpawn(
      ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData
   ) {
      EntityEquipmentConfig.equipEntity(this, "zombified_hornlin");
      return super.finalizeSpawn(pLevel,  pDifficulty,  pReason,  pSpawnData);
   }

   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(DATA_EATING, false);
      builder.define(DATA_PREPARING_EAT, false);
      builder.define(DATA_HELD_FOOD, ItemStack.EMPTY);
   }

   public void tick() {
      super.tick();
      if (!this.level().isClientSide) {
         if (this.goldEatingCooldown > 0) {
            this.goldEatingCooldown--;
         }

         if (this.slagProductionCooldown > 0) {
            this.slagProductionCooldown--;
         }

         if (this.getTarget() != null) {
            this.maybeAlertAllies();
         }
      }
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, false));
      this.goalSelector.addGoal(2, new ConsumeGoldGoal(this, this));
      this.goalSelector.addGoal(3, new GoldSeekingGoal(this, this, 1.0, 16.0F));
      this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
      this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this, new Class[0]).setAlertOthers(new Class[0]));
      this.targetSelector
         .addGoal(
            2,
            new NearestAttackableTargetGoal<>(
               this, Player.class, 10, true, false, player -> this.isAngryAt(player) && !((Player)player).isCreative() && !((Player)player).isSpectator()
            )
         );
      this.targetSelector.addGoal(3, new ResetUniversalAngerTargetGoal(this, true));
   }

   private void maybeAlertAllies() {
      if (this.ticksUntilNextAlert > 0) {
         this.ticksUntilNextAlert--;
      } else {
         if (this.getSensing().hasLineOfSight(this.getTarget())) {
            this.alertAllies();
         }

         this.ticksUntilNextAlert = ALERT_INTERVAL.sample(this.random);
      }
   }

   private void alertAllies() {
      AABB alertArea = AABB.unitCubeFromLowerCorner(this.position()).inflate(26.0, 10.0, 26.0);
      this.level()
         .getEntitiesOfClass(ZombifiedHornlinEntity.class, alertArea, EntitySelector.NO_SPECTATORS)
         .stream()
         .filter(entity -> entity != this)
         .filter(entity -> entity.getTarget() == null)
         .filter(entity -> !entity.isAlliedTo(this.getTarget()))
         .forEach(entity -> {
            entity.setTarget(this.getTarget());
            entity.startPersistentAngerTimer();
            if (this.getTarget() instanceof Player) {
               entity.setPersistentAngerTarget(this.getTarget().getUUID());
            }
         });
      this.level()
         .getEntitiesOfClass(ZombifiedPiglin.class, alertArea, EntitySelector.NO_SPECTATORS)
         .stream()
         .filter(entity -> entity.getTarget() == null)
         .filter(entity -> !entity.isAlliedTo(this.getTarget()))
         .forEach(entity -> {
            if (this.getTarget() instanceof Player player) {
               entity.setLastHurtByPlayer(player);
               entity.startPersistentAngerTimer();
               entity.setPersistentAngerTarget(player.getUUID());
               entity.setTarget(this.getTarget());

               try {
                  Field ticksUntilNextAlertField = entity.getClass().getDeclaredField("ticksUntilNextAlert");
                  ticksUntilNextAlertField.setAccessible(true);
                  ticksUntilNextAlertField.setInt(entity, 0);
                  Field playFirstAngerSoundInField = entity.getClass().getDeclaredField("playFirstAngerSoundIn");
                  playFirstAngerSoundInField.setAccessible(true);
                  playFirstAngerSoundInField.setInt(entity, 0);
               } catch (Exception var5) {
               }
            }
         });
   }

   public void setTarget(@Nullable LivingEntity target) {
      if (this.getTarget() == null && target != null) {
         this.playFirstAngerSoundIn = FIRST_ANGER_SOUND_DELAY.sample(this.random);
         this.ticksUntilNextAlert = ALERT_INTERVAL.sample(this.random);
      }

      if (target instanceof Player) {
         this.setLastHurtByPlayer((Player)target);
      }

      super.setTarget(target);
   }

   protected void customServerAiStep() {
      AttributeInstance attributeinstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
      if (this.isAngry()) {
         if (!this.isBaby() && !attributeinstance.hasModifier(SPEED_MODIFIER_ATTACKING_UUID)) {
            attributeinstance.addTransientModifier(SPEED_MODIFIER_ATTACKING);
         }

         this.maybePlayFirstAngerSound();
      } else if (attributeinstance.hasModifier(SPEED_MODIFIER_ATTACKING_UUID)) {
         attributeinstance.removeModifier(SPEED_MODIFIER_ATTACKING);
      }

      this.updatePersistentAnger((ServerLevel)this.level(), true);
      if (this.isAngry()) {
         this.lastHurtByPlayerTime = this.tickCount;
      }

      if (this.getTarget() == null && this.isAngry() && this.getPersistentAngerTarget() != null) {
         Player persistentTarget = this.level().getPlayerByUUID(this.getPersistentAngerTarget());
         if (persistentTarget != null && this.distanceToSqr(persistentTarget) <= this.getAttributeValue(Attributes.FOLLOW_RANGE) * this.getAttributeValue(Attributes.FOLLOW_RANGE)) {
            this.setTarget(persistentTarget);
         }
      }

      super.customServerAiStep();
   }

   private void maybePlayFirstAngerSound() {
      if (this.playFirstAngerSoundIn > 0) {
         this.playFirstAngerSoundIn--;
         if (this.playFirstAngerSoundIn == 0) {
            this.playAngerSound();
         }
      }
   }

   private void playAngerSound() {
      this.playSound(SoundEvents.ZOMBIFIED_PIGLIN_ANGRY, this.getSoundVolume() * 2.0F, this.getVoicePitch() * 1.8F);
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

   @Nullable
   protected SoundEvent getAmbientSound() {
      return this.isAngry() ? SoundEvents.ZOMBIFIED_PIGLIN_ANGRY : SoundEvents.ZOMBIFIED_PIGLIN_AMBIENT;
   }

   @Nullable
   protected SoundEvent getHurtSound(@NotNull DamageSource pDamageSource) {
      return SoundEvents.ZOMBIFIED_PIGLIN_HURT;
   }

   @Nullable
   protected SoundEvent getDeathSound() {
      return SoundEvents.ZOMBIFIED_PIGLIN_DEATH;
   }

   protected float getSoundVolume() {
      return 0.4F;
   }

   public float getVoicePitch() {
      return (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 0.7F;
   }

   public void performRangedAttack(@NotNull LivingEntity target, float distanceFactor) {
      this.doHurtTarget(target);
   }

   @Override
   public boolean isEatingGold() {
      return (Boolean)this.entityData.get(DATA_EATING);
   }

   public void setEatingGold(boolean eating) {
      this.entityData.set(DATA_EATING, eating);
   }

   @Override
   public boolean isPreparingToEat() {
      return (Boolean)this.entityData.get(DATA_PREPARING_EAT);
   }

   public void setPreparingToEat(boolean preparing) {
      this.entityData.set(DATA_PREPARING_EAT, preparing);
   }

   @Override
   public ItemStack getHeldFoodItem() {
      return (ItemStack)this.entityData.get(DATA_HELD_FOOD);
   }

   public void setHeldFoodItem(ItemStack stack) {
      this.entityData.set(DATA_HELD_FOOD, stack);
   }

   @Override
   public ItemEntity getTargetGoldItem() {
      return this.targetGoldItem;
   }

   @Override
   public void setTargetGoldItem(ItemEntity item) {
      this.targetGoldItem = item;
   }

   @Override
   public float getAccumulatedGoldValue() {
      return this.accumulatedGoldValue;
   }

   @Override
   public void setAccumulatedGoldValue(float value) {
      this.accumulatedGoldValue = value;
   }

   @Override
   public void addAccumulatedGoldValue(float value) {
      this.accumulatedGoldValue += value;
   }

   @Override
   public int getGoldEatingCooldown() {
      return this.goldEatingCooldown;
   }

   @Override
   public void setGoldEatingCooldown(int ticks) {
      this.goldEatingCooldown = ticks;
   }

   @Override
   public int getSlagProductionCooldown() {
      return this.slagProductionCooldown;
   }

   @Override
   public void setSlagProductionCooldown(int ticks) {
      this.slagProductionCooldown = ticks;
   }

   public void startPersistentAngerTimer() {
      this.setRemainingPersistentAngerTime(PERSISTENT_ANGER_TIME.sample(this.random));
   }

   public void setRemainingPersistentAngerTime(int time) {
      this.remainingPersistentAngerTime = time;
   }

   public int getRemainingPersistentAngerTime() {
      return this.remainingPersistentAngerTime;
   }

   public void setPersistentAngerTarget(@Nullable UUID target) {
      this.persistentAngerTarget = target;
   }

   @Nullable
   public UUID getPersistentAngerTarget() {
      return this.persistentAngerTarget;
   }

   public void addAdditionalSaveData(@NotNull CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      this.addPersistentAngerSaveData(tag);
      tag.putFloat("AccumulatedGoldValue", this.accumulatedGoldValue);
      tag.putInt("SlagProductionCooldown", this.slagProductionCooldown);
      tag.putInt("GoldEatingCooldown", this.goldEatingCooldown);
   }

   public void readAdditionalSaveData(@NotNull CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.readPersistentAngerSaveData(this.level(), tag);
      this.accumulatedGoldValue = tag.getFloat("AccumulatedGoldValue");
      this.slagProductionCooldown = tag.getInt("SlagProductionCooldown");
      this.goldEatingCooldown = tag.getInt("GoldEatingCooldown");
   }

   public boolean isPreventingPlayerRest(Player player) {
      return this.isAngryAt(player);
   }
}
