package top.ribs.scguns.compat.guardvillagers;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.player.Player;

/**
 * Turns a guard around when something hits it.
 *
 * <p>Guard Villagers registers its own hurt-by goal, but armed guards were observed ignoring attackers and keeping
 * their current target - which matters, because a guard that ignores the zombie behind it does not survive. This is a
 * narrow safety net on the target selector: whenever the guard has a last-hurt-by attacker that is a legitimate
 * enemy, that attacker becomes the target.</p>
 *
 * <p>It is deliberately eager about when it applies and strict about who counts: the guard itself, its allies and
 * creative or spectator players are ignored.</p>
 */
public final class GuardRetaliateGoal extends TargetGoal {
   /** Kept here rather than reusing a superclass field, which TargetGoal does not declare. */
   private LivingEntity attacker;

   public GuardRetaliateGoal(Mob guard) {
      super(guard, false);
      this.setFlags(EnumSet.of(Goal.Flag.TARGET));
   }

   @Override
   public boolean canUse() {
      LivingEntity candidate = this.mob.getLastHurtByMob();

      if (candidate == null || candidate == this.mob || !candidate.isAlive()) {
         return false;
      }

      if (candidate instanceof Player player && (player.isCreative() || player.isSpectator())) {
         return false;
      }

      if (GuardVillagersCompat.isFriendlyShot(this.mob, candidate)) {
         return false;
      }

      this.attacker = candidate;
      return true;
   }

   @Override
   public void start() {
      this.mob.setTarget(this.attacker);
      super.start();
   }
}
