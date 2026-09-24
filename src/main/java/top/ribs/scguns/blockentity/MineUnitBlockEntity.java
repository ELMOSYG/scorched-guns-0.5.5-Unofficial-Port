package top.ribs.scguns.blockentity;



import top.ribs.scguns.util.NbtHelper;
import net.minecraft.core.HolderLookup;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import top.ribs.scguns.entity.throwable.ThrowableBeaconGrenadeEntity;
import top.ribs.scguns.entity.throwable.ThrowableChokeBombEntity;
import top.ribs.scguns.entity.throwable.ThrowableGasGrenadeEntity;
import top.ribs.scguns.entity.throwable.ThrowableGrenadeEntity;
import top.ribs.scguns.entity.throwable.ThrowableHellfireBombEntity;
import top.ribs.scguns.entity.throwable.ThrowableMolotovCocktailEntity;
import top.ribs.scguns.entity.throwable.ThrowableNailBombEntity;
import top.ribs.scguns.entity.throwable.ThrowableStunGrenadeEntity;
import top.ribs.scguns.entity.throwable.ThrowableSwarmBombEntity;
import top.ribs.scguns.init.ModBlockEntities;
import top.ribs.scguns.init.ModTags;
import top.ribs.scguns.item.BeaconGrenadeItem;
import top.ribs.scguns.item.ChokeBombItem;
import top.ribs.scguns.item.GasGrenadeItem;
import top.ribs.scguns.item.GrenadeItem;
import top.ribs.scguns.item.HellfireBombItem;
import top.ribs.scguns.item.MolotovCocktailItem;
import top.ribs.scguns.item.NailBombItem;
import top.ribs.scguns.item.StunGrenadeItem;
import top.ribs.scguns.item.SwarmBombItem;

public class MineUnitBlockEntity extends BlockEntity {
   private ItemStack storedGrenade = ItemStack.EMPTY;
   private UUID placerUUID = null;
   private boolean primed = false;
   private static final double DETECTION_RADIUS = 1.25;

