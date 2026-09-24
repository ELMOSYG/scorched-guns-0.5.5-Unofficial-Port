package top.ribs.scguns.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import top.ribs.scguns.entity.monster.SubjugatorEntity;

public class SubjugatorRenderer extends MobRenderer<SubjugatorEntity, SubjugatorModel<SubjugatorEntity>> {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("scguns", "textures/entity/subjugator.png");

   public SubjugatorRenderer(Context context) {
      super(context, new SubjugatorModel(context.bakeLayer(ModModelLayers.SUBJUGATOR_LAYER)), 0.5F);
      this.addLayer(new ItemInHandLayer(this, context.getItemInHandRenderer()));
   }

   public ResourceLocation getTextureLocation(SubjugatorEntity entity) {
      return TEXTURE;
   }

   public void render(SubjugatorEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      poseStack.pushPose();
      poseStack.scale(1.15F, 1.15F, 1.15F);
      super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
      poseStack.popPose();
   }
}
