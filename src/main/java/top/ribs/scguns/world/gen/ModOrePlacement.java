package top.ribs.scguns.world.gen;

import java.util.List;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

public class ModOrePlacement {
   public ModOrePlacement() {
      super();
   }

   public static List<PlacementModifier> orePlacement(PlacementModifier p_195347_, PlacementModifier p_195348_) {
      return List.of(p_195347_, InSquarePlacement.spread(), p_195348_, BiomeFilter.biome());
   }

   public static List<PlacementModifier> commonOrePlacement(int p_195344_, PlacementModifier p_195345_) {
      return orePlacement(CountPlacement.of(p_195344_), p_195345_);
   }
}
