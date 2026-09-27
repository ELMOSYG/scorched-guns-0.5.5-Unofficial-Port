package top.ribs.scguns.mixin.common.compat.guardvillagers;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.ribs.scguns.item.GunItem;

/**
 * A guard holding a gun does not charge into melee (HANDOFF section 82.9).
 *
 * <p>The gun AI fights at the gun's ideal range: it approaches to that range, backs off when the target is
 * too close, and fires in bursts. Guard Villagers' own melee goal is happy to run at the same time - it only
 * asks for a live target - and because both goals call the navigation, the melee goal's charge won the race
 * every tick: an armed guard closed to melee range and took the occasional random shot on the way there,
 * which is exactly what the player reported.</p>
 *
 * <p>Suppressing melee <b>only while the guard holds a gun</b> is the one piece of Guard Villagers'
 * behaviour this compat changes, and it is deliberately narrow: the moment the gun is gone, melee comes
 * straight back. Everything else of the guard's AI - patrol, return to village, doors, shields, eating,
 * strolling - is untouched, because the gun AI reserves no goal flags.</p>
 *
 * <h2>Why the mob is reached by reflection and not by a shadowed field</h2>
 *
 * <p>The obvious version of this mixin shadows the goal's inherited field -
 * <code>@Shadow protected PathfinderMob mob;</code> - and that <b>stops the game from starting</b>: the
 * shadowed type puts {@code Mob} and therefore {@code LivingEntity} on the class-loading path while Mixin is
 * still preparing configs, and GeckoLib's own {@code LivingEntityMixin} then dies with
 * {@code MixinTargetAlreadyLoadedException: target net.minecraft.world.entity.LivingEntity was loaded too
 * early}. Loading {@code LivingEntity} early is fatal for every mod that mixes into it, and the failure
 * points at GeckoLib rather than at the mod that caused it.</p>
 *
 * <p>So this class names no entity type at all: the field is found through the superclass chain by name and
 * the hand is read reflectively, exactly as the standalone 1.20.1 compat did for the same reason. Type
 * references inside method bodies are resolved lazily and are therefore fine - it is the <b>field and
 * signature types</b> that are resolved during preparation.</p>
 */
@Pseudo
@Mixin(targets = "tallestegg.guardvillagers.common.entities.Guard$GuardMeleeGoal", remap = false)
public abstract class GuardMeleeGoalMixin {
   private static Field mobField;

   @Inject(method = "canUse", at = @At("HEAD"), cancellable = true, remap = false)
   private void scguns$noMeleeWithGun(CallbackInfoReturnable<Boolean> cir) {
      Object mob = readMob(this);
      if (mob == null) {
         return;
      }

      ItemStack held = readMainHand(mob);
      if (held != null && held.getItem() instanceof GunItem) {
         cir.setReturnValue(false);
      }
   }

   private static Object readMob(Object goal) {
      try {
         Field field = mobField;
         if (field == null || !field.getDeclaringClass().isInstance(goal)) {
            field = findMobField(goal.getClass());
            mobField = field;
         }

         return field == null ? null : field.get(goal);
      } catch (Throwable broken) {
         // Never throw out of a goal's canUse: without the suppression an armed guard simply melees, which
         // is exactly what the game did before this mixin existed.
         return null;
      }
   }

   private static Field findMobField(Class<?> type) {
      for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
         try {
            Field field = current.getDeclaredField("mob");
            field.setAccessible(true);
            return field;
         } catch (NoSuchFieldException ignored) {
            // MeleeAttackGoal declares it; keep walking just in case.
         }
      }

      return null;
   }

   private static ItemStack readMainHand(Object mob) {
      try {
         Method method = mob.getClass().getMethod("getMainHandItem");
         Object value = method.invoke(mob);
         return value instanceof ItemStack stack ? stack : null;
      } catch (Throwable broken) {
         return null;
      }
   }
}
