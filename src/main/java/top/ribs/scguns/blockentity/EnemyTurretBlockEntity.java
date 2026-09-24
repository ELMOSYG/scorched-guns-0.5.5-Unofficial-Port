package top.ribs.scguns.blockentity;


import net.minecraft.core.HolderLookup;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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
import top.ribs.scguns.entity.projectile.turret.TurretProjectileEntity;
import top.ribs.scguns.init.ModBlockEntities;
import top.ribs.scguns.util.PhysicsStructureHelper;
import top.ribs.scguns.init.ModSounds;
import top.ribs.scguns.network.PacketHandler;
import top.ribs.scguns.network.message.S2CMessageMuzzleFlash;

public class EnemyTurretBlockEntity extends BlockEntity {
   private static final double TARGETING_RADIUS = 24.0;
   private static final int COOLDOWN = 40;
   private static final float MAX_PITCH = 60.0F;
   private static final float MIN_PITCH = -25.0F;
   private static final float POSITION_SMOOTHING_FACTOR = 0.2F;
   private static final float ROTATION_SPEED = 0.45F;
   private static final float RECOIL_MAX = 4.0F;
   private static final float RECOIL_SPEED = 0.3F;
   private static final double MINIMUM_FIRING_DISTANCE = 1.3;
   private static final float INACCURACY = 0.05F;
   private Player target;
   private float yaw;
   private float pitch;
   private float previousYaw;
   private float previousPitch;
   private double smoothedTargetX;
   private double smoothedTargetY;
   private double smoothedTargetZ;
   private float recoilPitchOffset = 0.0F;
   private int cooldown = 40;
   private boolean disabled = false;
   private int disableCooldown = 0;
   private static final int MAX_DISABLE_TIME = 200;
   private float disabledRotationOffset = 0.0F;
   private float damageMultiplier = 1.0F;
   private float fireRateMultiplier = 1.0F;

