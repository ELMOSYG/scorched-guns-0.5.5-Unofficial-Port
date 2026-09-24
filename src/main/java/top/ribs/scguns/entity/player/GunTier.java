package top.ribs.scguns.entity.player;

import java.util.ArrayList;
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
