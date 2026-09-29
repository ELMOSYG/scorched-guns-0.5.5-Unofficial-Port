package top.ribs.scguns.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.ribs.scguns.client.handler.HeadshotBoxDebugRenderer;

/**
 * Draws the mod's headshot box alongside the entity box while F3+B is on (HANDOFF section 83.8).
 *
 * <p>Injected into {@code EntityRenderDispatcher.renderHitbox}, the private static method vanilla
 * calls once per entity from inside its F3+B pass. TAIL rather than HEAD so the mod's box is drawn
 * after the entity's own, with the pose already translated to the entity and the line buffer
 * already acquired.</p>
 *
 * <p>Inside a vanilla class rather than on a level-render event, on purpose: this way the box
 * appears and disappears with the key, costs nothing while the view is off, and cannot end up
 * drawn twice the way the bullet trails once did (HANDOFF 82.12).</p>
 *
 * <p>The target is written as an import plus a simple name, like every other mixin here, so
 * {@code tools/audit_mixins.py} can resolve it against the 1.21.1 sources and check the injection
 * point and the callback's signature. A fully qualified target parses fine and leaves the audit
 * blind to this file, which is worth avoiding.</p>
 */
@Mixin({EntityRenderDispatcher.class})
public class EntityRenderDispatcherMixin {
   public EntityRenderDispatcherMixin() {
      super();
   }

   /**
    * Static, because the target is: {@code renderHitbox} is {@code private static}, and Mixin
    * refuses an instance injector into a static target. A compile cannot catch that - it fails when
    * the client loads the mixin, which a dedicated-server smoke test never reaches (HANDOFF 83.8,
    * where this very file got it wrong first). {@code audit_mixins.py} now checks it both ways.
    */
   @Inject(
      method = {"renderHitbox"},
      at = {@At(value = "TAIL")}
   )
   private static void scgunsDrawHeadshotBox(
      PoseStack stack,
      VertexConsumer consumer,
      Entity entity,
      float red,
      float green,
      float blue,
      float alpha,
      CallbackInfo ci
   ) {
      HeadshotBoxDebugRenderer.render(stack, consumer, entity, entity.getBoundingBox());
   }
}
