package top.ribs.scguns.entity.player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GunTierRegistry {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final Map<String, GunTier> TIERS_BY_ID = new LinkedHashMap<>();
   private static final Map<Integer, List<GunTier>> TIERS_BY_LEVEL = new HashMap<>();
   private static boolean isLocked = false;

   public GunTierRegistry() {
      super();
   }

   public static GunTier register(String id, int level, @Nullable String tagName, int raidLevel) {
      if (isLocked) {
         throw new IllegalStateException("Cannot register tiers after initialization is complete!");
      } else if (TIERS_BY_ID.containsKey(id)) {
         return TIERS_BY_ID.get(id);
      } else {
         GunTier tier = new GunTier(id, level, tagName, raidLevel);
         TIERS_BY_ID.put(id, tier);
         TIERS_BY_LEVEL.computeIfAbsent(level, k -> new ArrayList<>()).add(tier);
         return tier;
      }
   }

   public static void lock() {
      isLocked = true;
   }

   @Nullable
   public static GunTier getTier(String id) {
      return TIERS_BY_ID.get(id);
   }

   @Nullable
   public static GunTier getTierByLevel(int level) {
      List<GunTier> tiersAtLevel = TIERS_BY_LEVEL.get(level);
      return tiersAtLevel != null && !tiersAtLevel.isEmpty() ? tiersAtLevel.get(0) : null;
   }

   public static List<GunTier> getTiersAtLevel(int level) {
      return TIERS_BY_LEVEL.getOrDefault(level, Collections.emptyList());
   }

   public static Collection<GunTier> getAllTiers() {
      return Collections.unmodifiableCollection(TIERS_BY_ID.values());
   }

   public static int getMaxLevel() {
      return TIERS_BY_LEVEL.keySet().stream().max(Integer::compareTo).orElse(0);
   }

   public static int getMaxRaidLevel() {
      return TIERS_BY_ID.values().stream().mapToInt(GunTier::getRaidLevel).max().orElse(0);
   }

   @Nullable
   public static GunTier getHighestTier() {
      int maxLevel = getMaxLevel();
      if (maxLevel == 0) {
         return null;
      } else {
         List<GunTier> tiersAtMax = TIERS_BY_LEVEL.get(maxLevel);
         return tiersAtMax != null && !tiersAtMax.isEmpty()
            ? tiersAtMax.stream().max(Comparator.comparingInt(GunTier::getRaidLevel)).orElse(tiersAtMax.get(0))
            : null;
      }
   }

   public static List<GunTier> getTiersUpToLevel(int maxLevel) {
      List<GunTier> result = new ArrayList<>();

      for (int level = 0; level <= maxLevel; level++) {
         result.addAll(TIERS_BY_LEVEL.getOrDefault(level, Collections.emptyList()));
      }

      return result;
   }

   public static boolean isRegistered(String id) {
      return TIERS_BY_ID.containsKey(id);
   }

   static void clear() {
      if (isLocked) {
         LOGGER.warn("Attempting to clear locked tier registry. This should only happen during testing!");
      }

      TIERS_BY_ID.clear();
      TIERS_BY_LEVEL.clear();
      isLocked = false;
   }
}
