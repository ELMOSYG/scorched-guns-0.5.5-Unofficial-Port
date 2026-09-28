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
    * Every tier a gunner mob may be equipped from once this tier is unlocked: the previous tiers, then this
    * one (HANDOFF section 82.29).
    *
    * <p>This used to return {@code previousTierIds} alone, leaving the tier itself out. Two things followed:
    * unlocking a tier announced the tier <em>below</em> it - "【antique】 enemies may now appear" immediately
    * after obtaining frontier guns - and the spawner, which reads this same list, could never equip a gunner
    * with the newest tier's weapons, so the newest tier was permanently unreachable.</p>
    *
    * <p>This tier goes last on purpose: {@code equipProgressionGun} weights the list by position, taking the
    * front half most of the time and the tail rarely, which keeps the newest tier the rare one.</p>
    */
   public List<GunTier> getAvailableMobTiers() {
      List<GunTier> tiers = new ArrayList<>();

      for (String id : this.previousTierIds) {
         GunTier tier = GunTierRegistry.getTier(id);
         if (tier != null) {
            tiers.add(tier);
         }
      }

      // The "none" tier (level 0) has no weapons and must never be offered to the spawner.
      if (this.level > 0) {
         tiers.add(this);
      }

      return tiers;
   }

   /**
    * The same tiers, newest first, for the unlock message: the tier just obtained should read first in a
    * sentence about what just became possible (HANDOFF section 82.29).
    *
    * <p>Sorted by level rather than by the declared previous-tier order: that order happens to be newest first
    * today, but the message must not depend on how the table was typed.</p>
    */
   public List<GunTier> getAvailableMobTiersNewestFirst() {
      List<GunTier> tiers = this.getAvailableMobTiers();
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
