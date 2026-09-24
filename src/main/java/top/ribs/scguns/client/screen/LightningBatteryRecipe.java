package top.ribs.scguns.client.screen;





import top.ribs.scguns.common.recipe.ContainerRecipeInput;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import top.ribs.scguns.common.recipe.ScRecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.CraftingHelper;

public class LightningBatteryRecipe implements Recipe<ContainerRecipeInput> {
   private final ResourceLocation id;
   private final Ingredient input;
   private final ItemStack output;
   private final int processingTime;
   private final int energyUse;

   public LightningBatteryRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processingTime, int energyUse) {
      super();
      this.id = id;
      this.input = input;
      this.output = output;
      this.processingTime = processingTime;
      this.energyUse = energyUse;
   }

   public boolean matches(ContainerRecipeInput container, Level level) {
      return this.input.test(container.getItem(0));
   }

   public ItemStack assemble(ContainerRecipeInput container, HolderLookup.Provider registryAccess) {
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

   public Ingredient getInput() {
      return this.input;
   }

   public int getEnergyUse() {
      return this.energyUse;
   }

   public ResourceLocation getId() {
      return this.id;
   }

   public RecipeSerializer<?> getSerializer() {
      return LightningBatteryRecipe.Serializer.INSTANCE;
   }

   public RecipeType<?> getType() {
      return LightningBatteryRecipe.Type.INSTANCE;
   }

   public static class Serializer extends ScRecipeSerializer<LightningBatteryRecipe> {
      public static final LightningBatteryRecipe.Serializer INSTANCE = new LightningBatteryRecipe.Serializer();
      public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("scguns", "lightning_battery");

      public Serializer() {
         super();
      }

      public LightningBatteryRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
         Ingredient input = Ingredient.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json.get("ingredients").getAsJsonArray().get(0)).getOrThrow();
         ItemStack output = ItemStack.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json.getAsJsonObject("result")).getOrThrow();
         int processingTime = json.get("processingTime").getAsInt();
         int requiredEnergy = json.get("requiredEnergy").getAsInt();
         return new LightningBatteryRecipe(recipeId, input, output, processingTime, requiredEnergy);
      }

      public LightningBatteryRecipe fromNetwork(ResourceLocation recipeId, RegistryFriendlyByteBuf buffer) {
         Ingredient input = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
         ItemStack output = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
         int processingTime = buffer.readInt();
         int requiredEnergy = buffer.readInt();
         return new LightningBatteryRecipe(recipeId, input, output, processingTime, requiredEnergy);
      }

      public void toNetwork(RegistryFriendlyByteBuf buffer, LightningBatteryRecipe recipe) {
         Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.getInput());
         ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, recipe.getResultItem(RegistryAccess.EMPTY));
         buffer.writeInt(recipe.getProcessingTime());
         buffer.writeInt(recipe.getEnergyUse());
      }
   }

   public static class Type implements RecipeType<LightningBatteryRecipe> {
      public static final LightningBatteryRecipe.Type INSTANCE = new LightningBatteryRecipe.Type();
      public static final String ID = "lightning_battery";

      public Type() {
         super();
      }
   }
}
