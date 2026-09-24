package top.ribs.scguns.block;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import top.ribs.scguns.blockentity.GunShelfBlockEntity;

public class GunShelfBlock extends Block implements EntityBlock {
   public static final List<Block> GUN_SHELF_BLOCKS = new ArrayList<>();
   protected static final VoxelShape SHAPE_NORTH = Block.box(0.0, 1.0, 14.0, 16.0, 12.0, 16.0);
   protected static final VoxelShape SHAPE_SOUTH = Block.box(0.0, 1.0, 0.0, 16.0, 12.0, 2.0);
   protected static final VoxelShape SHAPE_WEST = Block.box(14.0, 1.0, 0.0, 16.0, 12.0, 16.0);
   protected static final VoxelShape SHAPE_EAST = Block.box(0.0, 1.0, 0.0, 2.0, 12.0, 16.0);
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

   public GunShelfBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(WATERLOGGED, false)).setValue(FACING, Direction.NORTH));
      GUN_SHELF_BLOCKS.add(this);
   }

   public boolean isPathfindable(BlockState state, BlockGetter reader, BlockPos pos, PathComputationType pathType) {
      return true;
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{WATERLOGGED, FACING});
   }

   public BlockState rotate(BlockState state, Rotation rot) {
      return (BlockState)state.setValue(FACING, rot.rotate((Direction)state.getValue(FACING)));
   }

   public BlockState mirror(BlockState state, Mirror mirrorIn) {
      return state.rotate(mirrorIn.getRotation((Direction)state.getValue(FACING)));
   }

   public boolean canSurvive(BlockState state, LevelReader worldIn, BlockPos pos) {
      return !worldIn.getBlockState(pos.relative(((Direction)state.getValue(FACING)).getOpposite())).isAir();
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockState state = super.getStateForPlacement(context);
      return context.getClickedFace() != Direction.UP && context.getClickedFace() != Direction.DOWN
         ? (BlockState)state.setValue(FACING, context.getClickedFace())
         : (BlockState)state.setValue(FACING, Direction.NORTH);
   }

   public BlockState updateShape(BlockState stateIn, Direction facing, BlockState facingState, LevelAccessor worldIn, BlockPos currentPos, BlockPos facingPos) {
      if ((Boolean)stateIn.getValue(WATERLOGGED)) {
         worldIn.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(worldIn));
      }

      return facing == ((Direction)stateIn.getValue(FACING)).getOpposite() && !stateIn.canSurvive(worldIn, currentPos)
         ? Blocks.AIR.defaultBlockState()
         : super.updateShape(stateIn, facing, facingState, worldIn, currentPos, facingPos);
   }

   public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader world, BlockPos pos, Player player) {
      if (target.getLocation().y() >= (double)pos.getY() + 0.25 && world.getBlockEntity(pos) instanceof GunShelfBlockEntity tile) {
         ItemStack i = tile.getItem(0);
         if (!i.isEmpty()) {
            return i;
         }
      }

      return super.getCloneItemStack(state, target, world, pos, player);
   }

   public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
      if (!worldIn.isClientSide && worldIn.getBlockEntity(pos) instanceof GunShelfBlockEntity tile) {
         ItemStack heldItem = player.getItemInHand(handIn);
         if (tile.isEmpty() && !heldItem.isEmpty()) {
            ItemStack itemToPlace = heldItem.split(1);
            tile.setDisplayedItem(itemToPlace);
            worldIn.sendBlockUpdated(pos, state, state, 3);
            return ItemInteractionResult.SUCCESS;
         }

         if (!tile.isEmpty()) {
            ItemStack displayedItem = tile.getDisplayedItem();
            if (!player.getInventory().add(displayedItem)) {
               player.drop(displayedItem, false);
            }

            tile.setDisplayedItem(ItemStack.EMPTY);
            worldIn.sendBlockUpdated(pos, state, state, 3);
            return ItemInteractionResult.SUCCESS;
         }
      }

      return ItemInteractionResult.CONSUME;
   }

   @Nullable
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new GunShelfBlockEntity(pos, state);
   }

   public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
      return true;
   }

   public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return switch ((Direction)state.getValue(FACING)) {
         case SOUTH -> SHAPE_SOUTH;
         case EAST -> SHAPE_EAST;
         case WEST -> SHAPE_WEST;
         default -> SHAPE_NORTH;
      };
   }

   public MenuProvider getMenuProvider(BlockState state, Level worldIn, BlockPos pos) {
      return worldIn.getBlockEntity(pos) instanceof MenuProvider menuProvider ? menuProvider : null;
   }

   public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
      if (state.getBlock() != newState.getBlock()) {
         if (world.getBlockEntity(pos) instanceof GunShelfBlockEntity tile) {
            Containers.dropItemStack(world, (double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), tile.getDisplayedItem());
            tile.setDisplayedItem(ItemStack.EMPTY);
         }

         super.onRemove(state, world, pos, newState, isMoving);
      }
   }

   public boolean hasAnalogOutputSignal(BlockState state) {
      return true;
   }

   public int getAnalogOutputSignal(BlockState blockState, Level world, BlockPos pos) {
      if (world.getBlockEntity(pos) instanceof Container tile) {
         return tile.isEmpty() ? 0 : 15;
      } else {
         return 0;
      }
   }

   public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
      super.setPlacedBy(world, pos, state, entity, stack);
   }
}
