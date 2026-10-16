package top.ribs.scguns.client.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import top.ribs.scguns.util.NbtHelper;

/** Whether a gun is in the middle of a reload, as the client currently sees it. */
public final class GunReloadState {
   private GunReloadState() {
   }

   /**
    * True while a reload is in flight, including the animation that plays after it finishes.
    *
    * <p>The reload animations drive the magazine bone themselves, and the ammo counter kept driving it at the same
    * time - one bone, two systems. That is what made a counter magazine twitch as the count came back a round at a
    * time and snap to the empty position at the end. Screens that draw the counter use this to hand the bone to the
    * animation instead of fighting it.</p>
    */
   public static boolean isReloading(ItemStack stack) {
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
