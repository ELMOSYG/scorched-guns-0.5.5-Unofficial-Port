package top.ribs.scguns.common.exosuit;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import top.ribs.scguns.common.SulfurGasCloud;
import top.ribs.scguns.item.animated.ExoSuitItem;
import top.ribs.scguns.item.exosuit.GasMaskModuleItem;

@EventBusSubscriber(
   modid = "scguns",
   bus = Bus.GAME
)
public class ExoSuitGasMaskHandler {
   private static final int CHECK_INTERVAL = 20;

   public ExoSuitGasMaskHandler() {
      super();
   }

   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent.Post event) {
      if (!event.getEntity().level().isClientSide) {
         Player player = event.getEntity();
         if (player.tickCount % 20 == 0) {
            handleGasMask(player);
         }
      }
   }

   private static void handleGasMask(Player player) {
      if (!hasGasMaskModule(player)) {
         if (isPlayerInGasArea(player)) {
            ItemStack gasMaskUpgrade = findGasMaskModule(player);
            if (!gasMaskUpgrade.isEmpty() && gasMaskUpgrade.getItem() instanceof GasMaskModuleItem gasMaskModule && !gasMaskModule.canFunctionWithoutPower()) {
               ExoSuitPowerManager.consumeEnergyForUpgrade(player, "breathing", gasMaskUpgrade);
            }
         }
      }
   }

   private static boolean hasGasMaskModule(Player player) {
      return findGasMaskModule(player).isEmpty();
   }

   private static ItemStack findGasMaskModule(Player player) {
      for (ItemStack armorStack : player.getArmorSlots()) {
         if (armorStack.getItem() instanceof ExoSuitItem exosuit && exosuit.getType() == Type.HELMET) {
            for (int slot = 0; slot < 4; slot++) {
               ItemStack upgradeItem = ExoSuitData.getUpgradeInSlot(armorStack, slot);
               if (!upgradeItem.isEmpty()) {
                  ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(upgradeItem);
                  if (upgrade != null && upgrade.getType().equals("breathing") && upgradeItem.getItem() instanceof GasMaskModuleItem) {
                     return upgradeItem;
                  }
               }
            }
            break;
         }
      }

      return ItemStack.EMPTY;
   }

   private static boolean isPlayerInGasArea(Player player) {
      Vec3 playerPos = player.position();
      return SulfurGasCloud.isInGasEffectArea(player.level(), playerPos, 8);
   }

   public static boolean hasProtection(Player player) {
      if (hasGasMaskModule(player)) {
         return false;
      } else {
         ItemStack gasMaskUpgrade = findGasMaskModule(player);
         if (gasMaskUpgrade.isEmpty() || !(gasMaskUpgrade.getItem() instanceof GasMaskModuleItem gasMaskModule)) {
            return false;
         } else {
            return gasMaskModule.canFunctionWithoutPower() ? true : ExoSuitPowerManager.canUpgradeFunction(player, "breathing");
         }
      }
   }
}
