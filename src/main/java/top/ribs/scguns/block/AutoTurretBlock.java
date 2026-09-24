package top.ribs.scguns.block;


import com.mojang.serialization.MapCodec;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.NotNull;
import top.ribs.scguns.blockentity.AutoTurretBlockEntity;
import top.ribs.scguns.blockentity.TurretBlockEntity;
import top.ribs.scguns.init.ModBlockEntities;
import top.ribs.scguns.util.TurretTooltipHelper;

public class AutoTurretBlock extends BaseEntityBlock {
   public static final MapCodec<AutoTurretBlock> CODEC = simpleCodec(AutoTurretBlock::new);

   @Override
   protected MapCodec<? extends BaseEntityBlock> codec() {
      return CODEC;
   }

   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

   public AutoTurretBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH)).setValue(POWERED, false));
   }

   public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
      VoxelShape base = Block.box(0.0, 0.0, 0.0, 16.0, 10.0, 16.0);
      VoxelShape turretHead = Block.box(6.0, 10.0, 6.0, 10.0, 16.0, 10.0);
      return Shapes.or(base, turretHead);
   }

   @Override
   public void appendHoverText(ItemStack stack, Item.TooltipContext level, List<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, level, tooltip, flag);
      TurretTooltipHelper.addTurretTooltip(ResourceLocation.fromNamespaceAndPath("scguns", "auto_turret"), stack, level.level(), tooltip, flag);
   }

   @Nullable
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new AutoTurretBlockEntity(pos, state);
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return createTickerHelper(type, (BlockEntityType)ModBlockEntities.AUTO_TURRET.get(), TurretBlockEntity::tick);
   }

   public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
      if (!world.isClientSide) {
         MenuProvider menuProvider = this.getMenuProvider(state, world, pos);
         if (menuProvider != null) {
            ((ServerPlayer)player).openMenu(menuProvider, buf -> buf.writeBlockPos(pos));
         }
      }

      return InteractionResult.sidedSuccess(world.isClientSide);
   }

   public MenuProvider getMenuProvider(BlockState state, Level world, BlockPos pos) {
      BlockEntity blockEntity = world.getBlockEntity(pos);
      return blockEntity instanceof MenuProvider ? (MenuProvider)blockEntity : null;
   }

   public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
      if (!state.is(newState.getBlock())) {
         if (level.getBlockEntity(pos) instanceof AutoTurretBlockEntity turretBlockEntity) {
            turretBlockEntity.drops();
         }

         super.onRemove(state, level, pos, newState, isMoving);
      }
   }

   public RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING, POWERED});
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return (BlockState)this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
   }

   public void setPlacedBy(Level world, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
      if (!world.isClientSide && placer instanceof ServerPlayer serverPlayer && world.getBlockEntity(pos) instanceof AutoTurretBlockEntity turretBlockEntity) {
         turretBlockEntity.setOwner(serverPlayer);
      }
   }

   @NotNull
   public BlockState updateShape(
      BlockState state, @NotNull Direction direction, @NotNull BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos
   ) {
      boolean isPowered = level.hasNeighborSignal(pos);
      return (BlockState)state.setValue(POWERED, isPowered);
   }

   public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos neighborPos, boolean isMoving) {
      boolean isPowered = level.hasNeighborSignal(pos);
      if (isPowered != (Boolean)state.getValue(POWERED)) {
         level.setBlock(pos, (BlockState)state.setValue(POWERED, isPowered), 3);
      }
   }

   public BlockState rotate(BlockState state, Rotation rot) {
      return (BlockState)state.setValue(FACING, rot.rotate((Direction)state.getValue(FACING)));
   }

   public BlockState mirror(BlockState state, Mirror mirrorIn) {
      return state.rotate(mirrorIn.getRotation((Direction)state.getValue(FACING)));
   }

   public boolean isSignalSource(BlockState state) {
      return false;
   }

   public int getSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side) {
      return 0;
   }

   public int getDirectSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side) {
      return 0;
   }
}
