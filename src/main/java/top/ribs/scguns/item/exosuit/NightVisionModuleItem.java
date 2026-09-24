package top.ribs.scguns.item.exosuit;

import net.minecraft.world.item.Item.Properties;

public class NightVisionModuleItem extends EnergyUpgradeItem {
   public NightVisionModuleItem(Properties properties) {
      super(properties, EnergyUpgradeItem.EnergyConsumptionType.PER_SECOND);
   }
}
