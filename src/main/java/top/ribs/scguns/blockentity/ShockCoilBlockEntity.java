package top.ribs.scguns.blockentity;




import top.ribs.scguns.util.ScEffects;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.ribs.scguns.block.HostileTurretTargetingBlock;
import top.ribs.scguns.block.PlayerTurretTargetingBlock;
import top.ribs.scguns.block.ShockCoilBlock;
import top.ribs.scguns.block.TurretTargetingBlock;
import top.ribs.scguns.config.ShockCoilConfig;
import top.ribs.scguns.init.ModBlockEntities;

public class ShockCoilBlockEntity extends BlockEntity {
   private final EnergyStorage energyStorage = new EnergyStorage(ShockCoilConfig.getMaxEnergy()) {
      public int receiveEnergy(int maxReceive, boolean simulate) {
         int received = super.receiveEnergy(maxReceive, simulate);
         if (!simulate && received > 0) {
            ShockCoilBlockEntity.this.setChanged();
            ShockCoilBlockEntity.this.updateBlockState();
         }

         return received;
      }

      public int extractEnergy(int maxExtract, boolean simulate) {
         int extracted = super.extractEnergy(maxExtract, simulate);
         if (!simulate && extracted > 0) {
            ShockCoilBlockEntity.this.setChanged();
            ShockCoilBlockEntity.this.updateBlockState();
         }

         return extracted;
      }

      public boolean canExtract() {
         return true;
      }

      public boolean canReceive() {
         return true;
      }
   };
   private final IEnergyStorage energyHandler = this.energyStorage;
   private int zapCooldown = 0;
   private boolean redstoneDisabled = false;
   private ShockCoilBlockEntity.TargetingMode targetingMode = ShockCoilBlockEntity.TargetingMode.HOSTILE;

