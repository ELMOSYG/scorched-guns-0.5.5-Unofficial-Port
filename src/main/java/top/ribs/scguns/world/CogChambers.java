package top.ribs.scguns.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationContext;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationStub;
import net.minecraft.world.level.levelgen.structure.Structure.StructureSettings;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import top.ribs.scguns.init.ModStructures;

public class CogChambers extends Structure {
   public static final MapCodec<CogChambers> CODEC = RecordCodecBuilder.<CogChambers>mapCodec(
         codex -> codex.group(
                  settingsCodec(codex),
                  StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(structure -> structure.startPool),
                  HeightProvider.CODEC.fieldOf("start_height").forGetter(structure -> structure.startHeight),
                  Types.CODEC.optionalFieldOf("heightmap").forGetter(structure -> structure.heightmap)
               )
               .apply(codex, CogChambers::new)
      );
   public final Holder<StructureTemplatePool> startPool;
   public final HeightProvider startHeight;
   public final Optional<Types> heightmap;

   public CogChambers(StructureSettings config, Holder<StructureTemplatePool> pool, HeightProvider height, Optional<Types> map) {
      super(config);
      this.startPool = pool;
      this.startHeight = height;
      this.heightmap = map;
   }

   public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
      BlockPos pos = new BlockPos(
         context.chunkPos().getMinBlockX(),
         this.startHeight.sample(context.random(), new WorldGenerationContext(context.chunkGenerator(), context.heightAccessor())),
         context.chunkPos().getMinBlockZ()
      );
      return JigsawPlacement.addPieces(
         context,
         this.startPool,
         Optional.empty(),
         7,
         pos,
         false,
         this.heightmap,
         128,
         PoolAliasLookup.EMPTY,
         DimensionPadding.ZERO,
         LiquidSettings.APPLY_WATERLOGGING
      );
   }

   public StructureType<?> type() {
      return (StructureType<?>)ModStructures.CHAMBER.get();
   }
}
