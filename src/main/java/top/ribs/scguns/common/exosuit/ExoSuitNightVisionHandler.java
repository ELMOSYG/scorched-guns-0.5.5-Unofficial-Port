package top.ribs.scguns.common.exosuit;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem.Type;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import top.ribs.scguns.item.animated.ExoSuitItem;
import top.ribs.scguns.item.exosuit.NightVisionModuleItem;

@EventBusSubscriber(
   modid = "scguns",
   bus = Bus.GAME
)
public class ExoSuitNightVisionHandler {
   private static final int REFRESH_INTERVAL = 150;

   public ExoSuitNightVisionHandler() {
      super();
   }

   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent.Post event) {
      if (!event.getEntity().level().isClientSide) {
         Player player = event.getEntity();
         if (player.tickCount % 10 == 0) {
            handleNightVisionEnergy(player);
         }
      }
   }

   private static void handleNightVisionEnergy(Player player) {
      if (ExoSuitPowerManager.isPowerEnabled(player, "hud")) {
         if (hasNightVisionModule(player)) {
            if (ExoSuitPowerManager.canConsumeEnergy(player, "hud", 150)) {
               if (ExoSuitPowerManager.canUpgradeFunction(player, "hud")) {
                  ItemStack helmetUpgrade = findNightVisionModule(player);
                  if (!helmetUpgrade.isEmpty()
                     && helmetUpgrade.getItem() instanceof NightVisionModuleItem nightVisionModule
                     && !nightVisionModule.canFunctionWithoutPower()) {
                     ExoSuitPowerManager.consumeEnergyForUpgrade(player, "hud", helmetUpgrade);
                  }
               }
            }
         }
      }
   }

   private static boolean hasNightVisionModule(Player player) {
      return !findNightVisionModule(player).isEmpty();
   }

   private static ItemStack findNightVisionModule(Player player) {
      for (ItemStack armorStack : player.getArmorSlots()) {
         if (armorStack.getItem() instanceof ExoSuitItem exosuit && exosuit.getType() == Type.HELMET) {
            for (int slot = 0; slot < 4; slot++) {
               ItemStack upgradeItem = ExoSuitData.getUpgradeInSlot(armorStack, slot);
               if (!upgradeItem.isEmpty()) {
                  ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(upgradeItem);
                  if (upgrade != null && upgrade.getType().equals("hud") && upgradeItem.getItem() instanceof NightVisionModuleItem) {
                     return upgradeItem;
                  }
               }
            }
            break;
         }
      }

      return ItemStack.EMPTY;
   }

   public static void onPlayerLogout(Player player) {
   }
}
