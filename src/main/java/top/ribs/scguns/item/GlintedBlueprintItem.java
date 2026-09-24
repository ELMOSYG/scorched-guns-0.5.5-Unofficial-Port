package top.ribs.scguns.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;

public class GlintedBlueprintItem extends BlueprintItem {
   public GlintedBlueprintItem(Properties properties) {
      super(properties);
   }

   public boolean isFoil(ItemStack stack) {
      return true;
   }

   public boolean isFireResistant() {
      return true;
   }
}
