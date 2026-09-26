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
 * and every incoming damage event, on servers that do not have Guard Villagers installed at all. Java
 * resolves a class when the instruction naming it first executes, so keeping {@code Guard} in a separate
 * class that is only reached behind {@code GuardVillagersCompat.isGuard(...)} means the host never tries
 * to load a class that is not there.</p>
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
