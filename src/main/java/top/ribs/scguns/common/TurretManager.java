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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import top.ribs.scguns.network.PacketHandler;
import top.ribs.scguns.network.message.S2CMessageSyncTurrets;

@EventBusSubscriber(
   modid = "scguns"
)
public class TurretManager {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final Map<ResourceLocation, Turret> TURRET_CONFIGS = new HashMap<>();
   /** The JSON each config was parsed from, kept so a joining client can be sent the same text. */
   private static final Map<ResourceLocation, JsonObject> TURRET_SOURCES = new HashMap<>();
   private static final String TURRET_FOLDER = "turrets";

   public TurretManager() {
      super();
   }

   @Nullable
   public static Turret getTurret(ResourceLocation id) {
      return TURRET_CONFIGS.get(id);
   }

   public static void loadTurretConfig(ResourceManager resourceManager, ResourceLocation turretId) {
      ResourceLocation location = ResourceLocation.fromNamespaceAndPath(turretId.getNamespace(), "turrets/" + turretId.getPath() + ".json");

      try {
         Resource resource = (Resource)resourceManager.getResource(location).orElse(null);
         if (resource != null) {
            try (
               InputStream stream = resource.open();
               InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
            ) {
               JsonObject json = (JsonObject)GSON.fromJson(reader, JsonObject.class);
               Turret turret = parseTurret(json);
               if (turret != null) {
                  TURRET_CONFIGS.put(turretId, turret);
                  TURRET_SOURCES.put(turretId, json);
                  LOGGER.info("Successfully loaded turret config: {}", turretId);
               }
            }
         } else {
            LOGGER.warn("No JSON found for turret: {}", turretId);
         }
      } catch (Exception var12) {
         LOGGER.error("Failed to load turret config: {}", location, var12);
      }
   }

   private static Turret parseTurret(JsonObject json) {
      try {
         Turret turret = new Turret();
         if (json.has("targeting")) {
            parseTargeting(json.getAsJsonObject("targeting"), turret.getTargeting());
         }

         if (json.has("combat")) {
            parseCombat(json.getAsJsonObject("combat"), turret.getCombat());
         }

         if (json.has("ammunition")) {
            parseAmmunition(json.getAsJsonObject("ammunition"), turret.getAmmunition());
         }

         if (json.has("behavior")) {
            parseBehavior(json.getAsJsonObject("behavior"), turret.getBehavior());
         }

         if (json.has("display")) {
            parseDisplay(json.getAsJsonObject("display"), turret.getDisplay());
         }

         return turret;
      } catch (Exception var2) {
         LOGGER.error("Error parsing turret config", var2);
         return null;
      }
   }

   private static void parseTargeting(JsonObject json, Turret.Targeting targeting) {
      if (json.has("range")) {
         targeting.setRange(json.get("range").getAsDouble());
      }

      if (json.has("verticalRange")) {
         targeting.setVerticalRange(json.get("verticalRange").getAsDouble());
      }

      if (json.has("minFiringDistance")) {
         targeting.setMinFiringDistance(json.get("minFiringDistance").getAsDouble());
      }

      if (json.has("rotationSpeed")) {
         targeting.setRotationSpeed(json.get("rotationSpeed").getAsFloat());
      }

      if (json.has("positionSmoothing")) {
         targeting.setPositionSmoothing(json.get("positionSmoothing").getAsFloat());
      }

      if (json.has("maxPitch")) {
         targeting.setMaxPitch(json.get("maxPitch").getAsFloat());
      }

      if (json.has("minPitch")) {
         targeting.setMinPitch(json.get("minPitch").getAsFloat());
      }

      if (json.has("predictionMultiplier")) {
         targeting.setPredictionMultiplier(json.get("predictionMultiplier").getAsInt());
      }

      if (json.has("requiresLineOfSight")) {
         targeting.setRequiresLineOfSight(json.get("requiresLineOfSight").getAsBoolean());
      }
   }

   private static void parseCombat(JsonObject json, Turret.Combat combat) {
      if (json.has("cooldown")) {
         combat.setCooldown(json.get("cooldown").getAsInt());
      }

      if (json.has("inaccuracy")) {
         combat.setInaccuracy(json.get("inaccuracy").getAsFloat());
      }

      if (json.has("pelletCount")) {
         combat.setPelletCount(json.get("pelletCount").getAsInt());
      }

      if (json.has("spreadAngle")) {
         combat.setSpreadAngle(json.get("spreadAngle").getAsFloat());
      }

      if (json.has("recoilMax")) {
         combat.setRecoilMax(json.get("recoilMax").getAsFloat());
      }

      if (json.has("recoilSpeed")) {
         combat.setRecoilSpeed(json.get("recoilSpeed").getAsFloat());
      }

      if (json.has("damageModifier")) {
         combat.setDamageModifier(json.get("damageModifier").getAsInt());
      }

      if (json.has("projectileSpeed")) {
         combat.setProjectileSpeed(json.get("projectileSpeed").getAsDouble());
      }

      if (json.has("fireSound")) {
         combat.setFireSound(ResourceLocation.parse(json.get("fireSound").getAsString()));
      }
   }

