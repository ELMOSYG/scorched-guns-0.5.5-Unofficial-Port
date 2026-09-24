package top.ribs.scguns.init;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import top.ribs.scguns.world.CogChambers;

public class ModStructures {
   public static final DeferredRegister<StructureType<?>> REGISTRY = DeferredRegister.create(Registries.STRUCTURE_TYPE, "scguns");
   public static final DeferredHolder<StructureType<?>, StructureType<CogChambers>> CHAMBER = REGISTRY.register("cogchambers", () -> stuff(CogChambers.CODEC));

   public ModStructures() {
      super();
   }

   private static <T extends Structure> StructureType<T> stuff(MapCodec<T> codec) {
      return () -> codec;
   }
}
