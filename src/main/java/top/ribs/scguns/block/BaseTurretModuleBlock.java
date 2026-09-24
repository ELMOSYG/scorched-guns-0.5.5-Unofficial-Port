package top.ribs.scguns.block;


import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class BaseTurretModuleBlock extends BaseEntityBlock {
   // 1.21.1 makes BlockBehaviour#codec() abstract on BaseEntityBlock, and simpleCodec cannot build an
   // abstract block, so every concrete turret module below declares its own CODEC/codec() pair.

   public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
   public static final BooleanProperty CONNECTED = BooleanProperty.create("connected");
   protected static final VoxelShape SHAPE_CENTERED = Block.box(2.0, 0.0, 2.0, 14.0, 7.0, 14.0);
   protected static final VoxelShape SHAPE_CONNECTED_NORTH = Block.box(2.0, 0.0, 6.0, 14.0, 7.0, 16.0);
   protected static final VoxelShape SHAPE_CONNECTED_SOUTH = Block.box(2.0, 0.0, 0.0, 14.0, 7.0, 10.0);
   protected static final VoxelShape SHAPE_CONNECTED_EAST = Block.box(0.0, 0.0, 2.0, 10.0, 7.0, 14.0);
   protected static final VoxelShape SHAPE_CONNECTED_WEST = Block.box(6.0, 0.0, 2.0, 16.0, 7.0, 14.0);

   public BaseTurretModuleBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH)).setValue(CONNECTED, false));
   }

   public RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING, CONNECTED});
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockPos pos = context.getClickedPos();
      BlockGetter world = context.getLevel();
      Direction facing = context.getHorizontalDirection().getOpposite();
      boolean isConnected = this.isAdjacentToTurret(world, pos);
      if (isConnected && world instanceof Level level) {
         level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 1.0F, 1.0F);
      }

      return (BlockState)((BlockState)this.defaultBlockState().setValue(FACING, facing)).setValue(CONNECTED, isConnected);
   }

   public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      Direction facing = (Direction)state.getValue(FACING);
      boolean connected = (Boolean)state.getValue(CONNECTED);
      if (connected) {
         return switch (facing) {
            case NORTH -> SHAPE_CONNECTED_NORTH;
            case SOUTH -> SHAPE_CONNECTED_SOUTH;
            case EAST -> SHAPE_CONNECTED_EAST;
            case WEST -> SHAPE_CONNECTED_WEST;
            default -> SHAPE_CENTERED;
         };
      } else {
         return SHAPE_CENTERED;
      }
   }

   public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos neighborPos, boolean isMoving) {
      super.neighborChanged(state, world, pos, block, neighborPos, isMoving);
      if (!world.isClientSide) {
         boolean isConnected = this.isAdjacentToTurret(world, pos);
         Direction correctFacing = this.getCorrectFacingForTurret(world, pos);
         if (isConnected) {
            world.setBlock(pos, (BlockState)((BlockState)state.setValue(CONNECTED, true)).setValue(FACING, correctFacing), 3);
         } else {
            world.setBlock(pos, (BlockState)state.setValue(CONNECTED, false), 3);
         }
      }
   }

   protected Direction getCorrectFacingForTurret(BlockGetter world, BlockPos pos) {
      for (Direction direction : Plane.HORIZONTAL) {
         BlockPos neighborPos = pos.relative(direction);
         if (world.getBlockState(neighborPos).getBlock() instanceof BasicTurretBlock
            || world.getBlockState(neighborPos).getBlock() instanceof ShotgunTurretBlock
            || world.getBlockState(neighborPos).getBlock() instanceof SniperTurretBlock
            || world.getBlockState(neighborPos).getBlock() instanceof AutoTurretBlock) {
            return direction.getOpposite();
         }
      }

      return Direction.NORTH;
   }

   protected boolean isAdjacentToTurret(BlockGetter world, BlockPos pos) {
      for (Direction direction : Plane.HORIZONTAL) {
         BlockPos neighborPos = pos.relative(direction);
         if (world.getBlockState(neighborPos).getBlock() instanceof BasicTurretBlock
            || world.getBlockState(neighborPos).getBlock() instanceof ShotgunTurretBlock
            || world.getBlockState(neighborPos).getBlock() instanceof SniperTurretBlock
            || world.getBlockState(neighborPos).getBlock() instanceof AutoTurretBlock) {
            return true;
         }
      }

      return false;
   }
}
