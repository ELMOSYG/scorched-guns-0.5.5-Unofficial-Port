package top.ribs.scguns.common.exosuit;


import net.minecraft.core.registries.BuiltInRegistries;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.bus.api.SubscribeEvent;
import top.ribs.scguns.network.PacketHandler;
import top.ribs.scguns.network.message.S2CMessageSyncUpgradeRegistry;

public class ExoSuitUpgradeManager extends SimpleJsonResourceReloadListener {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
   private static ExoSuitUpgradeManager instance;
   private static final Map<ResourceLocation, ExoSuitUpgrade> itemUpgrades = new HashMap<>();

   public ExoSuitUpgradeManager() {
      super(GSON, "upgrades");
      instance = this;
   }

   protected void apply(Map<ResourceLocation, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
      itemUpgrades.clear();
      pObject.forEach((resourceLocation, jsonElement) -> {
         try {
            if (jsonElement.isJsonObject()) {
               JsonObject jsonObject = jsonElement.getAsJsonObject();
               ResourceLocation itemId = this.getItemIdFromJson(jsonObject, resourceLocation);
               if (BuiltInRegistries.ITEM.containsKey(itemId)) {
                  ExoSuitUpgrade upgrade = this.loadUpgradeFromJson(jsonObject);
                  if (upgrade != null) {
                     itemUpgrades.put(itemId, upgrade);
                  }
               }
            }
         } catch (Exception var6) {
            var6.printStackTrace();
         }
      });
      System.out.println("ExoSuitUpgradeManager: Loaded " + itemUpgrades.size() + " upgrades");
   }

   @SubscribeEvent
   public static void onDatapackSync(OnDatapackSyncEvent event) {
      Map<ResourceLocation, CompoundTag> upgradeData = serializeUpgrades();
      S2CMessageSyncUpgradeRegistry message = new S2CMessageSyncUpgradeRegistry(upgradeData);
      if (event.getPlayer() != null) {
         PacketHandler.getPlayChannel().sendToPlayer(event::getPlayer, message);
      } else {
         for (ServerPlayer player : event.getPlayerList().getPlayers()) {
            PacketHandler.getPlayChannel().sendToPlayer(() -> player, message);
         }
      }
   }

   public static Map<ResourceLocation, CompoundTag> serializeUpgrades() {
      Map<ResourceLocation, CompoundTag> serialized = new HashMap<>();

      for (Entry<ResourceLocation, ExoSuitUpgrade> entry : itemUpgrades.entrySet()) {
         serialized.put(entry.getKey(), entry.getValue().serializeNBT());
      }

      return serialized;
   }

   public static void deserializeUpgrades(Map<ResourceLocation, CompoundTag> upgradeData) {
      itemUpgrades.clear();

      for (Entry<ResourceLocation, CompoundTag> entry : upgradeData.entrySet()) {
         ExoSuitUpgrade upgrade = new ExoSuitUpgrade();
         upgrade.deserializeNBT(entry.getValue());
         itemUpgrades.put(entry.getKey(), upgrade);
      }

      System.out.println("ExoSuitUpgradeManager: Synced " + itemUpgrades.size() + " upgrades from server");
   }

   private ResourceLocation getItemIdFromJson(JsonObject json, ResourceLocation fileLocation) {
      if (json.has("item")) {
         String itemStr = json.get("item").getAsString();
         return ResourceLocation.parse(itemStr);
      } else {
         String path = fileLocation.getPath();
         if (path.contains("/")) {
            String[] parts = path.split("/");
            if (parts.length >= 2) {
               String namespace = parts[parts.length - 2];
               String itemName = parts[parts.length - 1];
               return ResourceLocation.fromNamespaceAndPath(namespace, itemName);
            }
         }

         return ResourceLocation.fromNamespaceAndPath(fileLocation.getNamespace(), fileLocation.getPath());
      }
   }

