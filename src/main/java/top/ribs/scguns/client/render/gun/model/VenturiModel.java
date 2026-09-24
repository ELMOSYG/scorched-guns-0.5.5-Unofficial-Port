package top.ribs.scguns.client.render.gun.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import top.ribs.scguns.client.SpecialModels;
import top.ribs.scguns.client.render.gun.IOverrideModel;
import top.ribs.scguns.client.util.RenderUtil;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.event.GunFireEvent;
import top.ribs.scguns.init.ModItems;
import top.ribs.scguns.item.attachment.IAttachment;

public class VenturiModel implements IOverrideModel {
   private static final float BOLT_MOVEMENT_DISTANCE = 1.0F;

   public VenturiModel() {
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
      RenderUtil.renderModel(SpecialModels.VENTURI_MAIN.getModel(), stack, matrixStack, buffer, light, overlay);
      if (Gun.getScope(stack) == null) {
         RenderUtil.renderModel(SpecialModels.VENTURI_SIGHTS.getModel(), stack, matrixStack, buffer, light, overlay);
      } else {
         RenderUtil.renderModel(SpecialModels.VENTURI_NO_SIGHTS.getModel(), stack, matrixStack, buffer, light, overlay);
      }

      this.renderStockAttachments(stack, matrixStack, buffer, light, overlay);
      if (entity.equals(Minecraft.getInstance().player)) {
         this.renderAnimatedParts(stack, matrixStack, buffer, light, overlay);
      }
   }

   private void renderStockAttachments(ItemStack stack, PoseStack matrixStack, MultiBufferSource buffer, int light, int overlay) {
      if (Gun.hasAttachmentEquipped(stack, IAttachment.Type.STOCK)) {
         if (Gun.getAttachment(IAttachment.Type.STOCK, stack).getItem() == ModItems.WEIGHTED_STOCK.get()) {
            RenderUtil.renderModel(SpecialModels.VENTURI_STOCK_HEAVY.getModel(), stack, matrixStack, buffer, light, overlay);
         }

         if (Gun.getAttachment(IAttachment.Type.STOCK, stack).getItem() == ModItems.BUMP_STOCK.get()) {
            RenderUtil.renderModel(SpecialModels.VENTURI_STOCK_HEAVY.getModel(), stack, matrixStack, buffer, light, overlay);
         }

         if (Gun.getAttachment(IAttachment.Type.STOCK, stack).getItem() == ModItems.LIGHT_STOCK.get()) {
            RenderUtil.renderModel(SpecialModels.VENTURI_STOCK_LIGHT.getModel(), stack, matrixStack, buffer, light, overlay);
         }

         if (Gun.getAttachment(IAttachment.Type.STOCK, stack).getItem() == ModItems.WOODEN_STOCK.get()) {
            RenderUtil.renderModel(SpecialModels.VENTURI_STOCK_WOODEN.getModel(), stack, matrixStack, buffer, light, overlay);
         }
      } else {
         RenderUtil.renderModel(SpecialModels.VENTURI_STANDARD_GRIP.getModel(), stack, matrixStack, buffer, light, overlay);
      }
   }

   private void renderAnimatedParts(ItemStack stack, PoseStack matrixStack, MultiBufferSource buffer, int light, int overlay) {
      matrixStack.pushPose();
      matrixStack.translate(0.0, -0.3625, 0.0);
      ItemCooldowns tracker = Minecraft.getInstance().player.getCooldowns();
      float cooldown = tracker.getCooldownPercent(stack.getItem(), Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
      cooldown = (float)this.ease((double)cooldown);
      matrixStack.translate(0.0F, 0.0F, cooldown / 8.0F);
      matrixStack.translate(0.0, 0.3625, 0.0);
      RenderUtil.renderModel(SpecialModels.VENTURI_BOLT.getModel(), stack, matrixStack, buffer, light, overlay);
      matrixStack.popPose();
   }

   private double ease(double x) {
      return 1.0 - Math.pow(1.0 - 2.0 * x, 4.0);
   }

   @EventBusSubscriber(
      modid = "scguns",
      value = {Dist.CLIENT}
   )
   public static class GunFireEventVenturiHandler {
      private static float pumpProgress = 0.0F;

      public GunFireEventVenturiHandler() {
         super();
      }

      @SubscribeEvent
      public static void onGunFire(GunFireEvent.Post event) {
         if (event.isClient()) {
            pumpProgress = 1.0F;
         }
      }

      public static float getPumpProgress(float partialTicks) {
         return pumpProgress > 0.0F ? (pumpProgress -= partialTicks * 0.1F) : 0.0F;
      }
   }
}
