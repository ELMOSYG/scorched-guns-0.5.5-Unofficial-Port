package top.ribs.scguns.blockentity;


import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import top.ribs.scguns.block.ShellCatcherModuleBlock;
import top.ribs.scguns.init.ModBlockEntities;

public class ShellCatcherModuleBlockEntity extends RandomizableContainerBlockEntity {
   private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
   private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
      protected void onOpen(Level p_155062_, BlockPos p_155063_, BlockState p_155064_) {
         ShellCatcherModuleBlockEntity.this.playSound(p_155064_, SoundEvents.BARREL_OPEN);
      }

      protected void onClose(Level p_155072_, BlockPos p_155073_, BlockState p_155074_) {
         ShellCatcherModuleBlockEntity.this.playSound(p_155074_, SoundEvents.BARREL_CLOSE);
      }

      protected void openerCountChanged(Level p_155066_, BlockPos p_155067_, BlockState p_155068_, int p_155069_, int p_155070_) {
      }

      protected boolean isOwnContainer(Player p_155060_) {
         if (p_155060_.containerMenu instanceof ChestMenu) {
            Container $$1 = ((ChestMenu)p_155060_.containerMenu).getContainer();
            return $$1 == ShellCatcherModuleBlockEntity.this;
         } else {
            return false;
         }
      }
   };

   public ShellCatcherModuleBlockEntity(BlockPos pPos, BlockState pBlockState) {
      super((BlockEntityType)ModBlockEntities.SHELL_CATCHER_MODULE.get(), pPos, pBlockState);
   }

   protected NonNullList<ItemStack> getItems() {
      return this.items;
   }

   protected void setItems(NonNullList<ItemStack> items) {
      this.items = items;
   }

   protected Component getDefaultName() {
      return Component.translatable("container.shell_catcher_module");
   }

   protected AbstractContainerMenu createMenu(int id, Inventory playerInventory) {
      return ChestMenu.threeRows(id, playerInventory, this);
   }

   public int getContainerSize() {
      return 27;
   }

   public void startOpen(Player player) {
      if (!this.remove && !player.isSpectator()) {
         this.openersCounter.incrementOpeners(player, this.getLevel(), this.getBlockPos(), this.getBlockState());
      }
   }

   public void stopOpen(Player player) {
      if (!this.remove && !player.isSpectator()) {
         this.openersCounter.decrementOpeners(player, this.getLevel(), this.getBlockPos(), this.getBlockState());
      }
   }

   void playSound(BlockState state, SoundEvent sound) {
      Vec3i directionVec = ((Direction)state.getValue(ShellCatcherModuleBlock.FACING)).getNormal();
      double x = (double)this.worldPosition.getX() + 0.5 + (double)directionVec.getX() / 2.0;
      double y = (double)this.worldPosition.getY() + 0.5 + (double)directionVec.getY() / 2.0;
      double z = (double)this.worldPosition.getZ() + 0.5 + (double)directionVec.getZ() / 2.0;
      this.level.playSound(null, x, y, z, sound, SoundSource.BLOCKS, 0.5F, this.level.random.nextFloat() * 0.1F + 0.9F);
   }

   protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.saveAdditional(tag, registries);
      ContainerHelper.saveAllItems(tag, this.items, registries);
   }

   public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.loadAdditional(tag, registries);
      this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
      ContainerHelper.loadAllItems(tag, this.items, registries);
   }

   public IItemHandlerModifiable getItemStackHandler() {
      return new IItemHandlerModifiable() {
         public void setStackInSlot(int slot, ItemStack stack) {
            ShellCatcherModuleBlockEntity.this.items.set(slot, stack);
         }

         public int getSlots() {
            return ShellCatcherModuleBlockEntity.this.items.size();
         }

         public ItemStack getStackInSlot(int slot) {
            return (ItemStack)ShellCatcherModuleBlockEntity.this.items.get(slot);
         }

         public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
         }

         public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return (ItemStack)ShellCatcherModuleBlockEntity.this.items.get(slot);
         }

         public int getSlotLimit(int slot) {
            return 64;
         }

         public boolean isItemValid(int slot, ItemStack stack) {
            return true;
         }
      };
   }
}
