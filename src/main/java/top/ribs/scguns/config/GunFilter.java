package top.ribs.scguns.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.ribs.scguns.ScorchedGuns;
import top.ribs.scguns.entity.player.GunTier;
import top.ribs.scguns.entity.player.GunTierRegistry;

/**
 * Restricts which gun tiers a pack offers.
 *
 * <p>Written for theme packs: a Napoleonic pack wants only {@code antique} guns, a western pack only {@code frontier}.
 * Empty {@code allowed_tiers} means "no restriction", which is the default and keeps the mod's normal behaviour.</p>
 *
 * <p>This only limits how a gun is <em>obtained</em>. Guns that already exist keep working, so an existing world is
 * never damaged by turning a tier off. The switch lives in {@code config/scguns-gunfilter.properties} rather than the
 * normal config screen because it is a pack authoring tool, not a player option.</p>
 */
public final class GunFilter {

   private static final String FILE_NAME = "scguns-gunfilter.properties";

   private static final Set<String> ALLOWED_TIERS = new LinkedHashSet<>();
   private static final Set<String> DISABLED_GUNS = new LinkedHashSet<>();

   private GunFilter() {
   }

   /** Called once, early, so the lists are ready before anything asks. */
   public static void load() {
      ALLOWED_TIERS.clear();
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

         for (String tier : split(properties.getProperty("allowed_tiers", ""))) {
            ALLOWED_TIERS.add(tier.toLowerCase(Locale.ROOT));
         }

         for (String gun : split(properties.getProperty("disabled_guns", ""))) {
            DISABLED_GUNS.add(gun.toLowerCase(Locale.ROOT));
         }

         ScorchedGuns.LOGGER.info("scguns: gun filter - allowed tiers {}, disabled guns {}",
            ALLOWED_TIERS.isEmpty() ? "(all)" : ALLOWED_TIERS, DISABLED_GUNS.isEmpty() ? "(none)" : DISABLED_GUNS);
      } catch (IOException failure) {
         ScorchedGuns.LOGGER.warn("scguns: could not read {}, no tier filtering is applied", FILE_NAME, failure);
      }
   }

   private static void writeDefault(Path path) throws IOException {
      try (OutputStream out = Files.newOutputStream(path)) {
         out.write(("# Restrict which gun tiers can be obtained.\n"
            + "# Leave allowed_tiers empty to allow every tier.\n"
            + "# Example, Napoleonic pack: allowed_tiers=antique\n"
            + "# Example, western pack:    allowed_tiers=frontier\n"
            + "# Tier ids: none, antique, frontier, copper, iron, wrecker, treated_brass,\n"
            + "#           ocean, deep_dark, end, scorched, diamond_steel, piglin, exo_suit\n"
            + "allowed_tiers=\n"
            + "# Individual guns to remove from every source, e.g. disabled_guns=scguns:defender_pistol,scguns:ak\n"
            + "disabled_guns=\n").getBytes("UTF-8"));
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

   public static boolean hasRestriction() {
      return !ALLOWED_TIERS.isEmpty() || !DISABLED_GUNS.isEmpty();
   }

   /** Whether a gun stack may be handed out by any source. */
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

      if (ALLOWED_TIERS.isEmpty()) {
         return true;
      }

      // A gun may sit in several tier tags; being in any allowed one is enough.
      for (String tierId : ALLOWED_TIERS) {
         GunTier tier = GunTierRegistry.getTier(tierId);
         if (tier == null || tier.getTagName() == null) {
            continue;
         }

         net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tierTag = net.minecraft.tags.ItemTags.create(
            ResourceLocation.fromNamespaceAndPath("scguns", tier.getTagName()));
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
