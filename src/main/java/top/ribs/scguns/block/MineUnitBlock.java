package top.ribs.scguns.block;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import top.ribs.scguns.blockentity.MineUnitBlockEntity;
import top.ribs.scguns.init.ModBlockEntities;
import top.ribs.scguns.init.ModTags;

public class MineUnitBlock extends Block implements EntityBlock {
   public static final BooleanProperty PRIMED = BooleanProperty.create("primed");
   private static final VoxelShape UNPRIMED_SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 4.0, 14.0);
   private static final VoxelShape PRIMED_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 0.1, 16.0);

   public MineUnitBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(PRIMED, false));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{PRIMED});
   }

   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return state.getValue(PRIMED) ? PRIMED_SHAPE : UNPRIMED_SHAPE;
   }

   @Nullable
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new MineUnitBlockEntity(pos, state);
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return type == ModBlockEntities.MINE_UNIT.get() ? MineUnitBlockEntity::tick : null;
   }

   @Override
   public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {

      ItemStack heldItem = stack;
      if (level.getBlockEntity(pos) instanceof MineUnitBlockEntity mineUnit) {
         if (mineUnit.isPrimed()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
         } else if (heldItem.is(ModTags.Items.GRENADES) && !mineUnit.hasGrenade()) {
            if (!level.isClientSide()) {
               ItemStack grenadeStack = heldItem.copy();
               grenadeStack.setCount(1);
               mineUnit.setGrenade(grenadeStack, player);
               if (!player.isCreative()) {
                  heldItem.shrink(1);
               }

               level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
               this.spawnLoadingParticles(level, pos);
            }

            return ItemInteractionResult.sidedSuccess(level.isClientSide());
         } else if (mineUnit.hasGrenade()) {
            if (!level.isClientSide()) {
               mineUnit.setPrimed(true);
               level.setBlock(pos, (BlockState)state.setValue(PRIMED, true), 3);
               level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.8F, 1.2F);
               level.playSound(null, pos, SoundEvents.TRIPWIRE_CLICK_ON, SoundSource.BLOCKS, 0.6F, 0.9F);
               this.spawnPrimingParticles(level, pos);
            } else {
               player.swing(hand);
            }

            return ItemInteractionResult.sidedSuccess(level.isClientSide());
         } else {
            if (level.isClientSide()) {
               player.displayClientMessage(Component.translatable("message.scguns.mine_unit.needs_grenade").withStyle(ChatFormatting.RED), true);
            }

            return ItemInteractionResult.sidedSuccess(level.isClientSide());
         }
      } else {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      }
   }

   private void spawnLoadingParticles(Level level, BlockPos pos) {
      if (level instanceof ServerLevel serverLevel) {
         for (int i = 0; i < 8; i++) {
            double offsetX = (double)pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 0.4;
            double offsetY = (double)pos.getY() + 0.2;
            double offsetZ = (double)pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 0.4;
            double velocityX = (level.random.nextDouble() - 0.5) * 0.1;
            double velocityY = level.random.nextDouble() * 0.05;
            double velocityZ = (level.random.nextDouble() - 0.5) * 0.1;
            serverLevel.sendParticles(ParticleTypes.CRIT, offsetX, offsetY, offsetZ, 1, velocityX, velocityY, velocityZ, 0.05);
         }
      }
   }

   private void spawnPrimingParticles(Level level, BlockPos pos) {
      if (level instanceof ServerLevel serverLevel) {
         BlockPos belowPos = pos.below();
         BlockState belowState = level.getBlockState(belowPos);
         if (!belowState.isAir()) {
            for (int i = 0; i < 15; i++) {
               double offsetX = (double)pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 0.6;
               double offsetY = (double)pos.getY() + 0.1;
               double offsetZ = (double)pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 0.6;
               double velocityX = (level.random.nextDouble() - 0.5) * 0.15;
               double velocityY = level.random.nextDouble() * 0.1;
               double velocityZ = (level.random.nextDouble() - 0.5) * 0.15;
               serverLevel.sendParticles(
                  new BlockParticleOption(ParticleTypes.BLOCK, belowState), offsetX, offsetY, offsetZ, 1, velocityX, velocityY, velocityZ, 0.1
               );
            }
         }
      }
   }

   public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
      if (!state.is(newState.getBlock())) {
         if (level.getBlockEntity(pos) instanceof MineUnitBlockEntity mineUnit) {
            mineUnit.dropGrenade();
         }

         super.onRemove(state, level, pos, newState, isMoving);
      }
   }
}
