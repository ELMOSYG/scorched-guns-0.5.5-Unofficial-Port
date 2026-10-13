package top.ribs.scguns.compat.emi;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
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
   }

   @Override
   public Component getName() {
      return Component.translatable(this.translationKey);
   }
}
