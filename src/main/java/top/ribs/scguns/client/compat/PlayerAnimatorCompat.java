package top.ribs.scguns.client.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.model.geom.ModelPart;
import net.neoforged.fml.ModList;
import top.ribs.scguns.ScorchedGuns;

/**
 * Clears the pose a player animation left on a model part, so the arms a gun draws are the arms this mod
 * intends to draw (HANDOFF section 82.25).
 *
 * <p>Player Animator (the library Better Combat and Iron's Spells' animations run on) animates the
 * <em>shared</em> {@code PlayerModel} instance of the local player. Its applier writes more than a rotation:
 * {@code AnimationApplier.updatePart} sets {@code x/y/z}, {@code xRot/yRot/zRot}, {@code xScale/yScale/zScale}
 * and, through bendylib, a per-cuboid <b>bend</b> ({@code IBendHelper.bend(part, side, amount)}). A gun draws
 * its arms by calling {@code ModelPart.render} on that same model, and in first person nothing re-runs
 * {@code setupAnim} for the local player, so whatever the last attack animation left behind - a stretched or
 * bent arm - is what the gun renders. Resetting position and rotation is not enough; the scale and the bend
 * live outside those fields.</p>
 *
 * <p>Player Animator is optional, so nothing here may make it a dependency: the mod id is checked first and
 * the three reflective handles are resolved once and cached, because this runs per arm per frame. The bend
 * library is only touched through {@code Object}/{@code Method}, never through a {@code dev.kosmx} type in a
 * signature - loading one of those on a client without the mod would be a hard failure (HANDOFF section
 * 82.10).</p>
 */
public final class PlayerAnimatorCompat {
   private static final String MOD_ID = "playeranimator";
   /** {@code IBendHelper.INSTANCE.bend(ModelPart, float side, float amount)}; amount ~0 restores the cuboid. */
   private static final String BEND_HELPER_CLASS = "dev.kosmx.playerAnim.impl.animation.IBendHelper";
   private static final String BEND_METHOD = "bend";
   private static final String INSTANCE_FIELD = "INSTANCE";
   private static final float NO_BEND = 0.0F;

   private static Object bendHelper;
   private static Method bend;
   private static boolean resolved;
   /** Set once anything about the reflective path fails, so a per-frame call does not retry and log forever. */
   private static boolean unusable;

   private PlayerAnimatorCompat() {
      // utility
   }

   public static boolean isLoaded() {
      ModList list = ModList.get();
      return list != null && list.isLoaded(MOD_ID);
   }

   /**
    * Restores a model part's cuboids to their unbent shape. A no-op (and never reflective) when Player
    * Animator is not installed.
    */
   public static void resetBend(ModelPart part) {
      if (part == null || unusable || !isLoaded()) {
         return;
      }

      if (!resolved) {
         resolve();
      }

      if (bendHelper == null || bend == null) {
         return;
      }

      try {
         // The library's own contract: an amount below 1.0E-4 takes the other branch and resets the cuboid.
         bend.invoke(bendHelper, part, NO_BEND, NO_BEND);
      } catch (Throwable throwable) {
         unusable = true;
         ScorchedGuns.LOGGER.warn("Could not clear a player animation's arm bend; gun arms may render bent", throwable);
      }
   }

   private static void resolve() {
      resolved = true;
      try {
         Class<?> helper = Class.forName(BEND_HELPER_CLASS);
         Field instance = helper.getField(INSTANCE_FIELD);
         bendHelper = instance.get(null);
         bend = helper.getMethod(BEND_METHOD, ModelPart.class, float.class, float.class);
      } catch (Throwable throwable) {
         unusable = true;
         bendHelper = null;
         bend = null;
         ScorchedGuns.LOGGER.warn("Player Animator is installed but its bend API could not be reached; gun arms "
            + "may keep a leftover animation pose", throwable);
      }
   }
}
