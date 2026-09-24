package top.ribs.scguns.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import top.ribs.scguns.entity.monster.AdjudicatorEntity;

public class AdjudicatorRenderer extends MobRenderer<AdjudicatorEntity, AdjudicatorModel<AdjudicatorEntity>> {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("scguns", "textures/entity/adjudicator.png");

   public AdjudicatorRenderer(Context context) {
      super(context, new AdjudicatorModel(context.bakeLayer(ModModelLayers.ADJUDICATOR_LAYER)), 0.6F);
      this.addLayer(new ItemInHandLayer(this, context.getItemInHandRenderer()));
   }

   public ResourceLocation getTextureLocation(AdjudicatorEntity entity) {
      return TEXTURE;
   }

   public void render(AdjudicatorEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      poseStack.pushPose();
      poseStack.scale(1.15F, 1.15F, 1.15F);
      super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
      poseStack.popPose();
   }
}
