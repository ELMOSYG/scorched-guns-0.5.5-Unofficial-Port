package top.ribs.scguns.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import top.ribs.scguns.entity.monster.DissidentEntity;

public class DissidentRenderer extends MobRenderer<DissidentEntity, DissidentModel<DissidentEntity>> {
   public DissidentRenderer(Context pContext) {
      super(pContext, new DissidentModel(pContext.bakeLayer(ModModelLayers.DISSIDENT_LAYER)), 0.7F);
   }

   public ResourceLocation getTextureLocation(DissidentEntity pEntity) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "textures/entity/dissident.png");
   }

   public void render(DissidentEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      poseStack.pushPose();
      poseStack.scale(1.15F, 1.15F, 1.15F);
      super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
      poseStack.popPose();
   }
}
