package top.ribs.scguns.client.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.util.NbtHelper;

/**
 * Where the magazine bone of an ammo-counter gun (Scrapper, Birdfeeder, Repeating Musket, Triquetra) should sit.
 *
 * <p>These are strip and clip fed guns: a reload seats a <b>fresh</b> strip, so for the whole reload the bone
 * belongs at the full position rather than at whatever the outgoing strip still held. It is also the only reading
 * that behaves: 1.21.1 hands the client item data differently from the 1.20.1 this model was written for, so while
 * a reload was in flight the client could still see the previous round's count, which dragged the bone one way and
 * back on every round and snapped it to the empty position for a frame once the reload finished.</p>
 *
 * <p>Outside a reload the position follows {@code AmmoCount} as before, so chambering a round still walks the bone
 * along and an empty magazine still reads empty straight away.</p>
 */
public final class MagazineAmmoDisplay {
   private MagazineAmmoDisplay() {
   }

   public static float position(ItemStack stack) {
      if (isReloading(stack)) {
         return 0.0F;
      }

      int maxAmmo = Gun.getMaxAmmo(stack);
      int currentAmmo = Gun.getAmmoCount(stack);
      return maxAmmo <= 0 ? 0.0F : Math.min((float)(maxAmmo - currentAmmo) / (float)maxAmmo, 1.0F);
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
