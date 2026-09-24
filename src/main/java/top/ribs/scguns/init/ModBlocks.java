package top.ribs.scguns.init;


import net.minecraft.core.registries.BuiltInRegistries;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import top.ribs.scguns.block.AdvancedComposterBlock;
import top.ribs.scguns.block.AmmoBoxBlock;
import top.ribs.scguns.block.AmmoModuleBlock;
import top.ribs.scguns.block.AsgharianPillarBlock;
import top.ribs.scguns.block.AutoTurretBlock;
import top.ribs.scguns.block.BasicDirectionalBlock;
import top.ribs.scguns.block.BasicTurretBlock;
import top.ribs.scguns.block.BatGuanoLayerBlock;
import top.ribs.scguns.block.ChargedAmethystRelayBlock;
import top.ribs.scguns.block.CryoniterBlock;
import top.ribs.scguns.block.DamageModuleBlock;
import top.ribs.scguns.block.EnemyTurretBlock;
import top.ribs.scguns.block.FakeSoulFireBlock;
import top.ribs.scguns.block.FireRateModuleBlock;
import top.ribs.scguns.block.GeothermalVentBlock;
import top.ribs.scguns.block.GrateBlock;
import top.ribs.scguns.block.GuanoCandleBlock;
import top.ribs.scguns.block.GunBenchBlock;
import top.ribs.scguns.block.GunShelfBlock;
import top.ribs.scguns.block.HostileTurretTargetingBlock;
import top.ribs.scguns.block.LightningBattery;
import top.ribs.scguns.block.LightningRodConnectorBlock;
import top.ribs.scguns.block.MaceratorBlock;
import top.ribs.scguns.block.MechanicalPressBlock;
import top.ribs.scguns.block.MemorialBlock;
import top.ribs.scguns.block.MineUnitBlock;
import top.ribs.scguns.block.MobTrapBlock;
import top.ribs.scguns.block.NiterGlassBlock;
import top.ribs.scguns.block.NiterLayerBlock;
import top.ribs.scguns.block.NitroKegBlock;
import top.ribs.scguns.block.PenetratorBlock;
import top.ribs.scguns.block.PhosphorLayerBlock;
import top.ribs.scguns.block.PillarBlock;
import top.ribs.scguns.block.PlayerTurretTargetingBlock;
import top.ribs.scguns.block.PolarGeneratorBlock;
import top.ribs.scguns.block.PowderKegBlock;
import top.ribs.scguns.block.PoweredMaceratorBlock;
import top.ribs.scguns.block.PoweredMechanicalPressBlock;
import top.ribs.scguns.block.RangeModuleBlock;
import top.ribs.scguns.block.SandbagBlock;
import top.ribs.scguns.block.ShellCatcherModuleBlock;
import top.ribs.scguns.block.ShockCoilBlock;
import top.ribs.scguns.block.ShotgunTurretBlock;
import top.ribs.scguns.block.SniperTurretBlock;
import top.ribs.scguns.block.SulfurLayerBlock;
import top.ribs.scguns.block.SulfurVentBlock;
import top.ribs.scguns.block.TemporaryLightBlock;
import top.ribs.scguns.block.ThermolithBlock;
import top.ribs.scguns.block.TurretTargetingBlock;
import top.ribs.scguns.block.VentCollectorBlock;
import top.ribs.scguns.block.ViciousAcidBlock;
import top.ribs.scguns.block.ViciousAcidCauldronBlock;

