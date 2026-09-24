package top.ribs.scguns.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class PoweredMaceratorScreen extends AbstractContainerScreen<PoweredMaceratorMenu> {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("scguns", "textures/gui/powered_macerator_gui.png");
   private static final int BAR_WIDTH = 7;
   private static final int BAR_HEIGHT = 42;
   private static final int BAR_X = 14;
   private static final int BAR_Y = 21;
   private static final int TEXTURE_BAR_X = 176;
   private static final int TEXTURE_BAR_Y = 32;

   public PoweredMaceratorScreen(PoweredMaceratorMenu menu, Inventory inv, Component title) {
      super(menu, inv, title);
   }

   protected void init() {
      super.init();
      this.titleLabelY = 6;
   }

   protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
      RenderSystem.setShader(GameRenderer::getPositionTexShader);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.setShaderTexture(0, TEXTURE);
      int x = (this.width - this.imageWidth) / 2;
      int y = (this.height - this.imageHeight) / 2;
      guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
      this.renderProgressArrow(guiGraphics, x, y);
      this.renderEnergyBar(guiGraphics, x, y);
   }

   private void renderProgressArrow(GuiGraphics guiGraphics, int x, int y) {
      if (((PoweredMaceratorMenu)this.menu).isCrafting()) {
         int progress = ((PoweredMaceratorMenu)this.menu).getScaledProgress();
         guiGraphics.blit(TEXTURE, x + 84, y + 25, 176, 14, progress + 1, 16);
      }
   }

   private void renderEnergyBar(GuiGraphics guiGraphics, int x, int y) {
      int energy = ((PoweredMaceratorMenu)this.menu).getEnergy();
      int maxEnergy = ((PoweredMaceratorMenu)this.menu).getMaxEnergy();
      int energyHeight = (int)((float)(energy * 42) / (float)maxEnergy);
      guiGraphics.blit(TEXTURE, x + 14, y + 21 + (42 - energyHeight), 176, 32 + (42 - energyHeight), 7, energyHeight);
   }

   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
      super.render(guiGraphics, mouseX, mouseY, delta);
      this.renderTooltip(guiGraphics, mouseX, mouseY);
      int x = (this.width - this.imageWidth) / 2;
      int y = (this.height - this.imageHeight) / 2;
      if (this.isMouseOverEnergyBar(mouseX, mouseY, x, y)) {
         this.renderEnergyTooltip(guiGraphics, mouseX, mouseY);
      }
   }

   private boolean isMouseOverEnergyBar(int mouseX, int mouseY, int x, int y) {
      return mouseX >= x + 14 && mouseX <= x + 14 + 7 && mouseY >= y + 21 && mouseY <= y + 21 + 42;
   }

   private void renderEnergyTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
      List<Component> tooltip = new ArrayList<>();
      tooltip.add(Component.translatable("tooltip.powered_macerator.energy", new Object[]{((PoweredMaceratorMenu)this.menu).getEnergy()}));
      guiGraphics.renderTooltip(this.font, tooltip, Optional.empty(), mouseX, mouseY);
   }
}
