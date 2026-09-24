package top.ribs.scguns.entity.ai;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@EventBusSubscriber(
   modid = "scguns",
   bus = Bus.GAME
)
public class GunMobFriendlyFire {
   private static final Logger LOGGER = LogManager.getLogger();

   public GunMobFriendlyFire() {
      super();
   }

   @SubscribeEvent
   public static void onLivingHurt(LivingIncomingDamageEvent event) {
      if (event.getEntity() instanceof Mob victim) {
         DamageSource source = event.getSource();
         if (source.getEntity() instanceof Mob mobAttacker) {
            if (shouldPreventFriendlyFire(victim, mobAttacker)) {
               event.setCanceled(true);
               victim.setLastHurtByMob(null);
            }
         }
      }
   }

   @SubscribeEvent
   public static void onChangeTarget(LivingChangeTargetEvent event) {
      if (event.getEntity() instanceof Mob mob) {
         if (event.getNewAboutToBeSetTarget() instanceof Mob targetMob) {
            if (shouldPreventFriendlyFire(targetMob, mob)) {
               event.setCanceled(true);
            }
         }
      }
   }

   private static boolean shouldPreventFriendlyFire(Mob victim, Mob attacker) {
      if (isInSameRaid(victim, attacker)) {
         return true;
      } else if (victim.getTags().contains("MobGunner") && attacker.getTags().contains("MobGunner")) {
         AIType victimAI = getAIType(victim);
         AIType attackerAI = getAIType(attacker);
         return victimAI == AIType.RECKLESS && attackerAI == AIType.RECKLESS
            ? false
            : victimAI == AIType.TACTICAL || victimAI == AIType.DEFAULT || attackerAI == AIType.TACTICAL || attackerAI == AIType.DEFAULT;
      } else {
         return false;
      }
   }

   private static AIType getAIType(Mob mob) {
      if (mob.getTags().contains("AI_TACTICAL")) {
         return AIType.TACTICAL;
      } else if (mob.getTags().contains("AI_DEFAULT")) {
         return AIType.DEFAULT;
      } else if (mob.getTags().contains("AI_RECKLESS")) {
         return AIType.RECKLESS;
      } else {
         return mob.getTags().contains("AI_COWARD") ? AIType.COWARD : AIType.DEFAULT;
      }
   }

   private static boolean isInSameRaid(Mob victim, Mob attacker) {
      for (String tag : victim.getTags()) {
         if (tag.startsWith("RaidMember_") && attacker.getTags().contains(tag)) {
            return true;
         }
      }

      return false;
   }
}
