package top.ribs.scguns.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;

import top.ribs.scguns.blockentity.VentCollectorBlockEntity;
import top.ribs.scguns.util.Caps;

public class VentCollectorBlock extends Block implements EntityBlock {
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
   public static final BooleanProperty ATTACHED = BooleanProperty.create("attached");
   private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 14.0, 15.0);

   public VentCollectorBlock(Properties properties) {
      super(properties.strength(0.5F).sound(SoundType.METAL));
      this.registerDefaultState(
         (BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(WATERLOGGED, false)).setValue(FACING, Direction.NORTH))
            .setValue(ATTACHED, false)
      );
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{WATERLOGGED, FACING, ATTACHED});
   }

   @Nullable
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
      BlockPos pos = context.getClickedPos().relative(context.getHorizontalDirection().getOpposite());
      boolean attached = context.getLevel().getBlockEntity(pos) != null;
      return (BlockState)((BlockState)((BlockState)this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()))
            .setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER))
         .setValue(ATTACHED, attached);
   }

   public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
      if ((Boolean)state.getValue(WATERLOGGED)) {
         level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
      }

      if (direction == state.getValue(FACING)) {
         boolean attached = this.hasInventory(level, neighborPos, direction.getOpposite());
         return (BlockState)state.setValue(ATTACHED, attached);
      } else {
         return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
      }
   }

   private boolean hasInventory(LevelAccessor level, BlockPos pos, Direction direction) {
      BlockEntity be = level.getBlockEntity(pos);
      return Caps.of(be, Capabilities.ItemHandler.BLOCK, direction) != null;
   }

   public FluidState getFluidState(BlockState state) {
      return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
   }

   @Nullable
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new VentCollectorBlockEntity(pos, state);
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return level.isClientSide ? null : (lvl, pos, st, t) -> {
         if (t instanceof VentCollectorBlockEntity) {
            VentCollectorBlockEntity.tick(lvl, pos, st, (VentCollectorBlockEntity)t);
         }
      };
   }

   public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (!level.isClientSide) {
         BlockEntity blockEntity = level.getBlockEntity(pos);
         if (blockEntity instanceof VentCollectorBlockEntity) {
            ((ServerPlayer)player).openMenu((VentCollectorBlockEntity)blockEntity, buf -> buf.writeBlockPos(pos));
         }
      }

      return InteractionResult.SUCCESS;
   }

   public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
      if (!state.is(newState.getBlock())) {
         BlockEntity blockEntity = level.getBlockEntity(pos);
         if (blockEntity instanceof VentCollectorBlockEntity) {
            ((VentCollectorBlockEntity)blockEntity).drops();
         }
      }

      super.onRemove(state, level, pos, newState, isMoving);
   }

   public boolean hasAnalogOutputSignal(BlockState state) {
      return true;
   }

   public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
      BlockEntity blockEntity = level.getBlockEntity(pos);
      return blockEntity instanceof VentCollectorBlockEntity ? AbstractContainerMenu.getRedstoneSignalFromContainer((Container)blockEntity) : 0;
   }

   public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
      BlockState belowState = level.getBlockState(pos.below());
      boolean isGeothermalVentBelow = belowState.getBlock() instanceof GeothermalVentBlock;
      boolean isSulfurVentBelow = belowState.getBlock() instanceof SulfurVentBlock;
      boolean isActive = isGeothermalVentBelow && (Boolean)belowState.getValue(GeothermalVentBlock.ACTIVE)
         || isSulfurVentBelow && (Boolean)belowState.getValue(SulfurVentBlock.ACTIVE);
      if (isActive) {
         if (random.nextInt(20) == 0) {
            level.playLocalSound(
               (double)pos.getX() + 0.5,
               (double)pos.getY() + 0.5,
               (double)pos.getZ() + 0.5,
               SoundEvents.CAMPFIRE_CRACKLE,
               SoundSource.BLOCKS,
               0.5F + random.nextFloat(),
               random.nextFloat() * 0.7F + 0.6F,
               false
            );
         }

         BlockEntity blockEntity = level.getBlockEntity(pos);
         boolean hasFilterCharge = false;
         if (blockEntity instanceof VentCollectorBlockEntity ventCollector) {
            hasFilterCharge = ventCollector.getFilterCharge() > 0;
         }

         Direction facing = (Direction)state.getValue(FACING);
         double x = (double)pos.getX() + 0.5;
         double y = (double)pos.getY() + 0.55;
         double z = (double)pos.getZ() + 0.7;
         if (facing == Direction.NORTH) {
            z += 0.45;
         } else if (facing == Direction.SOUTH) {
            z -= 0.45;
         } else if (facing == Direction.WEST) {
            x += 0.45;
         } else if (facing == Direction.EAST) {
            x -= 0.45;
         }

         for (int i = 0; i < random.nextInt(2) + 2; i++) {
            double offsetX = random.nextDouble() * 0.05 - 0.025;
            double offsetY = 0.05 + random.nextDouble() * 0.05;
            double offsetZ = random.nextDouble() * 0.05 - 0.025;
            if (hasFilterCharge) {
               level.addParticle(ParticleTypes.CLOUD, x, y, z, offsetX, offsetY, offsetZ);
            } else {
               level.addParticle(ParticleTypes.LARGE_SMOKE, x, y, z, offsetX, offsetY, offsetZ);
            }
         }
      }
   }
}
