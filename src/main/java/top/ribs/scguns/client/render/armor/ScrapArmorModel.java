package top.ribs.scguns.client.render.armor;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import top.ribs.scguns.item.animated.ScrapArmorItem;

public class ScrapArmorModel extends GeoModel<ScrapArmorItem> {
   public ScrapArmorModel() {
      super();
   }

   public ResourceLocation getModelResource(ScrapArmorItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "geo/scrap_armor.geo.json");
   }

   public ResourceLocation getTextureResource(ScrapArmorItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "textures/armor/scrap_armor.png");
   }

   public ResourceLocation getAnimationResource(ScrapArmorItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "animations/scrap_armor.animation.json");
   }
}
