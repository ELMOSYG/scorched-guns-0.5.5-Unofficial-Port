package top.ribs.scguns.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import top.ribs.scguns.entity.monster.HiveEntity;

public class HiveRenderer extends MobRenderer<HiveEntity, HiveModel<HiveEntity>> {
   public HiveRenderer(Context pContext) {
      super(pContext, new HiveModel(pContext.bakeLayer(ModModelLayers.HIVE_LAYER)), 0.6F);
   }

   public ResourceLocation getTextureLocation(HiveEntity hiveEntity) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "textures/entity/hive.png");
   }

   public void render(HiveEntity pEntity, float pEntityYaw, float pPartialTicks, PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight) {
      pMatrixStack.scale(0.9F, 0.9F, 0.9F);
      super.render(pEntity, pEntityYaw, pPartialTicks, pMatrixStack, pBuffer, pPackedLight);
   }
}
