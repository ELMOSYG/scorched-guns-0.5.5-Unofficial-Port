package top.ribs.scguns.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.FastColor;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import top.ribs.scguns.entity.monster.SulfurheadEntity;

public class SulfurheadGelLayer extends RenderLayer<SulfurheadEntity, SulfurheadModel<SulfurheadEntity>> {
   private static final ResourceLocation GEL_TEXTURE = ResourceLocation.fromNamespaceAndPath("scguns", "textures/entity/sulfurhead_gel.png");

   public SulfurheadGelLayer(RenderLayerParent<SulfurheadEntity, SulfurheadModel<SulfurheadEntity>> parent) {
      super(parent);
   }

   public void render(
      PoseStack poseStack,
      MultiBufferSource buffer,
      int packedLight,
      SulfurheadEntity entity,
      float limbSwing,
      float limbSwingAmount,
      float partialTicks,
      float ageInTicks,
      float netHeadYaw,
      float headPitch
   ) {
      VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucent(GEL_TEXTURE));
      float red = 1.0F;
      float green = 1.0F;
      float blue = 0.3F;
      float alpha = 0.4F;
      ((SulfurheadModel)this.getParentModel()).renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, FastColor.ARGB32.colorFromFloat(alpha, red, green, blue));
   }
}
