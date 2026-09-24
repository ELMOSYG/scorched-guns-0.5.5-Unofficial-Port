package top.ribs.scguns.mixin.client;

import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.ribs.scguns.client.handler.AimingHandler;
import top.ribs.scguns.client.render.pose.AimPose;
import top.ribs.scguns.client.render.pose.LimbPose;
import top.ribs.scguns.client.render.pose.WeaponPose;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.event.VehicleDetectionHandler;
import top.ribs.scguns.item.GunItem;

@Mixin(
   value = {PlayerModel.class},
   priority = 1100
)
public class VehiclePoseMixin<T extends LivingEntity> {
   public VehiclePoseMixin() {
      super();
   }

   @Inject(
      method = {"setupAnim*"},
      at = {@At("RETURN")},
      cancellable = false
   )
   private void applyVehicleGunPoses(
      T entity, float animationPos, float animationSpeed, float animationBob, float deltaHeadYaw, float headPitch, CallbackInfo ci
   ) {
      if (entity instanceof Player player) {
         if (VehicleDetectionHandler.isPlayerInVehicle(player)) {
            ItemStack heldItem = player.getMainHandItem();
            if (heldItem.getItem() instanceof GunItem gunItem) {
               PlayerModel var14 = (PlayerModel)(Object)this;
               float aimProgress = 0.0F;
               if (player.isLocalPlayer()) {
                  aimProgress = AimingHandler.get().getAimProgress(player, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
               }

               Gun gun = gunItem.getModifiedGun(heldItem);
               this.applyAdaptedPose(gun, heldItem, var14, aimProgress);
               copyModelAngles(var14.rightArm, var14.rightSleeve);
               copyModelAngles(var14.leftArm, var14.leftSleeve);
               copyModelAngles(var14.head, var14.hat);
            }
         }
      }
   }

   private void applyAdaptedPose(Gun gun, ItemStack stack, PlayerModel<T> model, float aimProgress) {
      try {
         if (gun.getGeneral().getGripType(stack).heldAnimation() instanceof WeaponPose weaponPose) {
            AimPose forwardPose = this.getForwardPose(weaponPose);
            if (forwardPose != null) {
               this.applyModifiedPose(forwardPose, model.rightArm, model.leftArm, aimProgress);
               return;
            }
         }
      } catch (Exception var8) {
      }

      this.applyFallbackPose(model.rightArm, model.leftArm, aimProgress);
   }

   private AimPose getForwardPose(WeaponPose weaponPose) {
      try {
         Method getForwardPoseMethod = WeaponPose.class.getDeclaredMethod("getForwardPose");
         getForwardPoseMethod.setAccessible(true);
         return (AimPose)getForwardPoseMethod.invoke(weaponPose);
      } catch (Exception var3) {
         return null;
      }
   }

   private void applyModifiedPose(AimPose forwardPose, ModelPart rightArm, ModelPart leftArm, float aimProgress) {
      AimPose.Instance idlePose = forwardPose.getIdle();
      AimPose.Instance aimingPose = forwardPose.getAiming();
      LimbPose rightArmIdle = idlePose.getRightArm();
      LimbPose rightArmAiming = aimingPose != null ? aimingPose.getRightArm() : rightArmIdle;
      this.applyLimbPose(rightArmIdle, rightArmAiming, rightArm, aimProgress, true);
      LimbPose leftArmIdle = idlePose.getLeftArm();
      LimbPose leftArmAiming = aimingPose != null ? aimingPose.getLeftArm() : leftArmIdle;
      this.applyLimbPose(leftArmIdle, leftArmAiming, leftArm, aimProgress, false);
   }

   private void applyLimbPose(LimbPose idlePose, LimbPose aimingPose, ModelPart modelPart, float aimProgress, boolean isRightArm) {
      float rotX = this.interpolate(idlePose.getRotationAngleX(), aimingPose.getRotationAngleX(), aimProgress);
      float rotY = this.interpolate(idlePose.getRotationAngleY(), aimingPose.getRotationAngleY(), aimProgress);
      float rotZ = this.interpolate(idlePose.getRotationAngleZ(), aimingPose.getRotationAngleZ(), aimProgress);
      if (isRightArm) {
         rotX = Math.max(rotX, -120.0F);
         modelPart.xRot = (float)Math.toRadians((double)rotX);
         modelPart.yRot = (float)Math.toRadians((double)(Math.min(Math.abs(rotY), 15.0F) * Math.signum(rotY)));
         modelPart.zRot = (float)Math.toRadians((double)(rotZ * 0.5F));
      } else {
         modelPart.xRot = (float)Math.toRadians(-20.0);
         modelPart.yRot = (float)Math.toRadians(15.0);
         modelPart.zRot = 0.0F;
      }

      if (idlePose.getRotationPointX() != null) {
         modelPart.x = this.interpolate(
            idlePose.getRotationPointX(), aimingPose.getRotationPointX() != null ? aimingPose.getRotationPointX() : idlePose.getRotationPointX(), aimProgress
         );
      }

      if (idlePose.getRotationPointY() != null) {
         modelPart.y = this.interpolate(
            idlePose.getRotationPointY(), aimingPose.getRotationPointY() != null ? aimingPose.getRotationPointY() : idlePose.getRotationPointY(), aimProgress
         );
      }

      if (idlePose.getRotationPointZ() != null) {
         modelPart.z = this.interpolate(
            idlePose.getRotationPointZ(), aimingPose.getRotationPointZ() != null ? aimingPose.getRotationPointZ() : idlePose.getRotationPointZ(), aimProgress
         );
      }
   }

   private float interpolate(Float start, Float end, float progress) {
      if (start == null) {
         start = 0.0F;
      }

      if (end == null) {
         end = start;
      }

      return start + (end - start) * progress;
   }

   private void applyFallbackPose(ModelPart rightArm, ModelPart leftArm, float aimProgress) {
      rightArm.xRot = (float)Math.toRadians(-80.0);
      rightArm.yRot = (float)Math.toRadians(-10.0);
      rightArm.zRot = 0.0F;
      leftArm.xRot = (float)Math.toRadians(-20.0);
      leftArm.yRot = (float)Math.toRadians(15.0);
      leftArm.zRot = 0.0F;
   }

   private static void copyModelAngles(ModelPart source, ModelPart target) {
      target.xRot = source.xRot;
      target.yRot = source.yRot;
      target.zRot = source.zRot;
   }
}
