package top.ribs.scguns.mixin.common;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.ribs.scguns.item.GunItem;

/**
 * Stops a mob holding a gun from swinging it like a melee weapon.
 *
 * <p>Combat overhaul mods that add mob attack animations treat anything with an attack damage attribute as a melee
 * weapon, so a gunner would wave its rifle around as if clubbing with it. Cancelling the swing at the source keeps the
 * arm where the gun's aiming pose puts it, without having to know which mod asked for the animation.</p>
 */
@Mixin(LivingEntity.class)
public abstract class MobGunSwingMixin {
   @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;Z)V", at = @At("HEAD"), cancellable = true)
   private void scguns$noSwingWithGun(InteractionHand hand, boolean updateSelf, CallbackInfo ci) {
      if (!((Object)this instanceof Mob mob)) {
         return;
      }

      if (mob.getItemInHand(hand).getItem() instanceof GunItem) {
         ci.cancel();
      }
   }
}
