package top.ribs.scguns.entity.client;

import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import top.ribs.scguns.entity.monster.TheMerchantEntity;

public class TheMerchantRenderer extends MobRenderer<TheMerchantEntity, TheMerchantModel<TheMerchantEntity>> {
   public TheMerchantRenderer(Context pContext) {
      super(pContext, new TheMerchantModel(pContext.bakeLayer(ModModelLayers.THE_MERCHANT_LAYER)), 0.7F);
   }

   public ResourceLocation getTextureLocation(TheMerchantEntity pEntity) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "textures/entity/the_merchant.png");
   }
}
