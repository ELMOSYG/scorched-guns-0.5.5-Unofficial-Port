package top.ribs.scguns.common;


import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mrcrayfish.framework.api.network.FrameworkNetwork;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InvalidObjectException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.apache.commons.lang3.Validate;
import top.ribs.scguns.ScorchedGuns;
import top.ribs.scguns.annotation.Validator;
import top.ribs.scguns.client.util.Easings;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.network.PacketHandler;
import top.ribs.scguns.network.message.S2CMessageUpdateGuns;

@EventBusSubscriber(
   modid = "scguns"
)
public class NetworkGunManager extends SimplePreparableReloadListener<Map<GunItem, Gun>> {
   private static final int FILE_TYPE_LENGTH_VALUE = ".json".length();
   private static final Gson GSON_INSTANCE = (Gson)Util.make(() -> {
      GsonBuilder builder = new GsonBuilder();
      builder.registerTypeAdapter(ResourceLocation.class, JsonDeserializers.RESOURCE_LOCATION);
      builder.registerTypeAdapter(FireMode.class, JsonDeserializers.FIRE_MODE);
      builder.registerTypeAdapter(ReloadType.class, JsonDeserializers.RELOAD_TYPE);
      builder.registerTypeAdapter(GripType.class, JsonDeserializers.GRIP_TYPE);
      builder.registerTypeAdapter(Easings.class, JsonDeserializers.EASING);
      builder.excludeFieldsWithModifiers(new int[]{128});
      return builder.create();
   });
   private static final List<GunItem> clientRegisteredGuns = new ArrayList<>();
   private static NetworkGunManager instance;
   private Map<ResourceLocation, Gun> registeredGuns = new HashMap<>();

   public NetworkGunManager() {
      super();
   }

   protected Map<GunItem, Gun> prepare(ResourceManager manager, ProfilerFiller profiler) {
      Map<GunItem, Gun> map = new HashMap<>();
      BuiltInRegistries.ITEM
         .stream()
         .filter(item -> item instanceof GunItem)
         .forEach(
            item -> {
               ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
               if (id != null) {
                  List<ResourceLocation> resources = new ArrayList<>(
                     manager.listResources("guns", fileName -> fileName.getPath().endsWith(id.getPath() + ".json")).keySet()
                  );
                  resources.sort((r1, r2) -> {
                     if (r1.getNamespace().equals(r2.getNamespace())) {
                        return 0;
                     } else {
                        return r2.getNamespace().equals("scguns") ? 1 : -1;
                     }
                  });
                  resources.forEach(resourceLocation -> {
                     String path = resourceLocation.getPath().substring(0, resourceLocation.getPath().length() - FILE_TYPE_LENGTH_VALUE);
                     String[] splitPath = path.split("/");
                     if (id.getPath().equals(splitPath[splitPath.length - 1])) {
                        if (id.getNamespace().equals(resourceLocation.getNamespace())) {
                           manager.getResource(resourceLocation).ifPresent(resource -> {
                              try (Reader reader = new BufferedReader(new InputStreamReader(resource.open(), StandardCharsets.UTF_8))) {
                                 JsonObject jsonObject = GsonHelper.parse(reader);
                                 Gun gun = (Gun)GSON_INSTANCE.fromJson(jsonObject, Gun.class);
                                 if (gun != null && Validator.isValidObject(gun)) {
                                    gun.loadAlternateProjectilesFromJson(jsonObject);
                                    map.put((GunItem)item, gun);
                                 } else {
                                    ScorchedGuns.LOGGER
                                       .error("Couldn't load data file {} as it is missing or malformed. Using default gun data", resourceLocation);
                                    map.putIfAbsent((GunItem)item, new Gun());
                                 }
                              } catch (InvalidObjectException var9) {
                                 ScorchedGuns.LOGGER.error("Missing required properties for {}", resourceLocation);
                                 var9.printStackTrace();
                              } catch (IOException var10) {
                                 ScorchedGuns.LOGGER.error("Couldn't parse data file {}", resourceLocation);
                              } catch (IllegalAccessException var11) {
                                 var11.printStackTrace();
                              }
                           });
                        }
                     }
                  });
               }
            }
         );
      return map;
   }

