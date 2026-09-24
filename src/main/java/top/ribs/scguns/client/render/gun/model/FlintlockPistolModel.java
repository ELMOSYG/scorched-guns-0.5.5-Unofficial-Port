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

public class FlintlockPistolModel implements IOverrideModel {
   private static final int FLASH_DURATION = 10;
   private int flashTimer = 0;

   public FlintlockPistolModel() {
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
      RenderUtil.renderModel(SpecialModels.FLINTLOCK_PISTOL_MAIN.getModel(), stack, matrixStack, buffer, light, overlay);
      if (Gun.hasAttachmentEquipped(stack, IAttachment.Type.STOCK)) {
         if (Gun.getAttachment(IAttachment.Type.STOCK, stack).getItem() == ModItems.WEIGHTED_STOCK.get()) {
            RenderUtil.renderModel(SpecialModels.FLINTLOCK_PISTOL_STOCK_WEIGHTED.getModel(), stack, matrixStack, buffer, light, overlay);
         }

         if (Gun.getAttachment(IAttachment.Type.STOCK, stack).getItem() == ModItems.LIGHT_STOCK.get()) {
            RenderUtil.renderModel(SpecialModels.FLINTLOCK_PISTOL_STOCK_LIGHT.getModel(), stack, matrixStack, buffer, light, overlay);
         }

         if (Gun.getAttachment(IAttachment.Type.STOCK, stack).getItem() == ModItems.WOODEN_STOCK.get()) {
            RenderUtil.renderModel(SpecialModels.FLINTLOCK_PISTOL_STOCK_WOODEN.getModel(), stack, matrixStack, buffer, light, overlay);
         }

         if (Gun.getAttachment(IAttachment.Type.STOCK, stack).getItem() == ModItems.BUMP_STOCK.get()) {
            RenderUtil.renderModel(SpecialModels.FLINTLOCK_PISTOL_STOCK_WEIGHTED.getModel(), stack, matrixStack, buffer, light, overlay);
         }
      }

      if (entity.equals(Minecraft.getInstance().player)) {
         matrixStack.pushPose();
         matrixStack.translate(0.0, -0.5, 0.23);
         ItemCooldowns tracker = Minecraft.getInstance().player.getCooldowns();
         float cooldown = tracker.getCooldownPercent(stack.getItem(), Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
         cooldown = (float)this.ease((double)cooldown);
         float rotationAngle = -cooldown * 45.0F;
         matrixStack.mulPose(Axis.XP.rotationDegrees(rotationAngle));
         matrixStack.translate(0.0, 0.5, -0.23);
         RenderUtil.renderModel(SpecialModels.FLINTLOCK_PISTOL_HAMMER.getModel(), stack, matrixStack, buffer, light, overlay);
         matrixStack.popPose();
         if (cooldown >= 0.9F) {
            this.flashTimer = 10;
         }
      }

      if (this.flashTimer > 0) {
         matrixStack.pushPose();
         matrixStack.translate(0.0, -0.0, -0.23);
         RenderUtil.renderModel(SpecialModels.MUSKET_FLASH.getModel(), stack, matrixStack, buffer, light, overlay);
         matrixStack.popPose();
         this.flashTimer--;
      }
   }

   private double ease(double x) {
      return 1.0 - Math.pow(1.0 - x, 4.0);
   }
}
