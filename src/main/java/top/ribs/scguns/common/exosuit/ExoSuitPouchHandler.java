package top.ribs.scguns.common.exosuit;



import net.minecraft.core.HolderLookup;
import top.ribs.scguns.util.NbtHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem.Type;
import net.neoforged.neoforge.items.ItemStackHandler;

import org.jetbrains.annotations.NotNull;
import top.ribs.scguns.item.animated.ExoSuitItem;

public class ExoSuitPouchHandler {
   private static final String POUCH_DATA_TAG = "PouchData";

   public ExoSuitPouchHandler() {
   }

   public static void openPouchInventory(ServerPlayer player) {
      ItemStack chestplate = getEquippedChestplate(player);
      if (!chestplate.isEmpty()) {
         ItemStack pouchUpgrade = findPouchUpgrade(chestplate);
         if (!pouchUpgrade.isEmpty()) {
            ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(pouchUpgrade);
            if (upgrade != null) {
               int storageSize = upgrade.getDisplay().getStorageSize();
               String containerType = upgrade.getDisplay().getContainerType();
               String pouchId = getPouchId(pouchUpgrade);
               openPouchContainer(player, chestplate, pouchId, storageSize, containerType, upgrade);
            }
         }
      }
   }

   private static void openPouchContainer(
      ServerPlayer player, final ItemStack chestplate, final String pouchId, final int storageSize, final String containerType, final ExoSuitUpgrade upgrade
   ) {
      final ItemStackHandler pouchInventory = getOrCreatePouchInventory(player.registryAccess(), chestplate, pouchId, storageSize);
      MenuProvider menuProvider = new MenuProvider() {
         @NotNull
         public Component getDisplayName() {
            return Component.translatable("container.scguns.pouch", new Object[]{upgrade.getDisplay().getModel()});
         }

         public AbstractContainerMenu createMenu(int id, @NotNull Inventory playerInventory, @NotNull Player player) {
            ExoSuitPouchHandler.ItemStackHandlerContainer container = new ExoSuitPouchHandler.ItemStackHandlerContainer(
               pouchInventory, chestplate, pouchId, player.registryAccess()
            );
            String var5 = containerType.toLowerCase();

            return (AbstractContainerMenu)(switch (var5) {
               case "chest" -> ChestMenu.threeRows(id, playerInventory, container);
               case "double_chest" -> ChestMenu.sixRows(id, playerInventory, container);
               case "dispenser" -> new DispenserMenu(id, playerInventory, container);
               default -> storageSize <= 9
               ? new DispenserMenu(id, playerInventory, container)
               : (storageSize <= 27 ? ChestMenu.threeRows(id, playerInventory, container) : ChestMenu.sixRows(id, playerInventory, container));
            });
         }
      };
      player.openMenu(menuProvider);
   }

   public static boolean canRemovePouch(ItemStack chestplate, ItemStack pouchUpgrade) {
      if (pouchUpgrade.isEmpty()) {
         return true;
      } else {
         ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(pouchUpgrade);
         if (upgrade != null && upgrade.getType().equals("pouches")) {
            String pouchId = getPouchId(pouchUpgrade);
            CompoundTag pouchData = getPouchData(chestplate);
            if (!pouchData.contains(pouchId)) {
               return true;
            } else {
               ItemStackHandler handler = new ItemStackHandler(upgrade.getDisplay().getStorageSize());
               handler.deserializeNBT(net.minecraft.client.Minecraft.getInstance().level.registryAccess(), pouchData.getCompound(pouchId));

               for (int i = 0; i < handler.getSlots(); i++) {
                  if (!handler.getStackInSlot(i).isEmpty()) {
                     return false;
                  }
               }

               return true;
            }
         } else {
            return true;
         }
      }
   }

   private static ItemStackHandler getOrCreatePouchInventory(final HolderLookup.Provider registries, final ItemStack chestplate, final String pouchId, int size) {
      CompoundTag pouchData = getPouchData(chestplate);
      ItemStackHandler handler = new ItemStackHandler(size) {
         protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            ExoSuitPouchHandler.savePouchInventory(registries, chestplate, pouchId, this);
         }
      };
      if (pouchData.contains(pouchId)) {
         handler.deserializeNBT(registries, pouchData.getCompound(pouchId));
      }

      return handler;
   }

   private static void savePouchInventory(HolderLookup.Provider registries, ItemStack chestplate, String pouchId, ItemStackHandler handler) {
      CompoundTag pouchData = getPouchData(chestplate);
      pouchData.put(pouchId, handler.serializeNBT(registries));
      setPouchData(chestplate, pouchData);
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

   private static ItemStack getEquippedChestplate(Player player) {
      for (ItemStack armorStack : player.getArmorSlots()) {
         if (armorStack.getItem() instanceof ExoSuitItem exosuit && exosuit.getType() == Type.CHESTPLATE) {
            return armorStack;
         }
      }

      return ItemStack.EMPTY;
   }

   private static CompoundTag getPouchData(ItemStack chestplate) {
      return NbtHelper.getOrCreateTag(chestplate).getCompound("PouchData");
   }

   private static void setPouchData(ItemStack chestplate, CompoundTag pouchData) {
      NbtHelper.getOrCreateTag(chestplate).put("PouchData", pouchData);
   }

   private static record ItemStackHandlerContainer(ItemStackHandler handler, ItemStack chestplate, String pouchId, HolderLookup.Provider registries)
      implements Container {
      private ItemStackHandlerContainer(ItemStackHandler handler, ItemStack chestplate, String pouchId, HolderLookup.Provider registries) {
         this.handler = handler;
         this.chestplate = chestplate;
         this.pouchId = pouchId;
         this.registries = registries;
      }

      public int getContainerSize() {
         return this.handler.getSlots();
      }

      public boolean isEmpty() {
         for (int i = 0; i < this.handler.getSlots(); i++) {
            if (!this.handler.getStackInSlot(i).isEmpty()) {
               return false;
            }
         }

         return true;
      }

      @NotNull
      public ItemStack getItem(int slot) {
         return this.handler.getStackInSlot(slot);
      }

      @NotNull
      public ItemStack removeItem(int slot, int count) {
         ItemStack result = this.handler.extractItem(slot, count, false);
         this.setChanged();
         return result;
      }

      @NotNull
      public ItemStack removeItemNoUpdate(int slot) {
         ItemStack stack = this.handler.getStackInSlot(slot);
         this.handler.setStackInSlot(slot, ItemStack.EMPTY);
         this.setChanged();
         return stack;
      }

      public void setItem(int slot, ItemStack stack) {
         this.handler.setStackInSlot(slot, stack);
         this.setChanged();
      }

      public void setChanged() {
         ExoSuitPouchHandler.savePouchInventory(this.registries, this.chestplate, this.pouchId, this.handler);
      }

      public boolean stillValid(Player player) {
         return true;
      }

      public void clearContent() {
         for (int i = 0; i < this.handler.getSlots(); i++) {
            this.handler.setStackInSlot(i, ItemStack.EMPTY);
         }

         this.setChanged();
      }
   }
}
