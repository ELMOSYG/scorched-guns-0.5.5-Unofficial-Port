package top.ribs.scguns.client.screen;




import top.ribs.scguns.common.recipe.ContainerRecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import com.google.gson.JsonObject;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import top.ribs.scguns.common.recipe.ScRecipeSerializer;
import top.ribs.scguns.init.ModBlocks;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class GunBenchRecipe implements Recipe<ContainerRecipeInput> {
   private final ResourceLocation id;
   private final ItemStack output;
   private final NonNullList<Ingredient> recipeItems;
   private final Ingredient blueprint;
   /** The ten attachment slots plus the blueprint, in bench-slot order (menu slots 1..11). */
   private final NonNullList<Ingredient> inputs;

   public GunBenchRecipe(ResourceLocation id, ItemStack output, NonNullList<Ingredient> recipeItems, Ingredient blueprint) {
      super();
      this.id = id;
      this.output = output;
      this.recipeItems = recipeItems;
      this.blueprint = blueprint;

      NonNullList<Ingredient> all = NonNullList.withSize(recipeItems.size() + 1, Ingredient.EMPTY);
      for (int i = 0; i < recipeItems.size(); i++) {
         all.set(i, recipeItems.get(i));
      }

      all.set(recipeItems.size(), blueprint);
      this.inputs = all;
   }

   public boolean matches(ContainerRecipeInput container, Level level) {
      ItemStack blueprintStack = container.getItem(11);
      if (!this.blueprint.test(blueprintStack)) {
         return false;
      } else {
         for (int i = 0; i < this.recipeItems.size(); i++) {
            ItemStack stackInSlot = container.getItem(i);
            Ingredient requiredIngredient = (Ingredient)this.recipeItems.get(i);
            if (!requiredIngredient.isEmpty() && !requiredIngredient.test(stackInSlot)) {
               return false;
            }

            if (requiredIngredient.isEmpty() && !stackInSlot.isEmpty()) {
               return false;
            }
         }

         return true;
      }
   }

   public ItemStack assemble(ContainerRecipeInput container, HolderLookup.Provider registryAccess) {
      return this.output.copy();
   }

   public boolean canCraftInDimensions(int width, int height) {
      return true;
   }

   public ItemStack getResultItem(@NotNull HolderLookup.Provider registryAccess) {
      return this.output.copy();
   }

   public ResourceLocation getId() {
      return this.id;
   }

   public RecipeSerializer<?> getSerializer() {
      return GunBenchRecipe.Serializer.INSTANCE;
   }

   public RecipeType<?> getType() {
      return GunBenchRecipe.Type.INSTANCE;
   }

   /**
    * Empty attachment slots are part of the layout, not missing data.
    *
    * <p>Vanilla's default treats <b>any</b> empty ingredient as an incomplete recipe, and the recipe
    * book drops incomplete recipes outright - {@code ClientRecipeBook.categorizeAndGroupRecipes} skips
    * {@code isSpecial()} and {@code isIncomplete()} recipes before it even looks at a category. Most
    * guns use only a few of the ten attachment slots, so <b>137 of the 146 recipes were filtered out
    * of the book</b> and only the 9 that fill every slot survived - exactly the nine the player saw
    * (HANDOFF section 42.7). {@code ShapedRecipe} overrides this the same way, for its empty pattern
    * cells; the filter here is identical, so an ingredient that resolves to no items at all still
    * counts as incomplete.</p>
    */
   @Override
   public boolean isIncomplete() {
      return this.recipeItems.isEmpty()
         || this.recipeItems.stream().filter(ingredient -> !ingredient.isEmpty())
            .anyMatch(Ingredient::hasNoItems);
   }

   /**
    * The recipe's inputs as the recipe book, the ghost and the auto-placer see them.
    *
    * <p>Vanilla drives all three from {@code getIngredients()} alone: {@code PlaceRecipe.placeRecipe}
    * walks a {@code gridWidth x gridHeight} grid, hands each ingredient the <b>menu slot</b> index it
    * belongs to (counting from 0 and stepping over the result slot), and the ghost is drawn on that
    * slot. With only the ten attachment slots listed, the blueprint - which sits in its own menu slot
    * outside that grid - could never be projected, so the player saw the gun's parts ghosted but an
    * empty blueprint slot (HANDOFF section 42.8). Listing it last, which is grid index 10 and therefore
    * menu slot 11, puts it exactly where it belongs.</p>
    */
   @Override
   public NonNullList<Ingredient> getIngredients() {
      return this.inputs;
   }

   /**
    * Only the ten attachment slots, in container order.
    *
    * <p>Callers that index the bench container directly must use this, not {@link #getIngredients()}:
    * the container's slot 10 is the <b>output</b> and its slot 11 the blueprint, so the full input list
    * does not line up with container indices.</p>
    */
   public NonNullList<Ingredient> getModuleIngredients() {
      return this.recipeItems;
   }

   public Ingredient getBlueprint() {
      return this.blueprint;
   }

   /**
    * The station icon the "new recipe unlocked" toast draws beside the result.
    *
    * <p>Vanilla's default is a <b>vanilla crafting table</b> ({@code Recipe#getToastSymbol}), so every
    * gun bench recipe announced itself with a workbench icon; the player reported exactly that - a
    * crafting table next to each unlocked gun recipe - and reasonably read the recipes as vanilla
    * crafting table recipes. The station that actually crafts these is the gun bench
    * (HANDOFF section 39.7).</p>
    */
   @Override
   public ItemStack getToastSymbol() {
      return new ItemStack(ModBlocks.GUN_BENCH.get());
   }

   private ItemStack getResultItem() {
      return this.output;
   }

   public static class Serializer extends ScRecipeSerializer<GunBenchRecipe> {
      public static final GunBenchRecipe.Serializer INSTANCE = new GunBenchRecipe.Serializer();
      public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("scguns", "gun_bench");

      public Serializer() {
         super();
      }

      public GunBenchRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
         ItemStack output = ItemStack.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, GsonHelper.getAsJsonObject(json, "result")).getOrThrow();
         JsonObject ingredients = GsonHelper.getAsJsonObject(json, "ingredients");
         NonNullList<Ingredient> inputs = NonNullList.withSize(10, Ingredient.EMPTY);
         if (ingredients.has("gun_top_internal_1")) {
            inputs.set(0, Ingredient.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, GsonHelper.getAsJsonObject(ingredients, "gun_top_internal_1")).getOrThrow());
         }

         if (ingredients.has("gun_top_internal_2")) {
            inputs.set(1, Ingredient.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, GsonHelper.getAsJsonObject(ingredients, "gun_top_internal_2")).getOrThrow());
         }

         if (ingredients.has("gun_top_barrel_1")) {
            inputs.set(2, Ingredient.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, GsonHelper.getAsJsonObject(ingredients, "gun_top_barrel_1")).getOrThrow());
         }

         if (ingredients.has("gun_top_barrel_2")) {
            inputs.set(3, Ingredient.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, GsonHelper.getAsJsonObject(ingredients, "gun_top_barrel_2")).getOrThrow());
         }

         if (ingredients.has("gun_internal_1")) {
            inputs.set(4, Ingredient.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, GsonHelper.getAsJsonObject(ingredients, "gun_internal_1")).getOrThrow());
         }

         if (ingredients.has("gun_internal_2")) {
            inputs.set(5, Ingredient.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, GsonHelper.getAsJsonObject(ingredients, "gun_internal_2")).getOrThrow());
         }

         if (ingredients.has("gun_barrel_1")) {
            inputs.set(6, Ingredient.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, GsonHelper.getAsJsonObject(ingredients, "gun_barrel_1")).getOrThrow());
         }

         if (ingredients.has("gun_barrel_2")) {
            inputs.set(7, Ingredient.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, GsonHelper.getAsJsonObject(ingredients, "gun_barrel_2")).getOrThrow());
         }

         if (ingredients.has("gun_grip")) {
            inputs.set(8, Ingredient.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, GsonHelper.getAsJsonObject(ingredients, "gun_grip")).getOrThrow());
         }

         if (ingredients.has("gun_magazine")) {
            inputs.set(9, Ingredient.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, GsonHelper.getAsJsonObject(ingredients, "gun_magazine")).getOrThrow());
         }

         Ingredient blueprint = Ingredient.EMPTY;
         if (ingredients.has("blueprint")) {
            blueprint = Ingredient.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, GsonHelper.getAsJsonObject(ingredients, "blueprint")).getOrThrow();
         }

         return new GunBenchRecipe(recipeId, output, inputs, blueprint);
      }

      public GunBenchRecipe fromNetwork(ResourceLocation recipeId, RegistryFriendlyByteBuf buffer) {
         NonNullList<Ingredient> inputs = NonNullList.withSize(10, Ingredient.EMPTY);

         for (int i = 0; i < inputs.size(); i++) {
            inputs.set(i, Ingredient.CONTENTS_STREAM_CODEC.decode(buffer));
         }

         Ingredient blueprint = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
         ItemStack output = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
         return new GunBenchRecipe(recipeId, output, inputs, blueprint);
      }

      public void toNetwork(@NotNull RegistryFriendlyByteBuf buffer, GunBenchRecipe recipe) {
         // The wire format is the ten attachment slots plus the blueprint, as it always was; the
         // combined getIngredients() list (11 entries) is for the recipe book, not for the network.
         for (Ingredient ing : recipe.getModuleIngredients()) {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ing);
         }

         Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.blueprint);
         ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, recipe.getResultItem(RegistryAccess.EMPTY));
      }
   }

   public static class Type implements RecipeType<GunBenchRecipe> {
      public static final GunBenchRecipe.Type INSTANCE = new GunBenchRecipe.Type();
      public static final String ID = "gun_bench";

      public Type() {
         super();
      }
   }
}
