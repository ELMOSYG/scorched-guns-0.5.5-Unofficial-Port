package top.ribs.scguns.item;


import net.minecraft.core.Holder;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties.Builder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import top.ribs.scguns.init.ModEffects;

public class WeirdFleshItem extends Item {
   private static final Map<Holder<MobEffect>, WeirdFleshItem.EffectData> WEIRD_FLESH_EFFECTS = new HashMap<>();

   public WeirdFleshItem() {
      // 1.21 dropped FoodProperties.Builder#meat() (wolf food is data-driven now) and
      // renamed alwaysEat() to alwaysEdible().
      super(new Properties().food(new Builder().nutrition(2).saturationModifier(0.1F).alwaysEdible().build()));
   }

   public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
      ItemStack result = super.finishUsingItem(stack, level, entity);
      if (!level.isClientSide()) {
         this.applyRandomEffect(entity);
      }

      return result;
   }

   private void applyRandomEffect(LivingEntity entity) {
      Random random = new Random();
      int totalWeight = 0;

      for (WeirdFleshItem.EffectData data : WEIRD_FLESH_EFFECTS.values()) {
         totalWeight += data.weight;
      }

      int roll = random.nextInt(totalWeight);
      int currentWeight = 0;

      for (Entry<Holder<MobEffect>, WeirdFleshItem.EffectData> entry : WEIRD_FLESH_EFFECTS.entrySet()) {
         currentWeight += entry.getValue().weight;
         if (roll < currentWeight) {
            Holder<MobEffect> effect = entry.getKey();
            WeirdFleshItem.EffectData data = entry.getValue();
            int duration = data.getDuration(random);
            int amplifier = data.getAmplifier(random);
            entity.addEffect(new MobEffectInstance(effect, duration, amplifier));
            break;
         }
      }
   }

   static {
      WEIRD_FLESH_EFFECTS.put(MobEffects.POISON, new WeirdFleshItem.EffectData(20, 100, 200, 0, 1));
      WEIRD_FLESH_EFFECTS.put(MobEffects.CONFUSION, new WeirdFleshItem.EffectData(18, 100, 180, 0, 0));
      WEIRD_FLESH_EFFECTS.put(MobEffects.WEAKNESS, new WeirdFleshItem.EffectData(15, 80, 160, 0, 1));
      WEIRD_FLESH_EFFECTS.put(MobEffects.MOVEMENT_SLOWDOWN, new WeirdFleshItem.EffectData(15, 60, 140, 0, 1));
      WEIRD_FLESH_EFFECTS.put(MobEffects.HUNGER, new WeirdFleshItem.EffectData(25, 100, 300, 0, 2));
      WEIRD_FLESH_EFFECTS.put(MobEffects.BLINDNESS, new WeirdFleshItem.EffectData(12, 60, 120, 0, 0));
      WEIRD_FLESH_EFFECTS.put(MobEffects.WITHER, new WeirdFleshItem.EffectData(10, 40, 100, 0, 1));
      WEIRD_FLESH_EFFECTS.put(ModEffects.SULFUR_POISONING, new WeirdFleshItem.EffectData(12, 60, 140, 0, 1));
      WEIRD_FLESH_EFFECTS.put(ModEffects.BLINDED, new WeirdFleshItem.EffectData(10, 40, 100, 0, 0));
      WEIRD_FLESH_EFFECTS.put(ModEffects.DEAFENED, new WeirdFleshItem.EffectData(10, 40, 100, 0, 0));
      WEIRD_FLESH_EFFECTS.put(ModEffects.LACERATED, new WeirdFleshItem.EffectData(8, 80, 160, 0, 1));
      WEIRD_FLESH_EFFECTS.put(MobEffects.LEVITATION, new WeirdFleshItem.EffectData(8, 40, 80, 0, 1));
      WEIRD_FLESH_EFFECTS.put(MobEffects.DIG_SLOWDOWN, new WeirdFleshItem.EffectData(10, 60, 120, 0, 1));
      WEIRD_FLESH_EFFECTS.put(MobEffects.REGENERATION, new WeirdFleshItem.EffectData(5, 60, 120, 0, 1));
      WEIRD_FLESH_EFFECTS.put(MobEffects.DAMAGE_RESISTANCE, new WeirdFleshItem.EffectData(4, 60, 140, 0, 1));
      WEIRD_FLESH_EFFECTS.put(MobEffects.MOVEMENT_SPEED, new WeirdFleshItem.EffectData(6, 100, 200, 0, 1));
      WEIRD_FLESH_EFFECTS.put(MobEffects.ABSORPTION, new WeirdFleshItem.EffectData(3, 80, 160, 0, 1));
      WEIRD_FLESH_EFFECTS.put(MobEffects.FIRE_RESISTANCE, new WeirdFleshItem.EffectData(4, 100, 200, 0, 0));
      WEIRD_FLESH_EFFECTS.put(MobEffects.WATER_BREATHING, new WeirdFleshItem.EffectData(4, 100, 200, 0, 0));
      WEIRD_FLESH_EFFECTS.put(MobEffects.NIGHT_VISION, new WeirdFleshItem.EffectData(5, 100, 300, 0, 0));
      WEIRD_FLESH_EFFECTS.put(MobEffects.HEAL, new WeirdFleshItem.EffectData(2, 1, 1, 0, 1));
      WEIRD_FLESH_EFFECTS.put(MobEffects.HARM, new WeirdFleshItem.EffectData(3, 1, 1, 0, 1));
   }

   private static record EffectData(int weight, int minDuration, int maxDuration, int minAmplifier, int maxAmplifier) {
      private EffectData(int weight, int minDuration, int maxDuration, int minAmplifier, int maxAmplifier) {
         this.weight = weight;
         this.minDuration = minDuration;
         this.maxDuration = maxDuration;
         this.minAmplifier = minAmplifier;
         this.maxAmplifier = maxAmplifier;
      }

      int getDuration(Random random) {
         return this.minDuration == this.maxDuration ? this.minDuration : this.minDuration + random.nextInt(this.maxDuration - this.minDuration + 1);
      }

      int getAmplifier(Random random) {
         return this.minAmplifier == this.maxAmplifier ? this.minAmplifier : this.minAmplifier + random.nextInt(this.maxAmplifier - this.minAmplifier + 1);
      }
   }
}
