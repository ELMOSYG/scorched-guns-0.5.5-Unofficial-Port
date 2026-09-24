package top.ribs.scguns.client.render.armor;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import top.ribs.scguns.item.animated.AnthraliteGasMaskArmorItem;

public class AnthraliteGasMaskArmorModel extends GeoModel<AnthraliteGasMaskArmorItem> {
   public AnthraliteGasMaskArmorModel() {
      super();
   }

   public ResourceLocation getModelResource(AnthraliteGasMaskArmorItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "geo/anthralite_respirator.geo.json");
   }

   public ResourceLocation getTextureResource(AnthraliteGasMaskArmorItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "textures/armor/anthralite_respirator.png");
   }

   public ResourceLocation getAnimationResource(AnthraliteGasMaskArmorItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "animations/anthralite_gas_mask.animation.json");
   }
}
