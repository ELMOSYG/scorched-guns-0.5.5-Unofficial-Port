package top.ribs.scguns.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MemorialBlock extends Block {
   public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.values());

   public MemorialBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH));
   }

   @Override
   public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

      if (level.isClientSide) {
         for (int i = 0; i < 5; i++) {
            double x = (double)pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 1.2;
            double y = (double)pos.getY() + 0.5 + (level.random.nextDouble() - 0.5) * 1.2;
            double z = (double)pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 1.2;
            double motionX = (level.random.nextDouble() - 0.5) * 0.05;
            double motionZ = (level.random.nextDouble() - 0.5) * 0.05;
            level.addParticle(ParticleTypes.HEART, x, y, z, motionX, 0.15, motionZ);
         }
      }

      return InteractionResult.sidedSuccess(level.isClientSide);
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return (BlockState)this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
   }

   public BlockState rotate(BlockState state, Rotation rotation) {
      return (BlockState)state.setValue(FACING, rotation.rotate((Direction)state.getValue(FACING)));
   }

   public BlockState mirror(BlockState state, Mirror mirror) {
      return state.rotate(mirror.getRotation((Direction)state.getValue(FACING)));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING});
   }

   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      VoxelShape base = Block.box(0.0, 0.0, 0.0, 16.0, 6.0, 16.0);
      VoxelShape top = Block.box(2.0, 6.0, 2.0, 14.0, 24.0, 14.0);
      VoxelShape combinedShape = Shapes.or(base, top);

      return switch ((Direction)state.getValue(FACING)) {
         case NORTH -> combinedShape;
         case SOUTH -> combinedShape;
         case EAST -> combinedShape;
         case WEST -> combinedShape;
         default -> combinedShape;
      };
   }

   public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
      if (state.getBlock() != newState.getBlock()) {
         super.onRemove(state, world, pos, newState, isMoving);
      }
   }
}
