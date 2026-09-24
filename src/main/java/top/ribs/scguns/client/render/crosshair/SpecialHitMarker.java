package top.ribs.scguns.client.render.crosshair;

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
import top.ribs.scguns.client.handler.CrosshairHandler;
import top.ribs.scguns.client.handler.HUDRenderHandler;

public class SpecialHitMarker extends Crosshair {
   private static final ResourceLocation NORMAL_HITMARKER = ResourceLocation.fromNamespaceAndPath("scguns", "textures/crosshair/special_hit_marker.png");
   private static final ResourceLocation ALT_HITMARKER = ResourceLocation.fromNamespaceAndPath("scguns", "textures/crosshair/special_hit_marker2.png");

   public SpecialHitMarker() {
      super(ResourceLocation.fromNamespaceAndPath("scguns", "special_hit_marker"));
   }

   @Override
   public void render(Minecraft mc, PoseStack stack, int windowWidth, int windowHeight, float partialTicks) {
      stack.pushPose();
      float alpha = 1.0F;
      float size = 9.0F;
      float hitMarkerProgress = HUDRenderHandler.getHitMarkerProgress(partialTicks);
      boolean crit = HUDRenderHandler.getHitMarkerCrit();
      float scale = 1.5F + hitMarkerProgress * 1.0F;
      stack.translate((double)Math.round((float)windowWidth / 2.0F) - 0.5, (double)Math.round((float)windowHeight / 2.0F) - 0.5, 0.0);
      stack.scale(scale, scale, scale);
      stack.translate(-size / 2.0F, -size / 2.0F, 0.0F);
      RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
      RenderSystem.setShaderColor(1.0F, !crit ? 1.0F : 0.25F, !crit ? 1.0F : 0.25F, 1.0F - hitMarkerProgress * 0.3F);
      boolean useAltHitMarker = CrosshairHandler.get().getCurrentCrosshair().getLocation().equals(ResourceLocation.fromNamespaceAndPath("scguns", "hit_marker"));
      ResourceLocation texture = useAltHitMarker ? ALT_HITMARKER : NORMAL_HITMARKER;
      RenderSystem.setShaderTexture(0, texture);
      RenderSystem.enableBlend();
      Matrix4f matrix = stack.last().pose();
      BufferBuilder buffer = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
      buffer.addVertex(matrix, 0.0F, size, 0.0F).setUv(0.0F, 1.0F).setColor(1.0F, 1.0F, 1.0F, alpha);
      buffer.addVertex(matrix, size, size, 0.0F).setUv(1.0F, 1.0F).setColor(1.0F, 1.0F, 1.0F, alpha);
      buffer.addVertex(matrix, size, 0.0F, 0.0F).setUv(1.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, alpha);
      buffer.addVertex(matrix, 0.0F, 0.0F, 0.0F).setUv(0.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, alpha);
      BufferUploader.drawWithShader(buffer.buildOrThrow());
      stack.popPose();
   }
}
