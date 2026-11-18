package com.scg2tlm.elmomod.mixin;

import com.scg2tlm.elmomod.compat.MaidGunParticleSuppressor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stops the hurt particle broadcast for a maid that was just shot by one of this mod's rounds.
 *
 * <p>The client renders the red damage particles from entity event 2, which the server broadcasts. Dropping the
 * broadcast is enough, so nothing here touches the client and nothing else about the hit changes.</p>
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelDamageParticlesMixin {

    @Inject(method = "broadcastEntityEvent", at = @At("HEAD"), cancellable = true)
    private void scg2tlm$skipMaidGunHitParticles(Entity entity, byte event, CallbackInfo ci) {
        if (event == 2 && MaidGunParticleSuppressor.wasShotThisTick(entity)) {
            ci.cancel();
        }
    }
}
