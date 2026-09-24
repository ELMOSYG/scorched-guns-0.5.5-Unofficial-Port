package top.ribs.scguns.client.render.curios;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public class AmmoBoxRenderer implements ICurioRenderer {
   public AmmoBoxRenderer() {
      super();
   }

   public <T extends LivingEntity, M extends EntityModel<T>> void render(
      ItemStack stack,
      SlotContext slotContext,
      PoseStack matrixStack,
      RenderLayerParent<T, M> renderLayerParent,
      MultiBufferSource bufferSource,
      int light,
      float limbSwing,
      float limbSwingAmount,
      float partialTicks,
      float ageInTicks,
      float netHeadYaw,
      float headPitch
   ) {
      matrixStack.pushPose();
      if (slotContext.index() == 0) {
         matrixStack.translate(-0.18, 0.55, 0.17);
         matrixStack.mulPose(new Quaternionf().rotationX(0.0F));
         matrixStack.mulPose(new Quaternionf().rotationY((float)Math.toRadians(90.0)));
         matrixStack.mulPose(new Quaternionf().rotationZ((float)Math.toRadians(90.0)));
      } else if (slotContext.index() == 1) {
         matrixStack.translate(0.18, 0.55, 0.17);
         matrixStack.mulPose(new Quaternionf().rotationX(0.0F));
         matrixStack.mulPose(new Quaternionf().rotationY((float)Math.toRadians(90.0)));
         matrixStack.mulPose(new Quaternionf().rotationZ((float)Math.toRadians(90.0)));
      }

      matrixStack.scale(0.8F, 0.8F, 0.8F);
      ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
      BakedModel bakedModel = itemRenderer.getModel(stack, null, null, 0);
      itemRenderer.render(stack, ItemDisplayContext.GROUND, false, matrixStack, bufferSource, light, OverlayTexture.NO_OVERLAY, bakedModel);
      matrixStack.popPose();
   }
}
