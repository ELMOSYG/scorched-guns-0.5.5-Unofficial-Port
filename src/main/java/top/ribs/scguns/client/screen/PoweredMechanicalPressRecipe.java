package top.ribs.scguns.client.screen;





import top.ribs.scguns.common.recipe.ContainerRecipeInput;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Iterator;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import top.ribs.scguns.common.recipe.ScRecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class PoweredMechanicalPressRecipe implements Recipe<ContainerRecipeInput> {
   private final NonNullList<Ingredient> inputItems;
   private final Ingredient moldItem;
   private final ItemStack output;
   private final int processingTime;
   private final int energyUse;
   private final ResourceLocation id;

   public PoweredMechanicalPressRecipe(
      ResourceLocation id, NonNullList<Ingredient> inputItems, Ingredient moldItem, ItemStack output, int processingTime, int energyUse
   ) {
      super();
      this.id = id;
      this.inputItems = inputItems;
      this.moldItem = moldItem;
      this.output = output;
      this.processingTime = processingTime;
      this.energyUse = energyUse;
   }

   public boolean requiresMold() {
      return !this.moldItem.isEmpty();
   }

   public boolean matches(ContainerRecipeInput inv, Level world) {
      if (world.isClientSide()) {
         return false;
      } else {
         NonNullList<Ingredient> requiredIngredients = NonNullList.create();
         requiredIngredients.addAll(this.inputItems);
         boolean moldMatched = this.moldItem.isEmpty() || this.moldItem.test(inv.getItem(3));

         for (int i = 0; i < inv.size(); i++) {
            if (i != 3) {
               ItemStack stackInSlot = inv.getItem(i);
               if (!stackInSlot.isEmpty()) {
                  boolean matched = false;
                  Iterator<Ingredient> iterator = requiredIngredients.iterator();

                  while (iterator.hasNext()) {
                     Ingredient ingredient = iterator.next();
                     if (ingredient.test(stackInSlot)) {
                        iterator.remove();
                        matched = true;
                        break;
                     }
                  }

                  if (!matched) {
                  }
               }
            }
         }

         return requiredIngredients.isEmpty() && moldMatched;
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

   public int getEnergyUse() {
      return this.energyUse;
   }

   public ResourceLocation getId() {
      return this.id;
   }

   public RecipeSerializer<?> getSerializer() {
      return PoweredMechanicalPressRecipe.Serializer.INSTANCE;
   }

   public RecipeType<?> getType() {
      return PoweredMechanicalPressRecipe.Type.INSTANCE;
   }

   public NonNullList<Ingredient> getIngredients() {
      return this.inputItems;
   }

   public Ingredient getMoldItem() {
      return this.moldItem;
   }

   public static class Serializer extends ScRecipeSerializer<PoweredMechanicalPressRecipe> {
      public static final PoweredMechanicalPressRecipe.Serializer INSTANCE = new PoweredMechanicalPressRecipe.Serializer();
      public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("scguns", "powered_mechanical_pressing");

      public Serializer() {
         super();
      }

      public PoweredMechanicalPressRecipe fromJson(@NotNull ResourceLocation recipeId, JsonObject json) {
         JsonArray ingredientsArray = json.getAsJsonArray("ingredients");
         NonNullList<Ingredient> ingredients = NonNullList.withSize(ingredientsArray.size(), Ingredient.EMPTY);

         for (int i = 0; i < ingredientsArray.size(); i++) {
            ingredients.set(i, Ingredient.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, ingredientsArray.get(i)).getOrThrow());
         }

         Ingredient moldItem = Ingredient.EMPTY;
         if (json.has("mold") && !json.get("mold").isJsonNull()) {
            moldItem = Ingredient.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json.getAsJsonObject("mold")).getOrThrow();
         }

         ItemStack output = ItemStack.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json.getAsJsonObject("result")).getOrThrow();
         int processingTime = GsonHelper.getAsInt(json, "processingTime", 200);
         int energyUse = GsonHelper.getAsInt(json, "energyUse", 1000);
         return new PoweredMechanicalPressRecipe(recipeId, ingredients, moldItem, output, processingTime, energyUse);
      }

      public PoweredMechanicalPressRecipe fromNetwork(@NotNull ResourceLocation recipeId, RegistryFriendlyByteBuf buffer) {
         int size = buffer.readInt();
         NonNullList<Ingredient> ingredients = NonNullList.withSize(size, Ingredient.EMPTY);

         for (int i = 0; i < size; i++) {
            ingredients.set(i, Ingredient.CONTENTS_STREAM_CODEC.decode(buffer));
         }

         Ingredient moldItem = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
         ItemStack output = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
         int processingTime = buffer.readInt();
         int energyUse = buffer.readInt();
         return new PoweredMechanicalPressRecipe(recipeId, ingredients, moldItem, output, processingTime, energyUse);
      }

      public void toNetwork(RegistryFriendlyByteBuf buffer, PoweredMechanicalPressRecipe recipe) {
         buffer.writeInt(recipe.inputItems.size());

         for (Ingredient ingredient : recipe.inputItems) {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient);
         }

         Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.getMoldItem());
         ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, recipe.getResultItem(RegistryAccess.EMPTY));
         buffer.writeInt(recipe.getProcessingTime());
         buffer.writeInt(recipe.getEnergyUse());
      }
   }

   public static class Type implements RecipeType<PoweredMechanicalPressRecipe> {
      public static final PoweredMechanicalPressRecipe.Type INSTANCE = new PoweredMechanicalPressRecipe.Type();
      public static final String ID = "powered_mechanical_pressing";

      public Type() {
         super();
      }
   }
}
