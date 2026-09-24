package top.ribs.scguns.item.exosuit;

import net.minecraft.world.item.Item.Properties;

public class JetpackModuleItem extends EnergyUpgradeItem {
   public JetpackModuleItem(Properties properties) {
      super(properties, EnergyUpgradeItem.EnergyConsumptionType.PER_SECOND);
   }
}
