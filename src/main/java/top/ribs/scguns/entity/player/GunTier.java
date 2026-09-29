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
    * <p>This method used to add {@code this} at the end (section 82.29), on the theory that a
    * gunner should be able to carry the newest tier. The player rejected that: mobs lag a tier
    * behind, and 0.5.5 always had them do so. The notice was built on the same wrong theory and
    * has been corrected to match - see {@link #getAvailableMobTiersNewestFirst()}, which orders
    * this same list for display and adds nothing.</p>
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
    * The same tiers, <b>newest first</b>, for the unlock notice: the same set
    * {@link #getAvailableMobTiers()} gives, only ordered for a sentence.
    *
    * <p>It deliberately does <b>not</b> add this tier, because mobs carrying the tier you have
    * just obtained do not exist: the spawner equips from {@link #getAvailableMobTiers()}, which
    * is 0.5.5's "the tiers below the one you have reached". A notice that added this tier would
    * be promising enemies that never spawn - at the antique tier, where the previous list is
    * empty, it announced antique enemies would appear and none ever do. The player caught that
    * one on screen.</p>
    *
    * <p>So this is a display ordering, not a second answer to a second question. Sorting by level
    * rather than by the declared previous-tier order: that order happens to be newest first today,
    * but the sentence must not depend on how the table was typed.</p>
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
