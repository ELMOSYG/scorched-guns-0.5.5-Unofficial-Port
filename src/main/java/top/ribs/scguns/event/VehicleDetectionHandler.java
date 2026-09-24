package top.ribs.scguns.event;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import top.ribs.scguns.ScorchedGuns;

@EventBusSubscriber(
   modid = "scguns",
   bus = Bus.GAME
)
public class VehicleDetectionHandler {
   private static final Map<UUID, VehicleDetectionHandler.VehicleInfo> playersInVehicles = new HashMap<>();

   public VehicleDetectionHandler() {
      super();
   }

   @SubscribeEvent
   public static void onEntityMount(EntityMountEvent event) {
      if (event.getEntity() instanceof Player player) {
         Entity var4 = event.getEntityBeingMounted();
         if (event.isMounting()) {
            if (isVehicle(var4)) {
               String vehicleType = getVehicleType(var4);
               playersInVehicles.put(player.getUUID(), new VehicleDetectionHandler.VehicleInfo(vehicleType, var4));
               ScorchedGuns.LOGGER
                  .debug("Player {} mounted {}: {} - Gun poses will be adjusted", player.getName().getString(), vehicleType, var4.getClass().getSimpleName());
            }
         } else if (playersInVehicles.remove(player.getUUID()) != null) {
            ScorchedGuns.LOGGER.debug("Player {} dismounted - Gun poses restored to normal", player.getName().getString());
         }
      }
   }

   public static boolean isPlayerInVehicle(Player player) {
      return playersInVehicles.containsKey(player.getUUID()) || player.isPassenger() && isVehicle(player.getVehicle());
   }

   public static VehicleDetectionHandler.VehicleInfo getPlayerVehicleInfo(Player player) {
      VehicleDetectionHandler.VehicleInfo cached = playersInVehicles.get(player.getUUID());
      if (cached != null) {
         return cached;
      } else if (player.isPassenger() && isVehicle(player.getVehicle())) {
         Entity vehicle = player.getVehicle();
         return new VehicleDetectionHandler.VehicleInfo(getVehicleType(vehicle), vehicle);
      } else {
         return null;
      }
   }

   public static String getVehicleTypeForPlayer(Player player) {
      VehicleDetectionHandler.VehicleInfo info = getPlayerVehicleInfo(player);
      return info != null ? info.vehicleType : null;
   }

   private static boolean isVehicle(Entity entity) {
      return entity == null
         ? false
         : entity instanceof Boat
            || entity instanceof AbstractMinecart
            || entity instanceof AbstractHorse
            || entity.getClass().getSimpleName().toLowerCase().contains("vehicle");
   }

   private static String getVehicleType(Entity vehicle) {
      if (vehicle == null) {
         return "unknown";
      } else if (vehicle instanceof Boat) {
         return "boat";
      } else if (vehicle instanceof AbstractMinecart) {
         return "minecart";
      } else {
         return vehicle instanceof AbstractHorse ? "horse" : vehicle.getClass().getSimpleName().toLowerCase();
      }
   }

   public static void cleanup() {
      playersInVehicles.clear();
   }

   public static class VehicleInfo {
      public final String vehicleType;
      public final Entity vehicle;

      public VehicleInfo(String vehicleType, Entity vehicle) {
         super();
         this.vehicleType = vehicleType;
         this.vehicle = vehicle;
      }
   }
}
