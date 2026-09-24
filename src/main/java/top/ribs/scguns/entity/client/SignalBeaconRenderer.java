package top.ribs.scguns.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import top.ribs.scguns.entity.monster.SignalBeaconEntity;

public class SignalBeaconRenderer extends MobRenderer<SignalBeaconEntity, SignalBeaconModel<SignalBeaconEntity>> {
   public SignalBeaconRenderer(Context pContext) {
      super(pContext, new SignalBeaconModel(pContext.bakeLayer(ModModelLayers.SIGNAL_BEACON_LAYER)), 0.4F);
   }

   public ResourceLocation getTextureLocation(SignalBeaconEntity pEntity) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "textures/entity/signal_beacon.png");
   }

   public void render(SignalBeaconEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
   }
}
