package top.ribs.scguns.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SandbagBlock extends Block {
   public static final EnumProperty<SandbagBlock.SandbagType> TYPE = EnumProperty.create("type", SandbagBlock.SandbagType.class);
   public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
   private static final VoxelShape SHAPE_SINGLE_NORTH_SOUTH = Block.box(0.0, 0.0, 4.0, 16.0, 6.0, 12.0);
   private static final VoxelShape SHAPE_DOUBLE_NORTH_SOUTH = Block.box(0.0, 0.0, 4.0, 16.0, 11.0, 12.0);
   private static final VoxelShape SHAPE_TRIPLE_NORTH_SOUTH = Block.box(0.0, 0.0, 4.0, 16.0, 16.0, 12.0);
   private static final VoxelShape SHAPE_SINGLE_EAST_WEST = Block.box(4.0, 0.0, 0.0, 12.0, 6.0, 16.0);
   private static final VoxelShape SHAPE_DOUBLE_EAST_WEST = Block.box(4.0, 0.0, 0.0, 12.0, 11.0, 16.0);
   private static final VoxelShape SHAPE_TRIPLE_EAST_WEST = Block.box(4.0, 0.0, 0.0, 12.0, 16.0, 16.0);

   public SandbagBlock(Properties properties) {
      super(Properties.ofLegacyCopy(Blocks.SAND));
      this.registerDefaultState(
         (BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(TYPE, SandbagBlock.SandbagType.SINGLE)).setValue(FACING, Direction.NORTH)
      );
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{TYPE, FACING});
   }

   public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
      Direction direction = (Direction)state.getValue(FACING);
      switch ((SandbagBlock.SandbagType)state.getValue(TYPE)) {
         case DOUBLE:
            return direction != Direction.NORTH && direction != Direction.SOUTH ? SHAPE_DOUBLE_EAST_WEST : SHAPE_DOUBLE_NORTH_SOUTH;
         case TRIPLE:
            return direction != Direction.NORTH && direction != Direction.SOUTH ? SHAPE_TRIPLE_EAST_WEST : SHAPE_TRIPLE_NORTH_SOUTH;
         case SINGLE:
         default:
            return direction != Direction.NORTH && direction != Direction.SOUTH ? SHAPE_SINGLE_EAST_WEST : SHAPE_SINGLE_NORTH_SOUTH;
      }
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockPos pos = context.getClickedPos();
      Level world = context.getLevel();
      BlockState state = world.getBlockState(pos);
      if (state.is(this)) {
         if (state.getValue(TYPE) == SandbagBlock.SandbagType.SINGLE) {
            return (BlockState)state.setValue(TYPE, SandbagBlock.SandbagType.DOUBLE);
         }

         if (state.getValue(TYPE) == SandbagBlock.SandbagType.DOUBLE) {
            return (BlockState)state.setValue(TYPE, SandbagBlock.SandbagType.TRIPLE);
         }
      }

      return (BlockState)((BlockState)this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())).setValue(TYPE, SandbagBlock.SandbagType.SINGLE);
   }

   @Override
   public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {

      if (stack.getItem() == this.asItem()) {
         if (state.getValue(TYPE) == SandbagBlock.SandbagType.SINGLE) {
            world.setBlock(pos, (BlockState)state.setValue(TYPE, SandbagBlock.SandbagType.DOUBLE), 3);
            world.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
            if (!player.isCreative()) {
               stack.shrink(1);
            }

            return ItemInteractionResult.SUCCESS;
         }

         if (state.getValue(TYPE) == SandbagBlock.SandbagType.DOUBLE) {
            world.setBlock(pos, (BlockState)state.setValue(TYPE, SandbagBlock.SandbagType.TRIPLE), 3);
            world.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
            if (!player.isCreative()) {
               stack.shrink(1);
            }

            return ItemInteractionResult.SUCCESS;
         }
      } else if (player.isShiftKeyDown() && stack.isEmpty()) {
         if (state.getValue(TYPE) == SandbagBlock.SandbagType.TRIPLE) {
            world.setBlock(pos, (BlockState)state.setValue(TYPE, SandbagBlock.SandbagType.DOUBLE), 3);
         } else if (state.getValue(TYPE) == SandbagBlock.SandbagType.DOUBLE) {
            world.setBlock(pos, (BlockState)state.setValue(TYPE, SandbagBlock.SandbagType.SINGLE), 3);
         } else if (state.getValue(TYPE) == SandbagBlock.SandbagType.SINGLE) {
            world.removeBlock(pos, false);
         }

         if (!player.isCreative()) {
            Block.popResource(world, pos, new ItemStack(this));
         }

         world.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
         return ItemInteractionResult.SUCCESS;
      }

      return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
   }

   public static enum SandbagType implements StringRepresentable {
      SINGLE("single"),
      DOUBLE("double"),
      TRIPLE("triple");

      private final String name;

      private SandbagType(String name) {
         this.name = name;
      }

      public String getSerializedName() {
         return this.name;
      }

      @Override
      public String toString() {
         return this.name;
      }
   }
}
