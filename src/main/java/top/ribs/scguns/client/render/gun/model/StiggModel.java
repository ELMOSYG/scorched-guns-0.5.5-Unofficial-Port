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

public class StiggModel implements IOverrideModel {
   public StiggModel() {
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
      RenderUtil.renderModel(SpecialModels.STIGG_MAIN.getModel(), stack, matrixStack, buffer, light, overlay);
      if (Gun.getScope(stack) == null) {
         RenderUtil.renderModel(SpecialModels.STIGG_SIGHTS.getModel(), stack, matrixStack, buffer, light, overlay);
      } else {
         RenderUtil.renderModel(SpecialModels.STIGG_NO_SIGHTS.getModel(), stack, matrixStack, buffer, light, overlay);
      }

      boolean extendedBarrelAttached = false;
      if (Gun.hasAttachmentEquipped(stack, IAttachment.Type.BARREL)) {
         if (Gun.getAttachment(IAttachment.Type.BARREL, stack).getItem() == ModItems.EXTENDED_BARREL.get()) {
            RenderUtil.renderModel(SpecialModels.STIGG_EXT_BARREL.getModel(), stack, matrixStack, buffer, light, overlay);
            extendedBarrelAttached = true;
         } else if (Gun.getAttachment(IAttachment.Type.BARREL, stack).getItem() == ModItems.SILENCER.get()) {
            RenderUtil.renderModel(SpecialModels.STIGG_SILENCER.getModel(), stack, matrixStack, buffer, light, overlay);
         } else if (Gun.getAttachment(IAttachment.Type.BARREL, stack).getItem() == ModItems.MUZZLE_BRAKE.get()) {
            RenderUtil.renderModel(SpecialModels.STIGG_MUZZLE_BRAKE.getModel(), stack, matrixStack, buffer, light, overlay);
         } else if (Gun.getAttachment(IAttachment.Type.BARREL, stack).getItem() == ModItems.ADVANCED_SILENCER.get()) {
            RenderUtil.renderModel(SpecialModels.STIGG_ADVANCED_SILENCER.getModel(), stack, matrixStack, buffer, light, overlay);
         }
      }

      if (!extendedBarrelAttached) {
         RenderUtil.renderModel(SpecialModels.STIGG_STAN_BARREL.getModel(), stack, matrixStack, buffer, light, overlay);
      }

      if (Gun.hasAttachmentEquipped(stack, IAttachment.Type.STOCK)) {
         if (Gun.getAttachment(IAttachment.Type.STOCK, stack).getItem() == ModItems.WOODEN_STOCK.get()) {
            RenderUtil.renderModel(SpecialModels.STIGG_STOCK_WOODEN.getModel(), stack, matrixStack, buffer, light, overlay);
         } else if (Gun.getAttachment(IAttachment.Type.STOCK, stack).getItem() == ModItems.LIGHT_STOCK.get()) {
            RenderUtil.renderModel(SpecialModels.STIGG_STOCK_LIGHT.getModel(), stack, matrixStack, buffer, light, overlay);
         } else if (Gun.getAttachment(IAttachment.Type.STOCK, stack).getItem() == ModItems.WEIGHTED_STOCK.get()) {
            RenderUtil.renderModel(SpecialModels.STIGG_STOCK_HEAVY.getModel(), stack, matrixStack, buffer, light, overlay);
         } else if (Gun.getAttachment(IAttachment.Type.STOCK, stack).getItem() == ModItems.BUMP_STOCK.get()) {
            RenderUtil.renderModel(SpecialModels.STIGG_STOCK_HEAVY.getModel(), stack, matrixStack, buffer, light, overlay);
         }
      }

      if (Gun.hasAttachmentEquipped(stack, IAttachment.Type.UNDER_BARREL)) {
         if (Gun.getAttachment(IAttachment.Type.UNDER_BARREL, stack).getItem() == ModItems.VERTICAL_GRIP.get()) {
            RenderUtil.renderModel(SpecialModels.STIGG_GRIP_VERTICAL.getModel(), stack, matrixStack, buffer, light, overlay);
         } else if (Gun.getAttachment(IAttachment.Type.UNDER_BARREL, stack).getItem() == ModItems.LIGHT_GRIP.get()) {
            RenderUtil.renderModel(SpecialModels.STIGG_GRIP_LIGHT.getModel(), stack, matrixStack, buffer, light, overlay);
         } else if (Gun.getAttachment(IAttachment.Type.UNDER_BARREL, stack).getItem() == ModItems.IRON_BAYONET.get()) {
            RenderUtil.renderModel(SpecialModels.STIGG_IRON_BAYONET.getModel(), stack, matrixStack, buffer, light, overlay);
         } else if (Gun.getAttachment(IAttachment.Type.UNDER_BARREL, stack).getItem() == ModItems.ANTHRALITE_BAYONET.get()) {
            RenderUtil.renderModel(SpecialModels.STIGG_ANTHRALITE_BAYONET.getModel(), stack, matrixStack, buffer, light, overlay);
         } else if (Gun.getAttachment(IAttachment.Type.UNDER_BARREL, stack).getItem() == ModItems.DIAMOND_BAYONET.get()) {
            RenderUtil.renderModel(SpecialModels.STIGG_DIAMOND_BAYONET.getModel(), stack, matrixStack, buffer, light, overlay);
         } else if (Gun.getAttachment(IAttachment.Type.UNDER_BARREL, stack).getItem() == ModItems.NETHERITE_BAYONET.get()) {
            RenderUtil.renderModel(SpecialModels.STIGG_NETHERITE_BAYONET.getModel(), stack, matrixStack, buffer, light, overlay);
         }
      }

      if (Gun.hasAttachmentEquipped(stack, IAttachment.Type.MAGAZINE)) {
         if (Gun.getAttachment(IAttachment.Type.MAGAZINE, stack).getItem() == ModItems.EXTENDED_MAG.get()) {
            RenderUtil.renderModel(SpecialModels.STIGG_EXTENDED_MAG.getModel(), stack, matrixStack, buffer, light, overlay);
         }

         if (Gun.getAttachment(IAttachment.Type.MAGAZINE, stack).getItem() == ModItems.PLUS_P_MAG.get()) {
            RenderUtil.renderModel(SpecialModels.STIGG_EXTENDED_MAG.getModel(), stack, matrixStack, buffer, light, overlay);
         }

         if (Gun.getAttachment(IAttachment.Type.MAGAZINE, stack).getItem() == ModItems.SPEED_MAG.get()) {
            RenderUtil.renderModel(SpecialModels.STIGG_SPEED_MAG.getModel(), stack, matrixStack, buffer, light, overlay);
         }
      } else {
         RenderUtil.renderModel(SpecialModels.STIGG_STANDARD_MAG.getModel(), stack, matrixStack, buffer, light, overlay);
      }

      if (entity.equals(Minecraft.getInstance().player)) {
         matrixStack.pushPose();
         matrixStack.translate(0.0, -0.3625, 0.0);
         ItemCooldowns tracker = Minecraft.getInstance().player.getCooldowns();
         float cooldown = tracker.getCooldownPercent(stack.getItem(), Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
         cooldown = (float)this.ease((double)cooldown);
         matrixStack.translate(0.0F, 0.0F, cooldown / 8.0F);
         matrixStack.translate(0.0, 0.3625, 0.0);
         RenderUtil.renderModel(SpecialModels.STIGG_BOLT.getModel(), stack, matrixStack, buffer, light, overlay);
         matrixStack.popPose();
      }
   }

   private double ease(double x) {
      return 1.0 - Math.pow(1.0 - 2.0 * x, 4.0);
   }
}
