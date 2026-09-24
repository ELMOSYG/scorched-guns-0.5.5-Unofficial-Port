package top.ribs.scguns.blockentity;



import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import top.ribs.scguns.block.VentBlock;
import top.ribs.scguns.block.VentCollectorBlock;
import top.ribs.scguns.client.screen.VentCollectorMenu;
import top.ribs.scguns.common.VentCollectorConfig;
import top.ribs.scguns.common.VentManager;
import top.ribs.scguns.init.ModBlockEntities;
import top.ribs.scguns.util.Caps;

public class VentCollectorBlockEntity extends BlockEntity implements MenuProvider {
   private static final ResourceLocation DEFAULT_CONFIG_ID = ResourceLocation.fromNamespaceAndPath("scguns", "vent_collector");
   private int pushCooldown = 0;
   private int filterProcessCooldown = 0;
   private final ItemStackHandler itemHandler = new ItemStackHandler(4) {
      protected void onContentsChanged(int slot) {
         VentCollectorBlockEntity.this.setChanged();
         if (slot == 0) {
            VentCollectorBlockEntity.this.processFilterItem();
         }
      }

      public boolean isItemValid(int slot, @NotNull ItemStack stack) {
         return slot == 0 ? VentCollectorBlockEntity.this.isValidFilterItem(stack) : slot > 0;
      }

      @NotNull
      public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
         if (slot == 0) {
            return super.insertItem(slot, stack, simulate);
         } else {
            return slot > 0 ? super.insertItem(slot, stack, simulate) : stack;
         }
      }
   };
   private final IItemHandler itemHandlerOptional = this.itemHandler;
   private int productionCounter;
   private int currentTickInterval;
   private int filterCharge;
   private final Random random = new Random();
   private VentCollectorConfig config;
   private final ContainerData data = new ContainerData() {
      public int get(int index) {
         return index == 0 ? VentCollectorBlockEntity.this.filterCharge : 0;
      }

      public void set(int index, int value) {
         if (index == 0) {
            VentCollectorBlockEntity.this.filterCharge = value;
         }
      }

      public int getCount() {
         return 1;
      }
   };

   public VentCollectorBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)ModBlockEntities.VENT_COLLECTOR.get(), pos, state);
      this.productionCounter = 0;
      this.currentTickInterval = 100;
      this.filterCharge = 0;
      this.reloadConfig();
   }

   public void reloadConfig() {
      this.config = VentManager.getVentCollectorConfig(DEFAULT_CONFIG_ID);
      if (this.config == null) {
         this.config = new VentCollectorConfig();
      }
   }

   public boolean isValidFilterItem(ItemStack stack) {
      if (this.config == null) {
         return false;
      } else {
         for (VentCollectorConfig.Filters.FilterItem filterItem : this.config.getFilters().getFilterItems()) {
            if (filterItem.isTag()) {
               ResourceLocation tagLocation = filterItem.getIdentifier();
               if (stack.is(TagKey.create(Registries.ITEM, tagLocation))) {
                  return true;
               }
            } else {
               ResourceLocation itemLocation = filterItem.getIdentifier();
               Item item = (Item)BuiltInRegistries.ITEM.get(itemLocation);
               if (item != null && stack.is(item)) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   public int getFilterCharge() {
      return this.filterCharge;
   }

   public int getMaxFilterCharge() {
      return this.config == null ? 64 : this.config.getFilters().getMaxCharge();
   }

   public static void tick(Level level, BlockPos pos, BlockState state, VentCollectorBlockEntity blockEntity) {
      if (!level.isClientSide) {
         BlockState belowState = level.getBlockState(pos.below());
         if (!(belowState.getBlock() instanceof VentBlock ventBlock)) {
            return;
         }

         boolean isActive = (Boolean)belowState.getValue(VentBlock.ACTIVE);
         if (!isActive) {
            return;
         }

         if (blockEntity.filterCharge <= 0) {
            return;
         }

         int ventPower = (Integer)belowState.getValue(VentBlock.VENT_POWER);
         float speedMultiplier = blockEntity.getSpeedMultiplier(ventPower);
         blockEntity.productionCounter += (int)speedMultiplier;
         if (blockEntity.productionCounter >= blockEntity.currentTickInterval) {
            blockEntity.productionCounter = 0;
            blockEntity.currentTickInterval = blockEntity.calculateNextTickInterval(ventBlock);
            if (!ventBlock.shouldProduce(level.random)) {
               blockEntity.setChanged();
               level.sendBlockUpdated(pos, state, state, 3);
               return;
            }

            ItemStack producedItem = ventBlock.selectRandomOutput(level.random);
            if (producedItem.isEmpty()) {
               return;
            }

            boolean produced = blockEntity.insertProducedItem(producedItem);
            if (produced && blockEntity.shouldConsumeFilter()) {
               blockEntity.filterCharge--;
            }

            blockEntity.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
         }

         if (blockEntity.filterProcessCooldown > 0) {
            blockEntity.filterProcessCooldown--;
         } else {
            blockEntity.processFilterItem();
            blockEntity.filterProcessCooldown = blockEntity.getFilterProcessCooldown();
         }

         if (blockEntity.pushCooldown > 0) {
            blockEntity.pushCooldown--;
         } else {
            blockEntity.pushItemsToAdjacentInventories(level, pos);
            blockEntity.pushCooldown = blockEntity.getPushCooldown();
         }
      }
   }

   private float getSpeedMultiplier(int ventPower) {
      if (this.config == null) {
         return 1.0F + (float)(ventPower - 1) * 0.35F;
      } else {
         float multiplier = this.config.getProcessing().getPowerSpeedMultiplier();
         return 1.0F + (float)(ventPower - 1) * multiplier;
      }
   }

   private boolean shouldConsumeFilter() {
      return this.config == null ? this.random.nextFloat() < 0.5F : this.random.nextFloat() < this.config.getFilters().getConsumptionChance();
   }

   private int getFilterProcessCooldown() {
      return this.config == null ? 2 : this.config.getFilters().getProcessCooldown();
   }

   private int getPushCooldown() {
      return this.config == null ? 5 : this.config.getProcessing().getPushCooldown();
   }

   private int calculateNextTickInterval(VentBlock ventBlock) {
      if (ventBlock.config == null) {
         ventBlock.reloadConfig();
         if (ventBlock.config == null) {
            return 100;
         }
      }

      return ventBlock.config.getPower().getBaseTickInterval() + this.random.nextInt(ventBlock.config.getPower().getTickWiggleRoom());
   }

   private boolean insertProducedItem(ItemStack producedItem) {
      for (int i = 1; i <= 3; i++) {
         ItemStack remaining = this.itemHandler.insertItem(i, producedItem, false);
         if (remaining.isEmpty()) {
            return true;
         }
      }

      return false;
   }

   private void processFilterItem() {
      ItemStack filterStack = this.itemHandler.getStackInSlot(0);
      if (!filterStack.isEmpty() && this.config != null) {
         int maxCharge = this.config.getFilters().getMaxCharge();
         if (this.filterCharge < maxCharge) {
            for (VentCollectorConfig.Filters.FilterItem filterItem : this.config.getFilters().getFilterItems()) {
               boolean matches = false;
               if (filterItem.isTag()) {
                  ResourceLocation tagLocation = filterItem.getIdentifier();
                  matches = filterStack.is(TagKey.create(Registries.ITEM, tagLocation));
               } else {
                  ResourceLocation itemLocation = filterItem.getIdentifier();
                  Item item = (Item)BuiltInRegistries.ITEM.get(itemLocation);
                  matches = item != null && filterStack.is(item);
               }

               if (matches) {
                  int chargeToAdd = filterItem.getChargeAmount();
                  int chargeNeeded = maxCharge - this.filterCharge;
                  if (chargeNeeded >= chargeToAdd) {
                     this.filterCharge += chargeToAdd;
                     filterStack.shrink(1);
                     this.setChanged();
                     return;
                  }
               }
            }
         }
      }
   }

   private void pushItemsToAdjacentInventories(Level level, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      if (state.getBlock() instanceof VentCollectorBlock) {
         Direction facing = (Direction)state.getValue(VentCollectorBlock.FACING);
         boolean isConnected = (Boolean)state.getValue(VentCollectorBlock.ATTACHED);
         if (isConnected) {
            BlockPos adjacentPos = pos.relative(facing);
            BlockEntity adjacentEntity = level.getBlockEntity(adjacentPos);
            if (adjacentEntity != null) {
               IItemHandler adjacentHandler = Caps.itemHandler(adjacentEntity, facing.getOpposite());

               if (adjacentHandler != null) {
                  for (int i = 1; i <= 3; i++) {
                     ItemStack stack = this.itemHandler.getStackInSlot(i);
                     if (!stack.isEmpty()) {
                        ItemStack singleItem = stack.copy();
                        singleItem.setCount(1);
                        ItemStack remaining = ItemHandlerHelper.insertItemStacked(adjacentHandler, singleItem, false);
                        if (remaining.isEmpty()) {
                           this.itemHandler.extractItem(i, 1, false);
                           this.setChanged();
                           return;
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @NotNull
   public <T> T getCapability(Object cap, @Nullable Direction side) {
      return cap == Capabilities.ItemHandler.BLOCK ? ((T) this.itemHandlerOptional) : null;
   }

   public Component getDisplayName() {
      return Component.translatable("block.scguns.vent_collector");
   }

   @Nullable
   public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
      return new VentCollectorMenu(windowId, playerInventory, this, this.data);
   }

   public ContainerData getData() {
      return this.data;
   }

   public ItemStackHandler getItemHandler() {
      return this.itemHandler;
   }

   protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.saveAdditional(tag, registries);
      tag.putInt("FilterCharge", this.filterCharge);
      tag.putInt("PushCooldown", this.pushCooldown);
      tag.put("Inventory", this.itemHandler.serializeNBT(registries));
   }

   public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.loadAdditional(tag, registries);
      this.filterCharge = tag.getInt("FilterCharge");
      this.pushCooldown = tag.getInt("PushCooldown");
      this.itemHandler.deserializeNBT(registries, tag.getCompound("Inventory"));
   }

   public void drops() {
      SimpleContainer inventory = new SimpleContainer(this.itemHandler.getSlots());

      for (int i = 0; i < this.itemHandler.getSlots(); i++) {
         inventory.setItem(i, this.itemHandler.getStackInSlot(i));
      }

      assert this.level != null;

      Containers.dropContents(this.level, this.worldPosition, inventory);
   }
}
