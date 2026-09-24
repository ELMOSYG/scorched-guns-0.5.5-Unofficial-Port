package top.ribs.scguns.init;


import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityType.Builder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import top.ribs.scguns.blockentity.AdvancedComposterBlockEntity;
import top.ribs.scguns.blockentity.AmmoBoxBlockEntity;
import top.ribs.scguns.blockentity.AmmoModuleBlockEntity;
import top.ribs.scguns.blockentity.AutoTurretBlockEntity;
import top.ribs.scguns.blockentity.BasicTurretBlockEntity;
import top.ribs.scguns.blockentity.ChargedAmethystRelayBlockEntity;
import top.ribs.scguns.blockentity.CryoniterBlockEntity;
import top.ribs.scguns.blockentity.EnemyTurretBlockEntity;
import top.ribs.scguns.blockentity.GunBenchBlockEntity;
import top.ribs.scguns.blockentity.GunShelfBlockEntity;
import top.ribs.scguns.blockentity.LightningBatteryBlockEntity;
import top.ribs.scguns.blockentity.MaceratorBlockEntity;
import top.ribs.scguns.blockentity.MechanicalPressBlockEntity;
import top.ribs.scguns.blockentity.MineUnitBlockEntity;
import top.ribs.scguns.blockentity.MobTrapBlockEntity;
import top.ribs.scguns.blockentity.NitroKegBlockEntity;
import top.ribs.scguns.blockentity.PenetratorBlockEntity;
import top.ribs.scguns.blockentity.PolarGeneratorBlockEntity;
import top.ribs.scguns.blockentity.PowderKegBlockEntity;
import top.ribs.scguns.blockentity.PoweredMaceratorBlockEntity;
import top.ribs.scguns.blockentity.PoweredMechanicalPressBlockEntity;
import top.ribs.scguns.blockentity.ShellCatcherModuleBlockEntity;
import top.ribs.scguns.blockentity.ShockCoilBlockEntity;
import top.ribs.scguns.blockentity.ShotgunTurretBlockEntity;
import top.ribs.scguns.blockentity.SniperTurretBlockEntity;
import top.ribs.scguns.blockentity.ThermolithBlockEntity;
import top.ribs.scguns.blockentity.VentCollectorBlockEntity;

