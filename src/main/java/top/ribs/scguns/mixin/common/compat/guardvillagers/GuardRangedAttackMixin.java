package top.ribs.scguns.mixin.common.compat.guardvillagers;

import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.ribs.scguns.compat.guardvillagers.GuardVillagersCompat;

/**
 * A guard holding a gun does not fire a crossbow (HANDOFF section 82.14).
 *
 * <p>Guard Villagers' {@code Guard} implements {@code CrossbowAttackMob}, so its ranged goal keeps calling
 * {@code performRangedAttack} no matter what the guard is actually holding. With a gun in hand that call
 * runs the crossbow path against a gun stack - wrong animation, wrong sound, and a shot that has nothing to
 * do with the mod's gun AI. The gun AI fires through {@code MobGunFire}/{@code AIGunEvent} instead, so the
 * crossbow call is cancelled outright while the main hand is a gun. This is carried over from the reference
 * 1.21.1 port, which cancels the same call the same way.</p>
 *
 * <h2>The injector takes no target-method parameters on purpose</h2>
 *
 * <p>{@code performRangedAttack(LivingEntity, float)} could be injected with the target and the distance
 * factor as parameters, but the injector's own signature is resolved while Mixin is still preparing the
 * configs, and naming an entity type there is what cost this port a full crash investigation (section 82.10:
 * {@code MixinTargetAlreadyLoadedException: target LivingEntity was loaded too early}, blamed on GeckoLib
 * and then on Curios). A {@code CallbackInfo}-only injector plus the reflective hand read in
 * {@link GuardVillagersCompat#isHoldingGun} keeps the entity types out of every signature in this file.</p>
 */
@Pseudo
@Mixin(
   targets = {"tallestegg.guardvillagers.common.entities.Guard"},
   remap = false
)
public abstract class GuardRangedAttackMixin {
   @Inject(
      method = {"performRangedAttack"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private void scguns$gunsAreNotCrossbows(CallbackInfo ci) {
      if (GuardVillagersCompat.isHoldingGun(this)) {
         ci.cancel();
      }
   }
}
