package top.ribs.scguns.effect;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import top.ribs.scguns.init.ModEffects;

public class SulfurPoisoningEffect extends MobEffect {
   /** 1.20.1 identified attribute modifiers by UUID string; 1.21 uses a {@link ResourceLocation}. */
   private static final ResourceLocation MOVEMENT_SPEED_MODIFIER = ResourceLocation.fromNamespaceAndPath("scguns", "sulfur_poisoning_movement_speed");
   private static final ResourceLocation JUMP_STRENGTH_MODIFIER = ResourceLocation.fromNamespaceAndPath("scguns", "sulfur_poisoning_jump_strength");
   private static final ResourceLocation ATTACK_SPEED_MODIFIER = ResourceLocation.fromNamespaceAndPath("scguns", "sulfur_poisoning_attack_speed");

   public SulfurPoisoningEffect(MobEffectCategory typeIn, int liquidColorIn) {
      super(typeIn, liquidColorIn);
      this.addAttributeModifier(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED_MODIFIER, -0.1, Operation.ADD_MULTIPLIED_TOTAL);
      this.addAttributeModifier(Attributes.JUMP_STRENGTH, JUMP_STRENGTH_MODIFIER, -0.15, Operation.ADD_MULTIPLIED_TOTAL);
      this.addAttributeModifier(Attributes.ATTACK_SPEED, ATTACK_SPEED_MODIFIER, -0.15, Operation.ADD_MULTIPLIED_TOTAL);
   }

   public boolean applyEffectTick(LivingEntity entity, int amplifier) {
      MobEffectInstance effect = entity.getEffect(ModEffects.SULFUR_POISONING);
      if (effect != null) {
         int duration = effect.getDuration();
         int originalDuration = this.getOriginalDuration(effect);
         float intensityRatio = this.calculateIntensityRatio(duration, originalDuration);
         int damageInterval = Math.max(15, (int)(30.0F - (float)(amplifier * 8) * intensityRatio));
         if (entity.tickCount % damageInterval == 0) {
            float damage = (1.5F + (float)amplifier * 0.75F) * intensityRatio;
            if (damage > 0.3F) {
               entity.hurt(entity.damageSources().magic(), damage);
            }
         }

         if (entity instanceof Player player) {
            this.handlePlayerEffects(player, amplifier, intensityRatio, entity.getRandom());
         }

         if (intensityRatio > 0.3F) {
            this.applyIntensePhaseEffects(entity, amplifier, intensityRatio, entity.getRandom());
         }

         super.applyEffectTick(entity, amplifier);
      }

      // 1.21 removes the effect when applyEffectTick returns false; 0.5.5 never expired early.
      return true;
   }

   private void handlePlayerEffects(Player player, int amplifier, float intensityRatio, RandomSource random) {
      if (intensityRatio > 0.5F) {
         if (player.tickCount % (int)(60.0F / intensityRatio) == 0) {
            player.getFoodData().addExhaustion(0.15F + (float)amplifier * 0.1F * intensityRatio);
         }
      } else if (player.tickCount % 100 == 0) {
         player.getFoodData().addExhaustion(0.05F + (float)amplifier * 0.03F * intensityRatio);
      }
   }

   private void applyIntensePhaseEffects(LivingEntity entity, int amplifier, float intensityRatio, RandomSource random) {
      if (!(intensityRatio < 0.7F)) {
         if (entity.tickCount % 300 == 0 && random.nextFloat() < 0.2F * intensityRatio) {
            entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, (int)(40.0F * intensityRatio), 0));
         }

         if (intensityRatio > 0.8F && entity.tickCount % 200 == 0 && random.nextFloat() < 0.25F) {
            entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, (int)(60.0F * intensityRatio), amplifier > 1 ? 1 : 0));
         }

         if (intensityRatio > 0.85F && amplifier >= 2 && entity.tickCount % 250 == 0 && random.nextFloat() < 0.15F) {
            entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
         }
      }
   }

   private float calculateIntensityRatio(int remainingDuration, int originalDuration) {
      if (originalDuration <= 0) {
         return 1.0F;
      } else {
         float ratio = (float)remainingDuration / (float)originalDuration;
         if (ratio > 0.7F) {
            return 1.0F;
         } else {
            return ratio > 0.3F ? 0.4F + (ratio - 0.3F) * 1.5F : Math.max(0.1F, ratio * 1.33F);
         }
      }
   }

   private int getOriginalDuration(MobEffectInstance effect) {
      int currentDuration = effect.getDuration();
      if (currentDuration > 1000) {
         return Math.max(1200, currentDuration + 200);
      } else {
         return currentDuration > 500 ? Math.max(800, currentDuration + 150) : Math.max(400, currentDuration + 100);
      }
   }

   public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
      return true;
   }

   public static boolean hasFireVulnerability(LivingEntity entity) {
      return entity.hasEffect(ModEffects.SULFUR_POISONING);
   }

   public static float getFireDamageMultiplier(LivingEntity entity) {
      if (!hasFireVulnerability(entity)) {
         return 1.0F;
      } else {
         MobEffectInstance effect = entity.getEffect(ModEffects.SULFUR_POISONING);
         if (effect == null) {
            return 1.0F;
         } else {
            int amplifier = effect.getAmplifier();
            // 1.21.1: MobEffectInstance#getEffect() returns a Holder<MobEffect>, not the effect. The
            // old 1.20.1 cast to SulfurPoisoningEffect therefore threw a ClassCastException - on the
            // hurt event, i.e. it crashed the game the first time a burning mob was damaged
            // (HANDOFF section 45). value() plus an instanceof keeps it safe whatever the holder is.
            if (!(effect.getEffect().value() instanceof SulfurPoisoningEffect poisonEffect)) {
               return 1.0F;
            }

            float intensityRatio = poisonEffect.calculateIntensityRatio(effect.getDuration(), poisonEffect.getOriginalDuration(effect));
            float baseMultiplier = 1.3F + (float)amplifier * 0.3F;
            return 1.0F + (baseMultiplier - 1.0F) * intensityRatio;
         }
      }
   }
}
