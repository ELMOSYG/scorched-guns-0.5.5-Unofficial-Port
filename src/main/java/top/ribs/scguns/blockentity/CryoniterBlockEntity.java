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
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import top.ribs.scguns.block.CryoniterBlock;
import top.ribs.scguns.client.screen.CryoniterMenu;
import top.ribs.scguns.init.ModBlockEntities;

public class CryoniterBlockEntity extends BlockEntity implements MenuProvider {
   private static final ResourceLocation CRYONITER_INGREDIENT_TAG = ResourceLocation.fromNamespaceAndPath("scguns", "cryoniter_ingredient");
   private static final int FREEZE_INTERVAL = 40;
   private static final double FREEZE_RADIUS = 4.0;
   private static final ThreadLocal<RandomSource> RANDOM = ThreadLocal.withInitial(RandomSource::create);
   private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
      protected void onContentsChanged(int slot) {
         CryoniterBlockEntity.this.setChanged();
         CryoniterBlockEntity.this.updateLitState();
      }
   };
   private final IItemHandler handler = this.itemHandler;
   private int tickCounter = 0;
   private boolean isLit = false;

   public CryoniterBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)ModBlockEntities.CRYONITER.get(), pos, state);
   }

   public void tick() {
      if (this.level != null && !this.level.isClientSide()) {
         this.updateLitState();
         if (this.isLit) {
            this.tickCounter++;
            if (this.tickCounter >= 40) {
               this.tickCounter = 0;
               this.freezeRandomWaterBlock();
            }
         } else {
            this.tickCounter = 0;
         }
      }
   }

   private void freezeRandomWaterBlock() {
      RandomSource rand = RANDOM.get();
      BlockPos startPos = this.findNearestIce();
      if (startPos == null) {
         startPos = this.worldPosition;
      }

      for (int radius = 1; (double)radius <= 4.0; radius++) {
         List<BlockPos> candidatesAtRadius = new ArrayList<>();

         for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
               for (int dy = -1; dy <= 1; dy++) {
                  if (Math.abs(dx) == radius || Math.abs(dz) == radius) {
                     BlockPos targetPos = startPos.offset(dx, dy, dz);
                     if (this.worldPosition.distSqr(targetPos) <= 16.0 && this.level.getBlockState(targetPos).is(Blocks.WATER)) {
                        candidatesAtRadius.add(targetPos);
                     }
                  }
               }
            }
         }

         if (!candidatesAtRadius.isEmpty()) {
            BlockPos targetPos = candidatesAtRadius.get(rand.nextInt(candidatesAtRadius.size()));
            Block iceType = this.determineIceType(rand);
            this.level.setBlockAndUpdate(targetPos, iceType.defaultBlockState());
            if (rand.nextFloat() < 0.5F) {
               this.itemHandler.extractItem(0, 1, false);
            }

            if (this.level instanceof ServerLevel serverLevel) {
               serverLevel.sendParticles(
                  ParticleTypes.SNOWFLAKE,
                  (double)targetPos.getX() + 0.5,
                  (double)targetPos.getY() + 1.0,
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

   private BlockPos findNearestIce() {
      for (int radius = 1; (double)radius <= 4.0; radius++) {
         for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
               for (int dy = -1; dy <= 1; dy++) {
                  if (Math.abs(dx) == radius || Math.abs(dz) == radius) {
                     BlockPos checkPos = this.worldPosition.offset(dx, dy, dz);
                     Block block = this.level.getBlockState(checkPos).getBlock();
                     if (block == Blocks.ICE || block == Blocks.PACKED_ICE || block == Blocks.BLUE_ICE) {
                        return checkPos;
                     }
                  }
               }
            }
         }
      }

      return null;
   }

   private Block determineIceType(RandomSource rand) {
      float roll = rand.nextFloat();
      if (roll < 0.02F) {
         return Blocks.BLUE_ICE;
      } else {
         return roll < 0.15F ? Blocks.PACKED_ICE : Blocks.ICE;
      }
   }

   private void updateLitState() {
      ItemStack stack = this.itemHandler.getStackInSlot(0);
      boolean shouldBeLit = !stack.isEmpty() && stack.is(ItemTags.create(CRYONITER_INGREDIENT_TAG));
      if (this.isLit != shouldBeLit) {
         this.isLit = shouldBeLit;
         this.level.setBlock(this.worldPosition, (BlockState)this.getBlockState().setValue(CryoniterBlock.LIT, this.isLit), 3);
      }
   }

   public Component getDisplayName() {
      return Component.translatable("container.cryoniter");
   }

   @Nullable
   public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
      return new CryoniterMenu(id, playerInventory, this);
   }

   public ItemStackHandler getItemHandler() {
      return this.itemHandler;
   }

   @Nonnull
   public <T> T getCapability(Object cap, @Nullable Direction side) {
      return cap == Capabilities.ItemHandler.BLOCK ? ((T) this.handler) : null;
   }

   protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      tag.put("inventory", this.itemHandler.serializeNBT(registries));
      tag.putBoolean("isLit", this.isLit);
      tag.putInt("tickCounter", this.tickCounter);
      super.saveAdditional(tag, registries);
   }

   public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.loadAdditional(tag, registries);
      this.itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
      this.isLit = tag.getBoolean("isLit");
      this.tickCounter = tag.getInt("tickCounter");
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

   public int getContainerSize() {
      return this.itemHandler.getSlots();
   }
}
