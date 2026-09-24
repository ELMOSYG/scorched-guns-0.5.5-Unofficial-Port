package top.ribs.scguns.client.screen;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem.Type;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;
import top.ribs.scguns.common.exosuit.ExoSuitData;
import top.ribs.scguns.common.exosuit.ExoSuitPouchHandler;
import top.ribs.scguns.common.exosuit.ExoSuitUpgrade;
import top.ribs.scguns.common.exosuit.ExoSuitUpgradeManager;
import top.ribs.scguns.item.animated.ExoSuitItem;
import top.ribs.scguns.network.PacketHandler;
import top.ribs.scguns.network.message.C2SMessageSaveExoSuitUpgrades;

public class ExoSuitMenu extends AbstractContainerMenu {
   public static final int ARMOR_SLOT = 0;
   public static final int UPGRADE_SLOT_1 = 1;
   public static final int UPGRADE_SLOT_2 = 2;
   public static final int UPGRADE_SLOT_3 = 3;
   public static final int UPGRADE_SLOT_4 = 4;
   private static final int HOTBAR_SLOT_COUNT = 9;
   private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
   private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
   private static final int PLAYER_INVENTORY_SLOT_COUNT = 27;
   private static final int VANILLA_SLOT_COUNT = 36;
   private static final int VANILLA_FIRST_SLOT_INDEX = 0;
   public static final int EXOSUIT_INVENTORY_FIRST_SLOT_INDEX = 36;
   private final Player player;
   private final InteractionHand hand;
   private final ItemStackHandler exosuitInventory = new ItemStackHandler(5) {
      protected void onContentsChanged(int slot) {
         super.onContentsChanged(slot);
         ExoSuitMenu.this.slotsChanged(null);
         if (slot >= 1 && slot <= 4) {
            ExoSuitMenu.this.saveUpgradesToServer();
         }
      }

      public boolean isItemValid(int slot, ItemStack stack) {
         return slot == 0 ? stack.getItem() instanceof ExoSuitItem : slot >= 1 && slot <= 4;
      }
   };

   public ExoSuitMenu(int id, Inventory playerInv, RegistryFriendlyByteBuf extraData) {
      this(id, playerInv, (InteractionHand)extraData.readEnum(InteractionHand.class));
   }

   public ExoSuitMenu(int id, Inventory playerInv, InteractionHand hand) {
      super((MenuType)ModMenuTypes.EXOSUIT_MENU.get(), id);
      this.player = playerInv.player;
      this.hand = hand;
      this.addPlayerInventory(playerInv);
      this.addPlayerHotbar(playerInv);
      this.addExoSuitSlots();
      this.moveExoSuitToArmorSlot();
      this.loadUpgradesFromArmor();
   }

   private void moveExoSuitToArmorSlot() {
      ItemStack heldStack = this.player.getItemInHand(this.hand);
      if (!heldStack.isEmpty() && heldStack.getItem() instanceof ExoSuitItem) {
         this.exosuitInventory.setStackInSlot(0, heldStack.copy());
         this.player.setItemInHand(this.hand, ItemStack.EMPTY);
      }
   }

   private void loadUpgradesFromArmor() {
      ItemStack armorPiece = this.getArmorPiece();
      if (!armorPiece.isEmpty() && armorPiece.getItem() instanceof ExoSuitItem) {
         CompoundTag upgradeData = ExoSuitData.getUpgradeData(armorPiece);
         if (upgradeData.contains("Upgrades")) {
            ListTag upgradeList = upgradeData.getList("Upgrades", 10);

            for (int i = 0; i < upgradeList.size(); i++) {
               CompoundTag slotTag = upgradeList.getCompound(i);
               int slot = slotTag.getInt("Slot");
               if (slot >= 0 && slot < 4 && slotTag.contains("Item")) {
                  ItemStack upgradeStack = top.ribs.scguns.util.NbtHelper.itemFromTag(slotTag.getCompound("Item"));
                  this.exosuitInventory.setStackInSlot(1 + slot, upgradeStack);
               }
            }
         }
      }
   }

