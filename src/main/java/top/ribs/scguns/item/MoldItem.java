package top.ribs.scguns.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import org.jetbrains.annotations.NotNull;

public class MoldItem extends Item {
   public MoldItem(Properties properties) {
      super(properties);
   }

   public boolean isDamageable(ItemStack stack) {
      return true;
   }

   public boolean isRepairable(@NotNull ItemStack stack) {
      return false;
   }

   public boolean hasCraftingRemainingItem(ItemStack stack) {
      return true;
   }

   public ItemStack getCraftingRemainingItem(ItemStack stack) {
      ItemStack remainingItem = stack.copy();
      remainingItem.setDamageValue(remainingItem.getDamageValue() + 1);
      return remainingItem.getDamageValue() >= remainingItem.getMaxDamage() ? ItemStack.EMPTY : remainingItem;
   }
}
