package top.ribs.scguns.common.exosuit;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import top.ribs.scguns.item.animated.ExoSuitItem;
import top.ribs.scguns.item.exosuit.RabbitModuleItem;

@EventBusSubscriber(
   modid = "scguns",
   bus = Bus.GAME
)
public class ExoSuitRabbitHandler {
   private static final int REFRESH_INTERVAL = 100;
   private static final double MOVEMENT_THRESHOLD = 0.01;
   private static final Map<UUID, Vec3> previousPositions = new HashMap<>();

   public ExoSuitRabbitHandler() {
      super();
   }

   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent.Post event) {
      if (!event.getEntity().level().isClientSide) {
         Player player = event.getEntity();
         if (player.tickCount % 20 == 0) {
            handleRabbitModuleEnergy(player);
         }
      }
   }

   private static void handleRabbitModuleEnergy(Player player) {
      if (ExoSuitPowerManager.isPowerEnabled(player, "mobility")) {
         if (hasRabbitModule(player)) {
            if (isPlayerMoving(player)) {
               if (ExoSuitPowerManager.canConsumeEnergy(player, "mobility", 100)) {
                  if (ExoSuitPowerManager.canUpgradeFunction(player, "mobility")) {
                     ItemStack rabbitUpgrade = findRabbitModule(player);
                     if (!rabbitUpgrade.isEmpty()
                        && rabbitUpgrade.getItem() instanceof RabbitModuleItem rabbitModule
                        && !rabbitModule.canFunctionWithoutPower()) {
                        ExoSuitPowerManager.consumeEnergyForUpgrade(player, "mobility", rabbitUpgrade);
                     }
                  }
               }
            }
         }
      }
   }

   private static boolean isPlayerMoving(Player player) {
      UUID playerId = player.getUUID();
      Vec3 currentPos = player.position();
      Vec3 previousPos = previousPositions.get(playerId);
      previousPositions.put(playerId, currentPos);
      if (previousPos == null) {
         return false;
      } else {
         double distanceMoved = currentPos.distanceTo(previousPos);
         boolean positionChanged = distanceMoved > 0.01;
         boolean hasVelocity = player.getDeltaMovement().lengthSqr() > 1.0E-4;
         boolean isWalking = player.isSprinting() || player.isSwimming() || player.isCrouching() || player.zza != 0.0F || player.xxa != 0.0F;
         return positionChanged || hasVelocity || isWalking;
      }
   }

   private static boolean hasRabbitModule(Player player) {
      return !findRabbitModule(player).isEmpty();
   }

   private static ItemStack findRabbitModule(Player player) {
      for (ItemStack armorStack : player.getArmorSlots()) {
         if (armorStack.getItem() instanceof ExoSuitItem exosuit && exosuit.getType() == Type.BOOTS) {
            for (int slot = 0; slot < 4; slot++) {
               ItemStack upgradeItem = ExoSuitData.getUpgradeInSlot(armorStack, slot);
               if (!upgradeItem.isEmpty()) {
                  ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(upgradeItem);
                  if (upgrade != null && upgrade.getType().equals("mobility") && upgradeItem.getItem() instanceof RabbitModuleItem) {
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
      previousPositions.remove(player.getUUID());
   }
}
