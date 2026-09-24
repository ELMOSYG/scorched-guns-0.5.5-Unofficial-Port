package top.ribs.scguns.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.IllagerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.VindicatorRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.monster.Vindicator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import top.ribs.scguns.item.GunItem;

@Mixin({VindicatorRenderer.class})
public abstract class MixinVindicatorRenderer {
   public MixinVindicatorRenderer() {
      super();
   }

   @Redirect(
      method = {"<init>(Lnet/minecraft/client/renderer/entity/EntityRendererProvider$Context;)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/entity/VindicatorRenderer;addLayer(Lnet/minecraft/client/renderer/entity/layers/RenderLayer;)Z"
      )
   )
   private boolean replaceItemLayer(VindicatorRenderer instance, RenderLayer layer, Context context) {
      return layer instanceof ItemInHandLayer
         ? instance.addLayer(
            new ItemInHandLayer<Vindicator, IllagerModel<Vindicator>>(instance, context.getItemInHandRenderer()) {
               public void render(
                  PoseStack poseStack,
                  MultiBufferSource buffer,
                  int packedLight,
                  Vindicator vindicator,
                  float limbSwing,
                  float limbSwingAmount,
                  float partialTicks,
                  float ageInTicks,
                  float netHeadYaw,
                  float headPitch
               ) {
                  if (vindicator.getMainHandItem().getItem() instanceof GunItem) {
                     super.render(poseStack, buffer, packedLight, vindicator, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch);
                  } else if (vindicator.isAggressive()) {
                     super.render(poseStack, buffer, packedLight, vindicator, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch);
                  }
               }
            }
         )
         : instance.addLayer(layer);
   }
}
