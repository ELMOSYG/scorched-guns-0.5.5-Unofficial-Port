package top.ribs.scguns.world;

import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool.Projection;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

public class VillageStructures {
   public VillageStructures() {
      super();
   }

   public static void addNewVillageBuilding(ServerAboutToStartEvent event) {
      Registry<StructureTemplatePool> templatePools = (Registry<StructureTemplatePool>)event.getServer().registryAccess().registry(Registries.TEMPLATE_POOL).get();
      Registry<StructureProcessorList> processorLists = (Registry<StructureProcessorList>)event.getServer().registryAccess().registry(Registries.PROCESSOR_LIST).get();
      addBuildingToPool(
         templatePools, processorLists, ResourceLocation.parse("minecraft:village/plains/houses"), "scguns:village/houses/plains_gunsmith_house", 8
      );
      addBuildingToPool(templatePools, processorLists, ResourceLocation.parse("minecraft:village/snowy/houses"), "scguns:village/houses/snowy_gunsmith_house", 8);
      addBuildingToPool(
         templatePools, processorLists, ResourceLocation.parse("minecraft:village/savanna/houses"), "scguns:village/houses/savanna_gunsmith_house", 8
      );
      addBuildingToPool(
         templatePools, processorLists, ResourceLocation.parse("minecraft:village/desert/houses"), "scguns:village/houses/desert_gunsmith_house", 8
      );
      addBuildingToPool(templatePools, processorLists, ResourceLocation.parse("minecraft:village/taiga/houses"), "scguns:village/houses/taiga_gunsmith_house", 8);
   }

   public static void addBuildingToPool(
      Registry<StructureTemplatePool> templatePoolRegistry,
      Registry<StructureProcessorList> processorListRegistry,
      ResourceLocation poolRL,
      String nbtPieceRL,
      int weight
   ) {
      StructureTemplatePool pool = (StructureTemplatePool)templatePoolRegistry.get(poolRL);
      ResourceLocation emptyProcessor = ResourceLocation.fromNamespaceAndPath("minecraft", "empty");
      Holder<StructureProcessorList> processorHolder = processorListRegistry.getHolderOrThrow(ResourceKey.create(Registries.PROCESSOR_LIST, emptyProcessor));
      SinglePoolElement piece = (SinglePoolElement)SinglePoolElement.single(nbtPieceRL, processorHolder).apply(Projection.RIGID);

      for (int i = 0; i < weight; i++) {
         assert pool != null;

         pool.templates.add(piece);
      }

      List<Pair<StructurePoolElement, Integer>> listOfPieceEntries = new ArrayList<>(pool.rawTemplates);
      listOfPieceEntries.add(new Pair(piece, weight));
      pool.rawTemplates = listOfPieceEntries;
   }
}