   public ShockCoilBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)ModBlockEntities.SHOCK_COIL.get(), pos, state);
   }

   public void setRedstoneDisabled(boolean disabled) {
      this.redstoneDisabled = disabled;
      this.updateBlockState();
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, ShockCoilBlockEntity blockEntity) {
      if (level != null && !level.isClientSide) {
         blockEntity.updateTargetingMode();
         if (blockEntity.zapCooldown > 0) {
            blockEntity.zapCooldown--;
         } else if (blockEntity.redstoneDisabled) {
            blockEntity.updateBlockState();
         } else {
            int energyPerZap = ShockCoilConfig.getEnergyPerZap();
            if (blockEntity.energyStorage.getEnergyStored() < energyPerZap) {
               blockEntity.updateBlockState();
            } else {
               Predicate<LivingEntity> targetPredicate = blockEntity.getTargetPredicate();
               List<LivingEntity> nearbyTargets = level.getEntitiesOfClass(
                  LivingEntity.class,
                  new AABB(pos).inflate(ShockCoilConfig.getZapRange()),
                  entity -> entity.isAlive() && !entity.isRemoved() && targetPredicate.test(entity)
               );
               if (!nearbyTargets.isEmpty()) {
                  int targetsZapped = 0;
                  Vec3 coilCenter = Vec3.atCenterOf(pos);

                  for (LivingEntity target : nearbyTargets) {
                     if (targetsZapped >= ShockCoilConfig.getMaxTargetsPerZap() || blockEntity.energyStorage.getEnergyStored() < energyPerZap) {
                        break;
                     }

                     Vec3 targetPos = new Vec3(target.getX(), target.getY() + (double)target.getEyeHeight() * 0.5, target.getZ());
                     blockEntity.zapTarget(target, coilCenter, targetPos);
                     blockEntity.energyStorage.extractEnergy(energyPerZap, false);
                     targetsZapped++;
                  }

                  if (targetsZapped > 0) {
                     blockEntity.zapCooldown = ShockCoilConfig.getZapCooldown();
                     blockEntity.setChanged();
                  }
               }
            }
         }
      }
   }

   private void updateTargetingMode() {
      if (this.level != null) {
         boolean hasPlayerModule = false;
         boolean hasHostileModule = false;
         boolean hasBaseModule = false;

         for (Direction direction : Direction.values()) {
            BlockState adjacentState = this.level.getBlockState(this.worldPosition.relative(direction));
            if (adjacentState.getBlock() instanceof PlayerTurretTargetingBlock) {
               hasPlayerModule = true;
            } else if (adjacentState.getBlock() instanceof HostileTurretTargetingBlock) {
               hasHostileModule = true;
            } else if (adjacentState.getBlock() instanceof TurretTargetingBlock
               && !(adjacentState.getBlock() instanceof PlayerTurretTargetingBlock)
               && !(adjacentState.getBlock() instanceof HostileTurretTargetingBlock)) {
               hasBaseModule = true;
            }
         }

         ShockCoilBlockEntity.TargetingMode newMode;
         if (hasPlayerModule) {
            newMode = ShockCoilBlockEntity.TargetingMode.PLAYER;
         } else if (hasBaseModule) {
            newMode = ShockCoilBlockEntity.TargetingMode.ALL;
         } else {
            newMode = ShockCoilBlockEntity.TargetingMode.HOSTILE;
         }

         if (this.targetingMode != newMode) {
            this.targetingMode = newMode;
            this.setChanged();
         }
      }
   }

   private Predicate<LivingEntity> getTargetPredicate() {
      return switch (this.targetingMode) {
         case HOSTILE -> entity -> entity instanceof Monster;
         case PLAYER -> entity -> entity instanceof Player;
         case ALL -> entity -> true;
      };
   }

   private void zapTarget(LivingEntity target, Vec3 start, Vec3 end) {
      if (this.level instanceof ServerLevel serverLevel) {
         this.spawnLightningArc(serverLevel, start, end);
         String soundId = ShockCoilConfig.getZapSound();
         SoundEvent soundEvent = (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse(soundId));
         if (soundEvent != null) {
            serverLevel.playSound(null, this.worldPosition, soundEvent, SoundSource.BLOCKS, 0.8F, 1.0F);
         }

         target.hurt(this.level.damageSources().lightningBolt(), ShockCoilConfig.getBaseDamage());

         for (ShockCoilConfig.StatusEffect statusEffect : ShockCoilConfig.getStatusEffects()) {
            if (this.level.random.nextFloat() < statusEffect.getChance()) {
               target.addEffect(new MobEffectInstance(ScEffects.holder(statusEffect.getEffect()), statusEffect.getDuration(), statusEffect.getAmplifier()));
            }
         }
      }
   }

   private void spawnLightningArc(ServerLevel serverLevel, Vec3 start, Vec3 end) {
      Vec3 direction = end.subtract(start);
      double distance = direction.length();
      direction = direction.normalize();
      double stepSize = 0.15;

      for (double d = 0.0; d < distance; d += stepSize) {
         Vec3 particlePos = start.add(direction.scale(d));
         serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, particlePos.x, particlePos.y, particlePos.z, 1, 0.05, 0.05, 0.05, 0.0);
      }
   }

   private void updateBlockState() {
      if (this.level != null && !this.level.isClientSide) {
         BlockState state = this.level.getBlockState(this.worldPosition);
         boolean isPowered = !this.redstoneDisabled && this.energyStorage.getEnergyStored() >= ShockCoilConfig.getEnergyPerZap();
         this.level.setBlock(this.worldPosition, (BlockState)state.setValue(ShockCoilBlock.POWERED, isPowered), 3);
      }
   }

   @NotNull
   public <T> T getCapability(Object cap, @Nullable Direction side) {
      return cap == Capabilities.EnergyStorage.BLOCK ? ((T) this.energyHandler) : null;
   }

   protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.saveAdditional(tag, registries);
      tag.put("Energy", this.energyStorage.serializeNBT(registries));
      tag.putInt("ZapCooldown", this.zapCooldown);
      tag.putBoolean("RedstoneDisabled", this.redstoneDisabled);
      tag.putString("TargetingMode", this.targetingMode.name());
   }

   public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.loadAdditional(tag, registries);
      this.energyStorage.deserializeNBT(registries, tag.get("Energy"));
      this.zapCooldown = tag.getInt("ZapCooldown");
      this.redstoneDisabled = tag.getBoolean("RedstoneDisabled");
      if (tag.contains("TargetingMode")) {
         try {
            this.targetingMode = ShockCoilBlockEntity.TargetingMode.valueOf(tag.getString("TargetingMode"));
         } catch (IllegalArgumentException var3) {
            this.targetingMode = ShockCoilBlockEntity.TargetingMode.HOSTILE;
         }
      }
   }

   @Nullable
   public Packet<ClientGamePacketListener> getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @NotNull
   public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
      return this.saveWithoutMetadata(registries);
   }

   public void drops() {
      SimpleContainer inventory = new SimpleContainer(0);

      assert this.level != null;

      Containers.dropContents(this.level, this.worldPosition, inventory);
   }

   public int getEnergy() {
      return this.energyStorage.getEnergyStored();
   }

   public int getMaxEnergy() {
      return this.energyStorage.getMaxEnergyStored();
   }

   public ShockCoilBlockEntity.TargetingMode getTargetingMode() {
      return this.targetingMode;
   }

   public static enum TargetingMode {
      HOSTILE,
      PLAYER,
      ALL;

      private TargetingMode() {
      }
   }
}
