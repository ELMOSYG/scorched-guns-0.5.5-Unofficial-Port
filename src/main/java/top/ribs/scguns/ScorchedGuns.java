package top.ribs.scguns;


import top.ribs.scguns.util.DistHelper;
import com.mrcrayfish.framework.api.FrameworkAPI;
import net.minecraft.resources.ResourceLocation;
import it.crystalnest.prometheus.api.FireManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.fml.event.config.ModConfigEvent.Loading;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import top.ribs.scguns.attributes.SCAttributes;
import top.ribs.scguns.client.ClientHandler;
import top.ribs.scguns.client.CustomGunManager;
import top.ribs.scguns.client.handler.BeamHandler;
import top.ribs.scguns.client.handler.BulletTrailRenderingHandler;
import top.ribs.scguns.client.handler.HUDRenderHandler;
import top.ribs.scguns.client.handler.InspectHandler;
import top.ribs.scguns.client.handler.RecoilHandler;
import top.ribs.scguns.client.screen.ModMenuTypes;
import top.ribs.scguns.common.BoundingBoxManager;
import top.ribs.scguns.common.NetworkGunManager;
import top.ribs.scguns.common.ProjectileManager;
import top.ribs.scguns.common.TurretManager;
import top.ribs.scguns.common.VentManager;
import top.ribs.scguns.common.exosuit.ExoSuitUpgradeManager;
import top.ribs.scguns.compat.CompatManager;
import top.ribs.scguns.config.AdvancedComposterDropsConfig;
import top.ribs.scguns.config.EliteTierConfig;
import top.ribs.scguns.config.EntityEquipmentConfig;
import top.ribs.scguns.config.GunMobValues;
import top.ribs.scguns.config.GunnerMobConfig;
import top.ribs.scguns.config.GunnerMobSpawner;
import top.ribs.scguns.config.MerchantTradeConfig;
import top.ribs.scguns.config.MobGuideConfig;
import top.ribs.scguns.config.ProjectileAdvantageConfig;
import top.ribs.scguns.config.RaidConfig;
import top.ribs.scguns.config.RaidFlareConfig;
import top.ribs.scguns.config.ShockCoilConfig;
import top.ribs.scguns.config.TieredWeaponConfig;
import top.ribs.scguns.entity.player.GunTierRegistry;
import top.ribs.scguns.entity.player.GunTiers;
import top.ribs.scguns.entity.projectile.AdvancedRoundProjectileEntity;
import top.ribs.scguns.entity.projectile.BasicBulletProjectileEntity;
import top.ribs.scguns.entity.projectile.BearPackShellProjectileEntity;
import top.ribs.scguns.entity.projectile.BeowulfProjectileEntity;
import top.ribs.scguns.entity.projectile.BlazeRodProjectileEntity;
import top.ribs.scguns.entity.projectile.BouncyGrenadeRoundEntity;
import top.ribs.scguns.entity.projectile.BuckshotProjectileEntity;
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
import top.ribs.scguns.entity.projectile.LightningProjectileEntity;
import top.ribs.scguns.entity.projectile.MicroJetEntity;
import top.ribs.scguns.entity.projectile.NeedleProjectileEntity;
import top.ribs.scguns.entity.projectile.OsborneSlugProjectileEntity;
import top.ribs.scguns.entity.projectile.PlasmaProjectileEntity;
import top.ribs.scguns.entity.projectile.ProjectileEntity;
import top.ribs.scguns.entity.projectile.RamrodProjectileEntity;
import top.ribs.scguns.entity.projectile.RocketEntity;
import top.ribs.scguns.entity.projectile.SculkCellEntity;
import top.ribs.scguns.entity.projectile.ShatterRoundProjectileEntity;
import top.ribs.scguns.entity.projectile.ShotballProjectileEntity;
import top.ribs.scguns.entity.projectile.ShulkshotProjectileEntity;
import top.ribs.scguns.entity.projectile.SyringeProjectileEntity;
import top.ribs.scguns.entity.throwable.GrenadeEntity;
import top.ribs.scguns.event.ModCommandsRegister;
import top.ribs.scguns.event.OceanWeaponEventHandler;
import top.ribs.scguns.event.PiglinWeaponEventHandler;
import top.ribs.scguns.event.TemporaryLightManager;
import top.ribs.scguns.event.WeaponMovementEventHandler;
import top.ribs.scguns.init.ModArmorMaterials;
import top.ribs.scguns.init.ModBlockEntities;
import top.ribs.scguns.init.ModBlocks;
import top.ribs.scguns.init.ModContainers;
import top.ribs.scguns.init.ModCreativeModeTabs;
import top.ribs.scguns.init.ModEffects;
import top.ribs.scguns.init.ModEnchantments;
import top.ribs.scguns.init.ModEntities;
import top.ribs.scguns.init.ModFeatures;
import top.ribs.scguns.init.ModFluids;
import top.ribs.scguns.init.ModItems;
import top.ribs.scguns.init.ModLootModifiers;
import top.ribs.scguns.init.ModPaintings;
import top.ribs.scguns.init.ModParticleTypes;
import top.ribs.scguns.init.ModPointOfInterestTypes;
import top.ribs.scguns.init.ModRecipes;
import top.ribs.scguns.init.ModSounds;
import top.ribs.scguns.init.ModStructures;
import top.ribs.scguns.init.ModSyncedDataKeys;
import top.ribs.scguns.init.ModVillagers;
import top.ribs.scguns.network.PacketHandler;
import top.ribs.scguns.util.ModCauldronInteraction;
import top.ribs.scguns.world.VillageStructures;

