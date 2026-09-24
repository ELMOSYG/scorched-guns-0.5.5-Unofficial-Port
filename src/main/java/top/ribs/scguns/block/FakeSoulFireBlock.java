package top.ribs.scguns.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.crystalnest.prometheus.api.FireManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import top.ribs.scguns.ScorchedGuns;

public class FakeSoulFireBlock extends BaseFireBlock {
   /** Mirrors BaseFireBlock's private fire damage so the codec can read it back. */
   private final float fireDamage;

   public static final MapCodec<FakeSoulFireBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
         net.minecraft.world.level.block.state.BlockBehaviour.Properties.CODEC.fieldOf("properties").forGetter(block -> block.properties),
         com.mojang.serialization.Codec.FLOAT.optionalFieldOf("fire_damage", 2.0F).forGetter(block -> block.fireDamage)
      ).apply(instance, FakeSoulFireBlock::new));

   @Override
   protected MapCodec<? extends BaseFireBlock> codec() {
      return CODEC;
   }

   public FakeSoulFireBlock(Properties pProperties, float pFireDamage) {
      super(pProperties.randomTicks(), pFireDamage);
      this.fireDamage = pFireDamage;
   }

   protected boolean canBurn(BlockState pState) {
      return false;
   }

   public boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
      BlockState belowState = pLevel.getBlockState(pPos.below());
      BlockState aboveState = pLevel.getBlockState(pPos.above());
      return aboveState.getBlock() instanceof BaseFireBlock ? false : belowState.isFaceSturdy(pLevel, pPos.below(), Direction.UP) || !belowState.isAir();
   }

   public BlockState updateShape(BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
      return this.canSurvive(pState, pLevel, pCurrentPos) ? pState : Blocks.AIR.defaultBlockState();
   }

   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      return this.canSurvive(this.defaultBlockState(), pContext.getLevel(), pContext.getClickedPos()) ? this.defaultBlockState() : null;
   }

   public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
      if (!pOldState.is(pState.getBlock())) {
         int randomDelay = 50 + pLevel.random.nextInt(21);
         pLevel.scheduleTick(pPos, this, randomDelay);
      }

      super.onPlace(pState, pLevel, pPos, pOldState, pIsMoving);
   }

   public void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
      pLevel.removeBlock(pPos, false);
   }

   public void entityInside(BlockState pState, Level pLevel, BlockPos pPos, Entity pEntity) {
      if (!pEntity.fireImmune()) {
         pEntity.setRemainingFireTicks(pEntity.getRemainingFireTicks() + 1);
         if (pEntity.getRemainingFireTicks() == 0) {
            pEntity.igniteForSeconds(8);
         }

         pEntity.hurt(pLevel.damageSources().inFire(), 2.0F);
         if (ScorchedGuns.soulFiredLoaded) {
            try {
               // 1.21: Soul Fire'd's fire API lives in Prometheus now, same signature as in
               // 0.5.5 (see ScorchedGuns#setSoulFireOnEntity for the evidence).
               FireManager.setOnFire(pEntity, pEntity.getRemainingFireTicks() / 20, FireManager.SOUL_FIRE_TYPE);
            } catch (Exception e) {
            }
         }
      }
   }

   public void animateTick(BlockState pState, Level pLevel, BlockPos pPos, RandomSource pRandom) {
      if (pRandom.nextInt(24) == 0) {
         pLevel.playLocalSound(
            (double)pPos.getX() + 0.5,
            (double)pPos.getY() + 0.5,
            (double)pPos.getZ() + 0.5,
            SoundEvents.FIRE_AMBIENT,
            SoundSource.BLOCKS,
            1.0F + pRandom.nextFloat(),
            pRandom.nextFloat() * 0.7F + 0.3F,
            false
         );
      }

      for (int i = 0; i < pRandom.nextInt(2) + 2; i++) {
         double x = (double)pPos.getX() + pRandom.nextDouble();
         double y = (double)pPos.getY() + pRandom.nextDouble() * 0.5 + 0.5;
         double z = (double)pPos.getZ() + pRandom.nextDouble();
         pLevel.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 0.0, 0.0, 0.0);
      }

      if (pRandom.nextInt(5) == 0) {
         double x = (double)pPos.getX() + pRandom.nextDouble();
         double y = (double)pPos.getY() + pRandom.nextDouble() * 0.5 + 0.5;
         double z = (double)pPos.getZ() + pRandom.nextDouble();
         pLevel.addParticle(ParticleTypes.SOUL, x, y, z, 0.0, 0.0, 0.0);
      }
   }
}
