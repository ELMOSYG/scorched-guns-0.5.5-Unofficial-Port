package top.ribs.scguns.mixin.client.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.ribs.scguns.client.compat.OptionalClientCompat;

/**
 * Skips the renderer's own first-person arm while a gun is held.
 *
 * <p>Not {@code renderHandsWithItems}: that method draws the held item as well, so cancelling it made the gun and the
 * arms disappear together. The arm alone is drawn by {@code renderPlayerArm}, which is the one thing a gun model
 * already provides for itself - and the thing model packs (Entity Model Features, Fresh Extensions) add on top, which
 * is what players saw as a third arm.</p>
 *
 * <p>{@code require = 0}: the signature is vanilla and may change; failing to find it must degrade to "arm drawn as
 * usual", never to a broken mixin config.</p>
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

   @Inject(method = "renderPlayerArm", at = @At("HEAD"), cancellable = true, require = 0)
   private void scguns$skipVanillaArmForGun(
      PoseStack poseStack, MultiBufferSource bufferSource, int light, float equipProgress, float swingProgress,
      HumanoidArm arm, CallbackInfo callbackInfo
   ) {
      if (OptionalClientCompat.shouldSuppressVanillaHands(null)) {
         OptionalClientCompat.logHandSuppressionOnce();
         callbackInfo.cancel();
      }
   }
}
