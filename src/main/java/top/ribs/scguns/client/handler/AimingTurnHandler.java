package top.ribs.scguns.client.handler;

import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import top.ribs.scguns.Config;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.init.ModSyncedDataKeys;
import top.ribs.scguns.item.GunItem;

/**
 * Scales the look speed while aiming down sights.
 *
 * <p>The mechanism is taken from the open-source 1.21.1 port whose jar sits in the instance's "其他移植"
 * folder (ScorchedGuns-1.5.jar). It listens to NeoForge's {@code CalculatePlayerTurnEvent} and converts the
 * wanted turn multiplier into a sensitivity value by inverting the vanilla curve:</p>
 *
 * <pre>
 * vanilla turn scale = v^3 * 8   with   v = sensitivity * 0.6 + 0.2
 * to multiply the turn by m:  v' = cbrt(v^3 * m),  sensitivity' = (v' - 0.2) / 0.6
 * </pre>
 *
 * <p>That inversion is what earlier attempts in this port missed. Scaling the event's sensitivity directly
 * (or writing the sensitivity option, Tweakeroo-style) ignores the {@code +0.2} offset, so the strength came
 * out wrong; writing the option also left persistent state, which caused the "sensitivity never goes back
 * after aiming" bug. The event carries a value per turn and nothing is stored, so that whole class of
 * problem cannot occur.</p>
 *
 * <p>The multiplier is driven by {@code getNormalisedAdsProgress()}, the same test the FOV zoom uses - it is
 * non-zero while aiming through a scope, whereas {@code AIMING} and {@code AimingHandler.isAiming()} read
 * false for scoped aiming and must not be used here. A scope attachment sets the value directly (long scope
 * 25%, medium scope 50%); guns without one use the configured ADS sensitivity, which reproduces 0.5.5's
 * behaviour of multiplying the final turn.</p>
 */
public final class AimingTurnHandler {
   /** Turn multiplier while fully aimed, per scope item. */
   private static final Map<ResourceLocation, Double> SCOPE_SENSITIVITY = Map.of(
      ResourceLocation.fromNamespaceAndPath("scguns", "long_scope"), 0.25D,
      ResourceLocation.fromNamespaceAndPath("scguns", "medium_scope"), 0.5D
   );

   /** Vanilla curve constants, straight from MouseHandler.turnPlayer. */
   private static final double CURVE_SLOPE = 0.6D;
   private static final double CURVE_OFFSET = 0.2D;

   private AimingTurnHandler() {
   }

   @SubscribeEvent
   public static void onCalculatePlayerTurn(CalculatePlayerTurnEvent event) {
      double multiplier = aimingMultiplier();
      if (multiplier >= 0.999D) {
         return;
      }

      double sensitivity = event.getMouseSensitivity();
      double curved = sensitivity * CURVE_SLOPE + CURVE_OFFSET;
      double scaled = Math.cbrt(curved * curved * curved * multiplier);
      event.setMouseSensitivity(Math.max(0.0D, (scaled - CURVE_OFFSET) / CURVE_SLOPE));
   }

   /** Turn multiplier for the current aiming state: exactly 1.0 when not aiming down sights. */
   private static double aimingMultiplier() {
      double progress = AimingHandler.get().getNormalisedAdsProgress();
      if (progress <= 0.0D) {
         return 1.0D;
      }

      Double scope = scopeValue();
      double base = scope != null
         ? scope
         : (Double)Config.CLIENT.controls.aimDownSightSensitivity.get();
      return 1.0D - (1.0D - base) * progress;
   }

   /** The configured turn multiplier for the scope fitted to the held gun, or null if there is none. */
   private static Double scopeValue() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || (Boolean)ModSyncedDataKeys.RELOADING.getValue(mc.player)) {
         return null;
      }

      ItemStack held = mc.player.getMainHandItem();
      if (!(held.getItem() instanceof GunItem)) {
         return null;
      }

      ItemStack scope = Gun.getScopeStack(held);
      return scope.isEmpty() ? null : SCOPE_SENSITIVITY.get(BuiltInRegistries.ITEM.getKey(scope.getItem()));
   }
}
