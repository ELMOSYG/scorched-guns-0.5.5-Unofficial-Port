package top.ribs.scguns.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import top.ribs.scguns.item.attachment.IBarrel;
import top.ribs.scguns.item.attachment.impl.Barrel;

public class BarrelItem extends AttachmentItem implements IBarrel, IColored {
   private final Barrel barrel;
   private final boolean colored;

   public BarrelItem(Barrel barrel, Properties properties) {
      super(properties);
      this.barrel = barrel;
      this.colored = true;
   }

   public BarrelItem(Barrel barrel, Properties properties, boolean colored) {
      super(properties);
      this.barrel = barrel;
      this.colored = colored;
   }

   public Barrel getProperties() {
      return this.barrel;
   }

   @Override
   public boolean canColor(ItemStack stack) {
      return this.colored;
   }


}
