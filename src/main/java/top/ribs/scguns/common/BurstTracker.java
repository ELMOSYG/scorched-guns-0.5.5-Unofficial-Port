package top.ribs.scguns.common;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import top.ribs.scguns.init.ModSyncedDataKeys;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.util.GunCompositeStatHelper;

@EventBusSubscriber(
   modid = "scguns"
)
public class BurstTracker {
   private static final Map<Player, BurstTracker> BURST_TRACKER_MAP = new WeakHashMap<>();
   private int burstTick;
   private final int slot;
   private final ItemStack stack;
   private final Gun gun;

   private BurstTracker(Player player) {
      super();
      this.burstTick = player.tickCount - 20;
      this.slot = player.getInventory().selected;
      this.stack = player.getInventory().getSelected();
      this.gun = ((GunItem)this.stack.getItem()).getModifiedGun(this.stack);
   }

   private boolean isSameWeapon(Player player) {
      return !this.stack.isEmpty() && player.getInventory().selected == this.slot && player.getInventory().getSelected() == this.stack;
   }

   private int getDeltaTicks(Player player) {
      return player.tickCount - this.burstTick;
   }

   private int getBurstDelayTicks(Player player) {
      int minTickDelay = GunCompositeStatHelper.getCompositeRate(this.stack, this.gun, player) + Gun.getBurstCooldown(this.stack);
      return minTickDelay - 1;
   }

   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent.Pre event) {
      if (!event.getEntity().level().isClientSide) {
         Player player = event.getEntity();
         if (!BURST_TRACKER_MAP.containsKey(player)) {
            if (!(player.getInventory().getSelected().getItem() instanceof GunItem)) {
               ModSyncedDataKeys.BURSTCOUNT.setValue(player, 0);
               ModSyncedDataKeys.ONBURSTCOOLDOWN.setValue(player, false);
               return;
            }

            BURST_TRACKER_MAP.put(player, new BurstTracker(player));
         }

         BurstTracker tracker = BURST_TRACKER_MAP.get(player);
         boolean resetBurst = false;
         if (player.getInventory().getSelected().getItem() instanceof GunItem) {
            GunItem gunItem = (GunItem)tracker.stack.getItem();
            if ((Boolean)ModSyncedDataKeys.SHOOTING.getValue(player) && Gun.hasBurstFire(tracker.stack)) {
               tracker.burstTick = player.tickCount;
            }

            if (tracker.isSameWeapon(player)) {
               if (!(Boolean)ModSyncedDataKeys.SHOOTING.getValue(player)) {
                  boolean onCooldown = tracker.getDeltaTicks(player) < tracker.getBurstDelayTicks(player);
                  if (!onCooldown) {
                     ModSyncedDataKeys.ONBURSTCOOLDOWN.setValue(player, false);
                  }
               } else if (Gun.hasBurstFire(tracker.stack)) {
                  ModSyncedDataKeys.ONBURSTCOOLDOWN.setValue(player, true);
               }
            } else {
               resetBurst = true;
            }
         } else {
            resetBurst = true;
         }

         if (resetBurst) {
            ModSyncedDataKeys.BURSTCOUNT.setValue(player, 0);
            if (BURST_TRACKER_MAP.containsKey(player)) {
               BURST_TRACKER_MAP.remove(player);
            }

            return;
         }
      }
   }

   @SubscribeEvent
   public static void onPlayerTick(PlayerLoggedOutEvent event) {
      MinecraftServer server = event.getEntity().getServer();
      if (server != null) {
         server.execute(() -> BURST_TRACKER_MAP.remove(event.getEntity()));
      }
   }
}
