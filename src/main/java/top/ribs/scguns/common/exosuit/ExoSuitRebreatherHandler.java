package top.ribs.scguns.common.exosuit;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem.Type;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import top.ribs.scguns.item.animated.ExoSuitItem;
import top.ribs.scguns.item.exosuit.RebreatherModuleItem;

@EventBusSubscriber(
   modid = "scguns",
   bus = Bus.GAME
)
public class ExoSuitRebreatherHandler {
   private static final int REFRESH_INTERVAL = 160;

   public ExoSuitRebreatherHandler() {
      super();
   }

   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent.Post event) {
      if (!event.getEntity().level().isClientSide) {
         Player player = event.getEntity();
         if (player.tickCount % 20 == 0) {
            handleRebreatherEnergy(player);
         }
      }
   }

   private static void handleRebreatherEnergy(Player player) {
      boolean inWater = player.isInWater() || player.isUnderWater();
      if (inWater) {
         if (hasRebreatherModule(player)) {
            if (ExoSuitPowerManager.canConsumeEnergy(player, "breathing", 160)) {
               if (ExoSuitPowerManager.canUpgradeFunction(player, "breathing")) {
                  ItemStack helmetUpgrade = findRebreatherModule(player);
                  if (!helmetUpgrade.isEmpty()
                     && helmetUpgrade.getItem() instanceof RebreatherModuleItem rebreatherModule
                     && !rebreatherModule.canFunctionWithoutPower()) {
                     ExoSuitPowerManager.consumeEnergyForUpgrade(player, "breathing", helmetUpgrade);
                  }
               }
            }
         }
      }
   }

   private static boolean hasRebreatherModule(Player player) {
      return !findRebreatherModule(player).isEmpty();
   }

   private static ItemStack findRebreatherModule(Player player) {
      for (ItemStack armorStack : player.getArmorSlots()) {
         if (armorStack.getItem() instanceof ExoSuitItem exosuit && exosuit.getType() == Type.HELMET) {
            for (int slot = 0; slot < 4; slot++) {
               ItemStack upgradeItem = ExoSuitData.getUpgradeInSlot(armorStack, slot);
               if (!upgradeItem.isEmpty()) {
                  ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(upgradeItem);
                  if (upgrade != null && upgrade.getType().equals("breathing") && upgradeItem.getItem() instanceof RebreatherModuleItem) {
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
