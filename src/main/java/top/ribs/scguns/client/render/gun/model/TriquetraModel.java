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

public class TriquetraModel implements IOverrideModel {
   public TriquetraModel() {
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
      RenderUtil.renderModel(SpecialModels.TRIQUETRA_MAIN.getModel(), stack, matrixStack, buffer, light, overlay);
      if (Gun.getScope(stack) == null) {
         RenderUtil.renderModel(SpecialModels.TRIQUETRA_SIGHTS.getModel(), stack, matrixStack, buffer, light, overlay);
      } else {
         RenderUtil.renderModel(SpecialModels.TRIQUETRA_NO_SIGHTS.getModel(), stack, matrixStack, buffer, light, overlay);
      }

      if (Gun.hasAttachmentEquipped(stack, IAttachment.Type.STOCK)) {
         if (Gun.getAttachment(IAttachment.Type.STOCK, stack).getItem() == ModItems.WEIGHTED_STOCK.get()) {
            RenderUtil.renderModel(SpecialModels.TRIQUETRA_WEIGHTED_STOCK.getModel(), stack, matrixStack, buffer, light, overlay);
         }

         if (Gun.getAttachment(IAttachment.Type.STOCK, stack).getItem() == ModItems.LIGHT_STOCK.get()) {
            RenderUtil.renderModel(SpecialModels.TRIQUETRA_LIGHT_STOCK.getModel(), stack, matrixStack, buffer, light, overlay);
         }

         if (Gun.getAttachment(IAttachment.Type.STOCK, stack).getItem() == ModItems.WOODEN_STOCK.get()) {
            RenderUtil.renderModel(SpecialModels.TRIQUETRA_WOODEN_STOCK.getModel(), stack, matrixStack, buffer, light, overlay);
         }

         if (Gun.getAttachment(IAttachment.Type.STOCK, stack).getItem() == ModItems.BUMP_STOCK.get()) {
            RenderUtil.renderModel(SpecialModels.TRIQUETRA_WEIGHTED_STOCK.getModel(), stack, matrixStack, buffer, light, overlay);
         }
      } else {
         RenderUtil.renderModel(SpecialModels.TRIQUETRA_STAN_GRIP.getModel(), stack, matrixStack, buffer, light, overlay);
      }

      float magazinePosition = this.calculateMagazinePosition(stack);
      float translationMultiplier = 0.25F;
      matrixStack.pushPose();
      matrixStack.translate(this.clampMagazinePosition(magazinePosition * translationMultiplier), 0.0F, 0.0F);
      RenderUtil.renderModel(SpecialModels.TRIQUETRA_STAN_MAG.getModel(), stack, matrixStack, buffer, light, overlay);
      matrixStack.popPose();
   }

   private double ease(double x) {
      return 1.0 - Math.pow(1.0 - 2.0 * x, 4.0);
   }

   private float calculateMagazinePosition(ItemStack stack) {
      int maxAmmo = Gun.getMaxAmmo(stack);
      int currentAmmo = Gun.getAmmoCount(stack);
      return Math.min((float)(maxAmmo - currentAmmo) / (float)maxAmmo, 1.0F);
   }

   private float clampMagazinePosition(float position) {
      return Math.max(0.0F, Math.min(position, 1.0F));
   }
}
