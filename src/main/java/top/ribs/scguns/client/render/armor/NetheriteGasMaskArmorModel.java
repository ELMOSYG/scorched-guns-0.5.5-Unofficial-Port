package top.ribs.scguns.client.render.armor;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import top.ribs.scguns.item.animated.NetheriteGasMaskArmorItem;

public class NetheriteGasMaskArmorModel extends GeoModel<NetheriteGasMaskArmorItem> {
   public NetheriteGasMaskArmorModel() {
      super();
   }

   public ResourceLocation getModelResource(NetheriteGasMaskArmorItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "geo/netherite_respirator.geo.json");
   }

   public ResourceLocation getTextureResource(NetheriteGasMaskArmorItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "textures/armor/netherite_respirator.png");
   }

   public ResourceLocation getAnimationResource(NetheriteGasMaskArmorItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "animations/netherite_gas_mask.animation.json");
   }
}
