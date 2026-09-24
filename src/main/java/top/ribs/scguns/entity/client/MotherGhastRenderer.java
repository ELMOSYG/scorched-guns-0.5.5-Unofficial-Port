package top.ribs.scguns.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import top.ribs.scguns.entity.monster.MotherGhastEntity;

public class MotherGhastRenderer extends MobRenderer<MotherGhastEntity, MotherGhastModel<MotherGhastEntity>> {
   public MotherGhastRenderer(Context pContext) {
      super(pContext, new MotherGhastModel(pContext.bakeLayer(ModModelLayers.MOTHER_GHAST_LAYER)), 3.5F);
   }

   public ResourceLocation getTextureLocation(MotherGhastEntity pEntity) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "textures/entity/mother_ghast.png");
   }

   public void render(MotherGhastEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      poseStack.pushPose();
      poseStack.translate(0.0, 2.0, 0.0);
      poseStack.scale(3.5F, 3.5F, 3.5F);
      super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
      poseStack.popPose();
   }
}
