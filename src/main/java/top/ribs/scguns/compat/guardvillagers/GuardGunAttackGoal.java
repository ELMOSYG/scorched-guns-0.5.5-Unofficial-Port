package top.ribs.scguns.compat.guardvillagers;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import top.ribs.scguns.entity.ai.AIType;
import top.ribs.scguns.entity.ai.GunAttackGoal;

/**
 * The gun AI a Guard Villagers guard uses (HANDOFF section 82.9): the mod's own {@link GunAttackGoal}, with
 * the guard's accuracy from the config.
 *
 * <p>Earlier versions of this compat wrote a guard-specific goal instead, and that was the wrong call twice
 * over. It reserved MOVE|LOOK, so while a guard had a gun and a target it silently disabled Guard
 * Villagers' own melee, patrol, return-to-village, door and stroll goals - the gun AI taking over the guard
 * AI. Removing those flags fixed that, but left the guard with nothing to fight <b>with</b>: its own AI only
 * knows how to close into melee, so an armed guard charged in and took the occasional random shot on the
 * way. The mod's own gunner AI already knows how to fight at a gun's range - it approaches to the gun's
 * ideal range, backs off when the target is too close, fires in bursts, uses cover as a TACTICAL gunner and
 * panics like any mob - and it reserves no flags, so none of the guard's own AI is lost by using it.</p>
 *
 * <p>This subclass therefore only picks the personality (TACTICAL: cover, steady bursts) and overrides the
 * accuracy with {@code common.compat.guard_gun_accuracy}, which is the one knob the standalone 1.20.1 compat
 * exposed. {@code accuracyModifier} is protected, so a subclass may set it directly.</p>
 */
public class GuardGunAttackGoal extends GunAttackGoal<PathfinderMob> {
   public GuardGunAttackGoal(PathfinderMob mob, ItemStack gunStack, int difficulty, float accuracy) {
      super(mob, gunStack, 1.0F, AIType.TACTICAL, difficulty);
      // A trained shot rather than a raider: the config's 3.5 against the raider AI's 1.2 to 2.5.
      this.accuracyModifier = accuracy;
   }
}
