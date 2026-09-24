package top.ribs.scguns.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import top.ribs.scguns.entity.monster.SupplyScampEntity;

public class SupplyScampRenderer extends MobRenderer<SupplyScampEntity, SupplyScampModel<SupplyScampEntity>> {
   public SupplyScampRenderer(Context pContext) {
      super(pContext, new SupplyScampModel(pContext.bakeLayer(ModModelLayers.SUPPLY_SCAMP_LAYER)), 0.7F);
      this.addLayer(new SupplyScampPumpkinLayer(this));
   }

   public ResourceLocation getTextureLocation(SupplyScampEntity pEntity) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "textures/entity/supply_scamp.png");
   }

   public void render(SupplyScampEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      poseStack.pushPose();
      poseStack.translate(0.0, 0.05, 0.0);
      super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
      poseStack.popPose();
   }
}
