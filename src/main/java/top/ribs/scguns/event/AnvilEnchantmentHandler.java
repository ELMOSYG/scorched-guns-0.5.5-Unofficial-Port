package top.ribs.scguns.event;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import top.ribs.scguns.item.GunItem;

/**
 * Refuses anvil combinations that would put an enchantment on a gun its own tag excludes.
 *
 * <p>The game's anvil works from a book, so the only gate it consults is {@link GunItem#supportsEnchantment}; this
 * handler is the belt to that braces, and it is also where the refusal is visible: the player gets no output instead
 * of a silent success. Only the mod's own enchantments are policed here - vanilla ones are already filtered by their
 * own tags.</p>
 */
public final class AnvilEnchantmentHandler {

   @SubscribeEvent
   public static void onAnvilUpdate(AnvilUpdateEvent event) {
      ItemStack gun = event.getLeft();
      ItemStack book = event.getRight();

      if (gun.isEmpty() || book.isEmpty() || !(gun.getItem() instanceof GunItem)) {
         return;
      }

      Set<Holder<Enchantment>> candidates = new HashSet<>(
         book.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).keySet());
      candidates.addAll(book.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).keySet());

      for (Holder<Enchantment> holder : candidates) {
         ResourceKey<Enchantment> key = holder.unwrapKey().orElse(null);
         if (key == null || !key.location().getNamespace().equals("scguns")) {
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
