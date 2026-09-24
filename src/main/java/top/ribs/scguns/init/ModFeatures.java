package top.ribs.scguns.init;


import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import top.ribs.scguns.world.NiterPatchConfiguration;
import top.ribs.scguns.world.NiterPatchFeature;
import top.ribs.scguns.world.VentFeature;
import top.ribs.scguns.world.VentFeatureConfiguration;

// The class used to carry @EventBusSubscriber(bus = Bus.MOD) as well, but it declares no
// @SubscribeEvent methods and is registered explicitly from ScorchedGuns. Forge ignored
// a subscriber with nothing to subscribe; NeoForge throws "class ... has no
// @SubscribeEvent methods, but register was called anyway", so the annotation is gone.
public class ModFeatures {
   public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(BuiltInRegistries.FEATURE, "scguns");
   public static final DeferredHolder<Feature<?>, Feature<VentFeatureConfiguration>> VENT_FEATURE = FEATURES.register(
      "vent", () -> new VentFeature(VentFeatureConfiguration.CODEC)
   );
   public static final DeferredHolder<Feature<?>, Feature<NiterPatchConfiguration>> NITER_PATCH = FEATURES.register(
      "niter_patch", () -> new NiterPatchFeature(NiterPatchConfiguration.CODEC)
   );

   public ModFeatures() {
      super();
   }

   public static void register(IEventBus bus) {
      FEATURES.register(bus);
   }
}
