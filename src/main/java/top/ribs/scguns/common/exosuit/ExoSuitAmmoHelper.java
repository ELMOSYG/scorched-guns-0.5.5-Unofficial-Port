package top.ribs.scguns.common.exosuit;



import net.minecraft.core.HolderLookup;
import top.ribs.scguns.util.NbtHelper;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem.Type;
import net.neoforged.neoforge.items.ItemStackHandler;
import top.ribs.scguns.item.AmmoBoxItem;
import top.ribs.scguns.item.animated.ExoSuitItem;

public class ExoSuitAmmoHelper {
   public ExoSuitAmmoHelper() {
      super();
   }

   public static ItemStack findAmmoInExoSuit(Player player, Item ammoItem) {
      ItemStack chestplate = getEquippedChestplate(player);
      if (chestplate.isEmpty()) {
         return ItemStack.EMPTY;
      } else {
         ItemStack pouchUpgrade = findPouchUpgrade(chestplate);
         if (pouchUpgrade.isEmpty()) {
            return ItemStack.EMPTY;
         } else {
            ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(pouchUpgrade);
            if (upgrade == null) {
               return ItemStack.EMPTY;
            } else {
               String pouchId = getPouchId(pouchUpgrade);
               ItemStackHandler pouchInventory = getPouchInventory(player.registryAccess(), chestplate, pouchId, upgrade.getDisplay().getStorageSize());

               for (int i = 0; i < pouchInventory.getSlots(); i++) {
                  ItemStack stack = pouchInventory.getStackInSlot(i);
                  if (!stack.isEmpty()) {
                     if (stack.getItem() == ammoItem) {
                        return stack;
                     }

                     if (stack.getItem() instanceof AmmoBoxItem) {
                        try {
                           for (ItemStack ammoStack : AmmoBoxItem.getContents(stack).toList()) {
                              if (!ammoStack.isEmpty() && ammoStack.getItem() == ammoItem) {
                                 return ammoStack;
                              }
                           }
                        } catch (Exception var12) {
                           System.out.println("Error reading AmmoBox contents: " + var12.getMessage());
                           var12.printStackTrace();
                        }
                     }
                  }
               }

               System.out.println("No ammo found in exo suit pouches");
               return ItemStack.EMPTY;
            }
         }
      }
   }

   public static List<ItemStack> findAllAmmoInExoSuit(Player player, Item ammoItem) {
      List<ItemStack> ammoStacks = new ArrayList<>();
      ItemStack chestplate = getEquippedChestplate(player);
      if (chestplate.isEmpty()) {
         return ammoStacks;
      } else {
         ItemStack pouchUpgrade = findPouchUpgrade(chestplate);
         if (pouchUpgrade.isEmpty()) {
            return ammoStacks;
         } else {
            ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(pouchUpgrade);
            if (upgrade == null) {
               return ammoStacks;
            } else {
               String pouchId = getPouchId(pouchUpgrade);
               ItemStackHandler pouchInventory = getPouchInventory(player.registryAccess(), chestplate, pouchId, upgrade.getDisplay().getStorageSize());

               for (int i = 0; i < pouchInventory.getSlots(); i++) {
                  ItemStack stack = pouchInventory.getStackInSlot(i);
                  if (!stack.isEmpty() && stack.getItem() == ammoItem) {
                     ammoStacks.add(stack);
                  }
               }

               for (int ix = 0; ix < pouchInventory.getSlots(); ix++) {
                  ItemStack stack = pouchInventory.getStackInSlot(ix);
                  if (!stack.isEmpty() && stack.getItem() instanceof AmmoBoxItem) {
                     try {
                        for (ItemStack ammoStack : AmmoBoxItem.getContents(stack).toList()) {
                           if (!ammoStack.isEmpty() && ammoStack.getItem() == ammoItem) {
                              ammoStacks.add(ammoStack);
                           }
                        }
                     } catch (Exception var13) {
                        System.out.println("Error reading AmmoBox contents in findAllAmmoInExoSuit: " + var13.getMessage());
                     }
                  }
               }

               return ammoStacks;
            }
         }
      }
   }