@Mod("scguns")
public class ScorchedGuns {
   public static final String MODID = "scguns";
   public static final Logger LOGGER = LogManager.getLogger("scguns");
   public static boolean backpackedLoaded;
   public static boolean curiosLoaded;
   public static boolean controllableLoaded;
   public static boolean playerReviveLoaded;
   public static boolean createLoaded;
   public static boolean farmersDelightLoaded;
   public static boolean mekanismLoaded;
   public static boolean ieLoaded;
   public static boolean valkyrienSkiesLoaded;
   /** Sable: the 1.21 physics mod that moves blocks around as "sub-levels" (Create: Aeronautics). */
   public static boolean sableLoaded;
   /**
    * True when a physics-structure mod is present. Those mods add no blocks of their own;
    * they replace {@code BlockGetter#clip} with an implementation that also raycasts their
    * moving structures (Sable does it in
    * {@code dev.ryanhcode.sable.mixin.clip_overwrite.BlockGetterMixin}, keeping the vanilla
    * traversal as {@code originalClip}). Code that walks blocks itself - like the gun
    * projectiles - therefore only ever sees the main level and shoots straight through
    * those structures.
    */
   public static boolean physicsStructuresLoaded;
   public static boolean soulFiredLoaded;
   public static boolean shoulderSurfingLoaded = false;
   public static boolean createIronWorksLoaded = false;

