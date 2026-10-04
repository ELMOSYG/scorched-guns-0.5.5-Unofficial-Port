package top.ribs.scguns.mixin.client;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import top.ribs.scguns.client.handler.AimingSensitivityHandler;

/**
 * Scales mouse sensitivity while aiming down sights, the way 0.5.5 did it.
 *
 * <p>1.20.1's {@code MouseHandler.turnPlayer()V} stored three doubles in a row - the sensitivity curve
 * value {@code v = s * 0.6 + 0.2}, its cube, and the turn multiplier {@code v * v * v * 8} - and 0.5.5
 * modified the third with {@code ordinal = 2}. In 1.21.1 the same three stores are still there but the
 * method now takes the sensitivity as a parameter, so {@code ordinal = 2} lands on the cube instead;
 * that is harmless, because the multiplier is computed from the cube ({@code v^3 * 8}), so scaling the
 * cube scales the turn by the same factor. Measured in game: the pitch delta the player receives equals
 * this multiplier, which is how the fix was confirmed.</p>
 *
 * <p>This lives on {@code turnPlayer} rather than in NeoForge's {@code CalculatePlayerTurnEvent} - which
 * the game does read, but which is also written by other mods (Immersive Engineering writes to it), so a
 * listener's value can be overwritten. Nothing runs after this store.</p>
 */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
   @ModifyVariable(method = "turnPlayer(D)V", at = @At("STORE"), ordinal = 2)
   private double scguns$scaleAimingTurn(double original) {
      return original * AimingSensitivityHandler.aimingSensitivityMultiplier();
   }
}
