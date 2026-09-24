package top.ribs.scguns.item;

import net.minecraft.world.item.Item.Properties;

public class ScorchedWeapon extends GunItem {
   public ScorchedWeapon(Properties properties) {
      super(properties);
   }

   public boolean isFireResistant() {
      return true;
   }
}
