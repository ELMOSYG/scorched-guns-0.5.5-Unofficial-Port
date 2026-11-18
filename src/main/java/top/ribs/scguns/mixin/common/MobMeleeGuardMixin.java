package top.ribs.scguns.mixin.common;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Two guard rails for every mob in the game, applied at the single place where a melee hit is actually dealt.
 *
 * <p>1. A dying mob must not keep swinging: death takes twenty ticks and the goals kept calling doHurtTarget during
 * that window, so players were hit by corpses.</p>
 *
 * <p>2. A melee hit must not pass through a wall. Vanilla relies on the attack goals to be standing next to the
 * target, but the mobs in this mod reach further than that (weapon reach from the gun JSON), which let them hit
 * through blocks. Checking line of sight here fixes every such path at once.</p>
 */
@Mixin(Mob.class)
public abstract class MobMeleeGuardMixin {

   @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
   private void scguns$guardMelee(Entity target, CallbackInfoReturnable<Boolean> callbackInfo) {
      Mob self = (Mob) (Object) this;

      // Only this mod's own mobs: other mods may rely on their own reach or on hitting through a wall, and silently
      // changing their combat would be a compat break nobody asked for.
      net.minecraft.resources.ResourceLocation id =
         net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(self.getType());
      if (id == null || !"scguns".equals(id.getNamespace())) {
         return;
      }

      if (self.isDeadOrDying()) {
         callbackInfo.setReturnValue(false);
         return;
      }

      if (target instanceof LivingEntity living && !self.getSensing().hasLineOfSight(living)) {
         callbackInfo.setReturnValue(false);
      }
   }
}
