package top.ribs.scguns.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.ribs.scguns.ScorchedGuns;
import top.ribs.scguns.entity.player.GunTier;
import top.ribs.scguns.entity.player.GunTierRegistry;

/**
 * Restricts which gun tiers a pack offers, so a themed pack can ship only the guns it wants.
 *
 * <p>A Napoleonic pack keeps {@code antique}, a western pack keeps {@code frontier}; the file
 * {@code config/scguns-gunfilter.properties} lists every blueprint the mod ships with a switch for each.
 * Turning a blueprint off only stops its guns from being obtained - guns that already exist keep working, so an
 * existing world is never broken by editing this file.</p>
 */
public final class GunFilter {

   private static final String FILE_NAME = "scguns-gunfilter.properties";

   private static final List<String> BLUEPRINT_TIERS = List.of(
      "antique", "frontier", "copper", "iron", "wrecker", "treated_brass",
      "diamond_steel", "ocean", "piglin", "deep_dark", "end", "scorched", "exo_suit");

   private static final Set<String> ALLOWED_TIERS = new LinkedHashSet<>();
   private static final Set<String> ENABLED_BLUEPRINTS = new LinkedHashSet<>();
   private static final Set<String> DISABLED_GUNS = new LinkedHashSet<>();

   /** Lite mode: guns and ammo only. Individual switches below can override the umbrella flag. */
   private static boolean liteMode;
   private static boolean liteDisableStructures;
   private static boolean liteDisableGunnerMobs;
   private static boolean liteDisableMobGunEquipment;
   private static boolean liteDisableProgressionMessages;

   private GunFilter() {
   }

   public static void load() {
      ALLOWED_TIERS.clear();
      ENABLED_BLUEPRINTS.clear();
      DISABLED_GUNS.clear();

      Path path = Path.of("config", FILE_NAME);
      try {
         if (!Files.exists(path)) {
            Files.createDirectories(path.getParent());
            writeDefault(path);
         }

         Properties properties = new Properties();
         try (InputStream in = Files.newInputStream(path)) {
            properties.load(in);
         }

         for (String tier : BLUEPRINT_TIERS) {
            if (Boolean.parseBoolean(properties.getProperty("blueprint." + tier, "true"))) {
               ENABLED_BLUEPRINTS.add(tier);
            }
         }

         for (String tier : split(properties.getProperty("allowed_tiers", ""))) {
            ALLOWED_TIERS.add(tier.toLowerCase(Locale.ROOT));
         }

         liteMode = Boolean.parseBoolean(properties.getProperty("lite_mode", "false"));
         liteDisableStructures = liteMode || Boolean.parseBoolean(properties.getProperty("lite.disable_structures", "false"));
         liteDisableGunnerMobs = liteMode || Boolean.parseBoolean(properties.getProperty("lite.disable_gunner_mobs", "false"));
         liteDisableMobGunEquipment = liteMode || Boolean.parseBoolean(properties.getProperty("lite.disable_mob_gun_equipment", "false"));
         liteDisableProgressionMessages = liteMode
            || Boolean.parseBoolean(properties.getProperty("lite.disable_progression_messages", "false"));

         for (String gun : split(properties.getProperty("disabled_guns", ""))) {
            DISABLED_GUNS.add(gun.toLowerCase(Locale.ROOT));
         }

         ScorchedGuns.LOGGER.info("scguns: gun filter - blueprints {}, override {}, disabled guns {}",
            ENABLED_BLUEPRINTS.size() == BLUEPRINT_TIERS.size() ? "(all on)" : ENABLED_BLUEPRINTS,
            ALLOWED_TIERS.isEmpty() ? "(from blueprints)" : ALLOWED_TIERS,
            DISABLED_GUNS.isEmpty() ? "(none)" : DISABLED_GUNS);
         // Raids already have their own switch in the normal config, so lite mode does not duplicate it.
         ScorchedGuns.LOGGER.info(
            "scguns: lite mode {} (structures {}, gunner mobs {}, mob guns {}, progression chat {})",
            liteMode, liteDisableStructures, liteDisableGunnerMobs, liteDisableMobGunEquipment,
            liteDisableProgressionMessages);
      } catch (IOException failure) {
         ScorchedGuns.LOGGER.warn("scguns: could not read {}, no tier filtering is applied", FILE_NAME, failure);
      }
   }

