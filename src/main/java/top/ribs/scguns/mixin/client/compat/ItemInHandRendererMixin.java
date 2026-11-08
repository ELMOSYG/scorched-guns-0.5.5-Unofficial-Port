package top.ribs.scguns.mixin.client.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.ribs.scguns.client.compat.OptionalClientCompat;

/**
 * Hides the vanilla first-person hands while a gun is held and Entity Model Features is installed.
 *
 * <p>This is the method Entity Model Features actually hijacks: its {@code Mixin_WrapFirstPersonHandRender} wraps
 * {@code ItemInHandRenderer#renderHandsWithItems} and draws the resource pack's animated first-person arm from there.
 * Cancelling {@code PlayerRenderer#renderHand} was useless, because that method is never reached on this path. A gun
 * draws its own arms inside the gun model, so with a gun in hand the hand pass is redundant and its extra arm is what
 * players see floating next to the weapon.</p>
 *
 * <p>{@code require = 0}: EMF is optional and the signature is vanilla, so a future change must degrade to "no
 * suppression" rather than fail the whole mixin config.</p>
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

   @Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true, require = 0)
   private void scguns$skipVanillaHandsForGun(
      float partialTicks, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, LocalPlayer player, int light,
      CallbackInfo callbackInfo
   ) {
      if (OptionalClientCompat.shouldSuppressVanillaHands(player)) {
         OptionalClientCompat.logHandSuppressionOnce();
         callbackInfo.cancel();
      }
   }
}
