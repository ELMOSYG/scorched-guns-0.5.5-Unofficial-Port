package top.ribs.scguns.config;


import net.minecraft.core.registries.BuiltInRegistries;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@EventBusSubscriber(
   modid = "scguns"
)
public class AdvancedComposterDropsConfig {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final ResourceLocation CONFIG_LOCATION = ResourceLocation.fromNamespaceAndPath("scguns", "composter/advanced_composter_drops.json");
   private static AdvancedComposterDropsConfig.ComposterLootTable LOOT_TABLE = new AdvancedComposterDropsConfig.ComposterLootTable();

   public AdvancedComposterDropsConfig() {
   }

   public static void loadConfig(ResourceManager resourceManager) {
      LOOT_TABLE.clear();

      try {
         Resource resource = (Resource)resourceManager.getResource(CONFIG_LOCATION).orElse(null);
         if (resource != null) {
            try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
               Gson gson = new Gson();
               JsonObject json = (JsonObject)gson.fromJson(reader, JsonObject.class);
               if (json != null) {
                  if (json.has("min_drops") && json.has("max_drops")) {
                     int minDrops = json.get("min_drops").getAsInt();
                     int maxDrops = json.get("max_drops").getAsInt();
                     LOOT_TABLE.setDropRange(minDrops, maxDrops);
                  }

                  if (json.has("drops")) {
                     for (JsonElement dropElement : json.getAsJsonArray("drops")) {
                        AdvancedComposterDropsConfig.DropEntry entry = AdvancedComposterDropsConfig.DropEntry.fromJson(dropElement.getAsJsonObject());
                        if (entry != null) {
                           LOOT_TABLE.addDrop(entry);
                        }
                     }
                  }

                  LOGGER.info(
                     "Loaded advanced composter drops config: {} drops configured, total weight: {}", LOOT_TABLE.getDropCount(), LOOT_TABLE.totalWeight
                  );
               }
            }
         } else {
            LOGGER.warn("Advanced composter drops config not found at {}", CONFIG_LOCATION);
            loadDefaultConfig();
         }
      } catch (Exception var11) {
         LOGGER.error("Failed to load advanced composter drops config at {}", CONFIG_LOCATION, var11);
         loadDefaultConfig();
      }
   }

   private static void loadDefaultConfig() {
      LOGGER.info("Loading default composter drops configuration");
      LOOT_TABLE.clear();
      Item bonemeal = (Item)BuiltInRegistries.ITEM.get(ResourceLocation.parse("minecraft:bone_meal"));
      if (bonemeal != null) {
         LOOT_TABLE.addDrop(new AdvancedComposterDropsConfig.DropEntry(bonemeal, 100, 1, 3));
      }

      LOOT_TABLE.setDropRange(1, 3);
   }

   public static List<ItemStack> generateDrops(Random random) {
      return LOOT_TABLE.generateDrops(random);
   }

   public static boolean hasDropsConfigured() {
      return !LOOT_TABLE.isEmpty();
   }

   @SubscribeEvent
   public static void onAddReloadListener(AddReloadListenerEvent event) {
      event.addListener(new SimplePreparableReloadListener<Void>() {
         protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
            return null;
         }

         protected void apply(Void object, ResourceManager resourceManager, ProfilerFiller profiler) {
            AdvancedComposterDropsConfig.loadConfig(resourceManager);
         }
      });
   }

   public static class ComposterLootTable {
      private final List<AdvancedComposterDropsConfig.DropEntry> drops = new ArrayList<>();
      private int totalWeight = 0;
      private int minDrops = 1;
      private int maxDrops = 3;

      public ComposterLootTable() {
      }

      public void addDrop(AdvancedComposterDropsConfig.DropEntry entry) {
         this.drops.add(entry);
         this.totalWeight = this.totalWeight + entry.weight();
      }

      public void setDropRange(int min, int max) {
         this.minDrops = Math.max(1, min);
         this.maxDrops = Math.max(this.minDrops, max);
      }

      public List<ItemStack> generateDrops(Random random) {
         if (this.drops.isEmpty()) {
            AdvancedComposterDropsConfig.LOGGER.warn("No drops configured for advanced composter!");
            return Collections.emptyList();
         } else {
            int dropCount = this.minDrops;
            if (this.maxDrops > this.minDrops) {
               dropCount = this.minDrops + random.nextInt(this.maxDrops - this.minDrops + 1);
            }

            List<ItemStack> result = new ArrayList<>();

            for (int i = 0; i < dropCount; i++) {
               AdvancedComposterDropsConfig.DropEntry entry = this.selectWeightedDrop(random);
               if (entry != null) {
                  result.add(entry.createStack(random));
               }
            }

            return result;
         }
      }

      @Nullable
      private AdvancedComposterDropsConfig.DropEntry selectWeightedDrop(Random random) {
         if (!this.drops.isEmpty() && this.totalWeight > 0) {
            int randomWeight = random.nextInt(this.totalWeight);
            int currentWeight = 0;

            for (AdvancedComposterDropsConfig.DropEntry entry : this.drops) {
               currentWeight += entry.weight();
               if (randomWeight < currentWeight) {
                  return entry;
               }
            }

            return this.drops.get(this.drops.size() - 1);
         } else {
            return null;
         }
      }

      public void clear() {
         this.drops.clear();
         this.totalWeight = 0;
         this.minDrops = 1;
         this.maxDrops = 3;
      }

      public boolean isEmpty() {
         return this.drops.isEmpty();
      }

      public int getDropCount() {
         return this.drops.size();
      }
   }

   public static record DropEntry(Item item, int weight, int minCount, int maxCount) {
      public DropEntry(Item item, int weight, int minCount, int maxCount) {
         this.item = item;
         this.weight = weight;
         this.minCount = minCount;
         this.maxCount = maxCount;
      }

      public ItemStack createStack(Random random) {
         int count = this.minCount;
         if (this.maxCount > this.minCount) {
            count = this.minCount + random.nextInt(this.maxCount - this.minCount + 1);
         }

         return new ItemStack(this.item, count);
      }

      public static AdvancedComposterDropsConfig.DropEntry fromJson(JsonObject json) {
         String itemId = json.get("item").getAsString();
         int weight = json.has("weight") ? json.get("weight").getAsInt() : 1;
         int minCount = json.has("min_count") ? json.get("min_count").getAsInt() : 1;
         int maxCount = json.has("max_count") ? json.get("max_count").getAsInt() : minCount;
         Item item = (Item)BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
         if (item == null) {
            AdvancedComposterDropsConfig.LOGGER.warn("Unknown item in composter drops: {}", itemId);
            return null;
         } else {
            return new AdvancedComposterDropsConfig.DropEntry(item, weight, minCount, maxCount);
         }
      }
   }
}
