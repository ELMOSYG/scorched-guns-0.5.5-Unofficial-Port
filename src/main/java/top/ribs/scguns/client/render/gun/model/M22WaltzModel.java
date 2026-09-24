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

public class M22WaltzModel implements IOverrideModel {
   public M22WaltzModel() {
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
         RenderUtil.renderModel(SpecialModels.M22_WALTZ_SIGHTS.getModel(), stack, matrixStack, buffer, light, overlay);
      } else {
         RenderUtil.renderModel(SpecialModels.M22_WALTZ_NO_SIGHTS.getModel(), stack, matrixStack, buffer, light, overlay);
      }

      RenderUtil.renderModel(SpecialModels.M22_WALTZ_MAIN.getModel(), stack, matrixStack, buffer, light, overlay);
      this.renderBarrelAndAttachments(stack, matrixStack, buffer, light, overlay);
      this.renderMagazineAttachments(stack, matrixStack, buffer, light, overlay);
      if (entity.equals(Minecraft.getInstance().player)) {
         matrixStack.pushPose();
         matrixStack.translate(0.0, -0.3625, 0.0);
         ItemCooldowns tracker = Minecraft.getInstance().player.getCooldowns();
         float cooldown = tracker.getCooldownPercent(stack.getItem(), Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
         cooldown = (float)this.ease((double)cooldown);
         matrixStack.translate(0.0F, 0.0F, cooldown / 8.0F);
         matrixStack.translate(0.0, 0.3625, 0.0);
         RenderUtil.renderModel(SpecialModels.M22_WALTZ_RECEIVER.getModel(), stack, matrixStack, buffer, light, overlay);
         matrixStack.popPose();
      }
   }

   private void renderBarrelAndAttachments(ItemStack stack, PoseStack matrixStack, MultiBufferSource buffer, int light, int overlay) {
      boolean hasExtendedBarrel = false;
      if (Gun.hasAttachmentEquipped(stack, IAttachment.Type.BARREL)) {
         if (Gun.getAttachment(IAttachment.Type.BARREL, stack).getItem() == ModItems.EXTENDED_BARREL.get()) {
            RenderUtil.renderModel(SpecialModels.M22_WALTZ_EXT_BARREL.getModel(), stack, matrixStack, buffer, light, overlay);
            hasExtendedBarrel = true;
         } else if (Gun.getAttachment(IAttachment.Type.BARREL, stack).getItem() == ModItems.SILENCER.get()) {
            RenderUtil.renderModel(SpecialModels.M22_WALTZ_SILENCER.getModel(), stack, matrixStack, buffer, light, overlay);
         } else if (Gun.getAttachment(IAttachment.Type.BARREL, stack).getItem() == ModItems.ADVANCED_SILENCER.get()) {
            RenderUtil.renderModel(SpecialModels.M22_WALTZ_ADVANCED_SILENCER.getModel(), stack, matrixStack, buffer, light, overlay);
         } else if (Gun.getAttachment(IAttachment.Type.BARREL, stack).getItem() == ModItems.MUZZLE_BRAKE.get()) {
            RenderUtil.renderModel(SpecialModels.M22_WALTZ_MUZZLE_BRAKE.getModel(), stack, matrixStack, buffer, light, overlay);
         }
      }

      if (!hasExtendedBarrel) {
         RenderUtil.renderModel(SpecialModels.M22_WALTZ_STAN_BARREL.getModel(), stack, matrixStack, buffer, light, overlay);
      }
   }

   private void renderMagazineAttachments(ItemStack stack, PoseStack matrixStack, MultiBufferSource buffer, int light, int overlay) {
      if (Gun.hasAttachmentEquipped(stack, IAttachment.Type.MAGAZINE)) {
         if (Gun.getAttachment(IAttachment.Type.MAGAZINE, stack).getItem() == ModItems.EXTENDED_MAG.get()) {
            RenderUtil.renderModel(SpecialModels.M22_WALTZ_EXTENDED_MAG.getModel(), stack, matrixStack, buffer, light, overlay);
         } else if (Gun.getAttachment(IAttachment.Type.MAGAZINE, stack).getItem() == ModItems.SPEED_MAG.get()) {
            RenderUtil.renderModel(SpecialModels.M22_WALTZ_STANDARD_MAG.getModel(), stack, matrixStack, buffer, light, overlay);
         } else if (Gun.getAttachment(IAttachment.Type.MAGAZINE, stack).getItem() == ModItems.PLUS_P_MAG.get()) {
            RenderUtil.renderModel(SpecialModels.M22_WALTZ_EXTENDED_MAG.getModel(), stack, matrixStack, buffer, light, overlay);
         }
      } else {
         RenderUtil.renderModel(SpecialModels.M22_WALTZ_STANDARD_MAG.getModel(), stack, matrixStack, buffer, light, overlay);
      }
   }

   private double ease(double x) {
      return 1.0 - Math.pow(1.0 - 2.0 * x, 4.0);
   }
}
