package top.ribs.scguns.client.compat;

import java.lang.reflect.Field;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
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
 * <p>EMF replaces the player model wholesale, so the pose injections this mod makes into {@code PlayerModel} never
 * run while it is installed. The least it can do is tell resource packs when the rendered player is holding a gun,
 * which is what the {@code is_scguns_gun_held} animation variable is for.</p>
 *
 * <p>Everything here is reflective and cached: EMF is optional, so the class cannot be referenced directly, and the
 * lookups must not happen per frame or per body part. A per-frame reflective lookup on every part of every animated
 * entity is what makes villages crawl when EMF is installed, so each class, method and field is resolved once and
 * kept.</p>
 */
public final class OptionalClientCompat {

   private static final String EMF_ANIMATION_API = "traben.entity_model_features.EMFAnimationApi";
   private static final String EMF_ANIMATION_CONTEXT = "traben.entity_model_features.models.animation.EMFAnimationEntityContext";

   private static Boolean emfLoaded;
   private static boolean variableRegistered;

   private static Method registerVariableMethod;
   private static Method contextGetEntityMethod;
   private static Field firstPersonHandField;
   private static boolean contextLookupFailed;

   private OptionalClientCompat() {
   }

   /** Whether EMF is present. Resolved once; a missing class is remembered as "absent", not retried every frame. */
   public static boolean isEntityModelFeaturesLoaded() {
      if (emfLoaded == null) {
         emfLoaded = isClassPresent(EMF_ANIMATION_API) && isClassPresent(EMF_ANIMATION_CONTEXT);
         if (!emfLoaded) {
            ScorchedGuns.LOGGER.debug("Entity Model Features is not installed; its animation variable is skipped.");
         }
      }

      return emfLoaded;
   }

   /**
    * Registers {@code is_scguns_gun_held} with EMF, once per game session.
    *
    * <p>Called from client setup. The API signature is looked up defensively because EMF has changed it between
    * releases: if the lookup fails, the mod keeps running without the variable instead of crashing.</p>
    */
   public static void registerEntityModelFeaturesVariables() {
      if (variableRegistered || !isEntityModelFeaturesLoaded()) {
         return;
      }

      try {
         Method register = registerVariableMethod();
         register.invoke(
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
    * Cancels the renderer's own first-person hand while a gun is held.
    *
    * <p>Entity Model Features (through Fresh Animations' player add-on) draws the first-person arm itself. With a gun
    * in hand that draws a second, animated arm on top of this mod's own arm, which is what players report as arms
    * rotated into the gun. Only the hands are suppressed - everything else EMF animates keeps working.</p>
    */
   public static void cancelFallbackFirstPersonHandWhenGunHeld(Player player, CallbackInfo callbackInfo) {
      if (isEntityModelFeaturesLoaded() && isScorchedGunHeldByPlayer(player)) {
         callbackInfo.cancel();
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

   /** EMF's own view of the entity currently being animated, if it exposes one. */
   public static Object getEntityModelFeaturesContextEntity() {
      try {
         return contextGetEntityMethod().invoke(null);
      } catch (ReflectiveOperationException | LinkageError exception) {
         contextFailure(exception);
         return null;
      }
   }

   /** Whether EMF is currently animating a first-person hand, which is when its pose has to be respected. */
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

      Object entity = getEntityModelFeaturesContextEntity();
      if (entity instanceof Player player) {
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
