package top.ribs.scguns.entity.raid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RaidSaveData extends SavedData {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final String DATA_NAME = "scguns_raid_save_data";
   private static final SavedData.Factory<RaidSaveData> FACTORY = new SavedData.Factory<>(RaidSaveData::new, (tag, registries) -> load(tag));
   private final Map<UUID, RaidSaveData.ActiveRaidData> activeRaidData = new HashMap<>();
   private final Map<ResourceLocation, RaidSaveData.ScheduledRaidData> scheduledRaids = new HashMap<>();
   private final Map<ResourceLocation, Long> lastRaidDayByDimension = new HashMap<>();

   public RaidSaveData() {
   }

   public static RaidSaveData get(ServerLevel level) {
      return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
   }

   @Override
   public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
      return this.save(tag);
   }

   public CompoundTag save(CompoundTag tag) {
      ListTag activeRaidsList = new ListTag();

      for (RaidSaveData.ActiveRaidData data : this.activeRaidData.values()) {
         activeRaidsList.add(data.save());
      }

      tag.put("ActiveRaids", activeRaidsList);
      ListTag scheduledRaidsList = new ListTag();

      for (RaidSaveData.ScheduledRaidData data : this.scheduledRaids.values()) {
         scheduledRaidsList.add(data.save());
      }

      tag.put("ScheduledRaids", scheduledRaidsList);
      CompoundTag lastRaidDaysTag = new CompoundTag();

      for (Entry<ResourceLocation, Long> entry : this.lastRaidDayByDimension.entrySet()) {
         lastRaidDaysTag.putLong(entry.getKey().toString(), entry.getValue());
      }

      tag.put("LastRaidDays", lastRaidDaysTag);
      return tag;
   }

   public static RaidSaveData load(CompoundTag tag) {
      RaidSaveData data = new RaidSaveData();
      if (tag.contains("ActiveRaids")) {
         ListTag activeRaidsList = tag.getList("ActiveRaids", 10);

         for (int i = 0; i < activeRaidsList.size(); i++) {
            RaidSaveData.ActiveRaidData raidData = RaidSaveData.ActiveRaidData.load(activeRaidsList.getCompound(i));
            if (raidData != null) {
               data.activeRaidData.put(raidData.raidId, raidData);
            }
         }
      }

      if (tag.contains("ScheduledRaids")) {
         ListTag scheduledRaidsList = tag.getList("ScheduledRaids", 10);

         for (int ix = 0; ix < scheduledRaidsList.size(); ix++) {
            RaidSaveData.ScheduledRaidData raidData = RaidSaveData.ScheduledRaidData.load(scheduledRaidsList.getCompound(ix));
            if (raidData != null) {
               data.scheduledRaids.put(raidData.dimension, raidData);
            }
         }
      }

      if (tag.contains("LastRaidDays")) {
         CompoundTag lastRaidDaysTag = tag.getCompound("LastRaidDays");

         for (String key : lastRaidDaysTag.getAllKeys()) {
            try {
               ResourceLocation dimension = ResourceLocation.parse(key);
               long lastDay = lastRaidDaysTag.getLong(key);
               data.lastRaidDayByDimension.put(dimension, lastDay);
            } catch (Exception var8) {
               LOGGER.warn("Failed to load last raid day for dimension: {}", key);
            }
         }
      }

      return data;
   }

   public void saveActiveRaid(ActiveRaid raid) {
      RaidSaveData.ActiveRaidData data = new RaidSaveData.ActiveRaidData(
         raid.getRaidId(),
         raid.getConfig().raidId(),
         raid.getRaidLevel(),
         raid.getSpawnCenter(),
         raid.getStartTime(),
         raid.getBossUUID(),
         raid.getMountUUID(),
         raid.getTargetPlayerUUID(),
         raid.getHenchmenUUIDs(),
         raid.getSpawnTimer(),
         raid.getTotalHenchmenSpawned(),
         raid.isActive(),
         raid.isBossConfirmed()
      );
      this.activeRaidData.put(raid.getRaidId(), data);
      this.setDirty();
   }

   public void removeActiveRaid(UUID raidId) {
      if (this.activeRaidData.remove(raidId) != null) {
         this.setDirty();
      }
   }

   public Collection<RaidSaveData.ActiveRaidData> getActiveRaidData() {
      return new ArrayList<>(this.activeRaidData.values());
   }

   public void scheduleRaid(ResourceLocation dimension, ServerPlayer player, String raidId, long day) {
      RaidSaveData.ScheduledRaidData data = new RaidSaveData.ScheduledRaidData(dimension, player.getUUID(), raidId, day);
      this.scheduledRaids.put(dimension, data);
      this.setDirty();
   }

   @Nullable
   public RaidSaveData.ScheduledRaidData getScheduledRaid(ResourceLocation dimension) {
      return this.scheduledRaids.get(dimension);
   }

   public void removeScheduledRaid(ResourceLocation dimension) {
      if (this.scheduledRaids.remove(dimension) != null) {
         this.setDirty();
      }
   }

   public void setLastRaidDay(ResourceLocation dimension, long day) {
      this.lastRaidDayByDimension.put(dimension, day);
      this.setDirty();
   }

   public long getLastRaidDay(ResourceLocation dimension) {
      return this.lastRaidDayByDimension.getOrDefault(dimension, -1000L);
   }

   /**
    * Whether the nightly raid is out of its cooldown, following the meaning `Config` has documented for
    * `minDaysBetweenRaids` since 0.5.5 (HANDOFF section 80).
    *
    * <p>The config says: <i>0 to allow raids every night, 1 to allow raids every other night, 2 or higher
    * to space out raids further</i>. That is this test, not {@code >=} - the comparison this method
    * shipped with until it was wired up, which would have made 0 and 1 behave identically ("every night")
    * and contradicted its own comment. Nothing called it before, so nobody ever noticed.</p>
    *
    * <table>
    * <tr><th>minDaysBetween</th><th>raid on day 7</th><th>next allowed</th><th>effect</th></tr>
    * <tr><td>0</td><td>7</td><td>8</td><td>every night</td></tr>
    * <tr><td>1</td><td>7</td><td>9</td><td>every other night</td></tr>
    * <tr><td>2</td><td>7</td><td>10</td><td>two nights skipped</td></tr>
    * </table>
    */
   public boolean canScheduleRaid(ResourceLocation dimension, long currentDay, int minDaysBetween) {
      long lastRaidDay = this.getLastRaidDay(dimension);
      return currentDay - lastRaidDay > (long)minDaysBetween;
   }

   /** The day the cooldown lifts, for the diagnostic in HANDOFF section 80. {@code -1000} (never) is
    * reported as-is; callers check {@link #hasLastRaidDay} first. */
   public long getNextAllowedRaidDay(ResourceLocation dimension, int minDaysBetween) {
      return this.getLastRaidDay(dimension) + (long)minDaysBetween + 1L;
   }

   public boolean hasLastRaidDay(ResourceLocation dimension) {
      return this.lastRaidDayByDimension.containsKey(dimension);
   }

   public void cleanupInvalidRaids(ServerLevel level) {
      Iterator<Entry<UUID, RaidSaveData.ActiveRaidData>> iterator = this.activeRaidData.entrySet().iterator();
      int removed = 0;

      while (iterator.hasNext()) {
         Entry<UUID, RaidSaveData.ActiveRaidData> entry = iterator.next();
         RaidSaveData.ActiveRaidData data = entry.getValue();
         if (!data.isActive) {
            iterator.remove();
            removed++;
         } else if (level.getEntity(data.bossUUID) == null && data.bossConfirmed) {
            iterator.remove();
            removed++;
         }
      }

      if (removed > 0) {
         this.setDirty();
      }
   }

   public static record ActiveRaidData(
      UUID raidId,
      String configRaidId,
      @Nullable Integer raidLevel,
      Vec3 spawnCenter,
      long startTime,
      UUID bossUUID,
      @Nullable UUID mountUUID,
      @Nullable UUID targetPlayerUUID,
      Set<UUID> henchmenUUIDs,
      int spawnTimer,
      int totalSpawned,
      boolean isActive,
      boolean bossConfirmed
   ) {
      public ActiveRaidData(
         UUID raidId,
         String configRaidId,
         @Nullable Integer raidLevel,
         Vec3 spawnCenter,
         long startTime,
         UUID bossUUID,
         @Nullable UUID mountUUID,
         @Nullable UUID targetPlayerUUID,
         Set<UUID> henchmenUUIDs,
         int spawnTimer,
         int totalSpawned,
         boolean isActive,
         boolean bossConfirmed
      ) {
         this.raidId = raidId;
         this.configRaidId = configRaidId;
         this.raidLevel = raidLevel;
         this.spawnCenter = spawnCenter;
         this.startTime = startTime;
         this.bossUUID = bossUUID;
         this.mountUUID = mountUUID;
         this.targetPlayerUUID = targetPlayerUUID;
         this.henchmenUUIDs = new HashSet<>(henchmenUUIDs);
         this.spawnTimer = spawnTimer;
         this.totalSpawned = totalSpawned;
         this.isActive = isActive;
         this.bossConfirmed = bossConfirmed;
      }

      public CompoundTag save() {
         CompoundTag tag = new CompoundTag();
         tag.putUUID("RaidId", this.raidId);
         tag.putString("ConfigRaidId", this.configRaidId);
         if (this.raidLevel != null) {
            tag.putInt("RaidLevel", this.raidLevel);
         }

         tag.putDouble("SpawnX", this.spawnCenter.x);
         tag.putDouble("SpawnY", this.spawnCenter.y);
         tag.putDouble("SpawnZ", this.spawnCenter.z);
         tag.putLong("StartTime", this.startTime);
         tag.putUUID("BossUUID", this.bossUUID);
         if (this.mountUUID != null) {
            tag.putUUID("MountUUID", this.mountUUID);
         }

         if (this.targetPlayerUUID != null) {
            tag.putUUID("TargetPlayerUUID", this.targetPlayerUUID);
         }

         ListTag henchmenList = new ListTag();

         for (UUID uuid : this.henchmenUUIDs) {
            CompoundTag henchmanTag = new CompoundTag();
            henchmanTag.putUUID("UUID", uuid);
            henchmenList.add(henchmanTag);
         }

         tag.put("Henchmen", henchmenList);
         tag.putInt("SpawnTimer", this.spawnTimer);
         tag.putInt("TotalSpawned", this.totalSpawned);
         tag.putBoolean("IsActive", this.isActive);
         tag.putBoolean("BossConfirmed", this.bossConfirmed);
         return tag;
      }

      @Nullable
      public static RaidSaveData.ActiveRaidData load(CompoundTag tag) {
         try {
            UUID raidId = tag.getUUID("RaidId");
            String configRaidId = tag.getString("ConfigRaidId");
            Integer raidLevel = null;
            if (tag.contains("RaidLevel")) {
               raidLevel = tag.getInt("RaidLevel");
            }

            Vec3 spawnCenter = new Vec3(tag.getDouble("SpawnX"), tag.getDouble("SpawnY"), tag.getDouble("SpawnZ"));
            long startTime = tag.getLong("StartTime");
            UUID bossUUID = tag.getUUID("BossUUID");
            UUID mountUUID = tag.contains("MountUUID") ? tag.getUUID("MountUUID") : null;
            UUID targetPlayerUUID = tag.contains("TargetPlayerUUID") ? tag.getUUID("TargetPlayerUUID") : null;
            Set<UUID> henchmenUUIDs = new HashSet<>();
            ListTag henchmenList = tag.getList("Henchmen", 10);

            for (int i = 0; i < henchmenList.size(); i++) {
               CompoundTag henchmanTag = henchmenList.getCompound(i);
               henchmenUUIDs.add(henchmanTag.getUUID("UUID"));
            }

            int spawnTimer = tag.getInt("SpawnTimer");
            int totalSpawned = tag.getInt("TotalSpawned");
            boolean isActive = tag.getBoolean("IsActive");
            boolean bossConfirmed = tag.getBoolean("BossConfirmed");
            return new RaidSaveData.ActiveRaidData(
               raidId,
               configRaidId,
               raidLevel,
               spawnCenter,
               startTime,
               bossUUID,
               mountUUID,
               targetPlayerUUID,
               henchmenUUIDs,
               spawnTimer,
               totalSpawned,
               isActive,
               bossConfirmed
            );
         } catch (Exception var16) {
            RaidSaveData.LOGGER.warn("Failed to load active raid data: {}", var16.getMessage());
            return null;
         }
      }
   }

   public static record ScheduledRaidData(ResourceLocation dimension, UUID targetPlayerUUID, String raidId, long scheduledDay) {
      public ScheduledRaidData(ResourceLocation dimension, UUID targetPlayerUUID, String raidId, long scheduledDay) {
         this.dimension = dimension;
         this.targetPlayerUUID = targetPlayerUUID;
         this.raidId = raidId;
         this.scheduledDay = scheduledDay;
      }

      public CompoundTag save() {
         CompoundTag tag = new CompoundTag();
         tag.putString("Dimension", this.dimension.toString());
         tag.putUUID("PlayerUUID", this.targetPlayerUUID);
         tag.putString("RaidId", this.raidId);
         tag.putLong("ScheduledDay", this.scheduledDay);
         return tag;
      }

      @Nullable
      public static RaidSaveData.ScheduledRaidData load(CompoundTag tag) {
         try {
            ResourceLocation dimension = ResourceLocation.parse(tag.getString("Dimension"));
            UUID playerUUID = tag.getUUID("PlayerUUID");
            String raidId = tag.getString("RaidId");
            long scheduledDay = tag.getLong("ScheduledDay");
            return new RaidSaveData.ScheduledRaidData(dimension, playerUUID, raidId, scheduledDay);
         } catch (Exception var6) {
            RaidSaveData.LOGGER.warn("Failed to load scheduled raid data: {}", var6.getMessage());
            return null;
         }
      }
   }
}
