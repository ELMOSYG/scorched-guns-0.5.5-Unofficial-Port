package top.ribs.scguns.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import top.ribs.scguns.entity.monster.FinforcerEntity;

public class FinforcerRenderer extends MobRenderer<FinforcerEntity, FinforcerModel<FinforcerEntity>> {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("scguns", "textures/entity/finforcer.png");

   public FinforcerRenderer(Context context) {
      super(context, new FinforcerModel(context.bakeLayer(ModModelLayers.FINFORCER_LAYER)), 0.5F);
      this.addLayer(new ItemInHandLayer(this, context.getItemInHandRenderer()));
   }

   public ResourceLocation getTextureLocation(FinforcerEntity entity) {
      return TEXTURE;
   }

   public void render(FinforcerEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      poseStack.pushPose();
      poseStack.translate(0.0, 0.1, 0.0);
      poseStack.scale(1.1F, 1.1F, 1.1F);
      super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
      poseStack.popPose();
   }
}
