package top.ribs.scguns.client.handler;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import top.ribs.scguns.Config;
import top.ribs.scguns.ScorchedGuns;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.init.ModSyncedDataKeys;
import top.ribs.scguns.item.GunItem;

/**
 * Scales mouse sensitivity while aiming down sights by writing the vanilla sensitivity option.
 *
 * <p>Tweakeroo's method, no mixin: remember the player's sensitivity, write the scaled value while aiming,
 * put the original back afterwards.</p>
 *
 * <p>Two conditions had to be got right, and each was wrong once:</p>
 * <ul>
 *   <li>{@code AimingHandler.isAiming()} reads false while aiming through a scope attachment, so gating on
 *       it made every scoped gun use 1.0. The FOV zoom path does not test it either. The aiming test below
 *       is the one {@code AimTracker.handleAiming} itself uses to drive the animation.</li>
 *   <li>Deciding to restore when the ADS progress reached 0 left the sensitivity scaled forever: the
 *       progress does not reliably decay back to zero (its decrement uses the held item's ADS speed, which
 *       can be 0), which is also why the logs showed "aiming=false progress=1.0" for seconds on end. The
 *       aiming test used here does not depend on that value, so releasing the aim always restores.</li>
 * </ul>
 *
 * <p>A temporary log prints the inputs once a second while a gun is held.</p>
 */
public final class AimingSensitivityHandler {
   /** The player's real sensitivity while we are overriding it; null means "not overriding". */
   private static Double savedSensitivity;
   private static long lastProbe;

   private AimingSensitivityHandler() {
   }

   /** Runs each client tick; safe to call before a world is loaded. */
   public static void onClientTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.options == null) {
         restore(mc);
         return;
      }

      double multiplier = aimingDownSights(mc) ? aimingSensitivityMultiplier() : 1.0;
      if (multiplier == 1.0) {
         restore(mc);
      } else {
         apply(mc, multiplier);
      }

      long now = System.currentTimeMillis();
      if (now - lastProbe > 1000L) {
         lastProbe = now;
         AimingHandler handler = AimingHandler.get();
         ItemStack held = mc.player.getMainHandItem();
         String zoom = "n/a";
         float fov = 1.0F;
         if (held.getItem() instanceof GunItem gun) {
            Gun modified = gun.getModifiedGun(held);
            zoom = modified.getModules().getZoom() == null ? "null" : "present";
            fov = Gun.getFovModifier(held, modified);
         }
         ScorchedGuns.LOGGER.info(
            "SCGUNS-ADS multiplier={} ads={} aiming={} isAiming={} progress={} zoom={} fov={} option={} saved={}",
            multiplier, aimingDownSights(mc), (Boolean)ModSyncedDataKeys.AIMING.getValue(mc.player),
            handler.isAiming(), handler.getNormalisedAdsProgress(), zoom, fov,
            mc.options.sensitivity().get(), savedSensitivity);
      }
   }

   /**
    * Whether the player is aiming down sights, using the condition {@code AimTracker.handleAiming} uses to
    * drive the ADS animation: the synced aiming key, or the local handler's own aiming state.
    */
   private static boolean aimingDownSights(Minecraft mc) {
      return (Boolean)ModSyncedDataKeys.AIMING.getValue(mc.player) || AimingHandler.get().isAiming();
   }

   private static void apply(Minecraft mc, double multiplier) {
      OptionInstance<Double> sensitivity = mc.options.sensitivity();
      if (savedSensitivity == null) {
         savedSensitivity = sensitivity.get();
      }

      sensitivity.set(Mth.clamp(savedSensitivity * multiplier, 0.0, 1.0));
   }

   private static void restore(Minecraft mc) {
      if (savedSensitivity == null) {
         return;
      }

      if (mc.options != null) {
         mc.options.sensitivity().set(savedSensitivity);
      }
      savedSensitivity = null;
   }

   /** Multiplier for the sensitivity option: exactly 1.0 when not aiming down sights. */
   public static double aimingSensitivityMultiplier() {
      double progress = AimingHandler.get().getNormalisedAdsProgress();
      double adsSensitivity = (Double)Config.CLIENT.controls.aimDownSightSensitivity.get();
      return (1.0 - (1.0 - adsSensitivity) * progress) * (double)scopeSensitivityFactor();
   }

   private static float scopeSensitivityFactor() {
      float factor = 1.0F;
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && !mc.player.getMainHandItem().isEmpty()
         && mc.options.getCameraType() == CameraType.FIRST_PERSON) {
         ItemStack heldItem = mc.player.getMainHandItem();
         if (heldItem.getItem() instanceof GunItem gunItem
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
