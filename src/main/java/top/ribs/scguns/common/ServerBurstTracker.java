package top.ribs.scguns.common;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import top.ribs.scguns.item.GunItem;

@EventBusSubscriber(
   modid = "scguns"
)
public class ServerBurstTracker {
   private static final Map<UUID, ServerBurstTracker.BurstData> burstDataMap = new WeakHashMap<>();

   public ServerBurstTracker() {
      super();
   }

   public static int getBurstCount(ServerPlayer player) {
      ServerBurstTracker.BurstData data = burstDataMap.get(player.getUUID());
      return data != null && data.isSameWeapon(player) ? data.burstCount : 0;
   }

   public static void setBurstCount(ServerPlayer player, int count) {
      ItemStack heldItem = player.getMainHandItem();
      if (heldItem.getItem() instanceof GunItem) {
         burstDataMap.put(player.getUUID(), new ServerBurstTracker.BurstData(count, player.getInventory().selected, heldItem));
      }
   }

   public static void decrementBurstCount(ServerPlayer player) {
      ServerBurstTracker.BurstData data = burstDataMap.get(player.getUUID());
      if (data != null && data.isSameWeapon(player)) {
         data.burstCount = Math.max(0, data.burstCount - 1);
      }
   }

   public static void clearBurstData(ServerPlayer player) {
      burstDataMap.remove(player.getUUID());
   }

   @SubscribeEvent
   public static void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         clearBurstData(player);
      }
   }

   public static class BurstData {
      public int burstCount = 0;
      public int weaponSlot = -1;
      public ItemStack weaponStack = ItemStack.EMPTY;

      public BurstData(int burstCount, int slot, ItemStack stack) {
         super();
         this.burstCount = burstCount;
         this.weaponSlot = slot;
         this.weaponStack = stack;
      }

      public boolean isSameWeapon(ServerPlayer player) {
         return player.getInventory().selected == this.weaponSlot && player.getInventory().getSelected() == this.weaponStack;
      }
   }
}
