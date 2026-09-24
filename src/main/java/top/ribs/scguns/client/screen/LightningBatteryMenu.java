package top.ribs.scguns.client.screen;


import top.ribs.scguns.util.Caps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.SlotItemHandler;
import top.ribs.scguns.blockentity.LightningBatteryBlockEntity;
import top.ribs.scguns.init.ModBlocks;

public class LightningBatteryMenu extends AbstractContainerMenu {
   private final LightningBatteryBlockEntity blockEntity;
   private final Level level;
   private final ContainerData data;
   private static final int HOTBAR_SLOT_COUNT = 9;
   private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
   private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
   private static final int PLAYER_INVENTORY_SLOT_COUNT = 27;
   private static final int VANILLA_SLOT_COUNT = 36;
   private static final int VANILLA_FIRST_SLOT_INDEX = 0;
   private static final int TE_INVENTORY_FIRST_SLOT_INDEX = 36;

   public LightningBatteryMenu(int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
      this(id, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(4));
   }

   public LightningBatteryMenu(int id, Inventory inv, BlockEntity entity, ContainerData data) {
      super((MenuType)ModMenuTypes.LIGHTING_BATTERY_MENU.get(), id);
      checkContainerSize(inv, 2);
      this.blockEntity = (LightningBatteryBlockEntity)entity;
      this.level = inv.player.level();
      this.data = data;
      this.addPlayerInventory(inv);
      this.addPlayerHotbar(inv);
      Caps.ifPresent(Caps.itemHandler(this.blockEntity, null), handler -> {
         this.addSlot(new SlotItemHandler(handler, 0, 56, 35));
         this.addSlot(new SlotItemHandler(handler, 1, 116, 35) {
            public boolean mayPlace(ItemStack stack) {
               return false;
            }
         });
      });
      this.addDataSlots(data);
   }

   public ItemStack quickMoveStack(Player player, int index) {
      ItemStack itemstack = ItemStack.EMPTY;
      Slot slot = (Slot)this.slots.get(index);
      if (slot.hasItem()) {
         ItemStack itemstack1 = slot.getItem();
         itemstack = itemstack1.copy();
         int outputSlotIndex = 37;
         if (index == outputSlotIndex) {
            if (!this.moveItemStackTo(itemstack1, 0, 36, true)) {
               return ItemStack.EMPTY;
            }

            slot.onQuickCraft(itemstack1, itemstack);
         } else if (index >= 0 && index < 36) {
            if (this.isInputItem(itemstack1)) {
               if (!this.moveItemStackTo(itemstack1, 36, 37, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (index >= 0 && index < 27) {
               if (!this.moveItemStackTo(itemstack1, 27, 36, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (index >= 27 && index < 36 && !this.moveItemStackTo(itemstack1, 0, 27, false)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo(itemstack1, 0, 36, false)) {
            return ItemStack.EMPTY;
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
      return stillValid(ContainerLevelAccess.create(this.level, this.blockEntity.getBlockPos()), player, (Block)ModBlocks.LIGHTNING_BATTERY.get());
   }

   private boolean isInputItem(ItemStack stack) {
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

   public boolean isCrafting() {
      return this.data.get(0) > 0;
   }

   public int getScaledProgress() {
      int progress = this.data.get(0);
      int maxProgress = this.data.get(1);
      int progressArrowSize = 24;
      return maxProgress != 0 && progress != 0 ? progress * progressArrowSize / maxProgress : 0;
   }

   public int getEnergy() {
      return this.data.get(2);
   }

   public int getMaxEnergy() {
      return this.data.get(3);
   }
}
