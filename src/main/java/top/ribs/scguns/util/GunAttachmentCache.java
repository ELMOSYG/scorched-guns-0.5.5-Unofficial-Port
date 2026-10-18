package top.ribs.scguns.util;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.item.attachment.IAttachment;

/**
 * Cached attachment lookups for guns.
 *
 * <p>Attachment reads sit on the gun rendering path and were the most frequent mod entry in a render profile: every
 * frame asked for the same few attachments and re-parsed their NBT. The values only change when the gun is modified,
 * so they are cached for a tick-ish window, keyed by stack identity with that identity re-checked on every hit.</p>
 *
 * <p>This deliberately lives outside {@code Gun}. That class is deserialised by Gson, and its builder overrides the
 * default field exclusion with {@code TRANSIENT} alone, so static fields are no longer skipped. A static field
 * holding {@code ItemStack}s therefore dragged the whole data-component type graph into Gson's reflection, and one
 * type in it declares a {@code java.util.regex.Pattern}, which JDK 21 refuses to reflect into - every gun file then
 * failed to load. Keeping the cache in its own class keeps {@code Gun}'s shape exactly as it was.</p>
 */
public final class GunAttachmentCache {
   private static final Map<Integer, Entry> CACHE = new HashMap<>();

   private static final int CACHE_LIMIT = 512;

   private static final long CACHE_WINDOW_MS = 50L;

   private GunAttachmentCache() {
   }

   private static final class Entry {
      private ItemStack source;
      private long window;
      private final ItemStack[] byType = new ItemStack[] {
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY
      };
   }

   public static ItemStack get(IAttachment.Type type, ItemStack gun) {
      if (type == null || gun == null || gun.isEmpty()) {
         return ItemStack.EMPTY;
      }

      int key = System.identityHashCode(gun);
      long window = System.currentTimeMillis() / CACHE_WINDOW_MS;
      Entry cached = CACHE.get(key);
      if (cached == null || cached.source != gun || cached.window != window) {
         cached = new Entry();
         cached.source = gun;
         cached.window = window;
         IAttachment.Type[] types = IAttachment.Type.values();
         for (int i = 0; i < types.length && i < cached.byType.length; i++) {
            cached.byType[i] = read(types[i], gun);
         }

         if (CACHE.size() >= CACHE_LIMIT) {
            CACHE.clear();
         }

         CACHE.put(key, cached);
      }

      int index = type.ordinal();
      return index >= 0 && index < cached.byType.length ? cached.byType[index] : ItemStack.EMPTY;
   }

   private static ItemStack read(IAttachment.Type type, ItemStack gun) {
      CompoundTag compound = NbtHelper.getTag(gun);
      if (compound != null && compound.contains("Attachments", 10)) {
         CompoundTag attachment = compound.getCompound("Attachments");
         if (attachment.contains(type.getTagKey(), 10)) {
            return NbtHelper.itemFromTag(attachment.getCompound(type.getTagKey()));
         }
      }

      return ItemStack.EMPTY;
   }
}
