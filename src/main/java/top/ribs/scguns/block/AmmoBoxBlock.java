package top.ribs.scguns.block;


import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
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

import top.ribs.scguns.blockentity.AmmoBoxBlockEntity;

public class AmmoBoxBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
   public static final MapCodec<AmmoBoxBlock> CODEC = simpleCodec(AmmoBoxBlock::new);

   @Override
   protected MapCodec<? extends BaseEntityBlock> codec() {
      return CODEC;
   }

   public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

   public AmmoBoxBlock(Properties properties) {
      super(properties);
      this.registerDefaultState(
         (BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH)).setValue(WATERLOGGED, false))
            .setValue(OPEN, false)
      );
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new AmmoBoxBlockEntity(pos, state);
   }

   public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (!level.isClientSide) {
         BlockEntity blockEntity = level.getBlockEntity(pos);
         if (blockEntity instanceof AmmoBoxBlockEntity) {
            boolean isOpen = (Boolean)state.getValue(OPEN);
            level.setBlock(pos, (BlockState)state.setValue(OPEN, !isOpen), 3);
            ((ServerPlayer)player).openMenu((AmmoBoxBlockEntity)blockEntity, buf -> buf.writeBlockPos(pos));
         }
      }

      return InteractionResult.SUCCESS;
   }

   public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
      if (!state.is(newState.getBlock())) {
         BlockEntity blockEntity = level.getBlockEntity(pos);
         if (blockEntity instanceof Container) {
            Containers.dropContents(level, pos, (Container)blockEntity);
            level.updateNeighbourForOutputSignal(pos, this);
         }

         super.onRemove(state, level, pos, newState, isMoving);
      }
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
      return (BlockState)((BlockState)((BlockState)this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()))
            .setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER))
         .setValue(OPEN, false);
   }

   public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
      // 1.20.1 copied the item's hover name onto the block entity here. In 1.21.1 that happens
      // automatically: BlockItem#place calls BlockEntity#applyComponentsFromItemStack before
      // setPlacedBy, and BaseContainerBlockEntity#applyImplicitComponents stores
      // DataComponents.CUSTOM_NAME. BaseContainerBlockEntity no longer exposes a setter.
   }

   public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
      if (!level.isClientSide) {
         boolean powered = level.hasNeighborSignal(pos);
         boolean isOpen = (Boolean)state.getValue(OPEN);
         if (powered && !isOpen) {
            level.setBlock(pos, (BlockState)state.setValue(OPEN, true), 3);
         } else if (!powered && isOpen) {
            level.setBlock(pos, (BlockState)state.setValue(OPEN, false), 3);
         }
      }
   }

   public BlockState rotate(BlockState state, Rotation rot) {
      return (BlockState)state.setValue(FACING, rot.rotate((Direction)state.getValue(FACING)));
   }

   public BlockState mirror(BlockState state, Mirror mirrorIn) {
      return state.rotate(mirrorIn.getRotation((Direction)state.getValue(FACING)));
   }

   public FluidState getFluidState(BlockState state) {
      return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
   }

   public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
      Direction direction = (Direction)state.getValue(FACING);
      VoxelShape shapeNorthSouth = Block.box(3.0, 0.0, 1.0, 13.0, 11.0, 15.0);
      VoxelShape shapeEastWest = Block.box(1.0, 0.0, 3.0, 15.0, 11.0, 13.0);

      return switch (direction) {
         case EAST, WEST -> shapeEastWest;
         default -> shapeNorthSouth;
      };
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING, WATERLOGGED, OPEN});
   }

   public RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }
}
