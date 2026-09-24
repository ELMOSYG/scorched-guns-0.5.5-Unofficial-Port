package top.ribs.scguns.item.exosuit;

import net.minecraft.world.item.Item.Properties;

public class TargetTrackerModuleItem extends EnergyUpgradeItem {
   public TargetTrackerModuleItem(Properties properties) {
      super(properties, EnergyUpgradeItem.EnergyConsumptionType.PER_TICK);
   }
}
