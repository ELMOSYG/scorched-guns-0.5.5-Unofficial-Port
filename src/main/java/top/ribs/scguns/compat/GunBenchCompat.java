package top.ribs.scguns.compat;

import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import top.ribs.scguns.common.recipe.ContainerRecipeInput;
import top.ribs.scguns.client.screen.GunBenchMenu;
import top.ribs.scguns.client.screen.GunBenchRecipe;

/**
 * Recipe lookup for the gun bench, routed through Polymorph when it is installed.
 *
 * <p>Several gun addons can ship the same bench recipe shape, and without Polymorph the bench would silently pick
 * whichever one the recipe manager happens to return first, with no way for the player to choose. This class is
 * the only entry point the menu uses; it never names a Polymorph type so the class loads fine without the mod,
 * and the actual call is delegated to {@link GunBenchPolymorph}, which is only touched when the mod is present.</p>
 */
public final class GunBenchCompat {
   private GunBenchCompat() {
   }

   public static Optional<GunBenchRecipe> findRecipe(GunBenchMenu menu, ContainerRecipeInput input, Level level) {
      if (CompatManager.POLYMORPH_LOADED) {
         try {
            return GunBenchPolymorph.findRecipe(menu, input, level);
         } catch (Throwable throwable) {
            // A Polymorph that is present but unusable must never take the bench down with it.
            top.ribs.scguns.ScorchedGuns.LOGGER.warn("Falling back to a plain gun bench recipe lookup", throwable);
         }
      }

      List<RecipeHolder<GunBenchRecipe>> candidates = level.getRecipeManager()
         .getAllRecipesFor(GunBenchRecipe.Type.INSTANCE)
         .stream()
         .filter(holder -> holder.value().matches(input, level))
         .toList();
      return candidates.isEmpty() ? Optional.empty() : Optional.of(candidates.get(0).value());
   }
}
