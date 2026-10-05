package top.ribs.scguns.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import top.ribs.scguns.client.handler.AimingSensitivityHandler;

/**
 * Scales mouse sensitivity while aiming down sights by scaling the look deltas as they are applied.
 *
 * <p>Earlier attempts hooked {@code MouseHandler.turnPlayer} and picked a local variable by ordinal. That
 * is exactly the kind of target that breaks silently: 1.20.1's {@code turnPlayer()V} had no parameters, so
 * {@code ordinal = 2} meant the third double stored; 1.21.1's {@code turnPlayer(double)} added a parameter
 * and the same ordinal landed elsewhere, the injection still applied, and nothing changed. Two rounds were
 * spent on that.</p>
 *
 * <p>This targets {@code Entity.turn(double, double)} instead - a two-argument method whose parameters are
 * the final yaw and pitch deltas, with no call site or ordinal ambiguity ({@code argsOnly} limits the
 * candidates to the parameters, and index 0/1 picks yaw and pitch). The previous round's temporary probe
 * injected into this very method and logged successfully, so the join point is known to work. Only the
 * local player is scaled; every other entity passes through untouched.</p>
 */
@Mixin(Entity.class)
public abstract class EntityTurnSensitivityMixin {
   @ModifyVariable(method = "turn(DD)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
   private double scguns$scaleAimingYaw(double yaw) {
      if ((Object)this != Minecraft.getInstance().player) {
         return yaw;
      }
      double multiplier = AimingSensitivityHandler.aimingSensitivityMultiplier();
      return multiplier == 1.0 ? yaw : yaw * multiplier;
   }

   @ModifyVariable(method = "turn(DD)V", at = @At("HEAD"), argsOnly = true, ordinal = 1)
   private double scguns$scaleAimingPitch(double pitch) {
      if ((Object)this != Minecraft.getInstance().player) {
         return pitch;
      }
      double multiplier = AimingSensitivityHandler.aimingSensitivityMultiplier();
      return multiplier == 1.0 ? pitch : pitch * multiplier;
   }
}
