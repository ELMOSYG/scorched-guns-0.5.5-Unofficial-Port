package top.ribs.scguns.compat;

import com.illusivesoulworks.polymorph.api.client.PolymorphWidgets;
import top.ribs.scguns.client.screen.GunBenchScreen;

/**
 * The half of the client integration that touches Polymorph types.
 *
 * <p>Nothing refers to this class by name except a reflective lookup, and it carries no annotation, so FML's
 * annotation scan never resolves it. That matters: the entry point it replaces used to hold the Polymorph types
 * directly, which meant the scan resolved them while the game was still constructing mods. When Polymorph is
 * absent - or present at a version whose {@code api.client.base} no longer contains the interface our widget
 * implements - that resolution failed with NoClassDefFoundError and the whole mod refused to load, which is an
 * unplayable state rather than a missing feature. Loading it only when the feature is actually wanted turns the
 * same situation into a logged warning and a gun bench without a recipe selector.</p>
 */
public final class GunBenchPolymorphWidgets {
   private GunBenchPolymorphWidgets() {
   }

   public static void install() {
      PolymorphWidgets.getInstance().registerWidget(
         screen -> screen instanceof GunBenchScreen bench ? new GunBenchRecipesWidget(bench) : null);
   }
}
