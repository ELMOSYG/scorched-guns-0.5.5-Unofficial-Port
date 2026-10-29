package top.ribs.scguns.mixin.common;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.monster.piglin.Piglin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.ribs.scguns.entity.monster.HornlinEntity;
import top.ribs.scguns.entity.monster.ZombifiedHornlinEntity;

/**
 * Makes the mod's piglin soldiers part of the piglin faction as far as friendly fire goes.
 *
 * <p>Vanilla only has two piglin-class mobs - {@code Piglin} and {@code PiglinBrute} - and neither retaliates against
 * its own kind. The hornlin soldiers are piglins in theme but not in class, so a stray round from one of them made
 * every nearby piglin hostile and turned a friendly camp into a firefight. Piglins now ignore damage that came from a
 * hornlin, which is what a shared faction would have given them.</p>
 */
@Mixin(Piglin.class)
public abstract class PiglinFactionMixin {
   @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
   private void scguns$ignorePiglinSoldierFriendlyFire(DamageSource source, float amount,
                                                       CallbackInfoReturnable<Boolean> cir) {
      if (source.getEntity() instanceof HornlinEntity || source.getEntity() instanceof ZombifiedHornlinEntity) {
         cir.setReturnValue(false);
      }
   }
}
