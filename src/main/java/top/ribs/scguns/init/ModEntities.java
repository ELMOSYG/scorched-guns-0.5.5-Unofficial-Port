package top.ribs.scguns.init;


import net.minecraft.core.registries.BuiltInRegistries;
import java.util.function.BiFunction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType.Builder;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import top.ribs.scguns.entity.block.PrimedNitroKeg;
import top.ribs.scguns.entity.block.PrimedPowderKeg;
import top.ribs.scguns.entity.monster.AdjudicatorEntity;
import top.ribs.scguns.entity.monster.BeaconProjectileEntity;
import top.ribs.scguns.entity.monster.BlundererEntity;
import top.ribs.scguns.entity.monster.CogKnightEntity;
import top.ribs.scguns.entity.monster.CogMinionEntity;
import top.ribs.scguns.entity.monster.DissidentEntity;
import top.ribs.scguns.entity.monster.FinforcerEntity;
import top.ribs.scguns.entity.monster.HiveEntity;
import top.ribs.scguns.entity.monster.HornlinEntity;
import top.ribs.scguns.entity.monster.MotherGhastEntity;
import top.ribs.scguns.entity.monster.PraetorEntity;
import top.ribs.scguns.entity.monster.ScampRocketEntity;
import top.ribs.scguns.entity.monster.ScampTankEntity;
import top.ribs.scguns.entity.monster.ScamplerEntity;
import top.ribs.scguns.entity.monster.SignalBeaconEntity;
import top.ribs.scguns.entity.monster.SkyCarrierEntity;
import top.ribs.scguns.entity.monster.SubjugatorEntity;
import top.ribs.scguns.entity.monster.SulfurheadEntity;
import top.ribs.scguns.entity.monster.SupplyScampEntity;
import top.ribs.scguns.entity.monster.SwarmEntity;
import top.ribs.scguns.entity.monster.TheMerchantEntity;
import top.ribs.scguns.entity.monster.TraumaUnitEntity;
import top.ribs.scguns.entity.monster.ViventrumEntity;
import top.ribs.scguns.entity.monster.ZombifiedHornlinEntity;
import top.ribs.scguns.entity.projectile.AdvancedRoundProjectileEntity;
import top.ribs.scguns.entity.projectile.BasicBulletProjectileEntity;
import top.ribs.scguns.entity.projectile.BearPackShellProjectileEntity;
import top.ribs.scguns.entity.projectile.BeowulfProjectileEntity;
import top.ribs.scguns.entity.projectile.BlazeRodProjectileEntity;
import top.ribs.scguns.entity.projectile.BouncyGrenadeRoundEntity;
import top.ribs.scguns.entity.projectile.BuckshotProjectileEntity;
import top.ribs.scguns.entity.projectile.EnemyProjectileEntity;
import top.ribs.scguns.entity.projectile.FireGrenadeRoundEntity;
import top.ribs.scguns.entity.projectile.FireRoundEntity;
import top.ribs.scguns.entity.projectile.FlechetteProjectileEntity;
import top.ribs.scguns.entity.projectile.FrogDartProjectileEntity;
import top.ribs.scguns.entity.projectile.GasGrenadeRoundEntity;
import top.ribs.scguns.entity.projectile.GibbsRoundProjectileEntity;
import top.ribs.scguns.entity.projectile.HardenedBulletProjectileEntity;
import top.ribs.scguns.entity.projectile.HeGrenadeRoundEntity;
import top.ribs.scguns.entity.projectile.HogRoundProjectileEntity;
import top.ribs.scguns.entity.projectile.KrahgRoundProjectileEntity;
import top.ribs.scguns.entity.projectile.MicroJetEntity;
import top.ribs.scguns.entity.projectile.NeedleProjectileEntity;
import top.ribs.scguns.entity.projectile.OsborneSlugProjectileEntity;
import top.ribs.scguns.entity.projectile.PlasmaProjectileEntity;
import top.ribs.scguns.entity.projectile.ProjectileEntity;
import top.ribs.scguns.entity.projectile.RaidFlareEntity;
import top.ribs.scguns.entity.projectile.RamrodProjectileEntity;
import top.ribs.scguns.entity.projectile.RocketEntity;
import top.ribs.scguns.entity.projectile.SculkCellEntity;
import top.ribs.scguns.entity.projectile.ShatterRoundProjectileEntity;
import top.ribs.scguns.entity.projectile.ShotballProjectileEntity;
import top.ribs.scguns.entity.projectile.ShulkshotProjectileEntity;
import top.ribs.scguns.entity.projectile.SulfurGasCloudEntity;
import top.ribs.scguns.entity.projectile.SyringeProjectileEntity;
import top.ribs.scguns.entity.projectile.TraumaHookEntity;
import top.ribs.scguns.entity.projectile.turret.TurretProjectileEntity;
import top.ribs.scguns.entity.throwable.GrenadeEntity;
import top.ribs.scguns.entity.throwable.ThrowableBeaconGrenadeEntity;
import top.ribs.scguns.entity.throwable.ThrowableChokeBombEntity;
import top.ribs.scguns.entity.throwable.ThrowableGasGrenadeEntity;
import top.ribs.scguns.entity.throwable.ThrowableGrenadeEntity;
import top.ribs.scguns.entity.throwable.ThrowableHellfireBombEntity;
import top.ribs.scguns.entity.throwable.ThrowableMolotovCocktailEntity;
import top.ribs.scguns.entity.throwable.ThrowableNailBombEntity;
import top.ribs.scguns.entity.throwable.ThrowableShotballEntity;
import top.ribs.scguns.entity.throwable.ThrowableStunGrenadeEntity;
import top.ribs.scguns.entity.throwable.ThrowableSwarmBombEntity;