   private void saveUpgradesToServer() {
      if (this.player.level().isClientSide) {
         List<ItemStack> upgradeStacks = new ArrayList<>();

         for (int i = 1; i <= 4; i++) {
            upgradeStacks.add(this.exosuitInventory.getStackInSlot(i));
         }

         PacketHandler.getPlayChannel().sendToServer(new C2SMessageSaveExoSuitUpgrades(upgradeStacks));
      }
   }

   public void removed(@NotNull Player player) {
      super.removed(player);
      if (player.level().isClientSide) {
         this.saveUpgradesToServer();
      }

      ItemStack armorPiece = this.exosuitInventory.getStackInSlot(0);
      if (!armorPiece.isEmpty()) {
         if (player.getItemInHand(this.hand).isEmpty()) {
            player.setItemInHand(this.hand, armorPiece);
         } else if (!player.getInventory().add(armorPiece)) {
            player.drop(armorPiece, false);
         }
      }
   }

   private void addExoSuitSlots() {
      this.addSlot(new ExoSuitMenu.ExoSuitArmorSlot(this.exosuitInventory, 0, 26, 35));
      this.addSlot(new ExoSuitMenu.ExoSuitUpgradeSlot(this.exosuitInventory, 1, 98, 26));
      this.addSlot(new ExoSuitMenu.ExoSuitUpgradeSlot(this.exosuitInventory, 2, 116, 26));
      this.addSlot(new ExoSuitMenu.ExoSuitUpgradeSlot(this.exosuitInventory, 3, 98, 44));
      this.addSlot(new ExoSuitMenu.ExoSuitUpgradeSlot(this.exosuitInventory, 4, 116, 44));
   }

   public ItemStack getArmorPiece() {
      return this.exosuitInventory.getStackInSlot(0);
   }

   public int getAvailableUpgradeSlots() {
      ItemStack armor = this.getArmorPiece();
      return armor.getItem() instanceof ExoSuitItem exosuit ? exosuit.getMaxUpgradeSlots() : 0;
   }

   public boolean isUpgradeSlotEnabled(int upgradeSlotIndex) {
      if (upgradeSlotIndex >= 1 && upgradeSlotIndex <= 4) {
         int slotNumber = upgradeSlotIndex - 1 + 1;
         return slotNumber <= this.getAvailableUpgradeSlots();
      } else {
         return false;
      }
   }

   @NotNull
   public ItemStack quickMoveStack(Player player, int index) {
      ItemStack itemstack = ItemStack.EMPTY;
      Slot slot = (Slot)this.slots.get(index);
      if (slot.hasItem()) {
         ItemStack itemstack1 = slot.getItem();
         itemstack = itemstack1.copy();
         if (index >= 36 && index < 41) {
            if (index == 36) {
               return ItemStack.EMPTY;
            }

            if (!this.moveItemStackTo(itemstack1, 0, 36, true)) {
               return ItemStack.EMPTY;
            }
         } else if (index >= 0 && index < 36) {
            if (itemstack1.getItem() instanceof ExoSuitItem) {
               return ItemStack.EMPTY;
            }

            if (!this.moveItemStackTo(itemstack1, 37, 41, false)) {
               return ItemStack.EMPTY;
            }
         }

         if (itemstack1.isEmpty()) {
            slot.set(ItemStack.EMPTY);
         } else {
            slot.setChanged();
         }

         if (itemstack1.getCount() == itemstack.getCount()) {
            return ItemStack.EMPTY;
         }

         slot.onTake(player, itemstack1);
      }

      return itemstack;
   }

   public boolean stillValid(Player player) {
      return true;
   }

   private void addPlayerInventory(Inventory playerInventory) {
      for (int i = 0; i < 3; i++) {
         for (int l = 0; l < 9; l++) {
            this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
         }
      }
   }

   private void addPlayerHotbar(Inventory playerInventory) {
      for (int i = 0; i < 9; i++) {
         this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
      }
   }

