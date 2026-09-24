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

public class MangalitsaModel implements IOverrideModel {
   private static final float BOLT_MOVEMENT_DISTANCE = 1.0F;

   public MangalitsaModel() {
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
      RenderUtil.renderModel(SpecialModels.MANGALITSA_MAIN.getModel(), stack, matrixStack, buffer, light, overlay);
      if (Gun.getScope(stack) == null) {
         RenderUtil.renderModel(SpecialModels.MANGALITSA_SIGHTS.getModel(), stack, matrixStack, buffer, light, overlay);
      } else {
         RenderUtil.renderModel(SpecialModels.MANGALITSA_NO_SIGHTS.getModel(), stack, matrixStack, buffer, light, overlay);
      }

      this.renderStockAttachments(stack, matrixStack, buffer, light, overlay);
      this.renderBarrelAndAttachments(stack, matrixStack, buffer, light, overlay);
      if (entity.equals(Minecraft.getInstance().player)) {
         this.renderAnimatedParts(stack, matrixStack, buffer, light, overlay);
      }

      this.renderNonAnimatedUnderbarrelAttachments(stack, matrixStack, buffer, light, overlay);
   }

   private void renderStockAttachments(ItemStack stack, PoseStack matrixStack, MultiBufferSource buffer, int light, int overlay) {
      if (Gun.hasAttachmentEquipped(stack, IAttachment.Type.STOCK)) {
         if (Gun.getAttachment(IAttachment.Type.STOCK, stack).getItem() == ModItems.WEIGHTED_STOCK.get()) {
            RenderUtil.renderModel(SpecialModels.MANGALITSA_STOCK_HEAVY.getModel(), stack, matrixStack, buffer, light, overlay);
         }

         if (Gun.getAttachment(IAttachment.Type.STOCK, stack).getItem() == ModItems.BUMP_STOCK.get()) {
            RenderUtil.renderModel(SpecialModels.MANGALITSA_STOCK_HEAVY.getModel(), stack, matrixStack, buffer, light, overlay);
         }

         if (Gun.getAttachment(IAttachment.Type.STOCK, stack).getItem() == ModItems.LIGHT_STOCK.get()) {
            RenderUtil.renderModel(SpecialModels.MANGALITSA_STOCK_LIGHT.getModel(), stack, matrixStack, buffer, light, overlay);
         }

         if (Gun.getAttachment(IAttachment.Type.STOCK, stack).getItem() == ModItems.WOODEN_STOCK.get()) {
            RenderUtil.renderModel(SpecialModels.MANGALITSA_STOCK_WOODEN.getModel(), stack, matrixStack, buffer, light, overlay);
         }
      } else {
         RenderUtil.renderModel(SpecialModels.MANGALITSA_STAN_GRIP.getModel(), stack, matrixStack, buffer, light, overlay);
      }
   }