   private ExoSuitUpgrade loadUpgradeFromJson(JsonObject json) {
      ExoSuitUpgrade upgrade = new ExoSuitUpgrade();

      try {
         if (json.has("type")) {
            upgrade.type = json.get("type").getAsString();
         }

         if (json.has("effects")) {
            JsonObject effects = json.getAsJsonObject("effects");
            ExoSuitUpgrade.Effects effectsData = upgrade.getEffects();
            if (effects.has("armorBonus")) {
               effectsData.armorBonus = effects.get("armorBonus").getAsFloat();
            }

            if (effects.has("armorToughness")) {
               effectsData.armorToughness = effects.get("armorToughness").getAsFloat();
            }

            if (effects.has("knockbackResistance")) {
               effectsData.knockbackResistance = effects.get("knockbackResistance").getAsFloat();
            }

            if (effects.has("speedModifier")) {
               effectsData.speedModifier = effects.get("speedModifier").getAsFloat();
            }

            if (effects.has("jumpBoost")) {
               effectsData.jumpBoost = effects.get("jumpBoost").getAsFloat();
            }

            if (effects.has("fallDamageReduction")) {
               effectsData.fallDamageReduction = effects.get("fallDamageReduction").getAsFloat();
            }

            if (effects.has("nightVision")) {
               effectsData.nightVision = effects.get("nightVision").getAsBoolean();
            }

            if (effects.has("flight")) {
               effectsData.flight = effects.get("flight").getAsBoolean();
            }

            if (effects.has("flightSpeed")) {
               effectsData.flightSpeed = effects.get("flightSpeed").getAsFloat();
            }

            if (effects.has("energyUse")) {
               effectsData.energyUse = effects.get("energyUse").getAsFloat();
            }

            if (effects.has("recoilAngleReduction")) {
               effectsData.recoilAngleReduction = effects.get("recoilAngleReduction").getAsFloat();
            }

            if (effects.has("recoilKickReduction")) {
               effectsData.recoilKickReduction = effects.get("recoilKickReduction").getAsFloat();
            }

            if (effects.has("spreadReduction")) {
               effectsData.spreadReduction = effects.get("spreadReduction").getAsFloat();
            }
         }

         if (json.has("display")) {
            JsonObject display = json.getAsJsonObject("display");
            ExoSuitUpgrade.Display displayData = upgrade.getDisplay();
            if (display.has("model")) {
               displayData.model = display.get("model").getAsString();
            }

            if (display.has("storageSize")) {
               displayData.storageSize = display.get("storageSize").getAsInt();
            }

            if (display.has("gridWidth")) {
               displayData.gridWidth = display.get("gridWidth").getAsInt();
            }

            if (display.has("gridHeight")) {
               displayData.gridHeight = display.get("gridHeight").getAsInt();
            }

            if (display.has("containerType")) {
               displayData.containerType = display.get("containerType").getAsString();
            }
         }

         return upgrade;
      } catch (Exception var5) {
         var5.printStackTrace();
         return null;
      }
   }

   public static ExoSuitUpgrade getUpgradeForItem(Item item) {
      ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
      return itemUpgrades.get(itemId);
   }

   public static ExoSuitUpgrade getUpgradeForItem(ItemStack stack) {
      return stack.isEmpty() ? null : getUpgradeForItem(stack.getItem());
   }

   public static ExoSuitUpgrade getUpgradeForItemInSlot(ItemStack stack, String slotType) {
      if (stack.isEmpty()) {
         return null;
      } else {
         ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
         String slotSpecificKey = itemId.toString() + "_" + slotType;
         ResourceLocation slotSpecificId = ResourceLocation.parse(slotSpecificKey);
         ExoSuitUpgrade slotSpecific = itemUpgrades.get(slotSpecificId);
         return slotSpecific != null ? slotSpecific : itemUpgrades.get(itemId);
      }
   }

   public static boolean isUpgradeItem(Item item) {
      ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
      return itemUpgrades.containsKey(itemId);
   }

   public static boolean isUpgradeItem(ItemStack stack) {
      return stack.isEmpty() ? false : isUpgradeItem(stack.getItem());
   }

   public static ExoSuitUpgradeManager getInstance() {
      return instance;
   }
}
