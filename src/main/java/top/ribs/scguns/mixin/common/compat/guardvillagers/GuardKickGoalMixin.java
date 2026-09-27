package top.ribs.scguns.mixin.common.compat.guardvillagers;

import java.lang.reflect.Field;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.ribs.scguns.compat.guardvillagers.GuardVillagersCompat;

/**
 * A guard holding a gun may still kick a target that gets right up against it (HANDOFF section 82.14).
 *
 * <h2>Why Guard Villagers says no, and exactly what is relaxed here</h2>
 *
 * <p>{@code Guard$KickGoal.canUse} is, from its bytecode:</p>
 *
 * <pre>
 *    target != null
 * &amp;&amp; guard.distanceTo(target) &lt;= 2.5
 * &amp;&amp; guard.getMainHandItem().getItem().useOnRelease(guard.getMainHandItem())   // &lt;-- the hand check
 * &amp;&amp; !guard.isBlocking()
 * &amp;&amp; guard.kickCoolDown == 0
 * </pre>
 *
 * <p>That hand check is {@code Item.useOnRelease}, which vanilla answers {@code true} for a <b>crossbow</b>
 * and a <b>trident</b> - Guard Villagers' kick is the "bow in hand, shove the thing that reached you" move.
 * A scorched gun is neither, so a guard that has been armed by this compat loses its kick completely: it
 * would rather stand there with a target inside its own muzzle. The reference 1.21.1 port relaxes this
 * condition, and so does this mixin - <b>but only that condition</b>.</p>
 *
 * <p>The reference replaces the whole decision with {@code !guard.isBlocking()} while the guard holds a gun,
 * which also throws away {@code kickCoolDown == 0}; the guard then kicks every tick that something stays
 * within 2.5 blocks, which is a worse bug than the one being fixed. Here the remaining four conditions are
 * checked as Guard Villagers wrote them, so a gun-armed guard kicks at exactly the cadence a crossbow-armed
 * one does. If the cooldown field cannot be read (a Guard Villagers version that renamed it), the mixin
 * leaves the decision entirely alone instead of guessing.</p>
 *
 * <p>The guard is reached through the goal's own {@code guard} field by reflection, and no entity type
 * appears in the injector's signature - see the class documentation of {@link GuardMeleeGoalMixin} for why
 * naming one there stops the game from starting altogether.</p>
 */
@Pseudo
@Mixin(
   targets = {"tallestegg.guardvillagers.common.entities.Guard$KickGoal"},
   remap = false
)
public abstract class GuardKickGoalMixin {
   /** Guard Villagers' own reach for a kick, in blocks; the bytecode compares {@code distanceTo(...) <= 2.5}. */
   private static final float KICK_REACH = 2.5F;

   private static Field guardField;
   private static Field cooldownField;

   @Inject(
      method = {"canUse"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private void scguns$kickIsStillAllowedWithAGun(CallbackInfoReturnable<Boolean> cir) {
      Object goal = this;
      Object guard = readField(goal, "guard");
      if (!(guard instanceof Mob mob) || !GuardVillagersCompat.isHoldingGun(mob)) {
         return;
      }

      LivingEntity target = mob.getTarget();
      if (target == null || mob.distanceTo(target) > KICK_REACH || mob.isBlocking()) {
         return;
      }

      int cooldown = readCooldown(guard);
      if (cooldown != 0) {
         // Unknown (-1) as well as "still cooling down": Guard Villagers' own answer stands.
         return;
      }

      cir.setReturnValue(true);
   }

   /** Reads Guard Villagers' own {@code kickCoolDown}; -1 when the field is not there (see the class doc). */
   private static int readCooldown(Object guard) {
      try {
         Field field = cooldownField;
         if (field == null || !field.getDeclaringClass().isInstance(guard)) {
            field = guard.getClass().getField("kickCoolDown");
            cooldownField = field;
         }

         return field.getInt(guard);
      } catch (Throwable broken) {
         return -1;
      }
   }

   private static Object readField(Object owner, String name) {
      try {
         Field field = guardField;
         if (field == null || !field.getDeclaringClass().isInstance(owner)) {
            field = findField(owner.getClass(), name);
            guardField = field;
         }

         return field == null ? null : field.get(owner);
      } catch (Throwable broken) {
         // Never throw out of canUse: falling through leaves Guard Villagers' own decision in place.
         return null;
      }
   }

   private static Field findField(Class<?> type, String name) {
      for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
         try {
            Field field = current.getDeclaredField(name);
            field.setAccessible(true);
            return field;
         } catch (NoSuchFieldException ignored) {
            // Keep walking: the field is declared on the goal itself, but nothing here depends on that.
         }
      }

      return null;
   }
}
