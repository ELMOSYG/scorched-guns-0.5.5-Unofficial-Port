package top.ribs.scguns.common.exosuit;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import top.ribs.scguns.item.animated.ExoSuitItem;
import top.ribs.scguns.item.exosuit.EnergyUpgradeItem;

@EventBusSubscriber(
   modid = "scguns",
   bus = Bus.GAME
)
public class ExoSuitFallDamageHandler {
   public ExoSuitFallDamageHandler() {
      super();
   }

   @SubscribeEvent
   public static void onLivingFall(LivingFallEvent event) {
      if (event.getEntity() instanceof Player player) {
         if (!player.level().isClientSide) {
            float totalFallDamageReduction = calculateTotalFallDamageReduction(player);
            if (totalFallDamageReduction > 0.0F) {
               float originalDamage = event.getDamageMultiplier();
               float reducedDamage = originalDamage * (1.0F - totalFallDamageReduction);
               reducedDamage = Math.max(0.0F, reducedDamage);
               event.setDamageMultiplier(reducedDamage);
            }
         }
      }
   }

   private static float calculateTotalFallDamageReduction(Player player) {
      float totalReduction = 0.0F;

      for (ItemStack armorStack : player.getArmorSlots()) {
         if (armorStack.getItem() instanceof ExoSuitItem) {
            totalReduction += getFallDamageReductionFromPiece(armorStack, player);
         }
      }

      return Math.min(totalReduction, 1.0F);
   }

   private static float getFallDamageReductionFromPiece(ItemStack armorStack, Player player) {
      float totalReduction = 0.0F;

      for (int slot = 0; slot < 4; slot++) {
         ItemStack upgradeItem = ExoSuitData.getUpgradeInSlot(armorStack, slot);
         if (!upgradeItem.isEmpty()) {
            ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(upgradeItem);
            if (upgrade != null) {
               ExoSuitUpgrade.Effects effects = upgrade.getEffects();
               if (canUpgradeFunction(player, upgrade, upgradeItem)) {
                  totalReduction += effects.getFallDamageReduction();
               }
            }
         }
      }

      return totalReduction;
   }

   private static boolean canUpgradeFunction(Player player, ExoSuitUpgrade upgrade, ItemStack upgradeItem) {
      String upgradeType = upgrade.getType();
      if (upgradeItem.getItem() instanceof EnergyUpgradeItem energyUpgrade) {
         boolean powerEnabled = ExoSuitPowerManager.isPowerEnabled(player, upgradeType);
         if (!powerEnabled) {
            return false;
         } else {
            return !energyUpgrade.canFunctionWithoutPower() ? ExoSuitPowerManager.canUpgradeFunction(player, upgradeType) : true;
         }
      } else {
         return true;
      }
   }
}
