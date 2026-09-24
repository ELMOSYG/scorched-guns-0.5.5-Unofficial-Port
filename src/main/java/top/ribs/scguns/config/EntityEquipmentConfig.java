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
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.util.GunCurseUtil;
import top.ribs.scguns.util.NbtHelper;

@EventBusSubscriber(
   modid = "scguns"
)
public class EntityEquipmentConfig {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final Map<String, EntityEquipmentConfig.EquipmentData> CONFIGS = new HashMap<>();

   public EntityEquipmentConfig() {
   }

   public static void loadConfig(ResourceManager resourceManager, ResourceLocation configLocation) {
      try {
         Resource resource = (Resource)resourceManager.getResource(configLocation).orElse(null);
         if (resource != null) {
            try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
               Gson gson = new Gson();
               JsonObject json = (JsonObject)gson.fromJson(reader, JsonObject.class);
               if (json.has("id")) {
                  String entityId = json.get("id").getAsString();
                  EntityEquipmentConfig.EquipmentData data = parseEquipmentData(json);
                  if (data != null) {
                     CONFIGS.put(entityId, data);
                     int totalEntries = data.entriesBySlot.values().stream().mapToInt(List::size).sum();
                     LOGGER.info(
                        "Loaded equipment config for {} from {}: {} entries across {} slots", entityId, configLocation, totalEntries, data.entriesBySlot.size()
                     );
                  }

                  return;
               }

               LOGGER.warn("Equipment config missing 'id' field: {}", configLocation);
            }

            return;
         } else {
            LOGGER.warn("Equipment config not found: {}", configLocation);
         }
      } catch (Exception var11) {
         LOGGER.error("Failed to load equipment config: {}", configLocation, var11);
      }
   }

   private static EntityEquipmentConfig.EquipmentData parseEquipmentData(JsonObject json) {
      float equipmentChance = json.has("equipment_chance") ? json.get("equipment_chance").getAsFloat() : 0.75F;
      Map<EquipmentSlot, List<EntityEquipmentConfig.EquipmentEntry>> entriesBySlot = new HashMap<>();
      if (json.has("items")) {
         for (JsonElement element : json.getAsJsonArray("items")) {
            JsonObject itemObj = element.getAsJsonObject();
            EntityEquipmentConfig.EquipmentEntry entry = parseEntry(itemObj);
            if (entry != null) {
               entriesBySlot.computeIfAbsent(entry.slot, k -> new ArrayList<>()).add(entry);
            }
         }
      }

      return new EntityEquipmentConfig.EquipmentData(equipmentChance, entriesBySlot);
   }

   @Nullable
   private static EntityEquipmentConfig.EquipmentEntry parseEntry(JsonObject json) {
      try {
         String itemId = json.get("item").getAsString();
         Item item = (Item)BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
         if (item == null) {
            LOGGER.warn("Unknown item: {}", itemId);
            return null;
         } else {
            float spawnWeight = json.has("weight") ? json.get("weight").getAsFloat() : 1.0F;
            float dropChance = json.has("drop_chance") ? json.get("drop_chance").getAsFloat() : 0.2F;
            EquipmentSlot slot = EquipmentSlot.MAINHAND;
            if (json.has("slot")) {
               String slotName = json.get("slot").getAsString().toLowerCase();

               slot = switch (slotName) {
                  case "head", "helmet" -> EquipmentSlot.HEAD;
                  case "chest", "chestplate" -> EquipmentSlot.CHEST;
                  case "legs", "leggings" -> EquipmentSlot.LEGS;
                  case "feet", "boots" -> EquipmentSlot.FEET;
                  case "offhand" -> EquipmentSlot.OFFHAND;
                  default -> EquipmentSlot.MAINHAND;
               };
            }

            Float minDurability = json.has("min_durability") ? json.get("min_durability").getAsFloat() : null;
            Float maxDurability = json.has("max_durability") ? json.get("max_durability").getAsFloat() : null;
            return new EntityEquipmentConfig.EquipmentEntry(item, spawnWeight, dropChance, slot, minDurability, maxDurability);
         }
      } catch (Exception var9) {
         LOGGER.error("Error parsing equipment entry", var9);
         return null;
      }
   }

   @Nullable
   public static EntityEquipmentConfig.EquipmentData getEquipmentData(String entityId) {
      return CONFIGS.get(entityId);
   }

   public static void equipEntity(Mob mob, String entityId) {
      EntityEquipmentConfig.EquipmentData data = getEquipmentData(entityId);
      if (data != null) {
         if (!(mob.getRandom().nextFloat() >= data.equipmentChance)) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
               EntityEquipmentConfig.EquipmentEntry entry = data.selectRandom(slot, mob.getRandom());
               if (entry != null) {
                  ItemStack stack = entry.createItemStack(mob.getRandom());
                  if (slot == EquipmentSlot.MAINHAND) {
                     GunCurseUtil.applyCurseIfRoll(stack, mob.getRandom());
                  }

                  mob.setItemSlot(slot, stack);
                  mob.setDropChance(slot, entry.dropChance);
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onAddReloadListener(AddReloadListenerEvent event) {
      event.addListener(new SimplePreparableReloadListener<Void>() {
         protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
            return null;
         }

         protected void apply(Void object, ResourceManager resourceManager, ProfilerFiller profiler) {
            EntityEquipmentConfig.CONFIGS.clear();
            EntityEquipmentConfig.loadAllConfigs(resourceManager);
         }
      });
   }

   private static void loadAllConfigs(ResourceManager resourceManager) {
      String namespace = "scguns";
      String folderPath = "entity/equipment";

      try {
         Map<ResourceLocation, Resource> resources = resourceManager.listResources(folderPath, locationx -> locationx.getPath().endsWith(".json"));

         for (ResourceLocation location : resources.keySet()) {
            if (location.getNamespace().equals(namespace)) {
               loadConfig(resourceManager, location);
            }
         }
      } catch (Exception var6) {
         LOGGER.error("Failed to scan equipment configs directory", var6);
      }
   }

   public static record EquipmentData(float equipmentChance, Map<EquipmentSlot, List<EntityEquipmentConfig.EquipmentEntry>> entriesBySlot) {
      public EquipmentData(float equipmentChance, Map<EquipmentSlot, List<EntityEquipmentConfig.EquipmentEntry>> entriesBySlot) {
         this.equipmentChance = equipmentChance;
         this.entriesBySlot = entriesBySlot;
      }

      @Nullable
      public EntityEquipmentConfig.EquipmentEntry selectRandom(EquipmentSlot slot, RandomSource random) {
         List<EntityEquipmentConfig.EquipmentEntry> entries = this.entriesBySlot.get(slot);
         if (entries != null && !entries.isEmpty()) {
            float totalWeight = 0.0F;

            for (EntityEquipmentConfig.EquipmentEntry entry : entries) {
               totalWeight += entry.spawnWeight;
            }

            float roll = random.nextFloat() * totalWeight;
            float currentWeight = 0.0F;

            for (EntityEquipmentConfig.EquipmentEntry entry : entries) {
               currentWeight += entry.spawnWeight;
               if (roll < currentWeight) {
                  return entry;
               }
            }

            return entries.get(entries.size() - 1);
         } else {
            return null;
         }
      }
   }

   public static record EquipmentEntry(
      Item item, float spawnWeight, float dropChance, EquipmentSlot slot, @Nullable Float minDurability, @Nullable Float maxDurability
   ) {
      public EquipmentEntry(Item item, float spawnWeight, float dropChance, EquipmentSlot slot, @Nullable Float minDurability, @Nullable Float maxDurability) {
         this.item = item;
         this.spawnWeight = spawnWeight;
         this.dropChance = dropChance;
         this.slot = slot;
         this.minDurability = minDurability;
         this.maxDurability = maxDurability;
      }

      public ItemStack createItemStack(RandomSource random) {
         ItemStack stack = new ItemStack(this.item);
         if (stack.isDamageableItem() && this.minDurability != null && this.maxDurability != null) {
            float durabilityPercent = this.minDurability + random.nextFloat() * (this.maxDurability - this.minDurability);
            int damage = (int)((float)stack.getMaxDamage() * (1.0F - durabilityPercent));
            stack.setDamageValue(damage);
         }

         // A gun must carry an AmmoCount or the mob's aim goal has nothing to
         // read. Only the creative tab used to initialise it, so every mob
         // equipped from this config held a gun with no custom data at all.
         if (this.item instanceof GunItem gunItem) {
            NbtHelper.getOrCreateTag(stack)
               .putInt("AmmoCount", gunItem.getModifiedGun(stack).getReloads().getMaxAmmo());
         }

         return stack;
      }
   }
}