   private void renderBarrelAndAttachments(ItemStack stack, PoseStack matrixStack, MultiBufferSource buffer, int light, int overlay) {
      boolean hasExtendedBarrel = false;
      if (Gun.hasAttachmentEquipped(stack, IAttachment.Type.BARREL)) {
         if (Gun.getAttachment(IAttachment.Type.BARREL, stack).getItem() == ModItems.EXTENDED_BARREL.get()) {
            RenderUtil.renderModel(SpecialModels.MANGALITSA_EXT_BARREL.getModel(), stack, matrixStack, buffer, light, overlay);
            hasExtendedBarrel = true;
         } else if (Gun.getAttachment(IAttachment.Type.BARREL, stack).getItem() == ModItems.SILENCER.get()) {
            RenderUtil.renderModel(SpecialModels.MANGALITSA_SILENCER.getModel(), stack, matrixStack, buffer, light, overlay);
         } else if (Gun.getAttachment(IAttachment.Type.BARREL, stack).getItem() == ModItems.MUZZLE_BRAKE.get()) {
            RenderUtil.renderModel(SpecialModels.MANGALITSA_MUZZLE_BRAKE.getModel(), stack, matrixStack, buffer, light, overlay);
         } else if (Gun.getAttachment(IAttachment.Type.BARREL, stack).getItem() == ModItems.ADVANCED_SILENCER.get()) {
            RenderUtil.renderModel(SpecialModels.MANGALITSA_ADVANCED_SILENCER.getModel(), stack, matrixStack, buffer, light, overlay);
         }
      }

      if (!hasExtendedBarrel) {
         RenderUtil.renderModel(SpecialModels.MANGALITSA_STAN_BARREL.getModel(), stack, matrixStack, buffer, light, overlay);
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
      RenderUtil.renderModel(SpecialModels.MANGALITSA_BOLT.getModel(), stack, matrixStack, buffer, light, overlay);
      this.renderAnimatedUnderbarrelAttachments(stack, matrixStack, buffer, light, overlay);
      matrixStack.popPose();
   }

   private void renderAnimatedUnderbarrelAttachments(ItemStack stack, PoseStack matrixStack, MultiBufferSource buffer, int light, int overlay) {
      if (Gun.hasAttachmentEquipped(stack, IAttachment.Type.UNDER_BARREL)) {
         if (Gun.getAttachment(IAttachment.Type.UNDER_BARREL, stack).getItem() == ModItems.VERTICAL_GRIP.get()) {
            RenderUtil.renderModel(SpecialModels.MANGALITSA_TACT_GRIP.getModel(), stack, matrixStack, buffer, light, overlay);
         } else if (Gun.getAttachment(IAttachment.Type.UNDER_BARREL, stack).getItem() == ModItems.LIGHT_GRIP.get()) {
            RenderUtil.renderModel(SpecialModels.MANGALITSA_LIGHT_GRIP.getModel(), stack, matrixStack, buffer, light, overlay);
         }
      }
   }

   private void renderNonAnimatedUnderbarrelAttachments(ItemStack stack, PoseStack matrixStack, MultiBufferSource buffer, int light, int overlay) {
      if (Gun.hasAttachmentEquipped(stack, IAttachment.Type.UNDER_BARREL)) {
         if (Gun.getAttachment(IAttachment.Type.UNDER_BARREL, stack).getItem() == ModItems.IRON_BAYONET.get()) {
            RenderUtil.renderModel(SpecialModels.MANGALITSA_IRON_BAYONET.getModel(), stack, matrixStack, buffer, light, overlay);
         } else if (Gun.getAttachment(IAttachment.Type.UNDER_BARREL, stack).getItem() == ModItems.ANTHRALITE_BAYONET.get()) {
            RenderUtil.renderModel(SpecialModels.MANGALITSA_ANTHRALITE_BAYONET.getModel(), stack, matrixStack, buffer, light, overlay);
         } else if (Gun.getAttachment(IAttachment.Type.UNDER_BARREL, stack).getItem() == ModItems.DIAMOND_BAYONET.get()) {
            RenderUtil.renderModel(SpecialModels.MANGALITSA_DIAMOND_BAYONET.getModel(), stack, matrixStack, buffer, light, overlay);
         } else if (Gun.getAttachment(IAttachment.Type.UNDER_BARREL, stack).getItem() == ModItems.NETHERITE_BAYONET.get()) {
            RenderUtil.renderModel(SpecialModels.MANGALITSA_NETHERITE_BAYONET.getModel(), stack, matrixStack, buffer, light, overlay);
         }
      }
   }

   public static float getBoltMovementDistance() {
      return 1.0F;
   }

   private double ease(double x) {
      return 1.0 - Math.pow(1.0 - 2.0 * x, 4.0);
   }

   @EventBusSubscriber(
      modid = "scguns",
      value = {Dist.CLIENT}
   )
   public static class GunFireEventCombatShotgunHandler {
      private static float pumpProgress = 0.0F;

      public GunFireEventCombatShotgunHandler() {
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
