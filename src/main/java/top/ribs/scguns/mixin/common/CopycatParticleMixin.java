package top.ribs.scguns.mixin.common;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.ribs.scguns.compat.BlockParticleSafety;

/**
 * Rewrites the block-break particle event (2001) for copycat blocks.
 *
 * <p>Copycat blocks draw their surface through a block entity and do not hand out a particle icon, so the client
 * paints their break particles as the magenta/black missing texture. Vanilla reaches the event through two
 * overloads - one with a player, one without - and the block-break path uses the one without. Both are covered here
 * so the substituted id reaches the client no matter which path emitted it.</p>
 */
@Mixin(Level.class)
public abstract class CopycatParticleMixin {

   @Inject(method = "levelEvent(Lnet/minecraft/world/entity/player/Player;ILnet/minecraft/core/BlockPos;I)V",
      at = @At("HEAD"), cancellable = true)
   private void scguns$remapCopycatBreakParticlesWithPlayer(Player player, int type, BlockPos pos, int data,
      CallbackInfo callbackInfo) {
      this.scguns$remap(player, type, pos, data, callbackInfo);
   }

   @Inject(method = "levelEvent(ILnet/minecraft/core/BlockPos;I)V", at = @At("HEAD"), cancellable = true)
   private void scguns$remapCopycatBreakParticles(int type, BlockPos pos, int data, CallbackInfo callbackInfo) {
      this.scguns$remap(null, type, pos, data, callbackInfo);
   }

   private void scguns$remap(Player player, int type, BlockPos pos, int data, CallbackInfo callbackInfo) {
      if (type != 2001) {
         return;
      }

      BlockState state = Block.stateById(data);
      if (!BlockParticleSafety.isCopycat(state)) {
         return;
      }

      Level self = (Level) (Object) this;
      int safe = BlockParticleSafety.safeId(state);
      if (player != null) {
         self.levelEvent(player, type, pos, safe);
      } else {
         self.levelEvent(type, pos, safe);
      }
      callbackInfo.cancel();
   }
}
