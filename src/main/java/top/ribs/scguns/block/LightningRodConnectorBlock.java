package top.ribs.scguns.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import top.ribs.scguns.util.ScEnchants;

public class LightningRodConnectorBlock extends LightningRodBlock {
   public LightningRodConnectorBlock(Properties pProperties) {
      super(pProperties);
   }

   public void onLightningStrike(BlockState pState, Level pLevel, BlockPos pPos) {
      pLevel.setBlock(pPos, (BlockState)pState.setValue(POWERED, true), 3);
      this.propagatePoweredState(pLevel, pPos, true);
      pLevel.scheduleTick(pPos, this, 8);
      pLevel.levelEvent(3002, pPos, ((Direction)pState.getValue(FACING)).getAxis().ordinal());
   }

   public void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
      pLevel.setBlock(pPos, (BlockState)pState.setValue(POWERED, false), 3);
      this.propagatePoweredState(pLevel, pPos, false);
   }

   public void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
      super.neighborChanged(pState, pLevel, pPos, pBlock, pFromPos, pIsMoving);
      this.checkAndUpdatePoweredState(pLevel, pPos);
   }

   private void propagatePoweredState(Level pLevel, BlockPos pPos, boolean isPowered) {
      BlockPos belowPos = pPos.below();
      BlockState belowState = pLevel.getBlockState(belowPos);
      if (belowState.getBlock() instanceof LightningRodConnectorBlock) {
         pLevel.setBlock(belowPos, (BlockState)belowState.setValue(POWERED, isPowered), 3);
         ((LightningRodConnectorBlock)belowState.getBlock()).propagatePoweredState(pLevel, belowPos, isPowered);
      }
   }

   private void checkAndUpdatePoweredState(Level pLevel, BlockPos pPos) {
      BlockPos abovePos = pPos.above();
      BlockState aboveState = pLevel.getBlockState(abovePos);
      if ((aboveState.getBlock() instanceof LightningRodBlock || aboveState.getBlock() instanceof LightningRodConnectorBlock)
         && (Boolean)aboveState.getValue(POWERED)) {
         pLevel.setBlock(pPos, (BlockState)pLevel.getBlockState(pPos).setValue(POWERED, true), 3);
         this.propagatePoweredState(pLevel, pPos, true);
      } else {
         pLevel.setBlock(pPos, (BlockState)pLevel.getBlockState(pPos).setValue(POWERED, false), 3);
         this.propagatePoweredState(pLevel, pPos, false);
      }
   }

   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      FluidState fluidstate = pContext.getLevel().getFluidState(pContext.getClickedPos());
      boolean flag = fluidstate.getType() == Fluids.WATER;
      return (BlockState)((BlockState)this.defaultBlockState().setValue(FACING, pContext.getClickedFace())).setValue(WATERLOGGED, flag);
   }

   public BlockState updateShape(BlockState pState, Direction pDirection, BlockState pNeighborState, LevelAccessor pLevel, BlockPos pPos, BlockPos pNeighborPos) {
      if ((Boolean)pState.getValue(WATERLOGGED)) {
         pLevel.scheduleTick(pPos, Fluids.WATER, Fluids.WATER.getTickDelay(pLevel));
      }

      return super.updateShape(pState, pDirection, pNeighborState, pLevel, pPos, pNeighborPos);
   }

   public FluidState getFluidState(BlockState pState) {
      return pState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(pState);
   }

   public int getSignal(BlockState pState, BlockGetter pLevel, BlockPos pPos, Direction pDirection) {
      return pState.getValue(POWERED) ? 15 : 0;
   }

   public int getDirectSignal(BlockState pState, BlockGetter pLevel, BlockPos pPos, Direction pDirection) {
      return pState.getValue(POWERED) && pState.getValue(FACING) == pDirection ? 15 : 0;
   }

   public void onProjectileHit(Level pLevel, BlockState pState, BlockHitResult pHit, Projectile pProjectile) {
      // ThrownTrident#isChanneling() was removed in 1.21; channeling is a data-driven enchantment now,
      // so test the trident's own stack for the vanilla Channeling enchantment instead.
      if (pLevel.isThundering()
         && pProjectile instanceof ThrownTrident trident
         && ScEnchants.has(trident.getPickupItemStackOrigin(), Enchantments.CHANNELING)) {
         BlockPos blockpos = pHit.getBlockPos();
         if (pLevel.canSeeSky(blockpos)) {
            LightningBolt lightningbolt = (LightningBolt)EntityType.LIGHTNING_BOLT.create(pLevel);
            if (lightningbolt != null) {
               lightningbolt.moveTo(Vec3.atBottomCenterOf(blockpos.above()));
               Entity entity = pProjectile.getOwner();
               lightningbolt.setCause(entity instanceof ServerPlayer ? (ServerPlayer)entity : null);
               pLevel.addFreshEntity(lightningbolt);
            }

            pLevel.playSound(
               null,
               (double)blockpos.getX() + 0.5,
               (double)blockpos.getY() + 0.5,
               (double)blockpos.getZ() + 0.5,
               SoundEvents.TRIDENT_THUNDER,
               SoundSource.WEATHER,
               5.0F,
               1.0F
            );
         }
      }
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
      pBuilder.add(new Property[]{FACING, POWERED, WATERLOGGED});
   }

   public boolean isSignalSource(BlockState pState) {
      return true;
   }
}
