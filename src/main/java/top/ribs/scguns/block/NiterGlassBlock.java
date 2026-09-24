package top.ribs.scguns.block;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;

public class NiterGlassBlock extends TransparentBlock {
   public static final BooleanProperty TRANSPARENT = BooleanProperty.create("transparent");

   public NiterGlassBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(TRANSPARENT, Boolean.FALSE));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{TRANSPARENT});
   }

   public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
      if (!world.isClientSide) {
         boolean isPowered = world.hasNeighborSignal(pos);
         if ((Boolean)state.getValue(TRANSPARENT) != isPowered) {
            this.updateStateAndNeighbors(world, pos, state, isPowered);
         }
      }
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      boolean isPowered = context.getLevel().hasNeighborSignal(context.getClickedPos());
      return (BlockState)this.defaultBlockState().setValue(TRANSPARENT, isPowered);
   }

   public boolean skipRendering(BlockState state, BlockState adjacentState, Direction direction) {
      return adjacentState.is(this) ? true : super.skipRendering(state, adjacentState, direction);
   }

   public boolean propagatesSkylightDown(BlockState state, BlockGetter world, BlockPos pos) {
      return (Boolean)state.getValue(TRANSPARENT);
   }

   public int getLightBlock(BlockState state, @NotNull BlockGetter world, BlockPos pos) {
      return state.getValue(TRANSPARENT) ? 0 : world.getMaxLightLevel();
   }

   private void updateStateAndNeighbors(Level world, BlockPos pos, BlockState initialState, boolean isPowered) {
      Queue<BlockPos> queue = new ArrayDeque<>();
      Set<BlockPos> visited = new HashSet<>();
      queue.add(pos);
      visited.add(pos);

      while (!queue.isEmpty()) {
         BlockPos currentPos = queue.poll();
         BlockState currentState = world.getBlockState(currentPos);
         if ((Boolean)currentState.getValue(TRANSPARENT) != isPowered) {
            BlockState newState = (BlockState)currentState.setValue(TRANSPARENT, isPowered);
            world.setBlock(currentPos, newState, 2);
         }

         for (Direction direction : Direction.values()) {
            BlockPos neighborPos = currentPos.relative(direction);
            if (!visited.contains(neighborPos) && world.getBlockState(neighborPos).getBlock() instanceof NiterGlassBlock) {
               queue.add(neighborPos);
               visited.add(neighborPos);
            }
         }
      }
   }
}
