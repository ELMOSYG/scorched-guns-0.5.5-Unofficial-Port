package top.ribs.scguns.block;


import net.minecraft.core.registries.BuiltInRegistries;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import top.ribs.scguns.common.Vent;
import top.ribs.scguns.common.VentManager;

public abstract class VentBlock extends Block {
   public static final EnumProperty<VentBlock.VentType> VENT_TYPE = EnumProperty.create("vent_type", VentBlock.VentType.class);
   public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
   public static final IntegerProperty VENT_POWER = IntegerProperty.create("vent_power", 1, 5);
   protected static final VoxelShape SHAPE_BASE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);
   protected static final VoxelShape SHAPE_MIDDLE_TOP = Block.box(1.0, 0.0, 1.0, 15.0, 16.0, 15.0);
   protected static final int BASE_TICK_INTERVAL = 100;
   protected final Random random = new Random();
   public final ResourceLocation ventId;
   public Vent config;

   public VentBlock(Properties properties, ResourceLocation ventId) {
      super(properties);
      this.ventId = ventId;
      this.config = VentManager.getVent(ventId);
      this.registerDefaultState(
         (BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(VENT_TYPE, VentBlock.VentType.BASE)).setValue(ACTIVE, false))
            .setValue(VENT_POWER, 1)
      );
   }

   public void reloadConfig() {
      this.config = VentManager.getVent(this.ventId);
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{VENT_TYPE, ACTIVE, VENT_POWER});
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      Level level = context.getLevel();
      BlockPos pos = context.getClickedPos();
      boolean isActive = this.isActive(level, pos);
      int ventPower = this.calculateVentPower(level, pos);
      return (BlockState)((BlockState)this.updateState(level.getBlockState(pos.below()), level.getBlockState(pos.above())).setValue(ACTIVE, isActive))
         .setValue(VENT_POWER, ventPower);
   }

   @NotNull
   public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
      if (level instanceof Level) {
         boolean isActive = this.isActive(level, pos);
         int ventPower = this.calculateVentPower((Level)level, pos);
         return (BlockState)((BlockState)this.updateState(level.getBlockState(pos.below()), level.getBlockState(pos.above())).setValue(ACTIVE, isActive))
            .setValue(VENT_POWER, ventPower);
      } else {
         return this.updateState(level.getBlockState(pos.below()), level.getBlockState(pos.above()));
      }
   }

   protected int calculateVentPower(LevelAccessor level, BlockPos pos) {
      if (this.config == null) {
         this.reloadConfig();
         if (this.config == null) {
            return 1;
         }
      }

      BlockPos basePos = this.getBasePos(level, pos);
      int power = 1;
      BlockPos checkPos = basePos.above();

      for (int maxPower = this.config.getPower().getMaxPower();
         level.getBlockState(checkPos).getBlock() instanceof VentBlock && power < maxPower;
         checkPos = checkPos.above()
      ) {
         power++;
      }

      return power;
   }

   protected void updateVentPower(Level level, BlockPos pos) {
      BlockPos basePos = this.getBasePos(level, pos);
      int power = this.calculateVentPower(level, basePos);
      BlockState baseState = level.getBlockState(basePos);
      level.setBlock(basePos, (BlockState)baseState.setValue(VENT_POWER, power), 3);

      for (BlockPos checkPos = basePos.above(); level.getBlockState(checkPos).getBlock() instanceof VentBlock; checkPos = checkPos.above()) {
         BlockState state = level.getBlockState(checkPos);
         level.setBlock(checkPos, (BlockState)state.setValue(VENT_POWER, power), 3);
      }
   }

   protected BlockState updateState(BlockState belowState, BlockState aboveState) {
      if (belowState.getBlock() instanceof VentBlock) {
         return aboveState.getBlock() instanceof VentBlock
            ? (BlockState)this.defaultBlockState().setValue(VENT_TYPE, VentBlock.VentType.MIDDLE)
            : (BlockState)this.defaultBlockState().setValue(VENT_TYPE, VentBlock.VentType.TOP);
      } else {
         return (BlockState)this.defaultBlockState().setValue(VENT_TYPE, VentBlock.VentType.BASE);
      }
   }

   public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return state.getValue(VENT_TYPE) == VentBlock.VentType.BASE ? SHAPE_BASE : SHAPE_MIDDLE_TOP;
   }

   public PushReaction getPistonPushReaction(BlockState state) {
      return PushReaction.DESTROY;
   }

   public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
      super.neighborChanged(state, level, pos, block, fromPos, isMoving);
      if (block instanceof PistonBaseBlock) {
         level.destroyBlock(pos, true);
      }

      boolean isActive = this.isActive(level, pos);
      level.setBlock(pos, (BlockState)state.setValue(ACTIVE, isActive), 3);
   }

   public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
      boolean isActive = this.isActive(level, pos);
      int ventPower = this.calculateVentPower(level, pos);
      level.setBlock(pos, (BlockState)((BlockState)state.setValue(ACTIVE, isActive)).setValue(VENT_POWER, ventPower), 3);
      level.scheduleTick(pos, this, this.calculateNextTickInterval());
      this.updateVentPower(level, pos);
   }

   public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
      super.onRemove(state, level, pos, newState, isMoving);
      if (!(newState.getBlock() instanceof VentBlock)) {
         BlockState stateBelow = level.getBlockState(pos.below());
         if (stateBelow.getBlock() instanceof VentBlock) {
            this.updateVentPower(level, pos.below());
         }
      }
   }

   public abstract void tick(BlockState var1, ServerLevel var2, BlockPos var3, RandomSource var4);

   protected int calculateNextTickInterval() {
      if (this.config == null) {
         this.reloadConfig();
         if (this.config == null) {
            return 100;
         }
      }

      return this.config.getPower().getBaseTickInterval() + this.random.nextInt(this.config.getPower().getTickWiggleRoom());
   }

   protected boolean hasVentCollectorAbove(LevelReader level, BlockPos pos) {
      BlockPos topPos = this.getTopPos(level, pos);
      return level.getBlockState(topPos.above()).getBlock() instanceof VentCollectorBlock;
   }

   protected BlockPos getTopPos(LevelReader level, BlockPos pos) {
      while (level.getBlockState(pos.above()).getBlock() instanceof VentBlock) {
         pos = pos.above();
      }

      return pos;
   }

   protected BlockPos getBasePos(LevelAccessor level, BlockPos pos) {
      while (level.getBlockState(pos.below()).getBlock() instanceof VentBlock && pos.getY() > 0) {
         pos = pos.below();
      }

      return pos;
   }

   protected boolean isVentAbove(Level level, BlockPos pos) {
      BlockPos abovePos = pos.above();
      return level.getBlockState(abovePos).getBlock() instanceof VentBlock;
   }

   protected abstract boolean isActive(LevelAccessor var1, BlockPos var2);

   protected boolean isTopOrBaseWithoutTop(BlockState state, Level level, BlockPos pos) {
      return state.getValue(VENT_TYPE) == VentBlock.VentType.TOP || state.getValue(VENT_TYPE) == VentBlock.VentType.BASE && !this.isVentAbove(level, pos);
   }

   protected void playAmbientSound(Level level, BlockPos pos, RandomSource random) {
      if (this.config != null && this.config.getParticles().showActive()) {
         ResourceLocation soundLoc = this.config.getParticles().getActiveSound();
         if (soundLoc != null) {
            SoundEvent sound = (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(soundLoc);
            if (sound != null) {
               if (random.nextInt(20) == 0) {
                  level.playLocalSound(
                     (double)pos.getX() + 0.5,
                     (double)pos.getY() + 0.5,
                     (double)pos.getZ() + 0.5,
                     sound,
                     SoundSource.BLOCKS,
                     0.5F + random.nextFloat(),
                     random.nextFloat() * 0.7F + 0.6F,
                     false
                  );
               }
            }
         }
      }
   }

   public ItemStack selectRandomOutput(RandomSource random) {
      if (this.config == null) {
         return ItemStack.EMPTY;
      } else {
         List<Vent.Production.OutputItem> outputs = this.config.getProduction().getOutputs();
         if (outputs.isEmpty()) {
            return ItemStack.EMPTY;
         } else {
            int totalWeight = 0;

            for (Vent.Production.OutputItem output : outputs) {
               totalWeight += output.getWeight();
            }

            if (totalWeight <= 0) {
               return ItemStack.EMPTY;
            } else {
               int randomValue = random.nextInt(totalWeight);
               int currentWeight = 0;

               for (Vent.Production.OutputItem output : outputs) {
                  currentWeight += output.getWeight();
                  if (randomValue < currentWeight) {
                     Item item = output.getItem();
                     if (item != null) {
                        return new ItemStack(item, 1);
                     }
                  }
               }

               return ItemStack.EMPTY;
            }
         }
      }
   }

   public boolean shouldProduce(RandomSource random) {
      return this.config == null ? false : random.nextFloat() < this.config.getProduction().getProductionChance();
   }

   protected boolean shouldShowParticles() {
      return this.config == null ? true : this.config.getParticles().showActive();
   }

   public static enum VentType implements StringRepresentable {
      BASE("base"),
      MIDDLE("middle"),
      TOP("top");

      private final String name;

      private VentType(String name) {
         this.name = name;
      }

      @NotNull
      public String getSerializedName() {
         return this.name;
      }

      @Override
      public String toString() {
         return this.name;
      }
   }
}
