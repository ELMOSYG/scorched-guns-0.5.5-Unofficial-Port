package top.ribs.scguns.compat.guardvillagers;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.AbstractVillager;
import tallestegg.guardvillagers.common.entities.Guard;

/**
 * The one place in the mod that names a Guard Villagers class (HANDOFF section 82).
 *
 * <p>Split out of {@link GuardVillagersCompat} on purpose: that class is called on every projectile hit
 * and every incoming damage event, on servers that do not have Guard Villagers installed at all.
 * {@code Guard} appears here only in an {@code instanceof}, and only behind
 * {@code GuardVillagersCompat.isGuard(...)}, so the host never tries to load a class that is not
 * there.</p>
 *
 * <p><b>Why "only in an instanceof" matters - and why it is not the general rule it looks like.</b>
 * A JVM verifies every method of a class when the class is <em>linked</em>, before any of them runs,
 * and the verifier resolves the types it has to decide assignability between. So an optional mod's
 * type in a <b>signature</b> (return type, parameter, field) of a class that gets linked
 * unconditionally is a latent {@code NoClassDefFoundError} that no {@code if (modLoaded)} inside
 * those methods can prevent: the class dies before the guard runs. That is exactly what broke
 * entering a save without Sable; {@code tools/audit_optional_api_refs.py} now enforces the rule
 * (HANDOFF section 83.2). An {@code instanceof} is fine - the verifier records the test and defers
 * the load to that instruction, which only runs behind the guard.</p>
 *
 * <p>The rule itself is the 1.20.1 compat's: a guard's shot must not hurt the guard's owner, another
 * guard, any villager, or an iron golem. Guard Villagers' own bow AI stops shooting at allies before
 * firing; that is exactly what a gun does not do, because its projectiles fly straight.</p>
 */
final class GuardFriendlyRules {
   private GuardFriendlyRules() {
   }

   static boolean isAlly(Entity shooter, Entity target) {
      if (!(shooter instanceof Guard guard) || !(target instanceof LivingEntity living)) {
         return false;
      }

      return guard.isOwner(living)
         || living instanceof Guard
         || living instanceof IronGolem
         || living instanceof AbstractVillager;
   }
}
