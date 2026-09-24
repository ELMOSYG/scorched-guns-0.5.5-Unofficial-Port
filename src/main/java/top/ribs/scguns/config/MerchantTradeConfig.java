package top.ribs.scguns.config;




import java.util.Optional;
import top.ribs.scguns.util.ScTrades;
import net.minecraft.core.registries.BuiltInRegistries;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@EventBusSubscriber(
   modid = "scguns"
)
public class MerchantTradeConfig {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final Map<String, List<MerchantTradeConfig.TradeData>> TRADE_SECTIONS = new HashMap<>();
   private static final Map<String, Integer> SECTION_LIMITS = new HashMap<>();
   private static int maxTotalOffers = 9;
   private static final ResourceLocation CONFIG_LOCATION = ResourceLocation.fromNamespaceAndPath("scguns", "entity/merchant_trades.json");

   public MerchantTradeConfig() {
      super();
   }

   public static void loadConfig(ResourceManager resourceManager) {
      TRADE_SECTIONS.clear();
      SECTION_LIMITS.clear();

      try {
         Resource resource = (Resource)resourceManager.getResource(CONFIG_LOCATION).orElse(null);
         if (resource != null) {
            try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
               Gson gson = new Gson();
               JsonObject json = (JsonObject)gson.fromJson(reader, JsonObject.class);
               if (json.has("max_total_offers")) {
                  maxTotalOffers = json.get("max_total_offers").getAsInt();
               }

               if (json.has("section_limits")) {
                  JsonObject limitsObj = json.getAsJsonObject("section_limits");

                  for (Entry<String, JsonElement> entry : limitsObj.entrySet()) {
                     SECTION_LIMITS.put(entry.getKey(), entry.getValue().getAsInt());
                  }
               }

               if (json.has("sections")) {
                  JsonObject sectionsObj = json.getAsJsonObject("sections");

                  for (Entry<String, JsonElement> sectionEntry : sectionsObj.entrySet()) {
                     String sectionName = sectionEntry.getKey();
                     JsonArray tradesArray = sectionEntry.getValue().getAsJsonArray();
                     List<MerchantTradeConfig.TradeData> sectionTrades = new ArrayList<>();

                     for (JsonElement element : tradesArray) {
                        JsonObject tradeObj = element.getAsJsonObject();
                        MerchantTradeConfig.TradeData trade = parseTradeData(tradeObj, sectionName);
                        if (trade != null) {
                           sectionTrades.add(trade);
                        }
                     }

                     TRADE_SECTIONS.put(sectionName, sectionTrades);
                  }
               }

               LOGGER.info("Loaded merchant trade config: {} sections with {} max offers", TRADE_SECTIONS.size(), maxTotalOffers);

               for (Entry<String, List<MerchantTradeConfig.TradeData>> entry : TRADE_SECTIONS.entrySet()) {
                  LOGGER.info("Section '{}': {} trades (limit: {})", entry.getKey(), entry.getValue().size(), SECTION_LIMITS.getOrDefault(entry.getKey(), 999));
               }
            }
         } else {
            LOGGER.warn("Merchant trades config not found at {}, using defaults", CONFIG_LOCATION);
            loadDefaultTrades();
         }
      } catch (Exception var17) {
         LOGGER.error("Failed to load merchant trades config at {}, using defaults", CONFIG_LOCATION, var17);
         loadDefaultTrades();
      }
   }

   private static MerchantTradeConfig.TradeData parseTradeData(JsonObject tradeObj, String section) {
      try {
         JsonObject buyItemObj = tradeObj.getAsJsonObject("buy_item");
         ItemStack buyItem = parseItemStack(buyItemObj);
         if (buyItem.isEmpty()) {
            LOGGER.warn("Invalid buy_item in trade config for section {}", section);
            return null;
         } else {
            ItemStack buyItem2 = ItemStack.EMPTY;
            if (tradeObj.has("buy_item_2")) {
               JsonObject buyItem2Obj = tradeObj.getAsJsonObject("buy_item_2");
               buyItem2 = parseItemStack(buyItem2Obj);
            }

            JsonObject sellItemObj = tradeObj.getAsJsonObject("sell_item");
            ItemStack sellItem = parseItemStack(sellItemObj);
            if (sellItem.isEmpty()) {
               LOGGER.warn("Invalid sell_item in trade config for section {}", section);
               return null;
            } else {
               int maxUses = tradeObj.has("max_uses") ? tradeObj.get("max_uses").getAsInt() : 2;
               int xpReward = tradeObj.has("xp_reward") ? tradeObj.get("xp_reward").getAsInt() : 5;
               float priceMultiplier = tradeObj.has("price_multiplier") ? tradeObj.get("price_multiplier").getAsFloat() : 0.05F;
               return new MerchantTradeConfig.TradeData(buyItem, buyItem2, sellItem, maxUses, xpReward, priceMultiplier, section);
            }
         }
      } catch (Exception var10) {
         LOGGER.error("Error parsing trade data for section {}", section, var10);
         return null;
      }
   }

   private static ItemStack parseItemStack(JsonObject itemObj) {
      try {
         String itemId = itemObj.get("item").getAsString();
         int count = itemObj.has("count") ? itemObj.get("count").getAsInt() : 1;
         ResourceLocation itemLocation = ResourceLocation.parse(itemId);
         Item item = (Item)BuiltInRegistries.ITEM.get(itemLocation);
         if (item != null && item != Items.AIR) {
            return new ItemStack(item, count);
         } else {
            LOGGER.warn("Unknown item: {}", itemId);
            return ItemStack.EMPTY;
         }
      } catch (Exception var5) {
         LOGGER.error("Error parsing item stack", var5);
         return ItemStack.EMPTY;
      }
   }

   private static void loadDefaultTrades() {
   }

   public static MerchantOffers createMerchantOffers() {
      return createRandomizedOffers(new Random());
   }

   public static MerchantOffers createRandomizedOffers(Random random) {
      MerchantOffers offers = new MerchantOffers();
      List<MerchantTradeConfig.TradeData> selectedTrades = new ArrayList<>();

      for (Entry<String, List<MerchantTradeConfig.TradeData>> sectionEntry : TRADE_SECTIONS.entrySet()) {
         String sectionName = sectionEntry.getKey();
         List<MerchantTradeConfig.TradeData> sectionTrades = sectionEntry.getValue();
         int sectionLimit = SECTION_LIMITS.getOrDefault(sectionName, Math.min(3, sectionTrades.size()));
         if (!sectionTrades.isEmpty()) {
            List<MerchantTradeConfig.TradeData> shuffledTrades = new ArrayList<>(sectionTrades);
            Collections.shuffle(shuffledTrades, random);
            int tradesToAdd = Math.min(sectionLimit, shuffledTrades.size());
            selectedTrades.addAll(shuffledTrades.subList(0, tradesToAdd));
         }
      }

      if (selectedTrades.size() > maxTotalOffers) {
         Collections.shuffle(selectedTrades, random);
         selectedTrades = selectedTrades.subList(0, maxTotalOffers);
      }

      for (MerchantTradeConfig.TradeData trade : selectedTrades) {
         offers.add(trade.toMerchantOffer());
      }

      return offers;
   }

   public static int getMaxTotalOffers() {
      return maxTotalOffers;
   }

   public static Map<String, Integer> getSectionLimits() {
      return new HashMap<>(SECTION_LIMITS);
   }

   @SubscribeEvent
   public static void onAddReloadListener(AddReloadListenerEvent event) {
      event.addListener(new SimplePreparableReloadListener<Void>() {
         protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
            return null;
         }

         protected void apply(Void object, ResourceManager resourceManager, ProfilerFiller profiler) {
            MerchantTradeConfig.loadConfig(resourceManager);
         }
      });
   }

   public static class TradeData {
      public final ItemStack buyItem;
      public final ItemStack buyItem2;
      public final ItemStack sellItem;
      public final int maxUses;
      public final int xpReward;
      public final float priceMultiplier;
      public final String section;

      public TradeData(ItemStack buyItem, ItemStack buyItem2, ItemStack sellItem, int maxUses, int xpReward, float priceMultiplier, String section) {
         super();
         this.buyItem = buyItem;
         this.buyItem2 = buyItem2;
         this.sellItem = sellItem;
         this.maxUses = maxUses;
         this.xpReward = xpReward;
         this.priceMultiplier = priceMultiplier;
         this.section = section;
      }

      public MerchantOffer toMerchantOffer() {
         return this.buyItem2.isEmpty()
            ? new MerchantOffer(ScTrades.cost(this.buyItem), this.sellItem, this.maxUses, this.xpReward, this.priceMultiplier)
            : new MerchantOffer(ScTrades.cost(this.buyItem), Optional.of(ScTrades.cost(this.buyItem2)), this.sellItem, this.maxUses, this.xpReward, this.priceMultiplier);
      }
   }
}
