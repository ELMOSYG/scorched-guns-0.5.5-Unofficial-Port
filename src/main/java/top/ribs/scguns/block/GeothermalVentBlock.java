package top.ribs.scguns.block;


import net.minecraft.core.registries.BuiltInRegistries;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import top.ribs.scguns.init.ModBlocks;

public class GeothermalVentBlock extends VentBlock implements SimpleWaterloggedBlock {
   private static final Logger LOGGER = LogManager.getLogger();
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   private static final VoxelShape GEOTHERMAL_SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 16.0, 15.0);

   public GeothermalVentBlock(Properties properties) {
      super(properties, ResourceLocation.fromNamespaceAndPath("scguns", "geothermal_vent"));
      this.registerDefaultState(
         (BlockState)((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(VENT_TYPE, VentBlock.VentType.BASE))
                  .setValue(WATERLOGGED, false))
               .setValue(ACTIVE, false))
            .setValue(VENT_POWER, 1)
      );
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder);
      builder.add(new Property[]{WATERLOGGED});
   }

   @Nullable
   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      Level level = context.getLevel();
      BlockPos pos = context.getClickedPos();
      FluidState fluidState = level.getFluidState(pos);
      boolean waterlogged = fluidState.getType() == Fluids.WATER;
      boolean isActive = this.isActive(level, pos);
      int ventPower = this.calculateVentPower(level, pos);
      return (BlockState)((BlockState)((BlockState)this.updateState(level.getBlockState(pos.below()), level.getBlockState(pos.above()))
               .setValue(WATERLOGGED, waterlogged))
            .setValue(ACTIVE, isActive))
         .setValue(VENT_POWER, ventPower);
   }

   @Override
   public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
      if ((Boolean)state.getValue(WATERLOGGED)) {
         level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
      }

      BlockState updatedState = super.updateShape(state, direction, neighborState, level, pos, neighborPos);
      return (BlockState)updatedState.setValue(WATERLOGGED, (Boolean)state.getValue(WATERLOGGED));
   }

   @Override
   public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
      super.onPlace(state, level, pos, oldState, isMoving);
      if ((Boolean)state.getValue(WATERLOGGED)) {
         level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
      }
   }

   @Override
   public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
      super.neighborChanged(state, level, pos, block, fromPos, isMoving);
      if ((Boolean)state.getValue(WATERLOGGED)) {
         level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
      }
   }

   @Override
   public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return GEOTHERMAL_SHAPE;
   }

   @Override
   public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
      if (!world.isClientSide
         && (Boolean)state.getValue(ACTIVE)
         && !this.hasVentCollectorAbove(world, pos)
         && this.config != null
         && this.config.getPlacement().isEnabled()
         && this.shouldPlaceBlock(random)
         && this.hasWaterAround(world, pos)) {
         this.placeLayerBlock(world, pos, random);
      }

      world.scheduleTick(pos, this, this.calculateNextTickInterval());
   }

   private boolean shouldPlaceBlock(RandomSource random) {
      return this.config == null ? false : random.nextFloat() < this.config.getPlacement().getPlacementChance();
   }

   private boolean hasWaterAround(LevelReader world, BlockPos pos) {
      for (Direction direction : Direction.values()) {
         if (direction != Direction.UP && direction != Direction.DOWN) {
            BlockPos adjacentPos = pos.relative(direction);
            if (world.getFluidState(adjacentPos).is(FluidTags.WATER)) {
               return true;
            }
         }
      }

      return false;
   }

   private void placeLayerBlock(ServerLevel world, BlockPos pos, RandomSource random) {
      if (this.config != null) {
         ResourceLocation blockToPlace = this.config.getPlacement().getBlockToPlace();
         if (blockToPlace != null) {
            Block layerBlock;
            if (blockToPlace.toString().equals("scguns:niter_layer")) {
               layerBlock = (Block)ModBlocks.NITER_LAYER.get();
            } else if (blockToPlace.toString().equals("scguns:sulfur_layer")) {
               layerBlock = (Block)ModBlocks.SULFUR_LAYER.get();
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
               if (this.canPlaceLayerBlock(world, randomPos)) {
                  BlockPos abovePos = randomPos.above();
                  BlockState currentState = world.getBlockState(abovePos);
                  boolean isWater = currentState.getFluidState().is(FluidTags.WATER);
                  if (currentState.isAir() || isWater) {
                     BlockState layerState = layerBlock.defaultBlockState();
                     if (layerState.hasProperty(NiterLayerBlock.LAYERS)) {
                        layerState = (BlockState)layerState.setValue(NiterLayerBlock.LAYERS, 1);
                     }

                     if (layerState.hasProperty(NiterLayerBlock.WATERLOGGED) && isWater) {
                        layerState = (BlockState)layerState.setValue(NiterLayerBlock.WATERLOGGED, true);
                     }

                     world.setBlock(abovePos, layerState, 3);
                  }
               }
            }
         }
      }
   }

   private boolean canPlaceLayerBlock(ServerLevel world, BlockPos pos) {
      BlockState state = world.getBlockState(pos);
      BlockState aboveState = world.getBlockState(pos.above());
      return state.isFaceSturdy(world, pos, Direction.UP)
         && (aboveState.isAir() || aboveState.getFluidState().is(FluidTags.WATER))
         && !(state.getBlock() instanceof GeothermalVentBlock)
         && !(aboveState.getBlock() instanceof GeothermalVentBlock);
   }

   public void animateTick(BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull RandomSource random) {
      if (this.isTopOrBaseWithoutTop(state, level, pos) && (Boolean)state.getValue(ACTIVE)) {
         BlockPos abovePos = pos.above();
         BlockState aboveState = level.getBlockState(abovePos);
         if (!(aboveState.getBlock() instanceof VentCollectorBlock)) {
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

               for (int i = 0; i < random.nextInt(2) + 2; i++) {
                  double offsetX = random.nextDouble() * 0.2 - 0.1;
                  double offsetY = 0.05 + random.nextDouble() * 0.05;
                  double offsetZ = random.nextDouble() * 0.2 - 0.1;
                  level.addParticle(
                     ParticleTypes.BUBBLE,
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
                     ParticleTypes.CAMPFIRE_COSY_SMOKE,
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
      if (baseBlock == null || !belowState.is(baseBlock)) {
         return false;
      } else if (this.config.getActivation().requiresWaterlogged()) {
         BlockState currentState = level.getBlockState(basePos);
         return currentState.hasProperty(WATERLOGGED) ? (Boolean)currentState.getValue(WATERLOGGED) : false;
      } else {
         return true;
      }
   }

   public FluidState getFluidState(BlockState state) {
      return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
   }
}
