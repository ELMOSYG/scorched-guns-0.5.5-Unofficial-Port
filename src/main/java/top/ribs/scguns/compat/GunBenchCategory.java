package top.ribs.scguns.compat;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import top.ribs.scguns.client.screen.GunBenchRecipe;
import top.ribs.scguns.init.ModBlocks;

public class GunBenchCategory implements IRecipeCategory<GunBenchRecipe> {
   public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("scguns", "gun_bench");
   public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("scguns", "textures/gui/gun_bench_gui.png");
   public static final RecipeType<GunBenchRecipe> GUN_BENCH_TYPE = new RecipeType(UID, GunBenchRecipe.class);
   private final IDrawable background;
   private final IDrawable icon;
   private final int offsetX = 22;
   private final int offsetY = 12;
   private final int slotOffsetX = -22;
   private final int slotOffsetY = -12;

   public GunBenchCategory(IGuiHelper helper) {
      super();
      this.background = helper.createDrawable(TEXTURE, 22, 12, 141, 59);
      this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack((ItemLike)ModBlocks.GUN_BENCH.get()));
   }

   public RecipeType<GunBenchRecipe> getRecipeType() {
      return GUN_BENCH_TYPE;
   }

   public Component getTitle() {
      return Component.translatable("block.scguns.gun_bench");
   }

   public IDrawable getBackground() {
      return this.background;
   }

   public IDrawable getIcon() {
      return this.icon;
   }

   public void setRecipe(IRecipeLayoutBuilder builder, GunBenchRecipe recipe, IFocusGroup focuses) {
      int[] slotX = new int[]{26, 44, 62, 80, 26, 44, 62, 80, 26, 62, 116};
      int[] slotY = new int[]{17, 17, 17, 17, 35, 35, 35, 35, 53, 53, 17};
      NonNullList<Ingredient> ingredients = recipe.getModuleIngredients();

      for (int i = 0; i < ingredients.size(); i++) {
         if (!((Ingredient)ingredients.get(i)).isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT, slotX[i] + -22, slotY[i] + -12).addIngredients((Ingredient)ingredients.get(i));
         }
      }

      // slotX[10]/slotY[10] is the blueprint's own position in this layout. The blueprint is added
      // here explicitly, which is why the loop above walks the ten attachment slots only - the
      // recipe's combined input list would add it a second time on the same slot.
      Ingredient blueprint = recipe.getBlueprint();
      if (!blueprint.isEmpty()) {
         builder.addSlot(RecipeIngredientRole.INPUT, slotX[10] + -22, slotY[10] + -12).addIngredients(blueprint);
      }

      builder.addSlot(RecipeIngredientRole.OUTPUT, 118, 32).addItemStack(recipe.getResultItem(null));
   }
}
