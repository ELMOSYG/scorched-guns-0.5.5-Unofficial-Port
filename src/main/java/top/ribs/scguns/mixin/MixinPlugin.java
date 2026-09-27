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
    * Guard Villagers is looked up in the <b>mod list</b>, not by loading one of its classes.
    *
    * <p>Probing {@code tallestegg.guardvillagers.common.entities.Guard} looks harmless and is fatal: the
    * probe runs while Mixin is preparing configs, and {@code Guard} extends {@code PathfinderMob}, so
    * {@code Mob} and then {@code LivingEntity} are loaded at that moment. Every mod that mixes into
    * {@code LivingEntity} - GeckoLib and Curios both do - then dies with
    * {@code MixinTargetAlreadyLoadedException: target net.minecraft.world.entity.LivingEntity was loaded too
    * early}, and the crash report names that innocent mod rather than this probe. {@code LoadingModList}
    * answers the same question without touching Minecraft at all (HANDOFF section 82.10).</p>
    */
   private boolean guardVillagersProbed;
   private boolean isGuardVillagersInstalled = true;

   public MixinPlugin() {
      super();
   }

   public void onLoad(String mixinPackage) {
      this.isGuardVillagersInstalled = isModLoaded("guardvillagers");

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

   /**
    * Whether Guard Villagers is in the mod list, by reflection into {@code LoadingModList} so the class is
    * only referenced if it exists.
    *
    * <p><b>Fails open</b>: if the lookup cannot be made, the guard mixins are applied, and because they are
    * {@code @Pseudo} Mixin skips the ones whose target is missing - which is exactly what happens without
    * Guard Villagers. Failing closed would silently disable them with the mod installed, which is the one
    * failure mode this class exists to prevent.</p>
    */
   private static boolean isModLoaded(String modId) {
      try {
         Class<?> loadingModList = Class.forName("net.neoforged.fml.loading.LoadingModList",
            false, MixinPlugin.class.getClassLoader());
         Object modList = loadingModList.getMethod("get").invoke(null);
         return loadingModList.getMethod("getModFileById", String.class).invoke(modList, modId) != null;
      } catch (Throwable notDeterminable) {
         return true;
      }
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
         // Re-probe if onLoad somehow never ran, so the gate cannot hinge on call order.
         if (!this.guardVillagersProbed) {
            this.guardVillagersProbed = true;
            this.isGuardVillagersInstalled = isModLoaded("guardvillagers");
         }

         return this.isGuardVillagersInstalled;
      }

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
