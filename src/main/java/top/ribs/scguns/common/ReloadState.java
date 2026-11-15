package top.ribs.scguns.common;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.ribs.scguns.init.ModSyncedDataKeys;
import top.ribs.scguns.util.NbtHelper;

/**
 * The single place that changes "is this player reloading".
 *
 * <p>The mod used to keep three parallel answers to that question - the synced flag, an {@code IsReloading} tag and a
 * {@code scguns:IsReloading} tag - written and read by nineteen different files. Nothing said which one was
 * authoritative, so a mismatch (a tag left behind by a cancelled reload, a synced flag cleared elsewhere) made the
 * loader run while the rest of the code believed it was idle. Every observable reload bug came out of that gap.</p>
 *
 * <p>Rules for new code:</p>
 * <ol>
 *   <li>Only this class writes the synced flag.</li>
 *   <li>The tags are a mirror for client animation. Never decide anything from them - call
 *       {@link #isReloading(Player)}.</li>
 *   <li>{@link #start}, {@link #cancel} and {@link #finish} are idempotent.</li>
 * </ol>
 */
public final class ReloadState {

   /** Mirror read by the animation code. Namespaced. */
   public static final String MIRROR_KEY = "scguns:IsReloading";
   /** Older, un-namespaced key. Still read for old saves, removed whenever it is seen. */
   public static final String LEGACY_KEY = "IsReloading";

   private ReloadState() {
   }

   @Nullable
   private static CompoundTag tagOf(ItemStack stack) {
      return stack.isEmpty() ? null : NbtHelper.getTagForWrite(stack);
   }

   /** Authoritative answer. Falls back to the tags so a save written by an older build still reports correctly. */
   public static boolean isReloading(Player player) {
      if (Boolean.TRUE.equals(ModSyncedDataKeys.RELOADING.getValue(player))) {
         return true;
      }

      CompoundTag tag = tagOf(player.getMainHandItem());
      return tag != null && (tag.getBoolean(MIRROR_KEY) || tag.getBoolean(LEGACY_KEY));
   }

   /** Marks the start of a reload. */
   public static void start(Player player, ItemStack stack) {
      ModSyncedDataKeys.RELOADING.setValue(player, true);
      CompoundTag tag = tagOf(stack);
      if (tag != null) {
         tag.putBoolean(MIRROR_KEY, true);
         tag.putBoolean(LEGACY_KEY, true);
      }
   }

   /**
    * Ends a reload without loading anything: used when the player unloads, switches away mid-reload, or a stale reload
    * is detected. Clears every piece of reload bookkeeping so nothing is left describing a load that will not happen.
    */
   public static void cancel(Player player, ItemStack stack) {
      ModSyncedDataKeys.RELOADING.setValue(player, false);
      CompoundTag tag = tagOf(stack);
      if (tag != null) {
         tag.remove(MIRROR_KEY);
         tag.remove(LEGACY_KEY);
         // Deliberately does not touch the loop bookkeeping (InReloadLoop, ReloadState, ShouldStopAfterLoop, ...).
         // Those belong to the running tracker; deleting them here meant the tracker could never reach its own end
         // condition, so a reload cancelled by unloading restarted and a full magazine never finished (Cowell).
      }
   }

   /** Ends a reload that completed. */
   public static void finish(Player player, ItemStack stack) {
      ModSyncedDataKeys.RELOADING.setValue(player, false);
      CompoundTag tag = tagOf(stack);
      if (tag != null) {
         tag.remove(MIRROR_KEY);
         tag.remove(LEGACY_KEY);
      }
   }
}
