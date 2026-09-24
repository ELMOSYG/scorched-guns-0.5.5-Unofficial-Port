package top.ribs.scguns.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import top.ribs.scguns.entity.projectile.SulfurGasCloudEntity;

public class SulfurGasCloudRenderer extends EntityRenderer<SulfurGasCloudEntity> {
   public SulfurGasCloudRenderer(Context context) {
      super(context);
   }

   public ResourceLocation getTextureLocation(SulfurGasCloudEntity entity) {
      return null;
   }

   public void render(SulfurGasCloudEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
   }
}
