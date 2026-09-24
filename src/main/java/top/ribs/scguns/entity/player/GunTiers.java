package top.ribs.scguns.entity.player;

public class GunTiers {
   public static final GunTier NONE = GunTierRegistry.register("none", 0, null, 0);
   public static final GunTier ANTIQUE = GunTierRegistry.register("antique", 1, "antique_gun_tier", 1);
   public static final GunTier FRONTIER = GunTierRegistry.register("frontier", 2, "frontier_gun_tier", 1).addPreviousTier("antique");
   public static final GunTier COPPER = GunTierRegistry.register("copper", 3, "copper_gun_tier", 2).addPreviousTier("frontier").addPreviousTier("antique");
   public static final GunTier IRON = GunTierRegistry.register("iron", 4, "iron_gun_tier", 3)
      .addPreviousTier("copper")
      .addPreviousTier("frontier")
      .addPreviousTier("antique");
   public static final GunTier WRECKER = GunTierRegistry.register("wrecker", 5, "wrecker_gun_tier", 3)
      .addPreviousTier("copper")
      .addPreviousTier("frontier")
      .addPreviousTier("antique");
   public static final GunTier OCEAN = GunTierRegistry.register("ocean", 5, "ocean_gun_tier", 3)
      .addPreviousTier("copper")
      .addPreviousTier("frontier")
      .addPreviousTier("antique");
   public static final GunTier DIAMOND_STEEL = GunTierRegistry.register("diamond_steel", 6, "diamond_steel_gun_tier", 4)
      .addPreviousTier("iron")
      .addPreviousTier("copper")
      .addPreviousTier("frontier")
      .addPreviousTier("antique");
   public static final GunTier TREATED_BRASS = GunTierRegistry.register("treated_brass", 6, "treated_brass_gun_tier", 4)
      .addPreviousTier("iron")
      .addPreviousTier("copper")
      .addPreviousTier("frontier")
      .addPreviousTier("antique");
   public static final GunTier PIGLIN = GunTierRegistry.register("piglin", 6, "piglin_gun_tier", 4)
      .addPreviousTier("iron")
      .addPreviousTier("copper")
      .addPreviousTier("frontier")
      .addPreviousTier("antique");
   public static final GunTier DEEP_DARK = GunTierRegistry.register("deep_dark", 6, "deep_dark_gun_tier", 4)
      .addPreviousTier("iron")
      .addPreviousTier("copper")
      .addPreviousTier("frontier")
      .addPreviousTier("antique");
   public static final GunTier END = GunTierRegistry.register("end", 7, "end_gun_tier", 5)
      .addPreviousTier("diamond_steel")
      .addPreviousTier("treated_brass")
      .addPreviousTier("iron")
      .addPreviousTier("copper")
      .addPreviousTier("frontier")
      .addPreviousTier("antique");
   public static final GunTier SCORCHED = GunTierRegistry.register("scorched", 7, "scorched_gun_tier", 5)
      .addPreviousTier("diamond_steel")
      .addPreviousTier("treated_brass")
      .addPreviousTier("iron")
      .addPreviousTier("copper")
      .addPreviousTier("frontier")
      .addPreviousTier("antique");

   public GunTiers() {
      super();
   }

   public static void init() {
   }
}
