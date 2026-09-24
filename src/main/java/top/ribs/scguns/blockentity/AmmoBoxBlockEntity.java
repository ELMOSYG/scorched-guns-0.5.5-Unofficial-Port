package top.ribs.scguns.blockentity;


import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import top.ribs.scguns.block.AmmoBoxBlock;
import top.ribs.scguns.init.ModBlockEntities;

public class AmmoBoxBlockEntity extends RandomizableContainerBlockEntity {
   private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
   private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
      protected void onOpen(Level pLevel, BlockPos pPos, BlockState pState) {
         AmmoBoxBlockEntity.this.playSound(pState, SoundEvents.CHEST_OPEN);
      }

      protected void onClose(Level pLevel, BlockPos pPos, BlockState pState) {
         AmmoBoxBlockEntity.this.playSound(pState, SoundEvents.CHEST_CLOSE);
      }

      protected void openerCountChanged(Level pLevel, BlockPos pPos, BlockState pState, int pOpenerCount, int pNewOpeners) {
      }

      protected boolean isOwnContainer(Player pPlayer) {
         return pPlayer.containerMenu instanceof ChestMenu && ((ChestMenu)pPlayer.containerMenu).getContainer() == AmmoBoxBlockEntity.this;
      }
   };

   public AmmoBoxBlockEntity(BlockPos pPos, BlockState pBlockState) {
      super((BlockEntityType)ModBlockEntities.AMMO_BOX.get(), pPos, pBlockState);
   }

   protected NonNullList<ItemStack> getItems() {
      return this.items;
   }

   protected void setItems(NonNullList<ItemStack> items) {
      this.items = items;
   }

   protected Component getDefaultName() {
      return Component.translatable("container.ammo_box");
   }

   protected AbstractContainerMenu createMenu(int id, Inventory playerInventory) {
      return ChestMenu.threeRows(id, playerInventory, this);
   }

   public int getContainerSize() {
      return 27;
   }

   public void startOpen(Player pPlayer) {
      if (!this.remove && !pPlayer.isSpectator()) {
         this.openersCounter.incrementOpeners(pPlayer, this.getLevel(), this.getBlockPos(), this.getBlockState());
         BlockState state = this.getBlockState();
         if (!(Boolean)state.getValue(AmmoBoxBlock.OPEN)) {
            this.getLevel().setBlock(this.worldPosition, (BlockState)state.setValue(AmmoBoxBlock.OPEN, true), 3);
         }
      }
   }

   public void stopOpen(Player pPlayer) {
      if (!this.remove && !pPlayer.isSpectator()) {
         this.openersCounter.decrementOpeners(pPlayer, this.getLevel(), this.getBlockPos(), this.getBlockState());
         BlockState state = this.getBlockState();
         if ((Boolean)state.getValue(AmmoBoxBlock.OPEN)) {
            this.getLevel().setBlock(this.worldPosition, (BlockState)state.setValue(AmmoBoxBlock.OPEN, false), 3);
         }
      }
   }

   void playSound(BlockState state, SoundEvent sound) {
      this.level.playSound(null, this.worldPosition, sound, SoundSource.BLOCKS, 0.5F, this.level.random.nextFloat() * 0.1F + 0.9F);
   }

   public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.loadAdditional(tag, registries);
      this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
      if (!this.tryLoadLootTable(tag)) {
         ContainerHelper.loadAllItems(tag, this.items, registries);
      }

      if (tag.contains("LootTable")) {
         System.out.println("Loaded loot table: " + tag.getString("LootTable"));
      }
   }

   protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.saveAdditional(tag, registries);
      if (!this.trySaveLootTable(tag)) {
         ContainerHelper.saveAllItems(tag, this.items, registries);
      }
   }
}
