package top.ribs.scguns.client.render.armor;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import top.ribs.scguns.item.animated.BrassMaskArmorItem;

public class BrassMaskArmorModel extends GeoModel<BrassMaskArmorItem> {
   public BrassMaskArmorModel() {
      super();
   }

   public ResourceLocation getModelResource(BrassMaskArmorItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "geo/brass_mask.geo.json");
   }

   public ResourceLocation getTextureResource(BrassMaskArmorItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "textures/armor/brass_mask.png");
   }

   public ResourceLocation getAnimationResource(BrassMaskArmorItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "animations/brass_mask.animation.json");
   }
}
