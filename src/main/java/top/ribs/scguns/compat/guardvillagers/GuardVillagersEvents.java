package top.ribs.scguns.compat.guardvillagers;

import java.util.WeakHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;

/**
 * A guard's gun must not hurt the village (HANDOFF section 82).
 *
 * <p>A gun projectile flies straight through whatever is behind its target, so unlike the guard's own bow
 * AI - which checks the firing line first - it can hit a villager, an iron golem or another guard. Two
 * layers stop that:</p>
 *
 * <ul>
 *   <li>{@code ProjectileEntity}'s target search skips allies of a guard shooter, so the shot passes
 *       through them entirely (no impact, no damage, no knockback);</li>
 *   <li>this handler cancels the damage anyway, and the knockback that would follow it, which also covers
 *       a guard's melee swing and any other damage source owned by a guard.</li>
 * </ul>
 *
 * <p>Both go through {@link GuardVillagersCompat#isFriendlyShot}, which is false for every non-guard, so
 * this subscriber is harmless - and these handlers never touch a Guard Villagers class - without the mod
 * installed.</p>
 */
@EventBusSubscriber(modid = "scguns", bus = EventBusSubscriber.Bus.GAME)
public class GuardVillagersEvents {
   /**
    * Victims a guard hit within the last few ticks, so the knockback that follows the (cancelled) damage
    * can be cancelled too. Weak keys: nothing here should keep an entity alive.
    */
   private static final WeakHashMap<LivingEntity, Long> RECENT_FRIENDLY_HITS = new WeakHashMap<>();

   /** How long after a guard's hit its knockback is still treated as friendly fire. */
   private static final long KNOCKBACK_GRACE_TICKS = 5L;

   @SubscribeEvent
   public static void onIncomingDamage(LivingIncomingDamageEvent event) {
      LivingEntity victim = event.getEntity();
      Entity attacker = event.getSource().getEntity();
      if (!GuardVillagersCompat.isFriendlyShot(attacker, victim)) {
         return;
      }

      event.setCanceled(true);
      RECENT_FRIENDLY_HITS.put(victim, victim.level().getGameTime());
   }

   @SubscribeEvent
   public static void onKnockback(LivingKnockBackEvent event) {
      LivingEntity victim = event.getEntity();
      Long hitAt = RECENT_FRIENDLY_HITS.get(victim);
      if (hitAt == null) {
         return;
      }

      if (victim.level().getGameTime() - hitAt <= KNOCKBACK_GRACE_TICKS) {
         event.setCanceled(true);
         RECENT_FRIENDLY_HITS.remove(victim);
      } else {
         RECENT_FRIENDLY_HITS.remove(victim);
      }
   }
}
