package top.ribs.scguns.block;


import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;
import top.ribs.scguns.blockentity.PoweredMaceratorBlockEntity;
import top.ribs.scguns.init.ModBlockEntities;

public class PoweredMaceratorBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
   public static final MapCodec<PoweredMaceratorBlock> CODEC = simpleCodec(PoweredMaceratorBlock::new);

   @Override
   protected MapCodec<? extends BaseEntityBlock> codec() {
      return CODEC;
   }

   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   public static final BooleanProperty LIT = BlockStateProperties.LIT;
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

   public PoweredMaceratorBlock(Properties properties) {
      super(properties);
      this.registerDefaultState(
         (BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(WATERLOGGED, Boolean.FALSE)).setValue(LIT, Boolean.FALSE))
            .setValue(FACING, Direction.NORTH)
      );
   }

   public RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }

   @Override
   public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

      if (!level.isClientSide()) {
         BlockEntity entity = level.getBlockEntity(pos);
         if (!(entity instanceof PoweredMaceratorBlockEntity)) {
            throw new IllegalStateException("Our Container provider is missing!");
         }

         ((ServerPlayer)player).openMenu((MenuProvider)entity, buf -> buf.writeBlockPos(pos));
      }

      return InteractionResult.sidedSuccess(level.isClientSide);
   }

   @Nullable
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new PoweredMaceratorBlockEntity(pos, state);
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return createTickerHelper(type, (BlockEntityType)ModBlockEntities.POWERED_MACERATOR.get(), PoweredMaceratorBlockEntity::tick);
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{WATERLOGGED, LIT, FACING});
   }

   public FluidState getFluidState(BlockState state) {
      return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      FluidState fluidstate = context.getLevel().getFluidState(context.getClickedPos());
      return (BlockState)((BlockState)this.defaultBlockState().setValue(WATERLOGGED, fluidstate.getType() == Fluids.WATER))
         .setValue(FACING, context.getHorizontalDirection().getOpposite());
   }

   public BlockState rotate(BlockState state, Rotation rot) {
      return (BlockState)state.setValue(FACING, rot.rotate((Direction)state.getValue(FACING)));
   }

   public BlockState mirror(BlockState state, Mirror mirrorIn) {
      return state.rotate(mirrorIn.getRotation((Direction)state.getValue(FACING)));
   }

   public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
      if (state.getBlock() != newState.getBlock()) {
         BlockEntity blockEntity = level.getBlockEntity(pos);
         if (blockEntity instanceof PoweredMaceratorBlockEntity) {
            ((PoweredMaceratorBlockEntity)blockEntity).drops();
         }
      }

      super.onRemove(state, level, pos, newState, isMoving);
   }

   public boolean hasAnalogOutputSignal(BlockState state) {
      return true;
   }

   public int getAnalogOutputSignal(BlockState blockState, Level level, BlockPos pos) {
      return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
   }

   public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
      if ((Boolean)state.getValue(LIT)) {
         double x = (double)pos.getX() + 0.5;
         double y = (double)pos.getY();
         double z = (double)pos.getZ() + 0.5;
         if (random.nextDouble() < 0.1) {
            level.playLocalSound(x, y, z, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
         }

         Direction direction = (Direction)state.getValue(FACING);
         Axis axis = direction.getAxis();
         double offset = 0.52;
         double offsetRandom = random.nextDouble() * 0.6 - 0.3;
         double offsetX = axis == Axis.X ? (double)direction.getStepX() * offset : offsetRandom;
         double offsetY = random.nextDouble() * 6.0 / 16.0;
         double offsetZ = axis == Axis.Z ? (double)direction.getStepZ() * offset : offsetRandom;
         level.addParticle(ParticleTypes.SMOKE, x + offsetX, y + offsetY, z + offsetZ, 0.0, 0.0, 0.0);
         level.addParticle(ParticleTypes.FLAME, x + offsetX, y + offsetY, z + offsetZ, 0.0, 0.0, 0.0);
      }
   }
}
