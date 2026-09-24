package top.ribs.scguns.common;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.Util;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.util.GunEnchantmentHelper;
import top.ribs.scguns.util.GunModifierHelper;

public class ShootTracker {
   private static final Map<Player, ShootTracker> SHOOT_TRACKER_MAP = new WeakHashMap<>();
   private final Map<Item, Pair<Long, Integer>> cooldownMap = Maps.newHashMap();

   public ShootTracker() {
      super();
   }

   public static ShootTracker getShootTracker(Player player) {
      return SHOOT_TRACKER_MAP.computeIfAbsent(player, player1 -> new ShootTracker());
   }

   public void putCooldown(ItemStack weapon, GunItem item, Gun modifiedGun) {
      int rate = GunEnchantmentHelper.getRate(weapon, modifiedGun);
      rate = GunModifierHelper.getModifiedRate(weapon, rate);
      this.cooldownMap.put(item, Pair.of(Util.getMillis(), rate * 50));
   }

   public boolean hasCooldown(GunItem item) {
      Pair<Long, Integer> pair = this.cooldownMap.get(item);
      return pair != null ? Util.getMillis() - (Long)pair.getLeft() < (long)((Integer)pair.getRight() - 50) : false;
   }

   public long getRemaining(GunItem item) {
      Pair<Long, Integer> pair = this.cooldownMap.get(item);
      return pair != null ? (long)((Integer)pair.getRight()).intValue() - (Util.getMillis() - (Long)pair.getLeft()) : 0L;
   }
}
