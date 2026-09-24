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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@EventBusSubscriber(
   modid = "scguns"
)
public class GunnerMobConfig {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final Map<ResourceLocation, GunnerMobConfig.MobGunnerData> GUNNER_MOBS = new HashMap<>();
   private static final ResourceLocation CONFIG_LOCATION = ResourceLocation.fromNamespaceAndPath("scguns", "entity/gunner_mobs.json");

   public GunnerMobConfig() {
   }

   public static void loadConfig(ResourceManager resourceManager) {
      GUNNER_MOBS.clear();

      try {
         Resource resource = (Resource)resourceManager.getResource(CONFIG_LOCATION).orElse(null);
         if (resource != null) {
            try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
               Gson gson = new Gson();
               JsonObject json = (JsonObject)gson.fromJson(reader, JsonObject.class);
               if (json != null && json.has("mobs")) {
                  JsonObject mobsObj = json.getAsJsonObject("mobs");

                  for (Entry<String, JsonElement> entry : mobsObj.entrySet()) {
                     String entityId = entry.getKey();
                     JsonObject mobData = entry.getValue().getAsJsonObject();
                     GunnerMobConfig.MobGunnerData gunnerData = parseMobData(mobData);
                     if (gunnerData != null) {
                        ResourceLocation entityKey = ResourceLocation.parse(entityId);
                        GUNNER_MOBS.put(entityKey, gunnerData);
                     }
                  }
               }

               LOGGER.info("Loaded gunner mob config: {} mob types configured", GUNNER_MOBS.size());
            }
         } else {
            LOGGER.warn("Gunner mob config not found at {}", CONFIG_LOCATION);
            loadDefaults();
         }
      } catch (Exception var14) {
         LOGGER.error("Failed to load gunner mob config at {}", CONFIG_LOCATION, var14);
         loadDefaults();
      }
   }

   private static GunnerMobConfig.MobGunnerData parseMobData(JsonObject mobData) {
      try {
         float spawnChance = mobData.has("spawn_chance") ? mobData.get("spawn_chance").getAsFloat() : 0.3F;
         int aiDifficulty = mobData.has("ai_difficulty") ? mobData.get("ai_difficulty").getAsInt() : 2;
         float weaponDropChance = mobData.has("weapon_drop_chance") ? mobData.get("weapon_drop_chance").getAsFloat() : 0.085F;
         List<Item> weapons = new ArrayList<>();
         if (mobData.has("weapons")) {
            for (JsonElement weaponElement : mobData.getAsJsonArray("weapons")) {
               String weaponId = weaponElement.getAsString();
               Item weapon = (Item)BuiltInRegistries.ITEM.get(ResourceLocation.parse(weaponId));
               if (weapon != null) {
                  weapons.add(weapon);
               } else {
                  LOGGER.warn("Unknown weapon item: {}", weaponId);
               }
            }
         }

         List<GunnerMobConfig.ArmorPiece> armor = new ArrayList<>();
         if (mobData.has("armor")) {
            for (JsonElement armorElement : mobData.getAsJsonArray("armor")) {
               JsonObject armorObj = armorElement.getAsJsonObject();
               String itemId = armorObj.get("item").getAsString();
               String slot = armorObj.get("slot").getAsString();
               float chance = armorObj.has("chance") ? armorObj.get("chance").getAsFloat() : 1.0F;
               Item armorItem = (Item)BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
               if (armorItem != null) {
                  armor.add(new GunnerMobConfig.ArmorPiece(armorItem, slot, chance));
               } else {
                  LOGGER.warn("Unknown armor item: {}", itemId);
               }
            }
         }

         return new GunnerMobConfig.MobGunnerData(spawnChance, weapons, armor, aiDifficulty, weaponDropChance);
      } catch (Exception var14) {
         LOGGER.error("Error parsing mob gunner data", var14);
         return null;
      }
   }

   private static void loadDefaults() {
      LOGGER.info("Loading default gunner mob configuration");
   }

   public static GunnerMobConfig.MobGunnerData getGunnerData(EntityType<?> entityType) {
      return GUNNER_MOBS.get(BuiltInRegistries.ENTITY_TYPE.getKey(entityType));
   }

   public static boolean canSpawnAsGunner(EntityType<?> entityType) {
      return GUNNER_MOBS.containsKey(BuiltInRegistries.ENTITY_TYPE.getKey(entityType));
   }

   @SubscribeEvent
   public static void onAddReloadListener(AddReloadListenerEvent event) {
      event.addListener(new SimplePreparableReloadListener<Void>() {
         protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
            return null;
         }

         protected void apply(Void object, ResourceManager resourceManager, ProfilerFiller profiler) {
            GunnerMobConfig.loadConfig(resourceManager);
         }
      });
   }

   public static record ArmorPiece(Item item, String slot, float spawnChance) {
      public ArmorPiece(Item item, String slot, float spawnChance) {
         this.item = item;
         this.slot = slot;
         this.spawnChance = spawnChance;
      }
   }

   public static record MobGunnerData(
      float spawnChance, List<Item> allowedWeapons, List<GunnerMobConfig.ArmorPiece> allowedArmor, int aiDifficulty, float weaponDropChance
   ) {
      public MobGunnerData(
         float spawnChance, List<Item> allowedWeapons, List<GunnerMobConfig.ArmorPiece> allowedArmor, int aiDifficulty, float weaponDropChance
      ) {
         this.spawnChance = spawnChance;
         this.allowedWeapons = allowedWeapons;
         this.allowedArmor = allowedArmor;
         this.aiDifficulty = aiDifficulty;
         this.weaponDropChance = weaponDropChance;
      }

      public Item getRandomWeapon(RandomSource random) {
         return this.allowedWeapons.isEmpty() ? null : this.allowedWeapons.get(random.nextInt(this.allowedWeapons.size()));
      }
   }
}
