package top.ribs.scguns.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * Counts fired rounds whose spent case has not been ejected yet.
 *
 * <h2>Why the gun has to remember this</h2>
 *
 * <p>Guns with {@code ejectDuringReload} do not drop a case when they fire: the fire path explicitly
 * skips the ejection ({@code ServerPlayHandler.handleCasingEjection} and its siblings return early)
 * because the case is supposed to come out during the reload animation, one per loading loop. The
 * animation cannot know how many rounds were actually fired, so the count has to live on the gun.</p>
 *
 * <p>Without it the reload-eject keyframe is free money, and the player found the exploit: unload the
 * magazine while it is being reloaded, and the rounds come back to the inventory while every loading
 * loop still ejects a case - so loading and unloading the same handful of shells farms casings for
 * ever. Counting fired rounds caps the payout at "one case per round actually fired", which cannot be
 * repeated without spending ammunition.</p>
 *
 * <p>The count is decremented per eject <b>attempt</b>, not per recovered case: the 40% roll decides
 * whether the player picks the case up, not whether it left the gun.</p>
 *
 * <p>The key is separate from {@code AmmoCount} on purpose - the magazine (how full) and the spent
 * cases (how many came out of it) are two different pieces of state, and {@link GunAmmo} stays the
 * single owner of the former.</p>
 */
public final class SpentRounds {
   /** Fired rounds waiting to have their case ejected. */
   public static final String KEY = "ScgunsSpentRounds";

   private SpentRounds() {
   }

   /** Read-only; 0 when nothing is pending. */
   public static int get(ItemStack stack) {
      CompoundTag tag = NbtHelper.getTag(stack);
      return tag == null ? 0 : tag.getInt(KEY);
   }

   /** A round was fired (and its case therefore has to come out at the next reload). */
   public static void add(ItemStack stack) {
      if (!GunAmmo.hasCustomData(stack)) {
         return;
      }

      NbtHelper.setInt(stack, KEY, get(stack) + 1);
   }

   /**
    * Claims one pending case, if there is one.
    *
    * @return true when a fired round was waiting, so the caller may eject a case
    */
   public static boolean take(ItemStack stack) {
      int pending = get(stack);
      if (pending <= 0) {
         return false;
      }

      NbtHelper.setInt(stack, KEY, pending - 1);
      return true;
   }
}
