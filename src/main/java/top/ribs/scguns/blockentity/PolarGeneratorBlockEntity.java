package top.ribs.scguns.blockentity;





import top.ribs.scguns.util.ScFuels;
import net.minecraft.core.HolderLookup;
import top.ribs.scguns.util.Caps;
import top.ribs.scguns.util.NbtHelper;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import top.ribs.scguns.Config;
import top.ribs.scguns.client.screen.PolarGeneratorMenu;
import top.ribs.scguns.init.ModBlockEntities;

public class PolarGeneratorBlockEntity extends BlockEntity implements MenuProvider {
   private final EnergyStorage energyStorage = new EnergyStorage(24000) {
      public int receiveEnergy(int maxReceive, boolean simulate) {
         int received = super.receiveEnergy(maxReceive, simulate);
         if (!simulate && received > 0) {
            PolarGeneratorBlockEntity.this.setChanged();
            PolarGeneratorBlockEntity.this.sync();
         }

         return received;
      }

      public int extractEnergy(int maxExtract, boolean simulate) {
         int extracted = super.extractEnergy(maxExtract, simulate);
         if (!simulate && extracted > 0) {
            PolarGeneratorBlockEntity.this.setChanged();
            PolarGeneratorBlockEntity.this.sync();
         }

         return extracted;
      }
   };
   private final IEnergyStorage internalEnergy = this.energyStorage;
   private final IEnergyStorage externalEnergy = new EnergyStorage(this.energyStorage.getMaxEnergyStored()) {
         public int receiveEnergy(int maxReceive, boolean simulate) {
            return 0;
         }

         public int extractEnergy(int maxExtract, boolean simulate) {
            return PolarGeneratorBlockEntity.this.energyStorage.extractEnergy(maxExtract, simulate);
         }

         public int getEnergyStored() {
            return PolarGeneratorBlockEntity.this.energyStorage.getEnergyStored();
         }

         public int getMaxEnergyStored() {
            return PolarGeneratorBlockEntity.this.energyStorage.getMaxEnergyStored();
         }

         public boolean canExtract() {
            return true;
         }

         public boolean canReceive() {
            return false;
         }
      };
   private final ItemStackHandler itemHandler = this.createHandler();
   private final IItemHandlerModifiable manualHandler = this.itemHandler;
   private final IItemHandler topHandler = new PolarGeneratorBlockEntity.InputItemHandler(this.itemHandler);
   private final IItemHandler sideHandler = new PolarGeneratorBlockEntity.SideItemHandler(this.itemHandler);
   private int burnTime;
   private int burnTimeTotal;
   private static final float WHEEL_ROTATION_SPEED = 20.0F;
   protected final ContainerData data = new ContainerData() {
      public int get(int index) {
         return switch (index) {
            case 0 -> PolarGeneratorBlockEntity.this.burnTime;
            case 1 -> PolarGeneratorBlockEntity.this.burnTimeTotal;
            case 2 -> PolarGeneratorBlockEntity.this.energyStorage.getEnergyStored();
            case 3 -> PolarGeneratorBlockEntity.this.energyStorage.getMaxEnergyStored();
            default -> 0;
         };
      }

      public void set(int index, int value) {
         switch (index) {
            case 0:
               PolarGeneratorBlockEntity.this.burnTime = value;
               break;
            case 1:
               PolarGeneratorBlockEntity.this.burnTimeTotal = value;
         }
      }

      public int getCount() {
         return 4;
      }
   };

