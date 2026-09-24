package top.ribs.scguns.client.render.armor;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import top.ribs.scguns.item.animated.AdrienArmorItem;

public class AdrienArmorModel extends GeoModel<AdrienArmorItem> {
   public AdrienArmorModel() {
      super();
   }

   public ResourceLocation getModelResource(AdrienArmorItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "geo/adrien_armor.geo.json");
   }

   public ResourceLocation getTextureResource(AdrienArmorItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "textures/armor/adrien_armor.png");
   }

   public ResourceLocation getAnimationResource(AdrienArmorItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "animations/adrien_armor.animation.json");
   }
}
