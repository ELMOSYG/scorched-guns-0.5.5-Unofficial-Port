package top.ribs.scguns.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;

public class NetherStarFragmentItem extends Item {
   public NetherStarFragmentItem(Properties properties) {
      super(properties);
   }

   public boolean isFoil(ItemStack stack) {
      return true;
   }
}
