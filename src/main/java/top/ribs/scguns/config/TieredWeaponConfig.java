package top.ribs.scguns.config;


import net.minecraft.core.registries.BuiltInRegistries;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import top.ribs.scguns.entity.player.GunTier;
import top.ribs.scguns.entity.player.GunTierRegistry;

@EventBusSubscriber(
   modid = "scguns"
)
public class TieredWeaponConfig {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final Map<String, List<Item>> TIER_WEAPONS = new HashMap<>();
   private static final ResourceLocation CONFIG_LOCATION = ResourceLocation.fromNamespaceAndPath("scguns", "entity/tier_weapons.json");

   public TieredWeaponConfig() {
      super();
   }

   public static void loadConfig(ResourceManager resourceManager) {
      TIER_WEAPONS.clear();

      try {
         Resource resource = (Resource)resourceManager.getResource(CONFIG_LOCATION).orElse(null);
         if (resource != null) {
            try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
               Gson gson = new Gson();
               JsonObject json = (JsonObject)gson.fromJson(reader, JsonObject.class);
               if (json != null) {
                  for (Entry<String, JsonElement> entry : json.entrySet()) {
                     String tierIdOrName = entry.getKey();
                     JsonArray weaponsArray = entry.getValue().getAsJsonArray();
                     GunTier tier = GunTierRegistry.getTier(tierIdOrName.toLowerCase());
                     if (tier == null) {
                        LOGGER.warn("Unknown tier in tier_weapons.json: {}", tierIdOrName);
                     } else {
                        List<Item> weapons = new ArrayList<>();

                        for (JsonElement weaponElement : weaponsArray) {
                           String weaponId = weaponElement.getAsString();
                           Item weapon = (Item)BuiltInRegistries.ITEM.get(ResourceLocation.parse(weaponId));
                           if (weapon != null) {
                              weapons.add(weapon);
                           } else {
                              LOGGER.warn("Unknown weapon item for tier {}: {}", tierIdOrName, weaponId);
                           }
                        }

                        TIER_WEAPONS.put(tier.getId(), weapons);
                     }
                  }
               }

               LOGGER.info("Loaded tiered weapon config: {} tiers configured", TIER_WEAPONS.size());
            }
         } else {
            LOGGER.warn("Tiered weapon config not found at {}", CONFIG_LOCATION);
            loadDefaults();
         }
      } catch (Exception var17) {
         LOGGER.error("Failed to load tiered weapon config at {}", CONFIG_LOCATION, var17);
         loadDefaults();
      }
   }

   private static void loadDefaults() {
      LOGGER.info("Loading default tiered weapon configuration");
   }

   @Nullable
   public static Item getRandomWeaponForTier(GunTier tier, RandomSource random) {
      if (tier == null) {
         return null;
      } else {
         List<Item> weapons = TIER_WEAPONS.get(tier.getId());
         return weapons != null && !weapons.isEmpty() ? weapons.get(random.nextInt(weapons.size())) : null;
      }
   }

   public static List<Item> getWeaponsForTier(GunTier tier) {
      return tier == null ? Collections.emptyList() : TIER_WEAPONS.getOrDefault(tier.getId(), Collections.emptyList());
   }

   public static boolean hasTierWeapons(GunTier tier) {
      if (tier == null) {
         return false;
      } else {
         List<Item> weapons = TIER_WEAPONS.get(tier.getId());
         return weapons != null && !weapons.isEmpty();
      }
   }

   @SubscribeEvent
   public static void onAddReloadListener(AddReloadListenerEvent event) {
      event.addListener(new SimplePreparableReloadListener<Void>() {
         protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
            return null;
         }

         protected void apply(Void object, ResourceManager resourceManager, ProfilerFiller profiler) {
            TieredWeaponConfig.loadConfig(resourceManager);
         }
      });
   }
}
