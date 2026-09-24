package top.ribs.scguns.entity.monster;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
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
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.ribs.scguns.config.EntityEquipmentConfig;
import top.ribs.scguns.entity.ai.AIType;
import top.ribs.scguns.entity.ai.GunAttackGoal;
import top.ribs.scguns.init.ModTags;
import top.ribs.scguns.item.GunItem;

public class SubjugatorEntity extends Monster implements RangedAttackMob {
   private static final EntityDataAccessor<Boolean> ATTACKING = SynchedEntityData.defineId(SubjugatorEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> ATTACK_TIMEOUT = SynchedEntityData.defineId(SubjugatorEntity.class, EntityDataSerializers.INT);
   private static final double ALLIANCE_RANGE = 32.0;
   private static final int ALERT_RANGE_Y = 10;
   private int ticksUntilNextAlert = 0;
   private int warpCooldown = 0;
   private int lastWarpTime = 0;

   public SubjugatorEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
   }

   public static Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, 45.0)
         .add(Attributes.FOLLOW_RANGE, 32.0)
         .add(Attributes.MOVEMENT_SPEED, 0.3)
         .add(Attributes.ARMOR_TOUGHNESS, 1.5)
         .add(Attributes.ARMOR, 5.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 0.7F)
         .add(Attributes.ATTACK_KNOCKBACK, 0.6F)
         .add(Attributes.ATTACK_DAMAGE, 5.0);
   }

   public HumanoidArm getMainArm() {
      return HumanoidArm.RIGHT;
   }

   @Override
   public SpawnGroupData finalizeSpawn(
      ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData
   ) {
      EntityEquipmentConfig.equipEntity(this, "scguns:subjugator");
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
         if (this.warpCooldown > 0) {
            this.warpCooldown--;
         }

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
            if (this.getHealth() < this.getMaxHealth() * 0.4F) {
               this.attemptTacticalWarp();
            }
         }
      } else if (this.random.nextInt(24) == 0) {
         this.level()
            .addParticle(
               ParticleTypes.PORTAL,
               this.getX() + (this.random.nextDouble() - 0.5) * (double)this.getBbWidth(),
               this.getY() + this.random.nextDouble() * (double)this.getBbHeight(),
               this.getZ() + (this.random.nextDouble() - 0.5) * (double)this.getBbWidth(),
               (this.random.nextDouble() - 0.5) * 0.5,
               -this.random.nextDouble(),
               (this.random.nextDouble() - 0.5) * 0.5
            );
      }
   }

   public boolean hurt(DamageSource pSource, float pAmount) {
      boolean wasHurt = super.hurt(pSource, pAmount);
      if (wasHurt && !this.level().isClientSide && this.warpCooldown <= 0 && this.random.nextFloat() < 0.25F) {
         this.performWarp(pSource);
      }

      return wasHurt;
   }

   private void attemptTacticalWarp() {
      if (this.warpCooldown <= 0 && this.onGround()) {
         LivingEntity target = this.getTarget();
         if (target != null) {
            double distance = this.distanceToSqr(target);
            if (distance < 25.0 || distance < 64.0 && this.random.nextFloat() < 0.05F) {
               this.performWarp(null);
            }
         }
      }
   }

   private void performWarp(DamageSource damageSource) {
      if (!this.level().isClientSide && this.isAlive()) {
         Vec3 warpPos = null;
         LivingEntity target = this.getTarget();

         for (int i = 0; i < 16; i++) {
            double randomX = this.getX() + (this.random.nextDouble() - 0.5) * 16.0;
            double randomY = this.getY() + (double)(this.random.nextInt(16) - 8);
            double randomZ = this.getZ() + (this.random.nextDouble() - 0.5) * 16.0;
            if (damageSource != null && damageSource.getEntity() != null) {
               Vec3 awayVector = this.position().subtract(damageSource.getEntity().position()).normalize();
               randomX = this.getX() + awayVector.x * (8.0 + this.random.nextDouble() * 8.0);
               randomZ = this.getZ() + awayVector.z * (8.0 + this.random.nextDouble() * 8.0);
            } else if (target != null) {
               double currentDist = (double)this.distanceTo(target);
               if (currentDist < 5.0) {
                  Vec3 awayVector = this.position().subtract(target.position()).normalize();
                  randomX = this.getX() + awayVector.x * 10.0;
                  randomZ = this.getZ() + awayVector.z * 10.0;
               }
            }

            if (this.randomTeleport(randomX, randomY, randomZ, true)) {
               warpPos = new Vec3(randomX, randomY, randomZ);
               break;
            }
         }

         if (warpPos != null) {
            this.level().broadcastEntityEvent(this, (byte)46);
            this.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
            this.warpCooldown = 100 + this.random.nextInt(100);
            this.lastWarpTime = this.tickCount;
         }
      }
   }

   public void handleEntityEvent(byte pId) {
      if (pId == 46) {
         for (int i = 0; i < 32; i++) {
            this.level()
               .addParticle(
                  ParticleTypes.PORTAL,
                  this.getX() + (this.random.nextDouble() - 0.5) * (double)this.getBbWidth() * 2.0,
                  this.getY() + this.random.nextDouble() * (double)this.getBbHeight(),
                  this.getZ() + (this.random.nextDouble() - 0.5) * (double)this.getBbWidth() * 2.0,
                  (this.random.nextDouble() - 0.5) * 2.0,
                  -this.random.nextDouble(),
                  (this.random.nextDouble() - 0.5) * 2.0
               );
         }
      } else {
         super.handleEntityEvent(pId);
      }
   }

   protected void registerGoals() {
      ItemStack mainHandItem = this.getMainHandItem();
      boolean hasGun = mainHandItem.getItem() instanceof GunItem;
      if (hasGun) {
         this.goalSelector.addGoal(1, new GunAttackGoal(this, mainHandItem, 1.0F, AIType.TACTICAL, 3));
      } else {
         this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
      }

      this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.9));
      this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
      this.targetSelector
         .addGoal(
            1,
            (new HurtByTargetGoal(this) {
                  public boolean canUse() {
                     return this.mob.getLastHurtByMob() != null && this.mob.getLastHurtByMob().getType().is(ModTags.Entities.ASGHARIAN_MOB_TYPES)
                        ? false
                        : super.canUse();
                  }
               })
               .setAlertOthers(new Class[0])
         );
      this.targetSelector.addGoal(2, new NearestAttackableTargetGoal(this, Player.class, true, target -> !(target instanceof Player player) || (!player.isCreative() && !player.isSpectator())));
   }

   public boolean canAttack(LivingEntity target) {
      return target.getType().is(ModTags.Entities.ASGHARIAN_MOB_TYPES) ? false : super.canAttack(target);
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
         .getEntitiesOfClass(Monster.class, alertArea, EntitySelector.NO_SPECTATORS)
         .stream()
         .filter(entity -> entity != this)
         .filter(entity -> entity.getType().is(ModTags.Entities.ASGHARIAN_MOB_TYPES))
         .filter(entity -> entity.getTarget() == null)
         .filter(entity -> !entity.isAlliedTo(this.getTarget()))
         .forEach(entity -> entity.setTarget(this.getTarget()));
   }

   public void setTarget(@Nullable LivingEntity target) {
      if (target == null || !target.getType().is(ModTags.Entities.ASGHARIAN_MOB_TYPES)) {
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
      return SoundEvents.ENDERMAN_AMBIENT;
   }

   @NotNull
   protected SoundEvent getHurtSound(@NotNull DamageSource pDamageSource) {
      return SoundEvents.ENDERMAN_HURT;
   }

   @NotNull
   protected SoundEvent getDeathSound() {
      return SoundEvents.ENDERMAN_DEATH;
   }

   public void performRangedAttack(@NotNull LivingEntity target, float distanceFactor) {
      this.doHurtTarget(target);
   }

   public void addAdditionalSaveData(@NotNull CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putInt("AlertCooldown", this.ticksUntilNextAlert);
      tag.putInt("WarpCooldown", this.warpCooldown);
      tag.putInt("LastWarpTime", this.lastWarpTime);
   }

   public void readAdditionalSaveData(@NotNull CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.ticksUntilNextAlert = tag.getInt("AlertCooldown");
      this.warpCooldown = tag.getInt("WarpCooldown");
      this.lastWarpTime = tag.getInt("LastWarpTime");
   }
}
