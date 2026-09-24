package top.ribs.scguns.client.render.crosshair;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import top.ribs.scguns.client.handler.AimingHandler;

public class TexturedCrosshair extends Crosshair {
   private final ResourceLocation texture;
   private final boolean blend;

   public TexturedCrosshair(ResourceLocation id) {
      this(id, true);
   }

   public TexturedCrosshair(ResourceLocation id, boolean blend) {
      super(id);
      this.texture = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "textures/crosshair/" + id.getPath() + ".png");
      this.blend = blend;
   }

   @Override
   public void render(Minecraft mc, PoseStack stack, int windowWidth, int windowHeight, float partialTicks) {
      stack.pushPose();
      float alpha = 1.0F - (float)AimingHandler.get().getNormalisedAdsProgress();
      float size = 8.0F;
      stack.translate(((float)windowWidth - size) / 2.0F, ((float)windowHeight - size) / 2.0F, 0.0F);
      RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.setShaderTexture(0, this.texture);
      RenderSystem.enableBlend();
      if (this.blend) {
         RenderSystem.blendFuncSeparate(SourceFactor.ONE_MINUS_DST_COLOR, DestFactor.ONE_MINUS_SRC_COLOR, SourceFactor.ONE, DestFactor.ZERO);
      }

      Matrix4f matrix = stack.last().pose();
      BufferBuilder buffer = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
      buffer.addVertex(matrix, 0.0F, size, 0.0F).setUv(0.0F, 1.0F).setColor(1.0F, 1.0F, 1.0F, alpha);
      buffer.addVertex(matrix, size, size, 0.0F).setUv(1.0F, 1.0F).setColor(1.0F, 1.0F, 1.0F, alpha);
      buffer.addVertex(matrix, size, 0.0F, 0.0F).setUv(1.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, alpha);
      buffer.addVertex(matrix, 0.0F, 0.0F, 0.0F).setUv(0.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, alpha);
      BufferUploader.drawWithShader(buffer.buildOrThrow());
      if (this.blend) {
         RenderSystem.defaultBlendFunc();
      }

      stack.popPose();
   }
}