public class ModBlockEntities {
   public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, "scguns");
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GunShelfBlockEntity>> GUN_SHELF_BLOCK_ENTITY = BLOCK_ENTITIES.register(
      "gun_shelf", () -> Builder.of(GunShelfBlockEntity::new, new Block[]{(Block)ModBlocks.GUN_SHELF.get()}).build(null)
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MobTrapBlockEntity>> MOB_TRAP = BLOCK_ENTITIES.register(
      "mob_trap", () -> Builder.of(MobTrapBlockEntity::new, new Block[]{(Block)ModBlocks.MOB_TRAP.get()}).build(null)
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AdvancedComposterBlockEntity>> ADVANCED_COMPOSTER = BLOCK_ENTITIES.register(
      "advanced_composter", () -> Builder.of(AdvancedComposterBlockEntity::new, new Block[]{(Block)ModBlocks.ADVANCED_COMPOSTER.get()}).build(null)
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowderKegBlockEntity>> POWDER_KEG = BLOCK_ENTITIES.register(
      "powder_keg", () -> Builder.of(PowderKegBlockEntity::new, new Block[]{(Block)ModBlocks.POWDER_KEG.get()}).build(null)
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NitroKegBlockEntity>> NITRO_KEG = BLOCK_ENTITIES.register(
      "nitro_keg", () -> Builder.of(NitroKegBlockEntity::new, new Block[]{(Block)ModBlocks.NITRO_KEG.get()}).build(null)
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChargedAmethystRelayBlockEntity>> CHARGED_AMETHYST_RELAY = BLOCK_ENTITIES.register(
      "charged_amethyst_relay",
      () -> Builder.of(ChargedAmethystRelayBlockEntity::new, new Block[]{(Block)ModBlocks.CHARGED_AMETHYST_RELAY.get()}).build(null)
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MineUnitBlockEntity>> MINE_UNIT = BLOCK_ENTITIES.register(
      "mine_unit", () -> Builder.of(MineUnitBlockEntity::new, new Block[]{(Block)ModBlocks.MINE_UNIT.get()}).build(null)
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GunBenchBlockEntity>> GUN_BENCH = BLOCK_ENTITIES.register(
      "gun_bench", () -> Builder.of(GunBenchBlockEntity::new, new Block[]{(Block)ModBlocks.GUN_BENCH.get()}).build(null)
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CryoniterBlockEntity>> CRYONITER = BLOCK_ENTITIES.register(
      "cryoniter", () -> Builder.of(CryoniterBlockEntity::new, new Block[]{(Block)ModBlocks.CRYONITER.get()}).build(null)
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ThermolithBlockEntity>> THERMOLITH = BLOCK_ENTITIES.register(
      "thermolith", () -> Builder.of(ThermolithBlockEntity::new, new Block[]{(Block)ModBlocks.THERMOLITH.get()}).build(null)
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PolarGeneratorBlockEntity>> POLAR_GENERATOR = BLOCK_ENTITIES.register(
      "polar_generator", () -> Builder.of(PolarGeneratorBlockEntity::new, new Block[]{(Block)ModBlocks.POLAR_GENERATOR.get()}).build(null)
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PenetratorBlockEntity>> PENETRATOR = BLOCK_ENTITIES.register(
      "penetrator", () -> Builder.of(PenetratorBlockEntity::new, new Block[]{(Block)ModBlocks.PENETRATOR.get()}).build(null)
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MaceratorBlockEntity>> MACERATOR = BLOCK_ENTITIES.register(
      "macerator", () -> Builder.of(MaceratorBlockEntity::new, new Block[]{(Block)ModBlocks.MACERATOR.get()}).build(null)
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PoweredMaceratorBlockEntity>> POWERED_MACERATOR = BLOCK_ENTITIES.register(
      "powered_macerator", () -> Builder.of(PoweredMaceratorBlockEntity::new, new Block[]{(Block)ModBlocks.POWERED_MACERATOR.get()}).build(null)
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MechanicalPressBlockEntity>> MECHANICAL_PRESS = BLOCK_ENTITIES.register(
      "mechanical_press", () -> Builder.of(MechanicalPressBlockEntity::new, new Block[]{(Block)ModBlocks.MECHANICAL_PRESS.get()}).build(null)
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PoweredMechanicalPressBlockEntity>> POWERED_MECHANICAL_PRESS = BLOCK_ENTITIES.register(
      "powered_mechanical_press",
      () -> Builder.of(PoweredMechanicalPressBlockEntity::new, new Block[]{(Block)ModBlocks.POWERED_MECHANICAL_PRESS.get()}).build(null)
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LightningBatteryBlockEntity>> LIGHTNING_BATTERY = BLOCK_ENTITIES.register(
      "lightning_battery", () -> Builder.of(LightningBatteryBlockEntity::new, new Block[]{(Block)ModBlocks.LIGHTNING_BATTERY.get()}).build(null)
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShockCoilBlockEntity>> SHOCK_COIL = BLOCK_ENTITIES.register(
      "shock_coil", () -> Builder.of(ShockCoilBlockEntity::new, new Block[]{(Block)ModBlocks.SHOCK_COIL.get()}).build(null)
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BasicTurretBlockEntity>> BASIC_TURRET = BLOCK_ENTITIES.register(
      "basic_turret", () -> Builder.of(BasicTurretBlockEntity::new, new Block[]{(Block)ModBlocks.BASIC_TURRET.get()}).build(null)
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AutoTurretBlockEntity>> AUTO_TURRET = BLOCK_ENTITIES.register(
      "auto_turret", () -> Builder.of(AutoTurretBlockEntity::new, new Block[]{(Block)ModBlocks.AUTO_TURRET.get()}).build(null)
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SniperTurretBlockEntity>> SNIPER_TURRET = BLOCK_ENTITIES.register(
      "sniper_turret", () -> Builder.of(SniperTurretBlockEntity::new, new Block[]{(Block)ModBlocks.SNIPER_TURRET.get()}).build(null)
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnemyTurretBlockEntity>> ENEMY_TURRET = BLOCK_ENTITIES.register(
      "enemy_turret", () -> Builder.of(EnemyTurretBlockEntity::new, new Block[]{(Block)ModBlocks.ENEMY_TURRET.get()}).build(null)
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShotgunTurretBlockEntity>> SHOTGUN_TURRET = BLOCK_ENTITIES.register(
      "shotgun_turret", () -> Builder.of(ShotgunTurretBlockEntity::new, new Block[]{(Block)ModBlocks.SHOTGUN_TURRET.get()}).build(null)
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShellCatcherModuleBlockEntity>> SHELL_CATCHER_MODULE = BLOCK_ENTITIES.register(
      "shell_catcher_module",
      () -> Builder.of(ShellCatcherModuleBlockEntity::new, new Block[]{(Block)ModBlocks.SHELL_CATCHER_TURRET_MODULE.get()}).build(null)
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AmmoModuleBlockEntity>> AMMO_MODULE = BLOCK_ENTITIES.register(
      "ammo_module", () -> Builder.of(AmmoModuleBlockEntity::new, new Block[]{(Block)ModBlocks.AMMO_TURRET_MODULE.get()}).build(null)
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AmmoBoxBlockEntity>> AMMO_BOX = BLOCK_ENTITIES.register(
      "ammo_box", () -> Builder.of(AmmoBoxBlockEntity::new, new Block[]{(Block)ModBlocks.AMMO_BOX.get()}).build(null)
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VentCollectorBlockEntity>> VENT_COLLECTOR = BLOCK_ENTITIES.register(
      "vent_collector", () -> Builder.of(VentCollectorBlockEntity::new, new Block[]{(Block)ModBlocks.VENT_COLLECTOR.get()}).build(null)
   );

   public ModBlockEntities() {
      super();
   }

   public static void register(IEventBus eventBus) {
      BLOCK_ENTITIES.register(eventBus);
   }
}
