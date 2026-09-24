package top.ribs.scguns.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import top.ribs.scguns.entity.throwable.ThrowableGrenadeEntity;

public class ThrowableItemRenderer extends EntityRenderer<ThrowableGrenadeEntity> {
   public ThrowableItemRenderer(Context context) {
      super(context);
   }

   @Nullable
   public ResourceLocation getTextureLocation(ThrowableGrenadeEntity entity) {
      return null;
   }

   public void render(ThrowableGrenadeEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource renderTypeBuffer, int light) {
      poseStack.pushPose();
      poseStack.translate(0.0, 0.0, 0.0);
      poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
      Minecraft.getInstance()
         .getItemRenderer()
         .renderStatic(entity.getItem(), ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, poseStack, renderTypeBuffer, entity.level(), 0);
      poseStack.popPose();
   }
}
