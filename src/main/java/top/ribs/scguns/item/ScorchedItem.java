package top.ribs.scguns.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;

public class ScorchedItem extends Item {
   public ScorchedItem(Properties properties) {
      super(properties);
   }

   public boolean isFireResistant() {
      return true;
   }
}
