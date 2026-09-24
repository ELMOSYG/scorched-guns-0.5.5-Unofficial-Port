package top.ribs.scguns.item.exosuit;

import net.minecraft.world.item.Item.Properties;
import top.ribs.scguns.common.exosuit.ExoSuitUpgrade;
import top.ribs.scguns.common.exosuit.ExoSuitUpgradeManager;

public class EnergyUpgradeItem extends DamageableUpgradeItem {
   private final EnergyUpgradeItem.EnergyConsumptionType consumptionType;

   public EnergyUpgradeItem(Properties properties) {
      this(properties, EnergyUpgradeItem.EnergyConsumptionType.PER_TICK);
   }

   public EnergyUpgradeItem(Properties properties, EnergyUpgradeItem.EnergyConsumptionType consumptionType) {
      super(properties);
      this.consumptionType = consumptionType;
   }

   public int getEnergyConsumption() {
      ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(this);
      return upgrade != null ? (int)upgrade.getEffects().getEnergyUse() : 10;
   }

   public EnergyUpgradeItem.EnergyConsumptionType getConsumptionType() {
      return this.consumptionType;
   }

   public boolean canFunctionWithoutPower() {
      return false;
   }

   public static enum EnergyConsumptionType {
      PER_TICK,
      PER_USE,
      PER_SECOND,
      ACTIVATION;

      private EnergyConsumptionType() {
      }
   }
}
