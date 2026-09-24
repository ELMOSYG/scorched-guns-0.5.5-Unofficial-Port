package top.ribs.scguns.compat;

import java.awt.Color;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import top.ribs.scguns.client.screen.MaceratorRecipe;
import top.ribs.scguns.init.ModBlocks;

public class MaceratorCategory implements IRecipeCategory<MaceratorRecipe> {
   public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("scguns", "macerating");
   public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("scguns", "textures/gui/macerator_gui.png");
   public static final RecipeType<MaceratorRecipe> MACERATING_TYPE = new RecipeType(UID, MaceratorRecipe.class);
   private final IDrawable background;
   private final IDrawable icon;
   private final int offsetX = 40;
   private final int offsetY = 26;

   public MaceratorCategory(IGuiHelper helper) {
      super();
      this.background = helper.createDrawable(TEXTURE, 40, 26, 93, 54);
      this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack((ItemLike)ModBlocks.MACERATOR.get()));
   }

   public RecipeType<MaceratorRecipe> getRecipeType() {
      return MACERATING_TYPE;
   }

   public Component getTitle() {
      return Component.translatable("block.scguns.macerator");
   }

   public IDrawable getBackground() {
      return this.background;
   }

   public IDrawable getIcon() {
      return this.icon;
   }

   public void setRecipe(IRecipeLayoutBuilder builder, MaceratorRecipe recipe, IFocusGroup focuses) {
      int inputBaseX = 4;
      int inputBaseY = 1;
      int inputSpacing = 18;

      for (int i = 0; i < 4; i++) {
         int row = i / 2;
         int col = i % 2;
         int x = inputBaseX + col * inputSpacing;
         int y = inputBaseY + row * inputSpacing;
         if (i < recipe.getIngredients().size()) {
            builder.addSlot(RecipeIngredientRole.INPUT, x, y).addIngredients((Ingredient)recipe.getIngredients().get(i));
         }
      }

      builder.addSlot(RecipeIngredientRole.OUTPUT, 74, 1).addItemStack(recipe.getResultItem(null));
   }

   public void draw(MaceratorRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
      IRecipeCategory.super.draw(recipe, recipeSlotsView, guiGraphics, mouseX, mouseY);
      int processingTime = recipe.getProcessingTime();
      if (processingTime > 0) {
         int seconds = processingTime / 20;
         String processingTimeText = String.format("%ds", seconds);
         guiGraphics.drawString(Minecraft.getInstance().font, processingTimeText, 70, 44, Color.gray.getRGB(), false);
      }
   }
}
