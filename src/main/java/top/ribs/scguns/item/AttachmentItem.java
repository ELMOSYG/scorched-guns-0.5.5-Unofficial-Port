package top.ribs.scguns.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import top.ribs.scguns.init.ModItems;

public class AttachmentItem extends Item implements IMeta {
   public AttachmentItem(Properties properties) {
      super(properties);
   }

   public boolean isFoil(ItemStack stack) {
      return false;
   }

   public boolean isValidRepairItem(ItemStack pToRepair, ItemStack pRepair) {
      return pRepair.is((Item)ModItems.REPAIR_KIT.get());
   }
}
