package top.ribs.scguns.blockentity;



import top.ribs.scguns.common.recipe.ContainerRecipeInput;
import net.minecraft.core.HolderLookup;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
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
import org.jetbrains.annotations.Nullable;
import top.ribs.scguns.block.PoweredMaceratorBlock;
import top.ribs.scguns.client.screen.MaceratorRecipe;
import top.ribs.scguns.client.screen.PoweredMaceratorMenu;
import top.ribs.scguns.init.ModBlockEntities;

public class PoweredMaceratorBlockEntity extends BlockEntity implements MenuProvider {
   public final ItemStackHandler itemHandler = new ItemStackHandler(5) {
      protected void onContentsChanged(int slot) {
         PoweredMaceratorBlockEntity.this.setChanged();
         if (!PoweredMaceratorBlockEntity.this.level.isClientSide()) {
            PoweredMaceratorBlockEntity.this.level
               .sendBlockUpdated(
                  PoweredMaceratorBlockEntity.this.getBlockPos(), PoweredMaceratorBlockEntity.this.getBlockState(), PoweredMaceratorBlockEntity.this.getBlockState(), 3
               );
            if (PoweredMaceratorBlockEntity.this.isInputSlot(slot) && !PoweredMaceratorBlockEntity.this.isRecipeValid()) {
               PoweredMaceratorBlockEntity.this.resetProgress();
            }
         }
      }
   };
   private IItemHandler lazyItemHandler = null;
   private final ContainerData data;
   private int progress = 0;
   private int maxProgress = 100;
   private final EnergyStorage energyStorage = new EnergyStorage(16000) {
      public int receiveEnergy(int maxReceive, boolean simulate) {
         int received = super.receiveEnergy(maxReceive, simulate);
         if (!simulate && received > 0) {
            PoweredMaceratorBlockEntity.this.setChanged();
            PoweredMaceratorBlockEntity.this.sync();
         }

         return received;
      }

      public int extractEnergy(int maxExtract, boolean simulate) {
         int extracted = super.extractEnergy(maxExtract, simulate);
         if (!simulate && extracted > 0) {
            PoweredMaceratorBlockEntity.this.setChanged();
            PoweredMaceratorBlockEntity.this.sync();
         }

         return extracted;
      }
   };
   private final IEnergyStorage energy = this.energyStorage;
   public static final int FIRST_INPUT_SLOT = 0;
   public static final int LAST_INPUT_SLOT = 3;
   public static final int OUTPUT_SLOT = 4;
   private static final float WHEEL_ROTATION_SPEED = 20.0F;

   private boolean isInputSlot(int slot) {
      return slot >= 0 && slot <= 3;
   }

