package top.ribs.scguns.client.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import software.bernie.geckolib.cache.object.GeoBone;
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

   // Arms are baked once from a vanilla player mesh and drawn from that copy. With EMF installed the player model's
   // own parts carry EMF's poses and an inflated outer sleeve, which is why the arm followed the gun while the sleeve
   // stayed behind: two different models, only one of them posed from the gun's bone.
   private static ModelPart regularRightArm;
   private static ModelPart regularRightSleeve;
   private static ModelPart regularLeftArm;
   private static ModelPart regularLeftSleeve;
   private static ModelPart slimRightArm;
   private static ModelPart slimRightSleeve;
   private static ModelPart slimLeftArm;
   private static ModelPart slimLeftSleeve;

   /**
    * Draws one first-person arm and its sleeve from the baked copy, posed from the gun's own bone.
    *
    * @return true when it drew, so the caller skips the player model's parts
    */
   public static boolean renderCleanAnimatedArm(
      LocalPlayer player, HumanoidArm arm, PoseStack poseStack, MultiBufferSource buffer, ResourceLocation skin,
      GeoBone bone, int light, int overlay
   ) {
      if (!isEntityModelFeaturesLoaded() || !isScorchedGunHeldByPlayer(player)) {
         return false;
      }

      boolean slim = "slim".equals(player.getSkin().model().id());
      ensureArmParts(slim);
      ModelPart armPart = armPart(slim, arm);
      ModelPart sleevePart = sleevePart(slim, arm);
      if (armPart == null || sleevePart == null) {
         return false;
      }

      poseStack.scale(0.66F, arm == HumanoidArm.RIGHT ? 0.78F : 0.79F, 0.66F);
      poseStack.translate(arm == HumanoidArm.RIGHT ? 0.25 : -0.25, -0.1, 0.1625);
      poseFromBone(armPart, bone);
      armPart.render(poseStack, buffer.getBuffer(RenderType.entitySolid(skin)), light, overlay);
      poseFromBone(sleevePart, bone);
      sleevePart.render(poseStack, buffer.getBuffer(RenderType.entityTranslucent(skin)), light, overlay);
      return true;
   }

   private static void poseFromBone(ModelPart part, GeoBone bone) {
      part.resetPose();
      part.visible = true;
      part.skipDraw = false;
      part.xScale = 1.0F;
      part.yScale = 1.0F;
      part.zScale = 1.0F;
      part.setPos(bone.getPivotX(), bone.getPivotY(), bone.getPivotZ());
      part.setRotation(0.0F, 0.0F, 0.0F);
   }

   private static void ensureArmParts(boolean slim) {
      if (slim) {
         if (slimRightArm == null) {
            bakeArmParts(true);
         }
      } else if (regularRightArm == null) {
         bakeArmParts(false);
      }
   }

   private static void bakeArmParts(boolean slim) {
      LayerDefinition definition = LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, slim), 64, 64);
      ModelPart root = definition.bakeRoot();
      if (slim) {
         slimRightArm = root.getChild("right_arm");
         slimRightSleeve = root.getChild("right_sleeve");
         slimLeftArm = root.getChild("left_arm");
         slimLeftSleeve = root.getChild("left_sleeve");
      } else {
         regularRightArm = root.getChild("right_arm");
         regularRightSleeve = root.getChild("right_sleeve");
         regularLeftArm = root.getChild("left_arm");
         regularLeftSleeve = root.getChild("left_sleeve");
      }
   }

   private static ModelPart armPart(boolean slim, HumanoidArm arm) {
      if (slim) {
         return arm == HumanoidArm.RIGHT ? slimRightArm : slimLeftArm;
      }

      return arm == HumanoidArm.RIGHT ? regularRightArm : regularLeftArm;
   }

   private static ModelPart sleevePart(boolean slim, HumanoidArm arm) {
      if (slim) {
         return arm == HumanoidArm.RIGHT ? slimRightSleeve : slimLeftSleeve;
      }

      return arm == HumanoidArm.RIGHT ? regularRightSleeve : regularLeftSleeve;
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
