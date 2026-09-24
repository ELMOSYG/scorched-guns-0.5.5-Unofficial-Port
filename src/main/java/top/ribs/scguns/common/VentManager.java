package top.ribs.scguns.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@EventBusSubscriber(
   modid = "scguns"
)
public class VentManager {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final Map<ResourceLocation, Vent> VENT_CONFIGS = new HashMap<>();
   private static final Map<ResourceLocation, VentCollectorConfig> COLLECTOR_CONFIGS = new HashMap<>();
   private static final String VENT_FOLDER = "vents";

   public VentManager() {
      super();
   }

   @Nullable
   public static Vent getVent(ResourceLocation id) {
      return VENT_CONFIGS.get(id);
   }

   @Nullable
   public static VentCollectorConfig getVentCollectorConfig(ResourceLocation id) {
      return COLLECTOR_CONFIGS.get(id);
   }

   public static void loadVentConfig(ResourceManager resourceManager, ResourceLocation ventId) {
      ResourceLocation location = ResourceLocation.fromNamespaceAndPath(ventId.getNamespace(), "vents/" + ventId.getPath() + ".json");

      try {
         Resource resource = (Resource)resourceManager.getResource(location).orElse(null);
         if (resource != null) {
            try (
               InputStream stream = resource.open();
               InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
            ) {
               JsonObject json = (JsonObject)GSON.fromJson(reader, JsonObject.class);
               Vent vent = parseVent(json);
               if (vent != null) {
                  VENT_CONFIGS.put(ventId, vent);
                  LOGGER.info("Successfully loaded vent config: {}", ventId);
               }
            }
         } else {
            LOGGER.warn("No JSON found for vent: {}", ventId);
         }
      } catch (Exception var12) {
         LOGGER.error("Failed to load vent config: {}", location, var12);
      }
   }

   public static void loadVentCollectorConfig(ResourceManager resourceManager, ResourceLocation collectorId) {
      ResourceLocation location = ResourceLocation.fromNamespaceAndPath(collectorId.getNamespace(), "vents/" + collectorId.getPath() + ".json");

      try {
         Resource resource = (Resource)resourceManager.getResource(location).orElse(null);
         if (resource != null) {
            try (
               InputStream stream = resource.open();
               InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
            ) {
               JsonObject json = (JsonObject)GSON.fromJson(reader, JsonObject.class);
               VentCollectorConfig config = parseVentCollector(json);
               if (config != null) {
                  COLLECTOR_CONFIGS.put(collectorId, config);
                  LOGGER.info("Successfully loaded vent collector config: {}", collectorId);
               }
            }
         } else {
            LOGGER.warn("No JSON found for vent collector: {}", collectorId);
         }
      } catch (Exception var12) {
         LOGGER.error("Failed to load vent collector config: {}", location, var12);
      }
   }

   private static Vent parseVent(JsonObject json) {
      try {
         Vent vent = new Vent();
         if (json.has("activation")) {
            parseActivation(json.getAsJsonObject("activation"), vent.getActivation());
         }

         if (json.has("power")) {
            parsePower(json.getAsJsonObject("power"), vent.getPower());
         }

         if (json.has("production")) {
            parseProduction(json.getAsJsonObject("production"), vent.getProduction());
         }

         if (json.has("placement")) {
            parsePlacement(json.getAsJsonObject("placement"), vent.getPlacement());
         }

         if (json.has("particles")) {
            parseParticles(json.getAsJsonObject("particles"), vent.getParticles());
         }

         return vent;
      } catch (Exception var2) {
         LOGGER.error("Error parsing vent config", var2);
         return null;
      }
   }

   private static VentCollectorConfig parseVentCollector(JsonObject json) {
      try {
         VentCollectorConfig config = new VentCollectorConfig();
         if (json.has("filters")) {
            parseFilters(json.getAsJsonObject("filters"), config.getFilters());
         }

         if (json.has("processing")) {
            parseProcessing(json.getAsJsonObject("processing"), config.getProcessing());
         }

         return config;
      } catch (Exception var2) {
         LOGGER.error("Error parsing vent collector config", var2);
         return null;
      }
   }

   private static void parseFilters(JsonObject json, VentCollectorConfig.Filters filters) {
      if (json.has("maxCharge")) {
         filters.setMaxCharge(json.get("maxCharge").getAsInt());
      }

      if (json.has("consumptionChance")) {
         filters.setConsumptionChance(json.get("consumptionChance").getAsFloat());
      }

      if (json.has("processCooldown")) {
         filters.setProcessCooldown(json.get("processCooldown").getAsInt());
      }

      if (json.has("filterItems")) {
         filters.clearFilterItems();

         for (JsonElement element : json.getAsJsonArray("filterItems")) {
            JsonObject itemObj = element.getAsJsonObject();
            VentCollectorConfig.Filters.FilterItem filterItem = new VentCollectorConfig.Filters.FilterItem();
            if (itemObj.has("tag")) {
               filterItem.setIdentifier(ResourceLocation.parse(itemObj.get("tag").getAsString()));
               filterItem.setIsTag(true);
            } else if (itemObj.has("item")) {
               filterItem.setIdentifier(ResourceLocation.parse(itemObj.get("item").getAsString()));
               filterItem.setIsTag(false);
            }

            if (itemObj.has("chargeAmount")) {
               filterItem.setChargeAmount(itemObj.get("chargeAmount").getAsInt());
            }

            filters.addFilterItem(filterItem);
         }
      }
   }

