package top.ribs.scguns.entity.raid;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent.BossBarOverlay;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import top.ribs.scguns.Config;
import top.ribs.scguns.config.RaidConfig;

public class ActiveRaid {
   private static final int BOSS_REVALIDATION_INTERVAL = 100;
   private static final int TARGET_UPDATE_INTERVAL = 40;
   private static final int BOSS_VALIDATION_TICKS = 600;
   /**
    * How long the raid survives the boss being **not resolvable** (its chunk is unloaded) before it is
    * given up as failed. This is deliberately generous and separate from the pre-confirmation wait:
    * "the boss is unloaded" is not "the boss is dead", and treating it as a defeat is what made a raid
    * vanish - boss bar and loot table included - the moment the player died and respawned away from it
    * (HANDOFF section 74).
    */
   private static final int BOSS_LOST_GRACE_TICKS = 6000;
   private final UUID raidId;
   private final Integer raidLevel;
   private final RaidConfig.RaidData config;
   private final ServerLevel level;
   private final Vec3 spawnCenter;
   private final long startTime;
   private UUID bossUUID;
   private UUID mountUUID;
   private UUID targetPlayerUUID;
   private final Set<UUID> henchmenUUIDs;
   private int spawnTimer;
   private int totalHenchmenSpawned;
   private boolean isActive;
   private boolean bossConfirmed;
   private ServerBossEvent bossBar;
   /** Ticks since the boss could last be resolved at all (unloaded counts, dead does not). */
   private int ticksSinceLoad = 0;
   /** Where the boss was last seen, so an unloaded boss can be brought back into view. */
   private BlockPos lastKnownBossPos;
   /** The chunk this raid forced loaded, so it can be released again. */
   private ChunkPos forcedChunkPos;
   private int ticksSinceLastValidation = 0;
   private int ticksSinceTargetUpdate = 0;
   private int ticksSinceStart = 0;

   public ActiveRaid(Integer raidLevel, RaidConfig.RaidData config, ServerLevel level, Vec3 spawnCenter, long startTime) {
      super();
      this.raidId = UUID.randomUUID();
      this.raidLevel = raidLevel;
      this.config = config;
      this.level = level;
      this.spawnCenter = spawnCenter;
      this.startTime = startTime;
      this.henchmenUUIDs = new HashSet<>();
      this.spawnTimer = config.henchmen().spawnIntervalTicks();
      this.totalHenchmenSpawned = 0;
      this.isActive = true;
      this.bossConfirmed = false;
      this.mountUUID = null;
      this.targetPlayerUUID = null;
      this.ticksSinceStart = 0;
      this.ticksSinceLoad = 0;
      this.ticksSinceLastValidation = 0;
      this.ticksSinceTargetUpdate = 0;
      this.createBossBar();
   }

   public static ActiveRaid restore(RaidSaveData.ActiveRaidData data, RaidConfig.RaidData config, ServerLevel level) {
      ActiveRaid raid = new ActiveRaid(data.raidLevel(), config, level, data.spawnCenter(), data.startTime());
      raid.bossUUID = data.bossUUID();
      raid.mountUUID = data.mountUUID();
      raid.targetPlayerUUID = data.targetPlayerUUID();
      raid.henchmenUUIDs.addAll(data.henchmenUUIDs());
      raid.spawnTimer = data.spawnTimer();
      raid.totalHenchmenSpawned = data.totalSpawned();
      raid.isActive = data.isActive();
      raid.bossConfirmed = false;
      raid.ticksSinceLoad = 0;
      // The boss's position and any forced chunk are runtime state: after a reload the chunk is simply
      // re-forced around the spawn centre until the boss is seen again.
      raid.lastKnownBossPos = null;
      raid.forcedChunkPos = null;
      long elapsedTime = level.getGameTime() - data.startTime();
      raid.ticksSinceStart = (int)Math.min(elapsedTime, 2147483647L);
      return raid;
   }

   private void createBossBar() {
      String bossName = this.config.boss().customName();
      Component title;
      if (bossName != null && bossName.startsWith("translation:")) {
         title = Component.translatable(bossName.substring(12));
      } else if (bossName != null) {
         title = Component.literal(bossName);
      } else {
         title = Component.literal("Raid Boss: " + this.config.raidId());
      }

      this.bossBar = new ServerBossEvent(title, BossBarColor.RED, BossBarOverlay.NOTCHED_10);
      this.bossBar.setProgress(1.0F);
      this.bossBar.setVisible(true);
   }

