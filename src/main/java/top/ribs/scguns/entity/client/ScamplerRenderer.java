package top.ribs.scguns.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import top.ribs.scguns.entity.monster.ScamplerEntity;

public class ScamplerRenderer extends MobRenderer<ScamplerEntity, ScamplerModel<ScamplerEntity>> {
   public ScamplerRenderer(Context pContext) {
      super(pContext, new ScamplerModel(pContext.bakeLayer(ModModelLayers.SCAMPLER_LAYER)), 0.4F);
   }

   public ResourceLocation getTextureLocation(ScamplerEntity pEntity) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "textures/entity/scampler.png");
   }

   public void render(ScamplerEntity pEntity, float pEntityYaw, float pPartialTicks, PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight) {
      pMatrixStack.scale(1.1F, 1.1F, 1.1F);
      super.render(pEntity, pEntityYaw, pPartialTicks, pMatrixStack, pBuffer, pPackedLight);
   }
}
