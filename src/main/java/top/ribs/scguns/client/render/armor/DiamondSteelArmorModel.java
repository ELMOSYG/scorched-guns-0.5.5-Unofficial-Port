package top.ribs.scguns.client.render.armor;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import top.ribs.scguns.item.animated.DiamondSteelArmorItem;

public class DiamondSteelArmorModel extends GeoModel<DiamondSteelArmorItem> {
   public DiamondSteelArmorModel() {
      super();
   }

   public ResourceLocation getModelResource(DiamondSteelArmorItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "geo/diamond_steel_armor.geo.json");
   }

   public ResourceLocation getTextureResource(DiamondSteelArmorItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "textures/armor/diamond_steel_armor.png");
   }

   public ResourceLocation getAnimationResource(DiamondSteelArmorItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "animations/diamond_steel_armor.animation.json");
   }
}
