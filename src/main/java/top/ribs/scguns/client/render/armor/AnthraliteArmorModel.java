package top.ribs.scguns.client.render.armor;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import top.ribs.scguns.item.animated.AnthraliteArmorItem;

public class AnthraliteArmorModel extends GeoModel<AnthraliteArmorItem> {
   public AnthraliteArmorModel() {
      super();
   }

   public ResourceLocation getModelResource(AnthraliteArmorItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "geo/anthralite_armor.geo.json");
   }

   public ResourceLocation getTextureResource(AnthraliteArmorItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "textures/armor/anthralite_armor.png");
   }

   public ResourceLocation getAnimationResource(AnthraliteArmorItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "animations/anthralite_armor.animation.json");
   }
}
