package top.ribs.scguns.mixin.client;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import top.ribs.scguns.Config;
import top.ribs.scguns.client.handler.AimingHandler;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.init.ModSyncedDataKeys;
import top.ribs.scguns.item.GunItem;

/**
 * Scales mouse sensitivity while aiming down sights (HANDOFF section 64).
 *
 * <p>0.5.5 injected into {@code MouseHandler.turnPlayer()V} - a no-argument method in 1.20.1 - and
 * picked its variable by {@code DSTORE ordinal 2}. 1.21.1's method is {@code turnPlayer(double)} and
 * takes the already-scaled sensitivity as an argument, so the port kept the old {@code ordinal = 2},
 * which simply landed on a different local: the injection applied, the client started, and the
 * sensitivity was never actually changed. The upstream 1.21.1 port scales the two arguments of the
 * {@code LocalPlayer.turn(DD)V} call instead, which is where the value is finally used, and that is
 * what this does now. The maths is 0.5.5's, unchanged.</p>
 */
@Mixin({MouseHandler.class})
public abstract class MouseHandlerMixin {
   @ModifyArg(
      method = {"turnPlayer(D)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"
      ),
      index = 0
   )
   private double scguns$scaleAimingYaw(double original) {
      return original * this.scguns$getAimingSensitivityMultiplier();
   }

   @ModifyArg(
      method = {"turnPlayer(D)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"
      ),
      index = 1
   )
   private double scguns$scaleAimingPitch(double original) {
      return original * this.scguns$getAimingSensitivityMultiplier();
   }

   private double scguns$getAimingSensitivityMultiplier() {
      float additionalAdsSensitivity = 1.0F;
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && !mc.player.getMainHandItem().isEmpty()
         && mc.options.getCameraType() == CameraType.FIRST_PERSON) {
         ItemStack heldItem = mc.player.getMainHandItem();
         if (heldItem.getItem() instanceof GunItem gunItem && AimingHandler.get().isAiming()
            && !(Boolean)ModSyncedDataKeys.RELOADING.getValue(mc.player)) {
            Gun modifiedGun = gunItem.getModifiedGun(heldItem);
            if (modifiedGun.getModules().getZoom() != null) {
               float modifier = Gun.getFovModifier(heldItem, modifiedGun);
               modifier = Mth.clamp(modifier, 0.1F, 10.0F);
               additionalAdsSensitivity = Mth.clamp((float)Math.pow((double)modifier, 0.25), 0.5F, 1.0F);
            }
         }
      }

      double adsSensitivity = (Double)Config.CLIENT.controls.aimDownSightSensitivity.get();
      return (1.0 - (1.0 - adsSensitivity) * AimingHandler.get().getNormalisedAdsProgress())
         * (double)additionalAdsSensitivity;
   }
}
