package top.ribs.scguns.client.handler;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.ItemStack;
import top.ribs.scguns.item.GunItem;

/**
 * Decides whether a gun's own animation file contains a melee/bayonet animation.
 *
 * <p>Before this, a gun only played a keyframed melee animation when its item data set
 * {@code uses_custom_melee_animation}; otherwise the renderer fell back to a hand-written pose. That made the pose the
 * rule and the animation the exception. It is now the other way round: if the gun's animation file defines a
 * {@code bayonet} (or {@code melee}) animation it is played, and the pose is only the fallback for guns that have no
 * such animation.</p>
 *
 * <p>The lookup reads the gun's animation JSON once and caches whether each name is present. It is client-only and
 * answerable from the resource manager, so nothing has to be declared in data.</p>
 */
public final class GunMeleeAnimationLookup {
   private static final Map<String, Boolean> BAYONET = new HashMap<>();
   private static final Map<String, Boolean> MELEE = new HashMap<>();

   private GunMeleeAnimationLookup() {
   }

   /** Whether this stack's gun has a {@code bayonet} animation of its own. */
   public static boolean hasBayonetAnimation(ItemStack stack) {
      return has(stack, "bayonet", BAYONET);
   }

   /** Whether this stack's gun has a {@code melee} (stock/body) animation of its own. */
   public static boolean hasMeleeAnimation(ItemStack stack) {
      return has(stack, "melee", MELEE);
   }

   private static boolean has(ItemStack stack, String name, Map<String, Boolean> cache) {
      if (!(stack.getItem() instanceof GunItem)) {
         return false;
      }

      // The animation file is named after the item, not after the gun data entry.
      ResourceLocation itemKey = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
      String gunId = itemKey.getPath();
      Boolean cached = cache.get(gunId);
      if (cached != null) {
         return cached;
      }

      boolean found = false;
      try {
         ResourceLocation location = ResourceLocation.fromNamespaceAndPath("scguns",
            "animations/item/" + gunId + ".animation.json");
         Resource resource = Minecraft.getInstance().getResourceManager().getResource(location).orElse(null);
         if (resource != null) {
            String json = new String(resource.open().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            found = json.contains("\"" + name + "\"");
         }
      } catch (Exception e) {
         found = false;
      }

      cache.put(gunId, found);
      return found;
   }
}
