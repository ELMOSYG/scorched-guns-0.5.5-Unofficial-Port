package top.ribs.scguns.mixin.common;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.ribs.scguns.Config;

/**
 * Optionally drops the vanilla red damage particles for every entity.
 *
 * <p>Clients draw those particles when the server broadcasts entity event 2, so cancelling the broadcast removes them
 * server wide without touching any client or any damage number. Off by default; the switch lives in the server
 * config section so a server owner controls it, not each client.</p>
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelHurtParticlesMixin {

   @Inject(method = "broadcastEntityEvent", at = @At("HEAD"), cancellable = true)
   private void scguns$dropHurtParticles(Entity entity, byte event, CallbackInfo callbackInfo) {
      if (event == 2 && Config.SERVER != null && Config.SERVER.disableHurtParticles.get()) {
         callbackInfo.cancel();
      }
   }
}
