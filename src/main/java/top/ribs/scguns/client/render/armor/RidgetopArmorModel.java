package top.ribs.scguns.client.render.armor;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import top.ribs.scguns.item.animated.RidgetopArmorItem;

public class RidgetopArmorModel extends GeoModel<RidgetopArmorItem> {
   public RidgetopArmorModel() {
      super();
   }

   public ResourceLocation getModelResource(RidgetopArmorItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "geo/ridgetop.geo.json");
   }

   public ResourceLocation getTextureResource(RidgetopArmorItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "textures/armor/ridgetop.png");
   }

   public ResourceLocation getAnimationResource(RidgetopArmorItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "animations/ridgetop.animation.json");
   }
}
