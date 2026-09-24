package top.ribs.scguns.client.render.gun.animated;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import top.ribs.scguns.item.animated.AnimatedGunItem;

public class AnimatedGunModel extends DefaultedItemGeoModel<AnimatedGunItem> {
   private final String modelPath;

   public AnimatedGunModel(ResourceLocation path) {
      super(path);
      this.modelPath = path.getPath();
   }

   public ResourceLocation getModelResource(AnimatedGunItem gunItem) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "geo/item/gun/" + this.modelPath + ".geo.json");
   }

   public ResourceLocation getTextureResource(AnimatedGunItem gunItem) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "textures/animated/gun/" + this.modelPath + ".png");
   }

   public ResourceLocation getAnimationResource(AnimatedGunItem gunItem) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "animations/item/" + this.modelPath + ".animation.json");
   }
}
