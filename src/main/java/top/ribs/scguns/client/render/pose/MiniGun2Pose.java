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
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;
import top.ribs.scguns.Config;
import top.ribs.scguns.client.handler.GunRenderingHandler;
import top.ribs.scguns.client.handler.ReloadHandler;
import top.ribs.scguns.client.util.RenderUtil;
import top.ribs.scguns.common.GripType;
import top.ribs.scguns.item.animated.AnimatedGunItem;

public class MiniGun2Pose extends WeaponPose {
   public MiniGun2Pose() {
      super();
   }

   @Override
   protected AimPose getUpPose() {
      AimPose pose = new AimPose();
      pose.getIdle()
         .setRenderYawOffset(45.0F)
         .setItemRotation(new Vector3f(10.0F, 0.0F, 0.0F))
         .setRightArm(new LimbPose().setRotationAngleX(-100.0F).setRotationAngleY(-45.0F).setRotationAngleZ(0.0F).setRotationPointY(2.0F))
         .setLeftArm(new LimbPose().setRotationAngleX(-150.0F).setRotationAngleY(40.0F).setRotationAngleZ(-10.0F).setRotationPointY(1.0F));
      return pose;
   }

   @Override
   protected AimPose getForwardPose() {
      AimPose pose = new AimPose();
      pose.getIdle()
         .setRenderYawOffset(45.0F)
         .setRightArm(new LimbPose().setRotationAngleX(-15.0F).setRotationAngleY(-45.0F).setRotationAngleZ(0.0F).setRotationPointY(2.0F))
         .setLeftArm(new LimbPose().setRotationAngleX(-45.0F).setRotationAngleY(30.0F).setRotationAngleZ(0.0F).setRotationPointY(2.0F));
      return pose;
   }

   @Override
   protected AimPose getDownPose() {
      AimPose pose = new AimPose();
      pose.getIdle()
         .setRenderYawOffset(45.0F)
         .setItemRotation(new Vector3f(-50.0F, 0.0F, 0.0F))
         .setItemTranslate(new Vector3f(0.0F, 0.0F, 1.0F))
         .setRightArm(new LimbPose().setRotationAngleX(0.0F).setRotationAngleY(-45.0F).setRotationAngleZ(0.0F).setRotationPointY(1.0F))
         .setLeftArm(new LimbPose().setRotationAngleX(-25.0F).setRotationAngleY(30.0F).setRotationAngleZ(15.0F).setRotationPointY(4.0F));
      return pose;
   }

   @Override
   protected AimPose getMeleePose() {
      AimPose meleePose = new AimPose();
      meleePose.getIdle()
         .setRenderYawOffset(0.0F)
         .setItemRotation(new Vector3f(0.0F, 0.0F, 0.0F))
         .setRightArm(new LimbPose().setRotationAngleX(-90.0F).setRotationAngleY(0.0F).setRotationPointX(0.0F).setRotationPointY(0.0F).setRotationPointZ(0.0F))
         .setLeftArm(new LimbPose().setRotationAngleX(-90.0F).setRotationAngleY(0.0F).setRotationPointX(0.0F).setRotationPointY(0.0F).setRotationPointZ(0.0F));
      meleePose.getAiming()
         .setRenderYawOffset(0.0F)
         .setItemRotation(new Vector3f(0.0F, 0.0F, 0.0F))
         .setRightArm(new LimbPose().setRotationAngleX(-90.0F).setRotationAngleY(0.0F).setRotationPointX(0.0F).setRotationPointY(0.0F).setRotationPointZ(0.0F))
         .setLeftArm(new LimbPose().setRotationAngleX(-90.0F).setRotationAngleY(0.0F).setRotationPointX(0.0F).setRotationPointY(0.0F).setRotationPointZ(0.0F));
      return meleePose;
   }

   @Override
   protected AimPose getBanzaiPose() {
      AimPose banzaiPose = new AimPose();
      banzaiPose.getIdle()
         .setRenderYawOffset(0.0F)
         .setItemRotation(new Vector3f(0.0F, 0.0F, 0.0F))
         .setRightArm(new LimbPose().setRotationAngleX(-90.0F).setRotationAngleY(0.0F).setRotationPointX(0.0F).setRotationPointY(0.0F).setRotationPointZ(0.0F))
         .setLeftArm(new LimbPose().setRotationAngleX(-90.0F).setRotationAngleY(0.0F).setRotationPointX(0.0F).setRotationPointY(0.0F).setRotationPointZ(0.0F));
      return banzaiPose;
   }

   @Override
   protected boolean hasAimPose() {
      return false;
   }

