package top.ribs.scguns.blockentity;


import net.minecraft.core.HolderLookup;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import top.ribs.scguns.block.AdvancedComposterBlock;
import top.ribs.scguns.config.AdvancedComposterDropsConfig;
import top.ribs.scguns.init.ModBlockEntities;

public class AdvancedComposterBlockEntity extends BlockEntity implements WorldlyContainer {
   private static final int INPUT_SLOT = 0;
   private static final int OUTPUT_SLOT_START = 1;
   private static final int OUTPUT_SLOT_COUNT = 3;
   private static final int TOTAL_SLOTS = 4;
   private final NonNullList<ItemStack> items = NonNullList.withSize(4, ItemStack.EMPTY);
   private static final int[] SLOTS_FOR_UP = new int[]{0};
   private static final int[] SLOTS_FOR_DOWN = new int[]{1, 2, 3};
   private static final int[] SLOTS_FOR_SIDES = new int[]{0};
   private static final int MAX_COMPOST_TIME = 300;
   private int compostTime = 0;
   private boolean isComposting = false;

   public AdvancedComposterBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)ModBlockEntities.ADVANCED_COMPOSTER.get(), pos, state);
   }

   public void tick(Level level, BlockPos pos, BlockState state) {
      if (!level.isClientSide && (Integer)state.getValue(AdvancedComposterBlock.LEVEL) == 7 && this.isComposting) {
         this.compostTime++;
         if (this.compostTime >= 300) {
            this.completeComposting(level, pos, state);
         }

         this.setChanged();
      }
   }

   public void startComposting() {
      if (!this.isComposting) {
         this.compostTime = 0;
         this.isComposting = true;
         this.setChanged();
      }
   }

   private void completeComposting(Level level, BlockPos pos, BlockState state) {
      level.setBlock(pos, (BlockState)state.setValue(AdvancedComposterBlock.LEVEL, 8), 3);
      level.playSound(null, pos, SoundEvents.COMPOSTER_READY, SoundSource.BLOCKS, 1.0F, 1.0F);
      this.isComposting = false;
      this.createOutputItems();
      this.setChanged();
   }

   private void createOutputItems() {
      Random random = new Random();
      List<ItemStack> output = AdvancedComposterDropsConfig.generateDrops(random);
      if (!output.isEmpty()) {
         for (int slot = 1; slot < 4 && !output.isEmpty(); slot++) {
            if (((ItemStack)this.items.get(slot)).isEmpty()) {
               this.items.set(slot, output.remove(0));
            }
         }

         this.setChanged();
      }
   }

   public boolean extractOneItem(Player player) {
      for (int slot = 1; slot < 4; slot++) {
         ItemStack stack = this.removeItem(slot, 1);
         if (!stack.isEmpty()) {
            if (player != null) {
               player.getInventory().add(stack);
            } else {
               assert this.level != null;

               Block.popResource(this.level, this.worldPosition.above(), stack);
            }

            this.setChanged();
            return true;
         }
      }

      return false;
   }

   public int getVisualLevel() {
      if (this.isOutputEmpty()) {
         return 0;
      } else {
         int filledSlots = (int)IntStream.range(1, 4).filter(slot -> !((ItemStack)this.items.get(slot)).isEmpty()).count();
         return Math.max(8, 11 - filledSlots);
      }
   }

   @NotNull
   public ItemStack removeItem(int index, int count) {
      ItemStack stack = ContainerHelper.removeItem(this.items, index, count);
      if (!stack.isEmpty() && index >= 1) {
         this.setChanged();
         if (this.level != null) {
            this.level.setBlock(this.worldPosition, (BlockState)this.getBlockState().setValue(AdvancedComposterBlock.LEVEL, this.getVisualLevel()), 3);
         }
      }

      return stack;
   }

   public boolean isOutputEmpty() {
      for (int slot = 1; slot < 4; slot++) {
         if (!((ItemStack)this.items.get(slot)).isEmpty()) {
            return false;
         }
      }

      return true;
   }

   public int getContainerSize() {
      return 4;
   }

   public boolean isEmpty() {
      for (ItemStack item : this.items) {
         if (!item.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   public ItemStack getItem(int index) {
      return (ItemStack)this.items.get(index);
   }

   public ItemStack removeItemNoUpdate(int index) {
      return ContainerHelper.takeItem(this.items, index);
   }

   public void setItem(int index, ItemStack stack) {
      this.items.set(index, stack);
      if (index == 0 && !stack.isEmpty()) {
         this.tryCompostItem(stack);
      }

      this.setChanged();
   }

   private void tryCompostItem(ItemStack stack) {
      BlockState state = this.getBlockState();
      int currentLevel = (Integer)state.getValue(AdvancedComposterBlock.LEVEL);
      if (currentLevel < 7 && state.getBlock() instanceof AdvancedComposterBlock composterBlock && composterBlock.isCompostable(stack)) {
         BlockState newState = composterBlock.addItem(null, state, this.level, this.worldPosition, stack);
         this.level.setBlock(this.worldPosition, newState, 3);
         stack.shrink(1);
         composterBlock.playComposterEffects(this.level, this.worldPosition, newState);
      }
   }

   public boolean stillValid(Player player) {
      return Container.stillValidBlockEntity(this, player);
   }

   public void clearContent() {
      this.items.clear();
   }

   public int[] getSlotsForFace(Direction side) {
      if (side == Direction.DOWN) {
         return SLOTS_FOR_DOWN;
      } else {
         return side == Direction.UP ? SLOTS_FOR_UP : SLOTS_FOR_SIDES;
      }
   }

   public boolean canPlaceItemThroughFace(int index, ItemStack itemStack, @Nullable Direction direction) {
      return index == 0 && this.canPlaceItem(index, itemStack) && (Integer)this.getBlockState().getValue(AdvancedComposterBlock.LEVEL) < 7;
   }

   public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
      return index >= 1 && index < 4 && direction == Direction.DOWN && !stack.isEmpty();
   }

   public boolean canPlaceItem(int index, ItemStack stack) {
      return index == 0 && this.getBlockState().getBlock() instanceof AdvancedComposterBlock composterBlock && composterBlock.isCompostable(stack);
   }

   public void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
      super.loadAdditional(nbt, registries);
      this.compostTime = nbt.getInt("CompostTime");
      this.isComposting = nbt.getBoolean("IsComposting");
      ContainerHelper.loadAllItems(nbt, this.items, registries);
   }

   protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
      super.saveAdditional(nbt, registries);
      nbt.putInt("CompostTime", this.compostTime);
      nbt.putBoolean("IsComposting", this.isComposting);
      ContainerHelper.saveAllItems(nbt, this.items, registries);
   }

   public void drops() {
      for (ItemStack item : this.items) {
         assert this.level != null;

         Block.popResource(this.level, this.worldPosition, item);
      }
   }
}
