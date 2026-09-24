package top.ribs.scguns.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import top.ribs.scguns.blockentity.PenetratorBlockEntity;

public class PenetratorBlock extends BaseEntityBlock {
   public static final MapCodec<PenetratorBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
         net.minecraft.world.level.block.state.BlockBehaviour.Properties.CODEC.fieldOf("properties").forGetter(block -> block.properties),
         com.mojang.serialization.Codec.INT.optionalFieldOf("tunnel_length", 3).forGetter(block -> block.tunnelLength)
      ).apply(instance, PenetratorBlock::new));

   @Override
   protected MapCodec<? extends BaseEntityBlock> codec() {
      return CODEC;
   }

   public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.values());
   private final int tunnelLength;

   public PenetratorBlock(Properties pProperties, int tunnelLength) {
      super(pProperties);
      this.tunnelLength = tunnelLength;
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.SOUTH));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
      pBuilder.add(new Property[]{FACING});
   }

   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      Direction facing;
      if (pContext.getPlayer() != null && pContext.getPlayer().isShiftKeyDown()) {
         facing = pContext.getClickedFace();
      } else {
         facing = pContext.getHorizontalDirection();
      }

      return (BlockState)this.defaultBlockState().setValue(FACING, facing);
   }

   public boolean canConnectRedstone(BlockState state, BlockGetter world, BlockPos pos, @Nullable Direction side) {
      Direction facing = (Direction)state.getValue(FACING);
      return side == null || side == facing;
   }

   public boolean isSignalSource(BlockState pState) {
      return false;
   }

   public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
      if (!level.isClientSide) {
         Direction facing = (Direction)state.getValue(FACING);
         BlockPos connectionPos = pos.relative(facing);
         boolean shouldActivate = false;
         if (fromPos.equals(connectionPos)) {
            shouldActivate = level.hasNeighborSignal(pos) || level.getSignal(connectionPos, facing) > 0;
         } else {
            shouldActivate = level.getSignal(connectionPos, facing) > 0 || level.hasNeighborSignal(pos);
         }

         if (shouldActivate) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof PenetratorBlockEntity) {
               PenetratorBlockEntity.tick(level, pos, state, (PenetratorBlockEntity)blockEntity);
            }
         }
      }
   }

   @Nullable
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new PenetratorBlockEntity(pos, state);
   }

   public int getTunnelLength() {
      return this.tunnelLength;
   }

   public RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }
}
