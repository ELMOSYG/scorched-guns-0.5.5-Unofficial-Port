package top.ribs.scguns.client.handler;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import top.ribs.scguns.Config;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.init.ModSyncedDataKeys;
import top.ribs.scguns.item.GunItem;

/**
 * Scales mouse sensitivity while aiming down sights.
 *
 * <p>Method taken from Tweakeroo (shipped here as tweakerge), which does the same thing for its zoom
 * without a single mixin: it remembers the player's sensitivity, writes a scaled value into the vanilla
 * option while zooming, and puts the original back when zooming ends. That works because
 * {@code MouseHandler.turnPlayer} reads exactly that option - verified in the 1.21.1 bytecode, where the
 * event's sensitivity comes from {@code minecraft.options.sensitivity().get()}. Nothing is injected, so
 * there is no ordinal to drift and no event for another mod to overwrite.</p>
 *
 * <p>Why the earlier attempts failed: 0.5.5 modified a local variable of {@code turnPlayer()V} selected by
 * {@code ordinal = 2}; 1.21.1 gave that method a parameter, the ordinal shifted to a different local, and
 * the injection still applied while doing nothing. Retargeting the arguments of {@code Entity.turn} and
 * using NeoForge's {@code CalculatePlayerTurnEvent} were also reported as having no effect in game. This
 * route changes the value the game asks for in the first place.</p>
 *
 * <p>Called every client tick. The multiplier is always applied to the <em>remembered original</em>, never
 * to the current option value, so repeated ticks cannot compound. When not aiming the original is restored
 * immediately, so the option is at its normal value whenever the player is not aiming - including after a
 * disconnect, where the tick simply restores it because no player is present.</p>
 */
public final class AimingSensitivityHandler {
   /** The player's real sensitivity while we are overriding it; null means "not overriding". */
   private static Double savedSensitivity;

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
      AimingHandler handler = AimingHandler.get();
      // The ADS progress is the blend for the transition into aiming, not the aiming state itself. It reads
      // ~1.0 while standing still (measured in game: "aiming=false progress=1.0" for seconds on end), which
      // applied the configured factor permanently and left aiming with nothing but the scope term.
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