   public ScorchedGuns(IEventBus modEventBus, ModContainer modContainer) {
      super();
      modContainer.registerConfig(Type.CLIENT, Config.clientSpec);
      modContainer.registerConfig(Type.COMMON, Config.commonSpec);
      modContainer.registerConfig(Type.SERVER, Config.serverSpec);
      IEventBus bus = modEventBus;
      modEventBus.addListener(this::onConfigLoad);
      ModItems.REGISTER.register(bus);
      // 1.21.1 keeps armor materials in a built-in registry, so they are registered
      // here rather than declared as data/scguns/armor_material/*.json (1.21.2+).
      ModArmorMaterials.REGISTER.register(bus);
      // 0.5.5 called ModRecipes.register(...) three times (lines 147, 161 and 169 of
      // the decompile). Forge's DeferredRegister#register was happy to be handed the
      // same bus repeatedly; NeoForge throws "Cannot register DeferredRegister to more
      // than one event bus" on any second call, so it is registered exactly once.
      ModRecipes.register(modEventBus);
      SCAttributes.ATTRIBUTES.register(modEventBus);
      this.initializeModDependencies();
      ModItems.registerItems();
      NeoForge.EVENT_BUS.addListener(VillageStructures::addNewVillageBuilding);
      ModCreativeModeTabs.register(bus);
      ModBlockEntities.BLOCK_ENTITIES.register(bus);
      ModBlocks.REGISTER.register(bus);
      ModContainers.REGISTER.register(bus);
      ModEffects.REGISTER.register(bus);
      ModMenuTypes.register(bus);
      ModEntities.REGISTER.register(bus);
      ModParticleTypes.REGISTER.register(bus);
      ModSounds.REGISTER.register(bus);
      ModVillagers.register(bus);
      ModFeatures.register(bus);
      ModFluids.FLUID_TYPES.register(modEventBus);
      ModFluids.FLUIDS.register(modEventBus);
      ModLootModifiers.LOOT_MODIFIERS.register(bus);
      ModPointOfInterestTypes.REGISTER.register(bus);
      ModPaintings.REGISTER.register(modEventBus);
      ModStructures.REGISTRY.register(bus);
      bus.addListener(this::onCommonSetup);
      // GunnerMobSpawner and ModCommandsRegister both carry
      // @EventBusSubscriber(modid = "scguns", bus = Bus.GAME), which already registers
      // them, and every one of their @SubscribeEvent methods is static. Adding an
      // *instance* registration made NeoForge abort mod construction with
      // "Expected @SubscribeEvent method ... to NOT be static because register() was
      // called with an instance type" (Forge 1.20.1 tolerated the mix). The annotation
      // is the registration now, and it avoids registering either class twice.
      DistHelper.runWhenOn(Dist.CLIENT, () -> {
            ClientHandler.registerClientHandlers(bus);
            // HUDRenderHandler is registered by ClientHandler.registerClientHandlers
            // above; registering it here as well made every @SubscribeEvent method in
            // it run twice per event, so the HUD was drawn twice. (Inherited from
            // 0.5.5, where Forge tolerated the duplicate.)
            NeoForge.EVENT_BUS.register(InspectHandler.get());
            NeoForge.EVENT_BUS.register(BeamHandler.class);
            NeoForge.EVENT_BUS.register(BulletTrailRenderingHandler.get());
         });
      NeoForge.EVENT_BUS.register(this);
      NeoForge.EVENT_BUS.register(WeaponMovementEventHandler.class);
      NeoForge.EVENT_BUS.register(OceanWeaponEventHandler.class);
      NeoForge.EVENT_BUS.register(PiglinWeaponEventHandler.class);
      NeoForge.EVENT_BUS.register(MerchantTradeConfig.class);
      NeoForge.EVENT_BUS.register(GunnerMobConfig.class);
      NeoForge.EVENT_BUS.register(TieredWeaponConfig.class);
      NeoForge.EVENT_BUS.register(EliteTierConfig.class);
      NeoForge.EVENT_BUS.register(AdvancedComposterDropsConfig.class);
      NeoForge.EVENT_BUS.register(MobGuideConfig.class);
      NeoForge.EVENT_BUS.register(EntityEquipmentConfig.class);
      NeoForge.EVENT_BUS.register(ProjectileAdvantageConfig.class);
      NeoForge.EVENT_BUS.register(TurretManager.class);
      NeoForge.EVENT_BUS.register(VentManager.class);
      NeoForge.EVENT_BUS.register(RaidConfig.class);
      NeoForge.EVENT_BUS.register(RaidFlareConfig.class);
      NeoForge.EVENT_BUS.register(ShockCoilConfig.class);
   }

   private void onConfigLoad(Loading event) {
      if (event.getConfig().getType() == Type.SERVER) {
         DistHelper.runWhenOn(Dist.CLIENT, () -> RecoilHandler.get().updateConfig());
      }
   }

