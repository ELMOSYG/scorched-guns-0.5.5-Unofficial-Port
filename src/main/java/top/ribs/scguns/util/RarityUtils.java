package top.ribs.scguns.util;


import net.minecraft.core.registries.BuiltInRegistries;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.core.Registry;

public class RarityUtils {
   private static final Map<ResourceLocation, Rarity> ITEM_RARITY_MAP = new HashMap<>();

   public RarityUtils() {
      super();
   }

   public static Rarity GetRarityFromResourceLocation(ResourceLocation location, Rarity oldRarity) {
      return ITEM_RARITY_MAP.getOrDefault(location, oldRarity);
   }

   public static Rarity GetRarityFromItem(Item item, Rarity old) {
      Registry<Item> items = BuiltInRegistries.ITEM;
      return items.containsValue(item) ? GetRarityFromResourceLocation(items.getKey(item), old) : old;
   }

   static {
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "m22_waltz"), Constants.TREATED_BRASS);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "waltz_conversion"), Constants.TREATED_BRASS);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "osgood_50"), Constants.TREATED_BRASS);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "grandle_og"), Constants.TREATED_BRASS);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "grandle"), Constants.TREATED_BRASS);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "cogloader"), Constants.TREATED_BRASS);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "gale"), Constants.TREATED_BRASS);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "jr_wristbreaker"), Constants.TREATED_BRASS);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "jackhammer"), Constants.TREATED_BRASS);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "howler"), Constants.TREATED_BRASS);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "howler_conversion"), Constants.TREATED_BRASS);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "gauss_rifle"), Constants.TREATED_BRASS);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "libertas"), Constants.TREATED_BRASS);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "niami"), Constants.TREATED_BRASS);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "spitfire"), Constants.TREATED_BRASS);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "gattaler"), Constants.TREATED_BRASS);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "thunderhead"), Constants.TREATED_BRASS);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "scratches"), Constants.TREATED_BRASS);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "hammer_gl"), Constants.TREATED_BRASS);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "cr4k_mining_laser"), Constants.TREATED_BRASS);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "dozier_rl"), Constants.TREATED_BRASS);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "treated_brass_blueprint"), Constants.TREATED_BRASS);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "krauser"), Constants.DIAMOND_STEEL);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "soul_drummer"), Constants.DIAMOND_STEEL);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "uppercut"), Constants.DIAMOND_STEEL);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "micina"), Constants.DIAMOND_STEEL);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "valora"), Constants.DIAMOND_STEEL);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "prush_gun"), Constants.DIAMOND_STEEL);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "drill"), Constants.DIAMOND_STEEL);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "drill_conversion"), Constants.DIAMOND_STEEL);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "lockewood"), Constants.DIAMOND_STEEL);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "zilk_45"), Constants.DIAMOND_STEEL);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "rg_jigsaw"), Constants.DIAMOND_STEEL);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "nailer"), Constants.DIAMOND_STEEL);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "inertial"), Constants.DIAMOND_STEEL);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "mas_55"), Constants.DIAMOND_STEEL);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "mas_peddler"), Constants.DIAMOND_STEEL);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "minksy"), Constants.DIAMOND_STEEL);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "inquisitor"), Constants.DIAMOND_STEEL);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "plasgun"), Constants.DIAMOND_STEEL);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "truant"), Constants.DIAMOND_STEEL);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "cyclone"), Constants.DIAMOND_STEEL);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "shard_culler"), Constants.DIAMOND_STEEL);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "diamond_steel_blueprint"), Constants.DIAMOND_STEEL);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "hyperbaria"), Constants.OCEANIC);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "floundergat"), Constants.OCEANIC);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "marlin"), Constants.OCEANIC);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "bomb_lance"), Constants.OCEANIC);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "ocean_blueprint"), Constants.OCEANIC);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "sequoia"), Constants.OCEANIC);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "hullbreaker"), Constants.OCEANIC);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "spirulida"), Constants.OCEANIC);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "super_shotgun"), Constants.PIGLISH);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "blasphemy"), Constants.PIGLISH);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "freyr"), Constants.PIGLISH);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "pyroclastic_flow"), Constants.PIGLISH);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "vulcanic_repeater"), Constants.PIGLISH);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "piglin_blueprint"), Constants.PIGLISH);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "mangalitsa"), Constants.PIGLISH);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "trotters"), Constants.PIGLISH);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "hog_round"), Constants.PIGLISH);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "whispers"), Constants.DEEP_DARK);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "echoes_2"), Constants.DEEP_DARK);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "sculk_resonator"), Constants.DEEP_DARK);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "deep_dark_blueprint"), Constants.DEEP_DARK);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "forlorn_hope"), Constants.DEEP_DARK);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "sculk_cell"), Constants.DEEP_DARK);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "end_blueprint"), Constants.ENDISH);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "lone_wonder"), Constants.ENDISH);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "raygun"), Constants.ENDISH);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "dark_matter"), Constants.ENDISH);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "shellurker"), Constants.ENDISH);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "carapice"), Constants.ENDISH);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "weevil"), Constants.ENDISH);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "shulkshot"), Constants.ENDISH);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "scorched_blueprint"), Constants.SCORCHED);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "scorched_ingot"), Constants.SCORCHED);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "earths_corpse"), Constants.SCORCHED);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "rat_king_and_queen"), Constants.SCORCHED);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "locust"), Constants.SCORCHED);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "newborn_cyst"), Constants.SCORCHED);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "astella"), Constants.SCORCHED);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "flayed_god"), Constants.SCORCHED);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "nervepinch"), Constants.SCORCHED);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "prima_materia"), Constants.SCORCHED);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "terra_incognita"), Constants.SCORCHED);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "sterilizer"), Constants.SCORCHED);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "advanced_exo_suit_core"), Constants.SCORCHED);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "the_pact"), Constants.SCORCHED);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "music_disc_mass_destruction"), Constants.SCORCHED);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "music_disc_mass_destruction_extended"), Constants.SCORCHED);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "felix_memorial"), Constants.PIGLISH);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "kiln_gun"), Constants.BIZARRE);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "ultra_knight_hawk"), Constants.BIZARRE);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "whizzbanger"), Constants.BIZARRE);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "big_bore"), Constants.BIZARRE);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "osborne_slug"), Constants.BIZARRE);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "ribs_glory"), Constants.PIGLISH);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "rusty_medal"), Constants.RUSTY);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "labor_trophy"), Constants.BIZARRE);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "snapped_cogwheel"), Constants.WRECKER);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "ceremonial_cod"), Constants.DIAMOND_STEEL);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "cog_heart"), Constants.TREATED_BRASS);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "gold_idol"), Constants.PIGLISH);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "leviathan_tooth"), Constants.OCEANIC);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "sculk_tome"), Constants.DEEP_DARK);
      ITEM_RARITY_MAP.put(ResourceLocation.fromNamespaceAndPath("scguns", "shulker_core"), Constants.ENDISH);
   }
}