   public static int getAmmoCountInExoSuit(Player player, Item ammoItem) {
      int totalCount = 0;
      ItemStack chestplate = getEquippedChestplate(player);
      if (chestplate.isEmpty()) {
         return totalCount;
      } else {
         ItemStack pouchUpgrade = findPouchUpgrade(chestplate);
         if (pouchUpgrade.isEmpty()) {
            return totalCount;
         } else {
            ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(pouchUpgrade);
            if (upgrade == null) {
               return totalCount;
            } else {
               String pouchId = getPouchId(pouchUpgrade);
               ItemStackHandler pouchInventory = getPouchInventory(player.registryAccess(), chestplate, pouchId, upgrade.getDisplay().getStorageSize());

               for (int i = 0; i < pouchInventory.getSlots(); i++) {
                  ItemStack stack = pouchInventory.getStackInSlot(i);
                  if (!stack.isEmpty() && stack.getItem() == ammoItem) {
                     totalCount += stack.getCount();
                  }
               }

               for (int ix = 0; ix < pouchInventory.getSlots(); ix++) {
                  ItemStack stack = pouchInventory.getStackInSlot(ix);
                  if (!stack.isEmpty() && stack.getItem() instanceof AmmoBoxItem) {
                     try {
                        for (ItemStack ammoStack : AmmoBoxItem.getContents(stack).toList()) {
                           if (!ammoStack.isEmpty() && ammoStack.getItem() == ammoItem) {
                              totalCount += ammoStack.getCount();
                           }
                        }
                     } catch (Exception var13) {
                        System.out.println("Error reading AmmoBox contents in getAmmoCountInExoSuit: " + var13.getMessage());
                     }
                  }
               }

               return totalCount;
            }
         }
      }
   }

   public static boolean addItemToExoSuit(Player player, ItemStack itemToAdd) {
      ItemStack chestplate = getEquippedChestplate(player);
      if (chestplate.isEmpty()) {
         return false;
      } else {
         ItemStack pouchUpgrade = findPouchUpgrade(chestplate);
         if (pouchUpgrade.isEmpty()) {
            return false;
         } else {
            ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(pouchUpgrade);
            if (upgrade == null) {
               return false;
            } else {
               String pouchId = getPouchId(pouchUpgrade);
               ItemStackHandler pouchInventory = getPouchInventory(player.registryAccess(), chestplate, pouchId, upgrade.getDisplay().getStorageSize());

               for (int i = 0; i < pouchInventory.getSlots(); i++) {
                  ItemStack remaining = pouchInventory.insertItem(i, itemToAdd, false);
                  if (remaining.isEmpty()) {
                     savePouchInventory(player.registryAccess(), chestplate, pouchId, pouchInventory);
                     return true;
                  }

                  if (remaining.getCount() < itemToAdd.getCount()) {
                     itemToAdd.setCount(remaining.getCount());
                  }
               }

               if (itemToAdd.getCount() < itemToAdd.getCount()) {
                  savePouchInventory(player.registryAccess(), chestplate, pouchId, pouchInventory);
               }

               return false;
            }
         }
      }
   }