   private void initializeModDependencies() {
      valkyrienSkiesLoaded = ModList.get().isLoaded("valkyrienskies");
      sableLoaded = ModList.get().isLoaded("sable");
      // Both Valkyrien Skies and Sable answer this by replacing BlockGetter#clip, which is
      // what the projectile raycast delegates to when the flag is set.
      physicsStructuresLoaded = sableLoaded || valkyrienSkiesLoaded;
      controllableLoaded = ModList.get().isLoaded("controllable");
      backpackedLoaded = ModList.get().isLoaded("backpacked");
      curiosLoaded = ModList.get().isLoaded("curios");
      playerReviveLoaded = ModList.get().isLoaded("playerrevive");
      createLoaded = ModList.get().isLoaded("create");
      farmersDelightLoaded = ModList.get().isLoaded("farmersdelight");
      ieLoaded = ModList.get().isLoaded("immersiveengineering");
      mekanismLoaded = ModList.get().isLoaded("mekanism");
      soulFiredLoaded = ModList.get().isLoaded("soul_fire_d");
      shoulderSurfingLoaded = ModList.get().isLoaded("shouldersurfing");
      createIronWorksLoaded = ModList.get().isLoaded("create_ironworks");
   }

   /**
    * 0.5.5 called {@code it.crystalnest.soul_fire_d.api.FireManager.setOnFire(entity, seconds,
    * SOUL_FIRE_TYPE)}. In the 1.21 rewrite Soul Fire'd moved that whole fire API into
    * Prometheus: its own {@code fire.FireRegistry} initialiser sets
    * {@code SOUL_FIRE_TYPE = it.crystalnest.prometheus.api.FireManager.SOUL_FIRE_TYPE} and
    * builds the fire through {@code FireManager.fireBuilder} (soul flame particles, light 10,
    * 2.0 damage). The call therefore moved to {@code prometheus.api.FireManager} with the
    * same signature; Prometheus is Soul Fire'd's required dependency, and this branch only
    * runs when {@code soul_fire_d} is loaded, so the class is never resolved otherwise.
    */
   public static void setSoulFireOnEntity(Entity entity, int seconds) {
      if (soulFiredLoaded) {
         try {
            FireManager.setOnFire(entity, seconds, FireManager.SOUL_FIRE_TYPE);
         } catch (Exception e) {
            entity.igniteForSeconds(seconds);
         }
      } else {
         entity.igniteForSeconds(seconds);
      }
   }

   @SubscribeEvent
   public void onAddReloadListeners(AddReloadListenerEvent event) {
      event.addListener(new ExoSuitUpgradeManager());
   }

