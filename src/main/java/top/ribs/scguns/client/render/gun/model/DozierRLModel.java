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

public class DozierRLModel implements IOverrideModel {
   private static final float ROTATION_INCREMENT = 90.0F;
   private int shotCount = 0;

   public DozierRLModel() {
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
      RenderUtil.renderModel(SpecialModels.DOZIER_RL_MAIN.getModel(), stack, matrixStack, buffer, light, overlay);
      if (Gun.getScope(stack) == null) {
         RenderUtil.renderModel(SpecialModels.DOZIER_RL_SIGHTS.getModel(), stack, matrixStack, buffer, light, overlay);
      } else {
         RenderUtil.renderModel(SpecialModels.DOZIER_RL_NO_SIGHTS.getModel(), stack, matrixStack, buffer, light, overlay);
      }

      if (Gun.hasAttachmentEquipped(stack, IAttachment.Type.UNDER_BARREL)) {
         if (Gun.getAttachment(IAttachment.Type.UNDER_BARREL, stack).getItem() == ModItems.VERTICAL_GRIP.get()) {
            RenderUtil.renderModel(SpecialModels.DOZIER_RL_GRIP_VERTICAL.getModel(), stack, matrixStack, buffer, light, overlay);
         } else if (Gun.getAttachment(IAttachment.Type.UNDER_BARREL, stack).getItem() == ModItems.LIGHT_GRIP.get()) {
            RenderUtil.renderModel(SpecialModels.DOZIER_RL_GRIP_LIGHT.getModel(), stack, matrixStack, buffer, light, overlay);
         } else if (Gun.getAttachment(IAttachment.Type.UNDER_BARREL, stack).getItem() == ModItems.IRON_BAYONET.get()) {
            RenderUtil.renderModel(SpecialModels.DOZIER_RL_IRON_BAYONET.getModel(), stack, matrixStack, buffer, light, overlay);
         } else if (Gun.getAttachment(IAttachment.Type.UNDER_BARREL, stack).getItem() == ModItems.ANTHRALITE_BAYONET.get()) {
            RenderUtil.renderModel(SpecialModels.DOZIER_RL_ANTHRALITE_BAYONET.getModel(), stack, matrixStack, buffer, light, overlay);
         } else if (Gun.getAttachment(IAttachment.Type.UNDER_BARREL, stack).getItem() == ModItems.DIAMOND_BAYONET.get()) {
            RenderUtil.renderModel(SpecialModels.DOZIER_RL_DIAMOND_BAYONET.getModel(), stack, matrixStack, buffer, light, overlay);
         } else if (Gun.getAttachment(IAttachment.Type.UNDER_BARREL, stack).getItem() == ModItems.NETHERITE_BAYONET.get()) {
            RenderUtil.renderModel(SpecialModels.DOZIER_RL_NETHERITE_BAYONET.getModel(), stack, matrixStack, buffer, light, overlay);
         }
      }

      if (entity.equals(Minecraft.getInstance().player)) {
         this.renderBoltAndMagazine(matrixStack, buffer, stack, light, overlay);
         this.renderFlame(matrixStack, buffer, stack, light, overlay);
      }
   }

   private void renderBoltAndMagazine(PoseStack matrixStack, MultiBufferSource buffer, ItemStack stack, int light, int overlay) {
      assert Minecraft.getInstance().player != null;

      ItemCooldowns tracker = Minecraft.getInstance().player.getCooldowns();
      float cooldown = tracker.getCooldownPercent(stack.getItem(), Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
      matrixStack.pushPose();
      matrixStack.translate(0.0, -0.3625, 0.0);
      if (cooldown > 0.0F) {
         this.shotCount = (this.shotCount + 1) % 4;
      }

      matrixStack.translate(0.0, 0.3625, 0.0);
      matrixStack.popPose();
      this.renderMagazineRotation(matrixStack, buffer, stack, light, overlay);
   }

   private void renderMagazineRotation(PoseStack matrixStack, MultiBufferSource buffer, ItemStack stack, int light, int overlay) {
      matrixStack.pushPose();
      matrixStack.translate(0.0, -0.0, 0.0);
      float currentRotation = (float)this.shotCount * 90.0F;
      matrixStack.mulPose(Axis.ZP.rotationDegrees(currentRotation));
      matrixStack.translate(0.0, 0.0, 0.0);
      RenderUtil.renderModel(SpecialModels.DOZIER_RL_DRUM.getModel(), stack, matrixStack, buffer, light, overlay);
      matrixStack.popPose();
   }

   private void renderFlame(PoseStack matrixStack, MultiBufferSource buffer, ItemStack stack, int light, int overlay) {
      matrixStack.pushPose();
      matrixStack.translate(0.0, -0.3625, 0.0);
      ItemCooldowns tracker = Minecraft.getInstance().player.getCooldowns();
      float cooldown = tracker.getCooldownPercent(stack.getItem(), Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
      cooldown = (float)this.ease((double)cooldown);
      float scale = cooldown > 0.0F ? 1.0F : 0.0F;
      matrixStack.scale(scale, scale, scale);
      matrixStack.translate(0.0F, 0.0F, cooldown / 8.0F);
      matrixStack.translate(0.0, 0.3625, 0.0);
      RenderUtil.renderModel(SpecialModels.DOZIER_RL_FIRE.getModel(), stack, matrixStack, buffer, light, overlay);
      matrixStack.popPose();
   }

   private double ease(double x) {
      return 1.0 - Math.pow(1.0 - 2.0 * x, 4.0);
   }
}
