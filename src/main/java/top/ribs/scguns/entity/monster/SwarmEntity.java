package top.ribs.scguns.entity.monster;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import top.ribs.scguns.util.MobType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.ribs.scguns.init.ModTags;

public class SwarmEntity extends FlyingMob implements Enemy {
   private static final int LIFESPAN_TICKS = 1200;
   private int lifespan;
   private boolean isActive = true;

   public SwarmEntity(EntityType<? extends SwarmEntity> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
      this.moveControl = new SwarmEntity.SwarmMoveGoal(this, 0.5F, 0.7F);
      this.lookControl = new SwarmEntity.SwarmLookGoal(this);
      this.lifespan = 1200;
   }

   @NotNull
   public MobType getMobType() {
      return MobType.ARTHROPOD;
   }

   public boolean isAlive() {
      return !this.isDeadOrDying() && super.isAlive();
   }

   public void die(DamageSource cause) {
      super.die(cause);
      this.isActive = false;
   }

   public boolean isActive() {
      return this.isActive;
   }

   public boolean isAffectedByPotions() {
      return true;
   }

   public boolean canBeAffected(MobEffectInstance effect) {
      return effect.getEffect() == MobEffects.POISON ? false : super.canBeAffected(effect);
   }

   public void tick() {
      super.tick();
      if (!this.level().isClientSide() && --this.lifespan <= 0) {
         this.discard();
      }
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
      this.goalSelector.addGoal(0, new SwarmEntity.FollowHiveGoal(this, 1.0, 10.0F));
      this.targetSelector.addGoal(0, new NearestAttackableTargetGoal(this, Player.class, true));
      this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<Monster>(this, Monster.class, true) {
         public boolean canUse() {
            return super.canUse() && SwarmEntity.this.isValidTarget(this.target);
         }
      });
      this.goalSelector.addGoal(1, new SwarmEntity.SwarmAttackGoal(this, 1.0));
   }

   public static Builder createAttributes() {
      return Animal.createLivingAttributes()
         .add(Attributes.MAX_HEALTH, 6.0)
         .add(Attributes.FOLLOW_RANGE, 24.0)
         .add(Attributes.ARMOR_TOUGHNESS, 0.1F)
         .add(Attributes.ATTACK_KNOCKBACK, 0.0)
         .add(Attributes.ATTACK_DAMAGE, 1.0);
   }

   @Nullable
   protected SoundEvent getAmbientSound() {
      return SoundEvents.BEE_LOOP;
   }

   @Nullable
   protected SoundEvent getHurtSound(DamageSource pDamageSource) {
      return SoundEvents.BEE_HURT;
   }

   @Nullable
   protected SoundEvent getDeathSound() {
      return SoundEvents.BEE_DEATH;
   }

   private boolean isValidTarget(LivingEntity target) {
      return target == null ? false : !target.getType().is(ModTags.Entities.NON_SWARM_TARGETED);
   }

   public class FollowHiveGoal extends Goal {
      private final SwarmEntity swarm;
      private final double followSpeed;
      private final float maxDist;
      private HiveEntity nearestHive;

      public FollowHiveGoal(SwarmEntity swarm, double followSpeed, float maxDist) {
         super();
         this.swarm = swarm;
         this.followSpeed = followSpeed;
         this.maxDist = maxDist;
         this.setFlags(EnumSet.of(Flag.MOVE));
      }

      public boolean canUse() {
         return this.swarm.getTarget() == null && this.findNearestHive();
      }

      public boolean canContinueToUse() {
         return this.swarm.getTarget() == null
            && this.nearestHive != null
            && this.nearestHive.isAlive()
            && this.swarm.distanceToSqr(this.nearestHive) > (double)(this.maxDist * this.maxDist);
      }

      public void start() {
         this.swarm.getNavigation().moveTo(this.nearestHive, this.followSpeed);
      }

      public void stop() {
         this.nearestHive = null;
         this.swarm.getNavigation().stop();
      }

      public void tick() {
         if (this.swarm.distanceToSqr(this.nearestHive) > (double)(this.maxDist * this.maxDist)) {
            this.swarm.getNavigation().moveTo(this.nearestHive, this.followSpeed);
         } else {
            this.swarm.getNavigation().stop();
         }
      }

      private boolean findNearestHive() {
         List<HiveEntity> hiveList = this.swarm.level().getEntitiesOfClass(HiveEntity.class, this.swarm.getBoundingBox().inflate((double)this.maxDist));
         if (hiveList.isEmpty()) {
            return false;
         } else {
            this.nearestHive = hiveList.get(0);
            return true;
         }
      }
   }

   public class SwarmAttackGoal extends Goal {
      private final SwarmEntity swarm;
      private final double speedTowardsTarget;
      private int attackCooldown;

      public SwarmAttackGoal(SwarmEntity swarm, double speedTowardsTarget) {
         super();
         this.swarm = swarm;
         this.speedTowardsTarget = speedTowardsTarget;
         this.setFlags(EnumSet.of(Flag.MOVE));
      }

      public boolean canUse() {
         LivingEntity target = this.swarm.getTarget();
         return target != null && target.isAlive() && this.isValidTarget(target);
      }

      public void tick() {
         LivingEntity target = this.swarm.getTarget();
         if (target != null && this.swarm.distanceToSqr(target) < 9.0) {
            if (this.attackCooldown <= 0) {
               this.swarm.doHurtTarget(target);
               target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
               this.attackCooldown = 20;
            } else {
               this.attackCooldown--;
            }
         }

         this.swarm.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), this.speedTowardsTarget);
      }

      private boolean isValidTarget(LivingEntity target) {
         return !target.getType().is(ModTags.Entities.NON_SWARM_TARGETED);
      }
   }

   public static class SwarmLookGoal extends LookControl {
      public SwarmLookGoal(Mob mob) {
         super(mob);
      }

      public void tick() {
         if (this.mob.getTarget() != null) {
            this.mob.lookAt(this.mob.getTarget(), 30.0F, 30.0F);
         }
      }
   }

   public class SwarmMoveGoal extends MoveControl {
      private final Mob mob;
      private final float speed;
      private final double maxSpeed;

      public SwarmMoveGoal(Mob mob, float speed, double maxSpeed) {
         super(mob);
         this.mob = mob;
         this.speed = speed;
         this.maxSpeed = maxSpeed;
      }

      public void tick() {
         if (this.operation == MoveControl.Operation.MOVE_TO && this.mob.getTarget() != null) {
            LivingEntity target = this.mob.getTarget();
            double dx = target.getX() - this.mob.getX();
            double dy = target.getY() - this.mob.getY() + (double)(target.getBbHeight() / 2.0F);
            double dz = target.getZ() - this.mob.getZ();
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance < 10.0) {
               Vec3 desiredMovement = new Vec3(dx / distance * (double)this.speed, dy / distance * (double)this.speed, dz / distance * (double)this.speed);
               Vec3 currentMovement = this.mob.getDeltaMovement();
               Vec3 newMovement = currentMovement.add(desiredMovement.subtract(currentMovement).scale(0.2));
               if (newMovement.length() > this.maxSpeed) {
                  newMovement = newMovement.normalize().scale(this.maxSpeed);
               }

               this.mob.setDeltaMovement(newMovement);
            }
         }
      }
   }
}
