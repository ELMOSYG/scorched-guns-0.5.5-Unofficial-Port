package top.ribs.scguns.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidType;
import top.ribs.scguns.init.ModBlocks;
import top.ribs.scguns.init.ModFluids;
import top.ribs.scguns.init.ModItems;

public abstract class ViciousAcidFluid extends FlowingFluid {
   public ViciousAcidFluid() {
      super();
   }

   public Fluid getFlowing() {
      return (Fluid)ModFluids.VICIOUS_ACID_FLOWING.get();
   }

   public Fluid getSource() {
      return (Fluid)ModFluids.VICIOUS_ACID_SOURCE.get();
   }

   public Item getBucket() {
      return (Item)ModItems.VICIOUS_ACID_BUCKET.get();
   }

   protected boolean canConvertToSource(Level level) {
      return false;
   }

   protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
      Block.dropResources(state, level, pos, level.getBlockEntity(pos));
   }

   private static void reactWithSurroundings(Level level, BlockPos pos) {
      if (level instanceof ServerLevel) {
         for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = pos.relative(direction);
            FluidState adjacentFluid = level.getFluidState(adjacentPos);
            BlockState adjacentState = level.getBlockState(adjacentPos);
            if (adjacentFluid.is(FluidTags.LAVA) || adjacentState.is(Blocks.FIRE)) {
               level.setBlock(adjacentPos, Blocks.AIR.defaultBlockState(), 3);
               level.explode(
                  null,
                  (double)adjacentPos.getX() + 0.5,
                  (double)adjacentPos.getY() + 0.5,
                  (double)adjacentPos.getZ() + 0.5,
                  2.0F,
                  ExplosionInteraction.BLOCK
               );
               return;
            }

            if (adjacentFluid.is(FluidTags.WATER)) {
               level.setBlock(adjacentPos, Blocks.PRISMARINE.defaultBlockState(), 3);
               level.playSound(
                  null, adjacentPos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F + (level.random.nextFloat() - level.random.nextFloat()) * 0.8F
               );

               for (int i = 0; i < 8; i++) {
                  ((ServerLevel)level)
                     .sendParticles(
                        ParticleTypes.LARGE_SMOKE,
                        (double)adjacentPos.getX() + 0.5,
                        (double)adjacentPos.getY() + 1.0,
                        (double)adjacentPos.getZ() + 0.5,
                        1,
                        0.3,
                        0.3,
                        0.3,
                        0.0
                     );
               }
            }
         }
      }
   }

   protected int getSlopeFindDistance(LevelReader level) {
      return 4;
   }

   protected int getDropOff(LevelReader level) {
      return 1;
   }

   public int getTickDelay(LevelReader level) {
      return 5;
   }

   protected float getExplosionResistance() {
      return 100.0F;
   }

   protected BlockState createLegacyBlock(FluidState state) {
      return (BlockState)((LiquidBlock)ModBlocks.VICIOUS_ACID_BLOCK.get()).defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
   }

   public boolean isSame(Fluid fluid) {
      return fluid == ModFluids.VICIOUS_ACID_SOURCE.get() || fluid == ModFluids.VICIOUS_ACID_FLOWING.get();
   }

   public FluidType getFluidType() {
      return (FluidType)ModFluids.VICIOUS_ACID_FLUID_TYPE.get();
   }

   public static class Flowing extends ViciousAcidFluid {
      public Flowing() {
         super();
      }

      protected void createFluidStateDefinition(Builder<Fluid, FluidState> builder) {
         super.createFluidStateDefinition(builder);
         builder.add(new Property[]{LEVEL});
      }

      protected boolean canBeReplacedWith(FluidState pState, BlockGetter pLevel, BlockPos pPos, Fluid pFluid, Direction pDirection) {
         return false;
      }

      public void tick(Level level, BlockPos pos, FluidState state) {
         ViciousAcidFluid.reactWithSurroundings(level, pos);
         super.tick(level, pos, state);
      }

      public int getAmount(FluidState state) {
         return (Integer)state.getValue(LEVEL);
      }

      public boolean isSource(FluidState state) {
         return false;
      }
   }

   public static class Source extends ViciousAcidFluid {
      public Source() {
         super();
      }

      public int getAmount(FluidState state) {
         return 8;
      }

      protected boolean canBeReplacedWith(FluidState pState, BlockGetter pLevel, BlockPos pPos, Fluid pFluid, Direction pDirection) {
         return false;
      }

      public void tick(Level level, BlockPos pos, FluidState state) {
         ViciousAcidFluid.reactWithSurroundings(level, pos);
         super.tick(level, pos, state);
      }

      public boolean isSource(FluidState state) {
         return true;
      }
   }
}
