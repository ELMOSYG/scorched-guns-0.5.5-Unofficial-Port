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
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;
import top.ribs.scguns.Config;
import top.ribs.scguns.client.handler.GunRenderingHandler;
import top.ribs.scguns.client.util.RenderUtil;
import top.ribs.scguns.common.GripType;
import top.ribs.scguns.item.animated.AnimatedGunItem;

public class TwoHandedShotgunPose extends WeaponPose {
   public TwoHandedShotgunPose() {
      super();
   }

   @Override
   protected AimPose getUpPose() {
      AimPose upPose = new AimPose();
      upPose.getIdle()
         .setRenderYawOffset(45.0F)
         .setItemRotation(new Vector3f(60.0F, 0.0F, 10.0F))
         .setRightArm(
            new LimbPose().setRotationAngleX(-120.0F).setRotationAngleY(-55.0F).setRotationPointX(-5.0F).setRotationPointY(3.0F).setRotationPointZ(0.0F)
         )
         .setLeftArm(
            new LimbPose().setRotationAngleX(-160.0F).setRotationAngleY(-20.0F).setRotationAngleZ(-30.0F).setRotationPointY(2.0F).setRotationPointZ(-1.0F)
         );
      upPose.getAiming()
         .setRenderYawOffset(45.0F)
         .setItemRotation(new Vector3f(40.0F, 0.0F, 30.0F))
         .setItemTranslate(new Vector3f(-1.0F, 0.0F, 0.0F))
         .setRightArm(
            new LimbPose().setRotationAngleX(-140.0F).setRotationAngleY(-55.0F).setRotationPointX(-5.0F).setRotationPointY(3.0F).setRotationPointZ(0.0F)
         )
         .setLeftArm(
            new LimbPose().setRotationAngleX(-170.0F).setRotationAngleY(-20.0F).setRotationAngleZ(-35.0F).setRotationPointY(1.0F).setRotationPointZ(0.0F)
         );
      return upPose;
   }

   @Override
   protected AimPose getForwardPose() {
      AimPose forwardPose = new AimPose();
      forwardPose.getIdle()
         .setRenderYawOffset(45.0F)
         .setItemRotation(new Vector3f(30.0F, -11.0F, 0.0F))
         .setRightArm(
            new LimbPose()
               .setRotationAngleX(-60.0F)
               .setRotationAngleY(-55.0F)
               .setRotationAngleZ(0.0F)
               .setRotationPointX(-5.0F)
               .setRotationPointY(2.0F)
               .setRotationPointZ(1.0F)
         )
         .setLeftArm(
            new LimbPose().setRotationAngleX(-65.0F).setRotationAngleY(-10.0F).setRotationAngleZ(5.0F).setRotationPointY(2.0F).setRotationPointZ(-1.0F)
         );
      forwardPose.getAiming()
         .setRenderYawOffset(45.0F)
         .setItemRotation(new Vector3f(5.0F, -21.0F, 0.0F))
         .setRightArm(
            new LimbPose().setRotationAngleX(-85.0F).setRotationAngleY(-65.0F).setRotationAngleZ(0.0F).setRotationPointX(-5.0F).setRotationPointY(2.0F)
         )
         .setLeftArm(new LimbPose().setRotationAngleX(-90.0F).setRotationAngleY(-15.0F).setRotationAngleZ(0.0F).setRotationPointY(2.0F).setRotationPointZ(0.0F));
      return forwardPose;
   }

