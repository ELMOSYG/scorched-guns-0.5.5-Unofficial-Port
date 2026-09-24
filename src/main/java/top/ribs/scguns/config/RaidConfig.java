package top.ribs.scguns.config;


import net.minecraft.core.registries.BuiltInRegistries;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import top.ribs.scguns.entity.ai.AIType;

@EventBusSubscriber(
   modid = "scguns"
)
public class RaidConfig {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final Map<String, RaidConfig.RaidData> RAIDS_BY_ID = new HashMap<>();
   private static final Map<Integer, List<RaidConfig.RaidData>> RAIDS_BY_LEVEL = new HashMap<>();

   public RaidConfig() {
   }

   public static void loadRaidConfigs(ResourceManager resourceManager) {
      RAIDS_BY_ID.clear();
      RAIDS_BY_LEVEL.clear();

      for (Entry<ResourceLocation, Resource> entry : resourceManager.listResources("raids", loc -> loc.getPath().endsWith(".json")).entrySet()) {
         ResourceLocation location = entry.getKey();
         String path = location.getPath();
         String fileName = path.substring(path.lastIndexOf(47) + 1);
         String raidId = fileName.replace("_raid.json", "").replace(".json", "");

         try (InputStreamReader reader = new InputStreamReader(entry.getValue().open(), StandardCharsets.UTF_8)) {
            Gson gson = new Gson();
            JsonObject json = (JsonObject)gson.fromJson(reader, JsonObject.class);
            RaidConfig.RaidData raidData = parseRaidData(json);
            if (raidData != null) {
               RAIDS_BY_ID.put(raidData.raidId(), raidData);
               if (raidData.raidLevel() != null) {
                  RAIDS_BY_LEVEL.computeIfAbsent(raidData.raidLevel(), k -> new ArrayList<>()).add(raidData);
                  LOGGER.info("Loaded progression raid: {} (Level: {})", raidData.raidId(), raidData.raidLevel());
               } else {
                  LOGGER.info("Loaded custom raid: {}", raidData.raidId());
               }
            }
         } catch (Exception var13) {
            LOGGER.error("Failed to load raid config: {}", raidId, var13);
         }
      }

      LOGGER.info(
         "Loaded {} total raids ({} progression, {} custom)",
         RAIDS_BY_ID.size(),
         RAIDS_BY_LEVEL.values().stream().mapToInt(List::size).sum(),
         RAIDS_BY_ID.size() - RAIDS_BY_LEVEL.values().stream().mapToInt(List::size).sum()
      );
   }

   @Nullable
   public static RaidConfig.RaidData getRaidById(String raidId) {
      return RAIDS_BY_ID.get(raidId);
   }

   @Nullable
   public static RaidConfig.RaidData getRaidByRaidId(String raidId) {
      return getRaidById(raidId);
   }

   public static List<RaidConfig.RaidData> getRaidsAtLevel(int level) {
      return RAIDS_BY_LEVEL.getOrDefault(level, Collections.emptyList());
   }

   public static List<RaidConfig.RaidData> getRaidsForLevel(int playerLevel) {
      List<RaidConfig.RaidData> availableRaids = new ArrayList<>();

      for (int level = 1; level <= playerLevel; level++) {
         availableRaids.addAll(getRaidsAtLevel(level));
      }

      return availableRaids;
   }

   public static int getMaxRaidLevel() {
      return RAIDS_BY_LEVEL.keySet().stream().max(Integer::compareTo).orElse(0);
   }

   public static Collection<RaidConfig.RaidData> getAllRaids() {
      return RAIDS_BY_ID.values();
   }

   public static Collection<RaidConfig.RaidData> getProgressionRaids() {
      List<RaidConfig.RaidData> progressionRaids = new ArrayList<>();

      for (RaidConfig.RaidData raid : RAIDS_BY_ID.values()) {
         if (raid.raidLevel() != null) {
            progressionRaids.add(raid);
         }
      }

      return progressionRaids;
   }

   public static Collection<RaidConfig.RaidData> getCustomRaids() {
      List<RaidConfig.RaidData> customRaids = new ArrayList<>();

      for (RaidConfig.RaidData raid : RAIDS_BY_ID.values()) {
         if (raid.raidLevel() == null) {
            customRaids.add(raid);
         }
      }

      return customRaids;
   }

