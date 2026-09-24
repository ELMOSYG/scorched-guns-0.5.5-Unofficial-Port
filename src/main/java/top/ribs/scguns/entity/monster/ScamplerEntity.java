package top.ribs.scguns.entity.monster;


import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import top.ribs.scguns.util.MobType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ScamplerEntity extends Monster {
   private int fuseTime = 25;
   private boolean hasIgnited = false;

   public ScamplerEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
   }

   public static Builder createAttributes() {
      return Animal.createLivingAttributes()
         .add(Attributes.MAX_HEALTH, 10.0)
         .add(Attributes.FOLLOW_RANGE, 12.0)
         .add(Attributes.MOVEMENT_SPEED, 0.36)
         .add(Attributes.KNOCKBACK_RESISTANCE, 0.6F);
   }

   @NotNull
   public MobType getMobType() {
      return MobType.UNDEFINED;
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

   public void tick() {
      super.tick();
      if (!this.level().isClientSide()) {
         LivingEntity target = this.getTarget();
         if (target != null && !this.hasIgnited) {
            double distanceToTarget = this.distanceToSqr(target);
            if (distanceToTarget <= 9.0) {
               this.ignite();
            }
         }

         if (this.hasIgnited) {
            this.fuseTime--;
            if (this.fuseTime <= 0) {
               this.explode();
            } else if (this.fuseTime == 20 || this.fuseTime == 10) {
               this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.CREEPER_PRIMED, this.getSoundSource(), 1.0F, 0.8F);
            }
         }
      }
   }

   private void ignite() {
      this.hasIgnited = true;
      this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.FLINTANDSTEEL_USE, this.getSoundSource(), 1.0F, 1.0F);
   }

   private void explode() {
      if (!this.level().isClientSide()) {
         this.level().explode(this, this.getX(), this.getY(), this.getZ(), 2.0F, ExplosionInteraction.NONE);
         this.discard();
      }
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, false));
      this.goalSelector.addGoal(2, new MoveTowardsTargetGoal(this, 1.2, 8.0F));
      this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this, new Class[0]).setAlertOthers(new Class[]{ScamplerEntity.class}));
      this.targetSelector.addGoal(2, new NearestAttackableTargetGoal(this, Player.class, true));
      this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
   }

   public boolean doHurtTarget(Entity pEntity) {
      return false;
   }

   @Nullable
   protected SoundEvent getHurtSound(DamageSource pDamageSource) {
      return SoundEvents.SNOW_GOLEM_HURT;
   }

   @Nullable
   protected SoundEvent getDeathSound() {
      return SoundEvents.SNOW_GOLEM_DEATH;
   }

   public int getFuseTime() {
      return this.fuseTime;
   }

   public boolean isIgnited() {
      return this.hasIgnited;
   }
}
