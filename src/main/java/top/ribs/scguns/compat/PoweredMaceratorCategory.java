package top.ribs.scguns.compat;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
import top.ribs.scguns.client.screen.PoweredMaceratorRecipe;
import top.ribs.scguns.init.ModBlocks;

public class PoweredMaceratorCategory implements IRecipeCategory<PoweredMaceratorRecipe> {
   public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("scguns", "powered_macerating");
   public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("scguns", "textures/gui/powered_macerator_gui.png");
   public static final RecipeType<PoweredMaceratorRecipe> POWERED_MACERATING_TYPE = new RecipeType(UID, PoweredMaceratorRecipe.class);
   private final IDrawable background;
   private final IDrawable icon;
   private final int offsetX = 11;
   private final int offsetY = 16;

   public PoweredMaceratorCategory(IGuiHelper helper) {
      super();
      this.background = helper.createDrawable(TEXTURE, 11, 16, 128, 54);
      this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack((ItemLike)ModBlocks.POWERED_MACERATOR.get()));
   }

   public RecipeType<PoweredMaceratorRecipe> getRecipeType() {
      return POWERED_MACERATING_TYPE;
   }

   public Component getTitle() {
      return Component.translatable("block.scguns.powered_macerator");
   }

   public IDrawable getBackground() {
      return this.background;
   }

   public IDrawable getIcon() {
      return this.icon;
   }

   public void setRecipe(IRecipeLayoutBuilder builder, PoweredMaceratorRecipe recipe, IFocusGroup focuses) {
      int inputBaseX = 33;
      int inputBaseY = 11;
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

      builder.addSlot(RecipeIngredientRole.OUTPUT, 103, inputBaseY).addItemStack(recipe.getResultItem(null));
   }

   public void draw(PoweredMaceratorRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
      IRecipeCategory.super.draw(recipe, recipeSlotsView, guiGraphics, mouseX, mouseY);
      int processingTime = recipe.getProcessingTime();
      if (processingTime > 0) {
         int seconds = processingTime / 20;
         String processingTimeText = String.format("%ds", seconds);
         guiGraphics.drawString(Minecraft.getInstance().font, processingTimeText, 81, 44, Color.gray.getRGB(), false);
      }

      this.drawEnergyBar(guiGraphics, recipe);
      if (this.isMouseOverEnergyBar(mouseX, mouseY, 3, 5, 7, 42)) {
         this.renderEnergyTooltip(guiGraphics, mouseX, mouseY, recipe.getEnergyUse());
      }
   }

   private void drawEnergyBar(GuiGraphics guiGraphics, PoweredMaceratorRecipe recipe) {
      int energyRequired = recipe.getEnergyUse();
      int maxEnergy = 10000;
      int barHeight = 42;
      int energyHeight = (int)((float)(energyRequired * barHeight) / (float)maxEnergy);
      int energyBarX = 3;
      int energyBarY = 5 + (barHeight - energyHeight);
      guiGraphics.blit(TEXTURE, energyBarX, energyBarY, 176, 20 + (barHeight - energyHeight), 14, energyHeight);
   }

   private boolean isMouseOverEnergyBar(double mouseX, double mouseY, int x, int y, int width, int height) {
      return mouseX >= (double)x && mouseX <= (double)(x + width) && mouseY >= (double)y && mouseY <= (double)(y + height);
   }

   private void renderEnergyTooltip(GuiGraphics guiGraphics, double mouseX, double mouseY, int energyRequired) {
      List<Component> tooltip = new ArrayList<>();
      tooltip.add(Component.translatable("tooltip.powered_macerator.energy", new Object[]{energyRequired}));
      guiGraphics.renderTooltip(Minecraft.getInstance().font, tooltip, Optional.empty(), (int)mouseX, (int)mouseY);
   }
}
