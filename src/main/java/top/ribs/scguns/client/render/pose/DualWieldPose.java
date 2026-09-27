package top.ribs.scguns.client.render.pose;


import net.minecraft.client.resources.PlayerSkin;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import top.ribs.scguns.client.handler.GunRenderingHandler;
import top.ribs.scguns.client.render.IHeldAnimation;
import top.ribs.scguns.client.util.RenderUtil;
import top.ribs.scguns.item.animated.AnimatedGunItem;

public class DualWieldPose implements IHeldAnimation {
   public DualWieldPose() {
      super();
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void applyPlayerModelRotation(Player player, ModelPart rightArm, ModelPart leftArm, ModelPart head, InteractionHand hand, float aimProgress) {
      boolean isLeftHand = hand == InteractionHand.OFF_HAND;
      ModelPart arm = isLeftHand ? leftArm : rightArm;
      GunRenderingHandler renderingHandler = GunRenderingHandler.get();
      if (renderingHandler.isThirdPersonMeleeAttacking()) {
         this.applySimplifiedMeleePose(player, arm, renderingHandler.getThirdPersonMeleeProgress());
      } else {
         rightArm.xRot = (float)Math.toRadians(-90.0);
         leftArm.xRot = (float)Math.toRadians(-90.0);
         rightArm.yRot = 0.0F;
         leftArm.yRot = 0.0F;
         rightArm.zRot = 0.0F;
         leftArm.zRot = 0.0F;
      }
   }

   private void applySimplifiedMeleePose(Player player, ModelPart arm, float progress) {
      if (progress < 0.2F) {
         float lowerProgress = progress / 0.2F;
         arm.xRot = (float)Math.toRadians((double)(-70.0F - 10.0F * lowerProgress));
         arm.yRot = 0.0F;
      } else if (progress < 0.7F) {
         float swingProgress = (progress - 0.2F) / 0.5F;
         arm.xRot = (float)Math.toRadians((double)(-80.0F + 80.0F * swingProgress));
         arm.yRot = 0.0F;
      } else {
         float returnProgress = (progress - 0.7F) / 0.3F;
         arm.xRot = (float)Math.toRadians((double)(0.0F - 70.0F * returnProgress));
         arm.yRot = 0.0F;
      }
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void renderFirstPersonArms(
      Player player, HumanoidArm hand, ItemStack stack, PoseStack poseStack, MultiBufferSource buffer, int light, float partialTicks
   ) {
      if (!(stack.getItem() instanceof AnimatedGunItem)) {
         poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
         BakedModel model = Minecraft.getInstance().getItemRenderer().getModel(stack, player.level(), player, 0);
         float translateX = model.getTransforms().firstPersonRightHand.translation.x();
         float translateZ = model.getTransforms().firstPersonRightHand.translation.z();
         int side = hand.getOpposite() == HumanoidArm.RIGHT ? 1 : -1;
         poseStack.translate(translateX * (float)side, 0.0F, -translateZ - 0.1F);
         boolean slim = Minecraft.getInstance().player.getSkin().model() == PlayerSkin.Model.SLIM;
         float armWidth = slim ? 3.0F : 4.0F;
         poseStack.pushPose();
         poseStack.scale(0.55F, 0.55F, 0.55F);
         poseStack.translate(-0.25 * (double)side, 0.0, 0.0);
         poseStack.translate(-((double)armWidth / 2.0) * 0.0625 * (double)side, 0.0, 0.1);
         poseStack.translate(0.0, 0.18, -1.3125);
         poseStack.mulPose(Axis.XP.rotationDegrees(75.0F));
         RenderUtil.renderFirstPersonArm((LocalPlayer)player, hand, poseStack, buffer, light);
         poseStack.popPose();
         poseStack.pushPose();
         poseStack.scale(0.55F, 0.55F, 0.55F);
         poseStack.translate(-1.21875 * (double)side, 0.0, 0.0);
         poseStack.translate(-((double)armWidth / 2.0) * 0.0625 * (double)side, 0.0, 0.1);
         poseStack.translate(0.0, 0.18, -1.3125);
         poseStack.mulPose(Axis.XP.rotationDegrees(75.0F));
         RenderUtil.renderFirstPersonArm((LocalPlayer)player, hand, poseStack, buffer, light);
         poseStack.popPose();
      }
   }

   @Override
   public boolean applyOffhandTransforms(Player player, PlayerModel model, ItemStack stack, PoseStack poseStack, float partialTicks) {
      poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
      poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
      if (player.isCrouching()) {
         poseStack.translate(-0.28125, -0.9375, -0.25);
      } else if (!player.getItemBySlot(EquipmentSlot.LEGS).isEmpty()) {
         poseStack.translate(-0.25, -0.8125, 0.0625);
      } else {
         poseStack.translate(-0.21875, -0.8125, 0.0625);
      }

      poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
      poseStack.mulPose(Axis.ZP.rotationDegrees(75.0F));
      poseStack.mulPose(Axis.ZP.rotationDegrees((float)(Math.toDegrees((double)model.rightLeg.xRot) / 10.0)));
      poseStack.scale(0.5F, 0.5F, 0.5F);
      return true;
   }

   @Override
   public boolean canApplySprintingAnimation() {
      return false;
   }

   @Override
   public boolean canRenderOffhandItem() {
      return false;
   }

   @Override
   public double getFallSwayZOffset() {
      return 0.5;
   }
}
