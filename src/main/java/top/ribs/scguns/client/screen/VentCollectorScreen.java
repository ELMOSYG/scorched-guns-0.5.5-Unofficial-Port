package top.ribs.scguns.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class VentCollectorScreen extends AbstractContainerScreen<VentCollectorMenu> {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("scguns", "textures/gui/vent_collector.png");

   public VentCollectorScreen(VentCollectorMenu menu, Inventory inv, Component title) {
      super(menu, inv, title);
      this.imageHeight = 166;
      this.inventoryLabelY = this.imageHeight - 94;
   }

   protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
      RenderSystem.setShader(GameRenderer::getPositionTexShader);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.setShaderTexture(0, TEXTURE);
      int x = (this.width - this.imageWidth) / 2;
      int y = (this.height - this.imageHeight) / 2;
      guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
      int filterCharge = ((VentCollectorMenu)this.menu).getFilterCharge();
      int maxCharge = ((VentCollectorMenu)this.menu).getMaxFilterCharge();
      int barWidth = (int)((float)filterCharge / (float)maxCharge * 52.0F);
      guiGraphics.blit(TEXTURE, x + 62, y + 37, 176, 0, barWidth, 8);
   }

   public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
      super.render(guiGraphics, mouseX, mouseY, delta);
      this.renderTooltip(guiGraphics, mouseX, mouseY);
   }

   protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
      super.renderTooltip(guiGraphics, x, y);
      int startX = (this.width - this.imageWidth) / 2 + 62;
      int startY = (this.height - this.imageHeight) / 2 + 37;
      if (x >= startX && x < startX + 52 && y >= startY && y < startY + 8) {
         int filterCharge = ((VentCollectorMenu)this.menu).getFilterCharge();
         int maxCharge = ((VentCollectorMenu)this.menu).getMaxFilterCharge();
         guiGraphics.renderTooltip(this.font, Component.translatable("gui.scguns.vent_collector.filter_charge", new Object[]{filterCharge, maxCharge}), x, y);
      }
   }
}
