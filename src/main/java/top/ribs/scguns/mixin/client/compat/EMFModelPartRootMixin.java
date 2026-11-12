package top.ribs.scguns.mixin.client.compat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.ribs.scguns.client.compat.OptionalClientCompat;

/**
 * Stops Entity Model Features from animating the player while a gun is held.
 *
 * <p>EMF replaces and animates every player model part, so the pose this mod applies to {@code PlayerModel} is
 * overwritten before the model is drawn. Skipping EMF's animation for that case is the one approach that holds: the
 * vanilla chain then runs untouched and the gun pose lands where it always did. Other entities and unarmed players are
 * untouched, so a model pack still animates everything else.</p>
 *
 * <p>{@code require = 0} and {@code @Pseudo}: EMF is optional, and a mixin that cannot find its target because the mod
 * is absent must not abort the whole config.</p>
 */
@Mixin(targets = "traben.entity_model_features.models.parts.EMFModelPartRoot", remap = false)
@Pseudo
public abstract class EMFModelPartRootMixin {

   @Inject(method = "animate", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
   private void scguns$skipEmfAnimationForGun(CallbackInfo callbackInfo) {
      try {
         if (OptionalClientCompat.shouldSkipEmfPlayerAnimation()) {
            callbackInfo.cancel();
         }
      } catch (Throwable ignored) {
         // A failure here must not break the entity render.
      }
   }
}
