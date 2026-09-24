package top.ribs.scguns.item.exosuit;

import net.minecraft.world.item.Item.Properties;

public class GasMaskModuleItem extends EnergyUpgradeItem {
   public GasMaskModuleItem(Properties properties) {
      super(properties, EnergyUpgradeItem.EnergyConsumptionType.PER_SECOND);
   }
}
