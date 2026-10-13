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
      api.registerBlockEntity(GunBenchBlockEntity.class, blockEntity -> new GunBenchRecipeData((GunBenchBlockEntity) blockEntity));
      api.registerMenu(menu -> menu instanceof GunBenchMenu bench ? resolve(bench) : null);
   }

   /** The recipe the player selected, or the first match when there is nothing to select between. */
   @SuppressWarnings("unchecked")
   public static Optional<GunBenchRecipe> findRecipe(GunBenchMenu menu, ContainerRecipeInput input, Level level) {
      List<RecipeHolder<GunBenchRecipe>> candidates = level.getRecipeManager().getAllRecipesFor(GunBenchRecipe.Type.INSTANCE);
      IBlockEntityRecipeData data = PolymorphApi.getInstance().getBlockEntityRecipeData(menu);
      if (data == null) {
         return candidates.stream().filter(holder -> holder.value().matches(input, level)).findFirst()
            .map(holder -> holder.value());
      }

      RecipeHolder<GunBenchRecipe> selected = data.getRecipe(GunBenchRecipe.Type.INSTANCE, input, level, candidates);
      return Optional.ofNullable(selected).map(RecipeHolder::value);
   }

   private static BlockEntity resolve(GunBenchMenu menu) {
      return menu.getContainerAccess().evaluate((level, pos) -> level.getBlockEntity(pos)).orElse(null);
   }
}
