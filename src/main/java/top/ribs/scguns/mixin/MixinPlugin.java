package top.ribs.scguns.mixin;

import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class MixinPlugin implements IMixinConfigPlugin {
   private static final Logger LOGGER = LogManager.getLogger("scguns-mixin");

   /**
    * Framework's bootstrap class, per loader. The name changed when Framework moved
    * to NeoForge: 1.20.1 Forge shipped {@code FrameworkForge}, NeoForge ships
    * {@code FrameworkNeoForge}.
    */
   private static final String[] FRAMEWORK_BOOTSTRAP = {
      "com.mrcrayfish.framework.FrameworkNeoForge",
      "com.mrcrayfish.framework.FrameworkForge",
   };

   private boolean isFrameworkInstalled;

   /**
    * Guard Villagers, probed by its Guard class - the same shape as the Framework probe above, and the same
    * caveat: if that class is ever renamed, this gate silently skips the guard compat's mixins (the guard
    * simply keeps its melee goal and loses nothing else), so it is a nuisance, not a crash.
    */
   private static final String GUARD_VILLAGERS_GUARD = "tallestegg.guardvillagers.common.entities.Guard";

   private boolean isGuardVillagersInstalled;
   private boolean guardProbed;

   public MixinPlugin() {
      super();
   }

   public void onLoad(String mixinPackage) {
      // Probed here, not in acceptTargets: Mixin asks shouldApplyMixin before that, so a flag set there is
      // still false when the gate is read and the guard mixins are skipped without a word - the same silent
      // failure this class exists to prevent.
      this.isGuardVillagersInstalled = isClassPresent(GUARD_VILLAGERS_GUARD);

      for (String candidate : FRAMEWORK_BOOTSTRAP) {
         try {
            Class.forName(candidate, false, this.getClass().getClassLoader());
            this.isFrameworkInstalled = true;
            return;
         } catch (Throwable ignored) {
            // Try the next candidate name.
         }
      }

      this.isFrameworkInstalled = false;
      LOGGER.warn(
         "Framework was not detected (probed {}). The mod's gun renderer therefore "
            + "runs without its mixins; gun-hold poses and third-person gun rendering "
            + "will be missing. If Framework IS installed, its bootstrap class has "
            + "been renamed again and this probe needs updating.",
         String.join(", ", FRAMEWORK_BOOTSTRAP)
      );
   }

   public String getRefMapperConfig() {
      return null;
   }

   /**
    * Everything applies, except the one optional-mod compat (HANDOFF section 82.9).
    *
    * <p>Nothing else under {@code top.ribs.scguns.mixin} references Framework (verified: the only
    * occurrence of "mrcrayfish"/"framework" in the package is the probe above), so the probe must never
    * decide whether those mixins load.</p>
    *
    * <p>0.5.5 returned {@code isFrameworkInstalled} here. Because the probe used the 1.20.1 class name, on
    * NeoForge it was always false and Mixin skipped <b>every</b> mixin in scguns.mixins.json. That is why no
    * mob arm pose, no third-person gun rendering through the gun renderer and no player gun-hold pose
    * existed - and why the failure was invisible: a skipped mixin logs nothing at normal log levels. The
    * probe now only drives the warning above.</p>
    *
    * <p>The guard villager mixins are the deliberate exception, and they are the reason the rule above is a
    * rule rather than a formality: they target a class that only exists when Guard Villagers is installed, so
    * they <b>must</b> be skipped without it. Only that prefix is gated - adding a second gate for anything
    * else would take the whole config down with it the moment its probe goes stale.</p>
    */
   public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
      if (mixinClassName.startsWith("top.ribs.scguns.mixin.common.compat.guardvillagers.")) {
         // Re-probe if onLoad somehow never ran, so the gate cannot fail closed by ordering alone.
         if (!this.isGuardVillagersInstalled && !this.guardProbed) {
            this.guardProbed = true;
            this.isGuardVillagersInstalled = isClassPresent(GUARD_VILLAGERS_GUARD);
         }

         return this.isGuardVillagersInstalled;
      }

      return true;
   }

   private static boolean isClassPresent(String className) {
      try {
         Class.forName(className, false, MixinPlugin.class.getClassLoader());
         return true;
      } catch (Throwable ignored) {
         return false;
      }
   }

   public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
   }

   public List<String> getMixins() {
      return null;
   }

   public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
   }

   public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
   }
}
