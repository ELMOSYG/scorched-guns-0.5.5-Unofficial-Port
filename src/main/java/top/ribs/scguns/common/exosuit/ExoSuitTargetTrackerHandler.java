package top.ribs.scguns.common.exosuit;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import top.ribs.scguns.item.animated.ExoSuitItem;
import top.ribs.scguns.item.exosuit.TargetTrackerModuleItem;

@EventBusSubscriber(
   modid = "scguns",
   bus = Bus.GAME
)
public class ExoSuitTargetTrackerHandler {
   private static final int REFRESH_INTERVAL = 50;
   private static final double DETECTION_RADIUS = 16.0;
   private static final int GLOW_DURATION = 120;
   private static final Map<UUID, Long> playerLastUpdate = new HashMap<>();

   public ExoSuitTargetTrackerHandler() {
      super();
   }

   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent.Post event) {
      if (!event.getEntity().level().isClientSide) {
         Player player = event.getEntity();
         if (player.tickCount % 10 == 0) {
            handleTargetTracker(player);
         }
      }
   }

   private static void handleTargetTracker(Player player) {
      boolean targetTrackerEnabled = ExoSuitPowerManager.isPowerEnabled(player, "hud");
      if (!targetTrackerEnabled) {
         removeEntityHighlights(player);
      } else if (!hasTargetTrackerModule(player)) {
         removeEntityHighlights(player);
      } else if (ExoSuitPowerManager.canConsumeEnergy(player, "hud", 50)) {
         if (!ExoSuitPowerManager.canUpgradeFunction(player, "hud")) {
            removeEntityHighlights(player);
         } else {
            ItemStack helmetUpgrade = findTargetTrackerModule(player);
            if (!helmetUpgrade.isEmpty()
               && helmetUpgrade.getItem() instanceof TargetTrackerModuleItem targetTrackerModule
               && !targetTrackerModule.canFunctionWithoutPower()
               && !ExoSuitPowerManager.consumeEnergyForUpgrade(player, "hud", helmetUpgrade)) {
               removeEntityHighlights(player);
            } else {
               highlightNearbyEntities(player);
               playerLastUpdate.put(player.getUUID(), player.level().getGameTime());
            }
         }
      }
   }

   private static void highlightNearbyEntities(Player player) {
      AABB detectionArea = new AABB(
         player.getX() - 16.0,
         player.getY() - 16.0,
         player.getZ() - 16.0,
         player.getX() + 16.0,
         player.getY() + 16.0,
         player.getZ() + 16.0
      );

      for (LivingEntity entity : player.level()
         .getEntitiesOfClass(LivingEntity.class, detectionArea, entityx -> entityx != player && entityx.isAlive() && !entityx.isInvisible())) {
         if (!entity.hasEffect(MobEffects.GLOWING)) {
            entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0, false, false, false));
         } else {
            MobEffectInstance currentGlow = entity.getEffect(MobEffects.GLOWING);
            if (currentGlow != null && currentGlow.getDuration() < 60) {
               entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0, false, false, false));
            }
         }
      }
   }

   private static void removeEntityHighlights(Player player) {
      playerLastUpdate.remove(player.getUUID());
   }

   public static boolean hasTargetTrackerModule(Player player) {
      return !findTargetTrackerModule(player).isEmpty();
   }

   public static boolean isTargetTrackerActive(Player player) {
      return hasTargetTrackerModule(player) && ExoSuitPowerManager.isPowerEnabled(player, "hud") && ExoSuitPowerManager.canUpgradeFunction(player, "hud");
   }

   private static ItemStack findTargetTrackerModule(Player player) {
      for (ItemStack armorStack : player.getArmorSlots()) {
         if (armorStack.getItem() instanceof ExoSuitItem exosuit && exosuit.getType() == Type.HELMET) {
            for (int slot = 0; slot < 4; slot++) {
               ItemStack upgradeItem = ExoSuitData.getUpgradeInSlot(armorStack, slot);
               if (!upgradeItem.isEmpty()) {
                  ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(upgradeItem);
                  if (upgrade != null && upgrade.getType().equals("hud") && upgradeItem.getItem() instanceof TargetTrackerModuleItem) {
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
      playerLastUpdate.remove(player.getUUID());
   }

   public static void onPlayerDeath(Player player) {
      removeEntityHighlights(player);
   }
}
