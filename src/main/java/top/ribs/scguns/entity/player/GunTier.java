package top.ribs.scguns.entity.player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GunTier {
   private final String id;
   private final int level;
   private final String tagName;
   private final int raidLevel;
   private final List<String> previousTierIds;

   public GunTier(String id, int level, String tagName, int raidLevel) {
      super();
      this.id = id;
      this.level = level;
      this.tagName = tagName;
      this.raidLevel = raidLevel;
      this.previousTierIds = new ArrayList<>();
   }

   public String getId() {
      return this.id;
   }

   public int getLevel() {
      return this.level;
   }

   public String getTagName() {
      return this.tagName;
   }

   public int getRaidLevel() {
      return this.raidLevel;
   }

   public List<String> getPreviousTierIds() {
      return new ArrayList<>(this.previousTierIds);
   }

   public GunTier addPreviousTier(String tierId) {
      if (!this.previousTierIds.contains(tierId)) {
         this.previousTierIds.add(tierId);
      }

      return this;
   }

   /**
    * Every tier a gunner mob may be equipped from once this tier is unlocked: <b>the previous
    * tiers, and not this one</b> (HANDOFF section 83).
    *
    * <p>That is 0.5.5's rule, unchanged, and the spawner depends on it: a player who has only
    * reached 古典 has an empty list here, so no gunner spawns at all, and a gunner that does
    * appear carries a weapon from a tier the player has already worked through. Including this
    * tier made the mobs keep pace with the player instead - the report that prompted this asked
    * for exactly that lag back.</p>
    *
    * <p>This method used to add {@code this} at the end (section 82.29), which was right for the
    * unlock message and wrong here, because {@code GunnerMobSpawner} reads this same list. The
    * two questions now have their own methods: this one answers "what may a mob carry", and
    * {@link #getUnlockedTiersNewestFirst()} answers "what has just become possible".</p>
    */
   public List<GunTier> getAvailableMobTiers() {
      List<GunTier> tiers = new ArrayList<>();

      for (String id : this.previousTierIds) {
         GunTier tier = GunTierRegistry.getTier(id);
         if (tier != null) {
            tiers.add(tier);
         }
      }

      return tiers;
   }

   /**
    * The tiers this unlock has made reachable, <b>including this one</b>, newest first: the tier
    * just obtained should read first in a sentence about what just became possible.
    *
    * <p>Separate from {@link #getAvailableMobTiers()} on purpose. The notice says "you may now
    * see 【X】 enemies" about the tier just picked up, so it needs this tier; the spawner must
    * not have it, or enemies would match the player's tier the moment it is reached.</p>
    *
    * <p>Sorted by level rather than by the declared previous-tier order: that order happens to be
    * newest first today, but the message must not depend on how the table was typed.</p>
    */
   public List<GunTier> getUnlockedTiersNewestFirst() {
      List<GunTier> tiers = this.getAvailableMobTiers();

      // The "none" tier (level 0) has no weapons and must never be announced.
      if (this.level > 0) {
         tiers.add(this);
      }

      tiers.sort(Comparator.comparingInt(GunTier::getLevel).reversed());
      return tiers;
   }

   @Override
   public String toString() {
      return "GunTier{id='" + this.id + "', level=" + this.level + "}";
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      } else {
         return obj instanceof GunTier other ? this.id.equals(other.id) : false;
      }
   }

   @Override
   public int hashCode() {
      return this.id.hashCode();
   }
}
