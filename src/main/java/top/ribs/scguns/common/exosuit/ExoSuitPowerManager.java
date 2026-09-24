package top.ribs.scguns.common.exosuit;


import top.ribs.scguns.util.NbtHelper;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem.Type;
import top.ribs.scguns.item.animated.ExoSuitItem;
import top.ribs.scguns.item.exosuit.EnergyUpgradeItem;

public class ExoSuitPowerManager {
   private static final String POWER_STATES_TAG = "ExoSuitPowerStates";
   private static final Map<UUID, Map<String, Integer>> playerCooldowns = new HashMap<>();

   public ExoSuitPowerManager() {
      super();
   }

   public static boolean consumeEnergy(Player player, String upgradeType, int energyRequired) {
      ItemStack chestplate = getEquippedChestplate(player);
      if (chestplate.isEmpty()) {
         return false;
      } else {
         ItemStack powerCore = findPowerCore(chestplate);
         if (powerCore.isEmpty()) {
            return false;
         } else {
            CompoundTag powerCoreTag = NbtHelper.getOrCreateTag(powerCore);
            int currentEnergy = powerCoreTag.getInt("Energy");
            if (currentEnergy >= energyRequired) {
               powerCoreTag.putInt("Energy", currentEnergy - energyRequired);
               updatePowerCoreInChestplate(chestplate, powerCore);
               return true;
            } else {
               sendPowerShortageNotification(player, upgradeType);
               return false;
            }
         }
      }
   }

   private static void updatePowerCoreInChestplate(ItemStack chestplate, ItemStack updatedPowerCore) {
      for (int slot = 0; slot < 4; slot++) {
         ItemStack upgradeItem = ExoSuitData.getUpgradeInSlot(chestplate, slot);
         if (!upgradeItem.isEmpty()) {
            ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(upgradeItem);
            if (upgrade != null && upgrade.getType().equals("power_core")) {
               CompoundTag upgradeData = ExoSuitData.getUpgradeData(chestplate);
               if (upgradeData.contains("Upgrades")) {
                  ListTag upgradeList = upgradeData.getList("Upgrades", 10);

                  for (int i = 0; i < upgradeList.size(); i++) {
                     CompoundTag slotTag = upgradeList.getCompound(i);
                     if (slotTag.getInt("Slot") == slot) {
                        slotTag.put("Item", top.ribs.scguns.util.NbtHelper.tagFromItem(updatedPowerCore));
                        break;
                     }
                  }

                  ExoSuitData.setUpgradeData(chestplate, upgradeData);
               }
               break;
            }
         }
      }
   }

   public static boolean consumeEnergyForUpgrade(Player player, String upgradeType, ItemStack upgradeItem) {
      ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(upgradeItem);
      if (upgrade == null) {
         return false;
      } else {
         int energyRequired = (int)upgrade.getEffects().getEnergyUse();
         return consumeEnergy(player, upgradeType, energyRequired);
      }
   }

   public static boolean canUpgradeFunction(Player player, String upgradeType) {
      ItemStack armorPiece = getArmorPieceForUpgradeType(player, upgradeType);
      if (armorPiece.isEmpty()) {
         return false;
      } else {
         ItemStack upgradeItem = findUpgradeByType(armorPiece, upgradeType);
         if (upgradeItem.isEmpty()) {
            return false;
         } else if (upgradeItem.getItem() instanceof EnergyUpgradeItem energyUpgrade) {
            if (energyUpgrade.canFunctionWithoutPower()) {
               return true;
            } else {
               ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(upgradeItem);
               if (upgrade == null) {
                  return false;
               } else {
                  int energyRequired = (int)upgrade.getEffects().getEnergyUse();
                  ItemStack chestplate = getEquippedChestplate(player);
                  if (chestplate.isEmpty()) {
                     return false;
                  } else {
                     ItemStack powerCore = findPowerCore(chestplate);
                     if (powerCore.isEmpty()) {
                        return false;
                     } else {
                        CompoundTag powerCoreTag = NbtHelper.getTag(powerCore);
                        int currentEnergy = powerCoreTag != null ? powerCoreTag.getInt("Energy") : 0;
                        return currentEnergy >= energyRequired;
                     }
                  }
               }
            }
         } else {
            return true;
         }
      }
   }

   public static boolean isPowerEnabled(Player player, String upgradeType) {
      ItemStack armorPiece = getArmorPieceForUpgradeType(player, upgradeType);
      if (armorPiece.isEmpty()) {
         return false;
      } else {
         CompoundTag powerStates = getPowerStates(armorPiece);
         return powerStates.getBoolean(upgradeType);
      }
   }

   public static void setPowerEnabled(Player player, String upgradeType, boolean enabled) {
      ItemStack armorPiece = getArmorPieceForUpgradeType(player, upgradeType);
      if (!armorPiece.isEmpty()) {
         CompoundTag powerStates = getPowerStates(armorPiece);
         powerStates.putBoolean(upgradeType, enabled);
         setPowerStates(armorPiece, powerStates);
      }
   }