   private static void writeDefault(Path path) throws IOException {
      StringBuilder text = new StringBuilder();
      text.append("# Which blueprints this pack offers. Every blueprint the mod ships is listed below.").append("\n");
      text.append("# false stops that blueprint's guns from being obtained (existing guns keep working).").append("\n");
      text.append("# Napoleonic pack: keep antique true, set the rest false.").append("\n");
      text.append("# Western pack:    keep frontier true, set the rest false.").append("\n");
      text.append("\n");
      text.append("# Lite mode: guns and ammo only. Turns off structures, gunner mobs, mob gun equipment").append("\n");
      text.append("# and the progression chat lines. Set it back to false to get the full mod.").append("\n");
      text.append("lite_mode=false").append("\n");
      text.append("# Fine grained overrides (each is implied by lite_mode=true).").append("\n");
      text.append("lite.disable_structures=false").append("\n");
      text.append("lite.disable_gunner_mobs=false").append("\n");
      text.append("lite.disable_mob_gun_equipment=false").append("\n");
      text.append("lite.disable_progression_messages=false").append("\n");
      text.append("\n");
      text.append("# Optional override: name tiers here to ignore the blueprint switches, e.g. allowed_tiers=antique").append("\n");
      text.append("allowed_tiers=").append("\n");
      text.append("\n");
      text.append("# Optional: individual guns to remove from every source, e.g. scguns:defender_pistol,scguns:ak").append("\n");
      text.append("disabled_guns=").append("\n");
      text.append("\n");
      for (String tier : BLUEPRINT_TIERS) {
         text.append("blueprint.").append(tier).append("=true").append("\n");
      }
      try (OutputStream out = Files.newOutputStream(path)) {
         out.write(text.toString().getBytes(StandardCharsets.UTF_8));
      }
   }

   private static List<String> split(@Nullable String value) {
      List<String> out = new ArrayList<>();
      if (value == null || value.isBlank()) {
         return out;
      }
      for (String part : value.split(",")) {
         String trimmed = part.trim();
         if (!trimmed.isEmpty()) {
            out.add(trimmed);
         }
      }
      return out;
   }

   /** Tiers to allow: the explicit override when given, otherwise the blueprints that are switched on. */
   private static Set<String> effectiveTiers() {
      if (!ALLOWED_TIERS.isEmpty()) {
         return ALLOWED_TIERS;
      }
      return ENABLED_BLUEPRINTS.size() == BLUEPRINT_TIERS.size() ? Collections.emptySet() : ENABLED_BLUEPRINTS;
   }

   public static boolean liteMode() {
      return liteMode;
   }

   public static boolean liteDisablesStructures() {
      return liteDisableStructures;
   }

   public static boolean liteDisablesGunnerMobs() {
      return liteDisableGunnerMobs;
   }

   public static boolean liteDisablesMobGunEquipment() {
      return liteDisableMobGunEquipment;
   }

   public static boolean liteDisablesProgressionMessages() {
      return liteDisableProgressionMessages;
   }

   public static boolean hasRestriction() {
      return !effectiveTiers().isEmpty() || !DISABLED_GUNS.isEmpty();
   }

   public static boolean isAllowed(ItemStack stack) {
      if (!hasRestriction() || stack.isEmpty()) {
         return true;
      }

      Item item = stack.getItem();
      ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
      if (DISABLED_GUNS.contains(id.toString().toLowerCase(Locale.ROOT))
         || DISABLED_GUNS.contains(id.getPath().toLowerCase(Locale.ROOT))) {
         return false;
      }

      Set<String> tiers = effectiveTiers();
      if (tiers.isEmpty()) {
         return true;
      }

      for (String tierId : tiers) {
         GunTier tier = GunTierRegistry.getTier(tierId);
         if (tier == null || tier.getTagName() == null) {
            continue;
         }
         TagKey<Item> tierTag = ItemTags.create(ResourceLocation.fromNamespaceAndPath("scguns", tier.getTagName()));
         if (stack.is(tierTag)) {
            return true;
         }
      }

      return false;
   }

   public static boolean isAllowed(Item item) {
      return isAllowed(new ItemStack(item));
   }

   public static List<Item> filter(List<Item> weapons) {
      if (!hasRestriction() || weapons.isEmpty()) {
         return weapons;
      }
      List<Item> kept = new ArrayList<>(weapons.size());
      for (Item weapon : weapons) {
         if (isAllowed(weapon)) {
            kept.add(weapon);
         }
      }
      return kept;
   }
}
