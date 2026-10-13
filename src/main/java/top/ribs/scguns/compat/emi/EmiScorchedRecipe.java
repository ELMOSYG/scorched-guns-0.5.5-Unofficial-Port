package top.ribs.scguns.compat.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * One Scorched Guns recipe drawn on the workstation's own GUI, using the slot positions the JEI category uses.
 */
public class EmiScorchedRecipe extends BasicEmiRecipe {
   /** One ingredient and the place the workstation's GUI expects it. */
   public record Slot(int x, int y, EmiIngredient ingredient) {
   }

   private final ScorchedEmiLayout layout;
   private final List<Slot> slots;
   private final Slot output;
   private final RecipeHolder<?> backing;

   public EmiScorchedRecipe(ScorchedEmiLayout layout, RecipeHolder<?> backing, List<Slot> slots, EmiStack result,
      int outputX, int outputY) {
      super(layout.category(), backing.id(), layout.width(), layout.height());
      this.layout = layout;
      this.backing = backing;
      this.slots = slots;
      this.output = new Slot(outputX, outputY, result);

      List<EmiIngredient> inputs = new ArrayList<>();
      for (Slot slot : slots) {
         inputs.add(slot.ingredient());
      }

      this.inputs = inputs;
      this.outputs = List.of(result);
   }

   @Override
   public void addWidgets(WidgetHolder widgets) {
      widgets.addTexture(this.layout.background(), 0, 0);

      for (Slot slot : this.slots) {
         widgets.addSlot(slot.ingredient(), slot.x(), slot.y());
      }

      widgets.addSlot(this.output.ingredient(), this.output.x(), this.output.y()).recipeContext(this);
   }

   @Override
   public RecipeHolder<?> getBackingRecipe() {
      return this.backing;
   }

   @Override
   public boolean supportsRecipeTree() {
      return true;
   }

   @Override
   public boolean hideCraftable() {
      return false;
   }
}
