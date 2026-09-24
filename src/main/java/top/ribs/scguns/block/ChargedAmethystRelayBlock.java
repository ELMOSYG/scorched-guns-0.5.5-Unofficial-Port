package top.ribs.scguns.block;


import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import top.ribs.scguns.blockentity.ChargedAmethystRelayBlockEntity;
import top.ribs.scguns.init.ModBlockEntities;

public class ChargedAmethystRelayBlock extends BaseEntityBlock {
   public static final MapCodec<ChargedAmethystRelayBlock> CODEC = simpleCodec(ChargedAmethystRelayBlock::new);

   @Override
   protected MapCodec<? extends BaseEntityBlock> codec() {
      return CODEC;
   }

   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   public static final EnumProperty<NoteBlockInstrument> INSTRUMENT = BlockStateProperties.NOTEBLOCK_INSTRUMENT;
   public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
   public static final int LISTEN_RADIUS = 20;
   protected static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0);

   public ChargedAmethystRelayBlock(Properties properties) {
      super(properties);
      this.registerDefaultState(
         (BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(POWERED, Boolean.FALSE))
               .setValue(INSTRUMENT, NoteBlockInstrument.HARP))
            .setValue(FACING, Direction.NORTH)
      );
   }

   public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
      if (direction == null) {
         return false;
      } else {
         Direction facing = (Direction)state.getValue(FACING);
         return direction.getOpposite() == facing;
      }
   }

   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{POWERED, INSTRUMENT, FACING});
   }

   private BlockState setInstrument(Level level, BlockPos pos, BlockState state) {
      NoteBlockInstrument instrumentAbove = level.getBlockState(pos.above()).instrument();
      if (instrumentAbove.worksAboveNoteBlock()) {
         return (BlockState)state.setValue(INSTRUMENT, instrumentAbove);
      } else {
         NoteBlockInstrument instrumentBelow = level.getBlockState(pos.below()).instrument();
         NoteBlockInstrument finalInstrument = instrumentBelow.worksAboveNoteBlock() ? NoteBlockInstrument.HARP : instrumentBelow;
         return (BlockState)state.setValue(INSTRUMENT, finalInstrument);
      }
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return this.setInstrument(
         context.getLevel(),
         context.getClickedPos(),
         (BlockState)((BlockState)this.defaultBlockState().setValue(POWERED, Boolean.FALSE)).setValue(FACING, context.getHorizontalDirection().getOpposite())
      );
   }

   @Override
   public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

      if (!level.isClientSide) {
         NoteBlockInstrument currentInstrument = (NoteBlockInstrument)state.getValue(INSTRUMENT);
         level.playSound(
            null,
            (double)pos.getX() + 0.5,
            (double)pos.getY() + 0.5,
            (double)pos.getZ() + 0.5,
            (SoundEvent)currentInstrument.getSoundEvent().value(),
            SoundSource.BLOCKS,
            3.0F,
            1.0F
         );
         if (level instanceof ServerLevel serverLevel) {
            double colorOffset = this.getInstrumentColorOffset(currentInstrument);
            serverLevel.sendParticles(
               ParticleTypes.NOTE,
               (double)pos.getX() + 0.5,
               (double)pos.getY() + 1.2,
               (double)pos.getZ() + 0.5,
               1,
               colorOffset,
               0.0,
               0.0,
               0.0
            );
         }

         return InteractionResult.CONSUME;
      } else {
         return InteractionResult.SUCCESS;
      }
   }

   private double getInstrumentColorOffset(NoteBlockInstrument instrument) {
      int instrumentIndex = instrument.ordinal();
      int totalInstruments = NoteBlockInstrument.values().length;
      return (double)instrumentIndex / (double)totalInstruments;
   }

   public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
      boolean flag = facing.getAxis() == Axis.Y;
      return flag ? this.setInstrument((Level)level, currentPos, state) : super.updateShape(state, facing, facingState, level, currentPos, facingPos);
   }

   public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
      BlockState newState = this.setInstrument(level, pos, state);
      if (!newState.equals(state)) {
         level.setBlock(pos, newState, 3);
         if (level.getBlockEntity(pos) instanceof ChargedAmethystRelayBlockEntity relay) {
            relay.onInstrumentTuned((NoteBlockInstrument)newState.getValue(INSTRUMENT));
         }
      }
   }

   @Nullable
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new ChargedAmethystRelayBlockEntity(pos, state);
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
      return level.isClientSide
         ? null
         : createTickerHelper(blockEntityType, (BlockEntityType)ModBlockEntities.CHARGED_AMETHYST_RELAY.get(), ChargedAmethystRelayBlockEntity::serverTick);
   }

   public RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }

   public boolean isSignalSource(BlockState state) {
      return (Boolean)state.getValue(POWERED);
   }

   public int getSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side) {
      if (!(Boolean)blockState.getValue(POWERED)) {
         return 0;
      } else {
         Direction facing = (Direction)blockState.getValue(FACING);
         return side.getOpposite() == facing ? 15 : 0;
      }
   }

   public int getDirectSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side) {
      if (!(Boolean)blockState.getValue(POWERED)) {
         return 0;
      } else {
         Direction facing = (Direction)blockState.getValue(FACING);
         return side.getOpposite() == facing ? 15 : 0;
      }
   }

   public void onInstrumentHeard(Level level, BlockPos relayPos, NoteBlockInstrument playedInstrument) {
      BlockState state = level.getBlockState(relayPos);
      if (state.getBlock() == this) {
         NoteBlockInstrument tunedInstrument = (NoteBlockInstrument)state.getValue(INSTRUMENT);
         if (playedInstrument == tunedInstrument) {
            if (level.getBlockEntity(relayPos) instanceof ChargedAmethystRelayBlockEntity relay) {
               relay.activateFromInstrument();
            }

            level.setBlock(relayPos, (BlockState)state.setValue(POWERED, true), 7);
            level.sendBlockUpdated(relayPos, state, (BlockState)state.setValue(POWERED, true), 3);
            level.scheduleTick(relayPos, this, 20);
         }
      }
   }

   public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      if ((Boolean)state.getValue(POWERED)) {
         level.setBlock(pos, (BlockState)state.setValue(POWERED, false), 7);
         level.sendBlockUpdated(pos, state, (BlockState)state.setValue(POWERED, false), 3);
         if (level.getBlockEntity(pos) instanceof ChargedAmethystRelayBlockEntity relay) {
            relay.deactivate();
         }
      }
   }

   public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
      if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ChargedAmethystRelayBlockEntity relay) {
         relay.deactivate();
      }

      super.onRemove(state, level, pos, newState, isMoving);
   }
}
