package top.ribs.scguns.entity.monster;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import top.ribs.scguns.util.MobType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.ribs.scguns.init.ModEntities;

public class HiveEntity extends Monster {
   private int swarmSummonCooldown = 0;
   private int spawnAnimationTicks = 0;
   static final List<SwarmEntity> summonedSwarm = new ArrayList<>();

   public HiveEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
   }

   public boolean isSpawning() {
      return this.spawnAnimationTicks > 0;
   }

   public float getSpawnAnimationProgress(float partialTick) {
      return this.spawnAnimationTicks <= 0 ? 0.0F : ((float)this.spawnAnimationTicks - partialTick) / 20.0F;
   }

   public Level getEntityLevel() {
      return this.level();
   }

   @NotNull
   public MobType getMobType() {
      return MobType.UNDEAD;
   }

   public void tick() {
      super.tick();
      if (this.swarmSummonCooldown > 0) {
         this.swarmSummonCooldown--;
      }

      if (this.spawnAnimationTicks > 0) {
         this.spawnAnimationTicks--;
      }

      summonedSwarm.removeIf(swarm -> !swarm.isAlive());
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
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 1.0));
      this.targetSelector.addGoal(1, new NearestAttackableTargetGoal(this, Player.class, true));
      this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 3.0F));
      this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
      this.goalSelector.addGoal(4, new HiveEntity.HiveSummonGoal(this));
   }

   public void die(@NotNull DamageSource cause) {
      super.die(cause);

      for (SwarmEntity swarm : summonedSwarm) {
         if (swarm != null) {
            swarm.discard();
         }
      }

      summonedSwarm.clear();
   }

   public static Builder createAttributes() {
      return Animal.createLivingAttributes()
         .add(Attributes.MAX_HEALTH, 20.0)
         .add(Attributes.FOLLOW_RANGE, 24.0)
         .add(Attributes.MOVEMENT_SPEED, 0.15)
         .add(Attributes.ARMOR_TOUGHNESS, 0.1F)
         .add(Attributes.ATTACK_KNOCKBACK, 0.5)
         .add(Attributes.ATTACK_DAMAGE, 2.0);
   }

   public boolean hurt(@NotNull DamageSource source, float amount) {
      boolean isHurt = super.hurt(source, amount);
      if (isHurt && this.swarmSummonCooldown <= 0) {
         this.summonSwarm();
      }

      return isHurt;
   }

   private boolean canSummonSwarm() {
      return summonedSwarm.isEmpty() || summonedSwarm.stream().noneMatch(SwarmEntity::isActive);
   }

   private void summonSwarm() {
      if (this.canSummonSwarm()) {
         SwarmEntity swarm = (SwarmEntity)((EntityType)ModEntities.SWARM.get()).create(this.level());
         if (swarm != null) {
            swarm.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
            this.level().addFreshEntity(swarm);
            summonedSwarm.add(swarm);
            this.swarmSummonCooldown = 60;
            this.spawnAnimationTicks = 20;
            this.playSwarmSummonedSound();
         }
      }
   }

   @Nullable
   protected SoundEvent getAmbientSound() {
      this.playAdditionalSound(SoundEvents.BEE_LOOP, 1.5F, this.getVoicePitch());
      return SoundEvents.ZOMBIE_AMBIENT;
   }

   @Nullable
   protected SoundEvent getHurtSound(@NotNull DamageSource pDamageSource) {
      this.playAdditionalSound(SoundEvents.BEE_HURT, 1.5F, this.getVoicePitch());
      return SoundEvents.ZOMBIE_HURT;
   }

   @Nullable
   protected SoundEvent getDeathSound() {
      this.playAdditionalSound(SoundEvents.BEE_DEATH, 1.5F, this.getVoicePitch());
      return SoundEvents.ZOMBIE_DEATH;
   }

   private void playAdditionalSound(SoundEvent soundEvent, float volume, float pitch) {
      if (this.level().isClientSide()) {
         this.level().playSound(null, this.blockPosition(), soundEvent, SoundSource.HOSTILE, volume, pitch);
      }
   }

   private void playSwarmSummonedSound() {
      if (!this.level().isClientSide()) {
         SoundEvent soundEvent = SoundEvents.BEEHIVE_EXIT;
         this.level().playSound(null, this.blockPosition(), soundEvent, SoundSource.NEUTRAL, 1.0F, 1.0F);
      }
   }

   public float getVoicePitch() {
      return super.getVoicePitch() * 1.3F;
   }

   public static class HiveSummonGoal extends Goal {
      private final HiveEntity hiveEntity;

      public HiveSummonGoal(HiveEntity hiveEntity) {
         super();
         this.hiveEntity = hiveEntity;
         this.setFlags(EnumSet.of(Flag.MOVE));
      }

      public boolean canUse() {
         return this.hiveEntity.getTarget() != null && this.hiveEntity.swarmSummonCooldown <= 0;
      }

      public void start() {
         this.hiveEntity.summonSwarm();
      }

      public void tick() {
         if (this.hiveEntity.swarmSummonCooldown <= 0 && this.hiveEntity.canSummonSwarm()) {
            this.hiveEntity.summonSwarm();
         }
      }
   }
}
