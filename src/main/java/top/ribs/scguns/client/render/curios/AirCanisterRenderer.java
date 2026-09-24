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

public class AirCanisterRenderer implements ICurioRenderer {
   public AirCanisterRenderer() {
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
      matrixStack.translate(0.0, 0.3, 0.2);
      matrixStack.mulPose(new Quaternionf().rotationX((float)Math.toRadians(0.0)));
      matrixStack.mulPose(new Quaternionf().rotationY((float)Math.toRadians(180.0)));
      matrixStack.scale(1.2F, 1.2F, 1.2F);
      ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
      BakedModel bakedModel = itemRenderer.getModel(stack, null, null, 0);
      itemRenderer.render(stack, ItemDisplayContext.FIXED, false, matrixStack, bufferSource, light, OverlayTexture.NO_OVERLAY, bakedModel);
      matrixStack.popPose();
   }
}
