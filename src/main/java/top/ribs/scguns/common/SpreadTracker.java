package top.ribs.scguns.common;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableLong;
import org.apache.commons.lang3.tuple.Pair;
import top.ribs.scguns.Config;
import top.ribs.scguns.init.ModSyncedDataKeys;
import top.ribs.scguns.item.GunItem;

@EventBusSubscriber(
   modid = "scguns"
)
public class SpreadTracker {
   private static final Map<Player, SpreadTracker> TRACKER_MAP = new WeakHashMap<>();
   private final Map<GunItem, Pair<MutableLong, MutableInt>> SPREAD_TRACKER_MAP = new HashMap<>();

   public SpreadTracker() {
      super();
   }

   public void update(Player player, GunItem item) {
      Pair<MutableLong, MutableInt> entry = this.SPREAD_TRACKER_MAP.computeIfAbsent(item, gun -> Pair.of(new MutableLong(-1L), new MutableInt()));
      MutableLong lastFire = (MutableLong)entry.getLeft();
      if (lastFire.getValue() != -1L) {
         MutableInt spreadCount = (MutableInt)entry.getRight();
         long deltaTime = System.currentTimeMillis() - lastFire.getValue();
         if (deltaTime < (long)((Integer)Config.COMMON.projectileSpread.spreadThreshold.get()).intValue()) {
            if (spreadCount.getValue() < (Integer)Config.COMMON.projectileSpread.maxCount.get()) {
               spreadCount.increment();
               if (spreadCount.getValue() < (Integer)Config.COMMON.projectileSpread.maxCount.get() && !(Boolean)ModSyncedDataKeys.AIMING.getValue(player)) {
                  spreadCount.increment();
               }
            }
         } else {
            spreadCount.setValue(0);
         }
      }

      lastFire.setValue(System.currentTimeMillis());
   }

   public float getSpread(GunItem item) {
      Pair<MutableLong, MutableInt> entry = this.SPREAD_TRACKER_MAP.get(item);
      return entry != null
         ? (float)((MutableInt)entry.getRight()).getValue().intValue() / (float)((Integer)Config.COMMON.projectileSpread.maxCount.get()).intValue()
         : 0.0F;
   }

   public static SpreadTracker get(Player player) {
      return TRACKER_MAP.computeIfAbsent(player, player1 -> new SpreadTracker());
   }

   @SubscribeEvent
   public static void onPlayerDisconnect(PlayerLoggedOutEvent event) {
      MinecraftServer server = event.getEntity().getServer();
      if (server != null) {
         server.execute(() -> TRACKER_MAP.remove(event.getEntity()));
      }
   }

   public float getNextSpread(GunItem gun, float finalSpreadTranslate) {
      return 0.0F;
   }
}
