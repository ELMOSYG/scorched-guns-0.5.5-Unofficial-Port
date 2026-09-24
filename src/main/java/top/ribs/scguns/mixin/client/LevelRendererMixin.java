package top.ribs.scguns.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.ribs.scguns.client.handler.BulletTrailRenderingHandler;
import top.ribs.scguns.client.handler.TurretBulletTrailRenderingHandler;

@Mixin({LevelRenderer.class})
public class LevelRendererMixin {
   public LevelRendererMixin() {
      super();
   }

   @Inject(
      method = {"renderLevel"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/LevelRenderer;checkPoseStack(Lcom/mojang/blaze3d/vertex/PoseStack;)V",
         ordinal = 0
      )}
   )
   private void renderBullets(
      DeltaTracker deltaTracker,
      boolean renderBlockOutline,
      Camera info,
      GameRenderer gameRenderer,
      LightTexture lightTexture,
      Matrix4f frustumMatrix,
      Matrix4f projection,
      CallbackInfo ci
   ) {
      float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(false);
      // 1.21's renderLevel has no PoseStack parameter. checkPoseStack() has just
      // asserted that the level's own stack is back at its base state, so it is the
      // identity at this point and a fresh stack is equivalent.
      PoseStack stack = new PoseStack();
      BulletTrailRenderingHandler.get().render(stack, partialTicks);
      TurretBulletTrailRenderingHandler.get().render(stack, partialTicks);
   }
}
