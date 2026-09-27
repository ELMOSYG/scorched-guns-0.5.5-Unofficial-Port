package top.ribs.scguns.mixin.common.compat.guardvillagers;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.ribs.scguns.compat.guardvillagers.GuardVillagersCompat;
import top.ribs.scguns.item.GunItem;

/**
 * A gun may always replace a guard's non-gun equipment (HANDOFF section 82.14).
 *
 * <p>This is the one piece of the reference 1.21.1 port's guard compat that is about equipment rather than
 * AI, and it exists because Guard Villagers equips its guards through {@code Guard.setItemSlot}, which it
 * <b>overrides</b>: a guard's own sword or crossbow can land on top of a gun the compat just gave it. The
 * compat re-arms a disarmed guard on the next equipment change, so this is not strictly required - but
 * letting the gun win the equipment decision is both cheaper and truer to what the player sees (a guard
 * that never loses its gun in the first place never has to be re-armed).</p>
 *
 * <h2>Two deliberate differences from the reference implementation</h2>
 *
 * <p>The reference applies this to <b>every</b> mob in the game ({@code @Mixin(Mob.class)} with no entity
 * check). That is a global change to vanilla's equipment rules for the sake of one optional mod's guards,
 * so this version keeps the mixin on {@code Mob} (Guard does not override {@code canReplaceCurrentItem},
 * so a guard does inherit this) and then requires {@link GuardVillagersCompat#isGuard} - the same
 * type-id-only guard check every other part of this compat uses.</p>
 *
 * <p>The second difference is the {@code remap} default: Guard Villagers' classes are named literally
 * ({@code remap = false} on the guarded-prefix mixins), but this mixin targets a <b>vanilla</b> class, so
 * it is left remapped - that is what {@code MixinHumanoidModel} and the other vanilla mixins in this mod
 * already do.</p>
 */
@Mixin(Mob.class)
public abstract class GuardEquipmentMixin {
   @Inject(
      method = {"canReplaceCurrentItem"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void scguns$gunsReplaceWhateverAGuardHolds(
      ItemStack candidate, ItemStack existing, CallbackInfoReturnable<Boolean> cir
   ) {
      if (candidate.getItem() instanceof GunItem
         && !(existing.getItem() instanceof GunItem)
         && GuardVillagersCompat.isGuard((Mob)(Object)this)) {
         cir.setReturnValue(true);
      }
   }
}
