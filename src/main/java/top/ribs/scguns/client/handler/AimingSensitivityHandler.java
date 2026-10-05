package top.ribs.scguns.client.handler;

import java.util.Map;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import top.ribs.scguns.Config;
import top.ribs.scguns.ScorchedGuns;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.init.ModSyncedDataKeys;
import top.ribs.scguns.item.GunItem;

/**
 * Scales mouse sensitivity while aiming down sights by writing the vanilla sensitivity option.
 *
 * <p>Tweakeroo's method, no mixin: remember the player's sensitivity, write the scaled value while aiming,
 * put the original back as soon as aiming stops.</p>
 *
 * <p>Which value: a scope attachment now decides it directly. The long scope is 25% sensitivity and the
 * medium scope 50%; those are the values the scopes are meant to have, rather than something derived from
 * the FOV modifier. Guns without one of those scopes (iron sights, or an unknown scope from an add-on)
 * keep the previous behaviour: the configured ADS factor blended in by the aiming progress, times a factor
 * derived from the gun's FOV. That means the config value only affects unscoped aiming now - the two named
 * scopes are exact.</p>
 *
 * <p>Two conditions had to be got right, each wrong once: {@code AimingHandler.isAiming()} reads false while
 * aiming through a scope, so gating on it made scoped guns use 1.0; and restoring on "ADS progress reached
 * zero" never restored, because the progress can stay above zero once the animation's decay stalls. The
 * aiming test used here is the one {@code AimTracker.handleAiming} uses to drive that animation.</p>
 */
public final class AimingSensitivityHandler {
   /** Sensitivity while fully aimed, per scope item. Long scope 25%, medium scope 50%. */
   private static final Map<ResourceLocation, Double> SCOPE_SENSITIVITY = Map.of(
      ResourceLocation.fromNamespaceAndPath("scguns", "long_scope"), 0.25D,
      ResourceLocation.fromNamespaceAndPath("scguns", "medium_scope"), 0.5D
   );

   /** The player's real sensitivity while we are overriding it; null means "not overriding". */
   private static Double savedSensitivity;
   private static long lastProbe;

   private AimingSensitivityHandler() {
   }

   /** Runs each client tick; safe to call before a world is loaded. */
   public static void onClientTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.options == null) {
         restore(mc);
         return;
      }

      // Apply on the ADS progress, the same test the FOV zoom uses: it is non-zero while aiming
      // through a scope, whereas AIMING and AimingHandler.isAiming() both read false there.
      double progress = AimingHandler.get().getNormalisedAdsProgress();
      double multiplier = progress != 0.0 ? aimingSensitivityMultiplier() : 1.0;
      if (multiplier == 1.0) {
         restore(mc);
      } else {
         apply(mc, multiplier);
      }

      long now = System.currentTimeMillis();
      if (now - lastProbe > 1000L) {
         lastProbe = now;
         AimingHandler handler = AimingHandler.get();
         ItemStack held = mc.player.getMainHandItem();
         ScorchedGuns.LOGGER.info(
            "SCGUNS-ADS multiplier={} ads={} scope={} progress={} isAiming={} option={} saved={}",
            multiplier, aimingDownSights(mc), scopeName(held), handler.getNormalisedAdsProgress(),
            handler.isAiming(), mc.options.sensitivity().get(), savedSensitivity);
      }
   }

   /**
    * Whether the player is aiming down sights, using the condition {@code AimTracker.handleAiming} uses to
    * drive the ADS animation: the synced aiming key, or the local handler's own aiming state.
    */
   private static boolean aimingDownSights(Minecraft mc) {
      return (Boolean)ModSyncedDataKeys.AIMING.getValue(mc.player) || AimingHandler.get().isAiming();
   }

   private static void apply(Minecraft mc, double multiplier) {
      OptionInstance<Double> sensitivity = mc.options.sensitivity();
      if (savedSensitivity == null) {
         savedSensitivity = sensitivity.get();
      }

      sensitivity.set(Mth.clamp(savedSensitivity * multiplier, 0.0, 1.0));
   }

   private static void restore(Minecraft mc) {
      if (savedSensitivity == null) {
         return;
      }

      if (mc.options != null) {
         mc.options.sensitivity().set(savedSensitivity);
      }
      savedSensitivity = null;
   }

   /** Multiplier for the sensitivity option: exactly 1.0 when not aiming down sights. */
   public static double aimingSensitivityMultiplier() {
      double progress = AimingHandler.get().getNormalisedAdsProgress();
      ItemStack heldItem = heldGun();

      Double scopeValue = scopeSensitivity(heldItem);
      if (scopeValue != null) {
         // The scope's own value is the final one at full ADS, blended in as the player raises the gun.
         return 1.0 - (1.0 - scopeValue) * progress;
      }

      double adsSensitivity = (Double)Config.CLIENT.controls.aimDownSightSensitivity.get();
      return (1.0 - (1.0 - adsSensitivity) * progress) * (double)ironSightFactor(heldItem);
   }

   /** The held item, if it is a gun, else empty. */
   private static ItemStack heldGun() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         return ItemStack.EMPTY;
      }
      ItemStack held = mc.player.getMainHandItem();
      return held.getItem() instanceof GunItem ? held : ItemStack.EMPTY;
   }

   /** The configured sensitivity for the scope fitted to this gun, or null if it has no known scope. */
   private static Double scopeSensitivity(ItemStack gun) {
      if (gun.isEmpty() || Minecraft.getInstance().options.getCameraType() != CameraType.FIRST_PERSON) {
         return null;
      }
      if ((Boolean)ModSyncedDataKeys.RELOADING.getValue(Minecraft.getInstance().player)) {
         return null;
      }

      ItemStack scope = Gun.getScopeStack(gun);
      return scope.isEmpty() ? null : SCOPE_SENSITIVITY.get(BuiltInRegistries.ITEM.getKey(scope.getItem()));
   }

   /** Fallback for guns with no known scope: a factor derived from the gun's FOV modifier. */
   private static float ironSightFactor(ItemStack gun) {
      if (gun.isEmpty() || Minecraft.getInstance().options.getCameraType() != CameraType.FIRST_PERSON) {
         return 1.0F;
      }
      if ((Boolean)ModSyncedDataKeys.RELOADING.getValue(Minecraft.getInstance().player)) {
         return 1.0F;
      }

      Gun modifiedGun = ((GunItem)gun.getItem()).getModifiedGun(gun);
      if (modifiedGun.getModules().getZoom() == null) {
         return 1.0F;
      }

      float modifier = Mth.clamp(Gun.getFovModifier(gun, modifiedGun), 0.1F, 10.0F);
      return Mth.clamp((float)Math.pow((double)modifier, 0.25), 0.5F, 1.0F);
   }

   /** Scope item id for the log, or "none". */
   private static String scopeName(ItemStack gun) {
      if (gun.isEmpty()) {
         return "none";
      }
      ItemStack scope = Gun.getScopeStack(gun);
      return scope.isEmpty() ? "none" : BuiltInRegistries.ITEM.getKey(scope.getItem()).toString();
   }
}
