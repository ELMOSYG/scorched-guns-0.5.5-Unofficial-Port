# JEI 19 migration notes (last block — do this only after the body compiles)

Scouted by reading the 8 excluded classes and `javap`-ing `libs/jei-1.21.1-neoforge-19.27.0.340.jar`.
Everything below is verified against the jar, not remembered.

## Scope

8 classes, all already in the tree and git-tracked; `build.gradle` excludes them from
`sourceSets.main.java` (lines 32–41):

| File | Lines |
|---|---|
| `compat/JEIScorchedPlugin.java` | 227 |
| `compat/GunBenchTransferInfo.java` | 63 |
| `compat/GunBenchCategory.java` | 71 |
| `compat/MaceratorCategory.java` | 81 |
| `compat/MechanicalPressCategory.java` | 83 |
| `compat/LightningBatteryCategory.java` | 97 |
| `compat/PoweredMaceratorCategory.java` | 109 |
| `compat/PoweredMechanicalPressCategory.java` | 111 |

## Headline: the JEI 19 API is *almost source-compatible* with what is already written

The 1.20.1-era code was written against JEI 15/18 idioms, but every symbol it uses still
exists in 19 with a compatible signature. This is not the big rewrite §5.6 of HANDOFF.md
feared. Verified signatures:

```
IRecipeCategory<T>
    RecipeType<T> getRecipeType()                 // same
    Component getTitle()                          // same
    IDrawable getBackground()                     // now a DEFAULT method - overriding is still legal
    int getWidth() / int getHeight()              // default (derived from getBackground())
    IDrawable getIcon()                           // same
    void setRecipe(IRecipeLayoutBuilder, T, IFocusGroup)   // same
    // new in 19 (all default, optional): createRecipeExtras, draw(T, IRecipeSlotsView, GuiGraphics, double, double),
    // getTooltip(ITooltipBuilder, ...), onDisplayedIngredientsUpdate, needsRecipeBorder

RecipeType<T>
    new RecipeType(ResourceLocation, Class<? extends T>)   // public ctor STILL EXISTS
    // also: create(String, String, Class), createFromVanilla(...), createRecipeHolderType(...)

IRecipeRegistration
    <T> void addRecipes(RecipeType<T>, List<T>)                        // List, not Collection
    <T> void addIngredientInfo(T, IIngredientType<T>, Component...)    // same order as before
    <T> void addIngredientInfo(List<T>, IIngredientType<T>, Component...)

IRecipeCategoryRegistration
    void addRecipeCategories(IRecipeCategory<?>...)     // passing an array still works

IRecipeCatalystRegistration
    default void addRecipeCatalyst(ItemStack, RecipeType<?>...)        // used by the plugin, unchanged

IGuiHandlerRegistration
    default <T extends AbstractContainerScreen<?>> void addRecipeClickArea(Class<? extends T>, int,int,int,int, RecipeType<?>...)

IRecipeTransferRegistration / IRecipeTransferInfo<C,R>
    <C,R> void addRecipeTransferHandler(IRecipeTransferInfo<C,R>)
    Class<? extends C> getContainerClass(); Optional<MenuType<C>> getMenuType(); RecipeType<R> getRecipeType();
    boolean canHandle(C,R); List<Slot> getRecipeSlots(C,R); List<Slot> getInventorySlots(C,R)

IGuiHelper
    IDrawableStatic createDrawable(ResourceLocation, int,int,int,int)
    <V> IDrawable createDrawableIngredient(IIngredientType<V>, V)

Packages that did NOT move (contrary to HANDOFF.md §5.6's warning):
    mezz.jei.api.recipe.RecipeIngredientRole
    mezz.jei.api.gui.drawable.IDrawable
    mezz.jei.api.gui.builder.IRecipeLayoutBuilder
```

## The actual delta to fix

1. **`RecipeManager#getAllRecipesFor` now returns `List<RecipeHolder<T>>`** (1.21).
   `JEIScorchedPlugin.registerRecipes` passes the result straight into `addRecipes(RecipeType<T>, List<T>)`,
   so each of the 6 lookups needs unwrapping, e.g.
   ```java
   List<GunBenchRecipe> gunBenchRecipes = recipeManager.getAllRecipesFor(GunBenchRecipe.Type.INSTANCE)
           .stream().map(RecipeHolder::value).toList();
   ```
   (The port already uses this `.map(RecipeHolder::value)` idiom elsewhere.)
2. **`getResultItem`** in the six `*Category` classes is called as `recipe.getResultItem(null)`.
   In 1.21 the parameter is `HolderLookup.Provider`; confirm the port's own recipe classes kept
   `getResultItem(HolderLookup.Provider)` and that `null` is accepted, otherwise pass
   `net.minecraft.core.RegistryAccess.EMPTY`-equivalent or the level's `registryAccess()`.
3. Confirm the port's recipe classes still expose `getIngredients()` (`NonNullList<Ingredient>`),
   `getBlueprint()` and `Type.INSTANCE` — the categories and the transfer info depend on them.
4. `GunBenchTransferInfo.getMenuType()` casts `ModMenuTypes.GUN_BENCH.get()`. That file
   (`client/screen/ModMenuTypes.java`) currently has its own `DeferredHolder` generic error, so
   fix that first; `MenuType<GunBenchMenu>` must actually come out of it.
5. `IRecipeCategory.getBackground()` being a default method means JEI 19 derives `getWidth()`/`getHeight()`
   from it, so the existing overrides keep working — no need to migrate to `createRecipeExtras`.

## Order of operations

1. Body compiles clean with the excludes still in place.
2. Delete the 8 `exclude` lines from `build.gradle`.
3. `python tools/round.py` → fix whatever the 8 classes surface (expect mostly item 1 and 2 above).
4. Re-verify `gradlew build` and check the plugin loads at runtime (`jei_plugin` uid
   `scguns:jei_plugin`), with the 6 recipe categories present and the GunBench transfer working.