   public void setActive(boolean active) {
      this.isActive = active;
   }

   public ServerBossEvent getBossBar() {
      return this.bossBar;
   }

   public void tick() {
      if (this.isActive) {
         // ticksSinceLoad counts "ticks since the boss could last be resolved", so it is incremented
         // exactly where the boss turns out to be unresolvable (validateBoss and the branch below),
         // never here - otherwise the grace would expire twice as fast.
         this.ticksSinceLastValidation++;
         this.ticksSinceTargetUpdate++;
         this.ticksSinceStart++;
         int timeoutMinutes = (Integer)Config.COMMON.raids.raidTimeoutMinutes.get();
         if (timeoutMinutes > 0) {
            int timeoutTicks = timeoutMinutes * 60 * 20;
            if (this.ticksSinceStart >= timeoutTicks) {
               this.announceToNearbyPlayers(
                  Component.translatable("raid.scguns.timeout").withStyle(ChatFormatting.RED),
                  Math.max(256.0, (double)this.config.spawnConditions().searchRadius())
               );
               this.endRaid(false);
               return;
            }
         }

         if (this.validateBoss()) {
            if (this.ticksSinceLastValidation >= 100) {
               this.revalidateRaidState();
               this.ticksSinceLastValidation = 0;
            }

            if (this.ticksSinceTargetUpdate >= 40) {
               this.updateMobTargets();
               this.ticksSinceTargetUpdate = 0;
            }

            LivingEntity boss = this.getBoss();
            if (boss != null && boss.isAlive()) {
               this.onBossResolved(boss);
               if (this.mountUUID != null) {
                  Entity mount = this.getMount();
                  if (mount == null || !mount.isAlive()) {
                     this.mountUUID = null;
                  }
               }

               this.updateBossBar();
               if (this.level.getGameTime() % 20L == 0L) {
                  this.updateBossBarPlayers();
               }

               if (this.bossConfirmed && this.spawnTimer > 0) {
                  this.spawnTimer--;
               }
            } else if (boss != null) {
               // Resolved and not alive: the boss really is dead (a kill whose death event the raid
               // never saw, or one from a session that ended). That is a defeat.
               this.endRaid(true);
            } else {
               // Not resolvable at all: its chunk is unloaded - the player died and respawned away
               // from it, was teleported, or simply walked off. 0.5.5 (and this port until now) treated
               // that as "boss defeated", which hid the boss bar for everyone, dropped the raid out of
               // the manager and made the raid's special loot table impossible to obtain. Keep the raid
               // alive instead, pull the boss's chunk back in, and only give up after a long grace.
               this.ticksSinceLoad++;
               this.keepBossChunkLoaded();
               if (this.ticksSinceLoad >= BOSS_LOST_GRACE_TICKS) {
                  this.announceToNearbyPlayers(
                     Component.translatable("raid.scguns.boss_lost").withStyle(ChatFormatting.RED),
                     Math.max(256.0, (double)this.config.spawnConditions().searchRadius()));
                  this.endRaid(false);
               }
            }
         }
      }
   }

   /**
    * The boss is loaded and alive: remember where it is, release the chunk this raid forced open and
    * clear the "boss not resolvable" counter (HANDOFF section 74).
    */
   private void onBossResolved(LivingEntity boss) {
      this.ticksSinceLoad = 0;
      this.lastKnownBossPos = boss.blockPosition();
      this.releaseForcedChunk();
   }

   /**
    * Force the chunk the boss was last seen in, so an unloaded boss comes back and can be validated.
    * Bounded: the caller gives up after {@link #BOSS_LOST_GRACE_TICKS} and {@link #releaseForcedChunk}
    * runs when the raid ends, so no chunk stays forced forever (0.5.5 never released the one it took
    * while waiting for the boss to appear).
    */
   private void keepBossChunkLoaded() {
      BlockPos target = this.lastKnownBossPos != null
         ? this.lastKnownBossPos
         : BlockPos.containing(this.spawnCenter.x, this.spawnCenter.y, this.spawnCenter.z);
      ChunkPos chunkPos = new ChunkPos(target);
      if (this.forcedChunkPos == null || !this.forcedChunkPos.equals(chunkPos)) {
         this.releaseForcedChunk();
         this.level.setChunkForced(chunkPos.x, chunkPos.z, true);
         this.forcedChunkPos = chunkPos;
      }
   }

