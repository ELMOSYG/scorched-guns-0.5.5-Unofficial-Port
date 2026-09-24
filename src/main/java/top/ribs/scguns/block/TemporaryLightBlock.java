package top.ribs.scguns.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TemporaryLightBlock extends Block {
   public static final IntegerProperty LIGHT_LEVEL = BlockStateProperties.LEVEL;
   public static final IntegerProperty LIFETIME = IntegerProperty.create("lifetime", 0, 40);

   public TemporaryLightBlock() {
      super(
         Properties.of()
            .noCollission()
            .noOcclusion()
            .air()
            .instabreak()
            .lightLevel(state -> (Integer)state.getValue(LIGHT_LEVEL))
            .pushReaction(PushReaction.DESTROY)
            .replaceable()
      );
      this.registerDefaultState((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(LIGHT_LEVEL, 7)).setValue(LIFETIME, 20));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{LIGHT_LEVEL, LIFETIME});
   }

   public RenderShape getRenderShape(BlockState state) {
      return RenderShape.INVISIBLE;
   }

   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return Shapes.empty();
   }

   public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return Shapes.empty();
   }

   public boolean isRandomlyTicking(BlockState state) {
      return false;
   }

   public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      int lifetime = (Integer)state.getValue(LIFETIME);
      if (lifetime <= 1) {
         level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
         level.sendBlockUpdated(pos, state, Blocks.AIR.defaultBlockState(), 3);
      } else {
         BlockState newState = (BlockState)state.setValue(LIFETIME, lifetime - 1);
         level.setBlock(pos, newState, 2);
         level.scheduleTick(pos, this, 1);
      }
   }

   public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
      super.onPlace(state, level, pos, oldState, isMoving);
      if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
         serverLevel.scheduleTick(pos, this, 1);
      }
   }

   public FluidState getFluidState(BlockState state) {
      return Fluids.EMPTY.defaultFluidState();
   }
}
