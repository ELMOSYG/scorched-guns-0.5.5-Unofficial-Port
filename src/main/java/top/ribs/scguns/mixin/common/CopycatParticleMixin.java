package top.ribs.scguns.mixin.common;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.ribs.scguns.compat.BlockParticleSafety;

/**
 * Redirects the block-break particle event for copycat blocks to a plain stand-in block.
 *
 * <p>Vanilla's own {@code destroyBlock} path sends event 2001 with the id of the block that was broken, and the
 * client then asks that block for a particle icon. Copycat blocks render through a block entity and answer with the
 * missing texture, so every destroyed copycat block threw magenta and black squares - from our rounds, from other
 * mods and from vanilla itself. Rewriting the id here covers all of those at once without touching any block.</p>
 */
@Mixin(ServerLevel.class)
public abstract class CopycatParticleMixin {

   @Inject(method = "levelEvent(Lnet/minecraft/world/entity/player/Player;ILnet/minecraft/core/BlockPos;I)V",
      at = @At("HEAD"), cancellable = true)
   private void scguns$remapCopycatBreakParticles(Player player, int type, BlockPos pos, int data, CallbackInfo callbackInfo) {
      if (type != 2001) {
         return;
      }

      BlockState state = Block.stateById(data);
      if (!BlockParticleSafety.isCopycat(state)) {
         return;
      }

      ServerLevel self = (ServerLevel) (Object) this;
      self.levelEvent(player, type, pos, BlockParticleSafety.safeId(state));
      callbackInfo.cancel();
   }
}
