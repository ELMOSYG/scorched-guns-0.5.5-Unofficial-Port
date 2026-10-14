package top.ribs.scguns.compat;

import com.illusivesoulworks.polymorph.api.PolymorphApi;
import com.illusivesoulworks.polymorph.api.common.capability.IBlockEntityRecipeData;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import top.ribs.scguns.blockentity.GunBenchBlockEntity;
import top.ribs.scguns.common.recipe.ContainerRecipeInput;
import top.ribs.scguns.client.screen.GunBenchMenu;
import top.ribs.scguns.client.screen.GunBenchRecipe;

/**
 * The Polymorph side of the gun bench integration. Every Polymorph type is confined to this class and to
 * {@link GunBenchRecipeData}, so a game without Polymorph never loads them.
 */
public final class GunBenchPolymorph {
   private GunBenchPolymorph() {
   }

   /** Teaches Polymorph how to reach the bench behind a menu and how to read its contents. */
   public static void register() {
      PolymorphApi api = PolymorphApi.getInstance();
      // Polymorph hands every block entity that asks for recipe data to every registered factory, not only the ones
      // whose registered class matches. The cast therefore has to be guarded: a powered macerator looking up its own
      // recipe reached it and brought the server down with a ClassCastException (crash-2026-10-14_12.34.37-server).
      api.registerBlockEntity(
         GunBenchBlockEntity.class,
         blockEntity -> blockEntity instanceof GunBenchBlockEntity bench ? new GunBenchRecipeData(bench) : null
      );
      api.registerMenu(menu -> menu instanceof GunBenchMenu bench ? resolve(bench) : null);
   }

   /**
    * The recipe the player selected among those that currently match, or empty when none does.
    *
    * <p>Only matching recipes are offered as candidates. Polymorph's contract is to return the player's selection
    * and, when nothing has been selected yet, the first entry of the list it is handed - it does not re-check that
    * the entry still fits the input. Passing every bench recipe therefore made an empty bench resolve to whatever
    * recipe happened to sort first (players saw the output slot fill with a gun and could take it with no
    * ingredients at all). Filtering first keeps the choice meaningful and an empty bench empty.</p>
    */
   public static Optional<GunBenchRecipe> findRecipe(GunBenchMenu menu, ContainerRecipeInput input, Level level) {
      List<RecipeHolder<GunBenchRecipe>> candidates = level.getRecipeManager()
         .getAllRecipesFor(GunBenchRecipe.Type.INSTANCE)
         .stream()
         .filter(holder -> holder.value().matches(input, level))
         .toList();
      if (candidates.isEmpty()) {
         return Optional.empty();
      }

      IBlockEntityRecipeData data = PolymorphApi.getInstance().getBlockEntityRecipeData(menu);
      if (data == null) {
         return Optional.of(candidates.get(0).value());
      }

      RecipeHolder<GunBenchRecipe> selected = data.getRecipe(GunBenchRecipe.Type.INSTANCE, input, level, candidates);
      // Belt and braces: never hand back a recipe that does not fit what is in the bench.
      return selected == null || !selected.value().matches(input, level)
         ? Optional.of(candidates.get(0).value())
         : Optional.of(selected.value());
   }

   private static BlockEntity resolve(GunBenchMenu menu) {
      return menu.getContainerAccess().evaluate((level, pos) -> level.getBlockEntity(pos)).orElse(null);
   }
}
