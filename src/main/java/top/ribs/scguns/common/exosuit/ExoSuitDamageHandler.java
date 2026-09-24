package top.ribs.scguns.common.exosuit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import top.ribs.scguns.item.animated.ExoSuitItem;
import top.ribs.scguns.item.exosuit.DamageableUpgradeItem;

@EventBusSubscriber(
   modid = "scguns",
   bus = Bus.GAME
)
public class ExoSuitDamageHandler {
   private static final Random RANDOM = new Random();
   private static final float PLATING_ABSORPTION = 0.8F;
   private static final float COMPONENT_ABSORPTION = 0.3F;
   private static final float EXOSUIT_ABSORPTION = 0.1F;
   private static final long DURABILITY_DAMAGE_COOLDOWN = 1000L;
   private static final Map<UUID, Long> lastDurabilityDamage = new HashMap<>();
   private static final long CONTINUOUS_DAMAGE_COOLDOWN = 2000L;
   private static final String[] CONTINUOUS_DAMAGE_SOURCES = new String[]{"slime", "magmaCube", "mob", "sweetBerryBush", "cactus", "hotFloor"};

   public ExoSuitDamageHandler() {
      super();
   }

   @SubscribeEvent(
      priority = EventPriority.HIGH
   )
   public static void onLivingAttack(LivingIncomingDamageEvent event) {
      if (event.getEntity() instanceof Player player) {
         if (!player.level().isClientSide) {
            List<ItemStack> exoSuitPieces = getEquippedExoSuitPieces(player);
            if (!exoSuitPieces.isEmpty()) {
               UUID playerUUID = player.getUUID();
               long currentTime = System.currentTimeMillis();
               long cooldownTime = isContinuousDamageSource(event.getSource().getMsgId()) ? 2000L : 1000L;
               Long lastDamageTime = lastDurabilityDamage.get(playerUUID);
               if (lastDamageTime == null || currentTime - lastDamageTime >= cooldownTime) {
                  lastDurabilityDamage.put(playerUUID, currentTime);
                  cleanupOldEntries(currentTime);
                  float damageToDistribute = event.getAmount();
                  boolean componentsChanged = false;

                  for (ItemStack exoSuitPiece : exoSuitPieces) {
                     float remainingDamage = distributeDamageToComponents(exoSuitPiece, damageToDistribute / (float)exoSuitPieces.size());
                     if (remainingDamage != damageToDistribute / (float)exoSuitPieces.size()) {
                        componentsChanged = true;
                     }

                     if (RANDOM.nextFloat() < 0.1F) {
                        exoSuitPiece.setDamageValue(exoSuitPiece.getDamageValue() + 1);
                     }
                  }

                  if (componentsChanged) {
                     player.level().getServer().execute(() -> ExoSuitEffectsHandler.applyExoSuitEffects(player));
                  }
               }
            }
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOW
   )
   public static void onLivingDamage(LivingDamageEvent.Pre event) {
      if (event.getEntity() instanceof Player player) {
         if (!player.level().isClientSide) {
            List<ItemStack> exoSuitPieces = getEquippedExoSuitPieces(player);
            if (!exoSuitPieces.isEmpty()) {
               float totalDamageReduction = calculatePlatingDamageReduction(exoSuitPieces, event.getNewDamage());
               if (totalDamageReduction > 0.0F) {
                  float newDamage = Math.max(0.0F, event.getNewDamage() - totalDamageReduction);
                  event.setNewDamage(newDamage);
               }
            }
         }
      }
   }

   private static boolean isContinuousDamageSource(String damageSourceId) {
      if (damageSourceId == null) {
         return false;
      } else {
         String lowerSource = damageSourceId.toLowerCase();

         for (String continuousSource : CONTINUOUS_DAMAGE_SOURCES) {
            if (lowerSource.contains(continuousSource)) {
               return true;
            }
         }

         return false;
      }
   }

   private static void cleanupOldEntries(long currentTime) {
      if (RANDOM.nextInt(100) == 0) {
         lastDurabilityDamage.entrySet().removeIf(entry -> currentTime - entry.getValue() > Math.max(1000L, 2000L) * 2L);
      }
   }

   private static float calculatePlatingDamageReduction(List<ItemStack> exoSuitPieces, float damage) {
      float totalReduction = 0.0F;
      int functionalPlatingCount = 0;

      for (ItemStack exoSuitPiece : exoSuitPieces) {
         List<ItemStack> upgradeItems = getUpgradeItems(exoSuitPiece);
         ItemStack plating = findUpgradeByType(upgradeItems, "plating");
         if (plating != null && isUpgradeFunctional(plating)) {
            functionalPlatingCount++;
         }
      }

      if (functionalPlatingCount > 0) {
         float baseReduction = damage * 0.15F;
         totalReduction = baseReduction * (float)Math.min(functionalPlatingCount, 4);
         if (functionalPlatingCount > 1) {
            totalReduction *= 0.8F + 0.2F / (float)functionalPlatingCount;
         }
      }

      return totalReduction;
   }

   private static List<ItemStack> getEquippedExoSuitPieces(Player player) {
      List<ItemStack> exoSuitPieces = new ArrayList<>();

      for (ItemStack armorStack : player.getArmorSlots()) {
         if (armorStack.getItem() instanceof ExoSuitItem) {
            exoSuitPieces.add(armorStack);
         }
      }

      return exoSuitPieces;
   }

   private static float distributeDamageToComponents(ItemStack exoSuitPiece, float incomingDamage) {
      float remainingDamage = incomingDamage;
      List<ItemStack> upgradeItems = getUpgradeItems(exoSuitPiece);
      if (upgradeItems.isEmpty()) {
         return incomingDamage;
      } else {
         boolean needsUpdate = false;
         ItemStack plating = findUpgradeByType(upgradeItems, "plating");
         if (plating != null && !plating.isEmpty() && isUpgradeDamageable(plating) && !isUpgradeBroken(plating)) {
            float absorbedDamage = incomingDamage * 0.8F;
            damageUpgradeItem(plating, (int)Math.ceil((double)(absorbedDamage * 0.1F)));
            remainingDamage = incomingDamage - absorbedDamage;
            if (isUpgradeBroken(plating)) {
               needsUpdate = true;
            }
         }

         int nonPlatingComponents = 0;

         for (ItemStack upgrade : upgradeItems) {
            if (upgrade != plating && !upgrade.isEmpty()) {
               ExoSuitUpgrade upgradeData = ExoSuitUpgradeManager.getUpgradeForItem(upgrade);
               if (upgradeData != null && !upgradeData.getType().equals("plating")) {
                  nonPlatingComponents++;
               }
            }
         }

         if (nonPlatingComponents > 0 && remainingDamage > 0.0F) {
            for (ItemStack upgradex : upgradeItems) {
               if (upgradex != plating && !upgradex.isEmpty() && remainingDamage > 0.0F) {
                  ExoSuitUpgrade upgradeData = ExoSuitUpgradeManager.getUpgradeForItem(upgradex);
                  if (upgradeData != null && !upgradeData.getType().equals("plating") && isUpgradeDamageable(upgradex) && !isUpgradeBroken(upgradex)) {
                     float componentDamage = remainingDamage * 0.3F * (1.0F / (float)nonPlatingComponents);
                     damageUpgradeItem(upgradex, (int)Math.ceil((double)(componentDamage * 0.05F)));
                     remainingDamage -= componentDamage;
                     if (isUpgradeBroken(upgradex)) {
                        needsUpdate = true;
                     }
                  }
               }
            }
         }

         if (needsUpdate) {
            removeBrokenComponents(exoSuitPiece);
         }

         return Math.max(0.0F, remainingDamage);
      }
   }

   private static List<ItemStack> getUpgradeItems(ItemStack exoSuitPiece) {
      List<ItemStack> upgrades = new ArrayList<>();

      for (int slot = 0; slot < 4; slot++) {
         ItemStack upgradeItem = ExoSuitData.getUpgradeInSlot(exoSuitPiece, slot);
         if (!upgradeItem.isEmpty()) {
            upgrades.add(upgradeItem);
         }
      }

      return upgrades;
   }

   private static ItemStack findUpgradeByType(List<ItemStack> upgrades, String type) {
      for (ItemStack upgrade : upgrades) {
         ExoSuitUpgrade upgradeData = ExoSuitUpgradeManager.getUpgradeForItem(upgrade);
         if (upgradeData != null && upgradeData.getType().equals(type)) {
            return upgrade;
         }
      }

      return null;
   }

   private static void damageUpgradeItem(ItemStack upgradeItem, int damage) {
      if (isUpgradeDamageable(upgradeItem) && damage > 0) {
         if (upgradeItem.getItem() instanceof DamageableUpgradeItem damageableUpgrade) {
            damageableUpgrade.onUpgradeDamaged(upgradeItem, damage);
         } else if (upgradeItem.isDamageableItem()) {
            int newDamage = upgradeItem.getDamageValue() + damage;
            int maxDamage = upgradeItem.getMaxDamage();
            upgradeItem.setDamageValue(Math.min(newDamage, maxDamage));
            if (upgradeItem.getDamageValue() >= maxDamage && maxDamage > 0) {
            }
         }
      }
   }

   private static boolean isUpgradeFunctional(ItemStack upgradeItem) {
      return !upgradeItem.isEmpty() && !isUpgradeBroken(upgradeItem);
   }

   private static boolean isUpgradeDamageable(ItemStack upgradeItem) {
      if (upgradeItem.isEmpty()) {
         return false;
      } else if (upgradeItem.getItem() instanceof DamageableUpgradeItem) {
         return true;
      } else {
         return upgradeItem.isDamageableItem() ? true : upgradeItem.getMaxDamage() > 0;
      }
   }

   private static boolean isUpgradeBroken(ItemStack upgradeItem) {
      if (!isUpgradeDamageable(upgradeItem)) {
         return false;
      } else {
         return upgradeItem.getItem() instanceof DamageableUpgradeItem damageableUpgrade
            ? damageableUpgrade.isBroken(upgradeItem)
            : upgradeItem.getDamageValue() >= upgradeItem.getMaxDamage() && upgradeItem.getMaxDamage() > 0;
      }
   }

   private static void removeBrokenComponents(ItemStack exoSuitPiece) {
      CompoundTag upgradeData = ExoSuitData.getUpgradeData(exoSuitPiece);
      if (upgradeData.contains("Upgrades")) {
         ListTag upgradeList = upgradeData.getList("Upgrades", 10);
         ListTag newUpgradeList = new ListTag();
         boolean removedAny = false;

         for (int i = 0; i < upgradeList.size(); i++) {
            CompoundTag slotTag = upgradeList.getCompound(i);
            if (slotTag.contains("Item")) {
               ItemStack upgradeStack = top.ribs.scguns.util.NbtHelper.itemFromTag(slotTag.getCompound("Item"));
               if (!upgradeStack.isEmpty() && !isUpgradeBroken(upgradeStack)) {
                  newUpgradeList.add(slotTag);
               } else if (!upgradeStack.isEmpty()) {
                  removedAny = true;
               }
            } else {
               newUpgradeList.add(slotTag);
            }
         }

         if (removedAny) {
            upgradeData.put("Upgrades", newUpgradeList);
            ExoSuitData.setUpgradeData(exoSuitPiece, upgradeData);
         }
      }
   }
}
