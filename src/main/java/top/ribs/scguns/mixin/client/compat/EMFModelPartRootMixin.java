package top.ribs.scguns.mixin.client.compat;

import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.ribs.scguns.client.compat.OptionalClientCompat;
import top.ribs.scguns.client.handler.AimingHandler;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.item.GunItem;

/**
 * Reapplies the gun's held pose after Entity Model Features has animated the player model.
 *
 * <p>EMF owns the player's {@code ModelPart}s and overwrites them every frame, so the pose this mod's
 * {@code PlayerModel} injection applies is gone before the model is drawn. This runs at the tail of EMF's own
 * animation step and re-applies the grip animation onto the vanilla parts EMF keeps around.</p>
 *
 * <p>{@code require = 0}: EMF is optional, and a mixin that cannot find its target because the mod is absent would
 * otherwise abort the whole config. {@code @Pseudo} keeps the class out of the refmap for the same reason.</p>
 */
@Mixin(targets = "traben.entity_model_features.models.parts.EMFModelPartRoot", remap = false)
@Pseudo
public abstract class EMFModelPartRootMixin {

   @Shadow(remap = false)
   @Final
   private Map<String, ModelPart> allVanillaParts;

   @Inject(method = "animate", at = @At("TAIL"), require = 0, remap = false)
   private void scguns$reapplyHeldPose(CallbackInfo callbackInfo) {
      try {
         if (OptionalClientCompat.isEntityModelFeaturesFirstPersonHand()) {
            return;
         }

         if (!(OptionalClientCompat.getEntityModelFeaturesContextEntity() instanceof Player player)) {
            return;
         }

         ItemStack stack = player.getMainHandItem();
         if (!(stack.getItem() instanceof GunItem gunItem)) {
            return;
         }

         ModelPart rightArm = this.allVanillaParts.get("right_arm");
         ModelPart leftArm = this.allVanillaParts.get("left_arm");
         ModelPart head = this.allVanillaParts.get("head");
         if (rightArm == null || leftArm == null || head == null) {
            return;
         }

         rightArm.resetPose();
         leftArm.resetPose();
         float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
         float aimProgress = AimingHandler.get().getAimProgress(player, partialTick);
         Gun gun = gunItem.getModifiedGun(stack);
         gun.getGeneral()
            .getGripType(stack)
            .heldAnimation()
            .applyPlayerModelRotation(player, rightArm, leftArm, head, InteractionHand.MAIN_HAND, aimProgress);

         copyPose(rightArm, this.allVanillaParts.get("right_sleeve"));
         copyPose(leftArm, this.allVanillaParts.get("left_sleeve"));
         copyPose(head, this.allVanillaParts.get("hat"));
         copyPose(head, this.allVanillaParts.get("headwear"));
      } catch (Throwable ignored) {
         // EMF's internals are not ours to depend on: a failure here must not break the entity render.
      }
   }

   private static void copyPose(ModelPart source, ModelPart target) {
      if (source != null && target != null) {
         target.copyFrom(source);
      }
   }
}
