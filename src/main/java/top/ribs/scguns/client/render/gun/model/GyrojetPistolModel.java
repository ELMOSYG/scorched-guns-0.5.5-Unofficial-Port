package top.ribs.scguns.client.render.gun.model;

import com.mojang.blaze3d.vertex.PoseStack;
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

public class GyrojetPistolModel implements IOverrideModel {
   public GyrojetPistolModel() {
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
      RenderUtil.renderModel(SpecialModels.GYROJET_PISTOL_MAIN.getModel(), stack, matrixStack, buffer, light, overlay);
      if (Gun.hasAttachmentEquipped(stack, IAttachment.Type.STOCK)) {
         if (Gun.getAttachment(IAttachment.Type.STOCK, stack).getItem() == ModItems.WOODEN_STOCK.get()) {
            RenderUtil.renderModel(SpecialModels.GYROJET_PISTOL_STOCK_WOODEN.getModel(), stack, matrixStack, buffer, light, overlay);
         } else if (Gun.getAttachment(IAttachment.Type.STOCK, stack).getItem() == ModItems.LIGHT_STOCK.get()) {
            RenderUtil.renderModel(SpecialModels.GYROJET_PISTOL_STOCK_LIGHT.getModel(), stack, matrixStack, buffer, light, overlay);
         } else if (Gun.getAttachment(IAttachment.Type.STOCK, stack).getItem() == ModItems.WEIGHTED_STOCK.get()) {
            RenderUtil.renderModel(SpecialModels.GYROJET_PISTOL_STOCK_HEAVY.getModel(), stack, matrixStack, buffer, light, overlay);
         } else if (Gun.getAttachment(IAttachment.Type.STOCK, stack).getItem() == ModItems.BUMP_STOCK.get()) {
            RenderUtil.renderModel(SpecialModels.GYROJET_PISTOL_STOCK_HEAVY.getModel(), stack, matrixStack, buffer, light, overlay);
         }
      }

      if (Gun.hasAttachmentEquipped(stack, IAttachment.Type.MAGAZINE)) {
         if (Gun.getAttachment(IAttachment.Type.MAGAZINE, stack).getItem() == ModItems.EXTENDED_MAG.get()) {
            RenderUtil.renderModel(SpecialModels.GYROJET_PISTOL_EXT_MAG.getModel(), stack, matrixStack, buffer, light, overlay);
         }

         if (Gun.getAttachment(IAttachment.Type.MAGAZINE, stack).getItem() == ModItems.PLUS_P_MAG.get()) {
            RenderUtil.renderModel(SpecialModels.GYROJET_PISTOL_EXT_MAG.getModel(), stack, matrixStack, buffer, light, overlay);
         }

         if (Gun.getAttachment(IAttachment.Type.MAGAZINE, stack).getItem() == ModItems.SPEED_MAG.get()) {
            RenderUtil.renderModel(SpecialModels.GYROJET_PISTOL_SPEED_MAG.getModel(), stack, matrixStack, buffer, light, overlay);
         }
      } else {
         RenderUtil.renderModel(SpecialModels.GYROJET_PISTOL_STAN_MAG.getModel(), stack, matrixStack, buffer, light, overlay);
      }

      if (entity.equals(Minecraft.getInstance().player)) {
         this.renderFlame(matrixStack, buffer, stack, light, overlay, true);
         this.renderFlame(matrixStack, buffer, stack, light, overlay, false);
      }
   }

   private void renderFlame(PoseStack matrixStack, MultiBufferSource buffer, ItemStack stack, int light, int overlay, boolean isLeft) {
      matrixStack.pushPose();
      matrixStack.translate(0.0, -0.3625, 0.0);
      ItemCooldowns tracker = Minecraft.getInstance().player.getCooldowns();
      float cooldown = tracker.getCooldownPercent(stack.getItem(), Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
      cooldown = (float)this.ease((double)cooldown);
      float scale = cooldown > 0.0F ? 1.0F : 0.0F;
      matrixStack.scale(scale, scale, scale);
      matrixStack.translate(cooldown / -26.0F, 0.0F, 0.0F);
      matrixStack.translate(0.0, 0.3625, 0.0);
      if (isLeft) {
         RenderUtil.renderModel(SpecialModels.GYROJET_PISTOL_FLAME_LEFT.getModel(), stack, matrixStack, buffer, light, overlay);
      } else {
         matrixStack.translate(-cooldown / -13.0F, 0.0F, 0.0F);
         RenderUtil.renderModel(SpecialModels.GYROJET_PISTOL_FLAME_RIGHT.getModel(), stack, matrixStack, buffer, light, overlay);
      }

      matrixStack.popPose();
   }

   private double ease(double x) {
      return 1.0 - Math.pow(1.0 - 2.0 * x, 4.0);
   }
}
