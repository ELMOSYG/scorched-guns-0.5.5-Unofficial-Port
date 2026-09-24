package top.ribs.scguns.item.exosuit;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;

public class DamageableUpgradeItem extends Item {
   public DamageableUpgradeItem(Properties properties) {
      super(properties);
   }

   public boolean isDamageable(ItemStack stack) {
      return true;
   }

   public int getMaxDamage(ItemStack stack) {
      return super.getMaxDamage(stack);
   }

   public boolean canBeDepleted() {
      return true;
   }

   public void onUpgradeDamaged(ItemStack stack, int damage) {
      stack.setDamageValue(Math.min(stack.getDamageValue() + damage, stack.getMaxDamage()));
   }

   public boolean isBroken(ItemStack stack) {
      return stack.getDamageValue() >= stack.getMaxDamage();
   }
}
