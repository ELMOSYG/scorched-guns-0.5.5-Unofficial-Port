package top.ribs.scguns.compat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import top.ribs.scguns.ScorchedGuns;
import top.ribs.scguns.item.GunItem;

/**
 * Keeps Punchy's item blacklist in step with this mod's guns.
 *
 * <p>Punchy animates whatever is held, and it only knows about the gun mods it was written for: its TaCZ
 * support is hardcoded in its own jar ({@code punchy.compat.TaczBlacklistCompat} reflects into TaCZ's
 * API), and there is no registration hook for anyone else. Its blacklist does accept wildcards
 * ({@code PunchyConfig#matchesBlacklistEntry} tests for {@code *} and {@code ?}), so {@code scguns:*}
 * would work - but that also covers ammo, attachments, armour, bayonets and every block, and those are
 * exactly the items that should keep their normal animations. So the entries here are computed from the
 * registry instead: every item whose class is a {@link GunItem}, which is the same test the rest of the
 * mod uses to decide whether something is a gun.</p>
 *
 * <p>The file is only ever edited when it already exists. Punchy stores its settings as plain fields
 * deserialised by GSON, so a file we invented would leave every flag at its Java default - false - and
 * silently turn Punchy's features off. If the player has not opened Punchy's config screen yet there is
 * nothing here to edit and the run is skipped.</p>
 *
 * <p>Registered from {@code ClientHandler#onClientSetup}, so it runs once per launch, client side only.
 * Punchy reads its config at startup, so a list written during this launch takes effect from the next
 * one.</p>
 */
public final class PunchyBlacklistCompat {
   private static final String PUNCHY_MOD_ID = "punchy";
   private static final String CONFIG_FILE = "punchy_config.json";
   private static final String BLACKLIST_KEY = "itemBlacklist";

   private PunchyBlacklistCompat() {
   }

   public static void onClientSetup() {
      if (!ModList.get().isLoaded(PUNCHY_MOD_ID)) {
         return;
      }

      Path config = findConfig();
      if (config == null) {
         ScorchedGuns.LOGGER.info(
            "Punchy is installed but has no {} yet - open its config screen once and this mod will add "
               + "its guns to the blacklist on the next launch.", CONFIG_FILE);
         return;
      }

      Set<String> guns = gunIds();
      if (guns.isEmpty()) {
         return;
      }

      try {
         JsonObject root = JsonParser.parseString(Files.readString(config, StandardCharsets.UTF_8))
            .getAsJsonObject();
         JsonArray list = root.has(BLACKLIST_KEY) && root.get(BLACKLIST_KEY).isJsonArray()
            ? root.getAsJsonArray(BLACKLIST_KEY)
            : new JsonArray();

         Set<String> present = new LinkedHashSet<>();
         for (JsonElement element : list) {
            if (element.isJsonPrimitive()) {
               present.add(element.getAsString());
            }
         }

         List<String> added = new ArrayList<>();
         for (String gun : guns) {
            if (present.add(gun)) {
               list.add(gun);
               added.add(gun);
            }
         }

         if (added.isEmpty()) {
            return;
         }

         root.add(BLACKLIST_KEY, list);
         Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
         Files.writeString(config, gson.toJson(root) + System.lineSeparator(), StandardCharsets.UTF_8);
         ScorchedGuns.LOGGER.info("Added {} scguns gun(s) to Punchy's blacklist in {} (effective next "
            + "launch). First few: {}", added.size(), config, added.subList(0, Math.min(5, added.size())));
      } catch (IOException | RuntimeException e) {
         ScorchedGuns.LOGGER.warn("Could not update Punchy's blacklist at {}: {}", config, e.toString());
      }
   }

   /** {@code config/punchy/punchy_config.json} where Punchy puts it, plus the flat path just in case. */
   private static Path findConfig() {
      Path dir = FMLPaths.CONFIGDIR.get();
      for (Path candidate : List.of(dir.resolve("punchy").resolve(CONFIG_FILE), dir.resolve(CONFIG_FILE))) {
         if (Files.isRegularFile(candidate)) {
            return candidate;
         }
      }
      return null;
   }

   /** Every registered item that is a gun. Not a wildcard: ammo, attachments and armour are not guns. */
   private static Set<String> gunIds() {
      Set<String> ids = new LinkedHashSet<>();
      for (Item item : BuiltInRegistries.ITEM) {
         if (item instanceof GunItem) {
            ids.add(BuiltInRegistries.ITEM.getKey(item).toString());
         }
      }
      return ids;
   }
}
