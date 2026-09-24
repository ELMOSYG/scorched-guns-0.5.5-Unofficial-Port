package top.ribs.scguns.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.Property;
import top.ribs.scguns.init.ModItems;

public class SulfurLayerBlock extends SnowLayerBlock {
   public SulfurLayerBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(LAYERS, 1));
   }

   @Override
   public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state) {
      return new ItemStack((ItemLike)ModItems.SULFUR_DUST.get());
   }

   public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean isMoving) {
      super.onPlace(state, world, pos, oldState, isMoving);
      if (!state.is(oldState.getBlock())) {
         world.playSound(null, pos, SoundEvents.SAND_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
      }
   }

   public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
      return super.canSurvive(state, world, pos);
   }

   @Nullable
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockPos pos = context.getClickedPos();
      BlockState blockstate = context.getLevel().getBlockState(pos);
      if (blockstate.is(this)) {
         int layers = (Integer)blockstate.getValue(LAYERS);
         return (BlockState)blockstate.setValue(LAYERS, Math.min(8, layers + 1));
      } else {
         return (BlockState)this.defaultBlockState().setValue(LAYERS, 1);
      }
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{LAYERS});
   }
}
