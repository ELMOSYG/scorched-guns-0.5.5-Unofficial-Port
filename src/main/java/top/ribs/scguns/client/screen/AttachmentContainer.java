package top.ribs.scguns.client.screen;


import top.ribs.scguns.util.NbtHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.common.container.slot.AttachmentSlot;
import top.ribs.scguns.init.ModContainers;
import top.ribs.scguns.item.attachment.IAttachment;

public class AttachmentContainer extends AbstractContainerMenu {
   private final ItemStack weapon;
   private final Container playerInventory;
   private final Container weaponInventory = new SimpleContainer(IAttachment.Type.values().length) {
      public void setChanged() {
         super.setChanged();
         AttachmentContainer.this.slotsChanged(this);
      }
   };
   private boolean loaded = false;

   public AttachmentContainer(int windowId, Inventory playerInventory, ItemStack stack) {
      this(windowId, playerInventory);
      ItemStack[] attachments = new ItemStack[IAttachment.Type.values().length];

      for (int i = 0; i < attachments.length; i++) {
         attachments[i] = Gun.getAttachment(IAttachment.Type.values()[i], stack);
      }

      for (int i = 0; i < attachments.length; i++) {
         this.weaponInventory.setItem(i, attachments[i]);
      }

      this.loaded = true;
   }

   public AttachmentContainer(int windowId, Inventory playerInventory) {
      super((MenuType)ModContainers.ATTACHMENTS.get(), windowId);
      this.weapon = playerInventory.getSelected();
      this.playerInventory = playerInventory;
      int numSlots = Math.min(5, IAttachment.Type.values().length);
      int centerX = 88 - numSlots * 18 / 2 + 18 - 5 - 6;

      for (int i = 0; i < numSlots; i++) {
         this.addSlot(
            new AttachmentSlot(this, this.weaponInventory, this.weapon, IAttachment.Type.values()[i], playerInventory.player, i, centerX + i * 18, 108)
         );
      }

      for (int i = 0; i < 3; i++) {
         for (int j = 0; j < 9; j++) {
            this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 20 + j * 18 - 6, 110 + i * 18 + 19));
         }
      }

      for (int i = 0; i < 9; i++) {
         if (i == playerInventory.selected) {
            this.addSlot(new Slot(playerInventory, i, 20 + i * 18 - 6, 187) {
               public boolean mayPickup(Player playerIn) {
                  return false;
               }
            });
         } else {
            this.addSlot(new Slot(playerInventory, i, 20 + i * 18 - 6, 187));
         }
      }
   }

   public boolean isLoaded() {
      return this.loaded;
   }

   public boolean stillValid(Player playerIn) {
      return true;
   }

   public void slotsChanged(Container inventoryIn) {
      CompoundTag attachments = new CompoundTag();
      boolean encodeFailed = false;

      for (int i = 0; i < this.getWeaponInventory().getContainerSize(); i++) {
         ItemStack attachment = this.getSlot(i).getItem();
         CompoundTag encoded = top.ribs.scguns.util.NbtHelper.tagFromItem(attachment);

         // An empty tag for a non-empty stack means the encode failed. Writing it would replace the
         // gun's stored attachment with nothing and destroy the item - that is exactly how enchanted
         // attachments vanished on install (HANDOFF section 58). Leave the tag alone instead.
         if (!attachment.isEmpty() && encoded.isEmpty()) {
            encodeFailed = true;
            break;
         }

         if (attachment.getItem() instanceof SwordItem) {
            attachments.put("Barrel", encoded);
         }

         if (attachment.getItem() instanceof IAttachment) {
            attachments.put(((IAttachment)attachment.getItem()).getType().getTagKey(), encoded);
         }
      }

      if (encodeFailed) {
         super.broadcastChanges();
         return;
      }

      CompoundTag tag = NbtHelper.getOrCreateTag(this.weapon);
      tag.put("Attachments", attachments);
      super.broadcastChanges();
   }

   public ItemStack quickMoveStack(Player playerIn, int index) {
      ItemStack copyStack = ItemStack.EMPTY;
      Slot slot = (Slot)this.slots.get(index);
      if (slot != null && slot.hasItem()) {
         ItemStack slotStack = slot.getItem();
         copyStack = slotStack.copy();
         if (index < this.weaponInventory.getContainerSize()) {
            if (!this.moveItemStackTo(slotStack, this.weaponInventory.getContainerSize(), this.slots.size(), true)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo(slotStack, 0, this.weaponInventory.getContainerSize(), false)) {
            return ItemStack.EMPTY;
         }

         if (slotStack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
         } else {
            slot.setChanged();
         }
      }

      return copyStack;
   }

   public Container getPlayerInventory() {
      return this.playerInventory;
   }

   public Container getWeaponInventory() {
      return this.weaponInventory;
   }
}