public class ModBlocks {
   public static final DeferredRegister<Block> REGISTER = DeferredRegister.create(BuiltInRegistries.BLOCK, "scguns");
   public static final DeferredHolder<Block, LiquidBlock> VICIOUS_ACID_BLOCK = REGISTER.register(
      "vicious_acid_block",
      () -> new ViciousAcidBlock(
            ModFluids.VICIOUS_ACID_SOURCE, Properties.ofLegacyCopy(Blocks.WATER).noCollission().strength(100.0F).lightLevel(state -> 4).noLootTable()
         )
   );
   public static final DeferredHolder<Block, Block> VICIOUS_ACID_CAULDRON = REGISTER.register(
      "vicious_acid_cauldron", () -> new ViciousAcidCauldronBlock(Properties.ofLegacyCopy(Blocks.CAULDRON).lightLevel(state -> 4))
   );
   public static final DeferredHolder<Block, Block> FAKE_SOUL_FIRE = REGISTER.register(
      "fake_soul_fire", () -> new FakeSoulFireBlock(Properties.ofLegacyCopy(Blocks.SOUL_FIRE), 2.0F)
   );
   public static final DeferredHolder<Block, Block> TEMPORARY_LIGHT = REGISTER.register("temporary_light", TemporaryLightBlock::new);
   public static final DeferredHolder<Block, Block> GUN_SHELF = register("gun_shelf", () -> new GunShelfBlock(Properties.ofLegacyCopy(Blocks.OAK_PLANKS).strength(0.5F)));
   public static final DeferredHolder<Block, Block> MOB_TRAP = register(
      "mob_trap", () -> new MobTrapBlock(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().strength(12.0F).noOcclusion())
   );
   public static final DeferredHolder<Block, Block> AMMO_BOX = register(
      "ammo_box", () -> new AmmoBoxBlock(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().noOcclusion().strength(2.5F))
   );
   public static final DeferredHolder<Block, Block> ADVANCED_COMPOSTER = register(
      "advanced_composter", () -> new AdvancedComposterBlock(Properties.ofLegacyCopy(Blocks.COMPOSTER).strength(0.5F))
   );
   public static final DeferredHolder<Block, Block> POWDER_KEG = register("powder_keg", () -> new PowderKegBlock(Properties.ofLegacyCopy(Blocks.BARREL).strength(0.5F)));
   public static final DeferredHolder<Block, Block> NITRO_KEG = register("nitro_keg", () -> new NitroKegBlock(Properties.ofLegacyCopy(Blocks.BARREL).strength(0.5F)));
   public static final DeferredHolder<Block, Block> CRYONITER = register(
      "cryoniter", () -> new CryoniterBlock(Properties.ofLegacyCopy(Blocks.STONE).requiresCorrectToolForDrops().strength(2.0F))
   );
   public static final DeferredHolder<Block, Block> THERMOLITH = register(
      "thermolith", () -> new ThermolithBlock(Properties.ofLegacyCopy(Blocks.STONE).requiresCorrectToolForDrops().strength(2.0F))
   );
   public static final DeferredHolder<Block, Block> BASIC_TURRET = register(
      "basic_turret", () -> new BasicTurretBlock(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().strength(3.0F).noOcclusion())
   );
   public static final DeferredHolder<Block, Block> SHOTGUN_TURRET = register(
      "shotgun_turret", () -> new ShotgunTurretBlock(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().strength(3.0F).noOcclusion())
   );
   public static final DeferredHolder<Block, Block> CHARGED_AMETHYST_RELAY = register(
      "charged_amethyst_relay", () -> new ChargedAmethystRelayBlock(Properties.ofLegacyCopy(Blocks.REPEATER).strength(0.0F).noOcclusion())
   );
   public static final DeferredHolder<Block, Block> AUTO_TURRET = register(
      "auto_turret", () -> new AutoTurretBlock(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().strength(3.0F).noOcclusion())
   );
   public static final DeferredHolder<Block, Block> SNIPER_TURRET = register(
      "sniper_turret", () -> new SniperTurretBlock(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().strength(8.0F).noOcclusion())
   );
   public static final DeferredHolder<Block, Block> ENEMY_TURRET = register(
      "enemy_turret", () -> new EnemyTurretBlock(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().strength(8.0F).noOcclusion())
   );
   public static final DeferredHolder<Block, Block> POLAR_GENERATOR = register(
      "polar_generator",
      () -> new PolarGeneratorBlock(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.COW_BELL).strength(3.0F).noOcclusion())
   );
   public static final DeferredHolder<Block, Block> LIGHTNING_ROD_CONNECTOR = register(
      "lightning_rod_connector", () -> new LightningRodConnectorBlock(Properties.ofLegacyCopy(Blocks.LIGHTNING_ROD).requiresCorrectToolForDrops().strength(2.0F).noOcclusion())
   );
   public static final DeferredHolder<Block, Block> LIGHTNING_BATTERY = register(
      "lightning_battery",
      () -> new LightningBattery(
            Properties.ofLegacyCopy(Blocks.STONE)
               .requiresCorrectToolForDrops()
               .instrument(NoteBlockInstrument.COW_BELL)
               .strength(3.0F)
               .noOcclusion()
               .lightLevel(state -> state.getValue(LightningBattery.CHARGED) ? 15 : 0)
         )
   );
   public static final DeferredHolder<Block, Block> MACERATOR = register(
      "macerator", () -> new MaceratorBlock(Properties.ofLegacyCopy(Blocks.STONE).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.HAT).strength(3.0F).noOcclusion())
   );
   public static final DeferredHolder<Block, Block> POWERED_MACERATOR = register(
      "powered_macerator",
      () -> new PoweredMaceratorBlock(Properties.ofLegacyCopy(Blocks.STONE).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.HAT).strength(3.0F).noOcclusion())
   );
   public static final DeferredHolder<Block, Block> MECHANICAL_PRESS = register(
      "mechanical_press",
      () -> new MechanicalPressBlock(Properties.ofLegacyCopy(Blocks.STONE).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BANJO).strength(3.0F).noOcclusion())
   );
   public static final DeferredHolder<Block, Block> POWERED_MECHANICAL_PRESS = register(
      "powered_mechanical_press",
      () -> new PoweredMechanicalPressBlock(Properties.ofLegacyCopy(Blocks.STONE).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BANJO).strength(3.0F).noOcclusion())
   );
   public static final DeferredHolder<Block, Block> GUN_BENCH = register(
      "gun_bench", () -> new GunBenchBlock(Properties.ofLegacyCopy(Blocks.CRAFTING_TABLE).instrument(NoteBlockInstrument.GUITAR).requiresCorrectToolForDrops().strength(2.5F).noOcclusion())
   );
   public static final DeferredHolder<Block, Block> SHOCK_COIL = register(
      "shock_coil",
      () -> new ShockCoilBlock(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.IRON_XYLOPHONE).strength(4.0F).noOcclusion())
   );
   public static final DeferredHolder<Block, Block> MINE_UNIT = register(
      "mine_unit", () -> new MineUnitBlock(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().strength(2.0F).noOcclusion())
   );
   public static final DeferredHolder<Block, Block> DIAMOND_STEEL_GRATE = register(
      "diamond_steel_grate",
      () -> new GrateBlock(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE).requiresCorrectToolForDrops().strength(5.0F).noOcclusion())
   );
   public static final DeferredHolder<Block, Block> CHISELED_DIAMOND_STEEL_BLOCK = register(
      "chiseled_diamond_steel_block",
      () -> new Block(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE).requiresCorrectToolForDrops().strength(5.0F))
   );
   public static final DeferredHolder<Block, Block> CUT_DIAMOND_STEEL = register(
      "cut_diamond_steel", () -> new Block(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE).requiresCorrectToolForDrops().strength(4.5F))
   );
   public static final DeferredHolder<Block, Block> CUT_DIAMOND_STEEL_SLAB = register(
      "cut_diamond_steel_slab",
      () -> new SlabBlock(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE).requiresCorrectToolForDrops().strength(4.5F))
   );
   public static final DeferredHolder<Block, Block> CUT_DIAMOND_STEEL_STAIRS = register(
      "cut_diamond_steel_stairs",
      () -> new StairBlock(((Block)CUT_DIAMOND_STEEL.get()).defaultBlockState(), Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE).requiresCorrectToolForDrops().strength(4.5F))
   );
   public static final DeferredHolder<Block, Block> DIAMOND_STEEL_BARS = register(
      "diamond_steel_bars",
      () -> new IronBarsBlock(Properties.ofLegacyCopy(Blocks.IRON_BARS).instrument(NoteBlockInstrument.IRON_XYLOPHONE).requiresCorrectToolForDrops().strength(5.0F).noOcclusion())
   );
   public static final DeferredHolder<Block, Block> DIAMOND_STEEL_PANEL = register(
      "diamond_steel_panel", () -> new Block(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE).requiresCorrectToolForDrops().strength(5.0F))
   );
   public static final DeferredHolder<Block, Block> DIAMOND_STEEL_PILLAR = register(
      "diamond_steel_pillar",
      () -> new PillarBlock(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE).requiresCorrectToolForDrops().strength(5.0F))
   );
   public static final DeferredHolder<Block, Block> DIAMOND_STEEL_LAMP = register(
      "diamond_steel_lamp",
      () -> new Block(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE).requiresCorrectToolForDrops().strength(5.0F).lightLevel(state -> 15))
   );
   public static final DeferredHolder<Block, Block> DIAMOND_STEEL_TILES = register(
      "diamond_steel_tiles", () -> new Block(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE).requiresCorrectToolForDrops().strength(5.0F))
   );
   public static final DeferredHolder<Block, Block> DIAMOND_STEEL_TILES_SLAB = register(
      "diamond_steel_tiles_slab",
      () -> new SlabBlock(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE).requiresCorrectToolForDrops().strength(5.0F))
   );
   public static final DeferredHolder<Block, Block> DIAMOND_STEEL_TILES_STAIRS = register(
      "diamond_steel_tiles_stairs",
      () -> new StairBlock(((Block)DIAMOND_STEEL_TILES.get()).defaultBlockState(), Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE).requiresCorrectToolForDrops().strength(5.0F))
   );
   public static final DeferredHolder<Block, Block> ANTHRALITE_BLOCK = register(
      "anthralite_block", () -> new Block(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE).requiresCorrectToolForDrops().strength(3.0F))
   );
   public static final DeferredHolder<Block, Block> ANTHRALITE_GRATE = register(
      "anthralite_grate",
      () -> new GrateBlock(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE).requiresCorrectToolForDrops().strength(3.0F).noOcclusion())
   );
   public static final DeferredHolder<Block, Block> ANTHRALITE_TILES = register(
      "anthralite_tiles", () -> new Block(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE).requiresCorrectToolForDrops().strength(3.0F))
   );
   public static final DeferredHolder<Block, Block> ANTHRALITE_PLATES = register(
      "anthralite_plates", () -> new Block(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE).requiresCorrectToolForDrops().strength(3.0F))
   );
   public static final DeferredHolder<Block, Block> CHISELED_ANTHRALITE_BLOCK = register(
      "chiseled_anthralite_block",
      () -> new RedstoneLampBlock(
            Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE).requiresCorrectToolForDrops().strength(3.0F).lightLevel(state -> 0)
         )
   );
   public static final DeferredHolder<Block, Block> CUT_ANTHRALITE = register(
      "cut_anthralite", () -> new Block(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE).requiresCorrectToolForDrops().strength(2.5F))
   );
   public static final DeferredHolder<Block, Block> CUT_ANTHRALITE_SLAB = register(
      "cut_anthralite_slab", () -> new SlabBlock(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE).requiresCorrectToolForDrops().strength(2.5F))
   );
   public static final DeferredHolder<Block, Block> CUT_ANTHRALITE_STAIRS = register(
      "cut_anthralite_stairs",
      () -> new StairBlock(((Block)CUT_ANTHRALITE.get()).defaultBlockState(), Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE).requiresCorrectToolForDrops().strength(2.5F))
   );
   public static final DeferredHolder<Block, Block> ANTHRALITE_TILES_SLAB = register(
      "anthralite_tiles_slab",
      () -> new SlabBlock(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE).requiresCorrectToolForDrops().strength(3.0F))
   );
   public static final DeferredHolder<Block, Block> ANTHRALITE_TILES_STAIRS = register(
      "anthralite_tiles_stairs",
      () -> new StairBlock(((Block)ANTHRALITE_TILES.get()).defaultBlockState(), Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE).requiresCorrectToolForDrops().strength(3.0F))
   );
   public static final DeferredHolder<Block, Block> ANTHRALITE_PILLAR = register(
      "anthralite_pillar", () -> new PillarBlock(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE).requiresCorrectToolForDrops().strength(3.0F))
   );
   public static final DeferredHolder<Block, Block> ANTHRALITE_LAMP = register(
      "anthralite_lamp",
      () -> new Block(Properties.ofLegacyCopy(Blocks.REDSTONE_LAMP).instrument(NoteBlockInstrument.IRON_XYLOPHONE).requiresCorrectToolForDrops().strength(3.0F).lightLevel(state -> 15))
   );
   public static final DeferredHolder<Block, Block> TREATED_IRON_BLOCK = register(
      "treated_iron_block", () -> new Block(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE).requiresCorrectToolForDrops().strength(3.0F))
   );
   public static final DeferredHolder<Block, Block> CHISELED_TREATED_IRON_BLOCK = register(
      "chiseled_treated_iron_block",
      () -> new Block(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE).requiresCorrectToolForDrops().strength(3.0F))
   );
   public static final DeferredHolder<Block, Block> CUT_TREATED_IRON = register(
      "cut_treated_iron", () -> new Block(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE).requiresCorrectToolForDrops().strength(2.5F))
   );
   public static final DeferredHolder<Block, Block> TREATED_IRON_LAMP = register(
      "treated_iron_lamp",
      () -> new Block(Properties.ofLegacyCopy(Blocks.REDSTONE_LAMP).instrument(NoteBlockInstrument.IRON_XYLOPHONE).requiresCorrectToolForDrops().strength(3.0F).lightLevel(state -> 15))
   );
   public static final DeferredHolder<Block, Block> CUT_TREATED_IRON_SLAB = register(
      "cut_treated_iron_slab",
      () -> new SlabBlock(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE).requiresCorrectToolForDrops().strength(2.5F))
   );
   public static final DeferredHolder<Block, Block> CUT_TREATED_IRON_STAIRS = register(
      "cut_treated_iron_stairs",
      () -> new StairBlock(((Block)CUT_TREATED_IRON.get()).defaultBlockState(), Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE).requiresCorrectToolForDrops().strength(2.5F))
   );
   public static final DeferredHolder<Block, Block> TREATED_IRON_PLATES = register(
      "treated_iron_plates", () -> new Block(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE).requiresCorrectToolForDrops().strength(3.0F))
   );
   public static final DeferredHolder<Block, Block> TREATED_IRON_BARS = register(
      "treated_iron_bars",
      () -> new IronBarsBlock(Properties.ofLegacyCopy(Blocks.IRON_BARS).instrument(NoteBlockInstrument.IRON_XYLOPHONE).requiresCorrectToolForDrops().strength(3.0F).noOcclusion())
   );
   public static final DeferredHolder<Block, Block> TREATED_BRASS_BLOCK = register(
      "treated_brass_block", () -> new Block(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE).requiresCorrectToolForDrops().strength(3.0F))
   );
   public static final DeferredHolder<Block, Block> VEHEMENT_COAL_BLOCK = registerBurnable(
      "vehement_coal_block", () -> new Block(Properties.ofLegacyCopy(Blocks.COAL_BLOCK).instrument(NoteBlockInstrument.DIDGERIDOO).requiresCorrectToolForDrops().strength(3.0F)), 43200
   );
   public static final DeferredHolder<Block, Block> PLASMA_BLOCK = registerBurnable(
      "plasma_block", () -> new Block(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().lightLevel(state -> 15).strength(3.0F)), 16200
   );
   public static final DeferredHolder<Block, Block> DIAMOND_STEEL_BLOCK = register(
      "diamond_steel_block", () -> new Block(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().strength(3.0F))
   );
   public static final DeferredHolder<Block, Block> RAW_ANTHRALITE_BLOCK = register(
      "raw_anthralite_block", () -> new Block(Properties.ofLegacyCopy(Blocks.STONE).requiresCorrectToolForDrops().strength(3.0F))
   );
   public static final DeferredHolder<Block, Block> ANTHRALITE_ORE = register(
      "anthralite_ore",
      () -> new DropExperienceBlock(net.minecraft.util.valueproviders.ConstantInt.of(0), Properties.ofLegacyCopy(Blocks.IRON_ORE).mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F))
   );
   public static final DeferredHolder<Block, Block> DEEPSLATE_ANTHRALITE_ORE = register(
      "deepslate_anthralite_ore",
      () -> new DropExperienceBlock(net.minecraft.util.valueproviders.ConstantInt.of(0), Properties.ofLegacyCopy(Blocks.DEEPSLATE_IRON_ORE).mapColor(MapColor.DEEPSLATE).strength(4.5F, 3.0F).sound(SoundType.DEEPSLATE))
   );
   public static final DeferredHolder<Block, Block> SULFUR_BLOCK = registerBurnable(
      "sulfur_block", () -> new Block(Properties.ofLegacyCopy(Blocks.STONE).requiresCorrectToolForDrops().strength(3.0F)), 7200
   );
   public static final DeferredHolder<Block, Block> ANCIENT_BRASS_BLOCK = register(
      "ancient_brass_block", () -> new Block(Properties.ofLegacyCopy(Blocks.RAW_COPPER_BLOCK).requiresCorrectToolForDrops().strength(2.0F))
   );
   public static final DeferredHolder<Block, Block> SKIBIDI = register(
      "skibidi", () -> new BasicDirectionalBlock(Properties.ofLegacyCopy(Blocks.RAW_COPPER_BLOCK).requiresCorrectToolForDrops().strength(2.0F).noOcclusion())
   );
   public static final DeferredHolder<Block, Block> FELIX_MEMORIAL = register(
      "felix_memorial", () -> new MemorialBlock(Properties.ofLegacyCopy(Blocks.ANDESITE).requiresCorrectToolForDrops().strength(4.0F).noOcclusion())
   );
   public static final DeferredHolder<Block, Block> TURRET_TARGETING_BLOCK = register(
      "turret_targeting_module", () -> new TurretTargetingBlock(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().strength(0.5F).noOcclusion())
   );
   public static final DeferredHolder<Block, Block> PLAYER_TURRET_TARGETING_BLOCK = register(
      "player_turret_targeting_module", () -> new PlayerTurretTargetingBlock(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().strength(0.5F).noOcclusion())
   );
   public static final DeferredHolder<Block, Block> HOSTILE_TURRET_TARGETING_BLOCK = register(
      "hostile_turret_targeting_module", () -> new HostileTurretTargetingBlock(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().strength(0.5F).noOcclusion())
   );
   public static final DeferredHolder<Block, Block> FIRE_RATE_TURRET_MODULE = register(
      "fire_rate_turret_module", () -> new FireRateModuleBlock(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().strength(0.5F).noOcclusion())
   );
   public static final DeferredHolder<Block, Block> DAMAGE_TURRET_MODULE = register(
      "damage_turret_module", () -> new DamageModuleBlock(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().strength(0.5F).noOcclusion())
   );
   public static final DeferredHolder<Block, Block> RANGE_TURRET_MODULE = register(
      "range_turret_module", () -> new RangeModuleBlock(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().strength(0.5F).noOcclusion())
   );
   public static final DeferredHolder<Block, Block> SHELL_CATCHER_TURRET_MODULE = register(
      "shell_catcher_turret_module", () -> new ShellCatcherModuleBlock(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().strength(0.5F).noOcclusion())
   );
   public static final DeferredHolder<Block, Block> AMMO_TURRET_MODULE = register(
      "ammo_turret_module", () -> new AmmoModuleBlock(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().strength(0.5F).randomTicks().noOcclusion())
   );
   public static final DeferredHolder<Block, Block> RICH_PHOSPHORITE = register(
      "rich_phosphorite", () -> new DropExperienceBlock(UniformInt.of(0, 1), Properties.ofLegacyCopy(Blocks.STONE).requiresCorrectToolForDrops().strength(2.0F))
   );
   public static final DeferredHolder<Block, Block> SULFUR_ORE = register(
      "sulfur_ore",
      () -> new DropExperienceBlock(UniformInt.of(0, 2), Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F))
   );
   public static final DeferredHolder<Block, Block> DEEPSLATE_SULFUR_ORE = register(
      "deepslate_sulfur_ore",
      () -> new DropExperienceBlock(UniformInt.of(0, 2), Properties.ofLegacyCopy(Blocks.COAL_ORE).mapColor(MapColor.DEEPSLATE).strength(4.5F, 3.0F).sound(SoundType.DEEPSLATE))
   );
   public static final DeferredHolder<Block, Block> NETHER_SULFUR_ORE = register(
      "nether_sulfur_ore",
      () -> new DropExperienceBlock(UniformInt.of(0, 1), Properties.of()
               .mapColor(MapColor.NETHER)
               .instrument(NoteBlockInstrument.BASEDRUM)
               .requiresCorrectToolForDrops()
               .strength(3.0F, 3.0F)
               .sound(SoundType.NETHER_GOLD_ORE))
   );
   public static final DeferredHolder<Block, Block> VEHEMENT_COAL_ORE = register(
      "vehement_coal_ore",
      () -> new DropExperienceBlock(UniformInt.of(0, 1), Properties.of()
               .mapColor(MapColor.NETHER)
               .instrument(NoteBlockInstrument.BASEDRUM)
               .requiresCorrectToolForDrops()
               .strength(3.0F, 3.0F)
               .sound(SoundType.NETHER_GOLD_ORE))
   );
   public static final DeferredHolder<Block, Block> NITER_LAYER = register("niter", () -> new NiterLayerBlock(Properties.ofLegacyCopy(Blocks.SAND)));
   public static final DeferredHolder<Block, Block> SULFUR_LAYER = register("sulfur", () -> new SulfurLayerBlock(Properties.ofLegacyCopy(Blocks.SAND)));
   public static final DeferredHolder<Block, Block> BAT_GUANO_LAYER = register("bat_guano_layer", () -> new BatGuanoLayerBlock(Properties.ofLegacyCopy(Blocks.SAND)));
   public static final DeferredHolder<Block, Block> PHOSPHOR_LAYER = register("phosphor_layer", () -> new PhosphorLayerBlock(Properties.ofLegacyCopy(Blocks.SAND)));
   public static final DeferredHolder<Block, Block> NITER_BLOCK = register("niter_block", () -> new Block(Properties.ofLegacyCopy(Blocks.SAND)));
   public static final DeferredHolder<Block, Block> GUANO_CANDLE = register(
      "guano_candle", () -> new GuanoCandleBlock(Properties.ofLegacyCopy(Blocks.CANDLE).lightLevel(state -> state.getValue(CandleBlock.LIT) ? 10 : 0))
   );
   public static final DeferredHolder<Block, Block> PENETRATOR = register(
      "penetrator", () -> new PenetratorBlock(Properties.ofLegacyCopy(Blocks.STONE).strength(1.0F), 10)
   );
   public static final DeferredHolder<Block, Block> PLASMA_LANTERN = register(
      "plasma_lantern", () -> new LanternBlock(Properties.ofLegacyCopy(Blocks.LANTERN).requiresCorrectToolForDrops().noOcclusion().strength(3.0F).lightLevel(state -> 15))
   );
   public static final DeferredHolder<Block, Block> GEOTHERMAL_VENT = register(
      "geothermal_vent", () -> new GeothermalVentBlock(Properties.ofLegacyCopy(Blocks.STONE).requiresCorrectToolForDrops().strength(3.0F).noOcclusion().randomTicks())
   );
   public static final DeferredHolder<Block, Block> SULFUR_VENT = register(
      "sulfur_vent", () -> new SulfurVentBlock(Properties.ofLegacyCopy(Blocks.OBSIDIAN).requiresCorrectToolForDrops().strength(12.0F).noOcclusion().randomTicks())
   );
   public static final DeferredHolder<Block, Block> VENT_COLLECTOR = register(
      "vent_collector", () -> new VentCollectorBlock(Properties.ofLegacyCopy(Blocks.STONE).noOcclusion().requiresCorrectToolForDrops().strength(0.5F).noOcclusion().randomTicks())
   );
   public static final DeferredHolder<Block, Block> NITER_GLASS = register("niter_glass", () -> new NiterGlassBlock(Properties.ofLegacyCopy(Blocks.GLASS)));
   public static final DeferredHolder<Block, Block> WHITE_NITER_GLASS = register("white_niter_glass", () -> new NiterGlassBlock(Properties.ofLegacyCopy(Blocks.GLASS)));
   public static final DeferredHolder<Block, Block> ORANGE_NITER_GLASS = register(
      "orange_niter_glass", () -> new NiterGlassBlock(Properties.ofLegacyCopy(Blocks.GLASS))
   );
   public static final DeferredHolder<Block, Block> MAGENTA_NITER_GLASS = register(
      "magenta_niter_glass", () -> new NiterGlassBlock(Properties.ofLegacyCopy(Blocks.GLASS))
   );
   public static final DeferredHolder<Block, Block> LIGHT_BLUE_NITER_GLASS = register(
      "light_blue_niter_glass", () -> new NiterGlassBlock(Properties.ofLegacyCopy(Blocks.GLASS))
   );
   public static final DeferredHolder<Block, Block> YELLOW_NITER_GLASS = register(
      "yellow_niter_glass", () -> new NiterGlassBlock(Properties.ofLegacyCopy(Blocks.GLASS))
   );
   public static final DeferredHolder<Block, Block> LIME_NITER_GLASS = register("lime_niter_glass", () -> new NiterGlassBlock(Properties.ofLegacyCopy(Blocks.GLASS)));
   public static final DeferredHolder<Block, Block> PINK_NITER_GLASS = register("pink_niter_glass", () -> new NiterGlassBlock(Properties.ofLegacyCopy(Blocks.GLASS)));
   public static final DeferredHolder<Block, Block> GRAY_NITER_GLASS = register("gray_niter_glass", () -> new NiterGlassBlock(Properties.ofLegacyCopy(Blocks.GLASS)));
   public static final DeferredHolder<Block, Block> LIGHT_GRAY_NITER_GLASS = register(
      "light_gray_niter_glass", () -> new NiterGlassBlock(Properties.ofLegacyCopy(Blocks.GLASS))
   );
   public static final DeferredHolder<Block, Block> CYAN_NITER_GLASS = register("cyan_niter_glass", () -> new NiterGlassBlock(Properties.ofLegacyCopy(Blocks.GLASS)));
   public static final DeferredHolder<Block, Block> PURPLE_NITER_GLASS = register(
      "purple_niter_glass", () -> new NiterGlassBlock(Properties.ofLegacyCopy(Blocks.GLASS))
   );
   public static final DeferredHolder<Block, Block> BLUE_NITER_GLASS = register("blue_niter_glass", () -> new NiterGlassBlock(Properties.ofLegacyCopy(Blocks.GLASS)));
   public static final DeferredHolder<Block, Block> BROWN_NITER_GLASS = register("brown_niter_glass", () -> new NiterGlassBlock(Properties.ofLegacyCopy(Blocks.GLASS)));
   public static final DeferredHolder<Block, Block> GREEN_NITER_GLASS = register("green_niter_glass", () -> new NiterGlassBlock(Properties.ofLegacyCopy(Blocks.GLASS)));
   public static final DeferredHolder<Block, Block> RED_NITER_GLASS = register("red_niter_glass", () -> new NiterGlassBlock(Properties.ofLegacyCopy(Blocks.GLASS)));
   public static final DeferredHolder<Block, Block> BLACK_NITER_GLASS = register("black_niter_glass", () -> new NiterGlassBlock(Properties.ofLegacyCopy(Blocks.GLASS)));
   public static final DeferredHolder<Block, Block> RAW_PHOSPHOR_BLOCK = register(
      "raw_phosphor_block", () -> new Block(Properties.ofLegacyCopy(Blocks.STONE).requiresCorrectToolForDrops().strength(2.0F))
   );
   public static final DeferredHolder<Block, Block> PHOSPHORITE = register(
      "phosphorite", () -> new Block(Properties.ofLegacyCopy(Blocks.STONE).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASS).strength(1.5F))
   );
   public static final DeferredHolder<Block, Block> SMOOTH_PHOSPHORITE = register(
      "smooth_phosphorite", () -> new Block(Properties.ofLegacyCopy(Blocks.STONE).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASS).strength(2.0F))
   );
   public static final DeferredHolder<Block, Block> POLISHED_PHOSPHORITE = register(
      "polished_phosphorite", () -> new Block(Properties.ofLegacyCopy(Blocks.STONE).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASS).strength(2.0F))
   );
   public static final DeferredHolder<Block, Block> PHOSPHORITE_BRICKS = register(
      "phosphorite_bricks", () -> new Block(Properties.ofLegacyCopy(Blocks.STONE).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASS).strength(2.0F))
   );
   public static final DeferredHolder<Block, Block> CRACKED_PHOSPHORITE_BRICKS = register(
      "cracked_phosphorite_bricks", () -> new Block(Properties.ofLegacyCopy(Blocks.STONE).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASS).strength(2.0F))
   );
   public static final DeferredHolder<Block, Block> PHOSPHORITE_BRICK_SLAB = register(
      "phosphorite_brick_slab", () -> new SlabBlock(Properties.ofLegacyCopy(Blocks.STONE).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASS).strength(2.0F))
   );
   public static final DeferredHolder<Block, Block> PHOSPHORITE_BRICK_STAIRS = register(
      "phosphorite_brick_stairs",
      () -> new StairBlock(((Block)PHOSPHORITE_BRICKS.get()).defaultBlockState(), Properties.ofLegacyCopy(Blocks.STONE).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASS).strength(2.0F))
   );
   public static final DeferredHolder<Block, Block> PHOSPHORITE_BRICK_WALL = register(
      "phosphorite_brick_wall", () -> new WallBlock(Properties.ofLegacyCopy(Blocks.STONE).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASS).strength(2.0F))
   );
   public static final DeferredHolder<Block, Block> ASGHARIAN_PILLAR = register(
      "asgharian_pillar", () -> new AsgharianPillarBlock(Properties.ofLegacyCopy(Blocks.STONE).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.PLING).strength(4.0F))
   );
   public static final DeferredHolder<Block, Block> ASGHARIAN_BRICKS = register(
      "asgharian_bricks", () -> new Block(Properties.ofLegacyCopy(Blocks.STONE).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.PLING).strength(4.0F))
   );
   public static final DeferredHolder<Block, Block> REINFORCED_ASGHARIAN_TILES = register(
      "reinforced_asgharian_tiles", () -> new Block(Properties.ofLegacyCopy(Blocks.STONE).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.PLING).strength(6.0F))
   );
   public static final DeferredHolder<Block, Block> ASGHARIAN_TILES = register(
      "asgharian_tiles", () -> new Block(Properties.ofLegacyCopy(Blocks.STONE).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.PLING).strength(4.0F))
   );
   public static final DeferredHolder<Block, Block> CRACKED_ASGHARIAN_TILES = register(
      "cracked_asgharian_tiles", () -> new Block(Properties.ofLegacyCopy(Blocks.STONE).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.PLING).strength(4.0F))
   );
   public static final DeferredHolder<Block, Block> MOSSY_ASGHARIAN_BRICKS = register(
      "mossy_asgharian_bricks", () -> new Block(Properties.ofLegacyCopy(Blocks.STONE).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.PLING).strength(4.0F))
   );
   public static final DeferredHolder<Block, Block> MOSSY_ASGHARIAN_TILES = register(
      "mossy_asgharian_tiles", () -> new Block(Properties.ofLegacyCopy(Blocks.STONE).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.PLING).strength(4.0F))
   );
   public static final DeferredHolder<Block, Block> CHISELED_ASGHARIAN_BRICKS = register(
      "chiseled_asgharian_bricks", () -> new Block(Properties.ofLegacyCopy(Blocks.STONE).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.PLING).strength(4.0F))
   );
   public static final DeferredHolder<Block, Block> POLISHED_ASGHARIAN_PANEL = register(
      "polished_asgharian_panel", () -> new Block(Properties.ofLegacyCopy(Blocks.STONE).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.PLING).strength(4.0F))
   );
   public static final DeferredHolder<Block, Block> CRACKED_ASGHARIAN_BRICKS = register(
      "cracked_asgharian_bricks", () -> new Block(Properties.ofLegacyCopy(Blocks.STONE).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.PLING).strength(4.0F))
   );
   public static final DeferredHolder<Block, Block> ASGHARIAN_BRICK_SLAB = register(
      "asgharian_brick_slab", () -> new SlabBlock(Properties.ofLegacyCopy(Blocks.STONE).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.PLING).strength(4.0F))
   );
   public static final DeferredHolder<Block, Block> ASGHARIAN_BRICK_STAIRS = register(
      "asgharian_brick_stairs",
      () -> new StairBlock(((Block)ASGHARIAN_BRICKS.get()).defaultBlockState(), Properties.ofLegacyCopy(Blocks.STONE).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.PLING).strength(4.0F))
   );
   public static final DeferredHolder<Block, Block> ASGHARIAN_BRICK_WALL = register(
      "asgharian_brick_wall", () -> new WallBlock(Properties.ofLegacyCopy(Blocks.STONE).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.PLING).strength(4.0F))
   );
   public static final DeferredHolder<Block, Block> TREATED_BRASS_PLATES = register(
      "treated_brass_plates", () -> new Block(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.FLUTE).strength(3.0F))
   );
   public static final DeferredHolder<Block, Block> CUT_TREATED_BRASS = register(
      "cut_treated_brass", () -> new Block(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.FLUTE).strength(2.5F))
   );
   public static final DeferredHolder<Block, Block> CUT_TREATED_BRASS_SLAB = register(
      "cut_treated_brass_slab", () -> new SlabBlock(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.FLUTE).strength(2.5F))
   );
   public static final DeferredHolder<Block, Block> CUT_TREATED_BRASS_STAIRS = register(
      "cut_treated_brass_stairs",
      () -> new StairBlock(((Block)CUT_TREATED_BRASS.get()).defaultBlockState(), Properties.ofLegacyCopy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.FLUTE).strength(2.5F))
   );
   public static final DeferredHolder<Block, Block> TREATED_IRON_GRATE = register(
      "treated_iron_grate",
      () -> new GrateBlock(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.IRON_XYLOPHONE).strength(3.0F).noOcclusion())
   );
   public static final DeferredHolder<Block, Block> TREATED_IRON_GRATE_PANE = register(
      "treated_iron_grate_pane",
      () -> new IronBarsBlock(Properties.ofLegacyCopy(Blocks.IRON_BARS).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.IRON_XYLOPHONE).strength(3.0F).noOcclusion())
   );
   public static final DeferredHolder<Block, Block> TREATED_BRASS_GRATE_PANE = register(
      "treated_brass_grate_pane",
      () -> new IronBarsBlock(Properties.ofLegacyCopy(Blocks.IRON_BARS).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.FLUTE).strength(3.0F).noOcclusion())
   );
   public static final DeferredHolder<Block, Block> DIAMOND_STEEL_GRATE_PANE = register(
      "diamond_steel_grate_pane",
      () -> new IronBarsBlock(Properties.ofLegacyCopy(Blocks.IRON_BARS).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.IRON_XYLOPHONE).strength(5.0F).noOcclusion())
   );
   public static final DeferredHolder<Block, Block> ANTHRALITE_GRATE_PANE = register(
      "anthralite_grate_pane",
      () -> new IronBarsBlock(Properties.ofLegacyCopy(Blocks.IRON_BARS).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.IRON_XYLOPHONE).strength(3.0F).noOcclusion())
   );
   public static final DeferredHolder<Block, Block> TREATED_BRASS_GRATE = register(
      "treated_brass_grate",
      () -> new GrateBlock(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.FLUTE).strength(3.0F).noOcclusion())
   );
   public static final DeferredHolder<Block, Block> CHISELED_TREATED_BRASS_BLOCK = register(
      "chiseled_treated_brass_block", () -> new Block(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.FLUTE).strength(3.0F))
   );
   public static final DeferredHolder<Block, Block> TREATED_BRASS_TILES = register(
      "treated_brass_tiles", () -> new Block(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.FLUTE).strength(3.0F))
   );
   public static final DeferredHolder<Block, Block> TREATED_BRASS_TILES_SLAB = register(
      "treated_brass_tiles_slab", () -> new SlabBlock(Properties.ofLegacyCopy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.FLUTE).strength(3.0F))
   );
   public static final DeferredHolder<Block, Block> TREATED_BRASS_TILES_STAIRS = register(
      "treated_brass_tiles_stairs",
      () -> new StairBlock(((Block)TREATED_BRASS_TILES.get()).defaultBlockState(), Properties.ofLegacyCopy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.FLUTE).strength(3.0F))
   );
   public static final DeferredHolder<Block, Block> TREATED_BRASS_LAMP = register(
      "treated_brass_lamp",
      () -> new RedstoneLampBlock(
            Properties.ofLegacyCopy(Blocks.REDSTONE_LAMP)
               .requiresCorrectToolForDrops()
               .instrument(NoteBlockInstrument.FLUTE)
               .strength(3.0F)
               .lightLevel(state -> state.getValue(RedstoneLampBlock.LIT) ? 15 : 0)
         )
   );
   public static final DeferredHolder<Block, Block> SCORCHED_BLOCK = register(
      "scorched_block", () -> new Block(Properties.ofLegacyCopy(Blocks.STONE).requiresCorrectToolForDrops().strength(5.5F).lightLevel(state -> 10))
   );
   public static final DeferredHolder<Block, Block> SANDBAG = register(
      "sandbag", () -> new SandbagBlock(Properties.ofLegacyCopy(Blocks.SANDSTONE).strength(0.5F).noOcclusion())
   );
   public static final DeferredHolder<Block, Block> SUPPLY_CRATE = register("supply_crate", () -> new Block(Properties.ofLegacyCopy(Blocks.OAK_PLANKS).strength(1.0F)));

   public ModBlocks() {
      super();
   }

   private static <T extends Block> DeferredHolder<Block, T> register(String id, Supplier<T> blockSupplier) {
      return register(id, blockSupplier, block1 -> new BlockItem(block1, new net.minecraft.world.item.Item.Properties()));
   }

   private static <T extends Block> DeferredHolder<Block, T> register(String id, Supplier<T> blockSupplier, @Nullable Function<T, BlockItem> supplier) {
      DeferredHolder<Block, T> registryObject = REGISTER.register(id, blockSupplier);
      if (supplier != null) {
         ModItems.REGISTER.register(id, () -> supplier.apply(registryObject.get()));
      }

      return registryObject;
   }

   private static <T extends Block> DeferredHolder<Block, T> registerBurnable(String id, Supplier<T> blockSupplier, int burnTime) {
      return register(id, blockSupplier, block -> new BlockItem(block, new net.minecraft.world.item.Item.Properties()) {
            public int getBurnTime(ItemStack itemStack, @Nullable RecipeType<?> recipeType) {
               return burnTime;
            }
         });
   }
}
