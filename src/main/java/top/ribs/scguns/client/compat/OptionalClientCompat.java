package top.ribs.scguns.client.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.ribs.scguns.ScorchedGuns;
import top.ribs.scguns.item.GunItem;

/**
 * Optional Entity Model Features support.
 *
 * <p>Two things are needed and nothing else. Resource packs are told when the rendered player holds a gun, through
 * the {@code is_scguns_gun_held} animation variable; and EMF's own first-person hand pass is skipped while a gun is
 * held, because a gun draws its arms inside its own model and EMF's extra arm is what players saw floating next to
 * the weapon.</p>
 *
 * <p>All EMF access is reflective and cached. EMF is optional, so nothing here may be referenced directly, and the
 * lookups must not repeat per frame: a reflective lookup for every part of every animated entity is what makes
 * villages crawl with EMF installed.</p>
 */
public final class OptionalClientCompat {

   private static final String EMF_ANIMATION_API = "traben.entity_model_features.EMFAnimationApi";
   private static final String EMF_ANIMATION_CONTEXT = "traben.entity_model_features.models.animation.EMFAnimationEntityContext";

   private static Boolean emfLoaded;
   private static boolean variableRegistered;
   private static boolean handSuppressionLogged;
   private static boolean contextLookupFailed;

   private static Method registerVariableMethod;
   private static Method contextGetEntityMethod;
   private static Field firstPersonHandField;

   private OptionalClientCompat() {
   }

   /** Whether EMF is present. Resolved once; an absent mod is remembered, not retested every frame. */
   public static boolean isEntityModelFeaturesLoaded() {
      if (emfLoaded == null) {
         emfLoaded = isClassPresent(EMF_ANIMATION_API) && isClassPresent(EMF_ANIMATION_CONTEXT);
         if (!emfLoaded) {
            ScorchedGuns.LOGGER.debug("Entity Model Features is not installed; its animation variable is skipped.");
         }
      }

      return emfLoaded;
   }

   /** Registers {@code is_scguns_gun_held} with EMF, once per game session. Called from client setup. */
   public static void registerEntityModelFeaturesVariables() {
      if (variableRegistered || !isEntityModelFeaturesLoaded()) {
         return;
      }

      try {
         registerVariableMethod().invoke(
            null,
            "scguns",
            "is_scguns_gun_held",
            "True when the rendered player is holding a Scorched Guns gun.",
            (BooleanSupplier) OptionalClientCompat::isScorchedGunHeldByEntityModelFeaturesContext
         );
         variableRegistered = true;
         ScorchedGuns.LOGGER.debug("Registered the EMF animation variable is_scguns_gun_held.");
      } catch (ReflectiveOperationException | LinkageError exception) {
         ScorchedGuns.LOGGER.debug("EMF animation variable registration was skipped.", exception);
      }
   }

   private static Method registerVariableMethod() throws ReflectiveOperationException {
      if (registerVariableMethod == null) {
         Class<?> api = Class.forName(EMF_ANIMATION_API, false, OptionalClientCompat.class.getClassLoader());
         registerVariableMethod = api.getMethod(
            "registerSingletonAnimationVariable", String.class, String.class, String.class, BooleanSupplier.class);
      }

      return registerVariableMethod;
   }

   /**
    * Whether the vanilla first-person hand pass should be skipped for this player.
    *
    * <p>Only with EMF installed and a gun actually held; every other item keeps rendering exactly as before.</p>
    */
   public static boolean shouldSuppressVanillaHands(Player player) {
      return isEntityModelFeaturesLoaded() && isScorchedGunHeldByPlayer(player);
   }

   /** One line per session, so a report can tell whether the suppression is active. */
   public static void logHandSuppressionOnce() {
      if (!handSuppressionLogged) {
         handSuppressionLogged = true;
         ScorchedGuns.LOGGER.info("scguns: suppressing the vanilla first-person hands while a gun is held (EMF present).");
      }
   }

   /** Whether the player is holding a gun in either hand. */
   public static boolean isScorchedGunHeldByPlayer(Player player) {
      return player != null && (isScorchedGunStack(player.getMainHandItem()) || isScorchedGunStack(player.getOffhandItem()));
   }

   public static boolean isScorchedGunHeldByLocalPlayer() {
      return isScorchedGunHeldByPlayer(Minecraft.getInstance().player);
   }

   public static boolean isScorchedGunStack(ItemStack stack) {
      return stack != null && !stack.isEmpty() && stack.getItem() instanceof GunItem;
   }

   /** EMF's own view of the entity being animated, which is not always the local player. */
   public static Object getEntityModelFeaturesContextEntity() {
      try {
         return contextGetEntityMethod().invoke(null);
      } catch (ReflectiveOperationException | LinkageError exception) {
         contextFailure(exception);
         return null;
      }
   }

   /** Whether EMF is animating a first-person hand right now, in which case the held pose must not be reapplied. */
   public static boolean isEntityModelFeaturesFirstPersonHand() {
      try {
         if (firstPersonHandField == null) {
            Class<?> context = Class.forName(EMF_ANIMATION_CONTEXT, false, OptionalClientCompat.class.getClassLoader());
            firstPersonHandField = context.getField("isFirstPersonHand");
         }

         return firstPersonHandField.getBoolean(null);
      } catch (ReflectiveOperationException | LinkageError exception) {
         contextFailure(exception);
         return false;
      }
   }

   private static Method contextGetEntityMethod() throws ReflectiveOperationException {
      if (contextGetEntityMethod == null) {
         Class<?> context = Class.forName(EMF_ANIMATION_CONTEXT, false, OptionalClientCompat.class.getClassLoader());
         contextGetEntityMethod = context.getMethod("getEMFEntity");
      }

      return contextGetEntityMethod;
   }

   private static boolean isScorchedGunHeldByEntityModelFeaturesContext() {
      if (contextLookupFailed) {
         return isScorchedGunHeldByLocalPlayer();
      }

      if (getEntityModelFeaturesContextEntity() instanceof Player player) {
         return isScorchedGunHeldByPlayer(player);
      }

      return isScorchedGunHeldByLocalPlayer();
   }

   /** One failed lookup disables the EMF context helpers for the session instead of throwing every frame. */
   private static void contextFailure(Throwable throwable) {
      if (!contextLookupFailed) {
         contextLookupFailed = true;
         ScorchedGuns.LOGGER.debug("The EMF animation context is not usable; falling back to the local player.", throwable);
      }
   }

   private static boolean isClassPresent(String className) {
      try {
         Class.forName(className, false, OptionalClientCompat.class.getClassLoader());
         return true;
      } catch (ClassNotFoundException | LinkageError exception) {
         return false;
      }
   }
}
