package top.ribs.scguns.client;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/**
 * Per-player gun animation state the client cannot derive locally.
 *
 * <p>Vanilla only exposes the local player's {@code ItemCooldowns}; every other player's recoil is invisible, so the
 * models read the local player's cooldown for everyone - which is both wrong (someone else's gun animates with your
 * cooldown) and null-unsafe. The server now broadcasts the duration of each shot, and this map remembers it.</p>
 */
public final class GunRecoilSync {
   private static final Map<Integer, Long> RECOIL_UNTIL = new HashMap<>();
   private static final Map<Integer, Integer> RECOIL_START = new HashMap<>();

   private GunRecoilSync() {
   }

   /** Called from the network handler; {@code gameTime} is sampled here so no clock is needed. */
   public static void record(int entityId, int durationTicks) {
      RECOIL_UNTIL.put(entityId, net.minecraft.client.Minecraft.getInstance().level == null
         ? 0L
         : net.minecraft.client.Minecraft.getInstance().level.getGameTime() + durationTicks);
      RECOIL_START.put(entityId, durationTicks);
   }

   /**
    * How far through its recoil the given entity's gun is, 0..1 - the same scale {@code ItemCooldowns} uses.
    *
    * <p>The local player keeps using the real cooldown, because that is what its own reload and fire logic drives.
    * Everyone else uses the value carried by the last broadcast.</p>
    */
   public static float recoilProgress(Entity entity, ItemStack stack, float partialTick) {
      var mc = net.minecraft.client.Minecraft.getInstance();
      if (mc.level == null || entity == null) {
         return 0.0F;
      }

      if (entity == mc.player && mc.player != null) {
         float ticks = mc.player.getCooldowns().getCooldownPercent(stack.getItem(), partialTick);
         return Math.max(0.0F, Math.min(1.0F, ticks));
      }

      Long until = RECOIL_UNTIL.get(entity.getId());
      Integer duration = RECOIL_START.get(entity.getId());
      if (until == null || duration == null || duration <= 0) {
         return 0.0F;
      }

      long now = mc.level.getGameTime();
      if (now >= until) {
         RECOIL_UNTIL.remove(entity.getId());
         RECOIL_START.remove(entity.getId());
         return 0.0F;
      }

      return Math.max(0.0F, Math.min(1.0F, (float)(until - now) / (float)duration));
   }

   public static void clear() {
      RECOIL_UNTIL.clear();
      RECOIL_START.clear();
   }
}