   private void releaseForcedChunk() {
      if (this.forcedChunkPos != null) {
         this.level.setChunkForced(this.forcedChunkPos.x, this.forcedChunkPos.z, false);
         this.forcedChunkPos = null;
      }
   }

   private void updateMobTargets() {
      ServerPlayer targetPlayer = this.getTargetPlayer(this.level);
      if (targetPlayer == null || targetPlayer.isRemoved() || targetPlayer.isSpectator() || targetPlayer.isCreative() || !targetPlayer.isAlive()) {
         targetPlayer = this.findNewTargetPlayer();
         if (targetPlayer != null) {
            this.targetPlayerUUID = targetPlayer.getUUID();
         }
      }

      if (targetPlayer != null) {
         LivingEntity boss = this.getBoss();
         if (boss instanceof PathfinderMob pathfinderBoss && pathfinderBoss.getTarget() == null) {
            pathfinderBoss.setTarget(targetPlayer);
            if (boss instanceof AbstractPiglin abstractPiglin) {
               try {
                  Brain<?> brain = abstractPiglin.getBrain();
                  brain.eraseMemory(MemoryModuleType.ANGRY_AT);
                  brain.setMemory(MemoryModuleType.ANGRY_AT, targetPlayer.getUUID());
                  brain.eraseMemory(MemoryModuleType.UNIVERSAL_ANGER);
                  brain.setMemory(MemoryModuleType.UNIVERSAL_ANGER, true);
                  brain.setMemory(MemoryModuleType.ATTACK_TARGET, targetPlayer);
                  brain.eraseMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER);
                  brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER, targetPlayer);
                  abstractPiglin.setLastHurtByMob(targetPlayer);
               } catch (Exception var10) {
               }
            }
         }

