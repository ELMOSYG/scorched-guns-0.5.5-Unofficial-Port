package top.ribs.scguns.blockentity;




import top.ribs.scguns.common.recipe.ContainerRecipeInput;
import top.ribs.scguns.util.ScFuels;
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
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.ribs.scguns.block.MaceratorBlock;
import top.ribs.scguns.client.screen.MaceratorMenu;
import top.ribs.scguns.client.screen.MaceratorRecipe;
import top.ribs.scguns.init.ModBlockEntities;

public class MaceratorBlockEntity extends BlockEntity implements MenuProvider {
   public final ItemStackHandler itemHandler = new ItemStackHandler(6) {
      protected void onContentsChanged(int slot) {
         MaceratorBlockEntity.this.setChanged();
         if (!MaceratorBlockEntity.this.level.isClientSide()) {
            MaceratorBlockEntity.this.level
               .sendBlockUpdated(MaceratorBlockEntity.this.getBlockPos(), MaceratorBlockEntity.this.getBlockState(), MaceratorBlockEntity.this.getBlockState(), 3);
            if (MaceratorBlockEntity.this.isInputSlot(slot) && !MaceratorBlockEntity.this.isRecipeValid()) {
               MaceratorBlockEntity.this.resetProgress();
            }
         }
      }

      public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
         if (slot >= 0 && slot <= 3) {
            ItemStack existingStack = MaceratorBlockEntity.this.itemHandler.getStackInSlot(slot);
            if (!existingStack.isEmpty() && !ItemStack.isSameItem(existingStack, stack)) {
               return stack;
            }
         }

         return super.insertItem(slot, stack, simulate);
      }
   };
   private IItemHandler lazyItemHandler = null;
   private final ContainerData data;
   private int progress = 0;
   private int maxProgress = 100;
   private int burnTime = 0;
   private int maxBurnTime = 0;
   private static final float WHEEL_ROTATION_SPEED = 20.0F;
   public static final int FIRST_INPUT_SLOT = 0;
   public static final int LAST_INPUT_SLOT = 3;
   public static final int FUEL_SLOT = 4;
   public static final int OUTPUT_SLOT = 5;

   private boolean isInputSlot(int slot) {
      return slot >= 0 && slot <= 3;
   }

   public MaceratorBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)ModBlockEntities.MACERATOR.get(), pos, state);
      this.data = new ContainerData() {
         public int get(int index) {
            switch (index) {
               case 0:
                  return MaceratorBlockEntity.this.progress;
               case 1:
                  return MaceratorBlockEntity.this.maxProgress;
               case 2:
                  return MaceratorBlockEntity.this.burnTime;
               case 3:
                  return MaceratorBlockEntity.this.maxBurnTime;
               default:
                  return 0;
            }
         }

         public void set(int index, int value) {
            switch (index) {
               case 0:
                  MaceratorBlockEntity.this.progress = value;
                  break;
               case 1:
                  MaceratorBlockEntity.this.maxProgress = value;
                  break;
               case 2:
                  MaceratorBlockEntity.this.burnTime = value;
                  break;
               case 3:
                  MaceratorBlockEntity.this.maxBurnTime = value;
            }
         }

         public int getCount() {
            return 4;
         }
      };
   }

   public Component getDisplayName() {
      return Component.translatable("container.macerator");
   }

   @Nullable
   public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
      return new MaceratorMenu(id, inv, this, this.data);
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
            Direction blockFacing = (Direction)this.getBlockState().getValue(MaceratorBlock.FACING);
            if (side == Direction.DOWN) {
               return ((T) new MaceratorBlockEntity.OutputItemHandler(this.itemHandler));
            } else if (side == Direction.UP) {
               return ((T) new MaceratorBlockEntity.InputItemHandler(this.itemHandler));
            } else {
               return side == blockFacing.getOpposite()
                  ? ((T) new MaceratorBlockEntity.FuelItemHandler(this.itemHandler))
                  : ((T) new MaceratorBlockEntity.InputItemHandler(this.itemHandler));
            }
         }
      } else {
         return null;
      }
   }

   protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.saveAdditional(tag, registries);
      tag.put("inventory", this.itemHandler.serializeNBT(registries));
      tag.putInt("macerator.progress", this.progress);
      tag.putInt("macerator.burnTime", this.burnTime);
      tag.putInt("macerator.maxBurnTime", this.maxBurnTime);
   }

   public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.loadAdditional(tag, registries);
      this.itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
      this.progress = tag.getInt("macerator.progress");
      this.burnTime = tag.getInt("macerator.burnTime");
      this.maxBurnTime = tag.getInt("macerator.maxBurnTime");
   }

   @Nullable
   public Packet<ClientGamePacketListener> getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @NotNull
   public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
      return this.saveWithoutMetadata(registries);
   }

   public static void tick(Level level, BlockPos pos, BlockState state, MaceratorBlockEntity blockEntity) {
      boolean wasLit = (Boolean)state.getValue(MaceratorBlock.LIT);
      boolean isLit = false;
      if (!level.isClientSide) {
         boolean hasValidRecipe = blockEntity.hasRecipe();
         if (blockEntity.hasFuel()) {
            blockEntity.burnTime--;
            isLit = true;
         } else if (hasValidRecipe && blockEntity.canBurnFuel()) {
            blockEntity.burnFuel();
            isLit = true;
         } else if (blockEntity.progress > 0) {
            blockEntity.resetProgress();
         }

         if (hasValidRecipe && blockEntity.hasFuel()) {
            blockEntity.progress++;
            if (blockEntity.progress >= blockEntity.maxProgress) {
               blockEntity.craftItem();
               blockEntity.resetProgress();
            }
         } else if (!hasValidRecipe) {
            blockEntity.resetProgress();
         }

         if (wasLit != isLit) {
            level.setBlock(pos, (BlockState)state.setValue(MaceratorBlock.LIT, isLit), 3);
         }
      }
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
         ItemStack outputStack = this.itemHandler.getStackInSlot(5);
         if (outputStack.isEmpty()
            || outputStack.getItem() == resultItem.getItem() && outputStack.getCount() + resultItem.getCount() <= outputStack.getMaxStackSize()) {
            for (int i = 0; i <= 3; i++) {
               this.itemHandler.extractItem(i, 1, false);
            }

            if (outputStack.isEmpty()) {
               this.itemHandler.setStackInSlot(5, resultItem.copy());
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

   private boolean canBurnFuel() {
      ItemStack fuelStack = this.itemHandler.getStackInSlot(4);
      return !fuelStack.isEmpty() && ScFuels.burnTime(fuelStack) > 0;
   }

   private void burnFuel() {
      ItemStack fuelStack = this.itemHandler.getStackInSlot(4);
      this.burnTime = ScFuels.burnTime(fuelStack);
      this.maxBurnTime = this.burnTime;
      if (fuelStack.hasCraftingRemainingItem()) {
         this.itemHandler.setStackInSlot(4, fuelStack.getCraftingRemainingItem());
      } else {
         fuelStack.shrink(1);
         if (fuelStack.isEmpty()) {
            this.itemHandler.setStackInSlot(4, ItemStack.EMPTY);
         }
      }
   }

   private boolean hasFuel() {
      return this.burnTime > 0;
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

   private class FuelItemHandler implements IItemHandlerModifiable {
      private final ItemStackHandler itemHandler;

      public FuelItemHandler(ItemStackHandler itemHandler) {
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
         return slot == 4 && ScFuels.burnTime(stack) > 0 ? this.itemHandler.insertItem(slot, stack, simulate) : stack;
      }

      public ItemStack extractItem(int slot, int amount, boolean simulate) {
         return ItemStack.EMPTY;
      }

      public int getSlotLimit(int slot) {
         return this.itemHandler.getSlotLimit(slot);
      }

      public boolean isItemValid(int slot, ItemStack stack) {
         return slot == 4 && ScFuels.burnTime(stack) > 0;
      }
   }

   private class InputItemHandler implements IItemHandlerModifiable {
      private final ItemStackHandler itemHandler;

      public InputItemHandler(ItemStackHandler itemHandler) {
         super();
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

   private class OutputItemHandler implements IItemHandlerModifiable {
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

      @NotNull
      public ItemStack getStackInSlot(int i) {
         return this.itemHandler.getStackInSlot(i);
      }

      @NotNull
      public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
         return stack;
      }

      @NotNull
      public ItemStack extractItem(int slot, int amount, boolean simulate) {
         return slot == 5 ? this.itemHandler.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
      }

      public int getSlotLimit(int slot) {
         return this.itemHandler.getSlotLimit(slot);
      }

      public boolean isItemValid(int slot, @NotNull ItemStack stack) {
         return false;
      }
   }
}