public class ModEntities {
   public static final DeferredRegister<EntityType<?>> REGISTER = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, "scguns");
   public static final DeferredHolder<EntityType<?>, EntityType<PrimedPowderKeg>> PRIMED_POWDER_KEG = REGISTER.register(
      "primed_powder_keg",
      () -> Builder.<PrimedPowderKeg>of(PrimedPowderKeg::new, MobCategory.MISC).sized(0.98F, 0.98F).clientTrackingRange(10).updateInterval(10).build("primed_powder_keg")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<PrimedNitroKeg>> PRIMED_NITRO_KEG = REGISTER.register(
      "primed_nitro_keg",
      () -> Builder.<PrimedNitroKeg>of(PrimedNitroKeg::new, MobCategory.MISC).sized(0.98F, 0.98F).clientTrackingRange(10).updateInterval(10).build("primed_nitro_keg")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<TurretProjectileEntity>> TURRET_PROJECTILE = REGISTER.register(
      "basic_turret", () -> Builder.<TurretProjectileEntity>of(TurretProjectileEntity::new, MobCategory.MISC).sized(0.5F, 0.5F).build("basic_turret")
   );
   public static final DeferredHolder<EntityType<ProjectileEntity>, EntityType<ProjectileEntity>> PROJECTILE = registerBasic("projectile", ProjectileEntity::new);
   public static final DeferredHolder<EntityType<BearPackShellProjectileEntity>, EntityType<BearPackShellProjectileEntity>> BEARPACK_SHELL_PROJECTILE = registerBasic(
      "bearpack_shell_projectile", BearPackShellProjectileEntity::new
   );
   public static final DeferredHolder<EntityType<OsborneSlugProjectileEntity>, EntityType<OsborneSlugProjectileEntity>> OSBORNE_SLUG_PROJECTILE = registerBasic(
      "osborne_slug_projectile", OsborneSlugProjectileEntity::new
   );
   public static final DeferredHolder<EntityType<PlasmaProjectileEntity>, EntityType<PlasmaProjectileEntity>> PLASMA_PROJECTILE = registerBasic("plasma_projectile", PlasmaProjectileEntity::new);
   public static final DeferredHolder<EntityType<RamrodProjectileEntity>, EntityType<RamrodProjectileEntity>> RAMROD_PROJECTILE = registerBasic("ramrod_projectile", RamrodProjectileEntity::new);
   public static final DeferredHolder<EntityType<HogRoundProjectileEntity>, EntityType<HogRoundProjectileEntity>> HOG_ROUND_PROJECTILE = registerBasic(
      "hog_round_projectile", HogRoundProjectileEntity::new
   );
   public static final DeferredHolder<EntityType<BeowulfProjectileEntity>, EntityType<BeowulfProjectileEntity>> BEOWULF_PROJECTILE = registerBasic(
      "beowulf_projectile", BeowulfProjectileEntity::new
   );
   public static final DeferredHolder<EntityType<BlazeRodProjectileEntity>, EntityType<BlazeRodProjectileEntity>> BLAZE_ROD_PROJECTILE = registerBasic(
      "blaze_rod_projectile", BlazeRodProjectileEntity::new
   );
   public static final DeferredHolder<EntityType<BasicBulletProjectileEntity>, EntityType<BasicBulletProjectileEntity>> BASIC_BULLET_PROJECTILE = registerBasic(
      "basic_bullet_projectile", BasicBulletProjectileEntity::new
   );
   public static final DeferredHolder<EntityType<NeedleProjectileEntity>, EntityType<NeedleProjectileEntity>> NEEDLE_PROJECTILE = registerBasic("needle_projectile", NeedleProjectileEntity::new);
   public static final DeferredHolder<EntityType<FlechetteProjectileEntity>, EntityType<FlechetteProjectileEntity>> FLECHETTE_PROJECTILE = registerBasic(
      "flechette_projectile", FlechetteProjectileEntity::new
   );
   public static final DeferredHolder<EntityType<HardenedBulletProjectileEntity>, EntityType<HardenedBulletProjectileEntity>> HARDENED_BULLET_PROJECTILE = registerBasic(
      "hardened_bullet_projectile", HardenedBulletProjectileEntity::new
   );
   public static final DeferredHolder<EntityType<BuckshotProjectileEntity>, EntityType<BuckshotProjectileEntity>> BUCKSHOT_PROJECTILE = registerBasic(
      "buckshot_projectile", BuckshotProjectileEntity::new
   );
   public static final DeferredHolder<EntityType<FireRoundEntity>, EntityType<FireRoundEntity>> FIRE_ROUND_PROJECTILE = registerBasic("fire_round_projectile", FireRoundEntity::new);
   public static final DeferredHolder<EntityType<GrenadeEntity>, EntityType<GrenadeEntity>> GRENADE = registerBasic("grenade", GrenadeEntity::new);
   public static final DeferredHolder<EntityType<RocketEntity>, EntityType<RocketEntity>> ROCKET = registerBasic("rocket", RocketEntity::new);
   public static final DeferredHolder<EntityType<HeGrenadeRoundEntity>, EntityType<HeGrenadeRoundEntity>> HE_GRENADE_PROJECTILE = registerBasic("he_grenade", HeGrenadeRoundEntity::new);
   public static final DeferredHolder<EntityType<FireGrenadeRoundEntity>, EntityType<FireGrenadeRoundEntity>> FIRE_GRENADE_PROJECTILE = registerBasic("fire_grenade", FireGrenadeRoundEntity::new);
   public static final DeferredHolder<EntityType<BouncyGrenadeRoundEntity>, EntityType<BouncyGrenadeRoundEntity>> BOUNCY_GRENADE_PROJECTILE = registerBasic(
      "bouncy_grenade", BouncyGrenadeRoundEntity::new
   );
   public static final DeferredHolder<EntityType<GasGrenadeRoundEntity>, EntityType<GasGrenadeRoundEntity>> GAS_GRENADE_PROJECTILE = registerBasic("gas_grenade", GasGrenadeRoundEntity::new);
   public static final DeferredHolder<EntityType<MicroJetEntity>, EntityType<MicroJetEntity>> MICROJET = registerBasic("microjet", MicroJetEntity::new);
   public static final DeferredHolder<EntityType<ShulkshotProjectileEntity>, EntityType<ShulkshotProjectileEntity>> SHULKSHOT = registerBasic("shulkshot_projectile", ShulkshotProjectileEntity::new);
   public static final DeferredHolder<EntityType<SculkCellEntity>, EntityType<SculkCellEntity>> SCULK_CELL = registerBasic("sculk_cell", SculkCellEntity::new);
   public static final DeferredHolder<EntityType<FrogDartProjectileEntity>, EntityType<FrogDartProjectileEntity>> FROG_DART_PROJECTILE = registerBasic(
      "frog_dart_projectile", FrogDartProjectileEntity::new
   );
   public static final DeferredHolder<EntityType<ShatterRoundProjectileEntity>, EntityType<ShatterRoundProjectileEntity>> SHATTER_ROUND_PROJECTILE = registerBasic(
      "shatter_round_projectile", ShatterRoundProjectileEntity::new
   );
   public static final DeferredHolder<EntityType<SyringeProjectileEntity>, EntityType<SyringeProjectileEntity>> SYRINGE_PROJECTILE = registerBasic(
      "syringe_projectile", SyringeProjectileEntity::new
   );
   public static final DeferredHolder<EntityType<KrahgRoundProjectileEntity>, EntityType<KrahgRoundProjectileEntity>> KRAHG_ROUND_PROJECTILE = registerBasic(
      "krahg_round_projectile", KrahgRoundProjectileEntity::new
   );
   public static final DeferredHolder<EntityType<AdvancedRoundProjectileEntity>, EntityType<AdvancedRoundProjectileEntity>> ADVANCED_ROUND_PROJECTILE = registerBasic(
      "advanced_round_projectile", AdvancedRoundProjectileEntity::new
   );
   public static final DeferredHolder<EntityType<GibbsRoundProjectileEntity>, EntityType<GibbsRoundProjectileEntity>> GIBBS_ROUND_PROJECTILE = registerBasic(
      "gibbs_round_projectile", GibbsRoundProjectileEntity::new
   );
   public static final DeferredHolder<EntityType<ShotballProjectileEntity>, EntityType<ShotballProjectileEntity>> SHOTBALL_PROJECTILE = registerBasic(
      "shotball_projectile", ShotballProjectileEntity::new
   );
   public static final DeferredHolder<EntityType<ThrowableGrenadeEntity>, EntityType<ThrowableGrenadeEntity>> THROWABLE_GRENADE = registerBasic("throwable_grenade", ThrowableGrenadeEntity::new);
   public static final DeferredHolder<EntityType<ThrowableStunGrenadeEntity>, EntityType<ThrowableStunGrenadeEntity>> THROWABLE_STUN_GRENADE = registerBasic(
      "throwable_stun_grenade", ThrowableStunGrenadeEntity::new
   );
   public static final DeferredHolder<EntityType<ThrowableMolotovCocktailEntity>, EntityType<ThrowableMolotovCocktailEntity>> THROWABLE_MOLOTOV_COCKTAIL = registerBasic(
      "throwable_molotov_cocktail", ThrowableMolotovCocktailEntity::new
   );
   public static final DeferredHolder<EntityType<ThrowableHellfireBombEntity>, EntityType<ThrowableHellfireBombEntity>> THROWABLE_HELLFIRE_BOMB = registerBasic(
      "throwable_hellfire_bomb", ThrowableHellfireBombEntity::new
   );
   public static final DeferredHolder<EntityType<ThrowableGasGrenadeEntity>, EntityType<ThrowableGasGrenadeEntity>> THROWABLE_GAS_GRENADE = registerBasic(
      "throwable_gas_grenade", ThrowableGasGrenadeEntity::new
   );
   public static final DeferredHolder<EntityType<ThrowableBeaconGrenadeEntity>, EntityType<ThrowableBeaconGrenadeEntity>> THROWABLE_BEACON_GRENADE = registerBasic(
      "throwable_beacon_grenade", ThrowableBeaconGrenadeEntity::new
   );
   public static final DeferredHolder<EntityType<ThrowableChokeBombEntity>, EntityType<ThrowableChokeBombEntity>> THROWABLE_CHOKE_BOMB = registerBasic(
      "throwable_choke_bomb", ThrowableChokeBombEntity::new
   );
   public static final DeferredHolder<EntityType<ThrowableSwarmBombEntity>, EntityType<ThrowableSwarmBombEntity>> THROWABLE_SWARM_BOMB = registerBasic(
      "throwable_swarm_bomb", ThrowableSwarmBombEntity::new
   );
   public static final DeferredHolder<EntityType<ThrowableShotballEntity>, EntityType<ThrowableShotballEntity>> THROWABLE_SHOTBALL = registerBasic(
      "throwable_shotball", ThrowableShotballEntity::new
   );
   public static final DeferredHolder<EntityType<ThrowableNailBombEntity>, EntityType<ThrowableNailBombEntity>> THROWABLE_NAIL_BOMB = registerBasic(
      "throwable_nail_bomb", ThrowableNailBombEntity::new
   );
   public static final DeferredHolder<EntityType<SulfurGasCloudEntity>, EntityType<SulfurGasCloudEntity>> SULFUR_GAS_CLOUD = registerBasic("sulfur_gas_cloud", SulfurGasCloudEntity::new);
   public static final DeferredHolder<EntityType<?>, EntityType<CogMinionEntity>> COG_MINION = REGISTER.register(
      "cog_minion", () -> Builder.<CogMinionEntity>of(CogMinionEntity::new, MobCategory.MONSTER).sized(0.7F, 1.8F).build("cog_minion")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<CogKnightEntity>> COG_KNIGHT = REGISTER.register(
      "cog_knight", () -> Builder.<CogKnightEntity>of(CogKnightEntity::new, MobCategory.MONSTER).sized(0.75F, 1.9F).build("cog_knight")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<SkyCarrierEntity>> SKY_CARRIER = REGISTER.register(
      "sky_carrier", () -> Builder.<SkyCarrierEntity>of(SkyCarrierEntity::new, MobCategory.MONSTER).sized(1.4F, 1.7F).build("sky_carrier")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<HiveEntity>> HIVE = REGISTER.register(
      "hive", () -> Builder.<HiveEntity>of(HiveEntity::new, MobCategory.MONSTER).sized(0.8F, 2.0F).build("hive")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<SwarmEntity>> SWARM = REGISTER.register(
      "swarm", () -> Builder.<SwarmEntity>of(SwarmEntity::new, MobCategory.MONSTER).sized(0.8F, 2.0F).build("swarm")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<SupplyScampEntity>> SUPPLY_SCAMP = REGISTER.register(
      "supply_scamp", () -> Builder.<SupplyScampEntity>of(SupplyScampEntity::new, MobCategory.CREATURE).sized(1.0F, 1.3F).build("supply_scamp")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<DissidentEntity>> DISSIDENT = REGISTER.register(
      "dissident", () -> Builder.<DissidentEntity>of(DissidentEntity::new, MobCategory.MONSTER).sized(1.4F, 1.7F).build("dissident")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<ViventrumEntity>> VIVENTRUM = REGISTER.register(
      "viventrum", () -> Builder.<ViventrumEntity>of(ViventrumEntity::new, MobCategory.MONSTER).sized(0.6F, 1.8F).build("viventrum")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<HornlinEntity>> HORNLIN = REGISTER.register(
      "hornlin", () -> Builder.<HornlinEntity>of(HornlinEntity::new, MobCategory.MONSTER).sized(1.4F, 1.7F).build("hornlin")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<ZombifiedHornlinEntity>> ZOMBIFIED_HORNLIN = REGISTER.register(
      "zombified_hornlin", () -> Builder.<ZombifiedHornlinEntity>of(ZombifiedHornlinEntity::new, MobCategory.MONSTER).sized(1.4F, 1.7F).build("zombified_hornlin")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<TheMerchantEntity>> THE_MERCHANT = REGISTER.register(
      "the_merchant",
      () -> Builder.<TheMerchantEntity>of((entityType, level) -> new TheMerchantEntity(entityType, level), MobCategory.MONSTER)
            .sized(1.5F, 2.25F)
            .build("the_merchant")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<SulfurheadEntity>> SULFURHEAD = REGISTER.register(
      "sulfurhead", () -> Builder.<SulfurheadEntity>of(SulfurheadEntity::new, MobCategory.MONSTER).sized(0.8F, 2.0F).build("sulfurhead")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<AdjudicatorEntity>> ADJUDICATOR = REGISTER.register(
      "adjudicator", () -> Builder.<AdjudicatorEntity>of(AdjudicatorEntity::new, MobCategory.MONSTER).sized(0.8F, 2.2F).build("adjudicator")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<SubjugatorEntity>> SUBJUGATOR = REGISTER.register(
      "subjugator", () -> Builder.<SubjugatorEntity>of(SubjugatorEntity::new, MobCategory.MONSTER).sized(1.0F, 2.5F).build("subjugator")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<BlundererEntity>> BLUNDERER = REGISTER.register(
      "blunderer", () -> Builder.<BlundererEntity>of(BlundererEntity::new, MobCategory.MONSTER).sized(1.5F, 2.8F).build("blunderer")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<TraumaUnitEntity>> TRAUMA_UNIT = REGISTER.register(
      "trauma_unit", () -> Builder.<TraumaUnitEntity>of(TraumaUnitEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).build("trauma_unit")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<ScampTankEntity>> SCAMP_TANK = REGISTER.register(
      "scamp_tank", () -> Builder.<ScampTankEntity>of(ScampTankEntity::new, MobCategory.MONSTER).sized(5.0F, 4.0F).build("scamp_tank")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<SignalBeaconEntity>> SIGNAL_BEACON = REGISTER.register(
      "signal_beacon", () -> Builder.<SignalBeaconEntity>of(SignalBeaconEntity::new, MobCategory.MISC).sized(1.0F, 1.0F).build("signal_beacon")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<ScamplerEntity>> SCAMPLER = REGISTER.register(
      "scampler", () -> Builder.<ScamplerEntity>of(ScamplerEntity::new, MobCategory.MONSTER).sized(1.0F, 1.0F).build("scampler")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<MotherGhastEntity>> MOTHER_GHAST = REGISTER.register(
      "mother_ghast", () -> Builder.<MotherGhastEntity>of(MotherGhastEntity::new, MobCategory.MONSTER).sized(8.0F, 7.0F).build("mother_ghast")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<FinforcerEntity>> FINFORCER = REGISTER.register(
      "finforcer", () -> Builder.<FinforcerEntity>of(FinforcerEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).build("finforcer")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<PraetorEntity>> PRAETOR = REGISTER.register(
      "praetor", () -> Builder.<PraetorEntity>of(PraetorEntity::new, MobCategory.MONSTER).sized(1.0F, 3.0F).build("praetor")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<BeaconProjectileEntity>> BEACON_PROJECTILE = REGISTER.register(
      "beacon_projectile",
      () -> Builder.<BeaconProjectileEntity>of(BeaconProjectileEntity::new, MobCategory.MISC)
            .sized(0.5F, 0.5F)
            .setTrackingRange(64)
            .setUpdateInterval(1)
            .setShouldReceiveVelocityUpdates(true)
            .build("beacon_projectile")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<TraumaHookEntity>> TRAUMA_HOOK = REGISTER.register(
      "trauma_hook",
      () -> Builder.<TraumaHookEntity>of(TraumaHookEntity::new, MobCategory.MISC)
            .sized(0.25F, 0.25F)
            .setTrackingRange(64)
            .setUpdateInterval(3)
            .setShouldReceiveVelocityUpdates(true)
            .build("trauma_hook")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<RaidFlareEntity>> RAID_FLARE = REGISTER.register(
      "raid_flare",
      () -> Builder.<RaidFlareEntity>of(RaidFlareEntity::new, MobCategory.MISC)
            .sized(0.25F, 0.25F)
            .setTrackingRange(100)
            .setUpdateInterval(1)
            .fireImmune()
            .setShouldReceiveVelocityUpdates(true)
            .build("raid_flare")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<EnemyProjectileEntity>> ENEMY_PROJECTILE = REGISTER.register(
      "brass_bolt",
      () -> Builder.<EnemyProjectileEntity>of(EnemyProjectileEntity::new, MobCategory.MISC)
            .sized(0.5F, 0.5F)
            .setTrackingRange(64)
            .setUpdateInterval(1)
            .setShouldReceiveVelocityUpdates(true)
            .build("brass_bolt")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<ScampRocketEntity>> SCAMP_ROCKET = REGISTER.register(
      "scamp_rocket",
      () -> Builder.<ScampRocketEntity>of(ScampRocketEntity::new, MobCategory.MISC)
            .sized(0.5F, 0.5F)
            .setTrackingRange(64)
            .setUpdateInterval(1)
            .setShouldReceiveVelocityUpdates(true)
            .build("scamp_rocket")
   );

   public ModEntities() {
      super();
   }

   @SuppressWarnings("unchecked")
   private static <T extends Entity> DeferredHolder<EntityType<T>, EntityType<T>> registerBasic(String id, BiFunction<EntityType<T>, Level, T> function) {
      DeferredHolder<EntityType<?>, EntityType<T>> holder = REGISTER.register(
         id,
         () -> Builder.<T>of(function::apply, MobCategory.MISC)
               .sized(0.25F, 0.25F)
               .setTrackingRange(100)
               .setUpdateInterval(1)
               .noSummon()
               .fireImmune()
               .noSave()
               .setShouldReceiveVelocityUpdates(true)
               .build(id)
      );
      return (DeferredHolder<EntityType<T>, EntityType<T>>) (Object) holder;
   }
}