   public PoweredMaceratorBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)ModBlockEntities.POWERED_MACERATOR.get(), pos, state);
      this.data = new ContainerData() {
         public int get(int index) {
            switch (index) {
               case 0:
                  return PoweredMaceratorBlockEntity.this.progress;
               case 1:
                  return PoweredMaceratorBlockEntity.this.maxProgress;
               case 2:
                  return PoweredMaceratorBlockEntity.this.energyStorage.getEnergyStored();
               case 3:
                  return PoweredMaceratorBlockEntity.this.energyStorage.getMaxEnergyStored();
               default:
                  return 0;
            }
         }

         public void set(int index, int value) {
            switch (index) {
               case 0:
                  PoweredMaceratorBlockEntity.this.progress = value;
                  break;
               case 1:
                  PoweredMaceratorBlockEntity.this.maxProgress = value;
            }
         }

         public int getCount() {
            return 4;
         }
      };
   }

   public Component getDisplayName() {
      return Component.translatable("container.powered_macerator");
   }

   @Nullable
   public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
      return new PoweredMaceratorMenu(id, inv, this, this.data);
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
               ? ((T) new PoweredMaceratorBlockEntity.OutputItemHandler(this.itemHandler))
               : ((T) new PoweredMaceratorBlockEntity.InputItemHandler(this.itemHandler));
         }
      } else {
         return cap == Capabilities.EnergyStorage.BLOCK ? ((T) this.energy) : null;
      }
   }

   protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.saveAdditional(tag, registries);
      tag.put("inventory", this.itemHandler.serializeNBT(registries));
      tag.putInt("powered_macerator.progress", this.progress);
      tag.put("Energy", this.energyStorage.serializeNBT(registries));
   }

   public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.loadAdditional(tag, registries);
      this.itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
      this.progress = tag.getInt("powered_macerator.progress");
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

   public static void tick(Level level, BlockPos pos, BlockState state, PoweredMaceratorBlockEntity blockEntity) {
      boolean wasLit = (Boolean)state.getValue(PoweredMaceratorBlock.LIT);
      boolean isLit = false;
      if (!level.isClientSide) {
         boolean hasValidRecipe = blockEntity.hasRecipe();
         ItemStack resultItem = blockEntity.getRecipeResult();
         if (hasValidRecipe && blockEntity.hasEnoughEnergy(50) && blockEntity.hasSpaceForOutput(resultItem)) {
            isLit = true;
            blockEntity.progress++;
            blockEntity.consumeEnergy(50);
            if (blockEntity.progress >= blockEntity.maxProgress) {
               blockEntity.craftItem();
               blockEntity.resetProgress();
            }
         } else if (blockEntity.progress > 0) {
            blockEntity.resetProgress();
         }

         if (wasLit != isLit) {
            level.setBlock(pos, (BlockState)state.setValue(PoweredMaceratorBlock.LIT, isLit), 3);
         }
      }
   }

   private ItemStack getRecipeResult() {
      SimpleContainer inventory = new SimpleContainer(4);

      for (int i = 0; i <= 3; i++) {
         inventory.setItem(i - 0, this.itemHandler.getStackInSlot(i));
      }

      assert this.level != null;

      Optional<MaceratorRecipe> match = this.level.getRecipeManager().getRecipeFor(MaceratorRecipe.Type.INSTANCE, new ContainerRecipeInput(inventory), this.level).map(net.minecraft.world.item.crafting.RecipeHolder::value);
      return match.<ItemStack>map(recipe -> recipe.getResultItem(this.level.registryAccess())).orElse(ItemStack.EMPTY);
   }

   private boolean hasSpaceForOutput(ItemStack output) {
      ItemStack currentOutput = this.itemHandler.getStackInSlot(4);
      return currentOutput.isEmpty()
         || currentOutput.getItem() == output.getItem() && currentOutput.getCount() + output.getCount() <= currentOutput.getMaxStackSize();
   }

   private boolean hasRecipe() {
      SimpleContainer inventory = new SimpleContainer(4);

      for (int i = 0; i <= 3; i++) {
         inventory.setItem(i - 0, this.itemHandler.getStackInSlot(i));
      }

      assert this.level != null;

      Optional<MaceratorRecipe> match = this.level.getRecipeManager().getRecipeFor(MaceratorRecipe.Type.INSTANCE, new ContainerRecipeInput(inventory), this.level).map(net.minecraft.world.item.crafting.RecipeHolder::value);
      return match.isPresent();
   }

   private void craftItem() {
      SimpleContainer inventory = new SimpleContainer(4);

      for (int i = 0; i <= 3; i++) {
         inventory.setItem(i - 0, this.itemHandler.getStackInSlot(i));
      }

      assert this.level != null;

      Optional<MaceratorRecipe> match = this.level.getRecipeManager().getRecipeFor(MaceratorRecipe.Type.INSTANCE, new ContainerRecipeInput(inventory), this.level).map(net.minecraft.world.item.crafting.RecipeHolder::value);
      if (match.isPresent()) {
         MaceratorRecipe recipe = match.get();
         ItemStack resultItem = recipe.getResultItem(this.level.registryAccess());
         ItemStack outputStack = this.itemHandler.getStackInSlot(4);
         if (outputStack.isEmpty()
            || outputStack.getItem() == resultItem.getItem() && outputStack.getCount() + resultItem.getCount() <= outputStack.getMaxStackSize()) {
            for (int i = 0; i <= 3; i++) {
               this.itemHandler.extractItem(i, 1, false);
            }

            if (outputStack.isEmpty()) {
               this.itemHandler.setStackInSlot(4, resultItem.copy());
            } else {
               outputStack.grow(resultItem.getCount());
            }
         }
      }
   }

   private void resetProgress() {
      this.progress = 0;
   }

   private boolean isRecipeValid() {
      SimpleContainer inventory = new SimpleContainer(4);

      for (int i = 0; i <= 3; i++) {
         inventory.setItem(i - 0, this.itemHandler.getStackInSlot(i));
      }

      Optional<MaceratorRecipe> currentRecipe = this.getCurrentRecipe();
      if (currentRecipe.isPresent()) {
         MaceratorRecipe recipe = currentRecipe.get();
         return recipe.matches(new ContainerRecipeInput(inventory), this.level);
      } else {
         return false;
      }
   }

   private Optional<MaceratorRecipe> getCurrentRecipe() {
      if (this.level == null) {
         return Optional.empty();
      } else {
         RecipeManager recipeManager = this.level.getRecipeManager();
         SimpleContainer inventory = new SimpleContainer(4);

         for (int i = 0; i <= 3; i++) {
            inventory.setItem(i - 0, this.itemHandler.getStackInSlot(i));
         }

         return recipeManager.getAllRecipesFor(MaceratorRecipe.Type.INSTANCE).stream().filter(recipe -> recipe.value().matches(new ContainerRecipeInput(inventory), this.level)).findFirst().map(net.minecraft.world.item.crafting.RecipeHolder::value);
      }
   }

   private boolean hasEnoughEnergy(int amount) {
      return this.energyStorage.getEnergyStored() >= amount;
   }

   public void consumeEnergy(int amount) {
      this.energyStorage.extractEnergy(amount, false);
      this.setChanged();
      this.sync();
   }

   public void addEnergy(int amount) {
      this.energyStorage.receiveEnergy(amount, false);
      this.setChanged();
      this.sync();
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

   public float getWheelRotation(float partialTicks) {
      assert this.level != null;

      return ((float)this.level.getGameTime() + partialTicks) * 20.0F % 360.0F;
   }

   private static record InputItemHandler(ItemStackHandler itemHandler) implements IItemHandlerModifiable {
      private InputItemHandler(ItemStackHandler itemHandler) {
         this.itemHandler = itemHandler;
      }

      public void setStackInSlot(int slot, ItemStack stack) {
         if (slot >= 0 && slot <= 3) {
            this.itemHandler.setStackInSlot(slot, stack);
         }
      }

      public int getSlots() {
         return 4;
      }

      public ItemStack getStackInSlot(int slot) {
         return slot >= 0 && slot < this.getSlots() ? this.itemHandler.getStackInSlot(0 + slot) : ItemStack.EMPTY;
      }

      public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
         if (slot >= 0 && slot < this.getSlots()) {
            int actualSlot = 0 + slot;
            ItemStack result = this.itemHandler.insertItem(actualSlot, stack, simulate);
            if (!result.equals(stack)) {
               return result;
            }
         }

         return this.insertIntoNextAvailableSlot(stack, simulate);
      }

      private ItemStack insertIntoNextAvailableSlot(ItemStack stack, boolean simulate) {
         for (int i = 0; i <= 3; i++) {
            ItemStack existingStack = this.itemHandler.getStackInSlot(i);
            if (!existingStack.isEmpty() && ItemStack.isSameItem(existingStack, stack)) {
               ItemStack result = this.itemHandler.insertItem(i, stack, simulate);
               if (!result.equals(stack)) {
                  return result;
               }
            }
         }

         for (int ix = 0; ix <= 3; ix++) {
            ItemStack existingStack = this.itemHandler.getStackInSlot(ix);
            if (existingStack.isEmpty()) {
               return this.itemHandler.insertItem(ix, stack, simulate);
            }
         }

         return stack;
      }

      public ItemStack extractItem(int slot, int amount, boolean simulate) {
         return ItemStack.EMPTY;
      }

      public int getSlotLimit(int slot) {
         return slot >= 0 && slot < this.getSlots() ? this.itemHandler.getSlotLimit(0 + slot) : 0;
      }

      public boolean isItemValid(int slot, ItemStack stack) {
         return slot >= 0 && slot < this.getSlots();
      }
   }

   private static record OutputItemHandler(ItemStackHandler itemHandler) implements IItemHandlerModifiable {
      private OutputItemHandler(ItemStackHandler itemHandler) {
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
         return slot == 4 ? this.itemHandler.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
      }

      public int getSlotLimit(int slot) {
         return this.itemHandler.getSlotLimit(slot);
      }

      public boolean isItemValid(int slot, @NotNull ItemStack stack) {
         return false;
      }
   }
}
