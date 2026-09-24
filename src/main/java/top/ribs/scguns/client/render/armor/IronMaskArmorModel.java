package top.ribs.scguns.client.render.armor;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import top.ribs.scguns.item.animated.IronMaskArmorItem;

public class IronMaskArmorModel extends GeoModel<IronMaskArmorItem> {
   public IronMaskArmorModel() {
      super();
   }

   public ResourceLocation getModelResource(IronMaskArmorItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "geo/iron_mask.geo.json");
   }

   public ResourceLocation getTextureResource(IronMaskArmorItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "textures/armor/iron_mask.png");
   }

   public ResourceLocation getAnimationResource(IronMaskArmorItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "animations/iron_mask.animation.json");
   }
}