   public static boolean hasRaidWithId(String raidId) {
      return RAIDS_BY_ID.containsKey(raidId);
   }

   @Nullable
   private static RaidConfig.RaidData parseRaidData(JsonObject json) {
      try {
         String raidId = json.get("raid_id").getAsString();
         Integer raidLevel = null;
         if (json.has("raid_level")) {
            raidLevel = json.get("raid_level").getAsInt();
         }

         RaidConfig.BossData boss = parseBossData(json.getAsJsonObject("boss"));
         RaidConfig.HenchmenData henchmen = parseHenchmenData(json.getAsJsonObject("henchmen"));
         RaidConfig.SpawnConditions conditions = parseSpawnConditions(json.getAsJsonObject("spawn_conditions"));
         return new RaidConfig.RaidData(raidId, raidLevel, boss, henchmen, conditions);
      } catch (Exception var6) {
         LOGGER.error("Error parsing raid data", var6);
         return null;
      }
   }

   @Nullable
   private static RaidConfig.BossData parseBossData(JsonObject json) {
      try {
         String entityId = json.get("entity_type").getAsString();
         EntityType<?> entityType = (EntityType<?>)BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(entityId));
         if (entityType == null) {
            LOGGER.warn("Unknown entity type: {}", entityId);
            return null;
         } else {
            String customName = json.has("custom_name") ? json.get("custom_name").getAsString() : null;
            RaidConfig.HealthConfig healthConfig = parseHealthConfig(json);
            RaidConfig.WeaponEntry weapon = null;
            if (json.has("weapon")) {
               JsonObject weaponObj = json.getAsJsonObject("weapon");
               Item weaponItem = (Item)BuiltInRegistries.ITEM.get(ResourceLocation.parse(weaponObj.get("item").getAsString()));
               float dropChance = weaponObj.has("drop_chance") ? weaponObj.get("drop_chance").getAsFloat() : 0.085F;
               CompoundTag nbt = null;
               if (weaponObj.has("nbt")) {
                  nbt = parseNBT(weaponObj.getAsJsonObject("nbt"));
               }

               weapon = new RaidConfig.WeaponEntry(weaponItem, dropChance, nbt);
            }

            List<RaidConfig.ArmorEntry> armor = new ArrayList<>();
            if (json.has("armor")) {
               for (JsonElement element : json.getAsJsonArray("armor")) {
                  JsonObject armorObj = element.getAsJsonObject();
                  Item armorItem = (Item)BuiltInRegistries.ITEM.get(ResourceLocation.parse(armorObj.get("item").getAsString()));
                  String slot = armorObj.get("slot").getAsString();
                  float dropChance = armorObj.has("drop_chance") ? armorObj.get("drop_chance").getAsFloat() : 0.085F;
                  CompoundTag nbt = null;
                  if (armorObj.has("nbt")) {
                     nbt = parseNBT(armorObj.getAsJsonObject("nbt"));
                  }

                  if (armorItem != null) {
                     armor.add(new RaidConfig.ArmorEntry(armorItem, slot, dropChance, nbt));
                  }
               }
            }

            List<RaidConfig.EffectEntry> effects = parseEffects(json);
            int aiDifficulty = json.has("ai_difficulty") ? json.get("ai_difficulty").getAsInt() : 3;
            AIType aiType = json.has("ai_type") ? AIType.valueOf(json.get("ai_type").getAsString()) : AIType.DEFAULT;
            ResourceLocation lootTable = json.has("special_loot_table") ? ResourceLocation.parse(json.get("special_loot_table").getAsString()) : null;
            RaidConfig.MountData mount = null;
            if (json.has("mount")) {
               mount = parseMountData(json.getAsJsonObject("mount"));
            }

            return new RaidConfig.BossData(entityType, customName, healthConfig, weapon, armor, effects, aiDifficulty, aiType, lootTable, mount);
         }
      } catch (Exception var15) {
         LOGGER.error("Error parsing boss data", var15);
         return null;
      }
   }

   @Nullable
   private static RaidConfig.MountData parseMountData(JsonObject json) {
      try {
         String mountId = json.get("entity_type").getAsString();
         EntityType<?> mountType = (EntityType<?>)BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(mountId));
         if (mountType == null) {
            LOGGER.warn("Unknown mount type: {}", mountId);
            return null;
         } else {
            RaidConfig.HealthConfig healthConfig = parseHealthConfig(json);
            List<RaidConfig.ArmorEntry> armor = new ArrayList<>();
            if (json.has("armor")) {
               for (JsonElement element : json.getAsJsonArray("armor")) {
                  JsonObject armorObj = element.getAsJsonObject();
                  Item armorItem = (Item)BuiltInRegistries.ITEM.get(ResourceLocation.parse(armorObj.get("item").getAsString()));
                  String slot = armorObj.get("slot").getAsString();
                  float dropChance = armorObj.has("drop_chance") ? armorObj.get("drop_chance").getAsFloat() : 0.085F;
                  CompoundTag nbt = null;
                  if (armorObj.has("nbt")) {
                     nbt = parseNBT(armorObj.getAsJsonObject("nbt"));
                  }

                  if (armorItem != null) {
                     armor.add(new RaidConfig.ArmorEntry(armorItem, slot, dropChance, nbt));
                  }
               }
            }

            List<RaidConfig.EffectEntry> effects = parseEffects(json);
            boolean dropsLoot = !json.has("drops_loot") || json.get("drops_loot").getAsBoolean();
            return new RaidConfig.MountData(mountType, healthConfig, armor, effects, dropsLoot);
         }
      } catch (Exception var13) {
         LOGGER.error("Error parsing mount data", var13);
         return null;
      }
   }

   @Nullable
   private static RaidConfig.HenchmenData parseHenchmenData(JsonObject json) {
      try {
         List<RaidConfig.HenchmanType> types = new ArrayList<>();
         if (json.has("types")) {
            for (JsonElement element : json.getAsJsonArray("types")) {
               JsonObject typeObj = element.getAsJsonObject();
               String entityId = typeObj.get("entity_type").getAsString();
               EntityType<?> entityType = (EntityType<?>)BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(entityId));
               if (entityType != null) {
                  float weight = typeObj.has("weight") ? typeObj.get("weight").getAsFloat() : 1.0F;
                  RaidConfig.HealthConfig healthConfig = parseHealthConfig(typeObj);
                  List<Item> weapons = new ArrayList<>();
                  if (typeObj.has("weapons")) {
                     for (JsonElement weaponElement : typeObj.getAsJsonArray("weapons")) {
                        Item weapon = (Item)BuiltInRegistries.ITEM.get(ResourceLocation.parse(weaponElement.getAsString()));
                        if (weapon != null) {
                           weapons.add(weapon);
                        }
                     }
                  }

                  List<RaidConfig.ArmorEntry> armor = new ArrayList<>();
                  if (typeObj.has("armor")) {
                     for (JsonElement armorElement : typeObj.getAsJsonArray("armor")) {
                        JsonObject armorObj = armorElement.getAsJsonObject();
                        Item armorItem = (Item)BuiltInRegistries.ITEM.get(ResourceLocation.parse(armorObj.get("item").getAsString()));
                        String slot = armorObj.get("slot").getAsString();
                        float chance = armorObj.has("chance") ? armorObj.get("chance").getAsFloat() : 0.5F;
                        CompoundTag nbt = null;
                        if (armorObj.has("nbt")) {
                           nbt = parseNBT(armorObj.getAsJsonObject("nbt"));
                        }

                        if (armorItem != null) {
                           armor.add(new RaidConfig.ArmorEntry(armorItem, slot, chance, nbt));
                        }
                     }
                  }

                  List<RaidConfig.EffectEntry> effects = parseEffects(typeObj);
                  int aiDifficulty = typeObj.has("ai_difficulty") ? typeObj.get("ai_difficulty").getAsInt() : 2;
                  AIType aiType = typeObj.has("ai_type") ? AIType.valueOf(typeObj.get("ai_type").getAsString()) : AIType.DEFAULT;
                  types.add(new RaidConfig.HenchmanType(entityType, weight, healthConfig, weapons, armor, effects, aiDifficulty, aiType));
               }
            }
         }

         int maxAlive = json.has("max_concurrent") ? json.get("max_concurrent").getAsInt() : (json.has("max_alive") ? json.get("max_alive").getAsInt() : 4);
         int maxTotal = json.has("max_total") ? json.get("max_total").getAsInt() : 15;
         int spawnInterval = json.has("spawn_interval_ticks") ? json.get("spawn_interval_ticks").getAsInt() : 200;
         int spawnRadius = json.has("spawn_radius") ? json.get("spawn_radius").getAsInt() : 20;
         int spawnAttempts = json.has("spawn_attempts_per_wave") ? json.get("spawn_attempts_per_wave").getAsInt() : 3;
         return new RaidConfig.HenchmenData(types, maxAlive, maxTotal, spawnInterval, spawnRadius, spawnAttempts);
      } catch (Exception var20) {
         LOGGER.error("Error parsing henchmen data", var20);
         return null;
      }
   }

   @Nullable
   private static CompoundTag parseNBT(JsonObject json) {
      try {
         CompoundTag tag = new CompoundTag();

         for (Entry<String, JsonElement> entry : json.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            if (value.isJsonPrimitive()) {
               JsonPrimitive primitive = value.getAsJsonPrimitive();
               if (primitive.isNumber()) {
                  if (primitive.getAsString().contains(".")) {
                     tag.putFloat(key, primitive.getAsFloat());
                  } else {
                     tag.putInt(key, primitive.getAsInt());
                  }
               } else if (primitive.isString()) {
                  tag.putString(key, primitive.getAsString());
               } else if (primitive.isBoolean()) {
                  tag.putBoolean(key, primitive.getAsBoolean());
               }
            } else if (!value.isJsonArray()) {
               if (value.isJsonObject()) {
                  tag.put(key, Objects.requireNonNull(parseNBT(value.getAsJsonObject())));
               }
            } else {
               JsonArray array = value.getAsJsonArray();
               ListTag list;
               Iterator var10;
               switch (key) {
                  case "Enchantments":
                     list = getTags(array);
                     tag.put(key, list);
                     continue;
                  case "Lore":
                     list = new ListTag();

                     for (JsonElement elemx : array) {
                        list.add(StringTag.valueOf(elemx.getAsString()));
                     }

                     tag.put(key, list);
                     continue;
                  case "AttributeModifiers":
                     list = new ListTag();

                     for (JsonElement elem : array) {
                        if (elem.isJsonObject()) {
                           list.add(parseNBT(elem.getAsJsonObject()));
                        }
                     }

                     tag.put(key, list);
                     continue;
                  default:
                     list = new ListTag();
                     var10 = array.iterator();
               }

               while (var10.hasNext()) {
                  JsonElement elemx = (JsonElement)var10.next();
                  if (elemx.isJsonObject()) {
                     list.add(parseNBT(elemx.getAsJsonObject()));
                  } else if (elemx.isJsonPrimitive()) {
                     JsonPrimitive prim = elemx.getAsJsonPrimitive();
                     if (prim.isString()) {
                        list.add(StringTag.valueOf(prim.getAsString()));
                     } else if (prim.isNumber()) {
                        CompoundTag numTag = new CompoundTag();
                        numTag.putInt("value", prim.getAsInt());
                        list.add(numTag);
                     }
                  }
               }

               tag.put(key, list);
            }
         }

         return tag;
      } catch (Exception var14) {
         LOGGER.error("Error parsing NBT data", var14);
         return null;
      }
   }

   @NotNull
   private static ListTag getTags(JsonArray array) {
      ListTag enchantments = new ListTag();

      for (JsonElement elem : array) {
         if (elem.isJsonObject()) {
            JsonObject enchObj = elem.getAsJsonObject();
            CompoundTag enchTag = new CompoundTag();
            enchTag.putString("id", enchObj.get("id").getAsString());
            enchTag.putInt("lvl", enchObj.get("lvl").getAsInt());
            enchantments.add(enchTag);
         }
      }

      return enchantments;
   }

   @Nullable
   private static RaidConfig.HealthConfig parseHealthConfig(JsonObject json) {
      Float fixedHealth = json.has("fixed_health") ? json.get("fixed_health").getAsFloat() : null;
      Float healthMultiplier = json.has("health_multiplier") ? json.get("health_multiplier").getAsFloat() : null;
      return new RaidConfig.HealthConfig(fixedHealth, healthMultiplier);
   }

   private static List<RaidConfig.EffectEntry> parseEffects(JsonObject json) {
      List<RaidConfig.EffectEntry> effects = new ArrayList<>();
      if (json.has("effects")) {
         for (JsonElement element : json.getAsJsonArray("effects")) {
            JsonObject effectObj = element.getAsJsonObject();
            String effectId = effectObj.get("effect").getAsString();
            MobEffect effect = (MobEffect)BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.parse(effectId));
            if (effect != null) {
               int amplifier = effectObj.has("amplifier") ? effectObj.get("amplifier").getAsInt() : 0;
               int duration = effectObj.has("duration") ? effectObj.get("duration").getAsInt() : -1;
               boolean ambient = effectObj.has("ambient") && effectObj.get("ambient").getAsBoolean();
               boolean visible = !effectObj.has("visible") || effectObj.get("visible").getAsBoolean();
               effects.add(new RaidConfig.EffectEntry(effect, amplifier, duration, ambient, visible));
            }
         }
      }

      return effects;
   }

   @Nullable
   private static RaidConfig.SpawnConditions parseSpawnConditions(JsonObject json) {
      try {
         int minPlayers = json.has("min_players_nearby") ? json.get("min_players_nearby").getAsInt() : 1;
         int searchRadius = json.has("search_radius") ? json.get("search_radius").getAsInt() : 64;
         List<ResourceLocation> validDimensions = new ArrayList<>();
         if (json.has("valid_dimensions")) {
            for (JsonElement element : json.getAsJsonArray("valid_dimensions")) {
               validDimensions.add(ResourceLocation.parse(element.getAsString()));
            }
         } else {
            validDimensions.add(ResourceLocation.parse("minecraft:overworld"));
         }

         String announcement = json.has("announcement_message") ? json.get("announcement_message").getAsString() : "Â§cÂ§lA raid approaches!";
         return new RaidConfig.SpawnConditions(minPlayers, searchRadius, validDimensions, announcement);
      } catch (Exception var7) {
         LOGGER.error("Error parsing spawn conditions", var7);
         return null;
      }
   }

   @SubscribeEvent
   public static void onAddReloadListener(AddReloadListenerEvent event) {
      event.addListener(new SimplePreparableReloadListener<Void>() {
         protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
            return null;
         }

         protected void apply(Void object, ResourceManager resourceManager, ProfilerFiller profiler) {
            RaidConfig.loadRaidConfigs(resourceManager);
         }
      });
   }

   public static record ArmorEntry(Item item, String slot, float dropChance, @Nullable CompoundTag nbt) {
      public ArmorEntry(Item item, String slot, float dropChance, @Nullable CompoundTag nbt) {
         this.item = item;
         this.slot = slot;
         this.dropChance = dropChance;
         this.nbt = nbt;
      }
   }

   public static record BossData(
      EntityType<?> entityType,
      @Nullable String customName,
      RaidConfig.HealthConfig healthConfig,
      @Nullable RaidConfig.WeaponEntry weapon,
      List<RaidConfig.ArmorEntry> armor,
      List<RaidConfig.EffectEntry> effects,
      int aiDifficulty,
      AIType aiType,
      @Nullable ResourceLocation specialLootTable,
      @Nullable RaidConfig.MountData mount
   ) {
      public BossData(
         EntityType<?> entityType,
         @Nullable String customName,
         RaidConfig.HealthConfig healthConfig,
         @Nullable RaidConfig.WeaponEntry weapon,
         List<RaidConfig.ArmorEntry> armor,
         List<RaidConfig.EffectEntry> effects,
         int aiDifficulty,
         AIType aiType,
         @Nullable ResourceLocation specialLootTable,
         @Nullable RaidConfig.MountData mount
      ) {
         this.entityType = entityType;
         this.customName = customName;
         this.healthConfig = healthConfig;
         this.weapon = weapon;
         this.armor = armor;
         this.effects = effects;
         this.aiDifficulty = aiDifficulty;
         this.aiType = aiType;
         this.specialLootTable = specialLootTable;
         this.mount = mount;
      }
   }

   public static record EffectEntry(MobEffect effect, int amplifier, int duration, boolean ambient, boolean visible) {
      public EffectEntry(MobEffect effect, int amplifier, int duration, boolean ambient, boolean visible) {
         this.effect = effect;
         this.amplifier = amplifier;
         this.duration = duration;
         this.ambient = ambient;
         this.visible = visible;
      }
   }

   public static record HealthConfig(@Nullable Float fixedHealth, @Nullable Float healthMultiplier) {
      public HealthConfig(@Nullable Float fixedHealth, @Nullable Float healthMultiplier) {
         this.fixedHealth = fixedHealth;
         this.healthMultiplier = healthMultiplier;
      }

      public boolean useMultiplier() {
         return this.healthMultiplier != null;
      }
   }

   public static record HenchmanType(
      EntityType<?> entityType,
      float weight,
      RaidConfig.HealthConfig healthConfig,
      List<Item> weapons,
      List<RaidConfig.ArmorEntry> armor,
      List<RaidConfig.EffectEntry> effects,
      int aiDifficulty,
      AIType aiType
   ) {
      public HenchmanType(
         EntityType<?> entityType,
         float weight,
         RaidConfig.HealthConfig healthConfig,
         List<Item> weapons,
         List<RaidConfig.ArmorEntry> armor,
         List<RaidConfig.EffectEntry> effects,
         int aiDifficulty,
         AIType aiType
      ) {
         this.entityType = entityType;
         this.weight = weight;
         this.healthConfig = healthConfig;
         this.weapons = weapons;
         this.armor = armor;
         this.effects = effects;
         this.aiDifficulty = aiDifficulty;
         this.aiType = aiType;
      }
   }

   public static record HenchmenData(
      List<RaidConfig.HenchmanType> types, int maxAlive, int maxTotal, int spawnIntervalTicks, int spawnRadius, int spawnAttemptsPerWave
   ) {
      public HenchmenData(List<RaidConfig.HenchmanType> types, int maxAlive, int maxTotal, int spawnIntervalTicks, int spawnRadius, int spawnAttemptsPerWave) {
         this.types = types;
         this.maxAlive = maxAlive;
         this.maxTotal = maxTotal;
         this.spawnIntervalTicks = spawnIntervalTicks;
         this.spawnRadius = spawnRadius;
         this.spawnAttemptsPerWave = spawnAttemptsPerWave;
      }

      @Nullable
      public RaidConfig.HenchmanType selectRandomType(RandomSource random) {
         if (this.types.isEmpty()) {
            return null;
         } else {
            float totalWeight = 0.0F;

            for (RaidConfig.HenchmanType type : this.types) {
               totalWeight += type.weight;
            }

            float roll = random.nextFloat() * totalWeight;
            float currentWeight = 0.0F;

            for (RaidConfig.HenchmanType type : this.types) {
               currentWeight += type.weight;
               if (roll < currentWeight) {
                  return type;
               }
            }

            return this.types.get(this.types.size() - 1);
         }
      }
   }

   public static record MountData(
      EntityType<?> entityType,
      RaidConfig.HealthConfig healthConfig,
      List<RaidConfig.ArmorEntry> armor,
      List<RaidConfig.EffectEntry> effects,
      boolean mountDropsLoot
   ) {
      public MountData(
         EntityType<?> entityType,
         RaidConfig.HealthConfig healthConfig,
         List<RaidConfig.ArmorEntry> armor,
         List<RaidConfig.EffectEntry> effects,
         boolean mountDropsLoot
      ) {
         this.entityType = entityType;
         this.healthConfig = healthConfig;
         this.armor = armor;
         this.effects = effects;
         this.mountDropsLoot = mountDropsLoot;
      }
   }

   public static record RaidData(
      String raidId, @Nullable Integer raidLevel, RaidConfig.BossData boss, RaidConfig.HenchmenData henchmen, RaidConfig.SpawnConditions spawnConditions
   ) {
      public RaidData(
         String raidId, @Nullable Integer raidLevel, RaidConfig.BossData boss, RaidConfig.HenchmenData henchmen, RaidConfig.SpawnConditions spawnConditions
      ) {
         this.raidId = raidId;
         this.raidLevel = raidLevel;
         this.boss = boss;
         this.henchmen = henchmen;
         this.spawnConditions = spawnConditions;
      }
   }

   public static record SpawnConditions(int minPlayersNearby, int searchRadius, List<ResourceLocation> validDimensions, String announcementMessage) {
      public SpawnConditions(int minPlayersNearby, int searchRadius, List<ResourceLocation> validDimensions, String announcementMessage) {
         this.minPlayersNearby = minPlayersNearby;
         this.searchRadius = searchRadius;
         this.validDimensions = validDimensions;
         this.announcementMessage = announcementMessage;
      }
   }

   public static record WeaponEntry(Item item, float dropChance, @Nullable CompoundTag nbt) {
      public WeaponEntry(Item item, float dropChance, @Nullable CompoundTag nbt) {
         this.item = item;
         this.dropChance = dropChance;
         this.nbt = nbt;
      }
   }
}
