package top.ribs.scguns.client.render.armor;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import top.ribs.scguns.item.animated.CogKnightArmorItem;

public class CogKnightArmorModel extends GeoModel<CogKnightArmorItem> {
   public CogKnightArmorModel() {
      super();
   }

   public ResourceLocation getModelResource(CogKnightArmorItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "geo/cog_knight_armor.geo.json");
   }

   public ResourceLocation getTextureResource(CogKnightArmorItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "textures/armor/cog_knight_armor.png");
   }

   public ResourceLocation getAnimationResource(CogKnightArmorItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "animations/cog_knight_armor.animation.json");
   }
}
