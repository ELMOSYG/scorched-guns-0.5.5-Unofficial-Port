package top.ribs.scguns.cache;


import top.ribs.scguns.util.ScEnchants;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import top.ribs.scguns.init.ModEnchantments;

public class HotBarrelCache {
   private static final Map<String, HotBarrelCache.HotBarrelData> HOT_BARREL_CACHE = new ConcurrentHashMap<>();
   private static final float MAX_HOT_BARREL = 100.0F;
   private static final float DECAY_RATE_PER_TICK = 0.7F;
   private static final int DECAY_START_DELAY = 15;

   public HotBarrelCache() {
      super();
   }

   private static String generateKey(Player player, ItemStack stack) {
      return player.getUUID() + "_" + stack.getItem().getDescriptionId();
   }

   public static void increaseHotBarrel(Player player, ItemStack stack, int amount) {
      if (hasHotBarrelEnchantment(stack)) {
         String key = generateKey(player, stack);
         HotBarrelCache.HotBarrelData data = HOT_BARREL_CACHE.computeIfAbsent(key, k -> new HotBarrelCache.HotBarrelData());
         data.level = Math.min(100.0F, data.level + (float)amount);
         data.ticksSinceLastShot = 0;
         data.lastUpdateTime = System.currentTimeMillis();
         HOT_BARREL_CACHE.put(key, data);
      }
   }

   public static int getHotBarrelLevel(Player player, ItemStack stack) {
      if (!hasHotBarrelEnchantment(stack)) {
         return 0;
      } else {
         String key = generateKey(player, stack);
         HotBarrelCache.HotBarrelData data = HOT_BARREL_CACHE.get(key);
         return data == null ? 0 : Math.round(data.level);
      }
   }

   public static float getSmoothHotBarrelLevel(Player player, ItemStack stack) {
      if (!hasHotBarrelEnchantment(stack)) {
         return 0.0F;
      } else {
         String key = generateKey(player, stack);
         HotBarrelCache.HotBarrelData data = HOT_BARREL_CACHE.get(key);
         return data != null && !(data.level <= 0.0F) ? Math.max(data.level, 0.0F) : 0.0F;
      }
   }

   public static void setHotBarrelLevel(Player player, ItemStack stack, int level) {
      if (hasHotBarrelEnchantment(stack)) {
         String key = generateKey(player, stack);
         HotBarrelCache.HotBarrelData data = HOT_BARREL_CACHE.computeIfAbsent(key, k -> new HotBarrelCache.HotBarrelData());
         data.level = Math.max(0.0F, Math.min(100.0F, (float)level));
         data.lastUpdateTime = System.currentTimeMillis();
         HOT_BARREL_CACHE.put(key, data);
      }
   }

   public static float getSmoothHotBarrelPercentage(Player player, ItemStack stack) {
      return getSmoothHotBarrelLevel(player, stack) / 100.0F;
   }

   public static void tickHotBarrel(Player player, ItemStack stack) {
      if (hasHotBarrelEnchantment(stack)) {
         String key = generateKey(player, stack);
         HotBarrelCache.HotBarrelData data = HOT_BARREL_CACHE.get(key);
         if (data != null && !(data.level <= 0.0F)) {
            data.ticksSinceLastShot++;
            if (data.ticksSinceLastShot >= 15) {
               data.level = Math.max(data.level - 0.7F, 0.0F);
               if (data.level <= 0.0F) {
                  HOT_BARREL_CACHE.remove(key);
                  return;
               }
            }

            data.lastUpdateTime = System.currentTimeMillis();
         }
      }
   }

   public static void clearHotBarrel(Player player, ItemStack stack) {
      String key = generateKey(player, stack);
      HOT_BARREL_CACHE.remove(key);
   }

   public static boolean hasHotBarrelEnchantment(ItemStack stack) {
      return ScEnchants.level(stack, ModEnchantments.HOT_BARREL) > 0;
   }

   public static void cleanupOldEntries() {
      long currentTime = System.currentTimeMillis();
      long maxAge = 300000L;
      HOT_BARREL_CACHE.entrySet().removeIf(entry -> currentTime - entry.getValue().lastUpdateTime > maxAge);
   }

   public static class HotBarrelData {
      public float level = 0.0F;
      public int ticksSinceLastShot = 0;
      public long lastUpdateTime = System.currentTimeMillis();

      public HotBarrelData() {
         super();
      }
   }
}
