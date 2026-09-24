package top.ribs.scguns.client.render.armor;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import top.ribs.scguns.item.animated.ExoSuitItem;

public class ExoSuitModel extends GeoModel<ExoSuitItem> {
   public ExoSuitModel() {
      super();
   }

   public ResourceLocation getModelResource(ExoSuitItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "geo/exo_suit.geo.json");
   }

   public ResourceLocation getTextureResource(ExoSuitItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "textures/armor/exo_suit.png");
   }

   public ResourceLocation getAnimationResource(ExoSuitItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "animations/exo_suit.animation.json");
   }
}