   private boolean canUpgradeGoInSlot(ExoSuitUpgrade upgrade, int slotIndex, Type armorType) {
      String upgradeType = upgrade.getType();
      if (slotIndex == 0) {
         return upgradeType.equals("plating");
      } else {
         return switch (armorType) {
            case HELMET -> {
               switch (slotIndex) {
                  case 1:
                     yield upgradeType.equals("hud");
                  case 2:
                     yield upgradeType.equals("breathing");
                  default:
                     yield false;
               }
            }
            case CHESTPLATE -> {
               switch (slotIndex) {
                  case 1:
                     yield upgradeType.equals("pauldron");
                  case 2:
                     yield upgradeType.equals("power_core");
                  case 3:
                     yield upgradeType.equals("utility") || upgradeType.equals("pouches");
                  default:
                     yield false;
               }
            }
            case LEGGINGS -> {
               switch (slotIndex) {
                  case 1:
                     yield upgradeType.equals("knee_guard") || upgradeType.equals("plating");
                  case 2:
                     yield upgradeType.equals("utility");
                  default:
                     yield false;
               }
            }
            case BOOTS -> {
               switch (slotIndex) {
                  case 1:
                     yield upgradeType.equals("mobility");
                  default:
                     yield false;
               }
            }
            default -> throw new IncompatibleClassChangeError();
         };
      }
   }

   private static class ExoSuitArmorSlot extends SlotItemHandler {
      public ExoSuitArmorSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
         super(itemHandler, index, xPosition, yPosition);
      }

      public boolean mayPlace(ItemStack stack) {
         return stack.getItem() instanceof ExoSuitItem;
      }

      public int getMaxStackSize() {
         return 1;
      }

      public boolean mayPickup(Player player) {
         return false;
      }

      public void onTake(Player player, ItemStack stack) {
         super.onTake(player, stack);
         player.closeContainer();
      }
   }

   private class ExoSuitUpgradeSlot extends SlotItemHandler {
      public ExoSuitUpgradeSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
         super(itemHandler, index, xPosition, yPosition);
      }

      public boolean mayPlace(@NotNull ItemStack stack) {
         if (!ExoSuitUpgradeManager.isUpgradeItem(stack)) {
            return false;
         } else if (!ExoSuitMenu.this.isUpgradeSlotEnabled(this.getSlotIndex())) {
            return false;
         } else {
            ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(stack);
            if (upgrade != null) {
               ItemStack armorPiece = ExoSuitMenu.this.getArmorPiece();
               if (armorPiece.getItem() instanceof ExoSuitItem exosuit) {
                  return ExoSuitMenu.this.canUpgradeGoInSlot(upgrade, this.getSlotIndex() - 1, exosuit.getType());
               }
            }

            return false;
         }
      }

      public boolean mayPickup(Player player) {
         ItemStack currentStack = this.getItem();
         if (currentStack.isEmpty()) {
            return true;
         } else {
            ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(currentStack);
            if (upgrade != null && upgrade.getType().equals("pouches")) {
               ItemStack armorPiece = ExoSuitMenu.this.getArmorPiece();
               if (!ExoSuitPouchHandler.canRemovePouch(armorPiece, currentStack)) {
                  return false;
               }
            }

            return super.mayPickup(player);
         }
      }

      public boolean isActive() {
         return ExoSuitMenu.this.isUpgradeSlotEnabled(this.getSlotIndex());
      }

      public int getMaxStackSize() {
         return 1;
      }

      public void onTake(Player player, ItemStack stack) {
         super.onTake(player, stack);
         if (player.level().isClientSide) {
            ExoSuitMenu.this.saveUpgradesToServer();
         }
      }

      public void set(ItemStack stack) {
         super.set(stack);
         if (!stack.isEmpty() && ExoSuitMenu.this.player.level().isClientSide) {
            ExoSuitUpgradeManager.getUpgradeForItem(stack);
         }
      }
   }
}
