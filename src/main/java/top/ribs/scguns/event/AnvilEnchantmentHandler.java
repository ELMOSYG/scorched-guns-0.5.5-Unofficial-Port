package top.ribs.scguns.event;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import top.ribs.scguns.init.ModEnchantments;

/**
 * Keeps Trigger Finger off the guns that are deliberately excluded from its tag.
 *
 * <p>The restriction cannot live in {@code GunItem#supportsEnchantment}: JEI answers its anvil page through that
 * method, so narrowing it emptied the page for every gun and every book. It also cannot live in the enchantment's own
 * {@code supported_items} alone, because an anvil works from a book and asks the item, not the other item in the
 * slot.</p>
 *
 * <p>So the block happens here instead, on the anvil itself: if the right-hand slot holds a Trigger Finger book and
 * the left-hand gun is not in {@code #scguns:enchantable/trigger_finger}, the combination is refused. JEI's display
 * is untouched, and a player who tries it gets no output rather than a silent success.</p>
 */
public final class AnvilEnchantmentHandler {

   @SubscribeEvent
   public static void onAnvilUpdate(AnvilUpdateEvent event) {
      ItemStack gun = event.getLeft();
      ItemStack book = event.getRight();
      if (gun.isEmpty() || book.isEmpty() || !(gun.getItem() instanceof top.ribs.scguns.item.GunItem)) {
         return;
      }

      // An enchanted book keeps its enchantments in STORED_ENCHANTMENTS; a plain enchanted item uses ENCHANTMENTS.
      ItemEnchantments stored = book.getOrDefault(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS,
         ItemEnchantments.EMPTY);
      ItemEnchantments direct = book.getOrDefault(net.minecraft.core.component.DataComponents.ENCHANTMENTS,
         ItemEnchantments.EMPTY);

      java.util.Set<Holder<Enchantment>> holders = new java.util.HashSet<>(stored.keySet());
      holders.addAll(direct.keySet());

      for (Holder<Enchantment> holder : holders) {
         ResourceKey<Enchantment> key = holder.unwrapKey().orElse(null);
         if (key == null || !key.equals(ModEnchantments.TRIGGER_FINGER)) {
            continue;
         }

         if (!holder.value().canEnchant(gun)) {
            event.setOutput(ItemStack.EMPTY);
            event.setCost(0);
            return;
         }
      }
   }
}
