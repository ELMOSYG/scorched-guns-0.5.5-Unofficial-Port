package top.ribs.scguns.blockentity;




import top.ribs.scguns.common.recipe.ContainerRecipeInput;
import net.minecraft.core.HolderLookup;
import top.ribs.scguns.util.Caps;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import top.ribs.scguns.block.LightningBattery;
import top.ribs.scguns.client.screen.LightningBatteryMenu;
import top.ribs.scguns.client.screen.LightningBatteryRecipe;
import top.ribs.scguns.init.ModBlockEntities;
import top.ribs.scguns.item.AirCanisterItem;

public class LightningBatteryBlockEntity extends BlockEntity implements MenuProvider {
   private static final int MAX_ENERGY = 32000;
   public final ItemStackHandler itemHandler = new ItemStackHandler(2) {
      protected void onContentsChanged(int slot) {
         LightningBatteryBlockEntity.this.setChanged();
         if (!LightningBatteryBlockEntity.this.level.isClientSide()) {
            LightningBatteryBlockEntity.this.level
               .sendBlockUpdated(
                  LightningBatteryBlockEntity.this.getBlockPos(), LightningBatteryBlockEntity.this.getBlockState(), LightningBatteryBlockEntity.this.getBlockState(), 3
               );
         }
      }
   };
   private IItemHandler lazyItemHandler = null;
   private final EnergyStorage energyStorage = new EnergyStorage(32000) {
      public int receiveEnergy(int maxReceive, boolean simulate) {
         int received = super.receiveEnergy(maxReceive, simulate);
         if (!simulate && received > 0) {
            LightningBatteryBlockEntity.this.setChanged();
            LightningBatteryBlockEntity.this.sync();
            LightningBatteryBlockEntity.this.updateBlockState();
         }

         return received;
      }

      public int extractEnergy(int maxExtract, boolean simulate) {
         int extracted = super.extractEnergy(maxExtract, simulate);
         if (!simulate && extracted > 0) {
            LightningBatteryBlockEntity.this.setChanged();
            LightningBatteryBlockEntity.this.sync();
            LightningBatteryBlockEntity.this.updateBlockState();
         }

         return extracted;
      }

      public boolean canExtract() {
         return true;
      }

      public boolean canReceive() {
         return true;
      }
   };
   private final IEnergyStorage internalEnergy = this.energyStorage;
   private final IEnergyStorage externalEnergy = new EnergyStorage(this.energyStorage.getMaxEnergyStored()) {
         public int receiveEnergy(int maxReceive, boolean simulate) {
            return LightningBatteryBlockEntity.this.energyStorage.receiveEnergy(maxReceive, simulate);
         }

         public int extractEnergy(int maxExtract, boolean simulate) {
            return LightningBatteryBlockEntity.this.energyStorage.extractEnergy(maxExtract, simulate);
         }

         public int getEnergyStored() {
            return LightningBatteryBlockEntity.this.energyStorage.getEnergyStored();
         }

         public int getMaxEnergyStored() {
            return LightningBatteryBlockEntity.this.energyStorage.getMaxEnergyStored();
         }

         public boolean canExtract() {
            return true;
         }

         public boolean canReceive() {
            return true;
         }
      };
   public static final int INPUT_SLOT = 0;
   public static final int OUTPUT_SLOT = 1;
   private int processingTime;
   private int processingTimeTotal;

