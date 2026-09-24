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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
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
import top.ribs.scguns.block.PoweredMechanicalPressBlock;
import top.ribs.scguns.client.screen.PoweredMechanicalPressMenu;
import top.ribs.scguns.client.screen.PoweredMechanicalPressRecipe;
import top.ribs.scguns.init.ModBlockEntities;
import top.ribs.scguns.item.MoldItem;

public class PoweredMechanicalPressBlockEntity extends BlockEntity implements MenuProvider {
   public final ItemStackHandler itemHandler = new ItemStackHandler(5) {
      protected void onContentsChanged(int slot) {
         PoweredMechanicalPressBlockEntity.this.setChanged();

         assert PoweredMechanicalPressBlockEntity.this.level != null;

         if (!PoweredMechanicalPressBlockEntity.this.level.isClientSide()) {
            PoweredMechanicalPressBlockEntity.this.level
               .sendBlockUpdated(
                  PoweredMechanicalPressBlockEntity.this.getBlockPos(),
                  PoweredMechanicalPressBlockEntity.this.getBlockState(),
                  PoweredMechanicalPressBlockEntity.this.getBlockState(),
                  3
               );
            if ((PoweredMechanicalPressBlockEntity.this.isInputSlot(slot) || PoweredMechanicalPressBlockEntity.this.isMoldSlot(slot))
               && !PoweredMechanicalPressBlockEntity.this.isRecipeValid()) {
               PoweredMechanicalPressBlockEntity.this.resetProgress();
            }
         }
      }

      @NotNull
      public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
         if (stack.getItem() instanceof MoldItem) {
            ItemStack moldStack = PoweredMechanicalPressBlockEntity.this.itemHandler.getStackInSlot(3);
            if (moldStack.isEmpty() || moldStack.isDamageableItem() && moldStack.getDamageValue() < moldStack.getMaxDamage()) {
               return super.insertItem(3, stack, simulate);
            }
         }

         return super.insertItem(slot, stack, simulate);
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
            PoweredMechanicalPressBlockEntity.this.setChanged();
            PoweredMechanicalPressBlockEntity.this.sync();
         }

