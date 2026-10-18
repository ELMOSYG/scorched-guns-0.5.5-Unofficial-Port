package top.ribs.scguns.client.util;

import java.util.EnumMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.ribs.scguns.common.exosuit.ExoSuitData;
import top.ribs.scguns.common.exosuit.ExoSuitUpgrade;
import top.ribs.scguns.common.exosuit.ExoSuitUpgradeManager;

/**
 * Resolved exosuit upgrade slots, kept out of the render and tick hot paths.
 *
 * <p>The suit stores its four upgrade slots in the item's NBT, so every slot read allocates and deserialises, and
 * mapping an upgrade item back to its definition walks the registered upgrade table. The renderer used to do that
 * once per bone, which on a thirty-bone suit came to well over a hundred NBT reads every frame - the cause of the
 * reported "wearing the suit costs performance" lag.</p>
 *
 * <p>This cache resolves the four slots once per client tick per equipment slot instead, and everything that only
 * needs to know "is there a jetpack" or "which upgrade is in slot 2" reads the arrays. A tick is enough coverage:
 * swapping suits, and changing an upgrade inside a suit, are both picked up because the source stack is compared
 * every tick, and {@link #invalidate()} forces a reload when the server syncs new upgrades.</p>
 */
public final class ExoSuitStateCache {
   private static final EquipmentSlot[] ARMOR_SLOTS = {
         EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
   };

   private static final Map<EquipmentSlot, Slots> BY_SLOT = new EnumMap<>(EquipmentSlot.class);

   private ExoSuitStateCache() {
   }

   /** One resolved suit piece: the four upgrade stacks and their definitions. */
   public static final class Slots {
      private final ItemStack[] stacks = new ItemStack[] {
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY
      };

      private final ExoSuitUpgrade[] upgrades = new ExoSuitUpgrade[4];

      private ItemStack source = null;

      private void rebuild(ItemStack armorStack) {
         this.source = armorStack;
         for (int slot = 0; slot < 4; slot++) {
            ItemStack upgradeItem = armorStack == null || armorStack.isEmpty()
                  ? ItemStack.EMPTY
                  : ExoSuitData.getUpgradeInSlot(armorStack, slot);
            this.stacks[slot] = upgradeItem;
            this.upgrades[slot] = upgradeItem.isEmpty() ? null : ExoSuitUpgradeManager.getUpgradeForItem(upgradeItem);
         }
      }

      public ItemStack stack(int slot) {
         return this.stacks[slot];
      }

      public ExoSuitUpgrade upgrade(int slot) {
         return this.upgrades[slot];
      }

      public boolean hasUpgrade(int slot) {
         return this.upgrades[slot] != null;
      }

      public boolean hasType(String upgradeType) {
         for (int slot = 0; slot < 4; slot++) {
            ExoSuitUpgrade upgrade = this.upgrades[slot];
            if (upgrade != null && upgrade.getType().equals(upgradeType)) {
               return true;
            }
         }

         return false;
      }

      @Nullable
      public ItemStack firstOfType(String upgradeType) {
         for (int slot = 0; slot < 4; slot++) {
            ExoSuitUpgrade upgrade = this.upgrades[slot];
            if (upgrade != null && upgrade.getType().equals(upgradeType)) {
               return this.stacks[slot];
            }
         }

         return null;
      }

      @Nullable
      public ExoSuitUpgrade upgradeOfType(String upgradeType) {
         for (int slot = 0; slot < 4; slot++) {
            ExoSuitUpgrade upgrade = this.upgrades[slot];
            if (upgrade != null && upgrade.getType().equals(upgradeType)) {
               return upgrade;
            }
         }

         return null;
      }
   }

   /** Called once per client tick; re-resolves only the pieces whose stack changed. */
   public static void tick(Player player) {
      if (player == null) {
         return;
      }

      for (EquipmentSlot slot : ARMOR_SLOTS) {
         ItemStack armorStack = player.getItemBySlot(slot);
         Slots entry = BY_SLOT.computeIfAbsent(slot, ignored -> new Slots());
         if (entry.source != armorStack) {
            entry.rebuild(armorStack);
         }
      }
   }

   /**
    * Re-resolves every piece immediately, used when the server syncs new upgrades.
    *
    * <p>It has to rebuild right away rather than blanking the entries: a blanked entry used to be rebuilt from an
    * empty stack on the next read, which hid every upgrade model for the frames between the sync packet and the
    * following tick - the one-or-two-frame flicker.</p>
    */
   public static void invalidate() {
      net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
      if (minecraft != null && minecraft.player != null) {
         tick(minecraft.player);
         return;
      }

      for (Slots entry : BY_SLOT.values()) {
         entry.source = null;
      }
   }

   public static Slots get(EquipmentSlot slot) {
      Slots entry = BY_SLOT.computeIfAbsent(slot, ignored -> new Slots());
      if (entry.source == null) {
         net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
         if (minecraft != null && minecraft.player != null) {
            tick(minecraft.player);
         }
      }

      return entry;
   }
}