   public PolarGeneratorBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)ModBlockEntities.POLAR_GENERATOR.get(), pos, state);
   }

   @NotNull
   public Component getDisplayName() {
      return Component.translatable("container.polar_generator");
   }

   @Nullable
   public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player playerEntity) {
      return new PolarGeneratorMenu(id, playerInventory, this, this.data);
   }

   private ItemStackHandler createHandler() {
      return new ItemStackHandler(1) {
         protected void onContentsChanged(int slot) {
            PolarGeneratorBlockEntity.this.setChanged();
            PolarGeneratorBlockEntity.this.sync();
         }

         public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && ScFuels.burnTime(stack) > 0;
         }

         @NotNull
         public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return slot == 0 ? super.insertItem(slot, stack, simulate) : stack;
         }

         @NotNull
         public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return super.extractItem(slot, amount, simulate);
         }
      };
   }

   @NotNull
   public <T> T getCapability(Object cap, @Nullable Direction side) {
      if (cap == Capabilities.ItemHandler.BLOCK) {
         if (side == null) {
            return ((T) this.manualHandler);
         } else {
            return side == Direction.UP ? ((T) this.topHandler) : ((T) this.sideHandler);
         }
      } else if (cap == Capabilities.EnergyStorage.BLOCK) {
         return side == null ? ((T) this.internalEnergy) : ((T) this.externalEnergy);
      } else {
         return null;
      }
   }

   protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.saveAdditional(tag, registries);
      tag.put("inventory", this.itemHandler.serializeNBT(registries));
      tag.putInt("polar_generator.burnTime", this.burnTime);
      tag.putInt("polar_generator.burnTimeTotal", this.burnTimeTotal);
      tag.put("Energy", this.energyStorage.serializeNBT(registries));
   }

   public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.loadAdditional(tag, registries);
      this.itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
      this.burnTime = tag.getInt("polar_generator.burnTime");
      this.burnTimeTotal = tag.getInt("polar_generator.burnTimeTotal");
      this.energyStorage.deserializeNBT(registries, tag.get("Energy"));
   }

   @Nullable
   public Packet<ClientGamePacketListener> getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @NotNull
   public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
      return this.saveWithoutMetadata(registries);
   }

   public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
      CompoundTag tag = pkt.getTag();
      if (tag != null) {
         this.handleUpdateTag(tag, registries);
      }
   }

   /** 1.20.1 entry point, kept for API compatibility. */
   public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
      this.onDataPacket(net, pkt, this.level == null ? RegistryAccess.EMPTY : this.level.registryAccess());
   }

   private void sync() {
      if (this.level != null && !this.level.isClientSide) {
         this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 2);
      }
   }

   public static void tick(Level level, BlockPos pos, BlockState state, PolarGeneratorBlockEntity blockEntity) {
      if (!level.isClientSide) {
         if (blockEntity.burnTime > 0) {
            blockEntity.burnTime--;
            blockEntity.energyStorage.receiveEnergy((Integer)Config.COMMON.gameplay.energyProductionRate.get(), false);
            blockEntity.setChanged();
            blockEntity.sync();
         }

         for (Direction direction : Direction.values()) {
            BlockEntity adjacentEntity = level.getBlockEntity(pos.relative(direction));
            if (adjacentEntity != null) {
               Caps.ifPresent(Caps.<IEnergyStorage>of(adjacentEntity, Capabilities.EnergyStorage.BLOCK, direction.getOpposite()), handler -> {
                  if (handler.canReceive()) {
                     int extracted = blockEntity.energyStorage.extractEnergy((Integer)Config.COMMON.gameplay.energyProductionRate.get(), true);
                     int accepted = handler.receiveEnergy(extracted, false);
                     blockEntity.energyStorage.extractEnergy(accepted, false);
                     blockEntity.setChanged();
                     blockEntity.sync();
                  }
               });
            }
         }

         if (blockEntity.burnTime == 0 && blockEntity.energyStorage.getEnergyStored() < blockEntity.energyStorage.getMaxEnergyStored()) {
            ItemStack fuelStack = blockEntity.itemHandler.getStackInSlot(0);
            if (!fuelStack.isEmpty()) {
               int burnTime = ScFuels.burnTime(fuelStack);
               if (burnTime > 0) {
                  blockEntity.burnTime = burnTime;
                  blockEntity.burnTimeTotal = burnTime;
                  ItemStack containerItem = fuelStack.getCraftingRemainingItem();
                  fuelStack.shrink(1);
                  if (!containerItem.isEmpty() && fuelStack.isEmpty()) {
                     blockEntity.itemHandler.setStackInSlot(0, containerItem);
                  } else if (!containerItem.isEmpty() && !fuelStack.isEmpty()) {
                     ItemStack remainder = blockEntity.itemHandler.insertItem(0, containerItem, false);
                     if (!remainder.isEmpty() && blockEntity.level != null) {
                        Containers.dropItemStack(
                           blockEntity.level,
                           (double)blockEntity.worldPosition.getX() + 0.5,
                           (double)blockEntity.worldPosition.getY() + 1.0,
                           (double)blockEntity.worldPosition.getZ() + 0.5,
                           remainder
                        );
                     }
                  }

                  blockEntity.setChanged();
                  blockEntity.sync();
               }
            }
         }

         boolean isLit = blockEntity.burnTime > 0;
         if ((Boolean)state.getValue(BlockStateProperties.LIT) != isLit) {
            level.setBlock(pos, (BlockState)state.setValue(BlockStateProperties.LIT, isLit), 3);
         }
      }
   }

   public float getWheelRotation(float partialTicks) {
      assert this.level != null;

      return ((float)this.level.getGameTime() + partialTicks) * 20.0F % 360.0F;
   }

   public void drops() {
      SimpleContainer inventory = new SimpleContainer(this.itemHandler.getSlots());

      for (int i = 0; i < this.itemHandler.getSlots(); i++) {
         inventory.setItem(i, this.itemHandler.getStackInSlot(i));
      }

      assert this.level != null;

      Containers.dropContents(this.level, this.worldPosition, inventory);
   }

   private static class InputItemHandler implements IItemHandlerModifiable {
      private final ItemStackHandler itemHandler;

      public InputItemHandler(ItemStackHandler itemHandler) {
         super();
         this.itemHandler = itemHandler;
      }

      public void setStackInSlot(int slot, ItemStack stack) {
         this.itemHandler.setStackInSlot(slot, stack);
      }

      public int getSlots() {
         return this.itemHandler.getSlots();
      }

      public ItemStack getStackInSlot(int slot) {
         return this.itemHandler.getStackInSlot(slot);
      }

      public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
         return slot == 0 && ScFuels.burnTime(stack) > 0 ? this.itemHandler.insertItem(slot, stack, simulate) : stack;
      }

      public ItemStack extractItem(int slot, int amount, boolean simulate) {
         return this.itemHandler.extractItem(slot, amount, simulate);
      }

      public int getSlotLimit(int slot) {
         return this.itemHandler.getSlotLimit(slot);
      }

      public boolean isItemValid(int slot, ItemStack stack) {
         return slot == 0 && ScFuels.burnTime(stack) > 0;
      }
   }

   private static class SideItemHandler implements IItemHandlerModifiable {
      private final ItemStackHandler itemHandler;

      public SideItemHandler(ItemStackHandler itemHandler) {
         super();
         this.itemHandler = itemHandler;
      }

      public void setStackInSlot(int slot, ItemStack stack) {
         this.itemHandler.setStackInSlot(slot, stack);
      }

      public int getSlots() {
         return this.itemHandler.getSlots();
      }

      public ItemStack getStackInSlot(int slot) {
         return this.itemHandler.getStackInSlot(slot);
      }

      public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
         return slot == 0 && ScFuels.burnTime(stack) > 0 ? this.itemHandler.insertItem(slot, stack, simulate) : stack;
      }

      public ItemStack extractItem(int slot, int amount, boolean simulate) {
         return this.itemHandler.extractItem(slot, amount, simulate);
      }

      public int getSlotLimit(int slot) {
         return this.itemHandler.getSlotLimit(slot);
      }

      public boolean isItemValid(int slot, ItemStack stack) {
         return slot == 0 && ScFuels.burnTime(stack) > 0;
      }
   }
}