         for (UUID henchmanUUID : this.henchmenUUIDs) {
            Entity entity = this.level.getEntity(henchmanUUID);
            if (entity instanceof PathfinderMob) {
               PathfinderMob pathfinderMob = (PathfinderMob)entity;
               if (pathfinderMob.getTarget() == null) {
                  pathfinderMob.setTarget(targetPlayer);
                  if (entity instanceof AbstractPiglin) {
                     AbstractPiglin abstractPiglin = (AbstractPiglin)entity;

                     try {
                        Brain<?> brain = abstractPiglin.getBrain();
                        brain.eraseMemory(MemoryModuleType.ANGRY_AT);
                        brain.setMemory(MemoryModuleType.ANGRY_AT, targetPlayer.getUUID());
                        brain.eraseMemory(MemoryModuleType.UNIVERSAL_ANGER);
                        brain.setMemory(MemoryModuleType.UNIVERSAL_ANGER, true);
                        brain.setMemory(MemoryModuleType.ATTACK_TARGET, targetPlayer);
                        brain.eraseMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER);
                        brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER, targetPlayer);
                        abstractPiglin.setLastHurtByMob(targetPlayer);
                     } catch (Exception var9) {
                     }
                  }
               }
            }
         }
      }
   }

   @Nullable
   private ServerPlayer findNewTargetPlayer() {
      List<ServerPlayer> nearbyPlayers = this.level
         .getPlayers(
            playerx -> !playerx.isSpectator()
                  && !playerx.isCreative()
                  && playerx.isAlive()
                  && playerx.position().distanceTo(this.spawnCenter) <= (double)this.config.spawnConditions().searchRadius()
         );
      if (nearbyPlayers.isEmpty()) {
         return null;
      } else {
         ServerPlayer closest = null;
         double closestDist = Double.MAX_VALUE;

         for (ServerPlayer player : nearbyPlayers) {
            double dist = player.position().distanceTo(this.spawnCenter);
            if (dist < closestDist) {
               closestDist = dist;
               closest = player;
            }
         }

         return closest;
      }
   }

   private boolean validateBoss() {
      if (this.bossUUID == null) {
         this.endRaid(false);
         return false;
      } else if (this.bossConfirmed) {
         return true;
      } else {
         LivingEntity boss = this.getBoss();
         if (boss != null && boss.isAlive()) {
            this.bossConfirmed = true;
            this.onBossResolved(boss);
            boss.setPos(this.spawnCenter.x, this.spawnCenter.y, this.spawnCenter.z);
            return true;
         } else if (boss != null) {
            // Resolved, and dead: there is nothing left to fight, so this raid is over as a failure.
            this.endRaid(false);
            return false;
         } else {
            // Not resolvable: an unloaded boss is not a lost raid. Wait for it with its chunk pulled
            // back in, and only give up after the same long grace the confirmed path uses - a raid that
            // starts while its boss is still out of view (restored from the save, player far away) must
            // not be thrown away after half a minute any more.
            this.ticksSinceLoad++;
            this.keepBossChunkLoaded();
            if (this.ticksSinceLoad >= BOSS_LOST_GRACE_TICKS) {
               this.endRaid(false);
            }

            return false;
         }
      }
   }

   private void revalidateRaidState() {
      this.henchmenUUIDs.removeIf(uuid -> this.level.getEntity(uuid) instanceof LivingEntity livingEntity ? !livingEntity.isAlive() : true);
   }

   public void updateBossBarPlayers() {
      if (this.bossBar != null) {
         LivingEntity boss = this.getBoss();
         if (boss != null && boss.isAlive()) {
            List<ServerPlayer> nearbyPlayers = this.level.getPlayers(playerx -> {
               if (playerx.isAlive() && !playerx.isRemoved() && !playerx.isSpectator()) {
                  double distance = playerx.position().distanceTo(boss.position());
                  return distance <= 128.0;
               } else {
                  return false;
               }
            });
            Set<ServerPlayer> currentPlayersSet = new HashSet<>(this.bossBar.getPlayers());

            for (ServerPlayer player : currentPlayersSet) {
               if (!nearbyPlayers.contains(player) || !player.isAlive() || player.isRemoved()) {
                  this.bossBar.removePlayer(player);
               }
            }

            for (ServerPlayer playerx : nearbyPlayers) {
               if (!currentPlayersSet.contains(playerx)) {
                  this.bossBar.addPlayer(playerx);
               }
            }
         }
      }
   }

   private void updateBossBar() {
      if (this.bossBar != null) {
         LivingEntity boss = this.getBoss();
         if (boss != null && boss.isAlive()) {
            float healthPercent = boss.getHealth() / boss.getMaxHealth();
            this.bossBar.setProgress(Math.max(0.0F, Math.min(1.0F, healthPercent)));
         } else if (this.bossConfirmed) {
            this.bossBar.setProgress(0.0F);
         }
      }
   }

   public void onBossDefeated() {
      this.endRaid(true);
   }

   public UUID getRaidId() {
      return this.raidId;
   }

   public Integer getRaidLevel() {
      return this.raidLevel;
   }

   public RaidConfig.RaidData getConfig() {
      return this.config;
   }

   public Vec3 getSpawnCenter() {
      return this.spawnCenter;
   }

   public boolean isActive() {
      return this.isActive;
   }

   public long getStartTime() {
      return this.startTime;
   }

   public int getSpawnTimer() {
      return this.spawnTimer;
   }

   public int getTotalHenchmenSpawned() {
      return this.totalHenchmenSpawned;
   }

   public boolean isBossConfirmed() {
      return this.bossConfirmed;
   }

   public void setBossUUID(UUID bossUUID) {
      this.bossUUID = bossUUID;
      this.bossConfirmed = false;
   }

   @Nullable
   public UUID getBossUUID() {
      return this.bossUUID;
   }

   public void setMountUUID(UUID mountUUID) {
      this.mountUUID = mountUUID;
   }

   @Nullable
   public UUID getMountUUID() {
      return this.mountUUID;
   }

   public void setTargetPlayer(UUID playerUUID) {
      this.targetPlayerUUID = playerUUID;
   }

   @Nullable
   public UUID getTargetPlayerUUID() {
      return this.targetPlayerUUID;
   }

   @Nullable
   public ServerPlayer getTargetPlayer(ServerLevel level) {
      return this.targetPlayerUUID == null ? null : level.getServer().getPlayerList().getPlayer(this.targetPlayerUUID);
   }

   @Nullable
   public LivingEntity getBoss() {
      if (this.bossUUID == null) {
         return null;
      } else {
         Entity entity = this.level.getEntity(this.bossUUID);
         return entity instanceof LivingEntity ? (LivingEntity)entity : null;
      }
   }

   @Nullable
   public Entity getMount() {
      return this.mountUUID == null ? null : this.level.getEntity(this.mountUUID);
   }

   public void addHenchman(UUID uuid) {
      this.henchmenUUIDs.add(uuid);
      this.totalHenchmenSpawned++;
   }

   public void removeHenchman(UUID uuid) {
      this.henchmenUUIDs.remove(uuid);
   }

   public int getAliveHenchmenCount() {
      this.revalidateRaidState();
      return this.henchmenUUIDs.size();
   }

   public Set<UUID> getHenchmenUUIDs() {
      return new HashSet<>(this.henchmenUUIDs);
   }

   public boolean canSpawnMoreHenchmen() {
      return this.getAliveHenchmenCount() < this.config.henchmen().maxAlive();
   }

   public boolean shouldSpawnHenchmen() {
      return this.spawnTimer <= 0 && this.canSpawnMoreHenchmen() && this.isActive && this.bossConfirmed;
   }

   public void resetSpawnTimer() {
      this.spawnTimer = this.config.henchmen().spawnIntervalTicks();
   }

   public void endRaid(boolean bossDefeated) {
      if (this.isActive) {
         this.isActive = false;
         // Never leave a chunk forced open after the raid is over.
         this.releaseForcedChunk();
         if (bossDefeated) {
            this.announceToNearbyPlayers(Component.translatable("raid.scguns.defeated"), 64.0);
         } else {
            this.announceToNearbyPlayers(Component.translatable("raid.scguns.failed"), 64.0);
         }

         if (this.bossBar != null) {
            this.bossBar.setVisible(false);
            this.bossBar.removeAllPlayers();
         }

         this.cleanupBoss(bossDefeated);
         this.cleanupHenchmen(bossDefeated);
         this.cleanupMount(bossDefeated);
      }
   }

   private void cleanupBoss(boolean wasBossDefeated) {
      if (this.bossUUID != null) {
         if (this.level.getEntity(this.bossUUID) instanceof Mob boss) {
            boss.removeTag("RaidBoss");
            boss.removeTag("RaidMember_" + this.raidId);
            if (!wasBossDefeated && boss.isAlive()) {
               boss.discard();
            }
         }
      }
   }

   private void cleanupHenchmen(boolean wasBossDefeated) {
      for (UUID uuid : new HashSet<>(this.henchmenUUIDs)) {
         Entity entity = this.level.getEntity(uuid);
         if (entity instanceof Mob) {
            Mob mob = (Mob)entity;
            mob.removeTag("RaidHenchman");
            mob.removeTag("RaidMember_" + this.raidId);
            if (!wasBossDefeated && mob.isAlive()) {
               mob.discard();
            }
         }
      }

      this.henchmenUUIDs.clear();
   }

   private void cleanupMount(boolean wasBossDefeated) {
      if (this.mountUUID != null && this.level.getEntity(this.mountUUID) instanceof Mob mob) {
         mob.removeTag("RaidMount");
         mob.removeTag("RaidMember_" + this.raidId);
         if (!wasBossDefeated && mob.isAlive()) {
            mob.discard();
         }
      }
   }

   public void announceToNearbyPlayers(Component message, double radius) {
      for (ServerPlayer player : this.level.getPlayers(playerx -> playerx.position().distanceTo(this.spawnCenter) <= radius)) {
         player.sendSystemMessage(message);
      }
   }

   public long getRaidDuration() {
      return this.level.getGameTime() - this.startTime;
   }

   public int getRemainingTicks() {
      int timeoutMinutes = (Integer)Config.COMMON.raids.raidTimeoutMinutes.get();
      if (timeoutMinutes <= 0) {
         return -1;
      } else {
         int timeoutTicks = timeoutMinutes * 60 * 20;
         return Math.max(0, timeoutTicks - this.ticksSinceStart);
      }
   }

   public int getRemainingMinutes() {
      int remainingTicks = this.getRemainingTicks();
      return remainingTicks < 0 ? -1 : remainingTicks / 1200;
   }

   public void setBossConfirmed(boolean b) {
      this.bossConfirmed = b;
   }
}