   @Override
   protected AimPose getDownPose() {
      AimPose downPose = new AimPose();
      downPose.getIdle()
         .setRenderYawOffset(45.0F)
         .setItemRotation(new Vector3f(-15.0F, -5.0F, 0.0F))
         .setItemTranslate(new Vector3f(0.0F, -0.5F, 0.5F))
         .setRightArm(
            new LimbPose().setRotationAngleX(-30.0F).setRotationAngleY(-65.0F).setRotationAngleZ(0.0F).setRotationPointX(-5.0F).setRotationPointY(2.0F)
         )
         .setLeftArm(new LimbPose().setRotationAngleX(-5.0F).setRotationAngleY(-20.0F).setRotationAngleZ(20.0F).setRotationPointY(5.0F).setRotationPointZ(0.0F));
      downPose.getAiming()
         .setRenderYawOffset(45.0F)
         .setItemRotation(new Vector3f(-20.0F, -5.0F, -10.0F))
         .setItemTranslate(new Vector3f(0.0F, -0.5F, 1.0F))
         .setRightArm(
            new LimbPose().setRotationAngleX(-30.0F).setRotationAngleY(-65.0F).setRotationAngleZ(0.0F).setRotationPointX(-5.0F).setRotationPointY(1.0F)
         )
         .setLeftArm(
            new LimbPose().setRotationAngleX(-10.0F).setRotationAngleY(-20.0F).setRotationAngleZ(30.0F).setRotationPointY(5.0F).setRotationPointZ(0.0F)
         );
      return downPose;
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

   private void applyBanzaiPose(ModelPart rightArm, ModelPart leftArm, float banzaiProgress) {
      float smoothProgress = this.easeInOut(banzaiProgress);
      rightArm.xRot = Mth.lerp(smoothProgress, rightArm.xRot, (float)Math.toRadians(-70.0));
      leftArm.xRot = Mth.lerp(smoothProgress, leftArm.xRot, (float)Math.toRadians(-70.0));
   }

   private float easeInOut(float t) {
      return t * t * (3.0F - 2.0F * t);
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void applyPlayerModelRotation(Player player, ModelPart rightArm, ModelPart leftArm, ModelPart head, InteractionHand hand, float aimProgress) {
      super.applyPlayerModelRotation(player, rightArm, leftArm, head, hand, aimProgress);
      float angle = this.getPlayerPitch(player);
      head.xRot = (float)Math.toRadians((double)angle > 0.0 ? (double)(angle * 70.0F) : (double)(angle * 90.0F));
      boolean isTwoHandedPose = true;
      if (!isTwoHandedPose) {
         player.getOffhandItem().isEmpty();
      }

      super.applyPlayerModelRotation(player, rightArm, leftArm, head, hand, aimProgress);
      if (GunRenderingHandler.get().isThirdPersonMeleeAttacking()) {
         float banzaiProgress = GunRenderingHandler.get().getThirdPersonMeleeProgress();
         this.applyBanzaiPose(rightArm, leftArm, banzaiProgress);
      }
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void applyPlayerPreRender(Player player, InteractionHand hand, float aimProgress, PoseStack poseStack, MultiBufferSource buffer) {
      if ((Boolean)Config.CLIENT.display.oldAnimations.get()) {
         boolean right = Minecraft.getInstance().options.mainHand().get() == HumanoidArm.RIGHT
            ? hand == InteractionHand.MAIN_HAND
            : hand == InteractionHand.OFF_HAND;
         player.yBodyRotO = player.yRotO + (right ? 25.0F : -25.0F) + aimProgress * (right ? 20.0F : -20.0F);
         player.yBodyRot = player.getYRot() + (right ? 25.0F : -25.0F) + aimProgress * (right ? 20.0F : -20.0F);
      } else {
         super.applyPlayerPreRender(player, hand, aimProgress, poseStack, buffer);
      }
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void applyHeldItemTransforms(Player player, InteractionHand hand, float aimProgress, PoseStack poseStack, MultiBufferSource buffer) {
      if ((Boolean)Config.CLIENT.display.oldAnimations.get()) {
         if (hand == InteractionHand.MAIN_HAND) {
            boolean right = Minecraft.getInstance().options.mainHand().get() == HumanoidArm.RIGHT;
            poseStack.translate(0.0, 0.0, 0.05);
            float invertRealProgress = 1.0F - aimProgress;
            poseStack.mulPose(Axis.ZP.rotationDegrees(25.0F * invertRealProgress * (right ? 1.0F : -1.0F)));
            poseStack.mulPose(Axis.YP.rotationDegrees((30.0F * invertRealProgress + aimProgress * -20.0F) * (right ? 1.0F : -1.0F)));
            poseStack.mulPose(Axis.XP.rotationDegrees(25.0F * invertRealProgress + aimProgress * 5.0F));
         }
      } else {
         super.applyHeldItemTransforms(player, hand, aimProgress, poseStack, buffer);
      }
   }

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
         ItemCooldowns tracker = Minecraft.getInstance().player.getCooldowns();
         float cooldown = tracker.getCooldownPercent(stack.getItem(), Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
         cooldown = (float)this.ease((double)cooldown);
         poseStack.translate(0.0F, -cooldown * 0.05F, -cooldown * 0.15F);
         poseStack.scale(0.5F, 0.5F, 0.5F);
         poseStack.translate(0.25 * (double)side, 0.0, 0.0);
         poseStack.translate((double)armWidth / 2.0 * 0.0625 * (double)side, 0.0, 0.0);
         poseStack.translate(-0.3 * (double)side, -0.1, -0.3);
         poseStack.mulPose(Axis.XP.rotationDegrees(80.0F));
         poseStack.mulPose(Axis.YP.rotationDegrees(15.0F * (float)(-side)));
         poseStack.mulPose(Axis.ZP.rotationDegrees(15.0F * (float)(-side)));
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

   private double ease(double x) {
      return 1.0 - Math.pow(1.0 - 2.0 * x, 4.0);
   }

   @Override
   public boolean applyOffhandTransforms(Player player, PlayerModel model, ItemStack stack, PoseStack poseStack, float partialTicks) {
      return GripType.applyBackTransforms(player, poseStack);
   }

   @Override
   protected AimPose getBanzaiPose() {
      AimPose banzaiPose = new AimPose();
      banzaiPose.getIdle()
         .setRenderYawOffset(0.0F)
         .setItemRotation(new Vector3f(0.0F, 0.0F, 0.0F))
         .setRightArm(new LimbPose().setRotationAngleX(-70.0F).setRotationAngleY(0.0F).setRotationPointX(0.0F).setRotationPointY(0.0F).setRotationPointZ(0.0F))
         .setLeftArm(new LimbPose().setRotationAngleX(-70.0F).setRotationAngleY(0.0F).setRotationPointX(0.0F).setRotationPointY(0.0F).setRotationPointZ(0.0F));
      banzaiPose.getAiming()
         .setRenderYawOffset(0.0F)
         .setItemRotation(new Vector3f(0.0F, 0.0F, 0.0F))
         .setRightArm(new LimbPose().setRotationAngleX(-70.0F).setRotationAngleY(0.0F).setRotationPointX(0.0F).setRotationPointY(0.0F).setRotationPointZ(0.0F))
         .setLeftArm(new LimbPose().setRotationAngleX(-70.0F).setRotationAngleY(0.0F).setRotationPointX(0.0F).setRotationPointY(0.0F).setRotationPointZ(0.0F));
      return banzaiPose;
   }
}
