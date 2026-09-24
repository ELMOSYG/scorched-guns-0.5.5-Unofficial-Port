package top.ribs.scguns.client.render.armor;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import top.ribs.scguns.item.animated.RedcoatArmorItem;

public class RedcoatArmorModel extends GeoModel<RedcoatArmorItem> {
   public RedcoatArmorModel() {
      super();
   }

   public ResourceLocation getModelResource(RedcoatArmorItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "geo/redcoat_armor.geo.json");
   }

   public ResourceLocation getTextureResource(RedcoatArmorItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "textures/armor/redcoat_armor.png");
   }

   public ResourceLocation getAnimationResource(RedcoatArmorItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "animations/redcoat_armor.animation.json");
   }
}
