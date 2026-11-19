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
 * Rewrites explicit level event 2001 calls that carry a copycat block id.
 *
 * <p>Some code paths send the destroy particle event directly instead of going through
 * {@code addDestroyBlockEffect}; this covers those so both routes end up with a usable particle block.</p>
 */
@Mixin(ServerLevel.class)
public abstract class CopycatLevelEventMixin {

   @Inject(method = "levelEvent(Lnet/minecraft/world/entity/player/Player;ILnet/minecraft/core/BlockPos;I)V",
      at = @At("HEAD"), cancellable = true)
   private void scguns$remapCopycatBreakParticles(Player player, int type, BlockPos pos, int data,
      CallbackInfo callbackInfo) {
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
