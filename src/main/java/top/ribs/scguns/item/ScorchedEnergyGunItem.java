package top.ribs.scguns.item;

import net.minecraft.world.item.Item.Properties;

public class ScorchedEnergyGunItem extends EnergyGunItem {
   public ScorchedEnergyGunItem(Properties properties, int capacity) {
      super(properties, capacity);
   }

   public boolean isFireResistant() {
      return true;
   }
}
