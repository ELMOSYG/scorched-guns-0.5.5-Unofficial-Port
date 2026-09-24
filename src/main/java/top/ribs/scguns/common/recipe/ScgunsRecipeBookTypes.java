package top.ribs.scguns.common.recipe;

import net.minecraft.world.inventory.RecipeBookType;

/**
 * The mod's recipe book types.
 *
 * <p>1.21's {@code RecipeBookType} is a closed enum, but NeoForge marks it extensible and applies
 * the constants declared in {@code META-INF/enumextensions.json}. A mod cannot name such a constant
 * at compile time, so it is looked up by name - lazily, so that this class can never be initialized
 * before the enum extension has been applied.</p>
 *
 * <p>Common rather than client-side: {@code RecipeBookMenu#getRecipeBookType} runs on the server as
 * well, where it feeds {@code ServerPlaceRecipe} and the per-type recipe book state.</p>
 */
public final class ScgunsRecipeBookTypes {
   private static RecipeBookType gunBench;
   private static boolean attempted;
   private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("scguns-recipebook");

   private ScgunsRecipeBookTypes() {
   }

   /**
    * The gun bench's recipe book type, falling back to vanilla's crafting type when the enum extension
    * did not apply.
    *
    * <p>It must never throw: this runs every time a player opens the bench, and an exception would break
    * the block rather than just its recipe book (HANDOFF section 39.5).</p>
    */
   public static RecipeBookType gunBench() {
      if (gunBench == null && !attempted) {
         attempted = true;

         try {
            gunBench = RecipeBookType.valueOf("SCGUNS_GUN_BENCH");
         } catch (IllegalArgumentException e) {
            gunBench = RecipeBookType.CRAFTING;
            LOGGER.warn("The gun bench recipe book type is missing - META-INF/enumextensions.json did not "
               + "apply. Falling back to the crafting book type; the bench itself still works.");
         }
      }

      return gunBench;
   }
}
