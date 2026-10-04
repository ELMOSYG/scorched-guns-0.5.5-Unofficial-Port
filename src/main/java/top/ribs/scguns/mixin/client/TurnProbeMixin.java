package top.ribs.scguns.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.ribs.scguns.ScorchedGuns;
import top.ribs.scguns.client.handler.AimingHandler;

/**
 * TEMPORARY PROBE - delete once the aiming-sensitivity question is settled.
 *
 * <p>The analysis is exhausted: {@code CalculatePlayerTurnEvent} carries the sensitivity, our handler
 * scales it, and {@code MouseHandler.turnPlayer} turns it into {@code v = s * 0.6 + 0.2},
 * {@code v * v * v * 8} and calls {@code Entity.turn} with {@code accumulatedDX * that}. Nothing in
 * between reads the raw option again, so the turn rate should differ by roughly three times between
 * aiming and not aiming. The player reports no difference, so instead of another round of reasoning
 * this measures the value {@code turn} actually receives, once a second, together with the aiming
 * state.</p>
 *
 * <p>Targeted at {@code Entity} because that is where {@code turn(DD)V} is declared - targeting
 * {@code LocalPlayer} made the injection fail outright. Only the client player is logged; this mixin
 * stays in the {@code client} list, so it never loads on a dedicated server.</p>
 */
@Mixin(Entity.class)
public abstract class TurnProbeMixin {
   private static long scguns$lastTurnLog;

   @Inject(method = "turn(DD)V", at = @At("HEAD"))
   private void scguns$probeTurn(double yaw, double pitch, CallbackInfo callback) {
      if ((Object)this != Minecraft.getInstance().player) {
         return;
      }

      long now = System.currentTimeMillis();
      if (now - scguns$lastTurnLog < 1000L) {
         return;
      }

      scguns$lastTurnLog = now;
      ScorchedGuns.LOGGER.info(
         "SCGUNS-TURN yaw={} pitch={} aiming={} progress={}",
         yaw, pitch, AimingHandler.get().isAiming(), AimingHandler.get().getNormalisedAdsProgress());
   }
}
