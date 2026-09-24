package top.ribs.scguns.config;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(
   modid = "scguns"
)
public class ProjectileAdvantageConfig {
   private static final Map<String, ProjectileAdvantageConfig.AdvantageData> ADVANTAGE_MAP = new HashMap<>();
   private static final ResourceLocation CONFIG_LOCATION = ResourceLocation.fromNamespaceAndPath("scguns", "entity/advantages.json");

   public ProjectileAdvantageConfig() {
   }

   public static void loadConfig(ResourceManager resourceManager) {
      ADVANTAGE_MAP.clear();

      try {
         Resource resource = (Resource)resourceManager.getResource(CONFIG_LOCATION).orElse(null);
         if (resource != null) {
            try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
               Gson gson = new Gson();
               JsonObject json = (JsonObject)gson.fromJson(reader, JsonObject.class);
               if (json != null && json.has("advantages")) {
                  JsonObject advantages = json.getAsJsonObject("advantages");

                  for (Entry<String, JsonElement> entry : advantages.entrySet()) {
                     String advantageKey = entry.getKey();
                     JsonObject advantageObj = entry.getValue().getAsJsonObject();
                     float multiplier = advantageObj.has("multiplier") ? advantageObj.get("multiplier").getAsFloat() : 1.0F;
                     boolean causesFire = advantageObj.has("causes_fire") && advantageObj.get("causes_fire").getAsBoolean();
                     int fireDuration = advantageObj.has("fire_duration") ? advantageObj.get("fire_duration").getAsInt() : 2;
                     Set<String> targetTags = new HashSet<>();
                     if (advantageObj.has("target_tags")) {
                        for (JsonElement tagElement : advantageObj.getAsJsonArray("target_tags")) {
                           targetTags.add(tagElement.getAsString());
                        }
                     }

                     ADVANTAGE_MAP.put(advantageKey, new ProjectileAdvantageConfig.AdvantageData(multiplier, causesFire, fireDuration, targetTags));
                  }
               }
            }
         } else {
            loadDefaults();
         }
      } catch (Exception var18) {
         loadDefaults();
      }
   }

   private static void loadDefaults() {
      ADVANTAGE_MAP.put("scguns:undead", new ProjectileAdvantageConfig.AdvantageData(1.25F, true, 2, Set.of("scguns:undead", "scguns:wither", "scguns:ghost")));
      ADVANTAGE_MAP.put("scguns:heavy", new ProjectileAdvantageConfig.AdvantageData(1.25F, false, 0, Set.of("scguns:heavy", "scguns:very_heavy")));
      ADVANTAGE_MAP.put("scguns:very_heavy", new ProjectileAdvantageConfig.AdvantageData(1.25F, false, 0, Set.of("scguns:heavy", "scguns:very_heavy")));
      ADVANTAGE_MAP.put("scguns:fire", new ProjectileAdvantageConfig.AdvantageData(1.25F, false, 0, Set.of("scguns:fire")));
      ADVANTAGE_MAP.put("scguns:illager", new ProjectileAdvantageConfig.AdvantageData(1.5F, false, 0, Set.of("scguns:illager")));
      ADVANTAGE_MAP.put("scguns:water", new ProjectileAdvantageConfig.AdvantageData(1.65F, false, 0, Set.of("scguns:water")));
      ADVANTAGE_MAP.put("scguns:bot", new ProjectileAdvantageConfig.AdvantageData(1.5F, false, 0, Set.of("scguns:bot")));
      ADVANTAGE_MAP.put("scguns:wither", new ProjectileAdvantageConfig.AdvantageData(1.25F, false, 0, Set.of("scguns:wither")));
   }

   public static ProjectileAdvantageConfig.AdvantageData getAdvantageData(String advantageKey) {
      return ADVANTAGE_MAP.get(advantageKey);
   }

   public static Map<String, ProjectileAdvantageConfig.AdvantageData> getAllAdvantages() {
      return new HashMap<>(ADVANTAGE_MAP);
   }

   @SubscribeEvent
   public static void onAddReloadListener(AddReloadListenerEvent event) {
      event.addListener(new SimplePreparableReloadListener<Void>() {
         protected Void prepare(@NotNull ResourceManager resourceManager, ProfilerFiller profiler) {
            return null;
         }

         protected void apply(Void object, ResourceManager resourceManager, ProfilerFiller profiler) {
            ProjectileAdvantageConfig.loadConfig(resourceManager);
         }
      });
   }

   public static record AdvantageData(float multiplier, boolean causesFire, int fireDuration, Set<String> targetTags) {
      public AdvantageData(float multiplier, boolean causesFire, int fireDuration, Set<String> targetTags) {
         this.multiplier = multiplier;
         this.causesFire = causesFire;
         this.fireDuration = fireDuration;
         this.targetTags = targetTags;
      }
   }
}
