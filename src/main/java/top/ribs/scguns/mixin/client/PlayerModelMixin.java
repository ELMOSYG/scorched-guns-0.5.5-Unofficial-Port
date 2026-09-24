package top.ribs.scguns.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.ribs.scguns.client.handler.AimingHandler;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.item.WaraxeItem;

@Mixin({PlayerModel.class})
public class PlayerModelMixin<T extends LivingEntity> {
   public PlayerModelMixin() {
      super();
   }

   @Inject(
      method = {"setupAnim*"},
      at = {@At("TAIL")}
   )
   private void setupAnimTail(T entity, float animationPos, float animationSpeed, float animationBob, float deltaHeadYaw, float headPitch, CallbackInfo ci) {
      if (entity instanceof Player player) {
         PlayerModel model = (PlayerModel)(Object)this;
         ItemStack heldItem = player.getMainHandItem();
         if (heldItem.getItem() instanceof WaraxeItem waraxe) {
            // 0.5.5 read Minecraft#getDeltaFrameTime() (ticks elapsed this frame);
            // the 1.21 equivalent is DeltaTracker#getGameTimeDeltaTicks().
            float delta = Minecraft.getInstance().getTimer().getGameTimeDeltaTicks();
            HumanoidModel<LivingEntity> humanoidModel = (HumanoidModel<LivingEntity>)model;
            waraxe.applyHoldingPose(humanoidModel, animationBob, player, heldItem, delta);
            copyModelAngles(model.rightArm, model.rightSleeve);
            copyModelAngles(model.leftArm, model.leftSleeve);
         } else if (heldItem.getItem() instanceof GunItem gunItem) {
            // 0.5.5 bailed out here whenever the LOCAL player was standing still
            // -- setupAnim's second argument is limbSwing, and
            // LivingEntityRenderer passes walkAnimation.position() into it, so
            // it is exactly 0.0F once the player stops. That zeroed both arms and
            // skipped the gun pose below. In third person standing still is
            // precisely the case you look at after pressing F5, so the gun
            // appeared to be held like an ordinary item. The bail-out is kept
            // only for the camera it can matter in: first person, where the
            // player model's arms are not the ones on screen.
            if (player.isLocalPlayer() && animationPos == 0.0F
               && Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
               model.rightArm.xRot = 0.0F;
               model.rightArm.yRot = 0.0F;
               model.rightArm.zRot = 0.0F;
               model.leftArm.xRot = 0.0F;
               model.leftArm.yRot = 0.0F;
               model.leftArm.zRot = 0.0F;
               copyModelAngles(model.rightArm, model.rightSleeve);
               copyModelAngles(model.leftArm, model.leftSleeve);
               return;
            }

            if (player.isSwimming() || player.isFallFlying() || player.isVisuallySwimming()) {
               this.applySwimmingGunPose(player, model, gunItem, heldItem);
               return;
            }

            Gun gun = gunItem.getModifiedGun(heldItem);
            gun.getGeneral()
               .getGripType(heldItem)
               .heldAnimation()
               .applyPlayerModelRotation(
                  player,
                  model.rightArm,
                  model.leftArm,
                  model.head,
                  InteractionHand.MAIN_HAND,
                  AimingHandler.get().getAimProgress(player, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false))
               );
            copyModelAngles(model.rightArm, model.rightSleeve);
            copyModelAngles(model.leftArm, model.leftSleeve);
            copyModelAngles(model.head, model.hat);
         }
      }
   }

   private void applySwimmingGunPose(Player player, PlayerModel<T> model, GunItem gunItem, ItemStack heldItem) {
      float aimProgress = 0.0F;
      if (player.isLocalPlayer()) {
         aimProgress = AimingHandler.get().getAimProgress(player, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
      }

      model.rightArm.xRot = (float)Math.toRadians((double)(-160.0F + aimProgress * 10.0F));
      model.rightArm.yRot = (float)Math.toRadians(-15.0);
      model.rightArm.zRot = (float)Math.toRadians(10.0);
      model.leftArm.xRot = (float)Math.toRadians(-140.0);
      model.leftArm.yRot = (float)Math.toRadians(20.0);
      model.leftArm.zRot = (float)Math.toRadians(-15.0);
      copyModelAngles(model.rightArm, model.rightSleeve);
      copyModelAngles(model.leftArm, model.leftSleeve);
   }

   private static void copyModelAngles(ModelPart source, ModelPart target) {
      target.xRot = source.xRot;
      target.yRot = source.yRot;
      target.zRot = source.zRot;
   }
}
