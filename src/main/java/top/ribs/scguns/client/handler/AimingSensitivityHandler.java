package top.ribs.scguns.client.handler;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import top.ribs.scguns.Config;
import top.ribs.scguns.ScorchedGuns;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.init.ModSyncedDataKeys;
import top.ribs.scguns.item.GunItem;

/**
 * Scales mouse sensitivity while aiming down sights.
 *
 * <p>0.5.5 injected into {@code MouseHandler.turnPlayer()V} - a no-argument method in 1.20.1 - and picked
 * its variable by {@code DSTORE ordinal 2}. 1.21.1's method is {@code turnPlayer(double)} and, before it
 * uses the value, it asks NeoForge for the final one:</p>
 *
 * <pre>
 * MouseHandler.handleAccumulatedMovement()
 *   -&gt; turnPlayer(double)
 *        -&gt; ClientHooks.getTurnPlayerValues(double sensitivity, boolean cinematic)
 *             -&gt; new CalculatePlayerTurnEvent(sensitivity, cinematic)
 *                NeoForge.EVENT_BUS.post(event)
 *        -&gt; LocalPlayer.turn(double, double)
 * </pre>
 *
 * <p>So the sensitivity the client finally turns by is the one this event carries, and the supported
 * place to change it is here. The mixin this replaces retargeted the two arguments of
 * {@code LocalPlayer.turn} instead - after the fact - and the player confirmed in game that it had no
 * effect, which is why it is gone rather than adjusted again.</p>
 *
 * <p>The maths is 0.5.5's, unchanged: the configured factor is blended in by the aiming progress, then
 * multiplied by a scope factor derived from the gun's FOV modifier.</p>
 */
public final class AimingSensitivityHandler {
   private static long lastProbeLog;

   private AimingSensitivityHandler() {
   }

   @SubscribeEvent
   public static void onCalculatePlayerTurn(CalculatePlayerTurnEvent event) {
      double multiplier = aimingSensitivityMultiplier();
      if (multiplier != 1.0) {
         event.setMouseSensitivity(event.getMouseSensitivity() * multiplier);
      }

      // TEMPORARY PROBE (removed once the player confirms it works): the previous two attempts at this
      // compiled and changed nothing, so this prints what the handler actually computes, at most once a
      // second, and only while aiming.
      if (multiplier != 1.0) {
         long now = System.currentTimeMillis();
         if (now - lastProbeLog > 1000L) {
            lastProbeLog = now;
            ScorchedGuns.LOGGER.info(
               "SCGUNS-ADS sensitivityIn={} multiplier={} aiming={} progress={} fov={}",
               event.getMouseSensitivity(), multiplier, AimingHandler.get().isAiming(),
               AimingHandler.get().getNormalisedAdsProgress(), fovModifierOfHeldGun());
         }
      }
   }

   private static double aimingSensitivityMultiplier() {
      double adsSensitivity = (Double)Config.CLIENT.controls.aimDownSightSensitivity.get();
      return (1.0 - (1.0 - adsSensitivity) * AimingHandler.get().getNormalisedAdsProgress())
         * (double)scopeSensitivityFactor();
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

   private static float fovModifierOfHeldGun() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         return 1.0F;
      }

      ItemStack heldItem = mc.player.getMainHandItem();
      return heldItem.getItem() instanceof GunItem gunItem
         ? Gun.getFovModifier(heldItem, gunItem.getModifiedGun(heldItem))
         : 1.0F;
   }
}
