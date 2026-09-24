package top.ribs.scguns.init;



import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import top.ribs.scguns.fluid.ViciousAcidFluid;
import top.ribs.scguns.fluid.ViciousAcidFluidType;

public class ModFluids {
   public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, "scguns");
   public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(BuiltInRegistries.FLUID, "scguns");
   public static final DeferredHolder<FluidType, FluidType> VICIOUS_ACID_FLUID_TYPE = FLUID_TYPES.register(
      "vicious_acid",
      () -> new ViciousAcidFluidType(ResourceLocation.fromNamespaceAndPath("scguns", "block/vicious_acid_still"), ResourceLocation.fromNamespaceAndPath("scguns", "block/vicious_acid_flow"))
   );
   public static final DeferredHolder<Fluid, FlowingFluid> VICIOUS_ACID_SOURCE = FLUIDS.register("vicious_acid_source", ViciousAcidFluid.Source::new);
   public static final DeferredHolder<Fluid, FlowingFluid> VICIOUS_ACID_FLOWING = FLUIDS.register("vicious_acid_flowing", ViciousAcidFluid.Flowing::new);

   public ModFluids() {
      super();
   }
}