   public LightningBatteryBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)ModBlockEntities.LIGHTNING_BATTERY.get(), pos, state);
   }

   public Component getDisplayName() {
      return Component.translatable("container.lightning_battery");
   }

   @Nullable
   public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
      return new LightningBatteryMenu(id, inv, this, new ContainerData() {
         public int get(int index) {
            return switch (index) {
               case 0 -> LightningBatteryBlockEntity.this.processingTime;
               case 1 -> LightningBatteryBlockEntity.this.processingTimeTotal;
               case 2 -> LightningBatteryBlockEntity.this.energyStorage.getEnergyStored();
               case 3 -> LightningBatteryBlockEntity.this.energyStorage.getMaxEnergyStored();
               default -> 0;
            };
         }

         public void set(int index, int value) {
            switch (index) {
               case 0:
                  LightningBatteryBlockEntity.this.processingTime = value;
                  break;
               case 1:
                  LightningBatteryBlockEntity.this.processingTimeTotal = value;
            }
         }

         public int getCount() {
            return 4;
         }
      });
   }

   public void onLoad() {
      super.onLoad();
      this.lazyItemHandler = this.itemHandler;
   }

   public <T> T getCapability(Object cap, @Nullable Direction side) {
      if (cap == Capabilities.ItemHandler.BLOCK) {
         if (side == null) {
            return ((T) this.lazyItemHandler);
         } else {
            return side == Direction.DOWN
               ? ((T) new LightningBatteryBlockEntity.OutputItemHandler(this.itemHandler))
               : ((T) new LightningBatteryBlockEntity.InputItemHandler(this.itemHandler));
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
      tag.put("Energy", this.energyStorage.serializeNBT(registries));
      tag.putInt("ProcessingTime", this.processingTime);
      tag.putInt("ProcessingTimeTotal", this.processingTimeTotal);
   }

   public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.loadAdditional(tag, registries);
      this.itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
      this.energyStorage.deserializeNBT(registries, tag.get("Energy"));
      this.processingTime = tag.getInt("ProcessingTime");
      this.processingTimeTotal = tag.getInt("ProcessingTimeTotal");
   }

   @Nullable
   public Packet<ClientGamePacketListener> getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @NotNull
   public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
      return this.saveWithoutMetadata(registries);
   }

   public void drops() {
      SimpleContainer inventory = new SimpleContainer(this.itemHandler.getSlots());

      for (int i = 0; i < this.itemHandler.getSlots(); i++) {
         inventory.setItem(i, this.itemHandler.getStackInSlot(i));
      }

      assert this.level != null;

      Containers.dropContents(this.level, this.worldPosition, inventory);
   }

   private void sync() {
      if (this.level != null && !this.level.isClientSide) {
         this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 2);
      }
   }

   public int getEnergy() {
      return this.energyStorage.getEnergyStored();
   }

   public int getMaxEnergy() {
      return this.energyStorage.getMaxEnergyStored();
   }

   public void setEnergyStored(int i) {
      this.energyStorage.receiveEnergy(i, false);
      this.updateBlockState();
   }

   public void tick() {
      if (this.level != null && !this.level.isClientSide) {
         boolean wasProcessing = this.isProcessing();
         ItemStack inputStack = this.itemHandler.getStackInSlot(0);
         ItemStack outputStack = this.itemHandler.getStackInSlot(1);
         if (!inputStack.isEmpty()) {
            IEnergyStorage itemEnergyCap = inputStack.getCapability(Capabilities.EnergyStorage.ITEM);
            if (itemEnergyCap != null) {
               Caps.ifPresent(itemEnergyCap, itemEnergy -> {
                  if (inputStack.getItem() instanceof AirCanisterItem) {
                     this.chargeAirCanister(inputStack, outputStack);
                  } else {
                     int energyToTransfer = Math.min(this.energyStorage.extractEnergy(100, true), itemEnergy.receiveEnergy(100, true));
                     if (energyToTransfer > 0) {
                        this.energyStorage.extractEnergy(energyToTransfer, false);
                        itemEnergy.receiveEnergy(energyToTransfer, false);
                        this.setChanged();
                        this.sync();
                     }

                     if (itemEnergy.getEnergyStored() >= itemEnergy.getMaxEnergyStored() && outputStack.isEmpty()) {
                        this.itemHandler.setStackInSlot(1, inputStack.copy());
                        this.itemHandler.extractItem(0, 1, false);
                     }
                  }
               });
            } else {
               LightningBatteryRecipe recipe = this.getRecipe(inputStack);
               if (recipe != null) {
                  ItemStack recipeOutput = recipe.getResultItem(RegistryAccess.EMPTY);
                  if (outputStack.isEmpty()
                     || outputStack.is(recipeOutput.getItem()) && outputStack.getCount() + recipeOutput.getCount() <= outputStack.getMaxStackSize()) {
                     if (this.hasEnoughEnergy(recipe.getEnergyUse())) {
                        this.processingTime++;
                        this.processingTimeTotal = recipe.getProcessingTime();
                        if (this.processingTime >= this.processingTimeTotal) {
                           this.consumeEnergy(recipe.getEnergyUse());
                           inputStack.shrink(1);
                           if (outputStack.isEmpty()) {
                              this.itemHandler.setStackInSlot(1, recipeOutput.copy());
                           } else {
                              outputStack.grow(recipeOutput.getCount());
                           }

                           this.processingTime = 0;
                        }
                     } else {
                        this.processingTime = 0;
                     }
                  } else {
                     this.processingTime = 0;
                  }
               } else {
                  this.processingTime = 0;
               }
            }
         } else {
            this.processingTime = 0;
         }

         if (wasProcessing != this.isProcessing()) {
            this.setChanged();
            this.sync();
         } else {
            this.setChanged();
         }

         this.updateBlockState();

         for (Direction direction : Direction.values()) {
            BlockEntity adjacentEntity = this.level.getBlockEntity(this.worldPosition.relative(direction));
            if (adjacentEntity != null) {
               Caps.ifPresent(Caps.<IEnergyStorage>of(adjacentEntity, Capabilities.EnergyStorage.BLOCK, direction.getOpposite()), handler -> {
                  if (handler.canReceive()) {
                     int extracted = this.energyStorage.extractEnergy(50, true);
                     int accepted = handler.receiveEnergy(extracted, false);
                     this.energyStorage.extractEnergy(accepted, false);
                     this.setChanged();
                     this.sync();
                  }
               });
            }
         }
      }
   }

   private void chargeAirCanister(ItemStack inputStack, ItemStack outputStack) {
      IEnergyStorage airStorage = inputStack.getCapability(Capabilities.EnergyStorage.ITEM);
      Caps.ifPresent(airStorage, airCap -> {
         int energyCostPerAir = 5;
         int airPerTick = 5;
         int energyCostPerTick = airPerTick * energyCostPerAir;
         if (this.energyStorage.getEnergyStored() >= energyCostPerTick && airCap.getEnergyStored() < airCap.getMaxEnergyStored()) {
            int maxAirToAdd = Math.min(airPerTick, airCap.getMaxEnergyStored() - airCap.getEnergyStored());
            int actualEnergyCost = maxAirToAdd * energyCostPerAir;
            if (this.energyStorage.getEnergyStored() >= actualEnergyCost) {
               int airAdded = airCap.receiveEnergy(maxAirToAdd, false);
               if (airAdded > 0) {
                  int actualCost = airAdded * energyCostPerAir;
                  this.energyStorage.extractEnergy(actualCost, false);
                  this.setChanged();
                  this.sync();
               }
            }
         }

         if (airCap.getEnergyStored() >= airCap.getMaxEnergyStored() && outputStack.isEmpty()) {
            this.itemHandler.setStackInSlot(1, inputStack.copy());
            this.itemHandler.extractItem(0, 1, false);
         }
      });
   }

   private LightningBatteryRecipe getRecipe(ItemStack inputStack) {
      return this.level == null
         ? null
         : (LightningBatteryRecipe)this.level
            .getRecipeManager()
            .getRecipeFor(LightningBatteryRecipe.Type.INSTANCE, new ContainerRecipeInput(new SimpleContainer(new ItemStack[]{inputStack})), this.level).map(net.minecraft.world.item.crafting.RecipeHolder::value)
            .orElse(null);
   }

   public boolean isProcessing() {
      return this.processingTime > 0 && this.processingTime < this.processingTimeTotal;
   }

   private boolean hasEnoughEnergy(int energyUse) {
      return this.energyStorage.getEnergyStored() >= energyUse;
   }

   private void consumeEnergy(int amount) {
      this.energyStorage.extractEnergy(amount, false);
   }

   private void updateBlockState() {
      if (this.level != null && !this.level.isClientSide) {
         BlockState state = this.level.getBlockState(this.worldPosition);
         int energyStored = this.getEnergy();
         boolean isCharged = energyStored > 0;
         LightningBattery.ChargeLevel chargeLevel = calculateChargeLevel(energyStored);
         this.level
            .setBlock(
               this.worldPosition,
               (BlockState)((BlockState)state.setValue(LightningBattery.CHARGED, isCharged)).setValue(LightningBattery.CHARGE_LEVEL, chargeLevel),
               3
            );
      }
   }

   public static LightningBattery.ChargeLevel calculateChargeLevel(int energyStored) {
      if (energyStored > 24000) {
         return LightningBattery.ChargeLevel.HIGH;
      } else if (energyStored > 12000) {
         return LightningBattery.ChargeLevel.MID;
      } else {
         return energyStored > 0 ? LightningBattery.ChargeLevel.LOW : LightningBattery.ChargeLevel.NONE;
      }
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
         return slot == 0 ? this.itemHandler.insertItem(slot, stack, simulate) : stack;
      }

      public ItemStack extractItem(int slot, int amount, boolean simulate) {
         return ItemStack.EMPTY;
      }

      public int getSlotLimit(int slot) {
         return this.itemHandler.getSlotLimit(slot);
      }

      public boolean isItemValid(int slot, ItemStack stack) {
         return slot == 0;
      }
   }

   private static class OutputItemHandler implements IItemHandlerModifiable {
      private final ItemStackHandler itemHandler;

      public OutputItemHandler(ItemStackHandler itemHandler) {
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
         return stack;
      }

      public ItemStack extractItem(int slot, int amount, boolean simulate) {
         return slot == 1 ? this.itemHandler.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
      }

      public int getSlotLimit(int slot) {
         return this.itemHandler.getSlotLimit(slot);
      }

      public boolean isItemValid(int slot, @NotNull ItemStack stack) {
         return false;
      }
   }
}