   public static boolean togglePower(Player player, String upgradeType) {
      boolean currentState = isPowerEnabled(player, upgradeType);
      boolean newState = !currentState;
      setPowerEnabled(player, upgradeType, newState);
      return newState;
   }

   public static boolean canConsumeEnergy(Player player, String upgradeType, int cooldownTicks) {
      UUID playerId = player.getUUID();
      Map<String, Integer> upgradeCooldowns = playerCooldowns.computeIfAbsent(playerId, k -> new HashMap<>());
      int lastConsumption = upgradeCooldowns.getOrDefault(upgradeType, 0);
      int currentTick = player.tickCount;
      if (currentTick - lastConsumption >= cooldownTicks) {
         upgradeCooldowns.put(upgradeType, currentTick);
         return false;
      } else {
         return true;
      }
   }

   private static ItemStack getArmorPieceForUpgradeType(Player player, String upgradeType) {
      return switch (upgradeType) {
         case "hud", "breathing", "night_vision" -> getEquippedHelmet(player);
         case "pauldron", "power_core", "utility" -> getEquippedChestplate(player);
         case "knee_guard" -> getEquippedLeggings(player);
         case "mobility" -> getEquippedBoots(player);
         default -> ItemStack.EMPTY;
      };
   }

   private static ItemStack getEquippedHelmet(Player player) {
      for (ItemStack armorStack : player.getArmorSlots()) {
         if (armorStack.getItem() instanceof ExoSuitItem exosuit && exosuit.getType() == Type.HELMET) {
            return armorStack;
         }
      }

      return ItemStack.EMPTY;
   }

   private static ItemStack getEquippedChestplate(Player player) {
      for (ItemStack armorStack : player.getArmorSlots()) {
         if (armorStack.getItem() instanceof ExoSuitItem exosuit && exosuit.getType() == Type.CHESTPLATE) {
            return armorStack;
         }
      }

      return ItemStack.EMPTY;
   }

   private static ItemStack getEquippedLeggings(Player player) {
      for (ItemStack armorStack : player.getArmorSlots()) {
         if (armorStack.getItem() instanceof ExoSuitItem exosuit && exosuit.getType() == Type.LEGGINGS) {
            return armorStack;
         }
      }

      return ItemStack.EMPTY;
   }

   private static ItemStack getEquippedBoots(Player player) {
      for (ItemStack armorStack : player.getArmorSlots()) {
         if (armorStack.getItem() instanceof ExoSuitItem exosuit && exosuit.getType() == Type.BOOTS) {
            return armorStack;
         }
      }

      return ItemStack.EMPTY;
   }

   private static ItemStack findPowerCore(ItemStack chestplate) {
      for (int slot = 0; slot < 4; slot++) {
         ItemStack upgradeItem = ExoSuitData.getUpgradeInSlot(chestplate, slot);
         if (!upgradeItem.isEmpty()) {
            ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(upgradeItem);
            if (upgrade != null && upgrade.getType().equals("power_core")) {
               return upgradeItem;
            }
         }
      }

      return ItemStack.EMPTY;
   }

   private static ItemStack findUpgradeByType(ItemStack armorPiece, String upgradeType) {
      for (int slot = 0; slot < 4; slot++) {
         ItemStack upgradeItem = ExoSuitData.getUpgradeInSlot(armorPiece, slot);
         if (!upgradeItem.isEmpty()) {
            ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(upgradeItem);
            if (upgrade != null && upgrade.getType().equals(upgradeType)) {
               return upgradeItem;
            }
         }
      }

      return ItemStack.EMPTY;
   }

   private static CompoundTag getPowerStates(ItemStack armorPiece) {
      CompoundTag upgradeData = ExoSuitData.getUpgradeData(armorPiece);
      return upgradeData.getCompound("ExoSuitPowerStates");
   }

   private static void setPowerStates(ItemStack armorPiece, CompoundTag powerStates) {
      CompoundTag upgradeData = ExoSuitData.getUpgradeData(armorPiece);
      upgradeData.put("ExoSuitPowerStates", powerStates);
      ExoSuitData.setUpgradeData(armorPiece, upgradeData);
   }

   public static void cleanupPlayerData(UUID playerId) {
      playerCooldowns.remove(playerId);
   }

   private static void sendPowerShortageNotification(Player player, String upgradeType) {
      if (!canConsumeEnergy(player, upgradeType + "_notification", 800)) {
         Component feedbackMessage = Component.translatable("exosuit.message.prefix")
            .withStyle(ChatFormatting.GOLD)
            .append(
               Component.translatable("exosuit.message.power_shortage", new Object[]{Component.translatable("exosuit.upgrade." + upgradeType)})
                  .withStyle(ChatFormatting.RED)
            );
         player.sendSystemMessage(feedbackMessage);
         player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.3F, 0.6F);
      }
   }
}
