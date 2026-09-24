package top.ribs.scguns.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import top.ribs.scguns.entity.monster.SkyCarrierEntity;

public class SkyCarrierRenderer extends MobRenderer<SkyCarrierEntity, SkyCarrierModel<SkyCarrierEntity>> {
   public SkyCarrierRenderer(Context pContext) {
      super(pContext, new SkyCarrierModel(pContext.bakeLayer(ModModelLayers.SKY_CARRIER_LAYER)), 0.4F);
   }

   public ResourceLocation getTextureLocation(SkyCarrierEntity pEntity) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "textures/entity/sky_carrier.png");
   }

   public void render(SkyCarrierEntity pEntity, float pEntityYaw, float pPartialTicks, PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight) {
      pMatrixStack.scale(1.1F, 1.1F, 1.1F);
      super.render(pEntity, pEntityYaw, pPartialTicks, pMatrixStack, pBuffer, pPackedLight);
   }

   protected void setupRotations(SkyCarrierEntity pEntityLiving, PoseStack pMatrixStack, float pAgeInTicks, float pRotationYaw, float pPartialTicks, float pScale) {
      super.setupRotations(pEntityLiving, pMatrixStack, pAgeInTicks, pRotationYaw, pPartialTicks, pScale);
      if (pEntityLiving.hurtTime > 0) {
         float hurtTime = (float)pEntityLiving.hurtTime - pPartialTicks;
         float shakeAmount = Mth.sin(hurtTime * 1.5F) * (float)pEntityLiving.hurtTime * 0.01F;
         pMatrixStack.mulPose(Axis.ZP.rotation(shakeAmount));
      }
   }
}
