package top.ribs.scguns.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.ribs.scguns.client.handler.AimingHandler;
import top.ribs.scguns.client.handler.GunRenderingHandler;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.item.animated.AnimatedGunItem;

/**
 * Player-side gun rendering in third person.  Deliberately PLAYER-ONLY, exactly
 * as in 0.5.5.
 *
 * <p>An earlier revision of this port copied the 0.4.7-based NeoForge port's
 * approach here and added an else-branch that also took over non-player mobs,
 * positioning their gun with {@code translateToHand} and a few hard-coded
 * rotations.  That is not what the mod does: 0.5.5 leaves mobs to vanilla's
 * {@link ItemInHandLayer#renderArmWithItem}, which draws the gun through its own
 * renderer with the item model's ordinary third-person transform, while the
 * mob's model poses the arms in {@code setupAnim}.  The added branch replaced
 * that with a static, gun-agnostic placement and made mobs look like they were
 * not holding the gun properly.  It is gone; mobs get the shipped behaviour.</p>
 */
@Mixin({ItemInHandLayer.class})
public class ItemInHandLayerMixin {
   public ItemInHandLayerMixin() {
      super();
   }

   @Inject(
      method = {"renderArmWithItem"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void renderArmWithItemHead(
      LivingEntity entity,
      ItemStack stack,
      ItemDisplayContext display,
      HumanoidArm arm,
      PoseStack poseStack,
      MultiBufferSource source,
      int light,
      CallbackInfo ci
   ) {
      if (entity.getType() == EntityType.PLAYER) {
         InteractionHand hand = Minecraft.getInstance().options.mainHand().get() == arm ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
         if (hand == InteractionHand.OFF_HAND) {
            if (stack.getItem() instanceof GunItem) {
               ci.cancel();
               return;
            }

            if (entity.getMainHandItem().getItem() instanceof GunItem gunItem) {
               Gun modifiedGun = gunItem.getModifiedGun(entity.getMainHandItem());
               if (!modifiedGun.getGeneral().getGripType(stack).heldAnimation().canRenderOffhandItem()) {
                  ci.cancel();
                  return;
               }
            }
         }

         if (stack.getItem() instanceof GunItem gunItemx) {
            ci.cancel();
            PlayerItemInHandLayer<?, ?> layer = (PlayerItemInHandLayer<?, ?>)(Object)this;
            mrCrayfishGunMod$renderArmWithGun(
               layer, (Player)entity, stack, gunItemx, display, hand, arm, poseStack, source, light, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false)
            );
         }
      }
   }

   @Unique
   private static void mrCrayfishGunMod$renderArmWithGun(
      PlayerItemInHandLayer<?, ?> layer,
      Player player,
      ItemStack stack,
      GunItem item,
      ItemDisplayContext display,
      InteractionHand hand,
      HumanoidArm arm,
      PoseStack poseStack,
      MultiBufferSource source,
      int light,
      float deltaTicks
   ) {
      poseStack.pushPose();
      ((ArmedModel)layer.getParentModel()).translateToHand(arm, poseStack);
      poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
      poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
      poseStack.translate((double)((float)(arm == HumanoidArm.LEFT ? -1 : 1) / 16.0F), 0.125, -0.625);
      GunRenderingHandler.get().applyWeaponScale(stack, poseStack);
      Gun gun = item.getModifiedGun(stack);
      gun.getGeneral()
         .getGripType(stack)
         .heldAnimation()
         .applyHeldItemTransforms(player, hand, AimingHandler.get().getAimProgress(player, deltaTicks), poseStack, source);
      if (stack.getItem() instanceof AnimatedGunItem) {
         poseStack.scale(1.0F, 1.0F, 1.0F);
      }

      GunRenderingHandler.get().renderWeapon(player, stack, display, poseStack, source, light, deltaTicks);
      poseStack.popPose();
   }
}
