package top.ribs.scguns.mixin.client;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import top.ribs.scguns.ScorchedGuns;
import top.ribs.scguns.client.handler.AimingSensitivityHandler;

/**
 * Scales mouse sensitivity while aiming down sights, the way 0.5.5 did it.
 *
 * <p>1.20.1's {@code MouseHandler.turnPlayer()V} stored three doubles in a row - the sensitivity curve
 * value, its cube, and the final turn multiplier - and 0.5.5 modified the third by {@code ordinal = 2}.
 * 1.21.1's method takes the sensitivity as a parameter and stores the same three values, so the same
 * ordinal still lands on the turn multiplier or on the cube it is derived from; either way the value
 * that multiplies {@code accumulatedDX} ends up scaled. Doing it here rather than in NeoForge's
 * {@code CalculatePlayerTurnEvent} matters: that event is written by other mods too (Immersive
 * Engineering writes to it), so a listener's value can be overwritten, while nothing runs after this
 * store inside {@code turnPlayer}.</p>
 *
 * <p>The log line is a TEMPORARY probe: 0.5.5's variable ordinal silently missed its target in an
 * earlier port attempt, so this prints what it actually scaled, at most once a second.</p>
 */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
   private static long scguns$lastProbeLog;

   @ModifyVariable(method = "turnPlayer(D)V", at = @At("STORE"), ordinal = 2)
   private double scguns$scaleAimingTurn(double original) {
      double multiplier = AimingSensitivityHandler.aimingSensitivityMultiplier();
      double scaled = original * multiplier;

      long now = System.currentTimeMillis();
      if (multiplier != 1.0 && now - scguns$lastProbeLog > 1000L) {
         scguns$lastProbeLog = now;
         ScorchedGuns.LOGGER.info("SCGUNS-MIXIN turnMultiplierIn={} multiplier={} turnMultiplierOut={}",
            original, multiplier, scaled);
      }

      return scaled;
   }
}
