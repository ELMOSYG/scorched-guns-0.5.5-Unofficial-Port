package top.ribs.scguns.common.exosuit;


import top.ribs.scguns.util.NbtHelper;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

public class ExoSuitData {
   private static final String UPGRADES_TAG = "ExoSuitUpgrades";
   private static final String HELMET_UPGRADES = "HelmetUpgrades";
   private static final String CHEST_UPGRADES = "ChestUpgrades";
   private static final String LEG_UPGRADES = "LegUpgrades";
   private static final String BOOT_UPGRADES = "BootUpgrades";

   public ExoSuitData() {
      super();
   }

   public static CompoundTag getUpgradeData(ItemStack exosuitPiece) {
      return exosuitPiece.isEmpty() ? new CompoundTag() : NbtHelper.getOrCreateTag(exosuitPiece).getCompound("ExoSuitUpgrades");
   }

   public static void setUpgradeData(ItemStack exosuitPiece, CompoundTag upgradeData) {
      if (!exosuitPiece.isEmpty()) {
         NbtHelper.getOrCreateTag(exosuitPiece).put("ExoSuitUpgrades", upgradeData);
      }
   }

   public static boolean hasUpgrades(ItemStack exosuitPiece) {
      CompoundTag upgrades = getUpgradeData(exosuitPiece);
      if (upgrades.contains("Upgrades")) {
         ListTag upgradeList = upgrades.getList("Upgrades", 10);
         return !upgradeList.isEmpty();
      } else {
         return false;
      }
   }

   // Every caller that walks the four slots used to re-parse this NBT: the status HUD alone did it six-odd times
   // every second frame, and ExoSuitPowerManager added more on top, which showed up as a fifth of the render frame.
   // The values only change when a suit or its upgrades change, so they are cached for a single tick-ish slice and
   // re-read after that. The cache is keyed on stack identity and checks that identity on every hit, so two stacks
   // that happen to hash alike can never be mistaken for each other.
   private static final Map<Integer, SlotCache> SLOT_CACHE = new HashMap<>();

   private static final int SLOT_CACHE_LIMIT = 512;

   private static final long SLOT_CACHE_WINDOW_MS = 50L;

   private static final class SlotCache {
      private ItemStack source;
      private long window;
      private final ItemStack[] slots = new ItemStack[] {
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY
      };
   }

   public static ItemStack getUpgradeInSlot(ItemStack exosuitPiece, int slot) {
      if (exosuitPiece == null || exosuitPiece.isEmpty() || slot < 0 || slot > 3) {
         return ItemStack.EMPTY;
      }

      int key = System.identityHashCode(exosuitPiece);
      long window = System.currentTimeMillis() / SLOT_CACHE_WINDOW_MS;
      SlotCache cached = SLOT_CACHE.get(key);
      if (cached == null || cached.source != exosuitPiece || cached.window != window) {
         cached = new SlotCache();
         cached.source = exosuitPiece;
         cached.window = window;
         for (int i = 0; i < 4; i++) {
            cached.slots[i] = readUpgradeInSlot(exosuitPiece, i);
         }

         if (SLOT_CACHE.size() >= SLOT_CACHE_LIMIT) {
            SLOT_CACHE.clear();
         }

         SLOT_CACHE.put(key, cached);
      }

      return cached.slots[slot];
   }

   private static ItemStack readUpgradeInSlot(ItemStack exosuitPiece, int slot) {
      CompoundTag upgradeData = getUpgradeData(exosuitPiece);
      if (upgradeData.contains("Upgrades")) {
         ListTag upgradeList = upgradeData.getList("Upgrades", 10);

         for (int i = 0; i < upgradeList.size(); i++) {
            CompoundTag slotTag = upgradeList.getCompound(i);
            if (slotTag.getInt("Slot") == slot && slotTag.contains("Item")) {
               return top.ribs.scguns.util.NbtHelper.itemFromTag(slotTag.getCompound("Item"));
            }
         }
      }

      return ItemStack.EMPTY;
   }
}
