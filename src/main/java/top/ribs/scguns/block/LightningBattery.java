package top.ribs.scguns.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.ribs.scguns.blockentity.LightningBatteryBlockEntity;

public class LightningBattery extends Block implements EntityBlock {
   public static final BooleanProperty CHARGED = BooleanProperty.create("charged");
   public static final EnumProperty<LightningBattery.ChargeLevel> CHARGE_LEVEL = EnumProperty.create("charge_level", LightningBattery.ChargeLevel.class);
   private static final int CHARGE_AMOUNT = 8000;

   public LightningBattery(Properties properties) {
      super(properties);
      this.registerDefaultState(
         (BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(CHARGED, Boolean.FALSE))
            .setValue(CHARGE_LEVEL, LightningBattery.ChargeLevel.NONE)
      );
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{CHARGED, CHARGE_LEVEL});
   }

   public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean isMoving) {
      super.onPlace(state, world, pos, oldState, isMoving);
      if (!world.isClientSide) {
         world.scheduleTick(pos, this, 1);
      }
   }

   public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
      if (!state.is(newState.getBlock())) {
         BlockEntity blockEntity = world.getBlockEntity(pos);
         if (blockEntity instanceof LightningBatteryBlockEntity) {
            ((LightningBatteryBlockEntity)blockEntity).drops();
         }

         super.onRemove(state, world, pos, newState, isMoving);
      }
   }

   public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
      super.neighborChanged(state, world, pos, block, fromPos, isMoving);
      if (world.isThundering() && this.isNearbyRodPowered(world, pos)) {
         this.chargeBattery(world, pos, state);
      }
   }

   private void chargeBattery(Level world, BlockPos pos, BlockState state) {
      if (world.getBlockEntity(pos) instanceof LightningBatteryBlockEntity battery) {
         int energyStored = battery.getEnergy();
         if (energyStored < battery.getMaxEnergy()) {
            int energyToAdd = Math.min(8000, battery.getMaxEnergy() - energyStored);
            battery.setEnergyStored(energyStored + energyToAdd);
            world.setBlock(
               pos, (BlockState)((BlockState)state.setValue(CHARGED, true)).setValue(CHARGE_LEVEL, calculateChargeLevel(energyStored + energyToAdd)), 3
            );
         }
      }
   }

   public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
      if ((Boolean)state.getValue(CHARGED)) {
         for (int i = 0; i < 5; i++) {
            double d0 = (double)pos.getX() + random.nextDouble();
            double d1 = (double)pos.getY() + random.nextDouble();
            double d2 = (double)pos.getZ() + random.nextDouble();
            world.addParticle(ParticleTypes.ELECTRIC_SPARK, d0, d1, d2, 0.0, 0.0, 0.0);
         }
      }
   }

   public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
      if (world.getBlockEntity(pos) instanceof LightningBatteryBlockEntity batteryEntity) {
         batteryEntity.tick();
      }

      world.scheduleTick(pos, this, 1);
   }

   @NotNull
   @Override
   public InteractionResult useWithoutItem(BlockState state, @NotNull Level world, @NotNull BlockPos pos, Player player, BlockHitResult hit) {

      if (!world.isClientSide) {
         BlockEntity blockEntity = world.getBlockEntity(pos);
         if (blockEntity instanceof LightningBatteryBlockEntity) {
            MenuProvider containerProvider = (LightningBatteryBlockEntity)blockEntity;
            ((ServerPlayer)player).openMenu(containerProvider, buf -> buf.writeBlockPos(pos));
         }

         return InteractionResult.CONSUME;
      } else {
         return InteractionResult.SUCCESS;
      }
   }

   public static LightningBattery.ChargeLevel calculateChargeLevel(int energyStored) {
      if (energyStored > 24000) {
         return LightningBattery.ChargeLevel.HIGH;
      } else if (energyStored > 12000) {
         return LightningBattery.ChargeLevel.MID;
      } else {
         return energyStored > 0 ? LightningBattery.ChargeLevel.LOW : LightningBattery.ChargeLevel.NONE;
      }
   }

   private boolean isNearbyRodPowered(Level world, BlockPos pos) {
      for (int y = 1; y <= 5; y++) {
         BlockPos abovePos = pos.above(y);
         BlockState state = world.getBlockState(abovePos);
         if ((state.getBlock() instanceof LightningRodBlock || state.getBlock() instanceof LightningRodConnectorBlock)
            && (Boolean)state.getValue(BlockStateProperties.POWERED)) {
            return true;
         }
      }

      return false;
   }

   @Nullable
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new LightningBatteryBlockEntity(pos, state);
   }

   public static enum ChargeLevel implements StringRepresentable {
      NONE("none"),
      LOW("low"),
      MID("mid"),
      HIGH("high");

      private final String name;

      private ChargeLevel(String name) {
         this.name = name;
      }

      public String getSerializedName() {
         return this.name;
      }

      @Override
      public String toString() {
         return this.getSerializedName();
      }
   }
}
