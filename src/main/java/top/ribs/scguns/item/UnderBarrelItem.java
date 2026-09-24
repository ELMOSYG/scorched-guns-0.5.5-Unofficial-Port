package top.ribs.scguns.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import top.ribs.scguns.item.attachment.IUnderBarrel;
import top.ribs.scguns.item.attachment.impl.UnderBarrel;

public class UnderBarrelItem extends AttachmentItem implements IUnderBarrel, IColored {
   private final UnderBarrel underBarrel;
   private final boolean colored;

   public UnderBarrelItem(UnderBarrel underBarrel, Properties properties) {
      super(properties);
      this.underBarrel = underBarrel;
      this.colored = true;
   }

   public UnderBarrelItem(UnderBarrel underBarrel, Properties properties, boolean colored) {
      super(properties);
      this.underBarrel = underBarrel;
      this.colored = colored;
   }

   public UnderBarrel getProperties() {
      return this.underBarrel;
   }

   @Override
   public boolean canColor(ItemStack stack) {
      return this.colored;
   }


}
