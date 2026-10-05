package top.ribs.scguns.client.handler;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import top.ribs.scguns.Config;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.init.ModSyncedDataKeys;
import top.ribs.scguns.item.GunItem;

/**
 * The aiming-down-sights sensitivity maths, kept in one place.
 *
 * <p>0.5.5 applied this from a mixin on {@code MouseHandler.turnPlayer()V}, scaling the local that holds
 * the final turn multiplier. The port first retargeted that mixin twice (the 1.20.1 variable ordinal
 * pointed somewhere else once the method grew a parameter), then moved the scaling into NeoForge's
 * {@code CalculatePlayerTurnEvent} - which the game does read, but which any other listener may write
 * afterwards (Immersive Engineering writes to it too). The scaling is back in the mixin, where nothing
 * runs after it; only the maths lives here.</p>
 *
 * <p>Formula, unchanged from 0.5.5: the configured factor is blended in by the ADS progress, then
 * multiplied by a factor derived from the gun's FOV modifier when a scope is actually being aimed.</p>
 */
public final class AimingSensitivityHandler {
   private AimingSensitivityHandler() {
   }

   /** Multiplier to apply to the turn: 1.0 when not aiming, smaller while aiming down sights. */
   public static double aimingSensitivityMultiplier() {
      AimingHandler handler = AimingHandler.get();
      // The ADS progress is the blend for the transition into aiming, not the aiming state itself. It
      // reads ~1.0 while standing still (measured in game: "aiming=false progress=1.0" for seconds on
      // end), which applied the configured factor permanently and left aiming with nothing but the scope
      // term - so the sensitivity barely moved when the player aimed, and the player reported the whole
      // feature as never having worked. Gating it on the aiming flag restores 0.5.5's intent: not aiming
      // means exactly 1.0, no scaling at all.
      double progress = handler.isAiming() ? handler.getNormalisedAdsProgress() : 0.0;
      double adsSensitivity = (Double)Config.CLIENT.controls.aimDownSightSensitivity.get();
      return (1.0 - (1.0 - adsSensitivity) * progress) * (double)scopeSensitivityFactor();
   }

   private static float scopeSensitivityFactor() {
      float factor = 1.0F;
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && !mc.player.getMainHandItem().isEmpty()
         && mc.options.getCameraType() == CameraType.FIRST_PERSON) {
         ItemStack heldItem = mc.player.getMainHandItem();
         if (heldItem.getItem() instanceof GunItem gunItem && AimingHandler.get().isAiming()
            && !(Boolean)ModSyncedDataKeys.RELOADING.getValue(mc.player)) {
            Gun modifiedGun = gunItem.getModifiedGun(heldItem);
            if (modifiedGun.getModules().getZoom() != null) {
               float modifier = Mth.clamp(Gun.getFovModifier(heldItem, modifiedGun), 0.1F, 10.0F);
               factor = Mth.clamp((float)Math.pow((double)modifier, 0.25), 0.5F, 1.0F);
            }
         }
      }

      return factor;
   }
}
