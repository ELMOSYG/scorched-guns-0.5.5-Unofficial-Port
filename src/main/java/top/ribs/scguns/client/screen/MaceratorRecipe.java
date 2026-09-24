package top.ribs.scguns.client.screen;





import top.ribs.scguns.common.recipe.ContainerRecipeInput;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import top.ribs.scguns.common.recipe.ScRecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class MaceratorRecipe implements Recipe<ContainerRecipeInput> {
   private final NonNullList<Ingredient> inputItems;
   private final ItemStack output;
   private final int processingTime;
   private final ResourceLocation id;

   public MaceratorRecipe(ResourceLocation id, NonNullList<Ingredient> inputItems, ItemStack output, int processingTime) {
      super();
      this.id = id;
      this.inputItems = inputItems;
      this.output = output;
      this.processingTime = processingTime;
   }

   public boolean matches(ContainerRecipeInput inv, Level world) {
      if (world.isClientSide()) {
         return false;
      } else {
         NonNullList<Ingredient> requiredIngredients = NonNullList.create();
         requiredIngredients.addAll(this.inputItems);

         for (int i = 0; i < inv.size(); i++) {
            ItemStack stackInSlot = inv.getItem(i);
            if (!stackInSlot.isEmpty()) {
               boolean matched = false;

               for (Ingredient ingredient : requiredIngredients) {
                  if (ingredient.test(stackInSlot)) {
                     requiredIngredients.remove(ingredient);
                     matched = true;
                     break;
                  }
               }

               if (!matched) {
                  return false;
               }
            }
         }

         return requiredIngredients.isEmpty();
      }
   }

   public ItemStack assemble(ContainerRecipeInput inv, HolderLookup.Provider registryAccess) {
      return this.output.copy();
   }

   public boolean canCraftInDimensions(int width, int height) {
      return true;
   }

   public ItemStack getResultItem(HolderLookup.Provider registryAccess) {
      return this.output.copy();
   }

   public int getProcessingTime() {
      return this.processingTime;
   }

   public ResourceLocation getId() {
      return this.id;
   }

   public RecipeSerializer<?> getSerializer() {
      return MaceratorRecipe.Serializer.INSTANCE;
   }

   public RecipeType<?> getType() {
      return MaceratorRecipe.Type.INSTANCE;
   }

   public NonNullList<Ingredient> getIngredients() {
      return this.inputItems;
   }

   public static class Serializer extends ScRecipeSerializer<MaceratorRecipe> {
      public static final MaceratorRecipe.Serializer INSTANCE = new MaceratorRecipe.Serializer();
      public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("scguns", "macerating");

      public Serializer() {
         super();
      }

      public MaceratorRecipe fromJson(@NotNull ResourceLocation recipeId, JsonObject json) {
         JsonArray ingredientsArray = json.getAsJsonArray("ingredients");
         NonNullList<Ingredient> ingredients = NonNullList.withSize(ingredientsArray.size(), Ingredient.EMPTY);

         for (int i = 0; i < ingredientsArray.size(); i++) {
            ingredients.set(i, Ingredient.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, ingredientsArray.get(i)).getOrThrow());
         }

         ItemStack output = ItemStack.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json.getAsJsonObject("result")).getOrThrow();
         int processingTime = json.get("processingTime").getAsInt();
         return new MaceratorRecipe(recipeId, ingredients, output, processingTime);
      }

      public MaceratorRecipe fromNetwork(@NotNull ResourceLocation recipeId, RegistryFriendlyByteBuf buffer) {
         int size = buffer.readInt();
         NonNullList<Ingredient> ingredients = NonNullList.withSize(size, Ingredient.EMPTY);

         for (int i = 0; i < size; i++) {
            ingredients.set(i, Ingredient.CONTENTS_STREAM_CODEC.decode(buffer));
         }

         ItemStack output = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
         int processingTime = buffer.readInt();
         return new MaceratorRecipe(recipeId, ingredients, output, processingTime);
      }

      public void toNetwork(RegistryFriendlyByteBuf buffer, MaceratorRecipe recipe) {
         buffer.writeInt(recipe.inputItems.size());

         for (Ingredient ingredient : recipe.inputItems) {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient);
         }

         ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, recipe.getResultItem(RegistryAccess.EMPTY));
         buffer.writeInt(recipe.getProcessingTime());
      }
   }

   public static class Type implements RecipeType<MaceratorRecipe> {
      public static final MaceratorRecipe.Type INSTANCE = new MaceratorRecipe.Type();
      public static final String ID = "macerating";

      public Type() {
         super();
      }
   }
}
