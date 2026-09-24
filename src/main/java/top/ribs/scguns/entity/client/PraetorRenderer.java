package top.ribs.scguns.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import top.ribs.scguns.entity.monster.PraetorEntity;

public class PraetorRenderer extends MobRenderer<PraetorEntity, PraetorModel<PraetorEntity>> {
   public PraetorRenderer(Context pContext) {
      super(pContext, new PraetorModel(pContext.bakeLayer(ModModelLayers.PRAETOR_LAYER)), 0.7F);
   }

   public ResourceLocation getTextureLocation(PraetorEntity pEntity) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "textures/entity/praetor.png");
   }

   public void render(PraetorEntity pEntity, float pEntityYaw, float pPartialTicks, PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight) {
      pMatrixStack.pushPose();
      pMatrixStack.translate(0.0, 0.0, 0.0);
      pMatrixStack.scale(1.1F, 1.1F, 1.1F);
      super.render(pEntity, pEntityYaw, pPartialTicks, pMatrixStack, pBuffer, pPackedLight);
      pMatrixStack.popPose();
   }
}
