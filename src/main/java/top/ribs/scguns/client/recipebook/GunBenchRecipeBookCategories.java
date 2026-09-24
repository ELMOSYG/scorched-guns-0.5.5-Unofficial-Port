package top.ribs.scguns.client.recipebook;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.client.RecipeBookCategories;
import net.neoforged.neoforge.client.event.RegisterRecipeBookCategoriesEvent;
import top.ribs.scguns.client.screen.GunBenchRecipe;
import top.ribs.scguns.common.recipe.GunBenchBookTabs;
import top.ribs.scguns.common.recipe.ScgunsRecipeBookTypes;

/**
 * Client half of the gun bench recipe book.
 *
 * <p>Three registrations are needed, and all three are load-bearing:</p>
 * <ul>
 *   <li>{@code registerBookCategories} - which tabs the gun bench's book shows at all. Without it the
 *       extended {@code RecipeBookType} has no categories and the book opens empty.</li>
 *   <li>{@code registerAggregateCategory} - vanilla's search tab aggregates the other tabs; ours has to
 *       do the same, exactly like {@code CRAFTING_SEARCH} does.</li>
 *   <li>{@code registerRecipeCategoryFinder} - maps recipes of our own {@code scguns:gun_bench} type to a
 *       tab. Vanilla's {@code ClientRecipeBook} only knows crafting and cooking recipes, so without this
 *       every gun bench recipe would be grouped as uncategorised and never shown.</li>
 * </ul>
 *
 * <p>Everything degrades instead of throwing: this event fires while the client starts, and an exception
 * there is a fatal mod-loading error - which is exactly how a missing enum extension crashed the game
 * once (HANDOFF section 39.5). The constants themselves come from
 * {@link ScgunsRecipeBookCategories}.</p>
 */
public final class GunBenchRecipeBookCategories {
   private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("scguns-recipebook");

   private GunBenchRecipeBookCategories() {
   }

   /**
    * Whether both extended categories resolved.
    *
    * <p>The screen asks first and shows no book at all when they did not: falling back to the crafting
    * type would hand the bench vanilla's crafting tab, which lists (and can place) recipes that have
    * nothing to do with it - the player saw exactly that once (HANDOFF 39.5).</p>
    */
   public static boolean isEnabled() {
      return ScgunsRecipeBookCategories.search() != null && ScgunsRecipeBookCategories.misc() != null;
   }

   /**
    * Whether this category is the search tab - the union of the others.
    *
    * <p>Diagnostics have to skip it: {@code ClientRecipeBook} fills an aggregate with the collections
    * of its members, so counting it reports every recipe and then zero for each real tab.</p>
    */
   public static boolean isAggregate(RecipeBookCategories category) {
      return category != null && category == ScgunsRecipeBookCategories.search();
   }

   /**
    * The shared {@code scguns-recipebook} logger.
    *
    * <p>Exposed so the bench screen logs what the book is about to draw under the same tag, which is
    * what makes a "the book shows too few recipes" report checkable from {@code latest.log}.</p>
    */
   public static org.slf4j.Logger logger() {
      return LOGGER;
   }

   public static void onRegisterRecipeBookCategories(RegisterRecipeBookCategoriesEvent event) {
      RecipeBookCategories search = ScgunsRecipeBookCategories.search();
      RecipeBookCategories misc = ScgunsRecipeBookCategories.misc();
      RecipeBookCategories turret = ScgunsRecipeBookCategories.turret();
      RecipeBookCategories exoSuit = ScgunsRecipeBookCategories.exoSuit();

      // One INFO line per client start, on purpose: it is the only way to see whether
      // META-INF/enumextensions.json was applied in a real jar-based game. A dev run cannot answer
      // that - there the mod is a directory and NeoForge never reads the file (HANDOFF 39.5).
      LOGGER.info("SCGUNS-RECIPEBOOK type={} tabs={} book={}", ScgunsRecipeBookTypes.gunBench(),
         describe(search, misc, turret, exoSuit),
         search == null || misc == null
            ? "disabled (enumextensions.json not applied; bench works, book stays empty)"
            : "enabled");

      if (search == null || misc == null) {
         LOGGER.warn("Skipping gun bench recipe book registration: its categories are unavailable.");
         return;
      }

      // Tab order is the search tab first, then the groups, as vanilla's crafting book does. The two
      // group tabs are optional on purpose: if only they failed to resolve, the book still opens with
      // the recipes in the general tab rather than losing the whole book.
      List<RecipeBookCategories> tabs = Lists.newArrayList(search, misc);
      List<RecipeBookCategories> aggregated = Lists.newArrayList(misc);
      if (turret != null) {
         tabs.add(turret);
         aggregated.add(turret);
      }

      if (exoSuit != null) {
         tabs.add(exoSuit);
         aggregated.add(exoSuit);
      }

      event.registerBookCategories(ScgunsRecipeBookTypes.gunBench(), tabs);
      event.registerAggregateCategory(search, aggregated);
      // The finder is registered for our own recipe type, so every holder reaching it is a gun bench
      // recipe; the instanceof keeps that assumption explicit instead of an unchecked cast.
      event.registerRecipeCategoryFinder(GunBenchRecipe.Type.INSTANCE,
         holder -> holder.value() instanceof GunBenchRecipe recipe
            ? categoryFor(recipe, misc, turret, exoSuit)
            : misc);
   }

   /**
    * The tab a recipe is shown in, or {@code misc} when its own tab is unavailable.
    *
    * <p>The split itself lives in {@link GunBenchBookTabs}, in a common package, because
    * {@code /scguns recipebook} reports the same grouping server-side.</p>
    */
   private static RecipeBookCategories categoryFor(GunBenchRecipe recipe, RecipeBookCategories misc,
                                                   RecipeBookCategories turret,
                                                   RecipeBookCategories exoSuit) {
      return switch (GunBenchBookTabs.of(recipe)) {
         case TURRETS -> turret == null ? misc : turret;
         case EXO_SUIT -> exoSuit == null ? misc : exoSuit;
         case GUNS -> misc;
      };
   }

   private static String describe(RecipeBookCategories... categories) {
      StringBuilder builder = new StringBuilder("[");
      String[] names = {"search", "misc", "turret", "exo_suit"};
      for (int i = 0; i < categories.length; i++) {
         if (i > 0) {
            builder.append(' ');
         }

         builder.append(names[i]).append('=').append(categories[i] == null ? "missing" : "ok");
      }

      return builder.append(']').toString();
   }
}
