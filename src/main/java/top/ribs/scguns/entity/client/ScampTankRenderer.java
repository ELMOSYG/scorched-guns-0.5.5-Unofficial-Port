package top.ribs.scguns.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import top.ribs.scguns.entity.monster.ScampTankEntity;

public class ScampTankRenderer extends MobRenderer<ScampTankEntity, ScampTankModel<ScampTankEntity>> {
   public ScampTankRenderer(Context pContext) {
      super(pContext, new ScampTankModel(pContext.bakeLayer(ModModelLayers.SCAMP_TANK_LAYER)), 2.8F);
   }

   public ResourceLocation getTextureLocation(ScampTankEntity pEntity) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "textures/entity/scamp_tank.png");
   }

   public void render(ScampTankEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      poseStack.pushPose();
      poseStack.scale(1.5F, 1.5F, 1.5F);
      super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
      poseStack.popPose();
   }
}
