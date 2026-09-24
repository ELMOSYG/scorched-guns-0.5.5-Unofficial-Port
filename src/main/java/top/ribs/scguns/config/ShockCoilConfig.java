package top.ribs.scguns.config;


import net.minecraft.core.registries.BuiltInRegistries;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@EventBusSubscriber(
   modid = "scguns"
)
public class ShockCoilConfig {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final ResourceLocation CONFIG_LOCATION = ResourceLocation.fromNamespaceAndPath("scguns", "turrets/shock_coil.json");
   private static ShockCoilConfig.ShockCoilData config = new ShockCoilConfig.ShockCoilData();

   public ShockCoilConfig() {
      super();
   }

   public static void loadConfig(ResourceManager resourceManager) {
      config = new ShockCoilConfig.ShockCoilData();

      try {
         Resource resource = (Resource)resourceManager.getResource(CONFIG_LOCATION).orElse(null);
         if (resource != null) {
            try (
               InputStream stream = resource.open();
               InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
            ) {
               JsonObject json = (JsonObject)GSON.fromJson(reader, JsonObject.class);
               parseConfig(json);
               LOGGER.info("Successfully loaded shock coil config");
            }
         } else {
            LOGGER.warn("No JSON found for shock coil config, using defaults");
            loadDefaults();
         }
      } catch (Exception var10) {
         LOGGER.error("Failed to load shock coil config at {}", CONFIG_LOCATION, var10);
         loadDefaults();
      }
   }

   private static void parseConfig(JsonObject json) {
      if (json.has("maxEnergy")) {
         config.setMaxEnergy(json.get("maxEnergy").getAsInt());
      }

      if (json.has("energyPerZap")) {
         config.setEnergyPerZap(json.get("energyPerZap").getAsInt());
      }

      if (json.has("zapRange")) {
         config.setZapRange(json.get("zapRange").getAsDouble());
      }

      if (json.has("zapCooldown")) {
         config.setZapCooldown(json.get("zapCooldown").getAsInt());
      }

      if (json.has("maxTargetsPerZap")) {
         config.setMaxTargetsPerZap(json.get("maxTargetsPerZap").getAsInt());
      }

      if (json.has("baseDamage")) {
         config.setBaseDamage(json.get("baseDamage").getAsFloat());
      }

      if (json.has("zapSound")) {
         config.setZapSound(json.get("zapSound").getAsString());
      }

      if (json.has("statusEffects")) {
         config.clearStatusEffects();

         for (JsonElement element : json.getAsJsonArray("statusEffects")) {
            JsonObject effectObj = element.getAsJsonObject();
            ShockCoilConfig.StatusEffect effect = parseStatusEffect(effectObj);
            if (effect != null) {
               config.addStatusEffect(effect);
            }
         }
      }
   }

   @Nullable
   private static ShockCoilConfig.StatusEffect parseStatusEffect(JsonObject json) {
      try {
         String effectId = json.get("effect").getAsString();
         int duration = json.has("duration") ? json.get("duration").getAsInt() : 100;
         int amplifier = json.has("amplifier") ? json.get("amplifier").getAsInt() : 0;
         float chance = json.has("chance") ? json.get("chance").getAsFloat() : 1.0F;
         MobEffect effect = (MobEffect)BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.parse(effectId));
         if (effect == null) {
            LOGGER.warn("Unknown effect: {}", effectId);
            return null;
         } else {
            return new ShockCoilConfig.StatusEffect(effect, duration, amplifier, chance);
         }
      } catch (Exception var6) {
         LOGGER.error("Error parsing status effect", var6);
         return null;
      }
   }

   private static void loadDefaults() {
      LOGGER.info("Loading default shock coil configuration");
      config = new ShockCoilConfig.ShockCoilData();
   }

   public static int getMaxEnergy() {
      return config.getMaxEnergy();
   }

   public static int getEnergyPerZap() {
      return config.getEnergyPerZap();
   }

   public static double getZapRange() {
      return config.getZapRange();
   }

   public static int getZapCooldown() {
      return config.getZapCooldown();
   }

   public static int getMaxTargetsPerZap() {
      return config.getMaxTargetsPerZap();
   }

   public static float getBaseDamage() {
      return config.getBaseDamage();
   }

   public static String getZapSound() {
      return config.getZapSound();
   }

   public static List<ShockCoilConfig.StatusEffect> getStatusEffects() {
      return config.getStatusEffects();
   }

   @SubscribeEvent
   public static void onAddReloadListener(AddReloadListenerEvent event) {
      event.addListener(new SimplePreparableReloadListener<Void>() {
         protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
            ShockCoilConfig.loadConfig(resourceManager);
            return null;
         }

         protected void apply(Void object, ResourceManager resourceManager, ProfilerFiller profiler) {
            ShockCoilConfig.LOGGER.info("Shock coil configuration loaded successfully");
         }
      });
   }

   public static class ShockCoilData {
      private int maxEnergy = 16000;
      private int energyPerZap = 100;
      private double zapRange = 8.0;
      private int zapCooldown = 20;
      private int maxTargetsPerZap = 3;
      private float baseDamage = 4.0F;
      private String zapSound = "scguns:item.shock.fire";
      private List<ShockCoilConfig.StatusEffect> statusEffects = new ArrayList<>();

      public ShockCoilData() {
         super();
      }

      public int getMaxEnergy() {
         return this.maxEnergy;
      }

      public int getEnergyPerZap() {
         return this.energyPerZap;
      }

      public double getZapRange() {
         return this.zapRange;
      }

      public int getZapCooldown() {
         return this.zapCooldown;
      }

      public int getMaxTargetsPerZap() {
         return this.maxTargetsPerZap;
      }

      public float getBaseDamage() {
         return this.baseDamage;
      }

      public String getZapSound() {
         return this.zapSound;
      }

      public List<ShockCoilConfig.StatusEffect> getStatusEffects() {
         return this.statusEffects;
      }

      void setMaxEnergy(int value) {
         this.maxEnergy = value;
      }

      void setEnergyPerZap(int value) {
         this.energyPerZap = value;
      }

      void setZapRange(double value) {
         this.zapRange = value;
      }

      void setZapCooldown(int value) {
         this.zapCooldown = value;
      }

      void setMaxTargetsPerZap(int value) {
         this.maxTargetsPerZap = value;
      }

      void setBaseDamage(float value) {
         this.baseDamage = value;
      }

      void setZapSound(String value) {
         this.zapSound = value;
      }

      void addStatusEffect(ShockCoilConfig.StatusEffect effect) {
         this.statusEffects.add(effect);
      }

      void clearStatusEffects() {
         this.statusEffects.clear();
      }
   }

   public static class StatusEffect {
      private final MobEffect effect;
      private final int duration;
      private final int amplifier;
      private final float chance;

      public StatusEffect(MobEffect effect, int duration, int amplifier, float chance) {
         super();
         this.effect = effect;
         this.duration = duration;
         this.amplifier = amplifier;
         this.chance = chance;
      }

      public MobEffect getEffect() {
         return this.effect;
      }

      public int getDuration() {
         return this.duration;
      }

      public int getAmplifier() {
         return this.amplifier;
      }

      public float getChance() {
         return this.chance;
      }
   }
}
