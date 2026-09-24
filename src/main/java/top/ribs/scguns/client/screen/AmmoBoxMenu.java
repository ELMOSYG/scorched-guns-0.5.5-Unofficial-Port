package top.ribs.scguns.client.screen;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class AmmoBoxMenu extends AbstractContainerMenu {
   private final Container container;

   public AmmoBoxMenu(int id, Inventory playerInventory, Container container) {
      super((MenuType)ModMenuTypes.AMMO_BOX.get(), id);
      this.container = container;

      for (int i = 0; i < 3; i++) {
         for (int j = 0; j < 9; j++) {
            this.addSlot(new Slot(container, j + i * 9, 8 + j * 18, 18 + i * 18));
         }
      }

      for (int i = 0; i < 3; i++) {
         for (int j = 0; j < 9; j++) {
            this.addSlot(new Slot(playerInventory, j + i * 9, 8 + j * 18, 84 + i * 18));
         }
      }

      for (int i = 0; i < 9; i++) {
         this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
      }
   }

   public AmmoBoxMenu(int i, Inventory inventory, RegistryFriendlyByteBuf friendlyByteBuf) {
      this(i, inventory, (Container)inventory.player.containerMenu);
   }

   public boolean stillValid(Player player) {
      return this.container.stillValid(player);
   }

   public ItemStack quickMoveStack(Player player, int index) {
      ItemStack itemstack = ItemStack.EMPTY;
      Slot slot = (Slot)this.slots.get(index);
      if (slot.hasItem()) {
         ItemStack itemstack1 = slot.getItem();
         itemstack = itemstack1.copy();
         if (index < this.container.getContainerSize()) {
            if (!this.moveItemStackTo(itemstack1, this.container.getContainerSize(), this.slots.size(), true)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo(itemstack1, 0, this.container.getContainerSize(), false)) {
            return ItemStack.EMPTY;
         }

         if (itemstack1.isEmpty()) {
            slot.set(ItemStack.EMPTY);
         } else {
            slot.setChanged();
         }
      }

      return itemstack;
   }

   public Container getContainer() {
      return this.container;
   }
}
