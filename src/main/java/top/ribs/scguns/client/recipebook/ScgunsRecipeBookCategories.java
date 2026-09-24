package top.ribs.scguns.client.recipebook;

import net.minecraft.client.RecipeBookCategories;

/**
 * The gun bench's recipe book categories, resolved by name from the enum constants NeoForge adds via
 * {@code META-INF/enumextensions.json}.
 *
 * <p>The constants cannot be referenced at compile time (they do not exist then), and the constructor
 * arguments they need live in {@code ScgunsRecipeBookEnumParameters} - in a <b>common</b> package,
 * because the transformer resolves them while applying the extension (HANDOFF section 39.5).</p>
 *
 * <p>Client-only: {@code RecipeBookCategories} exists only on the client.</p>
 */
public final class ScgunsRecipeBookCategories {
   private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("scguns-recipebook");

   private static RecipeBookCategories search;
   private static RecipeBookCategories misc;
   private static RecipeBookCategories turret;
   private static RecipeBookCategories exoSuit;
   private static boolean attempted;

   private ScgunsRecipeBookCategories() {
   }

   /**
    * Resolve the extended constants, or leave them null when the extension is not there.
    *
    * <p>Never throws: an unapplied extension must cost the recipe book, not the game. That is exactly
    * what happened once - {@code valueOf} threw {@code IllegalArgumentException} inside
    * {@code RegisterRecipeBookCategoriesEvent}, which NeoForge treats as a mod loading error, so the
    * client died at startup (HANDOFF section 39.5).</p>
    */
   public static void init() {
      if (search == null && !attempted) {
         attempted = true;

         try {
            search = RecipeBookCategories.valueOf("SCGUNS_GUN_BENCH_SEARCH");
            misc = RecipeBookCategories.valueOf("SCGUNS_GUN_BENCH_MISC");
            turret = RecipeBookCategories.valueOf("SCGUNS_GUN_BENCH_TURRET");
            exoSuit = RecipeBookCategories.valueOf("SCGUNS_GUN_BENCH_EXO_SUIT");
         } catch (IllegalArgumentException e) {
            search = null;
            misc = null;
            turret = null;
            exoSuit = null;
            LOGGER.warn("The gun bench recipe book categories are missing - META-INF/enumextensions.json "
               + "was not applied. The bench itself still works; its recipe book stays empty.");
         }
      }
   }

   public static RecipeBookCategories search() {
      init();
      return search;
   }

   public static RecipeBookCategories misc() {
      init();
      return misc;
   }

   /** The turret tab, or null when the extension did not apply. */
   public static RecipeBookCategories turret() {
      init();
      return turret;
   }

   /** The exo suit tab, or null when the extension did not apply. */
   public static RecipeBookCategories exoSuit() {
      init();
      return exoSuit;
   }

   /**
    * Every resolved category, in tab order, skipping any the extension failed to add.
    *
    * <p>For diagnostics that must work even when the book is disabled: the caller renders an empty
    * list rather than dereferencing a null constant.</p>
    */
   public static java.util.List<RecipeBookCategories> all() {
      init();
      java.util.List<RecipeBookCategories> categories = new java.util.ArrayList<>(4);
      for (RecipeBookCategories category : new RecipeBookCategories[] {search, misc, turret, exoSuit}) {
         if (category != null) {
            categories.add(category);
         }
      }

      return categories;
   }
}
