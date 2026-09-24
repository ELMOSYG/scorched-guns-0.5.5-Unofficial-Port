package top.ribs.scguns.blockentity;


import net.minecraft.core.HolderLookup;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import top.ribs.scguns.block.ThermolithBlock;
import top.ribs.scguns.client.screen.ThermolithMenu;
import top.ribs.scguns.init.ModBlockEntities;

public class ThermolithBlockEntity extends BlockEntity implements MenuProvider {
   private static final ResourceLocation THERMOLITH_INGREDIENT_TAG = ResourceLocation.fromNamespaceAndPath("scguns", "thermolith_ingredient");
   private static final ResourceLocation MELTABLE_BLOCKS_TAG = ResourceLocation.fromNamespaceAndPath("scguns", "meltable_blocks");
   private static final int MELT_INTERVAL = 40;
   private static final double MELT_RADIUS = 6.0;
   private static final ThreadLocal<RandomSource> RANDOM = ThreadLocal.withInitial(RandomSource::create);
   private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
      protected void onContentsChanged(int slot) {
         ThermolithBlockEntity.this.setChanged();
         ThermolithBlockEntity.this.updateLitState();
      }
   };
   private final IItemHandler handler = this.itemHandler;
   private int tickCounter = 0;
   private boolean isLit = false;

   public ThermolithBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)ModBlockEntities.THERMOLITH.get(), pos, state);
   }

   public void tick() {
      if (this.level != null && !this.level.isClientSide()) {
         this.updateLitState();
         if (this.isLit) {
            this.tickCounter++;
            if (this.tickCounter >= 40) {
               this.tickCounter = 0;
               this.meltRandomBlock();
            }
         } else {
            this.tickCounter = 0;
         }
      }
   }

   private void meltRandomBlock() {
      RandomSource rand = RANDOM.get();
      BlockPos startPos = this.findNearestLava();
      if (startPos == null) {
         startPos = this.worldPosition;
      }

      for (int radius = 1; (double)radius <= 6.0; radius++) {
         List<BlockPos> candidatesAtRadius = new ArrayList<>();

         for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
               for (int dy = -1; dy <= 1; dy++) {
                  if (Math.abs(dx) == radius || Math.abs(dz) == radius) {
                     BlockPos targetPos = startPos.offset(dx, dy, dz);
                     if (this.worldPosition.distSqr(targetPos) <= 36.0 && this.level.getBlockState(targetPos).is(BlockTags.create(MELTABLE_BLOCKS_TAG))) {
                        candidatesAtRadius.add(targetPos);
                     }
                  }
               }
            }
         }

         if (!candidatesAtRadius.isEmpty()) {
            BlockPos targetPos = candidatesAtRadius.get(rand.nextInt(candidatesAtRadius.size()));
            this.level.setBlockAndUpdate(targetPos, Blocks.LAVA.defaultBlockState());
            if (rand.nextFloat() < 0.15F) {
               this.itemHandler.extractItem(0, 1, false);
            }

            if (this.level instanceof ServerLevel serverLevel) {
               serverLevel.sendParticles(
                  ParticleTypes.LAVA,
                  (double)targetPos.getX() + 0.5,
                  (double)targetPos.getY() + 0.5,
                  (double)targetPos.getZ() + 0.5,
                  10,
                  0.5,
                  0.5,
                  0.5,
                  0.1
               );
            }

            return;
         }
      }
   }

   private BlockPos findNearestLava() {
      for (int radius = 1; (double)radius <= 6.0; radius++) {
         for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
               for (int dy = -1; dy <= 1; dy++) {
                  if (Math.abs(dx) == radius || Math.abs(dz) == radius) {
                     BlockPos checkPos = this.worldPosition.offset(dx, dy, dz);
                     if (this.level.getBlockState(checkPos).getBlock() == Blocks.LAVA) {
                        return checkPos;
                     }
                  }
               }
            }
         }
      }

      return null;
   }

   private void updateLitState() {
      ItemStack stack = this.itemHandler.getStackInSlot(0);
      boolean shouldBeLit = !stack.isEmpty() && stack.is(ItemTags.create(THERMOLITH_INGREDIENT_TAG));
      if (this.isLit != shouldBeLit) {
         this.isLit = shouldBeLit;
         this.level.setBlock(this.worldPosition, (BlockState)this.getBlockState().setValue(ThermolithBlock.LIT, this.isLit), 3);
      }
   }

   @NotNull
   public Component getDisplayName() {
      return Component.translatable("container.thermolith");
   }

   @Nullable
   public AbstractContainerMenu createMenu(int id, @NotNull Inventory playerInventory, @NotNull Player player) {
      return new ThermolithMenu(id, playerInventory, this);
   }

   public ItemStackHandler getItemHandler() {
      return this.itemHandler;
   }

   @Nonnull
   public <T> T getCapability(Object cap, @Nullable Direction side) {
      return cap == Capabilities.ItemHandler.BLOCK ? ((T) this.handler) : null;
   }

   public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.loadAdditional(tag, registries);
      this.itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
      this.isLit = tag.getBoolean("isLit");
      this.tickCounter = tag.getInt("tickCounter");
   }

   protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      tag.put("inventory", this.itemHandler.serializeNBT(registries));
      tag.putBoolean("isLit", this.isLit);
      tag.putInt("tickCounter", this.tickCounter);
      super.saveAdditional(tag, registries);
   }

   public int getContainerSize() {
      return this.itemHandler.getSlots();
   }

   public void drops() {
      SimpleContainer inventory = new SimpleContainer(this.itemHandler.getSlots());

      for (int i = 0; i < this.itemHandler.getSlots(); i++) {
         inventory.setItem(i, this.itemHandler.getStackInSlot(i));
      }

      assert this.level != null;

      Containers.dropContents(this.level, this.worldPosition, inventory);
   }

   public boolean isLit() {
      return this.isLit;
   }
}
