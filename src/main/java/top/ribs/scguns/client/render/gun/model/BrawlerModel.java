package top.ribs.scguns.client.render.gun.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import top.ribs.scguns.client.SpecialModels;
import top.ribs.scguns.client.render.gun.IOverrideModel;
import top.ribs.scguns.client.util.RenderUtil;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.init.ModItems;
import top.ribs.scguns.item.attachment.IAttachment;

public class BrawlerModel implements IOverrideModel {
   public BrawlerModel() {
      super();
   }

   @Override
   public void render(
      float partialTicks,
      ItemDisplayContext transformType,
      ItemStack stack,
      ItemStack parent,
      LivingEntity entity,
      PoseStack matrixStack,
      MultiBufferSource buffer,
      int light,
      int overlay
   ) {
      if (Gun.getScope(stack) == null) {
         RenderUtil.renderModel(SpecialModels.BRAWLER_SIGHTS.getModel(), stack, matrixStack, buffer, light, overlay);
      } else {
         RenderUtil.renderModel(SpecialModels.BRAWLER_NO_SIGHTS.getModel(), stack, matrixStack, buffer, light, overlay);
      }

      RenderUtil.renderModel(SpecialModels.BRAWLER_MAIN.getModel(), stack, matrixStack, buffer, light, overlay);
      this.renderBarrelAttachments(matrixStack, buffer, stack, light, overlay);
   }

   private void renderBarrelAttachments(PoseStack matrixStack, MultiBufferSource buffer, ItemStack stack, int light, int overlay) {
      boolean hasExtendedBarrel = false;
      if (Gun.hasAttachmentEquipped(stack, IAttachment.Type.BARREL)) {
         if (Gun.getAttachment(IAttachment.Type.BARREL, stack).getItem() == ModItems.EXTENDED_BARREL.get()) {
            RenderUtil.renderModel(SpecialModels.BRAWLER_EXT_BARREL.getModel(), stack, matrixStack, buffer, light, overlay);
            hasExtendedBarrel = true;
         } else if (Gun.getAttachment(IAttachment.Type.BARREL, stack).getItem() == ModItems.SILENCER.get()) {
            RenderUtil.renderModel(SpecialModels.BRAWLER_SILENCER.getModel(), stack, matrixStack, buffer, light, overlay);
         } else if (Gun.getAttachment(IAttachment.Type.BARREL, stack).getItem() == ModItems.MUZZLE_BRAKE.get()) {
            RenderUtil.renderModel(SpecialModels.BRAWLER_MUZZLE_BRAKE.getModel(), stack, matrixStack, buffer, light, overlay);
         } else if (Gun.getAttachment(IAttachment.Type.BARREL, stack).getItem() == ModItems.ADVANCED_SILENCER.get()) {
            RenderUtil.renderModel(SpecialModels.BRAWLER_ADVANCED_SILENCER.getModel(), stack, matrixStack, buffer, light, overlay);
         }
      }

      if (!hasExtendedBarrel) {
         RenderUtil.renderModel(SpecialModels.BRAWLER_STAN_BARREL.getModel(), stack, matrixStack, buffer, light, overlay);
      }
   }

   private double ease(double x) {
      return 1.0 - Math.pow(1.0 - 2.0 * x, 4.0);
   }
}