         return received;
      }

      public int extractEnergy(int maxExtract, boolean simulate) {
         int extracted = super.extractEnergy(maxExtract, simulate);
         if (!simulate && extracted > 0) {
            PoweredMechanicalPressBlockEntity.this.setChanged();
            PoweredMechanicalPressBlockEntity.this.sync();
         }

         return extracted;
      }
   };
   private final IEnergyStorage energy = this.energyStorage;
   public static final int FIRST_INPUT_SLOT = 0;
   public static final int LAST_INPUT_SLOT = 2;
   public static final int MOLD_SLOT = 3;
   public static final int OUTPUT_SLOT = 4;
   private float pressPosition = 0.0F;
   private final float pressSpeed = 0.04F;
   private boolean movingDown = true;

   private boolean isInputSlot(int slot) {
      return slot >= 0 && slot <= 2;
   }

   private boolean isMoldSlot(int slot) {
      return slot == 3;
   }

   public PoweredMechanicalPressBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)ModBlockEntities.POWERED_MECHANICAL_PRESS.get(), pos, state);
      this.data = new ContainerData() {
         public int get(int index) {
            return switch (index) {
               case 0 -> PoweredMechanicalPressBlockEntity.this.progress;
               case 1 -> PoweredMechanicalPressBlockEntity.this.maxProgress;
               case 2 -> PoweredMechanicalPressBlockEntity.this.energyStorage.getEnergyStored();
               case 3 -> PoweredMechanicalPressBlockEntity.this.energyStorage.getMaxEnergyStored();
               default -> 0;
            };
         }

         public void set(int index, int value) {
            switch (index) {
               case 0:
                  PoweredMechanicalPressBlockEntity.this.progress = value;
                  break;
               case 1:
                  PoweredMechanicalPressBlockEntity.this.maxProgress = value;
            }
         }

         public int getCount() {
            return 4;
         }
      };
   }

   public Component getDisplayName() {
      return Component.translatable("container.powered_mechanical_press");
   }

   @Nullable
   public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
      return new PoweredMechanicalPressMenu(id, inv, this, this.data);
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
               ? ((T) new PoweredMechanicalPressBlockEntity.OutputItemHandler(this.itemHandler))
               : ((T) new PoweredMechanicalPressBlockEntity.TopItemHandler(this.itemHandler));
         }
      } else {
         return cap == Capabilities.EnergyStorage.BLOCK ? ((T) this.energy) : null;
      }
   }

   protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.saveAdditional(tag, registries);
      tag.put("inventory", this.itemHandler.serializeNBT(registries));
      tag.putInt("powered_mechanical_press.progress", this.progress);
      tag.put("Energy", this.energyStorage.serializeNBT(registries));
   }

   public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.loadAdditional(tag, registries);
      this.itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
      this.progress = tag.getInt("powered_mechanical_press.progress");
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

   public static void tick(Level level, BlockPos pos, BlockState state, PoweredMechanicalPressBlockEntity blockEntity) {
      boolean wasLit = (Boolean)state.getValue(PoweredMechanicalPressBlock.LIT);
      boolean isLit = false;
      if (!level.isClientSide) {
         boolean hasValidRecipe = blockEntity.hasRecipe();
         boolean canOutput = blockEntity.canOutput();
         if (hasValidRecipe && blockEntity.hasEnoughEnergy(50) && canOutput) {
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
            level.setBlock(pos, (BlockState)state.setValue(PoweredMechanicalPressBlock.LIT, isLit), 3);
         }
      }

      if ((Boolean)state.getValue(PoweredMechanicalPressBlock.LIT)) {
         blockEntity.updatePressPosition();
      }
   }

   private boolean canOutput() {
      ItemStack outputStack = this.itemHandler.getStackInSlot(4);
      SimpleContainer inventory = new SimpleContainer(4);

      for (int i = 0; i <= 2; i++) {
         inventory.setItem(i - 0, this.itemHandler.getStackInSlot(i));
      }

      inventory.setItem(3, this.itemHandler.getStackInSlot(3));
      Optional<PoweredMechanicalPressRecipe> match = this.level.getRecipeManager().getRecipeFor(PoweredMechanicalPressRecipe.Type.INSTANCE, new ContainerRecipeInput(inventory), this.level).map(net.minecraft.world.item.crafting.RecipeHolder::value);
      if (match.isPresent()) {
         ItemStack resultItem = match.get().getResultItem(this.level.registryAccess());
         if (outputStack.isEmpty()
            || outputStack.getItem() == resultItem.getItem() && outputStack.getCount() + resultItem.getCount() <= outputStack.getMaxStackSize()) {
            return true;
         }
      }

      return false;
   }

   public float getPressPosition(float partialTicks, boolean isLit) {
      return isLit ? this.pressPosition + (this.movingDown ? -0.04F : 0.04F) * partialTicks : this.pressPosition;
   }

   public void updatePressPosition() {
      if (this.movingDown) {
         this.pressPosition -= 0.04F;
         float endPosition = -0.25F;
         if (this.pressPosition <= endPosition) {
            this.movingDown = false;
            if (this.level != null) {
               this.level.playSound(null, this.worldPosition, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.05F, 0.6F);
            }
         }
      } else {
         this.pressPosition += 0.04F;
         float startPosition = 0.0F;
         if (this.pressPosition >= startPosition) {
            this.movingDown = true;
         }
      }
   }

   private boolean hasRecipe() {
      SimpleContainer inventory = new SimpleContainer(4);

      for (int i = 0; i <= 2; i++) {
         inventory.setItem(i - 0, this.itemHandler.getStackInSlot(i));
      }

      inventory.setItem(3, this.itemHandler.getStackInSlot(3));

      assert this.level != null;

      Optional<PoweredMechanicalPressRecipe> match = this.level.getRecipeManager().getRecipeFor(PoweredMechanicalPressRecipe.Type.INSTANCE, new ContainerRecipeInput(inventory), this.level).map(net.minecraft.world.item.crafting.RecipeHolder::value);
      if (match.isPresent()) {
         PoweredMechanicalPressRecipe recipe = match.get();
         this.maxProgress = recipe.getProcessingTime();
         return true;
      } else {
         return false;
      }
   }

   private void craftItem() {
      SimpleContainer inventory = new SimpleContainer(4);

      for (int i = 0; i <= 2; i++) {
         inventory.setItem(i - 0, this.itemHandler.getStackInSlot(i));
      }

      inventory.setItem(3, this.itemHandler.getStackInSlot(3));

      assert this.level != null;

      Optional<PoweredMechanicalPressRecipe> match = this.level.getRecipeManager().getRecipeFor(PoweredMechanicalPressRecipe.Type.INSTANCE, new ContainerRecipeInput(inventory), this.level).map(net.minecraft.world.item.crafting.RecipeHolder::value);
      if (match.isPresent()) {
         PoweredMechanicalPressRecipe recipe = match.get();
         ItemStack resultItem = recipe.getResultItem(this.level.registryAccess());
         ItemStack outputStack = this.itemHandler.getStackInSlot(4);
         if (outputStack.isEmpty()
            || outputStack.getItem() == resultItem.getItem() && outputStack.getCount() + resultItem.getCount() <= outputStack.getMaxStackSize()) {
            for (Ingredient ingredient : recipe.getIngredients()) {
               for (int i = 0; i <= 2; i++) {
                  if (ingredient.test(this.itemHandler.getStackInSlot(i))) {
                     this.itemHandler.extractItem(i, 1, false);
                     break;
                  }
               }
            }

            if (recipe.requiresMold()) {
               ItemStack moldStack = this.itemHandler.getStackInSlot(3);
               if (!moldStack.isEmpty() && moldStack.isDamageableItem()) {
                  if (this.level instanceof ServerLevel serverLevel) {
                     moldStack.hurtAndBreak(1, serverLevel, (LivingEntity)null, item -> { });
                  }

                  this.itemHandler.setStackInSlot(3, moldStack);
               }
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

      for (int i = 0; i <= 2; i++) {
         inventory.setItem(i - 0, this.itemHandler.getStackInSlot(i));
      }

      inventory.setItem(3, this.itemHandler.getStackInSlot(3));
      Optional<PoweredMechanicalPressRecipe> currentRecipe = this.getCurrentRecipe();
      if (currentRecipe.isPresent()) {
         PoweredMechanicalPressRecipe recipe = currentRecipe.get();

         assert this.level != null;

         return recipe.matches(new ContainerRecipeInput(inventory), this.level);
      } else {
         return false;
      }
   }

   private Optional<PoweredMechanicalPressRecipe> getCurrentRecipe() {
      if (this.level == null) {
         return Optional.empty();
      } else {
         RecipeManager recipeManager = this.level.getRecipeManager();
         SimpleContainer inventory = new SimpleContainer(4);

         for (int i = 0; i <= 2; i++) {
            inventory.setItem(i - 0, this.itemHandler.getStackInSlot(i));
         }

         inventory.setItem(3, this.itemHandler.getStackInSlot(3));
         return recipeManager.getAllRecipesFor(PoweredMechanicalPressRecipe.Type.INSTANCE)
            .stream()
            .filter(recipe -> recipe.value().matches(new ContainerRecipeInput(inventory), this.level))
            .findFirst().map(net.minecraft.world.item.crafting.RecipeHolder::value);
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
         return slot != 0 && slot != 2 && !(stack.getItem() instanceof MoldItem) ? stack : this.itemHandler.insertItem(slot, stack, simulate);
      }

      public ItemStack extractItem(int slot, int amount, boolean simulate) {
         return slot >= 0 && slot <= 2 ? this.itemHandler.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
      }

      public int getSlotLimit(int slot) {
         return this.itemHandler.getSlotLimit(slot);
      }

      public boolean isItemValid(int slot, ItemStack stack) {
         return slot == 0 || slot == 2 || stack.getItem() instanceof MoldItem;
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
         return slot == 4 ? this.itemHandler.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
      }

      public int getSlotLimit(int slot) {
         return this.itemHandler.getSlotLimit(slot);
      }

      public boolean isItemValid(int slot, @NotNull ItemStack stack) {
         return false;
      }
   }

   private class TopItemHandler implements IItemHandlerModifiable {
      private final ItemStackHandler itemHandler;

      public TopItemHandler(ItemStackHandler itemHandler) {
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
         return this.isItemValid(slot, stack) ? this.itemHandler.insertItem(slot, stack, simulate) : stack;
      }

      public ItemStack extractItem(int slot, int amount, boolean simulate) {
         return ItemStack.EMPTY;
      }

      public int getSlotLimit(int slot) {
         return this.itemHandler.getSlotLimit(slot);
      }

      public boolean isItemValid(int slot, ItemStack stack) {
         return stack.getItem() instanceof MoldItem
            ? slot == 3
               && (
                  this.itemHandler.getStackInSlot(3).isEmpty()
                     || this.itemHandler.getStackInSlot(3).isDamageableItem()
                        && this.itemHandler.getStackInSlot(3).getDamageValue() < this.itemHandler.getStackInSlot(3).getMaxDamage()
               )
            : slot >= 0 && slot <= 2;
      }
   }
}
