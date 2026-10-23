package top.ribs.scguns.blockentity;


import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import top.ribs.scguns.client.screen.GunBenchMenu;
import top.ribs.scguns.init.ModBlockEntities;

public class GunBenchBlockEntity extends BlockEntity implements MenuProvider {
   private final SimpleContainer inventory = new SimpleContainer(12) {
      public void setChanged() {
         super.setChanged();
         GunBenchBlockEntity.this.setChanged();
      }
   };

   public GunBenchBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)ModBlockEntities.GUN_BENCH.get(), pos, state);
   }

   public Component getDisplayName() {
      return Component.translatable("container.gun_bench");
   }

   public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
      return new GunBenchMenu(id, playerInventory, this.inventory, ContainerLevelAccess.create(this.level, this.worldPosition));
   }

   /**
    * The container contents as a plain list, which is the shape {@link ContainerHelper} saves.
    *
    * <p>The bench used to write one {@code Item<n>} tag per slot with {@code ItemStack#save(provider, tag)}. In 1.21
    * that method returns the encoded tag rather than filling the one passed in, so every slot was written as an
    * empty compound - a blueprint in the blueprint slot came back as {@code Item11: {}} and was gone on the next
    * load. Marshalling through {@code ContainerHelper} uses the code path the rest of the mod's containers already
    * use, so the encoding is done by vanilla rather than by this class.</p>
    */
   private NonNullList<ItemStack> snapshotContents() {
      NonNullList<ItemStack> items = NonNullList.withSize(this.inventory.getContainerSize(), ItemStack.EMPTY);

      for (int i = 0; i < items.size(); i++) {
         items.set(i, this.inventory.getItem(i));
      }

      return items;
   }

   public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.loadAdditional(tag, registries);

      NonNullList<ItemStack> items = NonNullList.withSize(this.inventory.getContainerSize(), ItemStack.EMPTY);
      ContainerHelper.loadAllItems(tag, items, registries);

      for (int i = 0; i < items.size(); i++) {
         if (!items.get(i).isEmpty()) {
            this.inventory.setItem(i, items.get(i));
         }
      }

      // Saves made before this fix stored one "Item<n>" tag per slot. Those tags were written empty by the broken
      // encoder, but a save that predates that regression may still hold real data, so read them as a fallback.
      for (int i = 0; i < items.size(); i++) {
         CompoundTag legacy = tag.getCompound("Item" + i);

         if (!legacy.isEmpty() && this.inventory.getItem(i).isEmpty()) {
            this.inventory.setItem(i, top.ribs.scguns.util.NbtHelper.itemFromTag(legacy, registries));
         }
      }
   }

   protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.saveAdditional(tag, registries);
      // Vanilla writes the whole 12-slot grid as an "Items" list, skipping empty stacks itself. The previous
      // hand-rolled per-slot loop is explained above snapshotContents().
      ContainerHelper.saveAllItems(tag, this.snapshotContents(), registries);
   }

   public SimpleContainer getInventory() {
      return this.inventory;
   }

   public void dropContents(Player player) {
      for (int i = 0; i < this.inventory.getContainerSize(); i++) {
         if (i != 10) {
            ItemStack itemstack = this.inventory.getItem(i);
            if (!itemstack.isEmpty()) {
               if (player != null) {
                  player.drop(itemstack, false);
               } else {
                  assert this.level != null;

                  this.level
                     .addFreshEntity(
                        new ItemEntity(
                           this.level, (double)this.worldPosition.getX(), (double)this.worldPosition.getY(), (double)this.worldPosition.getZ(), itemstack
                        )
                     );
               }
            }
         }
      }

      this.inventory.clearContent();
   }
}
