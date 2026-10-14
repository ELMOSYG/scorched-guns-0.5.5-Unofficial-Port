package top.ribs.scguns.client.util;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.util.NbtHelper;

/**
 * Where the magazine bone of an ammo-counter gun (Scrapper, Birdfeeder, Repeating Musket, Triquetra) should sit.
 *
 * <p>The value follows {@code AmmoCount} - full magazine, bone at rest; spent rounds, bone pushed along. That is
 * what the model did upstream too, but 1.20.1 handed the client item data differently from 1.21.1: while a reload
 * is in flight the client can still see the previous round's count for a tick, so the bone was dragged one way and
 * back on every round (a visible twitch) and snapped all the way to the empty position for a frame when the reload
 * finished, before the filled count arrived (a visible flash).</p>
 *
 * <p>Rather than replicate upstream's timing, hold the last settled value for the whole reload and refresh once it
 * is over and the count has caught up. Outside a reload nothing is held, so an empty magazine still reads empty
 * immediately. The cache is keyed by stack and weak, so it cannot grow or pin a stack in memory.</p>
 */
public final class MagazineAmmoDisplay {
   private static final Map<ItemStack, Float> HELD = new WeakHashMap<>();

   private MagazineAmmoDisplay() {
   }

   public static float position(ItemStack stack) {
      int maxAmmo = Gun.getMaxAmmo(stack);
      int currentAmmo = Gun.getAmmoCount(stack);
      float position = maxAmmo <= 0 ? 0.0F : Math.min((float)(maxAmmo - currentAmmo) / (float)maxAmmo, 1.0F);
      if (isReloading(stack)) {
         Float held = HELD.get(stack);
         if (held != null) {
            return held;
         }
      }

      HELD.put(stack, position);
      return position;
   }

   private static boolean isReloading(ItemStack stack) {
      CompoundTag tag = NbtHelper.getTag(stack);
      if (tag == null) {
         return false;
      }

      if (tag.getBoolean("scguns:IsReloading") || tag.getBoolean("IsReloading") || tag.getBoolean("scguns:IsPlayingReloadStop")) {
         return true;
      }

      String state = tag.getString("scguns:ReloadState");
      return !state.isEmpty() && !state.equals("NONE");
   }
}
