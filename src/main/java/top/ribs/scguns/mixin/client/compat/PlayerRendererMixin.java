package top.ribs.scguns.mixin.client.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.ribs.scguns.client.compat.OptionalClientCompat;

/**
 * Hides the renderer's own first-person hand while a gun is held and Entity Model Features is installed.
 *
 * <p>Without EMF this mod's gun model supplies its own arms, and the vanilla hand is already suppressed elsewhere.
 * With EMF, Fresh Animations' player add-on re-enables an animated hand, which then draws on top of the gun. Only
 * that case is cancelled here.</p>
 */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {

   private static final class Probe {
      private static boolean logged;

      static void logIfFirst() {
         if (!logged) {
            logged = true;
            top.ribs.scguns.ScorchedGuns.LOGGER.info("scguns: PlayerRenderer#renderHand reached (EMF hand path is live).");
         }
      }
   }

   private static final Probe PROBE = new Probe();

   @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true, require = 0)
   private void scguns$hideHandWhenGunHeld(
      PoseStack poseStack, MultiBufferSource buffer, int light, AbstractClientPlayer player, ModelPart arm, ModelPart sleeve,
      CallbackInfo callbackInfo
   ) {
      // One log line per session: it is the only way to tell "the mixin never applied" (wrong method signature for
      // this Minecraft version) from "it applied and something redraws the hand afterwards".
      PROBE.logIfFirst();

      OptionalClientCompat.cancelFallbackFirstPersonHandWhenGunHeld(player, callbackInfo);
   }
}
