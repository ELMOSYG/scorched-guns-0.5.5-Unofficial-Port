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
import top.ribs.scguns.block.MechanicalPressBlock;
import top.ribs.scguns.client.screen.MechanicalPressMenu;
import top.ribs.scguns.client.screen.MechanicalPressRecipe;
import top.ribs.scguns.init.ModBlockEntities;
import top.ribs.scguns.item.MoldItem;

public class MechanicalPressBlockEntity extends BlockEntity implements MenuProvider {
   public final ItemStackHandler itemHandler = new ItemStackHandler(6) {
      protected void onContentsChanged(int slot) {
         MechanicalPressBlockEntity.this.setChanged();

         assert MechanicalPressBlockEntity.this.level != null;

         if (!MechanicalPressBlockEntity.this.level.isClientSide()) {
            MechanicalPressBlockEntity.this.level
               .sendBlockUpdated(MechanicalPressBlockEntity.this.getBlockPos(), MechanicalPressBlockEntity.this.getBlockState(), MechanicalPressBlockEntity.this.getBlockState(), 3);
            if ((MechanicalPressBlockEntity.this.isInputSlot(slot) || MechanicalPressBlockEntity.this.isMoldSlot(slot))
               && !MechanicalPressBlockEntity.this.isRecipeValid()) {
               MechanicalPressBlockEntity.this.resetProgress();
            }
         }
      }

      @NotNull
      public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
         if (stack.getItem() instanceof MoldItem) {
            ItemStack moldStack = MechanicalPressBlockEntity.this.itemHandler.getStackInSlot(3);
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
   private int burnTime = 0;
   private int maxBurnTime = 0;
   public static final int FIRST_INPUT_SLOT = 0;
   public static final int LAST_INPUT_SLOT = 2;
   public static final int MOLD_SLOT = 3;
   public static final int FUEL_SLOT = 4;
   public static final int OUTPUT_SLOT = 5;
   private float pressPosition = 0.0F;
   private final float pressSpeed = 0.04F;
   private boolean movingDown = true;

   private boolean isInputSlot(int slot) {
      return slot >= 0 && slot <= 2;
   }

   private boolean isMoldSlot(int slot) {
      return slot == 3;
   }

   public MechanicalPressBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)ModBlockEntities.MECHANICAL_PRESS.get(), pos, state);
      this.data = new ContainerData() {
         public int get(int index) {
            switch (index) {
               case 0:
                  return MechanicalPressBlockEntity.this.progress;
               case 1:
                  return MechanicalPressBlockEntity.this.maxProgress;
               case 2:
                  return MechanicalPressBlockEntity.this.burnTime;
               case 3:
                  return MechanicalPressBlockEntity.this.maxBurnTime;
               default:
                  return 0;
            }
         }

         public void set(int index, int value) {
            switch (index) {
               case 0:
                  MechanicalPressBlockEntity.this.progress = value;
                  break;
               case 1:
                  MechanicalPressBlockEntity.this.maxProgress = value;
                  break;
               case 2:
                  MechanicalPressBlockEntity.this.burnTime = value;
                  break;
               case 3:
                  MechanicalPressBlockEntity.this.maxBurnTime = value;
            }
         }

         public int getCount() {
            return 4;
         }
      };
   }

   public Component getDisplayName() {
      return Component.translatable("container.mechanical_press");
   }

   @Nullable
   public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
      return new MechanicalPressMenu(id, inv, this, this.data);
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
            Direction blockFacing = (Direction)this.getBlockState().getValue(MechanicalPressBlock.FACING);
            if (side == Direction.DOWN) {
               return ((T) new MechanicalPressBlockEntity.OutputItemHandler(this.itemHandler));
            } else {
               return side == blockFacing.getOpposite()
                  ? ((T) new MechanicalPressBlockEntity.FuelItemHandler(this.itemHandler))
                  : ((T) new MechanicalPressBlockEntity.TopItemHandler(this.itemHandler));
            }
         }
      } else {
         return null;
      }
   }

   protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.saveAdditional(tag, registries);
      tag.put("inventory", this.itemHandler.serializeNBT(registries));
      tag.putInt("mechanical_press.progress", this.progress);
      tag.putInt("mechanical_press.burnTime", this.burnTime);
      tag.putInt("mechanical_press.maxBurnTime", this.maxBurnTime);
   }

   public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.loadAdditional(tag, registries);
      this.itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
      this.progress = tag.getInt("mechanical_press.progress");
      this.burnTime = tag.getInt("mechanical_press.burnTime");
      this.maxBurnTime = tag.getInt("mechanical_press.maxBurnTime");
   }

   @Nullable
   public Packet<ClientGamePacketListener> getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @NotNull
   public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
      return this.saveWithoutMetadata(registries);
   }

   public static void tick(Level level, BlockPos pos, BlockState state, MechanicalPressBlockEntity blockEntity) {
      boolean wasLit = (Boolean)state.getValue(MechanicalPressBlock.LIT);
      boolean isLit = false;
      if (!level.isClientSide) {
         boolean hasValidRecipe = blockEntity.hasRecipe();
         boolean canOutput = blockEntity.canOutput();
         if (blockEntity.hasFuel()) {
            blockEntity.burnTime--;
            isLit = true;
         } else if (hasValidRecipe && blockEntity.canBurnFuel() && canOutput) {
            blockEntity.burnFuel();
            isLit = true;
         } else if (blockEntity.progress > 0) {
            blockEntity.resetProgress();
         }

         if (hasValidRecipe && blockEntity.hasFuel() && canOutput) {
            blockEntity.progress++;
            if (blockEntity.progress >= blockEntity.maxProgress) {
               blockEntity.craftItem();
               blockEntity.resetProgress();
            }
         } else if (!hasValidRecipe || !canOutput) {
            blockEntity.resetProgress();
         }

         if (wasLit != isLit) {
            level.setBlock(pos, (BlockState)state.setValue(MechanicalPressBlock.LIT, isLit), 3);
         }
      }

      if ((Boolean)state.getValue(MechanicalPressBlock.LIT)) {
         blockEntity.updatePressPosition();
      }
   }

   private boolean canOutput() {
      ItemStack outputStack = this.itemHandler.getStackInSlot(5);
      SimpleContainer inventory = new SimpleContainer(4);

      for (int i = 0; i <= 2; i++) {
         inventory.setItem(i - 0, this.itemHandler.getStackInSlot(i));
      }

      inventory.setItem(3, this.itemHandler.getStackInSlot(3));
      Optional<MechanicalPressRecipe> match = this.level.getRecipeManager().getRecipeFor(MechanicalPressRecipe.Type.INSTANCE, new ContainerRecipeInput(inventory), this.level).map(net.minecraft.world.item.crafting.RecipeHolder::value);
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

      Optional<MechanicalPressRecipe> match = this.level.getRecipeManager().getRecipeFor(MechanicalPressRecipe.Type.INSTANCE, new ContainerRecipeInput(inventory), this.level).map(net.minecraft.world.item.crafting.RecipeHolder::value);
      if (match.isPresent()) {
         MechanicalPressRecipe recipe = match.get();
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

      Optional<MechanicalPressRecipe> match = this.level.getRecipeManager().getRecipeFor(MechanicalPressRecipe.Type.INSTANCE, new ContainerRecipeInput(inventory), this.level).map(net.minecraft.world.item.crafting.RecipeHolder::value);
      if (match.isPresent()) {
         MechanicalPressRecipe recipe = match.get();
         ItemStack resultItem = recipe.getResultItem(this.level.registryAccess());
         ItemStack outputStack = this.itemHandler.getStackInSlot(5);
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

      for (int i = 0; i <= 2; i++) {
         inventory.setItem(i - 0, this.itemHandler.getStackInSlot(i));
      }

      inventory.setItem(3, this.itemHandler.getStackInSlot(3));
      Optional<MechanicalPressRecipe> currentRecipe = this.getCurrentRecipe();
      if (currentRecipe.isPresent()) {
         MechanicalPressRecipe recipe = currentRecipe.get();

         assert this.level != null;

         return recipe.matches(new ContainerRecipeInput(inventory), this.level);
      } else {
         return false;
      }
   }

   private Optional<MechanicalPressRecipe> getCurrentRecipe() {
      if (this.level == null) {
         return Optional.empty();
      } else {
         RecipeManager recipeManager = this.level.getRecipeManager();
         SimpleContainer inventory = new SimpleContainer(4);

         for (int i = 0; i <= 2; i++) {
            inventory.setItem(i - 0, this.itemHandler.getStackInSlot(i));
         }

         inventory.setItem(3, this.itemHandler.getStackInSlot(3));
         return recipeManager.getAllRecipesFor(MechanicalPressRecipe.Type.INSTANCE).stream().filter(recipe -> recipe.value().matches(new ContainerRecipeInput(inventory), this.level)).findFirst().map(net.minecraft.world.item.crafting.RecipeHolder::value);
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

   public void drops() {
      SimpleContainer inventory = new SimpleContainer(this.itemHandler.getSlots());

      for (int i = 0; i < this.itemHandler.getSlots(); i++) {
         inventory.setItem(i, this.itemHandler.getStackInSlot(i));
      }

      assert this.level != null;

      Containers.dropContents(this.level, this.worldPosition, inventory);
   }

   private static record FuelItemHandler(ItemStackHandler itemHandler) implements IItemHandlerModifiable {
      private FuelItemHandler(ItemStackHandler itemHandler) {
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

      @NotNull
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

   private class TopItemHandler implements IItemHandlerModifiable {
      private final ItemStackHandler itemHandler;

      public TopItemHandler(ItemStackHandler itemHandler) {
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
