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

public class FencerCarabineModel implements IOverrideModel {
   public FencerCarabineModel() {
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
      RenderUtil.renderModel(SpecialModels.FENCER_CARABINE_MAIN.getModel(), stack, matrixStack, buffer, light, overlay);
      if (Gun.hasAttachmentEquipped(stack, IAttachment.Type.UNDER_BARREL)) {
         if (Gun.getAttachment(IAttachment.Type.UNDER_BARREL, stack).getItem() == ModItems.VERTICAL_GRIP.get()) {
            RenderUtil.renderModel(SpecialModels.FENCER_CARABINE_TACT_GRIP.getModel(), stack, matrixStack, buffer, light, overlay);
         } else if (Gun.getAttachment(IAttachment.Type.UNDER_BARREL, stack).getItem() == ModItems.LIGHT_GRIP.get()) {
            RenderUtil.renderModel(SpecialModels.FENCER_CARABINE_LIGHT_GRIP.getModel(), stack, matrixStack, buffer, light, overlay);
         } else if (Gun.getAttachment(IAttachment.Type.UNDER_BARREL, stack).getItem() == ModItems.IRON_BAYONET.get()) {
            RenderUtil.renderModel(SpecialModels.FENCER_CARABINE_IRON_BAYONET.getModel(), stack, matrixStack, buffer, light, overlay);
         } else if (Gun.getAttachment(IAttachment.Type.UNDER_BARREL, stack).getItem() == ModItems.ANTHRALITE_BAYONET.get()) {
            RenderUtil.renderModel(SpecialModels.FENCER_CARABINE_ANTHRALITE_BAYONET.getModel(), stack, matrixStack, buffer, light, overlay);
         } else if (Gun.getAttachment(IAttachment.Type.UNDER_BARREL, stack).getItem() == ModItems.DIAMOND_BAYONET.get()) {
            RenderUtil.renderModel(SpecialModels.FENCER_CARABINE_DIAMOND_BAYONET.getModel(), stack, matrixStack, buffer, light, overlay);
         } else if (Gun.getAttachment(IAttachment.Type.UNDER_BARREL, stack).getItem() == ModItems.NETHERITE_BAYONET.get()) {
            RenderUtil.renderModel(SpecialModels.FENCER_CARABINE_NETHERITE_BAYONET.getModel(), stack, matrixStack, buffer, light, overlay);
         }
      }

      if (entity.equals(Minecraft.getInstance().player)) {
         matrixStack.pushPose();
         matrixStack.translate(0.0, -0.5, 0.5);
         ItemCooldowns tracker = Minecraft.getInstance().player.getCooldowns();
         float cooldown = tracker.getCooldownPercent(stack.getItem(), Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
         cooldown = (float)this.ease((double)cooldown);
         float rotationAngle = -cooldown * 15.0F;
         matrixStack.mulPose(Axis.XP.rotationDegrees(rotationAngle));
         matrixStack.translate(0.0, 0.5, -0.5);
         RenderUtil.renderModel(SpecialModels.FENCER_CARABINE_HAMMER.getModel(), stack, matrixStack, buffer, light, overlay);
         matrixStack.popPose();
      }
   }

   private double ease(double x) {
      return 1.0 - Math.pow(1.0 - 2.0 * x, 4.0);
   }
}
