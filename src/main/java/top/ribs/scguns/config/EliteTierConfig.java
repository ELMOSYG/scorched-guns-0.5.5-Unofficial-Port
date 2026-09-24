package top.ribs.scguns.config;


import net.minecraft.core.registries.BuiltInRegistries;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
public class EliteTierConfig {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final Map<String, EliteTierConfig.EliteData> ELITE_TIERS = new HashMap<>();
   private static final ResourceLocation CONFIG_LOCATION = ResourceLocation.fromNamespaceAndPath("scguns", "entity/elite_tiers.json");

   public EliteTierConfig() {
   }

   public static void loadConfig(ResourceManager resourceManager) {
      ELITE_TIERS.clear();

      try {
         Resource resource = (Resource)resourceManager.getResource(CONFIG_LOCATION).orElse(null);
         if (resource != null) {
            try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
               Gson gson = new Gson();
               JsonObject json = (JsonObject)gson.fromJson(reader, JsonObject.class);
               if (json != null) {
                  for (Entry<String, JsonElement> entry : json.entrySet()) {
                     String tierIdOrName = entry.getKey();
                     JsonObject tierData = entry.getValue().getAsJsonObject();
                     GunTier tier = GunTierRegistry.getTier(tierIdOrName.toLowerCase());
                     if (tier == null) {
                        LOGGER.warn("Unknown tier in elite config: {}", tierIdOrName);
                     } else {
                        List<Item> weapons = new ArrayList<>();
                        if (tierData.has("weapons")) {
                           for (JsonElement weaponElement : tierData.getAsJsonArray("weapons")) {
                              String weaponId = weaponElement.getAsString();
                              Item weapon = (Item)BuiltInRegistries.ITEM.get(ResourceLocation.parse(weaponId));
                              if (weapon != null) {
                                 weapons.add(weapon);
                              } else {
                                 LOGGER.warn("Unknown elite weapon for tier {}: {}", tierIdOrName, weaponId);
                              }
                           }
                        }

                        List<EliteTierConfig.ArmorPiece> armor = new ArrayList<>();
                        if (tierData.has("armor")) {
                           for (JsonElement armorElement : tierData.getAsJsonArray("armor")) {
                              EliteTierConfig.ArmorPiece piece = EliteTierConfig.ArmorPiece.fromJson(armorElement.getAsJsonObject());
                              if (piece != null) {
                                 armor.add(piece);
                              }
                           }
                        }

                        ELITE_TIERS.put(tier.getId(), new EliteTierConfig.EliteData(weapons, armor));
                     }
                  }
               }

               LOGGER.info("Loaded elite tier config: {} elite tiers configured", ELITE_TIERS.size());
            }
         } else {
            LOGGER.warn("Elite tier config not found at {}", CONFIG_LOCATION);
         }
      } catch (Exception var18) {
         LOGGER.error("Failed to load elite tier config at {}", CONFIG_LOCATION, var18);
      }
   }

   @Nullable
   public static EliteTierConfig.EliteData getEliteData(GunTier tier) {
      return tier == null ? null : ELITE_TIERS.get(tier.getId());
   }

   public static boolean hasEliteData(GunTier tier) {
      if (tier == null) {
         return false;
      } else {
         EliteTierConfig.EliteData data = ELITE_TIERS.get(tier.getId());
         return data != null && data.hasEliteWeapons();
      }
   }

   @SubscribeEvent
   public static void onAddReloadListener(AddReloadListenerEvent event) {
      event.addListener(new SimplePreparableReloadListener<Void>() {
         protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
            return null;
         }

         protected void apply(Void object, ResourceManager resourceManager, ProfilerFiller profiler) {
            EliteTierConfig.loadConfig(resourceManager);
         }
      });
   }

   public static record ArmorPiece(Item item, String slot, float chance) {
      public ArmorPiece(Item item, String slot, float chance) {
         this.item = item;
         this.slot = slot;
         this.chance = chance;
      }

      public static EliteTierConfig.ArmorPiece fromJson(JsonObject json) {
         String itemId = json.get("item").getAsString();
         String slot = json.get("slot").getAsString();
         float chance = json.has("chance") ? json.get("chance").getAsFloat() : 1.0F;
         Item item = (Item)BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
         if (item == null) {
            EliteTierConfig.LOGGER.warn("Unknown armor item: {}", itemId);
            return null;
         } else {
            return new EliteTierConfig.ArmorPiece(item, slot, chance);
         }
      }
   }

   public static record EliteData(List<Item> eliteWeapons, List<EliteTierConfig.ArmorPiece> armor) {
      public EliteData(List<Item> eliteWeapons, List<EliteTierConfig.ArmorPiece> armor) {
         this.eliteWeapons = eliteWeapons;
         this.armor = armor;
      }

      @Nullable
      public Item getRandomWeapon(RandomSource random) {
         return this.eliteWeapons.isEmpty() ? null : this.eliteWeapons.get(random.nextInt(this.eliteWeapons.size()));
      }

      public boolean hasEliteWeapons() {
         return !this.eliteWeapons.isEmpty();
      }
   }
}
