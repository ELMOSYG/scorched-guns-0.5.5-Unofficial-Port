package top.ribs.scguns.common.exosuit;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import top.ribs.scguns.item.animated.ExoSuitItem;
import top.ribs.scguns.network.PacketHandler;
import top.ribs.scguns.network.message.S2CMessageSyncExoSuitUpgrades;
import top.ribs.scguns.network.message.S2CMessageSyncUpgradeRegistry;

@EventBusSubscriber(
   modid = "scguns",
   bus = Bus.GAME
)
public class ExoSuitEventHandler {
   private static int tickCounter = 0;
   private static final int UPDATE_FREQUENCY = 20;
   private static int syncCounter = 0;
   private static final int SYNC_INTERVAL = 200;

   public ExoSuitEventHandler() {
      super();
   }

   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent.Post event) {
      if (!event.getEntity().level().isClientSide) {
         tickCounter++;
         if (tickCounter >= 20) {
            tickCounter = 0;
            updatePlayerExoSuitEffects(event.getEntity());
         }

         syncCounter++;
         if (syncCounter >= 200) {
            syncCounter = 0;
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
               periodicExoSuitSync(serverPlayer);
            }
         }
      }
   }

   private static void periodicExoSuitSync(ServerPlayer player) {
      List<ServerPlayer> nearbyPlayers = player.serverLevel().getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(128.0));
      if (nearbyPlayers.size() > 1) {
         for (ItemStack armorStack : player.getArmorSlots()) {
            Item slot = armorStack.getItem();
            if (slot instanceof ExoSuitItem) {
               ExoSuitItem exosuit = (ExoSuitItem)slot;
               EquipmentSlot slotx = getSlotForArmorType(exosuit);
               CompoundTag upgradeData = ExoSuitData.getUpgradeData(armorStack);

               for (ServerPlayer nearbyPlayer : nearbyPlayers) {
                  if (nearbyPlayer != player) {
                     PacketHandler.getPlayChannel().sendToPlayer(() -> nearbyPlayer, new S2CMessageSyncExoSuitUpgrades(player.getUUID(), slotx, upgradeData));
                  }
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onPlayerLoggedIn(PlayerLoggedInEvent event) {
      if (!event.getEntity().level().isClientSide && event.getEntity() instanceof ServerPlayer serverPlayer) {
         updatePlayerExoSuitEffects(event.getEntity());
         Map<ResourceLocation, CompoundTag> upgradeData = ExoSuitUpgradeManager.serializeUpgrades();
         PacketHandler.getPlayChannel().sendToPlayer(() -> serverPlayer, new S2CMessageSyncUpgradeRegistry(upgradeData));
         syncAllExoSuitPiecesToPlayer(serverPlayer);
      }
   }

   private static void syncAllExoSuitPiecesToPlayer(ServerPlayer joiningPlayer) {
      for (ServerPlayer nearbyPlayer : joiningPlayer.serverLevel().getEntitiesOfClass(ServerPlayer.class, joiningPlayer.getBoundingBox().inflate(128.0))) {
         for (ItemStack armorStack : nearbyPlayer.getArmorSlots()) {
            if (armorStack.getItem() instanceof ExoSuitItem) {
               EquipmentSlot slot = getSlotForArmorType((ExoSuitItem)armorStack.getItem());
               CompoundTag upgradeData = ExoSuitData.getUpgradeData(armorStack);
               PacketHandler.getPlayChannel().sendToPlayer(() -> joiningPlayer, new S2CMessageSyncExoSuitUpgrades(nearbyPlayer.getUUID(), slot, upgradeData));
            }
         }
      }
   }

   private static EquipmentSlot getSlotForArmorType(ExoSuitItem exosuit) {
      return switch (exosuit.getType()) {
         case HELMET -> EquipmentSlot.HEAD;
         case CHESTPLATE -> EquipmentSlot.CHEST;
         case LEGGINGS -> EquipmentSlot.LEGS;
         case BOOTS -> EquipmentSlot.FEET;
         default -> throw new IncompatibleClassChangeError();
      };
   }

   @SubscribeEvent
   public static void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
      if (hasAnyExoSuitPiece(event.getEntity())) {
         ExoSuitEffectsHandler.removeExoSuitEffects(event.getEntity());
         ExoSuitNightVisionHandler.onPlayerLogout(event.getEntity());
      }

      ExoSuitPowerManager.cleanupPlayerData(event.getEntity().getUUID());
      ExoSuitEffectsHandler.cleanupPlayerData(event.getEntity().getUUID());
   }

   @SubscribeEvent
   public static void onLivingEquipmentChange(LivingEquipmentChangeEvent event) {
      if (!event.getEntity().level().isClientSide) {
         if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack var10 = event.getFrom();
            ItemStack to = event.getTo();
            boolean wasExoSuit = var10.getItem() instanceof ExoSuitItem;
            boolean isExoSuit = to.getItem() instanceof ExoSuitItem;
            if (wasExoSuit || isExoSuit) {
               List<ServerPlayer> nearbyPlayers = player.serverLevel().getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(128.0));
               CompoundTag upgradeData = isExoSuit ? ExoSuitData.getUpgradeData(to) : new CompoundTag();

               for (ServerPlayer nearbyPlayer : nearbyPlayers) {
                  PacketHandler.getPlayChannel()
                     .sendToPlayer(() -> nearbyPlayer, new S2CMessageSyncExoSuitUpgrades(player.getUUID(), event.getSlot(), upgradeData));
               }

               updatePlayerExoSuitEffects(player);
            }
         }
      }
   }

   @SubscribeEvent
   public static void onPlayerRespawn(PlayerRespawnEvent event) {
      if (!event.getEntity().level().isClientSide) {
         Objects.requireNonNull(event.getEntity().level().getServer()).execute(() -> updatePlayerExoSuitEffects(event.getEntity()));
      }
   }

   private static void updatePlayerExoSuitEffects(Player player) {
      if (!player.level().isClientSide) {
         boolean hasExoSuit = hasAnyExoSuitPiece(player);
         if (hasExoSuit) {
            ExoSuitEffectsHandler.applyExoSuitEffects(player);
            initializePowerStatesIfNeeded(player);
         } else {
            ExoSuitEffectsHandler.removeExoSuitEffects(player);
            ExoSuitNightVisionHandler.onPlayerLogout(player);
         }
      }
   }

   private static boolean hasAnyExoSuitPiece(Player player) {
      for (ItemStack armorStack : player.getArmorSlots()) {
         if (armorStack.getItem() instanceof ExoSuitItem) {
            return true;
         }
      }

      return false;
   }

   private static void initializePowerStatesIfNeeded(Player player) {
      if (hasUpgradeType(player, "hud") && !hasPowerState(player, "hud")) {
         ExoSuitPowerManager.setPowerEnabled(player, "hud", false);
      }

      if (hasUpgradeType(player, "mobility") && !hasPowerState(player, "mobility")) {
         ExoSuitPowerManager.setPowerEnabled(player, "mobility", false);
      }
   }

   private static boolean hasUpgradeType(Player player, String upgradeType) {
      for (ItemStack armorStack : player.getArmorSlots()) {
         if (armorStack.getItem() instanceof ExoSuitItem) {
            for (int slot = 0; slot < 4; slot++) {
               ItemStack upgradeItem = ExoSuitData.getUpgradeInSlot(armorStack, slot);
               if (!upgradeItem.isEmpty()) {
                  ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(upgradeItem);
                  if (upgrade != null && upgrade.getType().equals(upgradeType)) {
                     return true;
                  }
               }
            }
         }
      }

      return false;
   }

   private static boolean hasPowerState(Player player, String upgradeType) {
      return ExoSuitPowerManager.isPowerEnabled(player, upgradeType);
   }
}
