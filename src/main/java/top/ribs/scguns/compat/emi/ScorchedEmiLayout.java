package top.ribs.scguns.compat.emi;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Where a workstation's own GUI picture sits in its texture sheet, and how big the cropped page is.
 *
 * <p>These are exactly the crops the JEI categories use ({@code createDrawable(TEXTURE, u, v, w, h)}), so an EMI
 * page and a JEI page for the same workstation show the same artwork, and the slot coordinates the recipes use are
 * relative to that crop in both viewers.</p>
 */
public record ScorchedEmiLayout(EmiRecipeCategory category, ResourceLocation texture, int u, int v, int width, int height) {
   public EmiTexture background() {
      return new EmiTexture(this.texture, this.u, this.v, this.width, this.height);
   }
}