   public EnemyTurretBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)ModBlockEntities.ENEMY_TURRET.get(), pos, state);
   }

   public static void tick(Level level, BlockPos pos, BlockState state, EnemyTurretBlockEntity turret) {
      if (turret.cooldown > 0) {
         turret.cooldown--;
      }

      turret.tickRecoil();
      if (turret.disabled) {
         turret.handleDisabled();
      } else {
         turret.findTarget(level, pos);
         turret.updateRotation();
         if (turret.target != null && turret.cooldown <= 0 && turret.isReadyToFire()) {
            turret.fire();
            turret.cooldown = 40;
         }
      }
   }

   private void handleDisabled() {
      this.disableCooldown--;
      if (this.disableCooldown <= 0) {
         this.disabled = false;
         this.disableCooldown = 0;
         this.disabledRotationOffset = 0.0F;
      } else {
         this.disabledRotationOffset = (float)Math.sin((double)this.disableCooldown * 0.1) * 5.0F;
      }

      this.resetToRestPosition();
   }

   private void findTarget(Level level, BlockPos pos) {
      this.target = null;
      Vec3 turretPos = new Vec3((double)this.worldPosition.getX() + 0.5, (double)this.worldPosition.getY() + 1.0, (double)this.worldPosition.getZ() + 0.5);
      // Line-of-sight raycasts happen in world space, while the distance comparisons below
      // stay in this turret's own frame.
      Vec3 convertedTurretPos = PhysicsStructureHelper.toWorld(level, this.worldPosition, turretPos);
      final Vec3 turretPosWorld = convertedTurretPos != null ? convertedTurretPos : turretPos;
      AABB searchBox = new AABB(pos).inflate(24.0, 24.0, 24.0);
      List<Player> potentialTargets = level.getEntitiesOfClass(
         Player.class,
         searchBox,
         player -> player != null
               && player.isAlive()
               && !player.isCreative()
               && !player.isSpectator()
               && !player.isInvisible()
               && this.hasLineOfSight(level, turretPosWorld, player)
      );
      if (!potentialTargets.isEmpty()) {
         this.target = potentialTargets.stream().min(Comparator.comparingDouble(player -> player.distanceToSqr(turretPos))).orElse(null);
         this.updateTargetPosition();
      }
   }

   private void updateTargetPosition() {
      if (this.target != null) {
         double predictedX = this.target.getX() + this.target.getDeltaMovement().x * 7.0;
         double predictedY = this.target.getY() + (double)this.target.getEyeHeight() + this.target.getDeltaMovement().y * 7.0;
         double predictedZ = this.target.getZ() + this.target.getDeltaMovement().z * 7.0;
         // Keep the aim point in this turret's own frame (see TurretBlockEntity).
         Vec3 predictedLocal = PhysicsStructureHelper.toLocal(this.level, this.worldPosition, new Vec3(predictedX, predictedY, predictedZ));
         if (predictedLocal != null) {
            predictedX = predictedLocal.x;
            predictedY = predictedLocal.y;
            predictedZ = predictedLocal.z;
         }

         this.smoothedTargetX = lerp(this.smoothedTargetX, predictedX, 0.2F);
         this.smoothedTargetY = lerp(this.smoothedTargetY, predictedY, 0.2F);
         this.smoothedTargetZ = lerp(this.smoothedTargetZ, predictedZ, 0.2F);
      }
   }

   private void updateRotation() {
      this.previousYaw = this.yaw;
      this.previousPitch = this.pitch;
      if (this.smoothedTargetX != 0.0 || this.smoothedTargetZ != 0.0) {
         this.updateYaw();
         this.updatePitch();
      }
   }

   private void updateYaw() {
      double dx = this.smoothedTargetX - ((double)this.worldPosition.getX() + 0.5);
      double dz = this.smoothedTargetZ - ((double)this.worldPosition.getZ() + 0.5);
      float targetYaw = (float)(Math.atan2(dx, dz) * (180.0 / Math.PI)) + 180.0F;
      targetYaw = (targetYaw + 360.0F) % 360.0F;
      float yawDifference = targetYaw - this.yaw;
      if (yawDifference > 180.0F) {
         yawDifference -= 360.0F;
      } else if (yawDifference < -180.0F) {
         yawDifference += 360.0F;
      }

      this.yaw += yawDifference * 0.45F;
      this.yaw %= 360.0F;
      if (this.yaw < 0.0F) {
         this.yaw += 360.0F;
      }
   }

   private void updatePitch() {
      if (this.smoothedTargetY != 0.0) {
         double dx = this.smoothedTargetX - ((double)this.worldPosition.getX() + 0.5);
         double dy = this.smoothedTargetY - ((double)this.worldPosition.getY() + 1.0);
         double dz = this.smoothedTargetZ - ((double)this.worldPosition.getZ() + 0.5);
         double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
         float targetPitch = (float)(Math.atan2(dy, horizontalDistance) * (180.0 / Math.PI));
         targetPitch = Mth.clamp(targetPitch, -25.0F, 60.0F);
         float pitchDifference = targetPitch - this.pitch;
         this.pitch += pitchDifference * 0.45F;
         this.pitch = Mth.clamp(this.pitch, -25.0F, 60.0F);
      }
   }

   private boolean isReadyToFire() {
      if (this.target == null) {
         return false;
      } else {
         double dx = this.smoothedTargetX - ((double)this.worldPosition.getX() + 0.5);
         double dy = this.smoothedTargetY - ((double)this.worldPosition.getY() + 1.0);
         double dz = this.smoothedTargetZ - ((double)this.worldPosition.getZ() + 0.5);
         double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
         float targetYaw = (float)(Math.atan2(dx, dz) * (180.0 / Math.PI)) + 180.0F;
         targetYaw = (targetYaw + 360.0F) % 360.0F;
         float targetPitch = (float)(Math.atan2(dy, horizontalDistance) * (180.0 / Math.PI));
         targetPitch = Mth.clamp(targetPitch, -25.0F, 60.0F);
         float yawDifference = Math.abs(targetYaw - this.yaw);
         if (yawDifference > 180.0F) {
            yawDifference = 360.0F - yawDifference;
         }

         float pitchDifference = Math.abs(targetPitch - this.pitch);
         double distanceSquared = dx * dx + dy * dy + dz * dz;
         return distanceSquared >= 1.6900000000000002 && yawDifference < 2.0F && pitchDifference < 2.0F;
      }
   }

   private void fire() {
      if (this.level != null && this.target != null) {
         Vec3 muzzleLocal = this.getMuzzlePosition(this.yaw, this.pitch);
         Vec3 convertedMuzzle = PhysicsStructureHelper.toWorld(this.level, this.worldPosition, muzzleLocal);
         Vec3 muzzlePos = convertedMuzzle != null ? convertedMuzzle : muzzleLocal;
         if (!this.level.isClientSide) {
            PacketHandler.getPlayChannel()
               .sendToTrackingChunk(() -> this.level.getChunkAt(BlockPos.containing(muzzlePos)), new S2CMessageMuzzleFlash(muzzlePos, this.yaw, this.pitch));
         }

         Vec3 targetPos = new Vec3(this.target.getX(), this.target.getY() + (double)this.target.getEyeHeight() * 0.5, this.target.getZ());
         Vec3 direction = targetPos.subtract(muzzlePos).normalize();
         direction = direction.add(
               this.level.random.triangle(0.0, 0.05F), this.level.random.triangle(0.0, 0.05F), this.level.random.triangle(0.0, 0.05F)
            )
            .normalize();
         TurretProjectileEntity projectile = new TurretProjectileEntity(this.level);
         projectile.setPos(muzzlePos.x, muzzlePos.y, muzzlePos.z);
         projectile.shoot(direction.x, direction.y, direction.z, 3.0F, 0.0F);
         projectile.setBaseDamage(2.5 * (double)this.damageMultiplier);
         this.level.addFreshEntity(projectile);
         this.level.playSound(null, muzzlePos.x, muzzlePos.y, muzzlePos.z, (SoundEvent)ModSounds.IRON_RIFLE_FIRE.get(), SoundSource.BLOCKS, 0.7F, 0.7F);
         this.recoilPitchOffset = 4.0F;
         this.cooldown = (int)(40.0F * this.fireRateMultiplier);
      }
   }

   private Vec3 getMuzzlePosition(float yaw, float pitch) {
      double muzzleLength = 1.0;
      double muzzleOffsetY = 1.4;
      double yawRad = Math.toRadians((double)yaw);
      double pitchRad = Math.toRadians((double)pitch);
      double muzzleX = -Math.sin(yawRad) * Math.cos(pitchRad) * muzzleLength;
      double muzzleY = Math.sin(pitchRad) * muzzleLength + muzzleOffsetY;
      double muzzleZ = -Math.cos(yawRad) * Math.cos(pitchRad) * muzzleLength;
      return new Vec3(
         (double)this.worldPosition.getX() + 0.5 + muzzleX, (double)this.worldPosition.getY() + muzzleY, (double)this.worldPosition.getZ() + 0.5 + muzzleZ
      );
   }

   private boolean hasLineOfSight(Level level, Vec3 turretPos, LivingEntity target) {
      Vec3 targetPos = target.getEyePosition();
      Vec3 toTarget = targetPos.subtract(turretPos);
      double distance = toTarget.length();
      Vec3 rayVector = toTarget.normalize().scale(distance);
      Vec3 adjustedTurretPos = turretPos.add(0.0, 0.5, 0.0);
      ClipContext clipContext = new ClipContext(adjustedTurretPos, adjustedTurretPos.add(rayVector), Block.COLLIDER, Fluid.NONE, net.minecraft.world.phys.shapes.CollisionContext.empty());
      BlockHitResult hitResult = level.clip(clipContext);
      return hitResult.getType() == Type.MISS;
   }

   private void tickRecoil() {
      if (this.recoilPitchOffset > 0.0F) {
         this.recoilPitchOffset -= 0.3F;
         if (this.recoilPitchOffset < 0.0F) {
            this.recoilPitchOffset = 0.0F;
         }
      }
   }

   private void resetToRestPosition() {
      this.target = null;
      float restingYaw = 0.0F;
      float restingPitch = -30.0F;
      this.previousYaw = this.yaw;
      this.previousPitch = this.pitch;
      float yawDifference = restingYaw + this.disabledRotationOffset - this.yaw;
      if (yawDifference > 180.0F) {
         yawDifference -= 360.0F;
      } else if (yawDifference < -180.0F) {
         yawDifference += 360.0F;
      }

      this.yaw += yawDifference * 0.45F;
      this.yaw %= 360.0F;
      if (this.yaw < 0.0F) {
         this.yaw += 360.0F;
      }

      float pitchDifference = restingPitch - this.pitch;
      this.pitch += pitchDifference * 0.45F;
      this.smoothedTargetX = 0.0;
      this.smoothedTargetY = 0.0;
      this.smoothedTargetZ = 0.0;
   }

   public void onHitByLightningProjectile() {
      this.disabled = true;
      this.disableCooldown = 200;
      this.resetToRestPosition();
      this.setChanged();
      if (this.level != null && !this.level.isClientSide) {
         this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
         this.spawnDisableParticles();
         this.level.playSound(null, this.worldPosition, SoundEvents.IRON_GOLEM_DAMAGE, SoundSource.BLOCKS, 1.0F, 0.5F);
      }
   }

   private void spawnDisableParticles() {
      if (this.level instanceof ServerLevel serverLevel) {
         Vec3 centre = PhysicsStructureHelper.toWorld(this.level, this.worldPosition,
               new Vec3((double)this.worldPosition.getX() + 0.5, (double)this.worldPosition.getY() + 1.0, (double)this.worldPosition.getZ() + 0.5));
         if (centre == null) {
            centre = new Vec3((double)this.worldPosition.getX() + 0.5, (double)this.worldPosition.getY() + 1.0, (double)this.worldPosition.getZ() + 0.5);
         }

         double x = centre.x;
         double y = centre.y;
         double z = centre.z;
         int particleCount = 20;
         double spread = 0.5;

         for (int i = 0; i < particleCount; i++) {
            double offsetX = this.level.random.nextDouble() * spread - spread / 2.0;
            double offsetY = this.level.random.nextDouble() * spread;
            double offsetZ = this.level.random.nextDouble() * spread - spread / 2.0;
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, x + offsetX, y + offsetY, z + offsetZ, 1, 0.0, 0.0, 0.0, 0.05);
         }

         serverLevel.playSound(null, x, y, z, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
      }
   }

   private static double lerp(double a, double b, double t) {
      return a + t * (b - a);
   }

   public float getYaw() {
      return this.yaw;
   }

   public float getPitch() {
      return this.pitch;
   }

   public float getPreviousYaw() {
      return this.previousYaw;
   }

   public float getPreviousPitch() {
      return this.previousPitch;
   }

   public float getRecoilPitchOffset() {
      return this.recoilPitchOffset;
   }

   public void setDamageMultiplier(float multiplier) {
      this.damageMultiplier = multiplier;
   }

   public void setFireRateMultiplier(float multiplier) {
      this.fireRateMultiplier = Math.max(0.1F, multiplier);
   }

   public float getFireRateMultiplier() {
      return this.fireRateMultiplier;
   }

   protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.saveAdditional(tag, registries);
      tag.putFloat("Yaw", this.yaw);
      tag.putFloat("Pitch", this.pitch);
      tag.putBoolean("Disabled", this.disabled);
      tag.putInt("DisableCooldown", this.disableCooldown);
      tag.putFloat("DamageMultiplier", this.damageMultiplier);
      tag.putFloat("FireRateMultiplier", this.fireRateMultiplier);
      tag.putFloat("DisabledRotationOffset", this.disabledRotationOffset);
   }

   public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.loadAdditional(tag, registries);
      this.yaw = tag.getFloat("Yaw");
      this.previousYaw = this.yaw;
      this.pitch = tag.getFloat("Pitch");
      this.previousPitch = this.pitch;
      this.disabled = tag.getBoolean("Disabled");
      this.disableCooldown = tag.getInt("DisableCooldown");
      this.damageMultiplier = tag.getFloat("DamageMultiplier");
      this.disabledRotationOffset = tag.getFloat("DisabledRotationOffset");
      this.fireRateMultiplier = tag.getFloat("FireRateMultiplier");
      if (this.fireRateMultiplier <= 0.0F) {
         this.fireRateMultiplier = 1.0F;
      }

      this.disabledRotationOffset = tag.getFloat("DisabledRotationOffset");
   }
}
