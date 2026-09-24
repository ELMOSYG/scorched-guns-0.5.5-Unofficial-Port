package top.ribs.scguns.client.render.gun.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
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
import top.ribs.scguns.event.GunFireEvent;

public class UltraKnightHawkModel implements IOverrideModel {
   private static final int TOTAL_SHOTS = 7;
   private static final float ROTATION_INCREMENT = 51.42857F;
   private float currentRotation = 0.0F;

   public UltraKnightHawkModel() {
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
      RenderUtil.renderModel(SpecialModels.ULTRA_KNIGHT_HAWK_MAIN.getModel(), stack, matrixStack, buffer, light, overlay);
      if (entity.equals(Minecraft.getInstance().player)) {
         matrixStack.pushPose();
         matrixStack.translate(0.0, -0.28, 0.36);
         ItemCooldowns tracker = Minecraft.getInstance().player.getCooldowns();
         float cooldown = tracker.getCooldownPercent(stack.getItem(), Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
         cooldown = (float)this.ease((double)cooldown);
         float rotationAngle = -cooldown * 38.0F;
         matrixStack.mulPose(Axis.XP.rotationDegrees(rotationAngle));
         matrixStack.translate(0.0, 0.28, -0.36);
         RenderUtil.renderModel(SpecialModels.ULTRA_KNIGHT_HAWK_HAMMER.getModel(), stack, matrixStack, buffer, light, overlay);
         matrixStack.popPose();
         this.renderDrumRotation(matrixStack, buffer, stack, partialTicks, light, overlay);
      }
   }

   private void renderDrumRotation(PoseStack matrixStack, MultiBufferSource buffer, ItemStack stack, float partialTicks, int light, int overlay) {
      assert Minecraft.getInstance().player != null;

      ItemCooldowns tracker = Minecraft.getInstance().player.getCooldowns();
      float cooldown = tracker.getCooldownPercent(stack.getItem(), Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
      int shotCount = UltraKnightHawkModel.GunFireEventKnightHandler.getShotCount();
      float targetRotation = (float)shotCount * 51.42857F;
      this.currentRotation = this.currentRotation + (targetRotation - this.currentRotation) * partialTicks;
      matrixStack.pushPose();
      matrixStack.translate(0.0, -0.26, 0.0);
      matrixStack.mulPose(Axis.ZP.rotationDegrees(this.currentRotation));
      matrixStack.translate(0.0, 0.26, 0.0);
      RenderUtil.renderModel(SpecialModels.ULTRA_KNIGHT_HAWK_DRUM.getModel(), stack, matrixStack, buffer, light, overlay);
      matrixStack.popPose();
   }

   private double ease(double x) {
      return 1.0 - Math.pow(1.0 - x, 4.0);
   }

   @EventBusSubscriber(
      modid = "scguns",
      value = {Dist.CLIENT}
   )
   public static class GunFireEventKnightHandler {
      private static int shotCount = 0;

      public GunFireEventKnightHandler() {
         super();
      }

      @SubscribeEvent
      public static void onGunFire(GunFireEvent.Post event) {
         if (event.isClient()) {
            shotCount++;
            shotCount %= 7;
         }
      }

      public static int getShotCount() {
         return shotCount;
      }
   }
}
