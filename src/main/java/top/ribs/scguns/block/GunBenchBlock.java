package top.ribs.scguns.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import top.ribs.scguns.blockentity.GunBenchBlockEntity;

public class GunBenchBlock extends Block implements EntityBlock {
   private static final Component CONTAINER_TITLE = Component.translatable("container.gun_bench");
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

   public GunBenchBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH));
   }

   @Override
   public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

      if (level.isClientSide) {
         return InteractionResult.SUCCESS;
      } else {
         // The writer is not optional. NeoForge only puts a menu's extra data into the packet when the
         // writer produced at least one byte (ServerPlayer#openMenu: an empty buffer means the plain
         // ClientboundOpenScreenPacket, which carries no data at all), and the client then calls the
         // menu type's factory with a null buffer. GunBenchMenu's buffer constructor happens to ignore
         // it, so this looked harmless - but any menu whose factory reads the buffer (MaceratorMenu and
         // every other machine here) dies with a NullPointerException on the client and the connection
         // is killed with "Network protocol error". Always write the position: it costs four bytes and
         // removes the whole failure mode. See tools/audit_menu_extra_data.py.
         // TEMPORARY PROBE (removed before the next round): see MaceratorBlock. This one now always
         // writes the position, so a reproduction should name the opener that still does not.
         top.ribs.scguns.ScorchedGuns.LOGGER.info("SCGUNS-MENU gun_bench pos={} player={} spectator={}",
            pos, player.getGameProfile().getName(), player.isSpectator());
         player.openMenu(state.getMenuProvider(level, pos), buf -> buf.writeBlockPos(pos));
         player.awardStat(Stats.INTERACT_WITH_CRAFTING_TABLE);
         return InteractionResult.CONSUME;
      }
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return (BlockState)this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
   }

   public BlockState rotate(BlockState state, Rotation rot) {
      return (BlockState)state.setValue(FACING, rot.rotate((Direction)state.getValue(FACING)));
   }

   public BlockState mirror(BlockState state, Mirror mirrorIn) {
      return state.rotate(mirrorIn.getRotation((Direction)state.getValue(FACING)));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING});
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new GunBenchBlockEntity(pos, state);
   }

   public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
      if (!state.is(newState.getBlock())) {
         BlockEntity blockEntity = level.getBlockEntity(pos);
         if (blockEntity instanceof GunBenchBlockEntity) {
            ((GunBenchBlockEntity)blockEntity).dropContents(null);
            level.updateNeighbourForOutputSignal(pos, this);
         }

         super.onRemove(state, level, pos, newState, isMoving);
      }
   }

   public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
      BlockEntity blockEntity = level.getBlockEntity(pos);
      return blockEntity instanceof GunBenchBlockEntity ? (GunBenchBlockEntity)blockEntity : null;
   }
}
