package top.ribs.scguns.block;


import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import top.ribs.scguns.common.SulfurGasCloud;
import top.ribs.scguns.init.ModBlocks;
import top.ribs.scguns.init.ModParticleTypes;

public class SulfurVentBlock extends VentBlock {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final int CLOUD_RADIUS = 8;
   private static final int MAX_DUST_PARTICLES_PER_TICK = 10;
   private static final int CLOUD_SPAWN_CHANCE = 95;
   private static final int DUST_SPAWN_CHANCE = 98;
   private static final int EFFECT_INTERVAL = 10;
   public static final int EFFECT_RADIUS = 8;
   public static final int MAX_ACTIVE_VENTS = 1;
   public static final int CHECK_RADIUS = 32;
   public static final int EFFECT_RADIUS_SQUARED = 64;

   public SulfurVentBlock(Properties properties) {
      super(properties, ResourceLocation.fromNamespaceAndPath("scguns", "sulfur_vent"));
   }

   @Override
   public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
      if (!world.isClientSide) {
         if (!(Boolean)state.getValue(ACTIVE)) {
            return;
         }

         Vec3 center = Vec3.atCenterOf(pos);
         if (SulfurGasCloud.checkAndHandleFireExplosion(world, center, 8.0)) {
            this.shutdownVentTemporarily(world, pos, state);
            return;
         }

         boolean hasCollectorAbove = this.hasVentCollectorAbove(world, pos);
         if (state.getValue(VENT_TYPE) == VentBlock.VentType.BASE && !hasCollectorAbove) {
            int tickCount = (int)(world.getGameTime() % 2147483647L);
            this.spawnSulfurCloud(world, pos, random, tickCount);
            this.spawnSulfurDust(world, pos, random, tickCount);
            this.performEnvironmentalAction(world, pos, center, random);
         }

         this.applyEffectsToEntities(world, pos);
         world.sendParticles(
            (SimpleParticleType)ModParticleTypes.SULFUR_SMOKE.get(),
            (double)pos.getX() + 0.5,
            (double)pos.getY() + 0.5,
            (double)pos.getZ() + 0.5,
            1,
            0.5,
            0.5,
            0.5,
            0.01
         );
         world.scheduleTick(pos, this, 10);
         if (world.getGameTime() % (long)this.calculateNextTickInterval() == 0L) {
            this.scheduleParticleSpawn(world, pos);
         }
      }
   }

   private void shutdownVentTemporarily(Level level, BlockPos pos, BlockState state) {
      level.setBlock(pos, (BlockState)state.setValue(ACTIVE, false), 3);
      level.sendBlockUpdated(pos, state, state, 2);
      level.scheduleTick(pos, this, 100);
   }

   private void performEnvironmentalAction(ServerLevel world, BlockPos pos, Vec3 center, RandomSource random) {
      SulfurGasCloud.destroyNatureInArea(world, center, 8.0, random);
      if (this.config != null && this.config.getPlacement().isEnabled() && this.shouldPlaceBlock(random)) {
         this.placeLayerBlock(world, pos, random);
      }
   }

   private boolean shouldPlaceBlock(RandomSource random) {
      return this.config == null ? true : random.nextFloat() < this.config.getPlacement().getPlacementChance();
   }

   private void placeLayerBlock(ServerLevel world, BlockPos pos, RandomSource random) {
      if (this.config != null) {
         ResourceLocation blockToPlace = this.config.getPlacement().getBlockToPlace();
         if (blockToPlace != null) {
            Block layerBlock;
            if (blockToPlace.toString().equals("scguns:sulfur_layer")) {
               layerBlock = (Block)ModBlocks.SULFUR_LAYER.get();
            } else if (blockToPlace.toString().equals("scguns:niter_layer")) {
               layerBlock = (Block)ModBlocks.NITER_LAYER.get();
            } else {
               layerBlock = (Block)BuiltInRegistries.BLOCK.get(blockToPlace);
            }

            if (layerBlock != null && layerBlock != Blocks.AIR) {
               int radius = this.config.getPlacement().getRadius();
               double angle = random.nextDouble() * 2.0 * Math.PI;
               double distance = Math.sqrt(random.nextDouble()) * (double)radius;
               int x = (int)Math.round(Math.cos(angle) * distance);
               int z = (int)Math.round(Math.sin(angle) * distance);
               int y = random.nextInt(3) - 1;
               BlockPos randomPos = pos.offset(x, y, z);
               if (this.canPlaceLayerBlock(world, randomPos, layerBlock)) {
                  BlockPos abovePos = randomPos.above();
                  BlockState currentState = world.getBlockState(abovePos);
                  boolean isWater = currentState.getFluidState().is(FluidTags.WATER);
                  if (currentState.isAir() || isWater) {
                     BlockState layerState = layerBlock.defaultBlockState();
                     if (layerState.hasProperty(SulfurLayerBlock.LAYERS)) {
                        layerState = (BlockState)layerState.setValue(SulfurLayerBlock.LAYERS, 1);
                     }

                     world.setBlock(abovePos, layerState, 3);
                  }
               }
            } else {
               LOGGER.warn("Could not resolve block: {}", blockToPlace);
            }
         }
      }
   }

   private boolean canPlaceLayerBlock(ServerLevel world, BlockPos pos, Block layerBlock) {
      BlockState state = world.getBlockState(pos);
      BlockState aboveState = world.getBlockState(pos.above());
      return state.isFaceSturdy(world, pos, Direction.UP)
         && (aboveState.isAir() || aboveState.getFluidState().is(FluidTags.WATER))
         && !(state.getBlock() instanceof SulfurVentBlock)
         && !(aboveState.getBlock() instanceof SulfurVentBlock);
   }

   @Override
   public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
      super.onRemove(state, level, pos, newState, isMoving);
   }

   private void spawnSulfurCloud(Level level, BlockPos pos, RandomSource random, int tickCount) {
      if (random.nextInt(100) < 95) {
         Vec3 center = Vec3.atCenterOf(pos);
         float intensity = random.nextFloat() * 0.5F + 0.5F;
         SulfurGasCloud.spawnEnhancedGasCloud(level, center, 8.0, intensity, random, tickCount);
         if ((double)random.nextFloat() < 0.2) {
            double x = (double)pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 2.0;
            double y = (double)pos.getY() + 0.5 + random.nextDouble();
            double z = (double)pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 2.0;
            if (level instanceof ServerLevel serverLevel) {
               for (ServerPlayer player : serverLevel.getEntitiesOfClass(
                  ServerPlayer.class,
                  new AABB(
                     (double)(pos.getX() - 256),
                     (double)(pos.getY() - 256),
                     (double)(pos.getZ() - 256),
                     (double)(pos.getX() + 256),
                     (double)(pos.getY() + 256),
                     (double)(pos.getZ() + 256)
                  )
               )) {
                  serverLevel.sendParticles(player, ParticleTypes.SMOKE, true, x, y, z, 1, 0.0, 0.05, 0.0, 0.1);
               }
            }
         }
      }
   }

   private void spawnSulfurDust(Level level, BlockPos pos, RandomSource random, int tickCount) {
      if (random.nextInt(100) < 98) {
         if (level instanceof ServerLevel serverLevel) {
            Vec3 center = Vec3.atCenterOf(pos);
            int particlesToSpawn = random.nextInt(10) + 5;
            SulfurGasCloud.spawnDustParticlesForced(serverLevel, center, 9.6, particlesToSpawn, random, tickCount);
         }
      }
   }

   private void applyEffectsToEntities(ServerLevel world, BlockPos pos) {
      SulfurGasCloud.applyGasEffects(world, pos, 8.0, 400, 1);
   }

   private void scheduleParticleSpawn(ServerLevel world, BlockPos pos) {
      world.sendBlockUpdated(pos, world.getBlockState(pos), world.getBlockState(pos), 2);
   }

   public void animateTick(BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull RandomSource random) {
      if (this.isTopOrBaseWithoutTop(state, level, pos) && (Boolean)state.getValue(ACTIVE)) {
         if (this.shouldShowParticles()) {
            this.playAmbientSound(level, pos, random);

            for (int i = 0; i < random.nextInt(2) + 2; i++) {
               double offsetX = random.nextDouble() * 0.05 - 0.025;
               double offsetY = 0.05 + random.nextDouble() * 0.05;
               double offsetZ = random.nextDouble() * 0.05 - 0.025;
               level.addParticle(
                  ParticleTypes.LARGE_SMOKE,
                  (double)pos.getX() + 0.5,
                  (double)pos.getY() + 1.0,
                  (double)pos.getZ() + 0.5,
                  offsetX,
                  offsetY,
                  offsetZ
               );
            }

            for (int i = 0; i < random.nextInt(2) + 2; i++) {
               double offsetX = random.nextDouble() * 0.2 - 0.1;
               double offsetY = 0.05 + random.nextDouble() * 0.05;
               double offsetZ = random.nextDouble() * 0.2 - 0.1;
               level.addParticle(
                  ParticleTypes.SMOKE,
                  (double)pos.getX() + 0.5,
                  (double)pos.getY() + 1.0,
                  (double)pos.getZ() + 0.5,
                  offsetX,
                  offsetY,
                  offsetZ
               );
            }

            for (int i = 0; i < random.nextInt(2) + 1; i++) {
               double offsetX = random.nextDouble() * 0.05 - 0.025;
               double offsetY = 0.2 + random.nextDouble() * 0.2;
               double offsetZ = random.nextDouble() * 0.05 - 0.025;
               level.addParticle(
                  ParticleTypes.LAVA,
                  (double)pos.getX() + 0.5,
                  (double)pos.getY() + 1.0,
                  (double)pos.getZ() + 0.5,
                  offsetX,
                  offsetY,
                  offsetZ
               );
            }
         }
      }
   }

   @Override
   protected boolean isActive(LevelAccessor level, BlockPos pos) {
      if (this.config == null) {
         this.reloadConfig();
         if (this.config == null) {
            return false;
         }
      }

      BlockPos basePos = this.getBasePos(level, pos);
      BlockState belowState = level.getBlockState(basePos.below());
      ResourceLocation configBaseBlock = this.config.getActivation().getBaseBlock();
      Block baseBlock = (Block)BuiltInRegistries.BLOCK.get(configBaseBlock);
      if (baseBlock != null && belowState.is(baseBlock)) {
         int activeVentCount = this.countActiveVentsNearby(level, basePos);
         return activeVentCount < 1;
      } else {
         return false;
      }
   }

   private int countActiveVentsNearby(LevelAccessor level, BlockPos pos) {
      int activeCount = 0;

      for (BlockPos checkPos : BlockPos.betweenClosed(pos.offset(-32, -32, -32), pos.offset(32, 32, 32))) {
         if (!checkPos.equals(pos)) {
            BlockState state = level.getBlockState(checkPos);
            if (state.getBlock() instanceof SulfurVentBlock && (Boolean)state.getValue(ACTIVE) && state.getValue(VENT_TYPE) == VentBlock.VentType.BASE) {
               activeCount++;
            }

            if (activeCount >= 1) {
               return activeCount;
            }
         }
      }

      return activeCount;
   }

   @Override
   public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
      super.onPlace(state, level, pos, oldState, isMoving);
      BlockPos basePos = this.getBasePos(level, pos);
      int activeVentCount = this.countActiveVentsNearby(level, basePos);
      if (activeVentCount >= 1 && state.getValue(VENT_TYPE) == VentBlock.VentType.BASE) {
         Player player = level.getNearestPlayer((double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), 5.0, false);
         if (player != null) {
            player.displayClientMessage(Component.translatable("message.sulfur_vent.too_many_active").withStyle(ChatFormatting.RED), true);
         }
      }
   }
}
