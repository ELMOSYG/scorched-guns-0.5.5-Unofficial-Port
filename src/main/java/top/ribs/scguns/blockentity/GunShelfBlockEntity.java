package top.ribs.scguns.blockentity;



import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import top.ribs.scguns.util.NbtHelper;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import top.ribs.scguns.init.ModBlockEntities;

public class GunShelfBlockEntity extends BlockEntity {
   private ItemStack displayedItem = ItemStack.EMPTY;

   public GunShelfBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)ModBlockEntities.GUN_SHELF_BLOCK_ENTITY.get(), pos, state);
   }

   public ItemStack getDisplayedItem() {
      return this.displayedItem;
   }

   public void setDisplayedItem(ItemStack stack) {
      this.displayedItem = stack == null ? ItemStack.EMPTY : stack;
      this.setChanged();
      if (this.level != null) {
         this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
         this.level.updateNeighborsAt(this.getBlockPos(), this.getBlockState().getBlock());
      }
   }

   public boolean isEmpty() {
      return this.displayedItem.isEmpty();
   }

   public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.loadAdditional(tag, registries);
      if (tag.contains("DisplayedItem", 10)) {
         this.displayedItem = top.ribs.scguns.util.NbtHelper.itemFromTag(tag.getCompound("DisplayedItem"));
      } else {
         this.displayedItem = ItemStack.EMPTY;
      }
   }

   protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.saveAdditional(tag, registries);
      if (!this.displayedItem.isEmpty()) {
         tag.put("DisplayedItem", top.ribs.scguns.util.NbtHelper.tagFromItem(this.displayedItem));
      }
   }

   public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
      CompoundTag tag = super.getUpdateTag(registries);
      this.saveAdditional(tag, registries);
      return tag;
   }

   /** 1.20.1 entry point, kept for API compatibility; 1.21 callers pass the registries they already hold. */
   public void handleUpdateTag(CompoundTag tag) {
      this.handleUpdateTag(tag, this.level == null ? RegistryAccess.EMPTY : this.level.registryAccess());
   }

   public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
      if (tag != null) {
         this.loadAdditional(tag, registries);
      } else {
         this.setDisplayedItem(ItemStack.EMPTY);
      }
   }

   @Nullable
   public Packet<ClientGamePacketListener> getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
      this.handleUpdateTag(pkt.getTag(), registries);
   }

   /** 1.20.1 entry point, kept for API compatibility. */
   public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
      this.onDataPacket(net, pkt, this.level == null ? RegistryAccess.EMPTY : this.level.registryAccess());
   }

   public ItemStack getItem(int i) {
      return i == 0 ? this.displayedItem : ItemStack.EMPTY;
   }
}
