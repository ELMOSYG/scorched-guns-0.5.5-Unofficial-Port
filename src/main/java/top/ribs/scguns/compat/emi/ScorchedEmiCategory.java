package top.ribs.scguns.compat.emi;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import java.util.Comparator;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

/**
 * An EMI category titled like the matching JEI one.
 *
 * <p>EMI derives a category's name from its id, which showed players the raw key
 * "emi.category.scguns.gun_bench". The JEI categories already title themselves with the workstation's block
 * translation, so this reuses that key and both viewers read the same.</p>
 */
public class ScorchedEmiCategory extends EmiRecipeCategory {
   private final String translationKey;

   public ScorchedEmiCategory(ResourceLocation id, Block icon, String translationKey) {
      super(id, EmiStack.of(icon));
      this.translationKey = translationKey;
      // EMI's default ordering is the order recipes were handed to it, which is the recipe manager's own
      // (hash-ordered) iteration - players saw the gun bench pages as a shuffled pile. JEI shows them grouped by
      // recipe id, and the bench recipe ids are "<gun>_from_gun_bench", so the same key restores that grouping.
      this.sorter = Comparator.comparing(EmiRecipe::getId, Comparator.nullsLast(Comparator.naturalOrder()));
   }

   @Override
   public Component getName() {
      return Component.translatable(this.translationKey);
   }
}
