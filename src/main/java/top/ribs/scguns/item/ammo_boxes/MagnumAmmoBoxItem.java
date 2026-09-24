package top.ribs.scguns.item.ammo_boxes;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import top.ribs.scguns.item.AmmoBoxItem;

public class MagnumAmmoBoxItem extends AmmoBoxItem {
   private static final int MAGNUM_BASE_CAPACITY = 512;

   public MagnumAmmoBoxItem(Properties properties) {
      super(properties);
   }

   @Override
   protected String getDescriptionKey() {
      return "item.scguns.magnum_ammo_box.description";
   }

   @Override
   protected ResourceLocation getAmmoTag() {
      return ResourceLocation.fromNamespaceAndPath("scguns", "magnum_ammo");
   }

   @Override
   protected int getBaseMaxItemCount() {
      return 512;
   }

   @Override
   public int getBarColor(ItemStack stack) {
      return Mth.color(0.4F, 0.4F, 0.7F);
   }
}
