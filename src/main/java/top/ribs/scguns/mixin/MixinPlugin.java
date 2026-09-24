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

   public MixinPlugin() {
      super();
   }

   public void onLoad(String mixinPackage) {
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
    * Always apply. Nothing under {@code top.ribs.scguns.mixin} references Framework
    * (verified: the only occurrence of "mrcrayfish"/"framework" in the package is
    * the probe above), so the probe must never decide whether the mixins load.
    *
    * <p>0.5.5 returned {@code isFrameworkInstalled} here. Because the probe used the
    * 1.20.1 class name, on NeoForge it was always false and Mixin skipped
    * <b>every</b> mixin in scguns.mixins.json. That is why no mob arm pose, no
    * third-person gun rendering through the gun renderer and no player gun-hold
    * pose existed -- and why the failure was invisible: a skipped mixin logs
    * nothing at normal log levels. The probe now only drives the warning above.</p>
    */
   public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
      return true;
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