   public static void shrinkAmmoInExoSuit(Player player, Item ammoItem, int amountToShrink) {
      ItemStack chestplate = getEquippedChestplate(player);
      if (!chestplate.isEmpty()) {
         ItemStack pouchUpgrade = findPouchUpgrade(chestplate);
         if (!pouchUpgrade.isEmpty()) {
            ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(pouchUpgrade);
            if (upgrade != null) {
               String pouchId = getPouchId(pouchUpgrade);
               ItemStackHandler pouchInventory = getPouchInventory(player.registryAccess(), chestplate, pouchId, upgrade.getDisplay().getStorageSize());
               int remainingToShrink = amountToShrink;

               for (int i = 0; i < pouchInventory.getSlots() && remainingToShrink > 0; i++) {
                  ItemStack stack = pouchInventory.getStackInSlot(i);
                  if (!stack.isEmpty() && stack.getItem() == ammoItem) {
                     int shrinkAmount = Math.min(remainingToShrink, stack.getCount());
                     stack.shrink(shrinkAmount);
                     remainingToShrink -= shrinkAmount;
                     if (stack.isEmpty()) {
                        pouchInventory.setStackInSlot(i, ItemStack.EMPTY);
                     }
                  }
               }

               for (int ix = 0; ix < pouchInventory.getSlots() && remainingToShrink > 0; ix++) {
                  ItemStack stack = pouchInventory.getStackInSlot(ix);
                  if (!stack.isEmpty() && stack.getItem() instanceof AmmoBoxItem) {
                     try {
                        List<ItemStack> contents = AmmoBoxItem.getContents(stack).toList();
                        boolean hasAmmo = false;

                        for (ItemStack ammoStack : contents) {
                           if (!ammoStack.isEmpty() && ammoStack.getItem() == ammoItem) {
                              hasAmmo = true;
                              break;
                           }
                        }

                        if (hasAmmo) {
                           new ItemStack(ammoItem, remainingToShrink);
                           CompoundTag tag = NbtHelper.getOrCreateTag(stack);
                           if (tag.contains("Items")) {
                              int ammoInBox = 0;

                              for (ItemStack ammoStackx : contents) {
                                 if (!ammoStackx.isEmpty() && ammoStackx.getItem() == ammoItem) {
                                    ammoInBox += ammoStackx.getCount();
                                 }
                              }

                              if (ammoInBox > 0) {
                                 int toExtract = Math.min(remainingToShrink, ammoInBox);
                                 ItemStack newAmmoBox = stack.copy();
                                 NbtHelper.getOrCreateTag(newAmmoBox).remove("Items");
                                 int extracted = 0;

                                 for (ItemStack ammoStackxx : contents) {
                                    if (extracted >= toExtract) {
                                       if (!ammoStackxx.isEmpty()) {
                                          AmmoBoxItem.add(newAmmoBox, ammoStackxx);
                                       }
                                    } else if (ammoStackxx.getItem() == ammoItem) {
                                       int canExtract = Math.min(toExtract - extracted, ammoStackxx.getCount());
                                       extracted += canExtract;
                                       if (ammoStackxx.getCount() > canExtract) {
                                          ItemStack remaining = ammoStackxx.copy();
                                          remaining.setCount(ammoStackxx.getCount() - canExtract);
                                          AmmoBoxItem.add(newAmmoBox, remaining);
                                       }
                                    } else if (!ammoStackxx.isEmpty()) {
                                       AmmoBoxItem.add(newAmmoBox, ammoStackxx);
                                    }
                                 }

                                 pouchInventory.setStackInSlot(ix, newAmmoBox);
                                 remainingToShrink -= extracted;
                              }
                           }
                        }
                     } catch (Exception var22) {
                        System.out.println("Error shrinking ammo from AmmoBox: " + var22.getMessage());
                        var22.printStackTrace();
                     }
                  }
               }

               savePouchInventory(player.registryAccess(), chestplate, pouchId, pouchInventory);
            }
         }
      }
   }

   private static ItemStack getEquippedChestplate(Player player) {
      for (ItemStack armorStack : player.getArmorSlots()) {
         if (armorStack.getItem() instanceof ExoSuitItem exosuit && exosuit.getType() == Type.CHESTPLATE) {
            return armorStack;
         }
      }

      return ItemStack.EMPTY;
   }

   private static ItemStack findPouchUpgrade(ItemStack chestplate) {
      for (int slot = 0; slot < 4; slot++) {
         ItemStack upgradeItem = ExoSuitData.getUpgradeInSlot(chestplate, slot);
         if (!upgradeItem.isEmpty()) {
            ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(upgradeItem);
            if (upgrade != null && upgrade.getType().equals("pouches")) {
               return upgradeItem;
            }
         }
      }

      return ItemStack.EMPTY;
   }

   private static String getPouchId(ItemStack pouchUpgrade) {
      return pouchUpgrade.getItem().toString();
   }

   private static ItemStackHandler getPouchInventory(HolderLookup.Provider registries, ItemStack chestplate, String pouchId, int size) {
      CompoundTag pouchData = NbtHelper.getOrCreateTag(chestplate).getCompound("PouchData");
      ItemStackHandler handler = new ItemStackHandler(size);
      if (pouchData.contains(pouchId)) {
         handler.deserializeNBT(registries, pouchData.getCompound(pouchId));
      }

      return handler;
   }

   private static void savePouchInventory(HolderLookup.Provider registries, ItemStack chestplate, String pouchId, ItemStackHandler handler) {
      CompoundTag pouchData = NbtHelper.getOrCreateTag(chestplate).getCompound("PouchData");
      pouchData.put(pouchId, handler.serializeNBT(registries));
      NbtHelper.getOrCreateTag(chestplate).put("PouchData", pouchData);
   }
}
