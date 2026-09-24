package top.ribs.scguns.entity.projectile;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import top.ribs.scguns.config.RaidConfig;
import top.ribs.scguns.config.RaidFlareConfig;
import top.ribs.scguns.entity.raid.RaidManager;
import top.ribs.scguns.init.ModEntities;
import top.ribs.scguns.network.PacketHandler;
import top.ribs.scguns.network.message.S2CMessageRaidFlareBurst;

public class RaidFlareEntity extends ThrowableProjectile {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final EntityDataAccessor<String> RAID_ID = SynchedEntityData.defineId(RaidFlareEntity.class, EntityDataSerializers.STRING);
   private String raidId;
   private int ticksExisted = 0;
   private boolean hasBurst = false;

   public RaidFlareEntity(EntityType<? extends RaidFlareEntity> type, Level level) {
      super(type, level);
   }

   public RaidFlareEntity(Level level, LivingEntity shooter, String raidId) {
      super((EntityType)ModEntities.RAID_FLARE.get(), shooter, level);
      this.raidId = raidId;
      this.entityData.set(RAID_ID, raidId);
   }

   public String getRaidId() {
      if (this.raidId == null || this.raidId.isEmpty()) {
         this.raidId = (String)this.entityData.get(RAID_ID);
      }

      return this.raidId;
   }

   protected void onHit(HitResult result) {
      if (!this.level().isClientSide && !this.hasBurst) {
         String currentRaidId = this.getRaidId();
         if (currentRaidId == null || currentRaidId.isEmpty()) {
            return;
         }

         RaidFlareConfig.FlareData flareData = RaidFlareConfig.getFlareData(currentRaidId);
         if (flareData == null) {
            return;
         }

         this.performBurst(flareData);
         this.hasBurst = true;
         this.setDeltaMovement(Vec3.ZERO);
      }
   }

   protected void onHitBlock(BlockHitResult result) {
      super.onHitBlock(result);
      this.onHit(result);
   }

   public void tick() {
      super.tick();
      this.ticksExisted++;
      String currentRaidId = this.getRaidId();
      if (currentRaidId != null && !currentRaidId.isEmpty()) {
         RaidFlareConfig.FlareData flareData = RaidFlareConfig.getFlareData(currentRaidId);
         if (flareData == null) {
            if (this.ticksExisted > 200 && !this.level().isClientSide) {
               this.discard();
            }
         } else {
            if (this.level().isClientSide && this.ticksExisted % 2 == 0) {
               this.spawnTrailParticles(flareData);
            }

            if (!this.level().isClientSide && this.ticksExisted >= flareData.burstDelay() && !this.hasBurst) {
               this.performBurst(flareData);
               this.hasBurst = true;
            }

            if (this.hasBurst && this.ticksExisted >= flareData.burstDelay() + 40 && !this.level().isClientSide) {
               this.discard();
            }

            Vec3 motion = this.getDeltaMovement();
            if (!this.onGround() && !this.hasBurst) {
               this.setDeltaMovement(motion.x * 0.99, motion.y - 0.04, motion.z * 0.99);
            } else {
               this.setDeltaMovement(Vec3.ZERO);
            }
         }
      } else {
         if (!this.level().isClientSide) {
            this.discard();
         }
      }
   }

   private void spawnTrailParticles(RaidFlareConfig.FlareData flareData) {
      for (RaidFlareConfig.ParticleEffect effect : flareData.trailParticles()) {
         ParticleOptions particle = this.getParticleType(effect.particleType());
         if (particle != null) {
            for (int i = 0; i < effect.count(); i++) {
               double offsetX = (this.random.nextDouble() - 0.5) * effect.spread();
               double offsetY = (this.random.nextDouble() - 0.5) * effect.spread();
               double offsetZ = (this.random.nextDouble() - 0.5) * effect.spread();
               this.level()
                  .addParticle(
                     particle,
                     this.getX() + offsetX,
                     this.getY() + offsetY,
                     this.getZ() + offsetZ,
                     offsetX * effect.speed(),
                     offsetY * effect.speed(),
                     offsetZ * effect.speed()
                  );
            }
         }
      }
   }

   private void performBurst(RaidFlareConfig.FlareData flareData) {
      if (this.level() instanceof ServerLevel serverLevel) {
         SoundEvent sound = (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse(flareData.burstSound()));
         if (sound != null) {
            this.level()
               .playSound(
                  null,
                  this.getX(),
                  this.getY(),
                  this.getZ(),
                  sound,
                  SoundSource.AMBIENT,
                  flareData.burstSoundVolume(),
                  flareData.burstSoundPitch()
               );
         }

         this.sendBurstParticlePacket(serverLevel, flareData);
         if (this.getOwner() instanceof ServerPlayer player) {
            RaidConfig.RaidData config = RaidConfig.getRaidByRaidId(flareData.raidId());
            if (config != null) {
               RaidManager manager = RaidManager.get(serverLevel);
               manager.startRaidFromPlayer(config, serverLevel, player);
            }
         }
      }
   }

   private void sendBurstParticlePacket(ServerLevel level, RaidFlareConfig.FlareData flareData) {
      List<S2CMessageRaidFlareBurst.ParticleData> particles = new ArrayList<>();

      for (RaidFlareConfig.ParticleEffect effect : flareData.burstParticles()) {
         particles.add(new S2CMessageRaidFlareBurst.ParticleData(effect.particleType(), effect.count(), effect.spread(), effect.speed(), effect.color()));
      }

      S2CMessageRaidFlareBurst message = this.createBurstMessage(flareData, particles);
      PacketHandler.getPlayChannel().sendToTrackingEntity(() -> this, message);
   }

   @NotNull
   private S2CMessageRaidFlareBurst createBurstMessage(RaidFlareConfig.FlareData flareData, List<S2CMessageRaidFlareBurst.ParticleData> particles) {
      String patternType = "default";
      double scale = 3.0;
      int repetitions = 1;
      if (flareData.pattern() != null) {
         patternType = flareData.pattern().patternType();
         scale = flareData.pattern().scale();
         repetitions = flareData.pattern().repetitions();
      }

      return new S2CMessageRaidFlareBurst(this.getX(), this.getY(), this.getZ(), patternType, scale, repetitions, particles);
   }

   @Nullable
   private ParticleOptions getParticleType(String particleId) {
      try {
         ResourceLocation location = ResourceLocation.parse(particleId);
         return (ParticleOptions)BuiltInRegistries.PARTICLE_TYPE.get(location);
      } catch (Exception var3) {
         return null;
      }
   }

   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putString("RaidId", this.getRaidId());
   }

   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      if (tag.contains("RaidId")) {
         this.raidId = tag.getString("RaidId");
         this.entityData.set(RAID_ID, this.raidId);
      }
   }

   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      builder.define(RAID_ID, "");
   }
}
