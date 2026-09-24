package top.ribs.scguns.init;


import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import com.google.common.collect.ImmutableSet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModVillagers {
   public static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, "scguns");
   public static final DeferredRegister<VillagerProfession> VILLAGER_PROFESSIONS = DeferredRegister.create(BuiltInRegistries.VILLAGER_PROFESSION, "scguns");
   public static final DeferredHolder<PoiType, PoiType> GUNSMITH_POI = POI_TYPES.register(
      "gunsmith_poi", () -> new PoiType(ImmutableSet.copyOf(((Block)ModBlocks.GUN_BENCH.get()).getStateDefinition().getPossibleStates()), 1, 1)
   );
   public static final DeferredHolder<VillagerProfession, VillagerProfession> GUNSMITH = VILLAGER_PROFESSIONS.register(
      "gunsmith",
      () -> new VillagerProfession(
            "gunsmith",
            holder -> holder.value() == GUNSMITH_POI.get(),
            holder -> holder.value() == GUNSMITH_POI.get(),
            ImmutableSet.of(),
            ImmutableSet.of(),
            SoundEvents.VILLAGER_WORK_ARMORER
         )
   );

   public ModVillagers() {
      super();
   }

   public static void register(IEventBus eventBus) {
      POI_TYPES.register(eventBus);
      VILLAGER_PROFESSIONS.register(eventBus);
   }
}
