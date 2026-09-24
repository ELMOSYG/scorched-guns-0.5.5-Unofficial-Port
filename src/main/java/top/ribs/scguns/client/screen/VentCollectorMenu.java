package top.ribs.scguns.client.screen;

import java.util.Objects;
import net.minecraft.client.Minecraft;
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
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import top.ribs.scguns.blockentity.VentCollectorBlockEntity;

public class VentCollectorMenu extends AbstractContainerMenu {
   private final VentCollectorBlockEntity blockEntity;
   private final ItemStackHandler itemHandler;
   private final ContainerData data;

   public VentCollectorMenu(int windowId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
      this(windowId, playerInventory, Objects.requireNonNull((VentCollectorBlockEntity)Minecraft.getInstance().level.getBlockEntity(extraData.readBlockPos())), new SimpleContainerData(1));
   }

   public VentCollectorMenu(int windowId, Inventory playerInventory, VentCollectorBlockEntity entity, ContainerData data) {
      super((MenuType)ModMenuTypes.VENT_COLLECTOR_MENU.get(), windowId);
      checkContainerSize(playerInventory, 4);
      this.blockEntity = entity;
      this.itemHandler = this.blockEntity.getItemHandler();
      this.data = data;
      this.addSlot(new SlotItemHandler(this.itemHandler, 0, 80, 53) {
         public boolean mayPlace(ItemStack stack) {
            return VentCollectorMenu.this.blockEntity.isValidFilterItem(stack);
         }
      });
      this.addSlot(new SlotItemHandler(this.itemHandler, 1, 62, 17) {
         public boolean mayPlace(ItemStack stack) {
            return false;
         }
      });
      this.addSlot(new SlotItemHandler(this.itemHandler, 2, 80, 17) {
         public boolean mayPlace(ItemStack stack) {
            return false;
         }
      });
      this.addSlot(new SlotItemHandler(this.itemHandler, 3, 98, 17) {
         public boolean mayPlace(ItemStack stack) {
            return false;
         }
      });

      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
         }
      }

      for (int col = 0; col < 9; col++) {
         this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
      }

      this.addDataSlots(data);
   }

   public int getFilterCharge() {
      return this.data.get(0);
   }

   public int getMaxFilterCharge() {
      return this.blockEntity.getMaxFilterCharge();
   }

   public boolean stillValid(Player player) {
      return stillValid(ContainerLevelAccess.create(this.blockEntity.getLevel(), this.blockEntity.getBlockPos()), player, this.blockEntity.getBlockState().getBlock());
   }

   public ItemStack quickMoveStack(Player player, int index) {
      ItemStack itemstack = ItemStack.EMPTY;
      Slot slot = (Slot)this.slots.get(index);
      if (slot.hasItem()) {
         ItemStack itemstack1 = slot.getItem();
         itemstack = itemstack1.copy();
         if (index < 4) {
            if (!this.moveItemStackTo(itemstack1, 4, this.slots.size(), true)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo(itemstack1, 0, 4, false)) {
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
}
