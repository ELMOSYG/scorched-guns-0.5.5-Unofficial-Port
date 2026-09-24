package top.ribs.scguns.entity.monster;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
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
import top.ribs.scguns.init.ModEntities;
import top.ribs.scguns.init.ModTags;

public class HornlinEntity extends Monster implements RangedAttackMob, IGoldConsumingEntity {
   private static final double ALLIANCE_RANGE = 26.0;
   private static final int ALERT_RANGE_Y = 10;
   private static final int CONVERSION_TIME = 200;
   private int ticksUntilNextAlert = 0;
   private static final EntityDataAccessor<Boolean> DATA_EATING = SynchedEntityData.defineId(HornlinEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> DATA_PREPARING_EAT = SynchedEntityData.defineId(HornlinEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<ItemStack> DATA_HELD_FOOD = SynchedEntityData.defineId(HornlinEntity.class, EntityDataSerializers.ITEM_STACK);
   private int goldEatingCooldown = 0;
   private int slagProductionCooldown = 0;
   private ItemEntity targetGoldItem = null;
   private float accumulatedGoldValue = 0.0F;
   private int timeInOverworld = 0;

   public HornlinEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
   }

   public static Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, 30.0)
         .add(Attributes.FOLLOW_RANGE, 26.0)
         .add(Attributes.MOVEMENT_SPEED, 0.24)
         .add(Attributes.ARMOR_TOUGHNESS, 0.5)
         .add(Attributes.ARMOR, 2.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
         .add(Attributes.ATTACK_KNOCKBACK, 0.8F)
         .add(Attributes.ATTACK_DAMAGE, 2.0);
   }

   public HumanoidArm getMainArm() {
      return HumanoidArm.RIGHT;
   }

   @Override
   public SpawnGroupData finalizeSpawn(
      ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData
   ) {
      EntityEquipmentConfig.equipEntity(this, "hornlin");
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

         this.checkForConversion();
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
               this, Player.class, true, player -> this.isWearingGold((Player)player) && !((Player)player).isCreative() && !((Player)player).isSpectator()
            )
         );
      this.targetSelector.addGoal(4, new NearestAttackableTargetGoal(this, WitherBoss.class, true));
      this.targetSelector.addGoal(4, new NearestAttackableTargetGoal(this, WitherSkeleton.class, true));
   }

   public boolean canAttack(LivingEntity target) {
      return target instanceof HornlinEntity ? false : super.canAttack(target);
   }

   private boolean isWearingGold(Player player) {
      for (ItemStack itemStack : player.getArmorSlots()) {
         if (itemStack.getItem() instanceof ArmorItem armorItem && armorItem.getMaterial() == ArmorMaterials.GOLD) {
            return false;
         }
      }

      return true;
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
      AABB alertArea = AABB.unitCubeFromLowerCorner(this.position()).inflate(26.0, 10.0, 26.0);
      this.level()
         .getEntitiesOfClass(HornlinEntity.class, alertArea, EntitySelector.NO_SPECTATORS)
         .stream()
         .filter(entity -> entity != this)
         .filter(entity -> entity.getTarget() == null)
         .filter(entity -> !entity.isAlliedTo(this.getTarget()))
         .forEach(entity -> entity.setTarget(this.getTarget()));
      this.level()
         .getEntitiesOfClass(Piglin.class, alertArea, EntitySelector.NO_SPECTATORS)
         .stream()
         .filter(entity -> entity.getTarget() == null)
         .filter(entity -> !entity.isAlliedTo(this.getTarget()))
         .forEach(entity -> {
            if (this.getTarget() instanceof Player player) {
               try {
                  Brain<Piglin> brain = entity.getBrain();
                  brain.setMemory(MemoryModuleType.ANGRY_AT, player.getUUID());
                  brain.setMemory(MemoryModuleType.ATTACK_TARGET, player);
                  entity.setTarget(player);
               } catch (Exception var4) {
                  entity.setTarget(player);
               }
            } else {
               entity.setTarget(this.getTarget());
            }
         });
   }

   public void setTarget(@Nullable LivingEntity target) {
      if (this.getTarget() == null && target != null) {
         this.ticksUntilNextAlert = this.random.nextInt(20);
      }

      super.setTarget(target);
   }

   private void checkForConversion() {
      if (this.level().dimension() == Level.OVERWORLD && !this.isConverting()) {
         this.timeInOverworld = 0;
      }

      if (this.isConverting()) {
         this.timeInOverworld++;
         if (this.timeInOverworld > 200) {
            this.convertToZombifiedHornlin();
         }
      }
   }

   public boolean isConverting() {
      if (this.level().dimension() == Level.OVERWORLD && !this.isNoAi()) {
         ItemStack helmet = this.getItemBySlot(EquipmentSlot.HEAD);
         return !helmet.is(ModTags.Items.GAS_MASK);
      } else {
         return false;
      }
   }

   private void convertToZombifiedHornlin() {
      ZombifiedHornlinEntity zombifiedHornlin = (ZombifiedHornlinEntity)this.convertTo((EntityType)ModEntities.ZOMBIFIED_HORNLIN.get(), true);
      if (zombifiedHornlin != null) {
         zombifiedHornlin.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
         this.playConvertedSound();
      }
   }

   protected void playConvertedSound() {
      this.playSound(SoundEvents.PIGLIN_CONVERTED_TO_ZOMBIFIED, 1.0F, 1.0F);
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
      return SoundEvents.PIGLIN_BRUTE_AMBIENT;
   }

   @NotNull
   protected SoundEvent getHurtSound(@NotNull DamageSource pDamageSource) {
      return SoundEvents.PIGLIN_BRUTE_HURT;
   }

   @NotNull
   protected SoundEvent getDeathSound() {
      return SoundEvents.PIGLIN_BRUTE_DEATH;
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

   public void addAdditionalSaveData(@NotNull CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putFloat("AccumulatedGoldValue", this.accumulatedGoldValue);
      tag.putInt("SlagProductionCooldown", this.slagProductionCooldown);
      tag.putInt("TimeInOverworld", this.timeInOverworld);
      tag.putInt("GoldEatingCooldown", this.goldEatingCooldown);
   }

   public void readAdditionalSaveData(@NotNull CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.accumulatedGoldValue = tag.getFloat("AccumulatedGoldValue");
      this.slagProductionCooldown = tag.getInt("SlagProductionCooldown");
      this.timeInOverworld = tag.getInt("TimeInOverworld");
      this.goldEatingCooldown = tag.getInt("GoldEatingCooldown");
   }
}