   public MineUnitBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)ModBlockEntities.MINE_UNIT.get(), pos, state);
   }

   public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T blockEntity) {
      if (blockEntity instanceof MineUnitBlockEntity mineUnit) {
         if (!level.isClientSide() && mineUnit.hasGrenade() && mineUnit.isPrimed()) {
            AABB detectionBox = new AABB(pos).inflate(1.25);
            List<Entity> nearbyEntities = level.getEntities(null, detectionBox);
            UUID placerUUID = mineUnit.getPlacerUUID();

            for (Entity entity : nearbyEntities) {
               if (!entity.getUUID().equals(placerUUID) && !entity.getType().is(ModTags.Entities.IGNORES_MINE_UNITS)) {
                  mineUnit.triggerGrenade(level, pos);
                  break;
               }
            }
         }
      }
   }

   private void triggerGrenade(Level level, BlockPos pos) {
      if (!this.storedGrenade.isEmpty()) {
         LivingEntity placer = null;
         if (this.placerUUID != null && level instanceof ServerLevel serverLevel && serverLevel.getEntity(this.placerUUID) instanceof LivingEntity living) {
            placer = living;
         }

         LivingEntity effectivePlacer = placer != null
            ? placer
            : new ItemEntity(level, (double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5, ItemStack.EMPTY).getControllingPassenger();
         double x = (double)pos.getX() + 0.5;
         double y = (double)pos.getY() + 0.5;
         double z = (double)pos.getZ() + 0.5;
         this.spawnTriggerEffects(level, x, y, z);
         Item grenadeItem = this.storedGrenade.getItem();
         if (grenadeItem instanceof StunGrenadeItem) {
            ThrowableStunGrenadeEntity grenade = new ThrowableStunGrenadeEntity(level, effectivePlacer, 2);
            grenade.moveTo(x, y, z);
            level.addFreshEntity(grenade);
         } else if (grenadeItem instanceof GasGrenadeItem) {
            ThrowableGasGrenadeEntity grenade = new ThrowableGasGrenadeEntity(level, effectivePlacer, 2, 6.0F);
            grenade.moveTo(x, y, z);
            grenade.setItem(this.storedGrenade.copy());
            level.addFreshEntity(grenade);
         } else if (grenadeItem instanceof MolotovCocktailItem) {
            ThrowableMolotovCocktailEntity grenade = new ThrowableMolotovCocktailEntity(level, effectivePlacer, 2);
            grenade.moveTo(x, y, z);
            level.addFreshEntity(grenade);
         } else if (grenadeItem instanceof HellfireBombItem) {
            ThrowableHellfireBombEntity grenade = new ThrowableHellfireBombEntity(level, effectivePlacer, 2);
            grenade.moveTo(x, y, z);
            level.addFreshEntity(grenade);
         } else if (grenadeItem instanceof ChokeBombItem) {
            ThrowableChokeBombEntity grenade = new ThrowableChokeBombEntity(level, effectivePlacer, 2, 4.0F);
            grenade.moveTo(x, y, z);
            grenade.setItem(this.storedGrenade.copy());
            level.addFreshEntity(grenade);
         } else if (grenadeItem instanceof BeaconGrenadeItem) {
            ThrowableBeaconGrenadeEntity grenade = new ThrowableBeaconGrenadeEntity(level, effectivePlacer, 2);
            grenade.moveTo(x, y, z);
            grenade.setItem(this.storedGrenade.copy());
            level.addFreshEntity(grenade);
         } else if (grenadeItem instanceof NailBombItem) {
            ThrowableNailBombEntity grenade = new ThrowableNailBombEntity(level, effectivePlacer, 2);
            grenade.moveTo(x, y, z);
            grenade.setItem(this.storedGrenade.copy());
            level.addFreshEntity(grenade);
         } else if (grenadeItem instanceof SwarmBombItem) {
            ThrowableSwarmBombEntity grenade = new ThrowableSwarmBombEntity(level, effectivePlacer, 2);
            grenade.moveTo(x, y, z);
            grenade.setItem(this.storedGrenade.copy());
            level.addFreshEntity(grenade);
         } else if (grenadeItem instanceof GrenadeItem) {
            ThrowableGrenadeEntity grenade = new ThrowableGrenadeEntity(level, effectivePlacer, 2);
            grenade.moveTo(x, y, z);
            grenade.setItem(this.storedGrenade.copy());
            level.addFreshEntity(grenade);
         }

         this.storedGrenade = ItemStack.EMPTY;
         this.setChanged();
         level.removeBlock(this.worldPosition, false);
      }
   }

   private void spawnTriggerEffects(Level level, double x, double y, double z) {
      if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
         level.playSound(null, x, y, z, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.8F, 1.5F);
         level.playSound(null, x, y, z, SoundEvents.TRIPWIRE_CLICK_ON, SoundSource.BLOCKS, 0.6F, 1.2F);

         for (int i = 0; i < 8; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 0.4;
            double offsetY = level.random.nextDouble() * 0.3;
            double offsetZ = (level.random.nextDouble() - 0.5) * 0.4;
            serverLevel.sendParticles(ParticleTypes.POOF, x + offsetX, y + offsetY, z + offsetZ, 1, 0.0, 0.05, 0.0, 0.02);
         }

         for (int i = 0; i < 5; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 0.3;
            double offsetY = level.random.nextDouble() * 0.2;
            double offsetZ = (level.random.nextDouble() - 0.5) * 0.3;
            serverLevel.sendParticles(ParticleTypes.SMOKE, x + offsetX, y + offsetY, z + offsetZ, 1, 0.0, 0.03, 0.0, 0.01);
         }
      }
   }

   public boolean hasGrenade() {
      return !this.storedGrenade.isEmpty();
   }

   public boolean isPrimed() {
      return this.primed;
   }

   public void setPrimed(boolean primed) {
      this.primed = primed;
      this.setChanged();
      if (this.level != null && !this.level.isClientSide) {
         this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
      }
   }

   public UUID getPlacerUUID() {
      return this.placerUUID;
   }

   public void setGrenade(ItemStack stack, LivingEntity placer) {
      this.storedGrenade = stack.copy();
      if (placer != null) {
         this.placerUUID = placer.getUUID();
      }

      this.setChanged();
      if (this.level != null && !this.level.isClientSide) {
         this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
      }
   }

   public void dropGrenade() {
      if (this.level != null && !this.storedGrenade.isEmpty()) {
         Containers.dropItemStack(
            this.level, (double)this.worldPosition.getX(), (double)this.worldPosition.getY(), (double)this.worldPosition.getZ(), this.storedGrenade
         );
         this.storedGrenade = ItemStack.EMPTY;
      }
   }

   protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.saveAdditional(tag, registries);
      if (!this.storedGrenade.isEmpty()) {
         tag.put("StoredGrenade", top.ribs.scguns.util.NbtHelper.tagFromItem(this.storedGrenade));
      }

      if (this.placerUUID != null) {
         tag.putUUID("PlacerUUID", this.placerUUID);
      }

      tag.putBoolean("Primed", this.primed);
   }

   public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.loadAdditional(tag, registries);
      if (tag.contains("StoredGrenade")) {
         this.storedGrenade = top.ribs.scguns.util.NbtHelper.itemFromTag(tag.getCompound("StoredGrenade"));
      }

      if (tag.hasUUID("PlacerUUID")) {
         this.placerUUID = tag.getUUID("PlacerUUID");
      }

      this.primed = tag.getBoolean("Primed");
   }

   public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
      return this.saveWithoutMetadata(registries);
   }

   @Nullable
   public Packet<ClientGamePacketListener> getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }
}
