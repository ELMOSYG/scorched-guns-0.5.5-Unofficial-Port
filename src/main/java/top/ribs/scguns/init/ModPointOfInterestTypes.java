package top.ribs.scguns.init;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

// The class used to carry @EventBusSubscriber(bus = Bus.MOD) as well, but it declares no
// @SubscribeEvent methods and its REGISTER is registered explicitly from ScorchedGuns.
// Forge ignored a subscriber with nothing to subscribe; NeoForge throws "class ... has no
// @SubscribeEvent methods, but register was called anyway", so the annotation is gone.
public final class ModPointOfInterestTypes {
   public static final DeferredRegister<PoiType> REGISTER = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, "scguns");

   public ModPointOfInterestTypes() {
      super();
   }

   private static DeferredHolder<PoiType, PoiType> register(String name, DeferredHolder<Block, Block> block, int maxFreeTickets) {
      List<DeferredHolder<Block, Block>> blocks = new ArrayList<>();
      blocks.add(block);
      return register(name, blocks, maxFreeTickets);
   }

   private static DeferredHolder<PoiType, PoiType> register(String name, Supplier<List<DeferredHolder<Block, Block>>> supplier, int maxFreeTickets) {
      return register(name, supplier.get(), maxFreeTickets);
   }

   private static DeferredHolder<PoiType, PoiType> register(String name, List<DeferredHolder<Block, Block>> blocks, int maxFreeTickets) {
      return REGISTER.register(name, () -> {
         Set<BlockState> blockStates = new HashSet<>();

         for (DeferredHolder<Block, Block> block : blocks) {
            blockStates.addAll(((Block)block.get()).getStateDefinition().getPossibleStates());
         }

         return new PoiType(blockStates, maxFreeTickets, 1);
      });
   }
}