   private static void parseProcessing(JsonObject json, VentCollectorConfig.Processing processing) {
      if (json.has("powerSpeedMultiplier")) {
         processing.setPowerSpeedMultiplier(json.get("powerSpeedMultiplier").getAsFloat());
      }

      if (json.has("pushCooldown")) {
         processing.setPushCooldown(json.get("pushCooldown").getAsInt());
      }
   }

   private static void parseActivation(JsonObject json, Vent.Activation activation) {
      if (json.has("baseBlock")) {
         activation.setBaseBlock(ResourceLocation.parse(json.get("baseBlock").getAsString()));
      }

      if (json.has("requiresWaterlogged")) {
         activation.setRequiresWaterlogged(json.get("requiresWaterlogged").getAsBoolean());
      }
   }

   private static void parsePower(JsonObject json, Vent.Power power) {
      if (json.has("maxPower")) {
         power.setMaxPower(json.get("maxPower").getAsInt());
      }

      if (json.has("baseTickInterval")) {
         power.setBaseTickInterval(json.get("baseTickInterval").getAsInt());
      }

      if (json.has("tickWiggleRoom")) {
         power.setTickWiggleRoom(json.get("tickWiggleRoom").getAsInt());
      }
   }

   private static void parseProduction(JsonObject json, Vent.Production production) {
      if (json.has("outputs")) {
         production.clearOutputs();

         for (JsonElement element : json.getAsJsonArray("outputs")) {
            JsonObject outputObj = element.getAsJsonObject();
            Vent.Production.OutputItem output = new Vent.Production.OutputItem();
            if (outputObj.has("item")) {
               output.setItem(ResourceLocation.parse(outputObj.get("item").getAsString()));
            }

            if (outputObj.has("weight")) {
               output.setWeight(outputObj.get("weight").getAsInt());
            }

            production.addOutput(output);
         }
      }

      if (json.has("productionChance")) {
         production.setProductionChance(json.get("productionChance").getAsFloat());
      }
   }

   private static void parsePlacement(JsonObject json, Vent.Placement placement) {
      if (json.has("enabled")) {
         placement.setEnabled(json.get("enabled").getAsBoolean());
      }

      if (json.has("blockToPlace")) {
         placement.setBlockToPlace(ResourceLocation.parse(json.get("blockToPlace").getAsString()));
      }

      if (json.has("radius")) {
         placement.setRadius(json.get("radius").getAsInt());
      }

      if (json.has("placementChance")) {
         placement.setPlacementChance(json.get("placementChance").getAsFloat());
      }
   }

   private static void parseParticles(JsonObject json, Vent.Particles particles) {
      if (json.has("showActive")) {
         particles.setShowActive(json.get("showActive").getAsBoolean());
      }

      if (json.has("activeSound")) {
         particles.setActiveSound(ResourceLocation.parse(json.get("activeSound").getAsString()));
      }
   }

   public static void clearAll() {
      VENT_CONFIGS.clear();
      COLLECTOR_CONFIGS.clear();
   }

   @SubscribeEvent
   public static void onAddReloadListener(AddReloadListenerEvent event) {
      event.addListener(
         new SimplePreparableReloadListener<Void>() {
            protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
               VentManager.clearAll();
               VentManager.loadVentConfig(resourceManager, ResourceLocation.fromNamespaceAndPath("scguns", "geothermal_vent"));
               VentManager.loadVentConfig(resourceManager, ResourceLocation.fromNamespaceAndPath("scguns", "sulfur_vent"));
               VentManager.loadVentCollectorConfig(resourceManager, ResourceLocation.fromNamespaceAndPath("scguns", "vent_collector"));
               VentManager.VENT_CONFIGS.get(ResourceLocation.fromNamespaceAndPath("scguns", "geothermal_vent"));
               VentManager.COLLECTOR_CONFIGS.get(ResourceLocation.fromNamespaceAndPath("scguns", "vent_collector"));
               return null;
            }

            protected void apply(Void object, ResourceManager resourceManager, ProfilerFiller profiler) {
               VentManager.LOGGER
                  .info("Loaded {} vent configurations and {} collector configurations", VentManager.VENT_CONFIGS.size(), VentManager.COLLECTOR_CONFIGS.size());
            }
         }
      );
   }
}
