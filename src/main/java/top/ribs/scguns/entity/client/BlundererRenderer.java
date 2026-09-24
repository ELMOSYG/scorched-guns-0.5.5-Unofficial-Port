package top.ribs.scguns.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import top.ribs.scguns.entity.monster.BlundererEntity;

public class BlundererRenderer extends MobRenderer<BlundererEntity, BlundererModel<BlundererEntity>> {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("scguns", "textures/entity/blunderer.png");

   public BlundererRenderer(Context context) {
      super(context, new BlundererModel(context.bakeLayer(ModModelLayers.BLUNDERER_LAYER)), 0.6F);
      this.addLayer(new ItemInHandLayer(this, context.getItemInHandRenderer()));
      this.addLayer(new CustomHeadLayer(this, context.getModelSet(), context.getItemInHandRenderer()));
   }

   public ResourceLocation getTextureLocation(BlundererEntity entity) {
      return TEXTURE;
   }

   public void render(BlundererEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      poseStack.pushPose();
      poseStack.scale(1.1F, 1.1F, 1.1F);
      super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
      poseStack.popPose();
   }
}
