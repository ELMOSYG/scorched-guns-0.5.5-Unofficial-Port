package top.ribs.scguns.item.exosuit;

import net.minecraft.world.item.Item.Properties;

public class RabbitModuleItem extends EnergyUpgradeItem {
   public RabbitModuleItem(Properties properties) {
      super(properties, EnergyUpgradeItem.EnergyConsumptionType.PER_TICK);
   }
}
