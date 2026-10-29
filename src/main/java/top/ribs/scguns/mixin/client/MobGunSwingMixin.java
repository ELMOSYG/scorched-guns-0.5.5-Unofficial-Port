package top.ribs.scguns.mixin.client;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.ribs.scguns.item.GunItem;

/**
 * Keeps a gunner from swinging its gun like a club.
 *
 * <p>Mob combat overhaul mods classify ranged weapons by vanilla types ({@code ProjectileWeaponItem}, {@code BowItem},
 * {@code CrossbowItem}, {@code TridentItem}) and treat everything else with an attack attribute as melee. A gun is none
 * of those types, so gunners were given a melee swing animation and waved their rifles around. Cancelling the swing
 * client-side removes the animation without touching any server logic: muzzle flashes and damage are driven by packets,
 * not by the swing flag.</p>
 */
@Mixin(LivingEntity.class)
public abstract class MobGunSwingMixin {
   @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;Z)V", at = @At("HEAD"), cancellable = true)
   private void scguns$noSwingWithGun(InteractionHand hand, boolean updateSelf, CallbackInfo ci) {
      if (!((Object)this instanceof Mob mob) || !mob.level().isClientSide()) {
         return;
      }

      if (mob.getItemInHand(hand).getItem() instanceof GunItem) {
         ci.cancel();
      }
   }
}
