package top.ribs.scguns.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import top.ribs.scguns.entity.monster.CogKnightEntity;

public class CogKnightRenderer extends MobRenderer<CogKnightEntity, CogKnightModel<CogKnightEntity>> {
   public CogKnightRenderer(Context pContext) {
      super(pContext, new CogKnightModel(pContext.bakeLayer(ModModelLayers.COG_KNIGHT_LAYER)), 0.7F);
      this.addLayer(new ItemInHandLayer(this, pContext.getItemInHandRenderer()));
   }

   public ResourceLocation getTextureLocation(CogKnightEntity pEntity) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "textures/entity/cog_knight.png");
   }

   public void render(CogKnightEntity pEntity, float pEntityYaw, float pPartialTicks, PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight) {
      pMatrixStack.pushPose();
      pMatrixStack.translate(0.0, 0.35, 0.0);
      pMatrixStack.scale(1.0F, 1.0F, 1.0F);
      super.render(pEntity, pEntityYaw, pPartialTicks, pMatrixStack, pBuffer, pPackedLight);
      pMatrixStack.popPose();
   }
}
