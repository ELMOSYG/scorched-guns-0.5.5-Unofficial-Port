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
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import top.ribs.scguns.client.handler.AimingHandler;

public class TechCrosshair extends Crosshair {
   private static final ResourceLocation TECH_CROSSHAIR = ResourceLocation.fromNamespaceAndPath("scguns", "textures/crosshair/tech.png");
   private static final ResourceLocation DOT_CROSSHAIR = ResourceLocation.fromNamespaceAndPath("scguns", "textures/crosshair/dot.png");
   private float scale;
   private float prevScale;
   private float rotation;
   private float prevRotation;

   public TechCrosshair() {
      super(ResourceLocation.fromNamespaceAndPath("scguns", "tech"));
   }

   @Override
   public void tick() {
      this.prevRotation = this.rotation;
      this.prevScale = this.scale;
      this.rotation += 4.0F;
      this.scale *= 0.75F;
   }

   @Override
   public void onGunFired() {
      this.scale = 1.5F;
   }

   @Override
   public void render(Minecraft mc, PoseStack stack, int windowWidth, int windowHeight, float partialTicks) {
      float alpha = 1.0F - (float)AimingHandler.get().getNormalisedAdsProgress();
      float size = 8.0F;
      RenderSystem.enableBlend();
      RenderSystem.blendFuncSeparate(SourceFactor.ONE_MINUS_DST_COLOR, DestFactor.ONE_MINUS_SRC_COLOR, SourceFactor.ONE, DestFactor.ZERO);
      BufferBuilder buffer = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
      stack.pushPose();
      RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.setShaderTexture(0, DOT_CROSSHAIR);
      Matrix4f matrix = stack.last().pose();
      stack.translate(((float)windowWidth - size) / 2.0F, ((float)windowHeight - size) / 2.0F, 0.0F);
      buffer.addVertex(matrix, 0.0F, size, 0.0F).setUv(0.0F, 1.0F).setColor(1.0F, 1.0F, 1.0F, alpha);
      buffer.addVertex(matrix, size, size, 0.0F).setUv(1.0F, 1.0F).setColor(1.0F, 1.0F, 1.0F, alpha);
      buffer.addVertex(matrix, size, 0.0F, 0.0F).setUv(1.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, alpha);
      buffer.addVertex(matrix, 0.0F, 0.0F, 0.0F).setUv(0.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, alpha);
      BufferUploader.drawWithShader(buffer.buildOrThrow());
      stack.popPose();
      stack.pushPose();
      matrix = stack.last().pose();
      stack.translate((float)windowWidth / 2.0F, (float)windowHeight / 2.0F, 0.0F);
      float scale = 1.0F + Mth.lerp(partialTicks, this.prevScale, this.scale);
      stack.scale(scale, scale, scale);
      stack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, this.prevRotation, this.rotation)));
      stack.translate(-size / 2.0F, -size / 2.0F, 0.0F);
      RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.setShaderTexture(0, TECH_CROSSHAIR);
      // 1.21 has no reusable Tesselator builder: each pass begins its own.
      buffer = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
      buffer.addVertex(matrix, 0.0F, size, 0.0F).setUv(0.0F, 1.0F).setColor(1.0F, 1.0F, 1.0F, alpha);
      buffer.addVertex(matrix, size, size, 0.0F).setUv(1.0F, 1.0F).setColor(1.0F, 1.0F, 1.0F, alpha);
      buffer.addVertex(matrix, size, 0.0F, 0.0F).setUv(1.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, alpha);
      buffer.addVertex(matrix, 0.0F, 0.0F, 0.0F).setUv(0.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, alpha);
      BufferUploader.drawWithShader(buffer.buildOrThrow());
      stack.popPose();
      RenderSystem.defaultBlendFunc();
   }
}
