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

public class RatKingAndQueenModel implements IOverrideModel {
   public RatKingAndQueenModel() {
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
      RenderUtil.renderModel(SpecialModels.RAT_KING_AND_QUEEN_MAIN.getModel(), stack, matrixStack, buffer, light, overlay);
      this.renderBarrelAttachments(matrixStack, buffer, stack, light, overlay);
      this.renderMagazineAttachments(matrixStack, buffer, stack, light, overlay);

      assert entity != null;

      if (entity.equals(Minecraft.getInstance().player)) {
         this.renderReceivers(matrixStack, buffer, stack, light, overlay);
      }
   }

   private void renderBarrelAttachments(PoseStack matrixStack, MultiBufferSource buffer, ItemStack stack, int light, int overlay) {
      boolean isExtendedBarrelEquipped = false;
      if (Gun.hasAttachmentEquipped(stack, IAttachment.Type.BARREL)
         && Gun.getAttachment(IAttachment.Type.BARREL, stack).getItem() == ModItems.EXTENDED_BARREL.get()) {
         isExtendedBarrelEquipped = true;
         RenderUtil.renderModel(SpecialModels.RAT_KING_AND_QUEEN_EXT_BARREL.getModel(), stack, matrixStack, buffer, light, overlay);
      }

      if (!isExtendedBarrelEquipped) {
         RenderUtil.renderModel(SpecialModels.RAT_KING_AND_QUEEN_STAN_BARREL.getModel(), stack, matrixStack, buffer, light, overlay);
      }

      if (Gun.hasAttachmentEquipped(stack, IAttachment.Type.BARREL)) {
         if (Gun.getAttachment(IAttachment.Type.BARREL, stack).getItem() == ModItems.SILENCER.get()) {
            RenderUtil.renderModel(SpecialModels.RAT_KING_AND_QUEEN_SILENCER.getModel(), stack, matrixStack, buffer, light, overlay);
         } else if (Gun.getAttachment(IAttachment.Type.BARREL, stack).getItem() == ModItems.MUZZLE_BRAKE.get()) {
            RenderUtil.renderModel(SpecialModels.RAT_KING_AND_QUEEN_MUZZLE_BRAKE.getModel(), stack, matrixStack, buffer, light, overlay);
         } else if (Gun.getAttachment(IAttachment.Type.BARREL, stack).getItem() == ModItems.ADVANCED_SILENCER.get()) {
            RenderUtil.renderModel(SpecialModels.RAT_KING_AND_QUEEN_ADVANCED_SILENCER.getModel(), stack, matrixStack, buffer, light, overlay);
         }
      }
   }

   private void renderMagazineAttachments(PoseStack matrixStack, MultiBufferSource buffer, ItemStack stack, int light, int overlay) {
      if (Gun.hasAttachmentEquipped(stack, IAttachment.Type.MAGAZINE)) {
         if (Gun.getAttachment(IAttachment.Type.MAGAZINE, stack).getItem() == ModItems.EXTENDED_MAG.get()) {
            RenderUtil.renderModel(SpecialModels.RAT_KING_AND_QUEEN_EXTENDED_MAG.getModel(), stack, matrixStack, buffer, light, overlay);
         } else if (Gun.getAttachment(IAttachment.Type.MAGAZINE, stack).getItem() == ModItems.SPEED_MAG.get()) {
            RenderUtil.renderModel(SpecialModels.RAT_KING_AND_QUEEN_SPEED_MAG.getModel(), stack, matrixStack, buffer, light, overlay);
         } else if (Gun.getAttachment(IAttachment.Type.MAGAZINE, stack).getItem() == ModItems.PLUS_P_MAG.get()) {
            RenderUtil.renderModel(SpecialModels.RAT_KING_AND_QUEEN_EXTENDED_MAG.getModel(), stack, matrixStack, buffer, light, overlay);
         }
      } else {
         RenderUtil.renderModel(SpecialModels.RAT_KING_AND_QUEEN_STANDARD_MAG.getModel(), stack, matrixStack, buffer, light, overlay);
      }
   }

   private void renderReceivers(PoseStack matrixStack, MultiBufferSource buffer, ItemStack stack, int light, int overlay) {
      matrixStack.pushPose();
      matrixStack.translate(0.0, -0.3625, 0.0);
      ItemCooldowns tracker = Minecraft.getInstance().player.getCooldowns();
      float cooldown = tracker.getCooldownPercent(stack.getItem(), Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
      cooldown = (float)this.ease((double)cooldown);
      int shotCount = RatKingAndQueenModel.GunFireEventRatHandler.getShotCount();
      matrixStack.pushPose();
      if (shotCount % 2 == 0 && cooldown > 0.0F) {
         matrixStack.translate(0.0F, 0.0F, cooldown / 8.0F);
      }

      matrixStack.translate(0.0, 0.3625, 0.0);
      RenderUtil.renderModel(SpecialModels.RAT_KING_AND_QUEEN_RECEIVER_1.getModel(), stack, matrixStack, buffer, light, overlay);
      matrixStack.popPose();
      matrixStack.pushPose();
      if (shotCount % 2 == 1 && cooldown > 0.0F) {
         matrixStack.translate(0.0F, 0.0F, cooldown / 8.0F);
      }

      matrixStack.translate(0.0, 0.3625, 0.0);
      RenderUtil.renderModel(SpecialModels.RAT_KING_AND_QUEEN_RECEIVER_2.getModel(), stack, matrixStack, buffer, light, overlay);
      matrixStack.popPose();
      matrixStack.popPose();
   }

   private double ease(double x) {
      return 1.0 - Math.pow(1.0 - 2.0 * x, 4.0);
   }

   @EventBusSubscriber(
      modid = "scguns",
      value = {Dist.CLIENT}
   )
   public static class GunFireEventRatHandler {
      private static int shotCount = 0;

      public GunFireEventRatHandler() {
         super();
      }

      @SubscribeEvent
      public static void onGunFire(GunFireEvent.Post event) {
         if (event.isClient()) {
            shotCount++;
         }
      }

      public static int getShotCount() {
         return shotCount;
      }
   }
}
