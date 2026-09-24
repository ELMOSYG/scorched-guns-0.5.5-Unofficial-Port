package top.ribs.scguns.client.render.gun.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import top.ribs.scguns.client.SpecialModels;
import top.ribs.scguns.client.render.gun.IOverrideModel;
import top.ribs.scguns.client.util.RenderUtil;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.init.ModItems;
import top.ribs.scguns.item.attachment.IAttachment;

public class Osgood50Model implements IOverrideModel {
   private static final int TOTAL_SHOTS = 7;
   private static final float ROTATION_INCREMENT = 51.42857F;
   private float currentRotation = 0.0F;
   private float targetRotation = 0.0F;

   public Osgood50Model() {
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
      RenderUtil.renderModel(SpecialModels.OSGOOD_50_MAIN.getModel(), stack, matrixStack, buffer, light, overlay);
      RenderUtil.renderModel(SpecialModels.OSGOOD_50_DRUM.getModel(), stack, matrixStack, buffer, light, overlay);
      this.renderStockAttachments(matrixStack, buffer, stack, light, overlay);
      this.renderBarrelAttachments(matrixStack, buffer, stack, light, overlay);
      if (entity.equals(Minecraft.getInstance().player)) {
         matrixStack.pushPose();
         matrixStack.translate(0.0, -0.3, 0.33);
         ItemCooldowns tracker = Minecraft.getInstance().player.getCooldowns();
         float cooldown = tracker.getCooldownPercent(stack.getItem(), Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
         cooldown = (float)this.ease((double)cooldown);
         float rotationAngle = -cooldown * 30.0F;
         matrixStack.mulPose(Axis.XP.rotationDegrees(rotationAngle));
         matrixStack.translate(0.0, 0.3, -0.33);
         RenderUtil.renderModel(SpecialModels.OSGOOD__50_HAMMER.getModel(), stack, matrixStack, buffer, light, overlay);
         matrixStack.popPose();
      }
   }

   private void renderStockAttachments(PoseStack matrixStack, MultiBufferSource buffer, ItemStack stack, int light, int overlay) {
      if (Gun.hasAttachmentEquipped(stack, IAttachment.Type.STOCK)) {
         if (Gun.getAttachment(IAttachment.Type.STOCK, stack).getItem() == ModItems.WOODEN_STOCK.get()) {
            RenderUtil.renderModel(SpecialModels.OSGOOD_50_STOCK_WOODEN.getModel(), stack, matrixStack, buffer, light, overlay);
         } else if (Gun.getAttachment(IAttachment.Type.STOCK, stack).getItem() == ModItems.LIGHT_STOCK.get()) {
            RenderUtil.renderModel(SpecialModels.OSGOOD_50_STOCK_LIGHT.getModel(), stack, matrixStack, buffer, light, overlay);
         } else if (Gun.getAttachment(IAttachment.Type.STOCK, stack).getItem() == ModItems.WEIGHTED_STOCK.get()) {
            RenderUtil.renderModel(SpecialModels.OSGOOD_50_STOCK_HEAVY.getModel(), stack, matrixStack, buffer, light, overlay);
         } else if (Gun.getAttachment(IAttachment.Type.STOCK, stack).getItem() == ModItems.BUMP_STOCK.get()) {
            RenderUtil.renderModel(SpecialModels.OSGOOD_50_STOCK_HEAVY.getModel(), stack, matrixStack, buffer, light, overlay);
         }
      }
   }

   private void renderBarrelAttachments(PoseStack matrixStack, MultiBufferSource buffer, ItemStack stack, int light, int overlay) {
      boolean hasExtendedBarrel = false;
      if (Gun.hasAttachmentEquipped(stack, IAttachment.Type.BARREL)) {
         if (Gun.getAttachment(IAttachment.Type.BARREL, stack).getItem() == ModItems.EXTENDED_BARREL.get()) {
            RenderUtil.renderModel(SpecialModels.OSGOOD_50_EXT_BARREL.getModel(), stack, matrixStack, buffer, light, overlay);
            hasExtendedBarrel = true;
         } else if (Gun.getAttachment(IAttachment.Type.BARREL, stack).getItem() == ModItems.SILENCER.get()) {
            RenderUtil.renderModel(SpecialModels.OSGOOD_50_SILENCER.getModel(), stack, matrixStack, buffer, light, overlay);
         } else if (Gun.getAttachment(IAttachment.Type.BARREL, stack).getItem() == ModItems.MUZZLE_BRAKE.get()) {
            RenderUtil.renderModel(SpecialModels.OSGOOD_50_MUZZLE_BRAKE.getModel(), stack, matrixStack, buffer, light, overlay);
         } else if (Gun.getAttachment(IAttachment.Type.BARREL, stack).getItem() == ModItems.ADVANCED_SILENCER.get()) {
            RenderUtil.renderModel(SpecialModels.OSGOOD_50_ADVANCED_SILENCER.getModel(), stack, matrixStack, buffer, light, overlay);
         }
      }

      if (!hasExtendedBarrel) {
         RenderUtil.renderModel(SpecialModels.OSGOOD_50_STAN_BARREL.getModel(), stack, matrixStack, buffer, light, overlay);
      }
   }

   private double ease(double x) {
      return 1.0 - Math.pow(1.0 - 2.0 * x, 4.0);
   }
}
