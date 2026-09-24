package top.ribs.scguns.item.exosuit;

import net.minecraft.world.item.Item.Properties;

public class RebreatherModuleItem extends EnergyUpgradeItem {
   public RebreatherModuleItem(Properties properties) {
      super(properties, EnergyUpgradeItem.EnergyConsumptionType.PER_SECOND);
   }
}
