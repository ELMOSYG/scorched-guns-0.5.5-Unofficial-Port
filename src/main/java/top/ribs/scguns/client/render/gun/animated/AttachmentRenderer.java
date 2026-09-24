package top.ribs.scguns.client.render.gun.animated;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import top.ribs.scguns.item.animated.AnimatedGunItem;

public class AttachmentRenderer extends GeoRenderLayer<AnimatedGunItem> {
   protected ItemStack currentItemStack;
   private static ResourceLocation model_resource = null;
   private static ResourceLocation texture_resource = null;
   private static ItemStack itemStack = null;

   public AttachmentRenderer(GeoRenderer entityRendererIn) {
      super(entityRendererIn);
   }

   public void updateAttachment(ItemStack attachmentStack) {
      itemStack = attachmentStack;
      // 0.5.5 built the path from Item#toString. 1.21 changed that to the full registry
      // name ("scguns:long_scope"), which is not a valid resource path, so the attachment
      // model and texture were never found. The working 1.21.1 port takes the namespace
      // and path from the registry key instead, which resolves to the same files 0.5.5
      // shipped (geo/item/attachment/<path>.geo.json, textures/animated/attachment/<path>.png).
      ResourceLocation attachmentId = BuiltInRegistries.ITEM.getKey(attachmentStack.getItem());
      String attachmentPath = attachmentId.getPath();
      model_resource = ResourceLocation.fromNamespaceAndPath(attachmentId.getNamespace(), "geo/item/attachment/" + attachmentPath + ".geo.json");
      texture_resource = ResourceLocation.fromNamespaceAndPath(attachmentId.getNamespace(), "textures/animated/attachment/" + attachmentPath + ".png");
   }

   public void renderForBone(
      PoseStack poseStack,
      AnimatedGunItem animatable,
      GeoBone bone,
      RenderType renderType,
      MultiBufferSource bufferSource,
      VertexConsumer buffer,
      float partialTick,
      int packedLight,
      int packedOverlay
   ) {
      if (model_resource != null && texture_resource != null && itemStack != null) {
         // 1.20.1: Item.toString() was the registry path. 1.21 returns "scguns:<path>",
         // which as a full-match regex never matches a bone name.
         ResourceLocation heldItemId = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
         String heldItemPath = heldItemId == null ? "" : heldItemId.getPath();
         AnimatedGunModel focusModel = new AnimatedGunModel(model_resource);
         ResourceLocation focusModelModelResource = model_resource;
         RenderType focusModelRenderLayer = RenderType.entityTranslucent(texture_resource);
         if (bone.getName().matches("attachment_bone") && !bone.getName().matches(heldItemPath) && itemStack != null) {
            this.getRenderer()
               .reRender(
                  focusModel.getBakedModel(focusModelModelResource),
                  poseStack,
                  bufferSource,
                  animatable,
                  focusModelRenderLayer,
                  bufferSource.getBuffer(focusModelRenderLayer),
                  partialTick,
                  packedLight,
                  OverlayTexture.NO_OVERLAY,
                  // GeckoLib 4.6+ takes one packed ARGB colour instead of four floats.
                  FastColor.ARGB32.colorFromFloat(1.0F, 1.0F, 1.0F, 1.0F)
               );
         }
      }
   }
}
