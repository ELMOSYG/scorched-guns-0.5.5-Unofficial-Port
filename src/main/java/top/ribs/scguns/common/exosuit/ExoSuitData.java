package top.ribs.scguns.common.exosuit;


import top.ribs.scguns.util.NbtHelper;
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

   public static ItemStack getUpgradeInSlot(ItemStack exosuitPiece, int slot) {
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
