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
import net.minecraft.world.item.crafting.RecipeHolder;
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

   private final RecipeHolder<?> backing;

   public EmiScorchedRecipe(EmiRecipeCategory category, RecipeHolder<? extends Recipe<ContainerRecipeInput>> backing) {
      super(category, backing.id(), displayWidth(backing.value()), displayHeight(backing.value()));
      this.backing = backing;

      Recipe<ContainerRecipeInput> recipe = backing.value();
      List<EmiIngredient> ingredients = new ArrayList<>();
      for (Ingredient ingredient : recipe.getIngredients()) {
         if (!ingredient.isEmpty()) {
            ingredients.add(EmiIngredient.of(ingredient));
         }
      }

      this.inputs = ingredients;
      this.outputs = List.of(EmiStack.of(recipe.getResultItem(RegistryAccess.EMPTY)));
   }

   /**
    * The recipe this page came from.
    *
    * <p>EMI uses this to tie a page back to the recipe holder, which is what lets a gun's entry in the item list
    * and in the favourites panel show the bench recipes that produce it. Without it the pages existed but nothing
    * linked them to the item.</p>
    */
   @Override
   public RecipeHolder<?> getBackingRecipe() {
      return this.backing;
   }

   /** Recipes are wanted in the tree: they are ordinary crafting pages, not a special case. */
   @Override
   public boolean supportsRecipeTree() {
      return true;
   }

   @Override
   public boolean hideCraftable() {
      return false;
   }

   // BasicEmiRecipe takes the canvas size in its constructor, so the layout has to be measured from the recipe
   // itself rather than assigned afterwards - computing it later left every page drawing inside an oversized canvas.
   private static int columns(Recipe<ContainerRecipeInput> recipe) {
      int count = 0;
      for (Ingredient ingredient : recipe.getIngredients()) {
         if (!ingredient.isEmpty()) {
            count++;
         }
      }

      return Math.max(1, Math.min(count, COLUMNS));
   }

   private static int rows(Recipe<ContainerRecipeInput> recipe) {
      int count = 0;
      for (Ingredient ingredient : recipe.getIngredients()) {
         if (!ingredient.isEmpty()) {
            count++;
         }
      }

      return Math.max(1, (count + COLUMNS - 1) / COLUMNS);
   }

   private static int displayWidth(Recipe<ContainerRecipeInput> recipe) {
      return columns(recipe) * SLOT + SLOT * 3;
   }

   private static int displayHeight(Recipe<ContainerRecipeInput> recipe) {
      return rows(recipe) * SLOT;
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
