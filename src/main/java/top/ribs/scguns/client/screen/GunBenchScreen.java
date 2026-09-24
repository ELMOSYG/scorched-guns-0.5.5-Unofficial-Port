package top.ribs.scguns.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.RecipeBookCategories;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import top.ribs.scguns.client.recipebook.GunBenchRecipeBookCategories;
import top.ribs.scguns.client.recipebook.ScgunsRecipeBookCategories;

/**
 * Gun bench screen, with vanilla's recipe book attached.
 *
 * <p>Mirrors {@code CraftingScreen} (the vanilla template, in {@code .refs/nf-src}): the book
 * component owns the tab strip and the ghost recipe, the toggle button shifts the panel to the right
 * when the book opens, and the input hooks forward clicks and keys to it first. The one difference is
 * that this GUI is drawn from its own texture at {@code leftPos/topPos} rather than vanilla's, which
 * is why {@code renderBg} follows those fields instead of computing the panel position itself.</p>
 */
@OnlyIn(Dist.CLIENT)
public class GunBenchScreen extends AbstractContainerScreen<GunBenchMenu> implements RecipeUpdateListener {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("scguns", "textures/gui/gun_bench_gui.png");
   private final RecipeBookComponent recipeBookComponent = new RecipeBookComponent();
   private boolean widthTooNarrow;

   public GunBenchScreen(GunBenchMenu menu, Inventory inv, Component title) {
      super(menu, inv, title);
   }

   /**
    * Only wire the book up when its categories exist.
    *
    * <p>With the enum extension missing there is no gun bench book type, and attaching the component
    * anyway would show vanilla's crafting tab inside the bench - the recipes of a block this is not
    * (HANDOFF 39.5). No book is strictly better than the wrong book.</p>
    */
   private boolean bookEnabled() {
      return GunBenchRecipeBookCategories.isEnabled();
   }

   @Override
   protected void init() {
      super.init();
      if (!this.bookEnabled()) {
         return;
      }

      this.widthTooNarrow = this.width < 379;
      this.recipeBookComponent.init(this.width, this.height, this.minecraft, this.widthTooNarrow, this.menu);
      this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);
      this.addRenderableWidget(new ImageButton(this.leftPos + 5, this.height / 2 - 49, 20, 18,
         RecipeBookComponent.RECIPE_BUTTON_SPRITES, button -> {
            this.recipeBookComponent.toggleVisibility();
            this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);
            button.setPosition(this.leftPos + 5, this.height / 2 - 49);
         }));
      this.addWidget(this.recipeBookComponent);
      this.titleLabelX = 29;
      this.logBookContents();
   }

   /**
    * One log line per book open, stating what the book will actually draw.
    *
    * <p>"The bench only shows N guns" cannot be settled from a screenshot: the book draws only the
    * recipes the player has <b>unlocked</b>, and one blueprint legitimately covers one tier, so a
    * short list is usually correct rather than broken. Printing the client's own collection sizes -
    * plus {@code /scguns recipebook} for the server's view - turns that into a number instead of a
    * guess (HANDOFF section 39.7).</p>
    */
   private void logBookContents() {
      if (this.minecraft == null || this.minecraft.player == null) {
         return;
      }

      ClientRecipeBook book = this.minecraft.player.getRecipeBook();
      List<RecipeBookCategories> categories = ScgunsRecipeBookCategories.all();
      StringBuilder counts = new StringBuilder();
      int unlocked = 0;

      for (RecipeBookCategories category : categories) {
         if (GunBenchRecipeBookCategories.isAggregate(category)) {
            // The search tab is the union of the others, so counting it would report every recipe
            // under it and then zero for every real tab - which is exactly how this line misled the
            // first reader (HANDOFF section 45). The groups are what the book actually shows.
            continue;
         }

         int count = 0;
         for (RecipeCollection collection : book.getCollection(category)) {
            count += collection.getRecipes(false).size();
         }

         unlocked += count;
         if (counts.length() > 0) {
            counts.append(' ');
         }

         counts.append(category.name()).append('=').append(count);
      }

      GunBenchRecipeBookCategories.logger().info("SCGUNS-RECIPEBOOK display type={} tabs=[{}] unlocked={}",
         this.menu.getRecipeBookType().name(), counts, unlocked);
   }

   @Override
   public void containerTick() {
      super.containerTick();
      if (this.bookEnabled()) {
         this.recipeBookComponent.tick();
      }
   }

   @Override
   protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
      RenderSystem.setShader(GameRenderer::getPositionTexShader);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.setShaderTexture(0, TEXTURE);
      guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
   }

   @Override
   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
      if (!this.bookEnabled()) {
         super.render(guiGraphics, mouseX, mouseY, delta);
         this.renderTooltip(guiGraphics, mouseX, mouseY);
         return;
      }

      if (this.recipeBookComponent.isVisible() && this.widthTooNarrow) {
         this.renderBackground(guiGraphics, mouseX, mouseY, delta);
         this.recipeBookComponent.render(guiGraphics, mouseX, mouseY, delta);
      } else {
         super.render(guiGraphics, mouseX, mouseY, delta);
         this.recipeBookComponent.render(guiGraphics, mouseX, mouseY, delta);
         this.recipeBookComponent.renderGhostRecipe(guiGraphics, this.leftPos, this.topPos, true, delta);
      }

      this.renderTooltip(guiGraphics, mouseX, mouseY);
      this.recipeBookComponent.renderTooltip(guiGraphics, this.leftPos, this.topPos, mouseX, mouseY);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      return this.bookEnabled() && this.recipeBookComponent.keyPressed(keyCode, scanCode, modifiers)
         || super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean charTyped(char codePoint, int modifiers) {
      return this.bookEnabled() && this.recipeBookComponent.charTyped(codePoint, modifiers)
         || super.charTyped(codePoint, modifiers);
   }

   @Override
   protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
      return (!this.bookEnabled() || !this.widthTooNarrow || !this.recipeBookComponent.isVisible())
         && super.isHovering(x, y, width, height, mouseX, mouseY);
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.bookEnabled() && this.recipeBookComponent.mouseClicked(mouseX, mouseY, button)) {
         this.setFocused(this.recipeBookComponent);
         return true;
      }

      return this.bookEnabled() && this.widthTooNarrow && this.recipeBookComponent.isVisible()
         || super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int mouseButton) {
      boolean outsideGui = mouseX < (double) guiLeft
         || mouseY < (double) guiTop
         || mouseX >= (double) (guiLeft + this.imageWidth)
         || mouseY >= (double) (guiTop + this.imageHeight);
      return (!this.bookEnabled() || this.recipeBookComponent.hasClickedOutside(mouseX, mouseY, this.leftPos,
         this.topPos, this.imageWidth, this.imageHeight, mouseButton)) && outsideGui;
   }

   @Override
   protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
      super.slotClicked(slot, slotId, mouseButton, type);
      if (this.bookEnabled()) {
         this.recipeBookComponent.slotClicked(slot);
      }
   }

   @Override
   public void recipesUpdated() {
      if (this.bookEnabled()) {
         this.recipeBookComponent.recipesUpdated();
      }
   }

   @Override
   public RecipeBookComponent getRecipeBookComponent() {
      return this.recipeBookComponent;
   }
}
