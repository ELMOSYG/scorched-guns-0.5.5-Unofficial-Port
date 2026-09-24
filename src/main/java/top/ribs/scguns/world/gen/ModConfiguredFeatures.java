package top.ribs.scguns.world.gen;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration.TargetBlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import top.ribs.scguns.init.ModBlocks;
import top.ribs.scguns.init.ModFeatures;
import top.ribs.scguns.world.NiterPatchConfiguration;

public class ModConfiguredFeatures {
   public static final ResourceKey<ConfiguredFeature<?, ?>> ANTHRALITE_ORE_KEY = registerKey("anthralite_ore");
   public static final ResourceKey<ConfiguredFeature<?, ?>> SULFUR_ORE_KEY = registerKey("sulfur_ore");
   public static final ResourceKey<ConfiguredFeature<?, ?>> NETHER_SULFUR_ORE_KEY = registerKey("nether_sulfur_ore");
   public static final ResourceKey<ConfiguredFeature<?, ?>> VEHEMENT_COAL_ORE_KEY = registerKey("vehement_coal_ore");
   public static final ResourceKey<ConfiguredFeature<?, ?>> RICH_PHOSPHORITE_ORE_KEY = registerKey("rich_phosphorite");
   public static final ResourceKey<ConfiguredFeature<?, ?>> PHOSPHORITE_KEY = registerKey("phosphorite");
   public static final ResourceKey<ConfiguredFeature<?, ?>> NITER_CAVE_PATCH_KEY = registerKey("niter_cave_patch");

   public ModConfiguredFeatures() {
      super();
   }

   public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
      RuleTest stoneReplaceables = new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES);
      RuleTest deepslateReplaceables = new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
      RuleTest netherrackReplaceables = new BlockMatchTest(Blocks.NETHERRACK);
      RuleTest phosphoriteReplaceables = new BlockMatchTest((Block)ModBlocks.PHOSPHORITE.get());
      List<TargetBlockState> richPhosphoriteOres = List.of(
         OreConfiguration.target(phosphoriteReplaceables, ((Block)ModBlocks.RICH_PHOSPHORITE.get()).defaultBlockState())
      );
      List<TargetBlockState> phosphoriteOres = List.of(OreConfiguration.target(stoneReplaceables, ((Block)ModBlocks.PHOSPHORITE.get()).defaultBlockState()));
      register(context, PHOSPHORITE_KEY, Feature.ORE, new OreConfiguration(phosphoriteOres, 48));
      register(context, RICH_PHOSPHORITE_ORE_KEY, Feature.ORE, new OreConfiguration(richPhosphoriteOres, 12));
      List<TargetBlockState> overworldAnthraliteOres = List.of(
         OreConfiguration.target(stoneReplaceables, ((Block)ModBlocks.ANTHRALITE_ORE.get()).defaultBlockState()),
         OreConfiguration.target(deepslateReplaceables, ((Block)ModBlocks.DEEPSLATE_ANTHRALITE_ORE.get()).defaultBlockState())
      );
      List<TargetBlockState> overworldSulfurOres = List.of(
         OreConfiguration.target(stoneReplaceables, ((Block)ModBlocks.SULFUR_ORE.get()).defaultBlockState()),
         OreConfiguration.target(deepslateReplaceables, ((Block)ModBlocks.DEEPSLATE_SULFUR_ORE.get()).defaultBlockState())
      );
      List<TargetBlockState> netherSulfurOres = List.of(
         OreConfiguration.target(netherrackReplaceables, ((Block)ModBlocks.NETHER_SULFUR_ORE.get()).defaultBlockState())
      );
      List<TargetBlockState> netherVehementCoalOres = List.of(
         OreConfiguration.target(netherrackReplaceables, ((Block)ModBlocks.VEHEMENT_COAL_ORE.get()).defaultBlockState())
      );
      register(context, ANTHRALITE_ORE_KEY, Feature.ORE, new OreConfiguration(overworldAnthraliteOres, 10));
      register(context, SULFUR_ORE_KEY, Feature.ORE, new OreConfiguration(overworldSulfurOres, 9));
      register(context, NETHER_SULFUR_ORE_KEY, Feature.ORE, new OreConfiguration(netherSulfurOres, 12));
      register(context, VEHEMENT_COAL_ORE_KEY, Feature.ORE, new OreConfiguration(netherVehementCoalOres, 6));
      register(
         context, NITER_CAVE_PATCH_KEY, (Feature)ModFeatures.NITER_PATCH.get(), new NiterPatchConfiguration((Block)ModBlocks.NITER_LAYER.get(), 1, 3, 3, 16)
      );
   }

   public static ResourceKey<ConfiguredFeature<?, ?>> registerKey(String name) {
      return ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath("scguns", name));
   }

   private static <FC extends FeatureConfiguration, F extends Feature<FC>> void register(
      BootstrapContext<ConfiguredFeature<?, ?>> context, ResourceKey<ConfiguredFeature<?, ?>> key, F feature, FC configuration
   ) {
      context.register(key, new ConfiguredFeature(feature, configuration));
   }
}
