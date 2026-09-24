package top.ribs.scguns.blockentity;




import net.minecraft.core.HolderLookup;
import top.ribs.scguns.util.PhysicsStructureHelper;
import top.ribs.scguns.util.NbtHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import top.ribs.scguns.Config;
import top.ribs.scguns.block.DamageModuleBlock;
import top.ribs.scguns.block.FireRateModuleBlock;
import top.ribs.scguns.block.HostileTurretTargetingBlock;
import top.ribs.scguns.block.PlayerTurretTargetingBlock;
import top.ribs.scguns.block.RangeModuleBlock;
import top.ribs.scguns.block.ShellCatcherModuleBlock;
import top.ribs.scguns.block.TurretTargetingBlock;
import top.ribs.scguns.common.Turret;
import top.ribs.scguns.common.TurretManager;
import top.ribs.scguns.entity.projectile.turret.TurretProjectileEntity;
import top.ribs.scguns.init.ModTags;
import top.ribs.scguns.item.EnemyLogItem;
import top.ribs.scguns.item.TeamLogItem;
import top.ribs.scguns.network.PacketHandler;
import top.ribs.scguns.network.message.S2CMessageMuzzleFlash;

public abstract class TurretBlockEntity extends BlockEntity implements MenuProvider {
   protected final ResourceLocation turretId;
   protected Turret config;
   protected double targetingRadius;
   protected int cooldown;
   public final ItemStackHandler itemHandler = new ItemStackHandler(10) {
      protected void onContentsChanged(int slot) {
         TurretBlockEntity.this.setChanged();
         if (TurretBlockEntity.this.level != null && !TurretBlockEntity.this.level.isClientSide()) {
            TurretBlockEntity.this.level.sendBlockUpdated(TurretBlockEntity.this.getBlockPos(), TurretBlockEntity.this.getBlockState(), TurretBlockEntity.this.getBlockState(), 3);
         }
      }
   };
   protected LivingEntity target;
   protected UUID ownerUUID;
   protected String ownerName;
   protected float yaw;
   protected double smoothedTargetX;
   protected double smoothedTargetZ;
   protected float pitch;
   protected double smoothedTargetY;
   protected float previousYaw;
   protected float previousPitch;
   public float recoilPitchOffset = 0.0F;
   protected boolean hasFireRateModule;
   protected boolean hasDamageModule;
   protected boolean hasRangeModule;
   protected boolean hasShellCatchingModule;
   public boolean disabled = false;
   public int disableCooldown = 0;
   private static final int DAMAGE_INCREASE = 2;
   private static final double RANGE_INCREASE = 8.0;
   private static final int IDLE_BEFORE_SCAN = 60;
   private static final float SCAN_ANGLE = 60.0F;
   private static final float SCAN_SPEED = 0.02F;
   private static final float SCAN_PITCH = 0.0F;
   private int idleTicks = 0;
   private boolean isScanning = false;
   private boolean scanningRight = true;
   private float scanStartYaw = 0.0F;
   private boolean returningToScanPitch = false;
   private IItemHandler lazyItemHandler = this.itemHandler;

