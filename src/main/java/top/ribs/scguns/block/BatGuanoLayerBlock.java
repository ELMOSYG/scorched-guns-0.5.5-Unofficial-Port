package top.ribs.scguns.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
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
import top.ribs.scguns.init.ModParticleTypes;

public class BatGuanoLayerBlock extends SnowLayerBlock {
   public BatGuanoLayerBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(LAYERS, 1));
   }

   @Override
   public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state) {
      return new ItemStack((ItemLike)ModItems.BAT_GUANO.get());
   }

   public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean isMoving) {
      super.onPlace(state, world, pos, oldState, isMoving);
      if (!state.is(oldState.getBlock())) {
         world.playSound(null, pos, SoundEvents.SLIME_BLOCK_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
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

   public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
      if (random.nextInt(10) == 0) {
         double d0 = (double)pos.getX() + 0.5;
         double d1 = (double)pos.getY() + 0.3;
         double d2 = (double)pos.getZ() + 0.5;
         double offsetX = (random.nextDouble() - 0.5) * 0.3;
         double offsetZ = (random.nextDouble() - 0.5) * 0.3;
         double offsetY = random.nextDouble() * 0.1;
         level.addParticle((ParticleOptions)ModParticleTypes.SULFUR_DUST.get(), d0 + offsetX, d1 + offsetY, d2 + offsetZ, 0.0, 0.03, 0.0);
      }
   }
}
