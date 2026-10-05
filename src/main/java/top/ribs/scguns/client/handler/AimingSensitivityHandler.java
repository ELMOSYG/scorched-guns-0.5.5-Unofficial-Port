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
 * <p>The write itself is Tweakeroo's method (it does the same for its zoom with no mixin at all): remember
 * the player's value, write the scaled one while aiming, restore it afterwards. That part is proven - the
 * player reports it works with iron sights.</p>
 *
 * <p>What that report also pinned down: with a scope <em>attachment</em> the multiplier came out as exactly
 * 1.0. The cause was this class gating on {@code AimingHandler.isAiming()}, which reads false for scoped
 * aiming. The FOV zoom path - the one that demonstrably works with scopes - does not use that flag at all;
 * {@code AimingHandler.onFovUpdate} tests {@code getNormalisedAdsProgress() != 0.0}. This now uses the same
 * test in both places, so the sensitivity follows the same state as the zoom the player can already see.</p>
 *
 * <p>A temporary log prints the inputs once a second while a gun is held, so if the numbers are still wrong
 * the next round has data instead of another guess.</p>
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

      double multiplier = aimingSensitivityMultiplier();
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
            "SCGUNS-ADS multiplier={} isAiming={} progress={} zoom={} fov={} option={} saved={}",
            multiplier, handler.isAiming(), handler.getNormalisedAdsProgress(), zoom, fov,
            mc.options.sensitivity().get(), savedSensitivity);
      }
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
      // Same test the FOV zoom uses. isAiming() is false while aiming through a scope attachment, which is
      // what made the multiplier 1.0 for every scoped gun while iron sights worked.
      if (progress == 0.0) {
         return 1.0;
      }

      double adsSensitivity = (Double)Config.CLIENT.controls.aimDownSightSensitivity.get();
      return (1.0 - (1.0 - adsSensitivity) * progress) * (double)scopeSensitivityFactor(progress);
   }

   private static float scopeSensitivityFactor(double progress) {
      float factor = 1.0F;
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && !mc.player.getMainHandItem().isEmpty()
         && mc.options.getCameraType() == CameraType.FIRST_PERSON) {
         ItemStack heldItem = mc.player.getMainHandItem();
         if (heldItem.getItem() instanceof GunItem gunItem && progress != 0.0
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
