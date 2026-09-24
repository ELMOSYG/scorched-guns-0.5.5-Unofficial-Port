package top.ribs.scguns.client.handler;


import net.minecraft.client.resources.PlayerSkin;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RenderPlayerEvent.Post;
import net.neoforged.neoforge.client.event.RenderPlayerEvent.Pre;
import net.neoforged.bus.api.SubscribeEvent;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.item.GunItem;

public class PlayerModelHandler {
   public PlayerModelHandler() {
      super();
   }

   @SubscribeEvent
   public void onRenderPlayer(Pre event) {
      Player player = event.getEntity();
      ItemStack heldItem = player.getMainHandItem();
      GunRenderingHandler renderingHandler = GunRenderingHandler.get();
      if (renderingHandler.isThirdPersonMeleeAttacking()) {
         this.applyCustomArmTransforms(event.getPoseStack(), event.getPartialTick(), (PlayerModel<AbstractClientPlayer>)event.getRenderer().getModel(), player);
      } else if (!heldItem.isEmpty() && heldItem.getItem() instanceof GunItem) {
         Gun gun = ((GunItem)heldItem.getItem()).getModifiedGun(heldItem);
         gun.getGeneral()
            .getGripType(heldItem)
            .heldAnimation()
            .applyPlayerPreRender(
               player,
               InteractionHand.MAIN_HAND,
               AimingHandler.get().getAimProgress(event.getEntity(), event.getPartialTick()),
               event.getPoseStack(),
               event.getMultiBufferSource()
            );
      }
   }

   @SubscribeEvent
   public void onRenderPlayer(Post event) {
      PlayerModel<AbstractClientPlayer> model = (PlayerModel<AbstractClientPlayer>)event.getRenderer().getModel();
      boolean slim = ((AbstractClientPlayer)event.getEntity()).getSkin().model() == PlayerSkin.Model.SLIM;
      model.rightArm.x = -5.0F;
      model.rightArm.y = slim ? 2.5F : 2.0F;
      model.rightArm.z = 0.0F;
      model.leftArm.x = 5.0F;
      model.leftArm.y = slim ? 2.5F : 2.0F;
      model.leftArm.z = 0.0F;
   }

   private void applyCustomArmTransforms(PoseStack poseStack, float partialTicks, PlayerModel<AbstractClientPlayer> model, Player player) {
      GunRenderingHandler renderingHandler = GunRenderingHandler.get();
      float progress = Mth.lerp(partialTicks, renderingHandler.prevThirdPersonMeleeProgress, renderingHandler.thirdPersonMeleeProgress);
      poseStack.pushPose();
      model.rightArm.translateAndRotate(poseStack);
      if (progress < 0.33F) {
         float raiseProgress = progress / 0.33F;
         poseStack.translate(0.0, 0.35 * (double)raiseProgress, 0.0);
         poseStack.translate(0.0, 0.0, 0.1 * (double)raiseProgress);
         poseStack.mulPose(Axis.XP.rotationDegrees(35.0F * raiseProgress));
      } else if (progress < 0.66F) {
         float swingProgress = (progress - 0.33F) / 0.33F;
         poseStack.translate(0.0, 0.35 - 0.7 * (double)swingProgress, 0.0);
         poseStack.translate(0.0, 0.1 - 0.2 * (double)swingProgress, 0.0);
         poseStack.mulPose(Axis.XP.rotationDegrees(35.0F - 70.0F * swingProgress));
      } else {
         float returnProgress = (progress - 0.66F) / 0.34F;
         poseStack.translate(0.0, -0.35 * (double)(1.0F - returnProgress), 0.0);
         poseStack.translate(0.0, 0.0, -0.1 * (double)(1.0F - returnProgress));
         poseStack.mulPose(Axis.XP.rotationDegrees(-35.0F * (1.0F - returnProgress)));
      }

      model.rightArm.translateAndRotate(poseStack);
      poseStack.popPose();
   }
}
