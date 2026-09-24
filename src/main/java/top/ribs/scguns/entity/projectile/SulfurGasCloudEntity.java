package top.ribs.scguns.entity.projectile;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import top.ribs.scguns.common.SulfurGasCloud;

public class SulfurGasCloudEntity extends Entity {
   private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(SulfurGasCloudEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer> DURATION = SynchedEntityData.defineId(SulfurGasCloudEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> BASE_DURATION = SynchedEntityData.defineId(SulfurGasCloudEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> BASE_AMPLIFIER = SynchedEntityData.defineId(SulfurGasCloudEntity.class, EntityDataSerializers.INT);
   private int ticksExisted = 0;
   private static final float DAMAGE_PHASE_RATIO = 0.85F;

   public SulfurGasCloudEntity(EntityType<?> entityType, Level level) {
      super(entityType, level);
      this.noPhysics = true;
   }

   public SulfurGasCloudEntity(EntityType<?> entityType, Level level, Vec3 position, float radius, int duration, int baseDuration, int baseAmplifier) {
      this(entityType, level);
      this.setPos(position.x, position.y, position.z);
      this.entityData.set(RADIUS, radius);
      this.entityData.set(DURATION, duration);
      this.entityData.set(BASE_DURATION, baseDuration);
      this.entityData.set(BASE_AMPLIFIER, baseAmplifier);
   }

   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      builder.define(RADIUS, 6.0F);
      builder.define(DURATION, 600);
      builder.define(BASE_DURATION, 100);
      builder.define(BASE_AMPLIFIER, 2);
   }

   public void tick() {
      super.tick();
      if (!this.level().isClientSide) {
         float radius = (Float)this.entityData.get(RADIUS);
         int duration = (Integer)this.entityData.get(DURATION);
         int baseDuration = (Integer)this.entityData.get(BASE_DURATION);
         int baseAmplifier = (Integer)this.entityData.get(BASE_AMPLIFIER);
         Vec3 center = this.position();
         float lifecycleProgress = (float)this.ticksExisted / (float)duration;
         float intensity = this.calculateIntensity(lifecycleProgress);
         SulfurGasCloud.spawnEnhancedGasCloud(this.level(), center, (double)radius, intensity, this.random, this.ticksExisted);
         int damagePhaseTicks = (int)((float)duration * 0.85F);
         if (this.ticksExisted < damagePhaseTicks) {
            float damageMultiplier = this.calculateDamageMultiplier(lifecycleProgress);
            int scaledDuration = (int)((float)baseDuration * damageMultiplier);
            SulfurGasCloud.applyGasEffects(this.level(), center, (double)radius, scaledDuration, baseAmplifier);
         }

         SulfurGasCloud.checkAndHandleFireExplosion(this.level(), center, (double)radius);
         this.ticksExisted++;
         if (this.ticksExisted >= duration) {
            this.discard();
         }
      }
   }

   private float calculateIntensity(float progress) {
      if (progress < 0.1F) {
         return Mth.clamp(progress / 0.1F, 0.0F, 1.0F);
      } else if (progress < 0.6F) {
         return 1.0F;
      } else {
         float fadeProgress = (progress - 0.6F) / 0.4F;
         return Mth.clamp(1.0F - fadeProgress, 0.0F, 1.0F);
      }
   }

   private float calculateDamageMultiplier(float progress) {
      if (progress < 0.15F) {
         return Mth.clamp(progress / 0.15F, 0.3F, 1.0F);
      } else if (progress < 0.5F) {
         return 1.0F;
      } else if (progress < 0.85F) {
         float fadeProgress = (progress - 0.5F) / 0.35000002F;
         return Mth.clamp(1.0F - fadeProgress * 0.7F, 0.3F, 1.0F);
      } else {
         return 0.0F;
      }
   }

   protected void readAdditionalSaveData(CompoundTag compound) {
      if (compound.contains("Radius")) {
         this.entityData.set(RADIUS, compound.getFloat("Radius"));
      }

      if (compound.contains("Duration")) {
         this.entityData.set(DURATION, compound.getInt("Duration"));
      }

      if (compound.contains("BaseDuration")) {
         this.entityData.set(BASE_DURATION, compound.getInt("BaseDuration"));
      }

      if (compound.contains("BaseAmplifier")) {
         this.entityData.set(BASE_AMPLIFIER, compound.getInt("BaseAmplifier"));
      }

      if (compound.contains("TicksExisted")) {
         this.ticksExisted = compound.getInt("TicksExisted");
      }
   }

   protected void addAdditionalSaveData(CompoundTag compound) {
      compound.putFloat("Radius", (Float)this.entityData.get(RADIUS));
      compound.putInt("Duration", (Integer)this.entityData.get(DURATION));
      compound.putInt("BaseDuration", (Integer)this.entityData.get(BASE_DURATION));
      compound.putInt("BaseAmplifier", (Integer)this.entityData.get(BASE_AMPLIFIER));
      compound.putInt("TicksExisted", this.ticksExisted);
   }



   public boolean shouldRenderAtSqrDistance(double distance) {
      return false;
   }

   public boolean isPickable() {
      return false;
   }
}