   private void onCommonSetup(FMLCommonSetupEvent event) {
      event.enqueueWork(
         () -> {
            GunTiers.init();
            GunTierRegistry.lock();
            PacketHandler.init();
            GunMobValues.init();
            FrameworkAPI.registerSyncedDataKey(ModSyncedDataKeys.AIMING);
            FrameworkAPI.registerSyncedDataKey(ModSyncedDataKeys.RELOADING);
            FrameworkAPI.registerSyncedDataKey(ModSyncedDataKeys.SHOOTING);
            FrameworkAPI.registerSyncedDataKey(ModSyncedDataKeys.BURSTCOUNT);
            FrameworkAPI.registerSyncedDataKey(ModSyncedDataKeys.ONBURSTCOOLDOWN);
            FrameworkAPI.registerSyncedDataKey(ModSyncedDataKeys.MELEE);
            ModCauldronInteraction.register();
            // Framework 0.13 removed registerLoginData; the gun registry is pushed
            // to each player when they join instead (see NetworkGunManager).
            ProjectileManager.getInstance()
               .registerFactory(
                  (Item)ModItems.POWDER_AND_BALL.get(),
                  (worldIn, entity, weapon, item, modifiedGun) -> new ProjectileEntity(
                        (EntityType<? extends Entity>)ModEntities.PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun
                     )
               );
            ProjectileManager.getInstance()
               .registerFactory(
                  (Item)ModItems.GRAPESHOT.get(),
                  (worldIn, entity, weapon, item, modifiedGun) -> new ProjectileEntity(
                        (EntityType<? extends Entity>)ModEntities.PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun
                     )
               );
            ProjectileManager.getInstance()
               .registerFactory(
                  (Item)ModItems.COMPACT_COPPER_ROUND.get(),
                  (worldIn, entity, weapon, item, modifiedGun) -> new ProjectileEntity(
                        (EntityType<? extends Entity>)ModEntities.PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun
                     )
               );
            ProjectileManager.getInstance()
               .registerFactory(
                  (Item)ModItems.HOG_ROUND.get(),
                  (worldIn, entity, weapon, item, modifiedGun) -> new HogRoundProjectileEntity(
                        (EntityType<? extends Entity>)ModEntities.HOG_ROUND_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun
                     )
               );
            ProjectileManager.getInstance()
               .registerFactory(
                  (Item)ModItems.STANDARD_COPPER_ROUND.get(),
                  (worldIn, entity, weapon, item, modifiedGun) -> new ProjectileEntity(
                        (EntityType<? extends Entity>)ModEntities.PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun
                     )
               );
            ProjectileManager.getInstance()
               .registerFactory(
                  (Item)ModItems.COMPACT_ADVANCED_ROUND.get(),
                  (worldIn, entity, weapon, item, modifiedGun) -> new ProjectileEntity(
                        (EntityType<? extends Entity>)ModEntities.PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun
                     )
               );
            ProjectileManager.getInstance()
               .registerFactory(
                  (Item)ModItems.RAMROD_ROUND.get(),
                  (worldIn, entity, weapon, item, modifiedGun) -> new RamrodProjectileEntity(
                        (EntityType<? extends Entity>)ModEntities.RAMROD_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun
                     )
               );
            ProjectileManager.getInstance()
               .registerFactory(
                  (Item)ModItems.ADVANCED_ROUND.get(),
                  (worldIn, entity, weapon, item, modifiedGun) -> new AdvancedRoundProjectileEntity(
                        (EntityType<? extends Entity>)ModEntities.ADVANCED_ROUND_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun
                     )
               );
            ProjectileManager.getInstance()
               .registerFactory(
                  (Item)ModItems.SHATTER_ROUND.get(),
                  (worldIn, entity, weapon, item, modifiedGun) -> new ShatterRoundProjectileEntity(
                        (EntityType<? extends Entity>)ModEntities.SHATTER_ROUND_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun
                     )
               );
            ProjectileManager.getInstance()
               .registerFactory(
                  (Item)ModItems.KRAHG_ROUND.get(),
                  (worldIn, entity, weapon, item, modifiedGun) -> new KrahgRoundProjectileEntity(
                        (EntityType<? extends Entity>)ModEntities.KRAHG_ROUND_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun
                     )
               );
            ProjectileManager.getInstance()
               .registerFactory(
                  (Item)ModItems.BEOWULF_ROUND.get(),
                  (worldIn, entity, weapon, item, modifiedGun) -> new BeowulfProjectileEntity(
                        (EntityType<? extends Entity>)ModEntities.BEOWULF_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun
                     )
               );
            ProjectileManager.getInstance()
               .registerFactory(
                  (Item)ModItems.GIBBS_ROUND.get(),
                  (worldIn, entity, weapon, item, modifiedGun) -> new GibbsRoundProjectileEntity(
                        (EntityType<? extends Entity>)ModEntities.GIBBS_ROUND_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun
                     )
               );
            ProjectileManager.getInstance()
               .registerFactory(
                  (Item)ModItems.SHOTGUN_SHELL.get(),
                  (worldIn, entity, weapon, item, modifiedGun) -> new ProjectileEntity(
                        (EntityType<? extends Entity>)ModEntities.PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun
                     )
               );
            ProjectileManager.getInstance()
               .registerFactory(
                  (Item)ModItems.BEARPACK_SHELL.get(),
                  (worldIn, entity, weapon, item, modifiedGun) -> new BearPackShellProjectileEntity(
                        (EntityType<? extends Entity>)ModEntities.BEARPACK_SHELL_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun
                     )
               );
            ProjectileManager.getInstance()
               .registerFactory(
                  (Item)ModItems.BLAZE_FUEL.get(),
                  (worldIn, entity, weapon, item, modifiedGun) -> new FireRoundEntity(
                        (EntityType<? extends Entity>)ModEntities.FIRE_ROUND_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun
                     )
               );
            ProjectileManager.getInstance()
               .registerFactory(
                  (Item)ModItems.SCULK_CELL.get(),
                  (worldIn, entity, weapon, item, modifiedGun) -> new SculkCellEntity(
                        (EntityType<? extends ProjectileEntity>)ModEntities.SCULK_CELL.get(), worldIn, entity, weapon, item, modifiedGun
                     )
               );
            ProjectileManager.getInstance()
               .registerFactory(
                  (Item)ModItems.SHOCK_CELL.get(),
                  (worldIn, entity, weapon, item, modifiedGun) -> new LightningProjectileEntity(
                        (EntityType<? extends Entity>)ModEntities.PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun
                     )
               );
            ProjectileManager.getInstance()
               .registerFactory(
                  (Item)ModItems.SHULKSHOT.get(),
                  (worldIn, entity, weapon, item, modifiedGun) -> new ShulkshotProjectileEntity(
                        (EntityType<? extends Entity>)ModEntities.SHULKSHOT.get(), worldIn, entity, weapon, item, modifiedGun
                     )
               );
            ProjectileManager.getInstance()
               .registerFactory(
                  (Item)ModItems.ENERGY_CELL.get(),
                  (worldIn, entity, weapon, item, modifiedGun) -> new PlasmaProjectileEntity(
                        (EntityType<? extends Entity>)ModEntities.PLASMA_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun
                     )
               );
            ProjectileManager.getInstance()
               .registerFactory(
                  (Item)ModItems.OSBORNE_SLUG.get(),
                  (worldIn, entity, weapon, item, modifiedGun) -> new OsborneSlugProjectileEntity(
                        (EntityType<? extends Entity>)ModEntities.OSBORNE_SLUG_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun
                     )
               );
            ProjectileManager.getInstance()
               .registerFactory(
                  Items.BLAZE_ROD,
                  (worldIn, entity, weapon, item, modifiedGun) -> new BlazeRodProjectileEntity(
                        (EntityType<? extends Entity>)ModEntities.BLAZE_ROD_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun
                     )
               );
            ProjectileManager.getInstance()
               .registerFactory(
                  (Item)ModItems.SYRINGE.get(),
                  (worldIn, entity, weapon, item, modifiedGun) -> new SyringeProjectileEntity(
                        (EntityType<? extends Entity>)ModEntities.SYRINGE_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun
                     )
               );
            ProjectileManager.getInstance()
               .registerFactory(
                  (Item)ModItems.STANDARD_BULLET.get(),
                  (worldIn, entity, weapon, item, modifiedGun) -> new BasicBulletProjectileEntity(
                        (EntityType<? extends Entity>)ModEntities.BASIC_BULLET_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun
                     )
               );
            ProjectileManager.getInstance()
               .registerFactory(
                  (Item)ModItems.ADVANCED_BULLET.get(),
                  (worldIn, entity, weapon, item, modifiedGun) -> new HardenedBulletProjectileEntity(
                        (EntityType<? extends Entity>)ModEntities.HARDENED_BULLET_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun
                     )
               );
            ProjectileManager.getInstance()
               .registerFactory(
                  (Item)ModItems.NEEDLE.get(),
                  (worldIn, entity, weapon, item, modifiedGun) -> new NeedleProjectileEntity(
                        (EntityType<? extends Entity>)ModEntities.NEEDLE_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun
                     )
               );
            ProjectileManager.getInstance()
               .registerFactory(
                  (Item)ModItems.FLECHETTE.get(),
                  (worldIn, entity, weapon, item, modifiedGun) -> new FlechetteProjectileEntity(
                        (EntityType<? extends Entity>)ModEntities.FLECHETTE_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun
                     )
               );
            ProjectileManager.getInstance()
               .registerFactory(
                  (Item)ModItems.BUCKSHOT.get(),
                  (worldIn, entity, weapon, item, modifiedGun) -> new BuckshotProjectileEntity(
                        (EntityType<? extends Entity>)ModEntities.BUCKSHOT_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun
                     )
               );
            ProjectileManager.getInstance()
               .registerFactory(
                  (Item)ModItems.NITRO_BUCKSHOT.get(),
                  (worldIn, entity, weapon, item, modifiedGun) -> new BuckshotProjectileEntity(
                        (EntityType<? extends Entity>)ModEntities.BUCKSHOT_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun
                     )
               );
            ProjectileManager.getInstance()
               .registerFactory(
                  (Item)ModItems.SHOTBALL.get(),
                  (worldIn, entity, weapon, item, modifiedGun) -> new ShotballProjectileEntity(
                        (EntityType<? extends Entity>)ModEntities.SHOTBALL_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun
                     )
               );
            ProjectileManager.getInstance()
               .registerFactory(
                  (Item)ModItems.ROCKET.get(),
                  (worldIn, entity, weapon, item, modifiedGun) -> new RocketEntity(
                        (EntityType<? extends ProjectileEntity>)ModEntities.ROCKET.get(), worldIn, entity, weapon, item, modifiedGun
                     )
               );
            ProjectileManager.getInstance()
               .registerFactory(
                  (Item)ModItems.MICROJET.get(),
                  (worldIn, entity, weapon, item, modifiedGun) -> new MicroJetEntity(
                        (EntityType<? extends ProjectileEntity>)ModEntities.MICROJET.get(), worldIn, entity, weapon, item, modifiedGun
                     )
               );
            ProjectileManager.getInstance()
               .registerFactory(
                  (Item)ModItems.GRENADE.get(),
                  (worldIn, entity, weapon, item, modifiedGun) -> new GrenadeEntity(
                        (EntityType<? extends ProjectileEntity>)ModEntities.GRENADE.get(), worldIn, entity, weapon, item, modifiedGun
                     )
               );
            ProjectileManager.getInstance()
               .registerFactory(
                  (Item)ModItems.FROG_DART.get(),
                  (worldIn, entity, weapon, item, modifiedGun) -> new FrogDartProjectileEntity(
                        (EntityType<? extends Entity>)ModEntities.FROG_DART_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun
                     )
               );
            ProjectileManager.getInstance()
               .registerFactory(
                  (Item)ModItems.HE_GRENADE_ROUND.get(),
                  (worldIn, entity, weapon, item, modifiedGun) -> new HeGrenadeRoundEntity(
                        (EntityType<? extends ProjectileEntity>)ModEntities.HE_GRENADE_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun
                     )
               );
            ProjectileManager.getInstance()
               .registerFactory(
                  (Item)ModItems.FIRE_GRENADE_ROUND.get(),
                  (worldIn, entity, weapon, item, modifiedGun) -> new FireGrenadeRoundEntity(
                        (EntityType<? extends ProjectileEntity>)ModEntities.FIRE_GRENADE_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun
                     )
               );
            ProjectileManager.getInstance()
               .registerFactory(
                  (Item)ModItems.GAS_GRENADE_ROUND.get(),
                  (worldIn, entity, weapon, item, modifiedGun) -> new GasGrenadeRoundEntity(
                        (EntityType<? extends ProjectileEntity>)ModEntities.GAS_GRENADE_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun
                     )
               );
            ProjectileManager.getInstance()
               .registerFactory(
                  (Item)ModItems.BOUNCY_GRENADE_ROUND.get(),
                  (worldIn, entity, weapon, item, modifiedGun) -> new BouncyGrenadeRoundEntity(
                        (EntityType<? extends ProjectileEntity>)ModEntities.BOUNCY_GRENADE_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun
                     )
               );
            if ((Boolean)Config.COMMON.gameplay.improvedHitboxes.get()) {
               NeoForge.EVENT_BUS.register(new BoundingBoxManager());
            }
         }
      );
   }

   public static boolean isDebugging() {
      return false;
   }
}
