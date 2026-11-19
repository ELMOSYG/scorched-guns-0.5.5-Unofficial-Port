package top.ribs.scguns.mixin.common;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.ribs.scguns.compat.BlockParticleSafety;

/**
 * Substitutes the block used for destroy particles when the broken block is a copycat.
 *
 * <p>{@code Level#addDestroyBlockEffect} is the single place the game turns a broken block state into particles, so
 * every break path - vanilla, this mod, other mods - goes through it. Copycat blocks answer with the missing texture,
 * which is why their debris came out magenta and black.</p>
 */
@Mixin(Level.class)
public abstract class CopycatParticleMixin {

   @Inject(method = "addDestroyBlockEffect", at = @At("HEAD"), cancellable = true)
   private void scguns$useSafeStateForCopycatParticles(BlockPos pos, BlockState state, CallbackInfo callbackInfo) {
      if (!BlockParticleSafety.isCopycat(state)) {
         return;
      }

      Level self = (Level) (Object) this;
      self.addDestroyBlockEffect(pos, BlockParticleSafety.safeState(state));
      callbackInfo.cancel();
   }
}
