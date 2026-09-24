package top.ribs.scguns.init;


import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModPaintings {
   public static final DeferredRegister<PaintingVariant> REGISTER = DeferredRegister.create(Registries.PAINTING_VARIANT, "scguns");
   // 0.5.5 declared this painting as 64x32, but PaintingVariant's width/height are in
   // BLOCKS and its codec only accepts 1..16 (ExtraCodecs.intRange(1, 16)), so the
   // datapack entry failed to parse and took the whole registry load down with it.
   // The shipped texture is 64x32 pixels, i.e. 4x2 blocks.
   public static final DeferredHolder<PaintingVariant, PaintingVariant> THE_COLLECTIVE = REGISTER.register(
      "the_collective",
      () -> new PaintingVariant(4, 2, ResourceLocation.fromNamespaceAndPath("scguns", "the_collective"))
   );

   public ModPaintings() {
      super();
   }
}