   private static void parseAmmunition(JsonObject json, Turret.Ammunition ammunition) {
      if (json.has("acceptedAmmo")) {
         ammunition.clearAcceptedAmmo();

         for (JsonElement element : json.getAsJsonArray("acceptedAmmo")) {
            JsonObject ammoObj = element.getAsJsonObject();
            Turret.Ammunition.AmmoType ammo = new Turret.Ammunition.AmmoType();
            if (ammoObj.has("item")) {
               ammo.setItem(ResourceLocation.parse(ammoObj.get("item").getAsString()));
            }

            if (ammoObj.has("bulletType")) {
               ammo.setBulletType(ResourceLocation.parse(ammoObj.get("bulletType").getAsString()));
            }

            if (ammoObj.has("casingType")) {
               ammo.setCasingType(ResourceLocation.parse(ammoObj.get("casingType").getAsString()));
            }

            if (ammoObj.has("damage")) {
               ammo.setDamage(ammoObj.get("damage").getAsDouble());
            }

            ammunition.addAmmoType(ammo);
         }
      }

      if (json.has("casingEjectChance")) {
         ammunition.setCasingEjectChance(json.get("casingEjectChance").getAsFloat());
      }
   }

   private static void parseBehavior(JsonObject json, Turret.Behavior behavior) {
      if (json.has("restingYaw")) {
         behavior.setRestingYaw(json.get("restingYaw").getAsFloat());
      }

      if (json.has("restingPitch")) {
         behavior.setRestingPitch(json.get("restingPitch").getAsFloat());
      }

      if (json.has("disableTime")) {
         behavior.setDisableTime(json.get("disableTime").getAsInt());
      }

      if (json.has("hasOpenAnimation")) {
         behavior.setHasOpenAnimation(json.get("hasOpenAnimation").getAsBoolean());
      }
   }

   private static void parseDisplay(JsonObject json, Turret.Display display) {
      if (json.has("muzzleLength")) {
         display.setMuzzleLength(json.get("muzzleLength").getAsDouble());
      }

      if (json.has("muzzleOffsetY")) {
         display.setMuzzleOffsetY(json.get("muzzleOffsetY").getAsDouble());
      }
   }

   public static void clearAll() {
      TURRET_CONFIGS.clear();
      TURRET_SOURCES.clear();
   }

   /** The raw JSON of every loaded turret, wrapped in a tag so the existing message codec can carry it. */
   public static Map<ResourceLocation, CompoundTag> serializeTurrets() {
      Map<ResourceLocation, CompoundTag> serialized = new HashMap<>();

      for (Map.Entry<ResourceLocation, JsonObject> entry : TURRET_SOURCES.entrySet()) {
         CompoundTag tag = new CompoundTag();
         tag.putString("JSON", GSON.toJson(entry.getValue()));
         serialized.put(entry.getKey(), tag);
      }

      return serialized;
   }

   /**
    * Fills the config map on a client. Without this the map stays empty there, the turret's {@code tick()} returns
    * early on every tick, and the barrel never turns - it only worked after a single-player session because that
    * filled the static map inside the same JVM.
    */
   public static void acceptSynced(Map<ResourceLocation, CompoundTag> turretData) {
      for (Map.Entry<ResourceLocation, CompoundTag> entry : turretData.entrySet()) {
         try {
            JsonObject json = (JsonObject)GSON.fromJson(entry.getValue().getString("JSON"), JsonObject.class);

            if (json != null) {
               Turret turret = parseTurret(json);

               if (turret != null) {
                  TURRET_CONFIGS.put(entry.getKey(), turret);
               }
            }
         } catch (Exception exception) {
            LOGGER.error("Failed to read the synced turret config {}", entry.getKey(), exception);
         }
      }
   }

   @SubscribeEvent
   public static void onDatapackSync(OnDatapackSyncEvent event) {
      S2CMessageSyncTurrets message = new S2CMessageSyncTurrets(serializeTurrets());

      if (event.getPlayer() != null) {
         PacketHandler.getPlayChannel().sendToPlayer(event::getPlayer, message);
      } else {
         for (ServerPlayer player : event.getPlayerList().getPlayers()) {
            PacketHandler.getPlayChannel().sendToPlayer(() -> player, message);
         }
      }
   }

   @SubscribeEvent
   public static void onAddReloadListener(AddReloadListenerEvent event) {
      event.addListener(new SimplePreparableReloadListener<Void>() {
         protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
            TurretManager.clearAll();
            TurretManager.loadTurretConfig(resourceManager, ResourceLocation.fromNamespaceAndPath("scguns", "auto_turret"));
            TurretManager.loadTurretConfig(resourceManager, ResourceLocation.fromNamespaceAndPath("scguns", "basic_turret"));
            TurretManager.loadTurretConfig(resourceManager, ResourceLocation.fromNamespaceAndPath("scguns", "shotgun_turret"));
            TurretManager.loadTurretConfig(resourceManager, ResourceLocation.fromNamespaceAndPath("scguns", "sniper_turret"));
            return null;
         }

         protected void apply(Void object, ResourceManager resourceManager, ProfilerFiller profiler) {
            TurretManager.LOGGER.info("Loaded {} turret configurations", TurretManager.TURRET_CONFIGS.size());
         }
      });
   }
}
