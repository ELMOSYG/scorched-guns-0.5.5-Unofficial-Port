package top.ribs.scguns.block;

import javax.annotation.Nullable;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import top.ribs.scguns.blockentity.AmmoModuleBlockEntity;
import top.ribs.scguns.init.ModBlockEntities;

public class AmmoModuleBlock extends BaseTurretModuleBlock {
   public static final MapCodec<AmmoModuleBlock> CODEC = simpleCodec(AmmoModuleBlock::new);

   @Override
   protected MapCodec<? extends BaseEntityBlock> codec() {
      return CODEC;
   }

   private static final VoxelShape SHAPE_CENTERED = Block.box(2.0, 0.0, 2.0, 14.0, 9.0, 14.0);
   private static final VoxelShape SHAPE_CONNECTED_NORTH = Block.box(2.0, 0.0, 6.0, 14.0, 9.0, 16.0);
   private static final VoxelShape SHAPE_CONNECTED_SOUTH = Block.box(2.0, 0.0, 0.0, 14.0, 9.0, 10.0);
   private static final VoxelShape SHAPE_CONNECTED_EAST = Block.box(0.0, 0.0, 2.0, 10.0, 9.0, 14.0);
   private static final VoxelShape SHAPE_CONNECTED_WEST = Block.box(6.0, 0.0, 2.0, 16.0, 9.0, 14.0);

   public AmmoModuleBlock(Properties properties) {
      super(properties);
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return level.isClientSide ? null : createTickerHelper(type, (BlockEntityType)ModBlockEntities.AMMO_MODULE.get(), AmmoModuleBlockEntity::tick);
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

   @Nullable
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new AmmoModuleBlockEntity(pos, state);
   }

   public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (!level.isClientSide) {
         BlockEntity blockEntity = level.getBlockEntity(pos);
         if (blockEntity instanceof AmmoModuleBlockEntity) {
            ((ServerPlayer)player).openMenu((AmmoModuleBlockEntity)blockEntity, buf -> buf.writeBlockPos(pos));
         }
      }

      return InteractionResult.SUCCESS;
   }

   public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
      if (!state.is(newState.getBlock())) {
         BlockEntity blockEntity = level.getBlockEntity(pos);
         if (blockEntity instanceof Container) {
            Containers.dropContents(level, pos, (Container)blockEntity);
            level.updateNeighbourForOutputSignal(pos, this);
         }

         super.onRemove(state, level, pos, newState, isMoving);
      }
   }

   public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
      // See AmmoBoxBlock#setPlacedBy: 1.21.1 applies DataComponents.CUSTOM_NAME to the block entity
      // automatically via BlockItem#place -> BlockEntity#applyComponentsFromItemStack.
   }

   @Override
   public RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }
}
