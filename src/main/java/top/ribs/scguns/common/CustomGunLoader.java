package top.ribs.scguns.common;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import java.io.InvalidObjectException;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import top.ribs.scguns.ScorchedGuns;
import top.ribs.scguns.annotation.Validator;

@EventBusSubscriber(
   modid = "scguns"
)
public class CustomGunLoader extends SimpleJsonResourceReloadListener {
   private static final Gson GSON_INSTANCE = (Gson)Util.make(() -> {
      GsonBuilder builder = new GsonBuilder();
      builder.registerTypeAdapter(ResourceLocation.class, JsonDeserializers.RESOURCE_LOCATION);
      builder.registerTypeAdapter(ItemStack.class, JsonDeserializers.ITEM_STACK);
      builder.registerTypeAdapter(FireMode.class, JsonDeserializers.FIRE_MODE);
      builder.registerTypeAdapter(ReloadType.class, JsonDeserializers.RELOAD_TYPE);
      builder.registerTypeAdapter(GripType.class, JsonDeserializers.GRIP_TYPE);
      return builder.create();
   });
   private static CustomGunLoader instance;
   private Map<ResourceLocation, CustomGun> customGunMap = new HashMap<>();

   public CustomGunLoader() {
      super(GSON_INSTANCE, "custom_guns");
   }

   protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager manager, ProfilerFiller profiler) {
      Builder<ResourceLocation, CustomGun> builder = ImmutableMap.builder();
      objects.forEach((resourceLocation, object) -> {
         try {
            CustomGun customGun = (CustomGun)GSON_INSTANCE.fromJson(object, CustomGun.class);
            if (customGun != null && Validator.isValidObject(customGun)) {
               builder.put(resourceLocation, customGun);
            } else {
               ScorchedGuns.LOGGER.error("Couldn't load data file {} as it is missing or malformed", resourceLocation);
            }
         } catch (InvalidObjectException var4x) {
            ScorchedGuns.LOGGER.error("Missing required properties for {}", resourceLocation);
            var4x.printStackTrace();
         } catch (IllegalAccessException var5) {
            var5.printStackTrace();
         }
      });
      this.customGunMap = builder.build();
   }

   public void writeCustomGuns(FriendlyByteBuf buffer) {
      buffer.writeVarInt(this.customGunMap.size());
      this.customGunMap.forEach((id, gun) -> {
         buffer.writeResourceLocation(id);
         buffer.writeNbt(gun.serializeNBT());
      });
   }

   public static ImmutableMap<ResourceLocation, CustomGun> readCustomGuns(FriendlyByteBuf buffer) {
      int size = buffer.readVarInt();
      if (size <= 0) {
         return ImmutableMap.of();
      } else {
         Builder<ResourceLocation, CustomGun> builder = ImmutableMap.builder();

         for (int i = 0; i < size; i++) {
            ResourceLocation id = buffer.readResourceLocation();
            CustomGun customGun = new CustomGun();
            customGun.deserializeNBT(buffer.readNbt());
            builder.put(id, customGun);
         }

         return builder.build();
      }
   }

   @SubscribeEvent
   public static void addReloadListenerEvent(AddReloadListenerEvent event) {
      CustomGunLoader customGunLoader = new CustomGunLoader();
      event.addListener(customGunLoader);
      instance = customGunLoader;
   }

   @Nullable
   public static CustomGunLoader get() {
      return instance;
   }
}
