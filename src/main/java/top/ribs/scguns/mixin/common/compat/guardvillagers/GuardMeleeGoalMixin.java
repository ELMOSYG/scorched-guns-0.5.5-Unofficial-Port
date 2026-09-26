package top.ribs.scguns.mixin.common.compat.guardvillagers;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.ribs.scguns.item.GunItem;

/**
 * A guard holding a gun does not charge into melee (HANDOFF section 82.9).
 *
 * <p>The gun AI fights at the gun's ideal range: it approaches to that range, backs off when the target is
 * too close, and fires in bursts. Guard Villagers' own melee goal is happy to run at the same time - it only
 * asks for a live target - and because both goals call the navigation, the melee goal's charge won the race
 * every tick: an armed guard closed to melee range and took the occasional random shot on the way there,
 * which is exactly what the player reported.</p>
 *
 * <p>Suppressing melee <b>only while the guard holds a gun</b> is the one piece of Guard Villagers'
 * behaviour this compat changes, and it is deliberately narrow: the moment the gun is gone (dropped, or
 * replaced), melee comes straight back. Everything else of the guard's AI - patrol, return to village,
 * doors, shields, eating, strolling - is untouched, because the gun AI reserves no goal flags.</p>
 *
 * <p>The mixin targets a Guard Villagers class by name, so it is <b>gated</b> in
 * {@code top.ribs.scguns.mixin.MixinPlugin} on that mod being installed; the shadowed field is the
 * inherited {@code MeleeAttackGoal.mob}, so no Guard Villagers type is named in code and the class only ever
 * loads when the gated mixin is applied.</p>
 */
@Pseudo
@Mixin(targets = "tallestegg.guardvillagers.common.entities.Guard$GuardMeleeGoal", remap = false)
public abstract class GuardMeleeGoalMixin {
   @Shadow
   protected PathfinderMob mob;

   @Inject(method = "canUse", at = @At("HEAD"), cancellable = true, remap = false)
   private void scguns$noMeleeWithGun(CallbackInfoReturnable<Boolean> cir) {
      if (this.mob == null) {
         return;
      }

      ItemStack held = this.mob.getMainHandItem();
      if (held.getItem() instanceof GunItem) {
         cir.setReturnValue(false);
      }
   }
}