   @Override
   public void applyPlayerModelRotation(Player player, ModelPart rightArm, ModelPart leftArm, ModelPart head, InteractionHand hand, float aimProgress) {
      if ((Boolean)Config.CLIENT.display.oldAnimations.get()) {
         boolean right = Minecraft.getInstance().options.mainHand().get() == HumanoidArm.RIGHT
            ? hand == InteractionHand.MAIN_HAND
            : hand == InteractionHand.OFF_HAND;
         ModelPart mainArm = right ? rightArm : leftArm;
         ModelPart secondaryArm = right ? leftArm : rightArm;
         mainArm.xRot = (float)Math.toRadians(-15.0);
         mainArm.yRot = (float)Math.toRadians(-45.0) * (right ? 1.0F : -1.0F);
         mainArm.zRot = (float)Math.toRadians(0.0);
         secondaryArm.xRot = (float)Math.toRadians(-45.0);
         secondaryArm.yRot = (float)Math.toRadians(30.0) * (right ? 1.0F : -1.0F);
         secondaryArm.zRot = (float)Math.toRadians(0.0);
      } else {
         super.applyPlayerModelRotation(player, rightArm, leftArm, head, hand, aimProgress);
      }

      if (GunRenderingHandler.get().isThirdPersonMeleeAttacking()) {
         float banzaiProgress = GunRenderingHandler.get().getThirdPersonMeleeProgress();
         this.applyBanzaiPose(rightArm, leftArm, banzaiProgress);
      }
   }

   private void applyBanzaiPose(ModelPart rightArm, ModelPart leftArm, float banzaiProgress) {
      rightArm.xRot = Mth.lerp(banzaiProgress, rightArm.xRot, (float)Math.toRadians(-90.0));
      leftArm.xRot = Mth.lerp(banzaiProgress, leftArm.xRot, (float)Math.toRadians(-90.0));
   }

   @Override
   public void applyPlayerPreRender(Player player, InteractionHand hand, float aimProgress, PoseStack poseStack, MultiBufferSource buffer) {
      if ((Boolean)Config.CLIENT.display.oldAnimations.get()) {
         boolean right = Minecraft.getInstance().options.mainHand().get() == HumanoidArm.RIGHT
            ? hand == InteractionHand.MAIN_HAND
            : hand == InteractionHand.OFF_HAND;
         player.yBodyRotO = player.yRotO + 45.0F * (right ? 1.0F : -1.0F);
         player.yBodyRot = player.getYRot() + 45.0F * (right ? 1.0F : -1.0F);
      } else {
         super.applyPlayerPreRender(player, hand, aimProgress, poseStack, buffer);
      }
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void applyHeldItemTransforms(Player player, InteractionHand hand, float aimProgress, PoseStack poseStack, MultiBufferSource buffer) {
      if ((Boolean)Config.CLIENT.display.oldAnimations.get()) {
         if (hand == InteractionHand.OFF_HAND) {
            poseStack.translate(0.0F, -0.625F, 0.0F);
            poseStack.translate(0.0F, 0.0F, -0.125F);
         }
      } else {
         super.applyHeldItemTransforms(player, hand, aimProgress, poseStack, buffer);
      }
   }

   @Override
   public boolean applyOffhandTransforms(Player player, PlayerModel model, ItemStack stack, PoseStack poseStack, float partialTicks) {
      return GripType.applyBackTransforms(player, poseStack);
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
         int side = hand.getOpposite() == HumanoidArm.RIGHT ? 1 : -1;
         poseStack.translate(translateX * (float)side, 0.0F, 0.0F);
         boolean slim = Minecraft.getInstance().player.getSkin().model() == PlayerSkin.Model.SLIM;
         float armWidth = slim ? 3.0F : 4.0F;
         poseStack.pushPose();
         float reloadProgress = ReloadHandler.get().getReloadProgress(partialTicks);
         poseStack.translate((double)reloadProgress * 0.5, (double)(-reloadProgress), (double)(-reloadProgress) * 0.5);
         poseStack.scale(0.6F, 0.6F, 0.6F);
         poseStack.translate(0.25 * (double)side, 0.3, 0.0);
         poseStack.translate((double)armWidth / 2.0 * 0.0625 * (double)side, 0.0, 0.0);
         poseStack.translate(-0.7 * (double)side, -0.1, -0.7);
         poseStack.mulPose(Axis.XP.rotationDegrees(110.0F));
         poseStack.mulPose(Axis.YP.rotationDegrees(12.0F * (float)(-side)));
         poseStack.mulPose(Axis.ZP.rotationDegrees(12.0F * (float)(-side)));
         poseStack.mulPose(Axis.XP.rotationDegrees(-35.0F));
         RenderUtil.renderFirstPersonArm((LocalPlayer)player, hand.getOpposite(), poseStack, buffer, light);
         poseStack.popPose();
         poseStack.pushPose();
         poseStack.translate(0.0, 0.1, -0.675);
         poseStack.scale(0.5F, 0.5F, 0.5F);
         poseStack.translate(-0.25 * (double)side, 0.0, 0.0);
         poseStack.translate(-((double)armWidth / 2.0) * 0.0625 * (double)side, 0.0, 0.0);
         poseStack.mulPose(Axis.XP.rotationDegrees(80.0F));
         RenderUtil.renderFirstPersonArm((LocalPlayer)player, hand, poseStack, buffer, light);
         poseStack.popPose();
      }
   }

   @Override
   public boolean canApplySprintingAnimation() {
      return false;
   }
}
