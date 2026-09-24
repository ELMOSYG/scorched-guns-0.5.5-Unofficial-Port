package top.ribs.scguns.item;

import java.util.Arrays;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;

public class GlintedHealingBandageItem extends HealingBandageItem {
   public GlintedHealingBandageItem(Properties properties, int healingAmount, MobEffectInstance... potionEffects) {
      super(properties, healingAmount, potionEffects);
      this.healingAmount = healingAmount;
      this.potionEffects = Arrays.asList(potionEffects);
   }

   public boolean isFoil(ItemStack stack) {
      return true;
   }
}
