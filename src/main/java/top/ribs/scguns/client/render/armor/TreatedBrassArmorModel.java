package top.ribs.scguns.client.render.armor;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import top.ribs.scguns.item.animated.TreatedBrassArmorItem;

public class TreatedBrassArmorModel extends GeoModel<TreatedBrassArmorItem> {
   public TreatedBrassArmorModel() {
      super();
   }

   public ResourceLocation getModelResource(TreatedBrassArmorItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "geo/treated_brass_armor.geo.json");
   }

   public ResourceLocation getTextureResource(TreatedBrassArmorItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "textures/armor/treated_brass_armor.png");
   }

   public ResourceLocation getAnimationResource(TreatedBrassArmorItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "animations/treated_brass_armor.animation.json");
   }
}
