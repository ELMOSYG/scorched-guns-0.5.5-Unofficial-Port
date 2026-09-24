package top.ribs.scguns.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import top.ribs.scguns.entity.projectile.ProjectileEntity;

public class GrenadeRoundRenderer extends EntityRenderer<ProjectileEntity> {
   public GrenadeRoundRenderer(Context context) {
      super(context);
   }

   public ResourceLocation getTextureLocation(ProjectileEntity entity) {
      return null;
   }

   public void render(ProjectileEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource renderTypeBuffer, int light) {
      if (!entity.getProjectile().isVisible() && entity.tickCount > 2) {
         poseStack.pushPose();
         poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
         poseStack.mulPose(Axis.YP.rotationDegrees(entityYaw));
         poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot() - 90.0F));
         float spin = ((float)entity.tickCount + partialTicks) * 12.0F;
         poseStack.mulPose(Axis.YP.rotationDegrees(spin));
         Minecraft.getInstance()
            .getItemRenderer()
            .renderStatic(entity.getItem(), ItemDisplayContext.NONE, 15728880, OverlayTexture.NO_OVERLAY, poseStack, renderTypeBuffer, entity.level(), 0);
         poseStack.scale(1.25F, 1.25F, 1.25F);
         poseStack.translate(0.0F, -1.0F, 0.0F);
         float pulseScale = 1.0F + Mth.sin(((float)entity.tickCount + partialTicks) * 0.5F) * 0.15F;
         poseStack.scale(pulseScale, pulseScale, pulseScale);
         poseStack.popPose();
      }
   }
}
