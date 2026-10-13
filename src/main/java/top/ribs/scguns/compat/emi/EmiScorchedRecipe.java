package top.ribs.scguns.compat.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import top.ribs.scguns.common.recipe.ContainerRecipeInput;

/**
 * One Scorched Guns recipe as an EMI page: the ingredients on the left, an arrow, the result on the right.
 *
 * <p>All six workstations expose the same shape - {@code getIngredients()} and
 * {@code getResultItem(RegistryAccess)}, the latter a plain copy of a stored stack, so the empty registry access is
 * enough here - which is why a single wrapper covers every category.</p>
 */
public class EmiScorchedRecipe extends BasicEmiRecipe {
   private static final int COLUMNS = 5;
   private static final int SLOT = 18;

   public EmiScorchedRecipe(EmiRecipeCategory category, ResourceLocation id, Recipe<ContainerRecipeInput> recipe) {
      super(category, id, 0, 0);

      List<EmiIngredient> ingredients = new ArrayList<>();
      for (Ingredient ingredient : recipe.getIngredients()) {
         if (!ingredient.isEmpty()) {
            ingredients.add(EmiIngredient.of(ingredient));
         }
      }

      this.inputs = ingredients;
      this.outputs = List.of(EmiStack.of(recipe.getResultItem(RegistryAccess.EMPTY)));

      int rows = Math.max(1, (ingredients.size() + COLUMNS - 1) / COLUMNS);
      this.width = Math.min(ingredients.size(), COLUMNS) * SLOT + SLOT * 2;
      this.height = rows * SLOT;
   }

   @Override
   public void addWidgets(WidgetHolder widgets) {
      for (int index = 0; index < this.inputs.size(); index++) {
         widgets.addSlot(this.inputs.get(index), index % COLUMNS * SLOT, index / COLUMNS * SLOT);
      }

      int arrowX = Math.min(this.inputs.size(), COLUMNS) * SLOT + 1;
      widgets.addTexture(EmiTexture.EMPTY_ARROW, arrowX, Math.max(0, this.height / 2 - 7));
      widgets.addSlot(this.outputs.get(0), arrowX + SLOT + 6, Math.max(0, this.height / 2 - 9)).recipeContext(this);
   }
}
