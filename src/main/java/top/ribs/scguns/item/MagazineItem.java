package top.ribs.scguns.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import top.ribs.scguns.item.attachment.IMagazine;
import top.ribs.scguns.item.attachment.impl.Magazine;

public class MagazineItem extends AttachmentItem implements IMagazine, IColored {
   private final Magazine magazine;
   private final boolean colored;

   public MagazineItem(Magazine magazine, Properties properties) {
      super(properties);
      this.magazine = magazine;
      this.colored = true;
   }

   public MagazineItem(Magazine magazine, Properties properties, boolean colored) {
      super(properties);
      this.magazine = magazine;
      this.colored = colored;
   }

   public Magazine getProperties() {
      return this.magazine;
   }

   @Override
   public boolean canColor(ItemStack stack) {
      return this.colored;
   }


}