   public TurretBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, ResourceLocation turretId) {
      super(type, pos, state);
      this.turretId = turretId;
      this.config = TurretManager.getTurret(turretId);
      if (this.config != null) {
         this.targetingRadius = this.config.getTargeting().getRange();
         this.cooldown = this.config.getCombat().getCooldown();
      } else {
         this.targetingRadius = 12.0;
         this.cooldown = 16;
      }
   }

   public void reloadConfig() {
      this.config = TurretManager.getTurret(this.turretId);
      if (this.config != null) {
         this.targetingRadius = this.config.getTargeting().getRange();
         this.cooldown = this.config.getCombat().getCooldown();
      }
   }

   public void tick() {
      if (this.level != null) {
         if (this.config == null) {
            this.reloadConfig();
            if (this.config == null) {
               return;
            }
         }

         this.hasFireRateModule = this.isAdjacentToFireRateModule(this.level, this.worldPosition);
         this.hasDamageModule = this.isAdjacentToDamageModule(this.level, this.worldPosition);
         this.hasRangeModule = this.isAdjacentToRangeModule(this.level, this.worldPosition);
         this.hasShellCatchingModule = this.isAdjacentToShellCatchingModule();
         int fireRateModifier = this.hasFireRateModule ? 2 : 1;
         int damageModifier = this.hasDamageModule ? 2 : 0;
         double rangeModifier = this.hasRangeModule ? 8.0 : 0.0;
         if (this.cooldown > 0) {
            this.cooldown -= fireRateModifier;
         }

         this.tickRecoil();
         if (this.disabled) {
            this.disableCooldown--;
            if (this.disableCooldown <= 0) {
               this.disabled = false;
               this.disableCooldown = 0;
            }

            this.resetToRestPosition();
            this.idleTicks = 0;
            this.isScanning = false;
         } else if (!this.isPowered(this.getBlockState())) {
            this.updateTargetRange(rangeModifier);
            if (!this.isTargetValid()) {
               this.target = null;
            }

            this.findTarget(this.level, this.worldPosition);
            if (this.target != null) {
               this.idleTicks = 0;
               this.isScanning = false;
               this.returningToScanPitch = false;
               this.updateYaw();
               this.updatePitch();
               if (this.cooldown <= 0 && this.isReadyToFire()) {
                  this.fireWeapon(damageModifier);
               }
            } else {
               this.idleTicks++;
               if (this.idleTicks < 60) {
                  this.previousYaw = this.yaw;
                  this.previousPitch = this.pitch;
               } else {
                  this.updateScanningBehavior();
               }
            }
         } else {
            this.resetToRestPosition();
            this.idleTicks = 0;
            this.isScanning = false;
            this.returningToScanPitch = false;
         }
      }
   }

   protected void updateScanningBehavior() {
      if (this.config != null) {
         this.previousYaw = this.yaw;
         this.previousPitch = this.pitch;
         if (!this.returningToScanPitch) {
            float pitchDiff = 0.0F - this.pitch;
            if (Math.abs(pitchDiff) > 0.5F) {
               this.pitch = this.pitch + pitchDiff * this.config.getTargeting().getRotationSpeed();
               return;
            }

            this.pitch = 0.0F;
            this.returningToScanPitch = true;
         }

         this.pitch = 0.0F;
         if (!this.isScanning) {
            this.isScanning = true;
            this.scanStartYaw = this.yaw;
            this.scanningRight = true;
         }

         float targetYaw;
         if (this.scanningRight) {
            targetYaw = this.scanStartYaw + 60.0F;
         } else {
            targetYaw = this.scanStartYaw - 60.0F;
         }

         float yawDiff = targetYaw - this.yaw;
         if (yawDiff > 180.0F) {
            yawDiff -= 360.0F;
         } else if (yawDiff < -180.0F) {
            yawDiff += 360.0F;
         }

         this.yaw += yawDiff * 0.02F;
         this.yaw %= 360.0F;
         if (this.yaw < 0.0F) {
            this.yaw += 360.0F;
         }

         float currentDiff = Math.abs(Mth.wrapDegrees(this.yaw - this.scanStartYaw));
         if (currentDiff >= 59.0F) {
            this.scanningRight = !this.scanningRight;
         }
      }
   }

   public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T t) {
      if (t instanceof TurretBlockEntity turret) {
         turret.tick();
      }
   }

   protected abstract boolean isPowered(BlockState var1);

   protected void fireWeapon(int damageModifier) {
      Turret.Ammunition.AmmoType ammoType = this.findAndConsumeAmmo();
      if (ammoType != null) {
         this.fire(ammoType, damageModifier);
         this.cooldown = this.config.getCombat().getCooldown();
      }
   }

   protected void fire(Turret.Ammunition.AmmoType ammoType, int damageModifier) {
      if (this.level != null && this.target != null && this.config != null) {
         float yaw = this.getYaw();
         float pitch = this.getPitch();
         // getMuzzlePosition works in this turret's frame; the projectile, its flash and its
         // sound all live in the world, so convert once here.
         Vec3 muzzleLocal = this.getMuzzlePosition(yaw, pitch);
         Vec3 convertedMuzzle = PhysicsStructureHelper.toWorld(this.level, this.worldPosition, muzzleLocal);
         Vec3 muzzlePos = convertedMuzzle != null ? convertedMuzzle : muzzleLocal;
         if (!this.level.isClientSide) {
            PacketHandler.getPlayChannel()
               .sendToTrackingChunk(() -> this.level.getChunkAt(BlockPos.containing(muzzlePos)), new S2CMessageMuzzleFlash(muzzlePos, yaw, pitch));
         }

         SoundEvent fireSound = null;
         ResourceLocation soundLoc = this.config.getCombat().getFireSound();
         if (soundLoc != null) {
            fireSound = (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(soundLoc);
         }

         if (fireSound != null) {
            this.level.playSound(null, muzzlePos.x, muzzlePos.y, muzzlePos.z, fireSound, SoundSource.BLOCKS, 0.7F, 0.7F);
         }

         Vec3 targetPos = new Vec3(this.target.getX(), this.target.getY() + (double)this.target.getEyeHeight() * 0.5, this.target.getZ());
         Vec3 direction = targetPos.subtract(muzzlePos).normalize();
         float inaccuracy = this.config.getCombat().getInaccuracy();
         if (inaccuracy > 0.0F) {
            direction = direction.add(
                  this.level.random.triangle(0.0, (double)inaccuracy),
                  this.level.random.triangle(0.0, (double)inaccuracy),
                  this.level.random.triangle(0.0, (double)inaccuracy)
               )
               .normalize();
         }

         int pelletCount = this.config.getCombat().getPelletCount();
         if (pelletCount > 1) {
            this.fireCluster(ammoType, muzzlePos, direction, damageModifier, pelletCount);
         } else {
            this.fireSingleProjectile(ammoType, muzzlePos, direction, damageModifier);
         }

         this.recoilPitchOffset = this.config.getCombat().getRecoilMax();
         this.handleCasingEjection(ammoType);
      }
   }

   protected void fireSingleProjectile(Turret.Ammunition.AmmoType ammoType, Vec3 muzzlePos, Vec3 direction, int damageModifier) {
      TurretProjectileEntity projectile = this.createProjectile();
      projectile.setPos(muzzlePos.x, muzzlePos.y, muzzlePos.z);
      double speed = this.config.getCombat().getProjectileSpeed();
      projectile.shoot(direction.x, direction.y, direction.z, (float)speed, 0.0F);
      double finalDamage = this.getDamageForAmmoType(ammoType) + (double)damageModifier;
      projectile.setBaseDamage(finalDamage);
      projectile.setArmorPenetration(ammoType.getArmorPenetration());
      String bulletType = ammoType.getBulletType().toString();
      if (bulletType.equals("scguns:bear_pack_shell")) {
         projectile.setMobPenetration(1);
      } else if (bulletType.equals("scguns:gibbs_round")) {
         projectile.setGibbsRound(true);
      } else if (bulletType.equals("scguns:shatter_round")) {
         projectile.setShatterRound(true);
      }

      assert this.level != null;

      this.level.addFreshEntity(projectile);
   }

   protected void fireCluster(Turret.Ammunition.AmmoType ammoType, Vec3 muzzlePos, Vec3 baseDirection, int damageModifier, int pelletCount) {
      double baseDamage = this.getDamageForAmmoType(ammoType);
      double finalDamage = baseDamage + (double)damageModifier;
      double pelletDamage = finalDamage / (double)pelletCount;
      float spreadAngle = this.config.getCombat().getSpreadAngle();
      String bulletType = ammoType.getBulletType().toString();
      boolean isBearPackShell = bulletType.equals("scguns:bear_pack_shell");
      boolean isGibbsRound = bulletType.equals("scguns:gibbs_round");
      boolean isShatterRound = bulletType.equals("scguns:shatter_round");

      for (int i = 0; i < pelletCount; i++) {
         Vec3 spreadDirection = this.applySpread(baseDirection, spreadAngle);
         TurretProjectileEntity projectile = this.createProjectile();
         projectile.setPos(muzzlePos.x, muzzlePos.y, muzzlePos.z);
         double speed = this.config.getCombat().getProjectileSpeed();
         projectile.shoot(spreadDirection.x, spreadDirection.y, spreadDirection.z, (float)speed, 0.0F);
         projectile.setBaseDamage(pelletDamage);
         projectile.setArmorPenetration(ammoType.getArmorPenetration());
         if (isBearPackShell) {
            projectile.setMobPenetration(1);
         } else if (isGibbsRound) {
            projectile.setGibbsRound(true);
         } else if (isShatterRound) {
            projectile.setShatterRound(true);
         }

         assert this.level != null;

         this.level.addFreshEntity(projectile);
      }
   }

   protected Vec3 applySpread(Vec3 baseDirection, float spreadAngle) {
      assert this.level != null;

      float angleX = (float)(this.level.random.nextGaussian() * (double)spreadAngle);
      float angleY = (float)(this.level.random.nextGaussian() * (double)spreadAngle);
      double yawRad = Math.toRadians((double)angleX);
      double pitchRad = Math.toRadians((double)angleY);
      double x = baseDirection.x;
      double y = baseDirection.y;
      double z = baseDirection.z;
      double tempX = x * Math.cos(yawRad) - z * Math.sin(yawRad);
      double tempZ = x * Math.sin(yawRad) + z * Math.cos(yawRad);
      double tempY = y * Math.cos(pitchRad) - tempZ * Math.sin(pitchRad);
      tempZ = y * Math.sin(pitchRad) + tempZ * Math.cos(pitchRad);
      return new Vec3(tempX, tempY, tempZ).normalize();
   }

   protected TurretProjectileEntity createProjectile() {
      return new TurretProjectileEntity(this.level);
   }

   protected double getDamageForAmmoType(Turret.Ammunition.AmmoType ammoType) {
      double baseDamage = ammoType.getDamage();
      return baseDamage * (Double)Config.COMMON.gameplay.globalTurretDamageMultiplier.get();
   }

   protected void handleCasingEjection(Turret.Ammunition.AmmoType ammoType) {
      if (this.hasShellCatchingModule) {
         boolean inserted = this.tryInsertIntoShellCatcher(ammoType);
         if (!inserted) {
            this.spawnCasing(ammoType);
         }
      } else {
         float ejectChance = this.config.getAmmunition().getCasingEjectChance();

         assert this.level != null;

         if (this.level.random.nextFloat() < ejectChance) {
            this.spawnCasing(ammoType);
         }
      }
   }

   protected void spawnCasing(Turret.Ammunition.AmmoType ammoType) {
      ResourceLocation casingType = ammoType.getCasingType();
      if (casingType != null) {
         ItemStack casingStack = new ItemStack(Objects.requireNonNull((Item)BuiltInRegistries.ITEM.get(casingType)));

         assert this.level != null;

         ItemEntity casingEntity = new ItemEntity(
            this.level,
            (double)this.worldPosition.getX() + 0.5,
            (double)this.worldPosition.getY() + 1.0,
            (double)this.worldPosition.getZ() + 0.5,
            casingStack
         );
         double ejectSpeed = 0.1;
         double ejectX = (double)Direction.NORTH.getStepX() * ejectSpeed;
         double ejectY = 0.15;
         double ejectZ = (double)Direction.NORTH.getStepZ() * ejectSpeed;
         casingEntity.setDeltaMovement(ejectX, ejectY, ejectZ);
         this.level.addFreshEntity(casingEntity);
      }
   }

   protected boolean tryInsertIntoShellCatcher(Turret.Ammunition.AmmoType ammoType) {
      ResourceLocation casingType = ammoType.getCasingType();
      if (casingType == null) {
         return false;
      } else {
         for (Direction direction : Direction.values()) {
            BlockPos neighborPos = this.worldPosition.relative(direction);

            assert this.level != null;

            if (this.level.getBlockEntity(neighborPos) instanceof ShellCatcherModuleBlockEntity shellCatcher) {
               ItemStack casingStack = new ItemStack(Objects.requireNonNull((Item)BuiltInRegistries.ITEM.get(casingType)));

               for (int i = 0; i < shellCatcher.getContainerSize(); i++) {
                  ItemStack existingStack = shellCatcher.getItemStackHandler().getStackInSlot(i);
                  if (existingStack.isEmpty()) {
                     shellCatcher.getItemStackHandler().setStackInSlot(i, casingStack);
                     return true;
                  }

                  if (ItemStack.isSameItemSameComponents(existingStack, casingStack) && existingStack.getCount() < existingStack.getMaxStackSize()) {
                     existingStack.grow(1);
                     shellCatcher.getItemStackHandler().setStackInSlot(i, existingStack);
                     return true;
                  }
               }
            }
         }

         return false;
      }
   }

   protected Vec3 getMuzzlePosition(float yaw, float pitch) {
      double muzzleLength = this.config.getDisplay().getMuzzleLength();
      double muzzleOffsetY = this.config.getDisplay().getMuzzleOffsetY();
      double yawRad = Math.toRadians((double)yaw);
      double pitchRad = Math.toRadians((double)pitch);
      double muzzleX = -Math.sin(yawRad) * Math.cos(pitchRad) * muzzleLength;
      double muzzleY = Math.sin(pitchRad) * muzzleLength + muzzleOffsetY;
      double muzzleZ = -Math.cos(yawRad) * Math.cos(pitchRad) * muzzleLength;
      return new Vec3(
         (double)this.worldPosition.getX() + 0.5 + muzzleX, (double)this.worldPosition.getY() + muzzleY, (double)this.worldPosition.getZ() + 0.5 + muzzleZ
      );
   }

   protected void updateTargetRange(double rangeModifier) {
      this.targetingRadius = this.config.getTargeting().getRange() + rangeModifier;
   }

   public boolean isReadyToFire() {
      if (this.target != null && this.config != null) {
         double dx = this.smoothedTargetX - ((double)this.worldPosition.getX() + 0.5);
         double dy = this.smoothedTargetY - ((double)this.worldPosition.getY() + 1.0);
         double dz = this.smoothedTargetZ - ((double)this.worldPosition.getZ() + 0.5);
         double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
         float targetYaw = (float)(Math.atan2(dx, dz) * (180.0 / Math.PI)) + 180.0F;
         targetYaw = (targetYaw + 360.0F) % 360.0F;
         float targetPitch = (float)(Math.atan2(dy, horizontalDistance) * (180.0 / Math.PI));
         targetPitch = Mth.clamp(targetPitch, this.config.getTargeting().getMinPitch(), this.config.getTargeting().getMaxPitch());
         float yawDifference = Math.abs(targetYaw - this.yaw);
         if (yawDifference > 180.0F) {
            yawDifference = 360.0F - yawDifference;
         }

         float pitchDifference = Math.abs(targetPitch - this.pitch);
         double distanceSquared = dx * dx + dy * dy + dz * dz;
         double minDist = this.config.getTargeting().getMinFiringDistance();
         return distanceSquared < minDist * minDist ? false : yawDifference < 2.0F && pitchDifference < 2.0F;
      } else {
         return false;
      }
   }

   public void tickRecoil() {
      if (this.config != null) {
         if (this.recoilPitchOffset > 0.0F) {
            this.recoilPitchOffset = this.recoilPitchOffset - this.config.getCombat().getRecoilSpeed();
            if (this.recoilPitchOffset < 0.0F) {
               this.recoilPitchOffset = 0.0F;
            }
         }
      }
   }

   public void resetToRestPosition() {
      if (this.config != null) {
         this.target = null;
         float restingYaw = this.config.getBehavior().getRestingYaw();
         float restingPitch = this.config.getBehavior().getRestingPitch();
         this.previousYaw = this.yaw;
         this.previousPitch = this.pitch;
         float yawDifference = restingYaw - this.yaw;
         if (yawDifference > 180.0F) {
            yawDifference -= 360.0F;
         } else if (yawDifference < -180.0F) {
            yawDifference += 360.0F;
         }

         float rotSpeed = this.config.getTargeting().getRotationSpeed();
         this.yaw += yawDifference * rotSpeed;
         this.yaw %= 360.0F;
         if (this.yaw < 0.0F) {
            this.yaw += 360.0F;
         }

         float pitchDifference = restingPitch - this.pitch;
         this.pitch += pitchDifference * rotSpeed;
         this.smoothedTargetX = 0.0;
         this.smoothedTargetY = 0.0;
         this.smoothedTargetZ = 0.0;
      }
   }

   public void onHitByLightningProjectile() {
      if (this.config != null) {
         this.disabled = true;
         this.disableCooldown = this.config.getBehavior().getDisableTime();
         this.resetToRestPosition();
         this.setChanged();
         if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
            this.spawnDisableParticles();
         }
      }
   }

   protected void spawnDisableParticles() {
      if (this.level instanceof ServerLevel serverLevel) {
         Vec3 centre = PhysicsStructureHelper.toWorld(this.level, this.worldPosition,
               new Vec3((double)this.worldPosition.getX() + 0.5, (double)this.worldPosition.getY() + 1.0, (double)this.worldPosition.getZ() + 0.5));
         if (centre == null) {
            centre = new Vec3((double)this.worldPosition.getX() + 0.5, (double)this.worldPosition.getY() + 1.0, (double)this.worldPosition.getZ() + 0.5);
         }

         double x = centre.x;
         double y = centre.y;
         double z = centre.z;

         for (int i = 0; i < 20; i++) {
            double offsetX = this.level.random.nextDouble() * 0.5 - 0.25;
            double offsetY = this.level.random.nextDouble() * 0.5;
            double offsetZ = this.level.random.nextDouble() * 0.5 - 0.25;
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, x + offsetX, y + offsetY, z + offsetZ, 1, 0.0, 0.0, 0.0, 0.05);
         }

         serverLevel.playSound(null, x, y, z, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
      }
   }

   @Nullable
   protected Turret.Ammunition.AmmoType findAndConsumeAmmo() {
      if (this.config == null) {
         return null;
      } else {
         for (int i = 0; i < this.itemHandler.getSlots(); i++) {
            ItemStack stack = this.itemHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
               for (Turret.Ammunition.AmmoType ammoType : this.config.getAmmunition().getAcceptedAmmo()) {
                  if (stack.getItem() == ammoType.getItem()) {
                     this.consumeAmmo(i);
                     return ammoType;
                  }
               }
            }
         }

         return null;
      }
   }

   protected void consumeAmmo(int slot) {
      ItemStack stack = this.itemHandler.getStackInSlot(slot);
      stack.shrink(1);
      if (stack.isEmpty()) {
         this.itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
      }
   }

   protected void findTarget(Level level, BlockPos pos) {
      if (this.config != null) {
         this.target = null;
         boolean hasTargetingModule = false;
         boolean isPlayerTargetingModule = false;
         boolean isHostileTargetingModule = false;

         for (Direction direction : Direction.values()) {
            BlockState blockState = level.getBlockState(pos.relative(direction));
            if (blockState.getBlock() instanceof TurretTargetingBlock) {
               hasTargetingModule = true;
               if (blockState.getBlock() instanceof PlayerTurretTargetingBlock) {
                  isPlayerTargetingModule = true;
               } else if (blockState.getBlock() instanceof HostileTurretTargetingBlock) {
                  isHostileTargetingModule = true;
               }
               break;
            }
         }

         if (hasTargetingModule) {
            ItemStack logStack = this.itemHandler.getStackInSlot(9);
            boolean hasTeamLog = logStack.getItem() instanceof TeamLogItem && !(logStack.getItem() instanceof EnemyLogItem);
            boolean hasEnemyLog = logStack.getItem() instanceof EnemyLogItem;
            List<UUID> loggedEntityUUIDs = new ArrayList<>();
            List<String> blacklistedEntityTypes = new ArrayList<>();
            List<UUID> whitelistedEntityUUIDs = new ArrayList<>();
            List<String> whitelistedEntityTypes = new ArrayList<>();
            if (hasTeamLog || hasEnemyLog) {
               CompoundTag tag = NbtHelper.getTag(logStack);
               if (tag != null) {
                  if (hasTeamLog) {
                     if (tag.contains("Entities", 9)) {
                        ListTag listTag = tag.getList("Entities", 10);

                        for (int i = 0; i < listTag.size(); i++) {
                           CompoundTag entityTag = listTag.getCompound(i);
                           loggedEntityUUIDs.add(entityTag.getUUID("UUID"));
                        }
                     }

                     if (tag.contains("Blacklist", 9)) {
                        ListTag blacklistTag = tag.getList("Blacklist", 8);

                        for (int i = 0; i < blacklistTag.size(); i++) {
                           blacklistedEntityTypes.add(blacklistTag.getString(i));
                        }
                     }
                  } else {
                     if (tag.contains("Whitelist", 9)) {
                        ListTag listTag = tag.getList("Whitelist", 10);

                        for (int i = 0; i < listTag.size(); i++) {
                           CompoundTag entityTag = listTag.getCompound(i);
                           whitelistedEntityUUIDs.add(entityTag.getUUID("UUID"));
                        }
                     }

                     if (tag.contains("WhitelistEntityTypes", 9)) {
                        ListTag whitelistTag = tag.getList("WhitelistEntityTypes", 8);

                        for (int i = 0; i < whitelistTag.size(); i++) {
                           whitelistedEntityTypes.add(whitelistTag.getString(i));
                        }
                     }
                  }
               }
            }

            Vec3 turretPos = new Vec3((double)this.worldPosition.getX() + 0.5, (double)this.worldPosition.getY() + 1.0, (double)this.worldPosition.getZ() + 0.5);
            // Line-of-sight raycasts happen in world space, while the distance comparisons
            // below stay in this turret's own frame (the search box is built from blockPos).
            Vec3 convertedTurretPos = PhysicsStructureHelper.toWorld(level, this.worldPosition, turretPos);
            final Vec3 turretPosWorld = convertedTurretPos != null ? convertedTurretPos : turretPos;
            double verticalSearchRange = this.config.getTargeting().getVerticalRange();
            AABB searchBox = new AABB(pos).inflate(this.targetingRadius, verticalSearchRange, this.targetingRadius);
            boolean finalIsPlayerTargetingModule = isPlayerTargetingModule;
            boolean finalIsHostileTargetingModule = isHostileTargetingModule;
            List<LivingEntity> potentialTargets = level.getEntitiesOfClass(
               LivingEntity.class,
               searchBox,
               entity -> entity != null
                     && entity.isAlive()
                     && !this.isOwner(entity)
                     && (
                        !hasTeamLog && !hasEnemyLog
                           || hasTeamLog
                              && !loggedEntityUUIDs.contains(entity.getUUID())
                              && !blacklistedEntityTypes.contains(EntityType.getKey(entity.getType()).toString())
                           || hasEnemyLog
                              && (
                                 whitelistedEntityUUIDs.contains(entity.getUUID())
                                    || whitelistedEntityTypes.contains(EntityType.getKey(entity.getType()).toString())
                              )
                     )
                     && !(entity instanceof EnderMan)
                     && (!entity.isInvisible() || this.hasRangeModule)
                     && (!finalIsPlayerTargetingModule || entity instanceof Player && !((Player)entity).isCreative())
                     && (
                        !finalIsHostileTargetingModule
                           || entity.getType().getCategory() == MobCategory.MONSTER
                           || entity.getType().is(ModTags.Entities.TURRET_ENEMY_WHITELIST)
                     )
                     && !entity.getType().is(ModTags.Entities.TURRET_BLACKLIST)
            );
            if (!potentialTargets.isEmpty()) {
               if (this.config.getTargeting().requiresLineOfSight()) {
                  this.target = potentialTargets.stream()
                     .filter(entity -> this.hasLineOfSight(level, turretPosWorld, entity))
                     .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(turretPos)))
                     .orElse(null);
               } else {
                  this.target = potentialTargets.stream().min(Comparator.comparingDouble(entity -> entity.distanceToSqr(turretPos))).orElse(null);
               }

               if (this.target != null) {
                  int predMult = this.config.getTargeting().getPredictionMultiplier();
                  double predictedX = this.target.getX() + this.target.getDeltaMovement().x * (double)predMult;
                  double predictedY = this.target.getY() + (double)(this.target.getBbHeight() / 2.0F);
                  double predictedZ = this.target.getZ() + this.target.getDeltaMovement().z * (double)predMult;
                  // Entities report world coordinates; inside a physics structure this turret's
                  // block position is in that structure's own frame. Store the aim point in the
                  // turret's frame so every later yaw/pitch/distance calculation stays in one
                  // frame - mixing the two is what pinned a contraption-mounted turret's aim.
                  Vec3 predictedLocal = PhysicsStructureHelper.toLocal(level, this.worldPosition, new Vec3(predictedX, predictedY, predictedZ));
                  if (predictedLocal != null) {
                     predictedX = predictedLocal.x;
                     predictedY = predictedLocal.y;
                     predictedZ = predictedLocal.z;
                  }

                  float smoothing = this.config.getTargeting().getPositionSmoothing();
                  double resultX = lerp(this.smoothedTargetX, predictedX, (double)smoothing);
                  double resultY = lerp(this.smoothedTargetY, predictedY, (double)smoothing);
                  double resultZ = lerp(this.smoothedTargetZ, predictedZ, (double)smoothing);
                  this.smoothedTargetX = resultX;
                  this.smoothedTargetY = resultY;
                  this.smoothedTargetZ = resultZ;
               }
            }
         }
      }
   }

   protected boolean hasLineOfSight(Level level, Vec3 turretPos, LivingEntity target) {
      Vec3 targetPos = target.getEyePosition();
      Vec3 toTarget = targetPos.subtract(turretPos);
      double distance = toTarget.length();
      Vec3 rayVector = toTarget.normalize().scale(distance);
      Vec3 adjustedTurretPos = turretPos.add(0.0, 0.5, 0.0);
      ClipContext clipContext = new ClipContext(adjustedTurretPos, adjustedTurretPos.add(rayVector), Block.COLLIDER, Fluid.NONE, net.minecraft.world.phys.shapes.CollisionContext.empty());
      BlockHitResult hitResult = level.clip(clipContext);
      return hitResult.getType() == Type.MISS;
   }

   protected boolean isTargetValid() {
      if (this.target != null && this.target.isAlive() && !this.target.isRemoved()) {
         ChunkPos targetChunkPos = new ChunkPos(this.target.blockPosition());

         assert this.level != null;

         if (!this.level.hasChunk(targetChunkPos.x, targetChunkPos.z)) {
            return false;
         } else {
            double distanceSquared = this.target
               .distanceToSqr((double)this.worldPosition.getX() + 0.5, (double)this.worldPosition.getY() + 0.5, (double)this.worldPosition.getZ() + 0.5);
            return distanceSquared <= this.targetingRadius * this.targetingRadius;
         }
      } else {
         return false;
      }
   }

   protected static double lerp(double a, double b, double t) {
      return a + t * (b - a);
   }

   protected void updateYaw() {
      if (this.config != null) {
         this.previousYaw = this.yaw;
         if (this.smoothedTargetX != 0.0 || this.smoothedTargetZ != 0.0) {
            double dx = this.smoothedTargetX - ((double)this.worldPosition.getX() + 0.5);
            double dz = this.smoothedTargetZ - ((double)this.worldPosition.getZ() + 0.5);
            float targetYaw = (float)(Math.atan2(dx, dz) * (180.0 / Math.PI)) + 180.0F;
            targetYaw = (targetYaw + 360.0F) % 360.0F;
            this.yaw = (this.yaw + 360.0F) % 360.0F;
            float yawDifference = targetYaw - this.yaw;
            if (yawDifference > 180.0F) {
               yawDifference -= 360.0F;
            } else if (yawDifference < -180.0F) {
               yawDifference += 360.0F;
            }

            float rotSpeed = this.config.getTargeting().getRotationSpeed();
            this.yaw += yawDifference * rotSpeed;
            this.yaw %= 360.0F;
            if (this.yaw < 0.0F) {
               this.yaw += 360.0F;
            }
         }
      }
   }

   protected void updatePitch() {
      if (this.config != null) {
         this.previousPitch = this.pitch;
         if (this.smoothedTargetY != 0.0) {
            double dx = this.smoothedTargetX - ((double)this.worldPosition.getX() + 0.5);
            float pitchDifference = this.getPitchDifference(dx);
            float rotSpeed = this.config.getTargeting().getRotationSpeed();
            this.pitch += pitchDifference * rotSpeed;
         }
      }
   }

   private float getPitchDifference(double dx) {
      double dy = this.smoothedTargetY - ((double)this.worldPosition.getY() + 1.0);
      double dz = this.smoothedTargetZ - ((double)this.worldPosition.getZ() + 0.5);
      double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
      float targetPitch = (float)(Math.atan2(dy, horizontalDistance) * (180.0 / Math.PI));
      targetPitch = Mth.clamp(targetPitch, this.config.getTargeting().getMinPitch(), this.config.getTargeting().getMaxPitch());
      return targetPitch - this.pitch;
   }

   protected boolean isAdjacentToFireRateModule(BlockGetter world, BlockPos pos) {
      for (Direction direction : Direction.values()) {
         BlockPos neighborPos = pos.relative(direction);
         if (world.getBlockState(neighborPos).getBlock() instanceof FireRateModuleBlock) {
            return true;
         }
      }

      return false;
   }

   protected boolean isAdjacentToDamageModule(BlockGetter world, BlockPos pos) {
      for (Direction direction : Direction.values()) {
         BlockPos neighborPos = pos.relative(direction);
         if (world.getBlockState(neighborPos).getBlock() instanceof DamageModuleBlock) {
            return true;
         }
      }

      return false;
   }

   protected boolean isAdjacentToRangeModule(BlockGetter world, BlockPos pos) {
      for (Direction direction : Direction.values()) {
         BlockPos neighborPos = pos.relative(direction);
         if (world.getBlockState(neighborPos).getBlock() instanceof RangeModuleBlock) {
            return true;
         }
      }

      return false;
   }

   protected boolean isAdjacentToShellCatchingModule() {
      for (Direction direction : Direction.values()) {
         BlockPos neighborPos = this.worldPosition.relative(direction);

         assert this.level != null;

         if (this.level.getBlockState(neighborPos).getBlock() instanceof ShellCatcherModuleBlock) {
            return true;
         }
      }

      return false;
   }

   protected boolean isOwner(LivingEntity entity) {
      return entity.getUUID().equals(this.ownerUUID);
   }

   public float getPreviousYaw() {
      return this.previousYaw;
   }

   public float getPreviousPitch() {
      return this.previousPitch;
   }

   public float getYaw() {
      return this.yaw;
   }

   public float getPitch() {
      return this.pitch;
   }

   public float getRecoilPitchOffset() {
      return this.recoilPitchOffset;
   }

   public void setOwner(ServerPlayer player) {
      this.ownerUUID = player.getUUID();
      this.ownerName = player.getName().getString();
   }

   public String getOwnerName() {
      return this.ownerName;
   }

   public void onLoad() {
      super.onLoad();
      this.lazyItemHandler = this.itemHandler;
   }

   public void drops() {
      SimpleContainer inventory = new SimpleContainer(this.itemHandler.getSlots());

      for (int i = 0; i < this.itemHandler.getSlots(); i++) {
         inventory.setItem(i, this.itemHandler.getStackInSlot(i));
      }

      assert this.level != null;

      Containers.dropContents(this.level, this.worldPosition, inventory);
   }

   protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.saveAdditional(tag, registries);
      tag.put("Inventory", this.itemHandler.serializeNBT(registries));
      tag.putFloat("Yaw", this.yaw);
      tag.putFloat("Pitch", this.pitch);
      tag.putBoolean("Disabled", this.disabled);
      tag.putInt("DisableCooldown", this.disableCooldown);
      tag.putInt("IdleTicks", this.idleTicks);
      tag.putBoolean("IsScanning", this.isScanning);
      tag.putBoolean("ScanningRight", this.scanningRight);
      tag.putFloat("ScanStartYaw", this.scanStartYaw);
      tag.putBoolean("ReturningToScanPitch", this.returningToScanPitch);
      if (this.ownerUUID != null) {
         tag.putUUID("OwnerUUID", this.ownerUUID);
         tag.putString("OwnerName", this.ownerName);
      }
   }

   public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.loadAdditional(tag, registries);
      this.yaw = tag.getFloat("Yaw");
      this.previousYaw = this.yaw;
      this.pitch = tag.getFloat("Pitch");
      this.previousPitch = this.pitch;
      this.disabled = tag.getBoolean("Disabled");
      this.disableCooldown = tag.getInt("DisableCooldown");
      this.idleTicks = tag.getInt("IdleTicks");
      this.isScanning = tag.getBoolean("IsScanning");
      this.scanningRight = tag.getBoolean("ScanningRight");
      this.scanStartYaw = tag.getFloat("ScanStartYaw");
      this.returningToScanPitch = tag.getBoolean("ReturningToScanPitch");
      this.itemHandler.deserializeNBT(registries, tag.getCompound("Inventory"));
      if (tag.hasUUID("OwnerUUID")) {
         this.ownerUUID = tag.getUUID("OwnerUUID");
         this.ownerName = tag.getString("OwnerName");
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

   /** 1.20.1 entry point, kept for API compatibility; 1.21 callers pass the registries they already hold. */
   public void handleUpdateTag(CompoundTag tag) {
      this.handleUpdateTag(tag, this.level == null ? RegistryAccess.EMPTY : this.level.registryAccess());
   }

   public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
      this.loadAdditional(tag, registries);
   }

   public SimpleContainer getContainer() {
      SimpleContainer container = new SimpleContainer(10);

      for (int i = 0; i < 10; i++) {
         container.setItem(i, this.itemHandler.getStackInSlot(i));
      }

      return container;
   }

   @NotNull
   public <T> T getCapability(Object cap, @Nullable Direction side) {
      return cap == Capabilities.ItemHandler.BLOCK && side != Direction.UP ? ((T) this.lazyItemHandler) : null;
   }

   public ItemStackHandler getItemStackHandler() {
      return this.itemHandler;
   }

   public void setChanged() {
      super.setChanged();
      if (this.level != null) {
         this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
      }
   }
}
