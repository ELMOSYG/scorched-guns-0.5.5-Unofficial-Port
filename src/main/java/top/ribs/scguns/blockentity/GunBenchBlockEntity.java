package top.ribs.scguns.blockentity;


import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
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

   public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.loadAdditional(tag, registries);

      for (int i = 0; i < this.inventory.getContainerSize(); i++) {
         CompoundTag itemTag = tag.getCompound("Item" + i);
         if (!itemTag.isEmpty()) {
            this.inventory.setItem(i, top.ribs.scguns.util.NbtHelper.itemFromTag(itemTag));
         }
      }
   }

   protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.saveAdditional(tag, registries);

      for (int i = 0; i < this.inventory.getContainerSize(); i++) {
         ItemStack itemstack = this.inventory.getItem(i);
         // ItemStack#save throws "Cannot encode empty ItemStack" on an empty stack, and a bench
         // always has empty slots - so this loop used to throw on every chunk save, and the block
         // entity was dropped with "It will not persist": the bench silently lost everything in it
         // (HANDOFF section 45). Load treats a missing tag as empty, so skipping is enough.
         if (itemstack.isEmpty()) {
            continue;
         }

         CompoundTag itemTag = new CompoundTag();
         itemstack.save(registries, itemTag);
         tag.put("Item" + i, itemTag);
      }
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
