package top.ribs.scguns.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import top.ribs.scguns.entity.monster.SwarmEntity;

public class SwarmRenderer extends MobRenderer<SwarmEntity, SwarmModel<SwarmEntity>> {
   public SwarmRenderer(Context pContext) {
      super(pContext, new SwarmModel(pContext.bakeLayer(ModModelLayers.SWARM_LAYER)), 0.4F);
   }

   public ResourceLocation getTextureLocation(SwarmEntity pEntity) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "textures/entity/swarm.png");
   }

   public void render(SwarmEntity pEntity, float pEntityYaw, float pPartialTicks, PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight) {
      pMatrixStack.scale(1.5F, 1.0F, 1.5F);
      super.render(pEntity, pEntityYaw, pPartialTicks, pMatrixStack, pBuffer, pPackedLight);
   }
}
