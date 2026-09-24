package top.ribs.scguns.config;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
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
public class RaidFlareConfig {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final Map<String, RaidFlareConfig.FlareData> FLARES = new HashMap<>();

   public RaidFlareConfig() {
   }

   public static void loadFlareConfigs(ResourceManager resourceManager) {
      FLARES.clear();
      ResourceLocation.fromNamespaceAndPath("scguns", "flares");

      for (Entry<ResourceLocation, Resource> entry : resourceManager.listResources("flares", loc -> loc.getPath().endsWith(".json")).entrySet()) {
         ResourceLocation location = entry.getKey();
         String path = location.getPath();
         String fileName = path.substring(path.lastIndexOf(47) + 1);
         String flareId = fileName.replace("_flare.json", "").replace(".json", "");

         try (InputStreamReader reader = new InputStreamReader(entry.getValue().open(), StandardCharsets.UTF_8)) {
            Gson gson = new Gson();
            JsonObject json = (JsonObject)gson.fromJson(reader, JsonObject.class);
            RaidFlareConfig.FlareData flareData = parseFlareData(json, flareId);
            if (flareData != null) {
               FLARES.put(flareData.raidId(), flareData);
               LOGGER.info("Loaded flare config: {} -> raid: {}", flareId, flareData.raidId());
            }
         } catch (Exception var14) {
            LOGGER.error("Failed to load flare config: {}", flareId, var14);
         }
      }

      LOGGER.info("Loaded {} flare configurations", FLARES.size());
   }

   @Nullable
   private static RaidFlareConfig.FlareData parseFlareData(JsonObject json, String flareId) {
      try {
         String raidId = json.has("raid_id") ? json.get("raid_id").getAsString() : flareId;
         int burstDelay = json.has("burst_delay") ? json.get("burst_delay").getAsInt() : 40;
         int duration = json.has("duration") ? json.get("duration").getAsInt() : 200;
         List<RaidFlareConfig.ParticleEffect> trailParticles = parseParticleEffects(json, "trail_particles");
         List<RaidFlareConfig.ParticleEffect> burstParticles = parseParticleEffects(json, "burst_particles");
         RaidFlareConfig.FlarePattern pattern = null;
         if (json.has("pattern")) {
            pattern = parsePattern(json.getAsJsonObject("pattern"));
         }

         String burstSound = json.has("burst_sound") ? json.get("burst_sound").getAsString() : "minecraft:entity.firework_rocket.large_blast";
         float burstSoundVolume = json.has("burst_sound_volume") ? json.get("burst_sound_volume").getAsFloat() : 1.0F;
         float burstSoundPitch = json.has("burst_sound_pitch") ? json.get("burst_sound_pitch").getAsFloat() : 1.0F;
         return new RaidFlareConfig.FlareData(
            raidId, burstDelay, duration, trailParticles, burstParticles, pattern, burstSound, burstSoundVolume, burstSoundPitch
         );
      } catch (Exception var11) {
         LOGGER.error("Error parsing flare data for: {}", flareId, var11);
         return null;
      }
   }

   private static List<RaidFlareConfig.ParticleEffect> parseParticleEffects(JsonObject json, String key) {
      List<RaidFlareConfig.ParticleEffect> effects = new ArrayList<>();
      if (json.has(key)) {
         for (JsonElement element : json.getAsJsonArray(key)) {
            JsonObject effectObj = element.getAsJsonObject();
            String particleType = effectObj.get("particle").getAsString();
            int count = effectObj.has("count") ? effectObj.get("count").getAsInt() : 10;
            double spread = effectObj.has("spread") ? effectObj.get("spread").getAsDouble() : 0.3;
            double speed = effectObj.has("speed") ? effectObj.get("speed").getAsDouble() : 0.1;
            int color = effectObj.has("color") ? Integer.parseInt(effectObj.get("color").getAsString().replace("#", ""), 16) : 16777215;
            effects.add(new RaidFlareConfig.ParticleEffect(particleType, count, spread, speed, color));
         }
      }

      return effects;
   }

   @Nullable
   private static RaidFlareConfig.FlarePattern parsePattern(JsonObject json) {
      String patternType = json.has("type") ? json.get("type").getAsString() : "circle";
      int repetitions = json.has("repetitions") ? json.get("repetitions").getAsInt() : 1;
      double scale = json.has("scale") ? json.get("scale").getAsDouble() : 1.0;
      List<RaidFlareConfig.Vec3Data> points = new ArrayList<>();
      if (json.has("points")) {
         for (JsonElement element : json.getAsJsonArray("points")) {
            JsonObject pointObj = element.getAsJsonObject();
            points.add(new RaidFlareConfig.Vec3Data(pointObj.get("x").getAsDouble(), pointObj.get("y").getAsDouble(), pointObj.get("z").getAsDouble()));
         }
      }

      return new RaidFlareConfig.FlarePattern(patternType, points, repetitions, scale);
   }

   @Nullable
   public static RaidFlareConfig.FlareData getFlareData(String raidId) {
      return FLARES.get(raidId);
   }

   public static boolean hasFlareForRaid(String raidId) {
      return FLARES.containsKey(raidId);
   }

   public static Set<String> getAllRaidIds() {
      return FLARES.keySet();
   }

   @SubscribeEvent
   public static void onAddReloadListener(AddReloadListenerEvent event) {
      event.addListener(new SimplePreparableReloadListener<Void>() {
         protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
            return null;
         }

         protected void apply(Void object, ResourceManager resourceManager, ProfilerFiller profiler) {
            RaidFlareConfig.loadFlareConfigs(resourceManager);
         }
      });
   }

   public static record FlareData(
      String raidId,
      int burstDelay,
      int duration,
      List<RaidFlareConfig.ParticleEffect> trailParticles,
      List<RaidFlareConfig.ParticleEffect> burstParticles,
      @Nullable RaidFlareConfig.FlarePattern pattern,
      String burstSound,
      float burstSoundVolume,
      float burstSoundPitch
   ) {
      public FlareData(
         String raidId,
         int burstDelay,
         int duration,
         List<RaidFlareConfig.ParticleEffect> trailParticles,
         List<RaidFlareConfig.ParticleEffect> burstParticles,
         @Nullable RaidFlareConfig.FlarePattern pattern,
         String burstSound,
         float burstSoundVolume,
         float burstSoundPitch
      ) {
         this.raidId = raidId;
         this.burstDelay = burstDelay;
         this.duration = duration;
         this.trailParticles = trailParticles;
         this.burstParticles = burstParticles;
         this.pattern = pattern;
         this.burstSound = burstSound;
         this.burstSoundVolume = burstSoundVolume;
         this.burstSoundPitch = burstSoundPitch;
      }
   }

   public static record FlarePattern(String patternType, List<RaidFlareConfig.Vec3Data> points, int repetitions, double scale) {
      public FlarePattern(String patternType, List<RaidFlareConfig.Vec3Data> points, int repetitions, double scale) {
         this.patternType = patternType;
         this.points = points;
         this.repetitions = repetitions;
         this.scale = scale;
      }
   }

   public static record ParticleEffect(String particleType, int count, double spread, double speed, int color) {
      public ParticleEffect(String particleType, int count, double spread, double speed, int color) {
         this.particleType = particleType;
         this.count = count;
         this.spread = spread;
         this.speed = speed;
         this.color = color;
      }
   }

   public static record Vec3Data(double x, double y, double z) {
      public Vec3Data(double x, double y, double z) {
         this.x = x;
         this.y = y;
         this.z = z;
      }
   }
}
