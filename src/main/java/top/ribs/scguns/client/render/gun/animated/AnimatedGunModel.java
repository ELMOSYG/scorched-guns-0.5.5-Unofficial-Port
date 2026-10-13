package top.ribs.scguns.client.render.gun.animated;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import top.ribs.scguns.item.animated.AnimatedGunItem;

public class AnimatedGunModel extends DefaultedItemGeoModel<AnimatedGunItem> {
   private final String modelPath;
   private final java.util.Set<ResourceLocation> snapshotsSaved = new java.util.HashSet<>();
   public AnimatedGunModel(ResourceLocation path) {
      super(path);
      this.modelPath = path.getPath();
   }
   public ResourceLocation getModelResource(AnimatedGunItem gunItem) {
      return this.getGunModelResource();
   }

   public ResourceLocation getGunModelResource() {
      return ResourceLocation.fromNamespaceAndPath("scguns", "geo/item/gun/" + this.modelPath + ".geo.json");
   }

   /**
    * Records every bone's baked state the first time the model is baked.
    *
    * <p>GeckoLib can restore a bone to the way the model file left it, but only from the snapshot taken by
    * {@code saveInitialSnapshot}, and nothing in the mod ever called it. Baking is the one moment the bones
    * are guaranteed untouched by an animation, so the snapshot is taken here.</p>
    */
   @Override
   public BakedGeoModel getBakedModel(ResourceLocation location) {
      BakedGeoModel model = super.getBakedModel(location);
      if (model != null && this.snapshotsSaved.add(location)) {
         for (GeoBone bone : model.topLevelBones()) {
            saveInitialSnapshots(bone);
         }
      }
      return model;
   }

   private static void saveInitialSnapshots(GeoBone bone) {
      bone.saveInitialSnapshot();
      for (GeoBone child : bone.getChildBones()) {
         saveInitialSnapshots(child);
      }
   }

   public ResourceLocation getTextureResource(AnimatedGunItem gunItem) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "textures/animated/gun/" + this.modelPath + ".png");
   }

   public ResourceLocation getAnimationResource(AnimatedGunItem gunItem) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "animations/item/" + this.modelPath + ".animation.json");
   }
}
