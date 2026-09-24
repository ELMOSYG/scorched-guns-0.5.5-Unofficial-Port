package top.ribs.scguns.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class FireRateModuleBlock extends BaseTurretModuleBlock {
   public static final MapCodec<FireRateModuleBlock> CODEC = simpleCodec(FireRateModuleBlock::new);

   @Override
   protected MapCodec<? extends BaseEntityBlock> codec() {
      return CODEC;
   }

   private static final VoxelShape SHAPE_CENTERED = Block.box(2.0, 0.0, 2.0, 14.0, 11.0, 14.0);
   private static final VoxelShape SHAPE_CONNECTED_NORTH = Block.box(2.0, 0.0, 6.0, 14.0, 11.0, 16.0);
   private static final VoxelShape SHAPE_CONNECTED_SOUTH = Block.box(2.0, 0.0, 0.0, 14.0, 11.0, 10.0);
   private static final VoxelShape SHAPE_CONNECTED_EAST = Block.box(0.0, 0.0, 2.0, 10.0, 11.0, 14.0);
   private static final VoxelShape SHAPE_CONNECTED_WEST = Block.box(6.0, 0.0, 2.0, 16.0, 11.0, 14.0);

   public FireRateModuleBlock(Properties properties) {
      super(properties);
   }

   @Override
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

   @Override
   public RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }

   @Nullable
   public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
      return null;
   }
}
