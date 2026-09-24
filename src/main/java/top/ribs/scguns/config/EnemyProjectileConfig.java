package top.ribs.scguns.config;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@EventBusSubscriber(
   modid = "scguns"
)
public class EnemyProjectileConfig {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final Map<String, Double> DAMAGE_VALUES = new HashMap<>();
   private static final double DEFAULT_DAMAGE = 4.0;
   private static final ResourceLocation CONFIG_LOCATION = ResourceLocation.fromNamespaceAndPath("scguns", "entity/enemy_projectile_damage.json");

   public EnemyProjectileConfig() {
      super();
   }

   public static void loadConfig(ResourceManager resourceManager) {
      DAMAGE_VALUES.clear();

      try {
         Resource resource = (Resource)resourceManager.getResource(CONFIG_LOCATION).orElse(null);
         if (resource != null) {
            try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
               Gson gson = new Gson();
               JsonObject json = (JsonObject)gson.fromJson(reader, JsonObject.class);
               JsonObject damageValues = json.getAsJsonObject("damage_values");
               if (damageValues != null) {
                  for (Entry<String, JsonElement> entry : damageValues.entrySet()) {
                     DAMAGE_VALUES.put(entry.getKey(), entry.getValue().getAsDouble());
                  }
               }
            }
         } else {
            LOGGER.warn("Enemy Projectile damage config not found at {}", CONFIG_LOCATION);
         }
      } catch (Exception var10) {
         LOGGER.error("Failed to load Enemy Projectile damage config at {}", CONFIG_LOCATION, var10);
      }
   }

   public static double getDamageForEntity(EntityType<?> entityType) {
      String entityId = EntityType.getKey(entityType).toString();
      return DAMAGE_VALUES.getOrDefault(entityId, 4.0);
   }

   @SubscribeEvent
   public static void onAddReloadListener(AddReloadListenerEvent event) {
      event.addListener(new SimplePreparableReloadListener<Void>() {
         protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
            return null;
         }

         protected void apply(Void object, ResourceManager resourceManager, ProfilerFiller profiler) {
            EnemyProjectileConfig.loadConfig(resourceManager);
         }
      });
   }
}
