package top.ribs.scguns.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import top.ribs.scguns.init.ModItems;

public class NiterLayerBlock extends SnowLayerBlock implements LiquidBlockContainer {
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

   public NiterLayerBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(LAYERS, 1)).setValue(WATERLOGGED, false));
   }

   @Override
   public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state) {
      return new ItemStack((ItemLike)ModItems.NITER_DUST.get());
   }

   public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean isMoving) {
      super.onPlace(state, world, pos, oldState, isMoving);
      if (!state.is(oldState.getBlock())) {
         world.playSound(null, pos, SoundEvents.SAND_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
      }
   }

   public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
      FluidState fluidstate = world.getFluidState(pos);
      return super.canSurvive(state, world, pos) || fluidstate.getType() == Fluids.WATER || (Boolean)state.getValue(WATERLOGGED);
   }

   public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor world, BlockPos currentPos, BlockPos facingPos) {
      if ((Boolean)state.getValue(WATERLOGGED)) {
         world.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
      }

      return super.updateShape(state, facing, facingState, world, currentPos, facingPos);
   }

   public FluidState getFluidState(BlockState state) {
      return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
   }

   public boolean canPlaceLiquid(@Nullable Player player, BlockGetter world, BlockPos pos, BlockState state, Fluid fluid) {
      return !(Boolean)state.getValue(WATERLOGGED) && fluid == Fluids.WATER;
   }

   public boolean placeLiquid(LevelAccessor world, BlockPos pos, BlockState state, FluidState fluidState) {
      if (!(Boolean)state.getValue(WATERLOGGED) && fluidState.getType() == Fluids.WATER) {
         if (!world.isClientSide()) {
            world.setBlock(pos, (BlockState)state.setValue(WATERLOGGED, true), 3);
            world.scheduleTick(pos, fluidState.getType(), fluidState.getType().getTickDelay(world));
         }

         return true;
      } else {
         return false;
      }
   }

   @Nullable
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockPos pos = context.getClickedPos();
      FluidState fluidstate = context.getLevel().getFluidState(pos);
      BlockState blockstate = context.getLevel().getBlockState(pos);
      if (blockstate.is(this)) {
         int layers = (Integer)blockstate.getValue(LAYERS);
         return (BlockState)((BlockState)blockstate.setValue(LAYERS, Math.min(8, layers + 1)))
            .setValue(WATERLOGGED, fluidstate.getType() == Fluids.WATER || fluidstate.getType() == Fluids.FLOWING_WATER);
      } else {
         return (BlockState)((BlockState)this.defaultBlockState().setValue(LAYERS, 1))
            .setValue(WATERLOGGED, fluidstate.getType() == Fluids.WATER || fluidstate.getType() == Fluids.FLOWING_WATER);
      }
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{LAYERS, WATERLOGGED});
   }
}
