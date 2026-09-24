package top.ribs.scguns.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import top.ribs.scguns.entity.throwable.GrenadeEntity;

public class GrenadeRenderer extends EntityRenderer<GrenadeEntity> {
   public GrenadeRenderer(Context context) {
      super(context);
   }

   public ResourceLocation getTextureLocation(GrenadeEntity entity) {
      return null;
   }

   public void render(GrenadeEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource renderTypeBuffer, int light) {
      if (!entity.getProjectile().isVisible() && entity.tickCount > 1) {
         poseStack.pushPose();
         poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
         poseStack.mulPose(Axis.YP.rotationDegrees(entityYaw));
         poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
         float rotation = (float)entity.tickCount + partialTicks;
         poseStack.translate(0.0, 0.15, 0.0);
         poseStack.mulPose(Axis.XN.rotationDegrees(rotation * 20.0F));
         poseStack.translate(0.0, -0.15, 0.0);
         poseStack.translate(0.0, 0.5, 0.0);
         Minecraft.getInstance()
            .getItemRenderer()
            .renderStatic(entity.getItem(), ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, poseStack, renderTypeBuffer, entity.level(), 0);
         poseStack.popPose();
      }
   }
}
