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
import top.ribs.scguns.blockentity.AutoTurretBlockEntity;

@OnlyIn(Dist.CLIENT)
public class AutoTurretScreen extends AbstractContainerScreen<AutoTurretMenu> {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("scguns", "textures/gui/basic_turret.png");

   public AutoTurretScreen(AutoTurretMenu menu, Inventory inv, Component title) {
      super(menu, inv, title);
      this.imageHeight = 168;
      this.inventoryLabelY = this.imageHeight - 94;
   }

   protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
      RenderSystem.setShader(GameRenderer::getPositionTexShader);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.setShaderTexture(0, TEXTURE);
      int x = (this.width - this.imageWidth) / 2;
      int y = (this.height - this.imageHeight) / 2;
      guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
   }

   public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
      super.render(guiGraphics, mouseX, mouseY, delta);
      this.renderTooltip(guiGraphics, mouseX, mouseY);
      AutoTurretBlockEntity turret = ((AutoTurretMenu)this.menu).getBlockEntity();
      String ownerName = turret.getOwnerName();
      Component ownerText = Component.translatable("info.scguns.turret.owner", new Object[]{ownerName});
      guiGraphics.drawString(this.font, ownerText, this.leftPos + 90, this.topPos + 6, 4210752, false);
   }
}