   protected void apply(Map<GunItem, Gun> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
      Builder<ResourceLocation, Gun> builder = ImmutableMap.builder();
      objects.forEach((item, gun) -> {
         builder.put(Objects.requireNonNull(BuiltInRegistries.ITEM.getKey(item)), gun);
         item.setGun(new NetworkGunManager.Supplier(gun));
      });
      this.registeredGuns = builder.build();
   }

   public void writeRegisteredGuns(FriendlyByteBuf buffer) {
      buffer.writeVarInt(this.registeredGuns.size());
      this.registeredGuns.forEach((id, gun) -> {
         buffer.writeResourceLocation(id);
         buffer.writeNbt(gun.serializeNBT());
      });
   }

   public static ImmutableMap<ResourceLocation, Gun> readRegisteredGuns(FriendlyByteBuf buffer) {
      int size = buffer.readVarInt();
      if (size <= 0) {
         return ImmutableMap.of();
      } else {
         Builder<ResourceLocation, Gun> builder = ImmutableMap.builder();

         for (int i = 0; i < size; i++) {
            ResourceLocation id = buffer.readResourceLocation();
            Gun gun = Gun.create(buffer.readNbt());
            builder.put(id, gun);
         }

         return builder.build();
      }
   }

   public static boolean updateRegisteredGuns(S2CMessageUpdateGuns message) {
      return updateRegisteredGuns(message.getRegisteredGuns());
   }

   private static boolean updateRegisteredGuns(Map<ResourceLocation, Gun> registeredGuns) {
      clientRegisteredGuns.clear();
      if (registeredGuns != null) {
         for (Entry<ResourceLocation, Gun> entry : registeredGuns.entrySet()) {
            Item item = (Item)BuiltInRegistries.ITEM.get(entry.getKey());
            if (!(item instanceof GunItem)) {
               return false;
            }

            ((GunItem)item).setGun(new NetworkGunManager.Supplier(entry.getValue()));
            clientRegisteredGuns.add((GunItem)item);
         }

         return true;
      } else {
         return false;
      }
   }

   public Map<ResourceLocation, Gun> getRegisteredGuns() {
      return this.registeredGuns;
   }

   public static List<GunItem> getClientRegisteredGuns() {
      return ImmutableList.copyOf(clientRegisteredGuns);
   }

   @SubscribeEvent
   public static void onServerStopped(ServerStoppedEvent event) {
      instance = null;
   }

   @SubscribeEvent
   public static void addReloadListenerEvent(AddReloadListenerEvent event) {
      NetworkGunManager networkGunManager = new NetworkGunManager();
      event.addListener(networkGunManager);
      instance = networkGunManager;
   }

   @SubscribeEvent
   public static void onDatapackSync(OnDatapackSyncEvent event) {
      if (event.getPlayer() == null) {
         PacketHandler.getPlayChannel().sendToAll(new S2CMessageUpdateGuns());
      }
   }

   @Nullable
   public static NetworkGunManager get() {
      return instance;
   }

   /**
    * Framework 0.13 dropped {@code registerLoginData}/{@code ILoginData}, so the
    * gun registry that used to ride along with the login packet is pushed as a
    * normal play message when a player joins.
    */
   @SubscribeEvent
   public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         FrameworkNetwork channel = PacketHandler.getPlayChannel();
         if (channel != null) {
            channel.sendToPlayer(() -> player, new S2CMessageUpdateGuns());
         }
      }
   }

   public static class Supplier {
      private final Gun gun;

      private Supplier(Gun gun) {
         super();
         this.gun = gun;
      }

      public Gun getGun() {
         return this.gun;
      }
   }
}
