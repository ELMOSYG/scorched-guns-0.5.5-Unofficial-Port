package top.ribs.scguns.config;


import net.minecraft.core.registries.BuiltInRegistries;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
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
public class MobGuideConfig {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final Map<EntityType<?>, MobGuideConfig.MobGuide> GUIDES = new HashMap<>();
   private static final List<String> GUIDE_FILES = Arrays.asList("viventrum", "supply_scamp");

   public MobGuideConfig() {
   }

   public static void loadConfig(ResourceManager resourceManager) {
      GUIDES.clear();
      int loadedCount = 0;

      for (String guideName : GUIDE_FILES) {
         ResourceLocation location = ResourceLocation.fromNamespaceAndPath("scguns", "guides/" + guideName + ".json");

         try {
            Resource resource = (Resource)resourceManager.getResource(location).orElse(null);
            if (resource != null) {
               try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                  Gson gson = new Gson();
                  JsonObject json = (JsonObject)gson.fromJson(reader, JsonObject.class);
                  if (json != null) {
                     String id = json.get("id").getAsString();
                     String titleKey = json.get("title").getAsString();
                     List<MobGuideConfig.GuidePage> pages = new ArrayList<>();
                     if (json.has("pages")) {
                        for (JsonElement pageElement : json.getAsJsonArray("pages")) {
                           MobGuideConfig.GuidePage page = MobGuideConfig.GuidePage.fromJson(pageElement.getAsJsonObject());
                           pages.add(page);
                        }
                     }

                     MobGuideConfig.MobGuide guide = new MobGuideConfig.MobGuide(id, titleKey, pages);
                     EntityType<?> entityType = (EntityType<?>)BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(id));
                     if (entityType != null) {
                        GUIDES.put(entityType, guide);
                        loadedCount++;
                     }
                  }
               }
            }
         } catch (Exception var18) {
            LOGGER.error("Failed to load mob guide: {}", guideName, var18);
         }
      }
   }

   @Nullable
   public static MobGuideConfig.MobGuide getGuide(EntityType<?> entityType) {
      return GUIDES.get(entityType);
   }

   public static boolean hasGuide(EntityType<?> entityType) {
      return GUIDES.containsKey(entityType);
   }

   public static Collection<MobGuideConfig.MobGuide> getAllGuides() {
      return GUIDES.values();
   }

   @SubscribeEvent
   public static void onAddReloadListener(AddReloadListenerEvent event) {
      event.addListener(new SimplePreparableReloadListener<Void>() {
         protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
            return null;
         }

         protected void apply(Void object, ResourceManager resourceManager, ProfilerFiller profiler) {
            MobGuideConfig.loadConfig(resourceManager);
         }
      });
   }

   public static record GuidePage(String type, String text, @Nullable String image) {
      public GuidePage(String type, String text, @Nullable String image) {
         this.type = type;
         this.text = text;
         this.image = image;
      }

      public Component getTextComponent() {
         return Component.translatable(this.text);
      }

      public static MobGuideConfig.GuidePage fromJson(JsonObject json) {
         String type = json.has("type") ? json.get("type").getAsString() : "text";
         String text = json.get("text").getAsString();
         String image = json.has("image") ? json.get("image").getAsString() : null;
         return new MobGuideConfig.GuidePage(type, text, image);
      }
   }

   public static record MobGuide(String id, String titleKey, List<MobGuideConfig.GuidePage> pages) {
      public MobGuide(String id, String titleKey, List<MobGuideConfig.GuidePage> pages) {
         this.id = id;
         this.titleKey = titleKey;
         this.pages = pages;
      }

      public Component getTitle() {
         return Component.translatable(this.titleKey);
      }

      public int getPageCount() {
         return this.pages.size();
      }

      public MobGuideConfig.GuidePage getPage(int index) {
         return index >= 0 && index < this.pages.size() ? this.pages.get(index) : null;
      }

      public Iterable<? extends MobGuideConfig.GuidePage> getPages() {
         return this.pages;
      }
   }
}
