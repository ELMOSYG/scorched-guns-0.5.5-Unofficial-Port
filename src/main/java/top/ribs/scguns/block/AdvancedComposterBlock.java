package top.ribs.scguns.block;


import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import top.ribs.scguns.blockentity.AdvancedComposterBlockEntity;
import top.ribs.scguns.init.ModBlockEntities;
import top.ribs.scguns.init.ModParticleTypes;
import top.ribs.scguns.init.ModTags;

public class AdvancedComposterBlock extends BaseEntityBlock {
   public static final MapCodec<AdvancedComposterBlock> CODEC = simpleCodec(AdvancedComposterBlock::new);

   @Override
   protected MapCodec<? extends BaseEntityBlock> codec() {
      return CODEC;
   }

   public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 11);
   public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
   private static final VoxelShape INSIDE = Block.box(2.0, 4.0, 2.0, 14.0, 14.0, 14.0);
   private static final VoxelShape SHAPE = Shapes.or(
      Block.box(0.0, 0.0, 0.0, 16.0, 4.0, 16.0),
      new VoxelShape[]{
         Block.box(0.0, 4.0, 0.0, 2.0, 14.0, 16.0),
         Block.box(14.0, 4.0, 0.0, 16.0, 14.0, 16.0),
         Block.box(2.0, 4.0, 0.0, 14.0, 14.0, 2.0),
         Block.box(2.0, 4.0, 14.0, 14.0, 14.0, 16.0)
      }
   );

   public AdvancedComposterBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(LEVEL, 0)).setValue(FACING, Direction.NORTH));
   }

   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
      return INSIDE;
   }

   public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   public RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new AdvancedComposterBlockEntity(pos, state);
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{LEVEL, FACING});
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
      return createTickerHelper(blockEntityType, (BlockEntityType)ModBlockEntities.ADVANCED_COMPOSTER.get(), AdvancedComposterBlock::tick);
   }

   private static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T blockEntity) {
      if (blockEntity instanceof AdvancedComposterBlockEntity composter) {
         composter.tick(level, pos, state);
      }
   }

   public BlockState rotate(BlockState state, Rotation rotation) {
      return (BlockState)state.setValue(FACING, rotation.rotate((Direction)state.getValue(FACING)));
   }

   public BlockState mirror(BlockState state, Mirror mirror) {
      return state.rotate(mirror.getRotation((Direction)state.getValue(FACING)));
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return (BlockState)this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
   }

   public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
      if ((Integer)state.getValue(LEVEL) == 7) {
         level.scheduleTick(pos, this, 20);
      }
   }

   public BlockState addItem(Player player, BlockState state, Level level, BlockPos pos, ItemStack stack) {
      int currentLevel = (Integer)state.getValue(LEVEL);
      float chance = this.getCompostChance(stack);
      if ((currentLevel != 0 || chance > 0.0F) && level.random.nextDouble() < (double)chance) {
         int newLevel = Math.min(7, currentLevel + 1);
         BlockState newState = (BlockState)state.setValue(LEVEL, newLevel);
         level.setBlock(pos, newState, 3);
         if (newLevel == 7) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof AdvancedComposterBlockEntity) {
               ((AdvancedComposterBlockEntity)blockEntity).startComposting();
            }
         }

         this.playComposterEffects(level, pos, state);
         return newState;
      } else {
         return state;
      }
   }

   @Override
   public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {

      ItemStack heldItem = stack;
      if ((Integer)state.getValue(LEVEL) < 7 && this.isCompostable(heldItem)) {
         if (!level.isClientSide) {
            BlockState newState = this.addItem(player, state, level, pos, heldItem);
            if (state != newState) {
               player.awardStat(Stats.ITEM_USED.get(heldItem.getItem()));
               if (!player.getAbilities().instabuild) {
                  heldItem.shrink(1);
               }
            }
         }

         return ItemInteractionResult.sidedSuccess(level.isClientSide);
      } else if ((Integer)state.getValue(LEVEL) >= 8) {
         return !level.isClientSide ? this.extractProduce(player, state, level, pos) : ItemInteractionResult.sidedSuccess(true);
      } else {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      }
   }

   private ItemInteractionResult extractProduce(Player player, BlockState state, Level level, BlockPos pos) {
      if (level.getBlockEntity(pos) instanceof AdvancedComposterBlockEntity composter) {
         boolean extracted = composter.extractOneItem(player);
         if (extracted) {
            level.playSound(null, pos, SoundEvents.COMPOSTER_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.setBlock(pos, (BlockState)state.setValue(LEVEL, composter.getVisualLevel()), 3);
            return ItemInteractionResult.SUCCESS;
         }
      }

      return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
   }

   public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
      if (!state.is(newState.getBlock())) {
         BlockEntity blockEntity = world.getBlockEntity(pos);
         if (blockEntity instanceof AdvancedComposterBlockEntity) {
            ((AdvancedComposterBlockEntity)blockEntity).drops();
         }

         super.onRemove(state, world, pos, newState, isMoving);
      }
   }

   public boolean hasAnalogOutputSignal(BlockState state) {
      return true;
   }

   public int getAnalogOutputSignal(BlockState blockState, Level level, BlockPos pos) {
      return (Integer)blockState.getValue(LEVEL);
   }

   public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
      int currentLevel = (Integer)state.getValue(LEVEL);
      if (currentLevel > 0) {
         double d0 = (double)pos.getX() + 0.5;
         double d1 = (double)pos.getY() + 1.0;
         double d2 = (double)pos.getZ() + 0.5;
         int particleChance = currentLevel == 7 ? 10 : 5;
         if (random.nextInt(particleChance) == 0) {
            double offsetX = (random.nextDouble() - 0.5) * 0.5;
            double offsetZ = (random.nextDouble() - 0.5) * 0.5;
            double offsetY = random.nextDouble() * 0.1;
            level.addParticle((ParticleOptions)ModParticleTypes.SULFUR_DUST.get(), d0 + offsetX, d1 + offsetY, d2 + offsetZ, 0.0, 0.03, 0.0);
         }
      }
   }

   public void playComposterEffects(Level level, BlockPos pos, BlockState state) {
      if (!level.isClientSide) {
         level.levelEvent(1500, pos, state.getValue(LEVEL) > 0 ? 1 : 0);
      }
   }

   public boolean isCompostable(ItemStack stack) {
      return stack.is(ModTags.Items.WEAK_COMPOST) || stack.is(ModTags.Items.NORMAL_COMPOST) || stack.is(ModTags.Items.STRONG_COMPOST);
   }

   private float getCompostChance(ItemStack stack) {
      if (stack.is(ModTags.Items.WEAK_COMPOST)) {
         return 0.4F;
      } else if (stack.is(ModTags.Items.NORMAL_COMPOST)) {
         return 0.6F;
      } else {
         return stack.is(ModTags.Items.STRONG_COMPOST) ? 0.8F : 0.0F;
      }
   }
}
