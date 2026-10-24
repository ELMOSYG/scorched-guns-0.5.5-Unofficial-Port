package top.ribs.scguns.entity.monster;


import net.minecraft.core.Holder;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.ribs.scguns.Config;
import top.ribs.scguns.config.EntityEquipmentConfig;
import top.ribs.scguns.init.ModEntities;
import top.ribs.scguns.item.GunItem;

public class CogKnightEntity extends Monster {
   private static final EntityDataAccessor<Boolean> ATTACKING = SynchedEntityData.defineId(CogKnightEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> ATTACK_TIMEOUT = SynchedEntityData.defineId(CogKnightEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> CHARGING = SynchedEntityData.defineId(CogKnightEntity.class, EntityDataSerializers.BOOLEAN);
   public final AnimationState idleAnimationState = new AnimationState();

   public CogKnightEntity(EntityType<? extends CogKnightEntity> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
   }

   public HumanoidArm getMainArm() {
      return HumanoidArm.LEFT;
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
      return Animal.createLivingAttributes()
         .add(Attributes.MAX_HEALTH, 34.0)
         .add(Attributes.FOLLOW_RANGE, 24.0)
         .add(Attributes.MOVEMENT_SPEED, 0.25)
         .add(Attributes.ARMOR_TOUGHNESS, 0.5)
         .add(Attributes.ARMOR, 3.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
         .add(Attributes.ATTACK_KNOCKBACK, 0.8F)
         .add(Attributes.ATTACK_DAMAGE, 5.0);
   }

   public void tick() {
      super.tick();
      if (!this.level().isClientSide() && this.isAttacking() && this.getAttackTimeout() > 0) {
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

      if (this.level().isClientSide()) {
         this.setupAnimationStates();
      }
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

   private void setupAnimationStates() {
      if (!this.idleAnimationState.isStarted()) {
         this.idleAnimationState.start(this.tickCount);
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
      builder.define(CHARGING, false);
   }

   public void setCharging(boolean charging) {
      this.entityData.set(CHARGING, charging);
   }

   public boolean isCharging() {
      return (Boolean)this.entityData.get(CHARGING);
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

   @Override
   public SpawnGroupData finalizeSpawn(
      ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData
   ) {
      EntityEquipmentConfig.equipEntity(this, "scguns:cog_knight");
      return super.finalizeSpawn(pLevel,  pDifficulty,  pReason,  pSpawnData);
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(1, new CogKnightEntity.ChargeAttackGoal(this, 1.15, 12.0, 80));
      this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false) {
         @Override
         protected void checkAndPerformAttack(LivingEntity pEnemy) {
            double pDistToEnemySqr = this.mob.distanceToSqr(pEnemy);
            if (pDistToEnemySqr <= top.ribs.scguns.util.CombatHelper.attackReachSqr(this.mob, pEnemy) && this.getTicksUntilNextAttack() <= 0 && !CogKnightEntity.this.isAttacking()) {
               CogKnightEntity.this.setAttacking(true);
               this.resetAttackCooldown();
               this.mob.swing(InteractionHand.MAIN_HAND);
            }
         }

         protected double getAttackReachSqr(LivingEntity pEnemy) {
            return top.ribs.scguns.util.CombatHelper.attackReachSqr(this.mob, pEnemy) * 1.5;
         }

         protected void resetAttackCooldown() {
            this.adjustedTickDelay(25);
         }
      });
      this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
      this.goalSelector.addGoal(3, new MoveTowardsTargetGoal(this, 1.0, 30.0F));
      this.goalSelector.addGoal(4, new RandomStrollGoal(this, 1.0));
      this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 3.0F));
      this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this, new Class[0]).setAlertOthers(new Class[]{CogKnightEntity.class}));
      this.targetSelector.addGoal(2, new NearestAttackableTargetGoal(this, Player.class, true, target -> !(target instanceof Player player) || (!player.isCreative() && !player.isSpectator())));
   }

   public void handleEntityEvent(byte pId) {
      if (pId == 4) {
         if (this.level().isClientSide) {
            for (int i = 0; i < 8; i++) {
               this.level()
                  .addParticle(
                     ParticleTypes.CLOUD,
                     this.getX() + (this.random.nextDouble() - 0.5) * (double)this.getBbWidth(),
                     this.getY() + this.random.nextDouble() * (double)this.getBbHeight(),
                     this.getZ() + (this.random.nextDouble() - 0.5) * (double)this.getBbWidth(),
                     0.0,
                     0.0,
                     0.0
                  );
            }
         }
      } else {
         super.handleEntityEvent(pId);
      }
   }



   @Nullable
   protected SoundEvent getHurtSound(@NotNull DamageSource pDamageSource) {
      return SoundEvents.IRON_GOLEM_HURT;
   }

   @Nullable
   protected SoundEvent getDeathSound() {
      return SoundEvents.IRON_GOLEM_DEATH;
   }

   public void aiStep() {
      super.aiStep();
      if (!this.onGround() && this.getDeltaMovement().y < 0.0) {
         this.setDeltaMovement(this.getDeltaMovement().multiply(1.0, 0.6, 1.0));
      }
   }

   protected void checkFallDamage(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
      double velocityThreshold = -0.5;
      if (y >= velocityThreshold) {
         this.fallDistance = 0.0F;
      } else {
         super.checkFallDamage(y, onGroundIn, state, pos);
      }
   }

   /** Upper bound on the raw charge damage, applied before the difficulty multiplier and armour. */
   /** Wind-up ticks before the charge moves: the window in which it can be dodged. */
   private static final int CHARGE_WIND_UP_TICKS = 6;
   /** Ticks of the target's momentum used to lead the charge. */
   private static final double CHARGE_LEAD_TICKS = 5.0;
   private static final float CHARGE_DAMAGE_CAP = 18.0F;

   public class ChargeAttackGoal extends Goal {
      private final CogKnightEntity mob;
      private final double speedModifier;
      private final double chargeRange;
      private final int chargeCooldown;
      private int cooldownTicks;
      private int chargeTicks;
      /** Ticks left in the wind-up before the charge actually moves the knight. */
      private int windUpTicks;
      /** Direction captured when the charge starts; the knight no longer re-aims while charging. */
      private double chargeDirX;
      private double chargeDirZ;
      /** Cleared when a charge starts and set on the first hit, so one charge lands once. */
      private boolean hasHitThisCharge;
      private LivingEntity target;
      private boolean isCharging;

      public ChargeAttackGoal(CogKnightEntity mob, double speedModifier, double chargeRange, int chargeCooldown) {
         super();
         this.mob = mob;
         this.speedModifier = speedModifier;
         this.chargeRange = chargeRange;
         this.chargeCooldown = chargeCooldown;
         this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      public boolean canUse() {
         ItemStack mainHandItem = this.mob.getMainHandItem();
         if (mainHandItem.getItem() instanceof GunItem || mainHandItem.getItem() instanceof BowItem) {
            return false;
         } else if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return false;
         } else {
            this.target = this.mob.getTarget();
            if (this.target == null) {
               return false;
            } else {
               double distanceToTarget = this.mob.distanceToSqr(this.target);
               return distanceToTarget <= this.chargeRange * this.chargeRange && distanceToTarget > 4.0;
            }
         }
      }

      public boolean canContinueToUse() {
         return this.target != null && this.target.isAlive() && this.chargeTicks > 0;
      }

      public void start() {
         this.isCharging = true;
         this.chargeTicks = 16;
         this.windUpTicks = CHARGE_WIND_UP_TICKS;

         // Capture the direction once. Re-aiming every tick made the charge impossible to sidestep, which is what
         // players were complaining about: the only counter-play was to out-run a 0.8-block-per-tick homing dash.
         double dirX = this.target.getX() - this.mob.getX();
         double dirZ = this.target.getZ() - this.mob.getZ();
         double len = Math.sqrt(dirX * dirX + dirZ * dirZ);
         if (len > 0.01) {
            this.chargeDirX = dirX / len;
            this.chargeDirZ = dirZ / len;
         } else {
            this.chargeDirX = 0.0;
            this.chargeDirZ = 0.0;
         }
      this.hasHitThisCharge = false;
         this.mob.setCharging(true);
         this.mob.level().broadcastEntityEvent(this.mob, (byte)4);
      }

      public void tick() {
         if (this.target != null) {
            this.mob.getLookControl().setLookAt(this.target, 30.0F, 30.0F);
            if (this.chargeTicks > 0) {
               if (this.windUpTicks > 0) {
                  // Telegraph: the knight stands still and keeps tracking, so the player can see the charge coming.
                  this.windUpTicks--;
                  this.mob.getLookControl().setLookAt(this.target, 30.0F, 30.0F);
                  this.mob.setDeltaMovement(0.0, this.mob.getDeltaMovement().y, 0.0);

                  // Keep re-aiming through the wind-up, and lead the target: charging where the player is standing
                  // made a simple sidestep enough, while a straight dash at a moving player was free damage.
                  double predictedX = this.target.getX() + this.target.getDeltaMovement().x * CHARGE_LEAD_TICKS;
                  double predictedZ = this.target.getZ() + this.target.getDeltaMovement().z * CHARGE_LEAD_TICKS;
                  double dx = predictedX - this.mob.getX();
                  double dz = predictedZ - this.mob.getZ();
                  double len = Math.sqrt(dx * dx + dz * dz);
                  if (len > 0.01) {
                     this.chargeDirX = dx / len;
                     this.chargeDirZ = dz / len;
                  }
                  return;
               }

               this.chargeTicks--;
               this.mob.setDeltaMovement(
                  this.chargeDirX * this.speedModifier, this.mob.getDeltaMovement().y, this.chargeDirZ * this.speedModifier);

               if (!this.hasHitThisCharge && this.mob.distanceToSqr(this.target) <= 2.0) {
                  this.performChargeAttack();
               }
            }
         }
      }

      public void stop() {
         this.isCharging = false;
         this.mob.setCharging(false);
         this.cooldownTicks = this.chargeCooldown;
         this.target = null;
         this.chargeTicks = 0;
      this.hasHitThisCharge = false;
      }

      private void performChargeAttack() {
         if (this.target != null) {
            this.hasHitThisCharge = true;
            float difficultyScale = this.mob.level().getDifficulty() == net.minecraft.world.Difficulty.HARD ? 1.5F : 1.0F;
            // CHARGE_DAMAGE_CAP is the ceiling the player sees, and the difficulty multiplier is applied later,
            // so divide by it here to keep the final number at the cap on every difficulty.
            float chargeDamage = Math.min((float)this.mob.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.5F, CHARGE_DAMAGE_CAP / difficultyScale);   
            this.target.hurt(this.mob.damageSources().mobAttack(this.mob), chargeDamage);
            double knockbackStrength = 1.0;
            double dx = this.target.getX() - this.mob.getX();
            double dz = this.target.getZ() - this.mob.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance > 0.0) {
               this.target.setDeltaMovement(this.target.getDeltaMovement().add(dx / distance * knockbackStrength, 0.2, dz / distance * knockbackStrength));
            }

            this.mob.playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 1.0F);
         }

         this.chargeTicks = 0;
      }
   }
}
