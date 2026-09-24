package top.ribs.scguns.block;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import top.ribs.scguns.blockentity.ShockCoilBlockEntity;
import top.ribs.scguns.init.ModBlockEntities;
import top.ribs.scguns.util.ShockCoilTooltipHelper;

public class ShockCoilBlock extends Block implements EntityBlock {
   public static final BooleanProperty POWERED = BooleanProperty.create("powered");
   private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);

   public ShockCoilBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(POWERED, false));
   }

   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{POWERED});
   }

   public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
      if (!level.isClientSide) {
         boolean hasRedstonePower = level.hasNeighborSignal(pos);
         if (level.getBlockEntity(pos) instanceof ShockCoilBlockEntity shockCoil) {
            shockCoil.setRedstoneDisabled(hasRedstonePower);
         }
      }

      super.neighborChanged(state, level, pos, block, fromPos, isMoving);
   }

   @Nullable
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new ShockCoilBlockEntity(pos, state);
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return level.isClientSide ? null : createTickerHelper(type, (BlockEntityType)ModBlockEntities.SHOCK_COIL.get(), ShockCoilBlockEntity::serverTick);
   }

   @Override
   public void appendHoverText(ItemStack stack, Item.TooltipContext level, List<Component> tooltip, TooltipFlag flag) {
      ShockCoilTooltipHelper.addShockCoilTooltip(stack, level.level(), tooltip, flag);
   }

   @Nullable
   protected static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
      BlockEntityType<A> givenType, BlockEntityType<E> expectedType, BlockEntityTicker<? super E> ticker
   ) {
      return expectedType == givenType ? (BlockEntityTicker<A>)ticker : null;
   }

   public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
      if (!state.is(newState.getBlock())) {
         BlockEntity blockEntity = level.getBlockEntity(pos);
         if (blockEntity instanceof ShockCoilBlockEntity) {
            ((ShockCoilBlockEntity)blockEntity).drops();
         }

         super.onRemove(state, level, pos, newState, isMoving);
      }
   }
}
