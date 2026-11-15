package top.ribs.scguns.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * The only place that reads and writes a gun's {@code AmmoCount} custom-data key.
 *
 * <h2>Why this exists</h2>
 *
 * <p>Before this class the key was spelled out at roughly forty call sites, each one choosing its own
 * way to reach the tag: {@code getTag} (read-only), {@code getTagForWrite} (detaches, may be null),
 * {@code getOrCreateTag} (detaches, always present) or {@code NbtHelper.setInt}. Three consequences
 * followed, all recorded in {@code docs/reload-system.md} as the reload system's structural defects:</p>
 *
 * <ul>
 *   <li><b>Reads wrote to the stack.</b> {@code Gun.getAmmoCount} and
 *       {@code ReloadTracker.isWeaponFull} used {@code getOrCreateTag}, which copies the tag and
 *       re-installs the component on <i>every</i> call - including the per-frame render paths and the
 *       per-tick reload check. That is a wasted copy per call, and it silently orphaned whatever tag
 *       the caller had fetched earlier in the same block.</li>
 *   <li><b>Writes depended on statement order.</b> An in-place write is only visible while the tag is
 *       still the one inside the component; any helper called in between that fetches the tag again
 *       (a reload, a clamp, another ammo write) detaches it, and every write after that point was
 *       lost. This is how a reload could report the pre-reload magazine to the client
 *       ({@code C2SMessageGunLoaded}) and why clearing {@code IsReloading} after the reload wrote the
 *       magazine did nothing ({@code ReloadTracker.loaded}).</li>
 *   <li><b>No single writer.</b> {@code ReloadTracker}, the unload handler, the beam handler and the
 *       creative ammo box each wrote the key with their own clamp rules, so what "the magazine"
 *       means could differ between two code paths in the same tick.</li>
 * </ul>
 *
 * <h2>Rules for callers</h2>
 *
 * <ul>
 *   <li>{@link #get(ItemStack)} / {@link #get(CompoundTag)} do <b>not</b> create or re-install
 *       anything. Use them everywhere, including render and tick loops.</li>
 *   <li>{@link #set(ItemStack, int)} writes atomically (private copy, then write back) and is a
 *       no-op - returning false - when the stack carries no custom data at all, which is exactly what
 *       the 0.5.5 {@code tag != null} guards meant. Use {@link #hasCustomData(ItemStack)} when the
 *       guard needs to decide something else too (whether to consume ammo, for instance).</li>
 *   <li>{@link #setOrCreate(ItemStack, int)} is for the paths that hand out a gun and must give it a
 *       magazine (creative tab, mob equipment, addons).</li>
 *   <li>A {@link #set(ItemStack, int)} / {@link #setOrCreate(ItemStack, int)} call re-installs the
 *       component, so <b>any tag fetched before it is stale afterwards</b>. Do not keep using a local
 *       {@link CompoundTag} across one of these calls: fetch again
 *       ({@code NbtHelper.getTagForWrite}) or do the ammo write last.</li>
 * </ul>
 *
 * <p>Nothing outside this class may name the key - {@code tools/audit_ammo_writer.py} enforces that,
 * so a future writer cannot quietly reintroduce a fourth meaning of "the magazine".</p>
 */
public final class GunAmmo {
   /** The NBT key 0.5.5 used and downstream addons still read. Kept verbatim on purpose. */
   public static final String KEY = "AmmoCount";

   private GunAmmo() {
   }

   /**
    * The magazine, 0 when the stack has no custom data at all.
    *
    * <p>Read-only: it never creates the tag and never re-installs the component, so it is safe to
    * call from {@code appendHoverText} and from the render loop.</p>
    */
   public static int get(ItemStack stack) {
      return get(NbtHelper.getTag(stack));
   }

   /** {@link #get(ItemStack)} for a caller that already holds the tag. Null reads as 0. */
   public static int get(@Nullable CompoundTag tag) {
      return tag == null ? 0 : tag.getInt(KEY);
   }

   /**
    * Whether the stack actually carries an integer {@code AmmoCount}.
    *
    * <p>Distinct from {@link #get(ItemStack)} returning 0: a gun that has never been filled has no key
    * at all, and the unload / overflow paths only hand a magazine back out when the key was really
    * there. 0.5.5 spelled this {@code tag.contains("AmmoCount", 3)}.</p>
    */
   public static boolean has(ItemStack stack) {
      return has(NbtHelper.getTag(stack));
   }

   /** {@link #has(ItemStack)} for a caller that already holds the tag. */
   public static boolean has(@Nullable CompoundTag tag) {
      return tag != null && tag.contains(KEY, Tag.TAG_INT);
   }

   /**
    * Whether the stack carries custom data at all.
    *
    * <p>This is the guard every 0.5.5 write site had ({@code getTagForWrite(...) != null}). Keeping it
    * explicit matters: turning a guarded write into an unguarded one would, for instance, start filling
    * magazines that the old code left alone <i>and</i> start consuming the player's ammo for them.</p>
    */
   public static boolean hasCustomData(ItemStack stack) {
      return NbtHelper.getTag(stack) != null;
   }

   /**
    * Writes the magazine atomically, keeping the stack's other custom data.
    *
    * <p>The tag is copied, changed and written back in one go, so the result cannot depend on which
    * helper ran earlier in the same block: the write either lands on the stack as a whole or does not
    * happen at all.</p>
    *
    * @return false when the stack has no custom data (nothing was written), true otherwise
    */
   public static boolean set(ItemStack stack, int ammo) {
      if (!hasCustomData(stack)) {
         return false;
      }

      NbtHelper.setInt(stack, KEY, ammo);
      return true;
   }

   /**
    * Writes the magazine, creating the custom data when the stack has none.
    *
    * <p>For paths that hand out a gun and are responsible for it being usable: the creative tab, the
    * mob equipment config, the raid and spawner gun builders, and addons.</p>
    */
   public static void setOrCreate(ItemStack stack, int ammo) {
      NbtHelper.setInt(stack, KEY, ammo);
   }
}
