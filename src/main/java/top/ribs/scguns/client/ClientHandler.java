package top.ribs.scguns.client;

import com.mrcrayfish.framework.api.client.FrameworkClientAPI;
import java.lang.reflect.Field;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.OptionsList;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.minecraft.client.gui.screens.options.MouseSettingsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.InputEvent.Key;
import net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional;
import net.neoforged.neoforge.client.event.ScreenEvent.Init.Post;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import top.ribs.scguns.ScorchedGuns;
import top.ribs.scguns.client.handler.AimingHandler;
import top.ribs.scguns.client.handler.BulletTrailRenderingHandler;
import top.ribs.scguns.client.handler.ControllerHandler;
import top.ribs.scguns.client.handler.CrosshairHandler;
import top.ribs.scguns.client.handler.EntityMuzzleFlashHandler;
import top.ribs.scguns.client.handler.GunRenderingHandler;
import top.ribs.scguns.client.handler.HUDRenderHandler;
import top.ribs.scguns.client.handler.MeleeAttackHandler;
import top.ribs.scguns.client.handler.PlayerModelHandler;
import top.ribs.scguns.client.handler.RecoilHandler;
import top.ribs.scguns.client.handler.ReloadHandler;
import top.ribs.scguns.client.handler.ShootingHandler;
import top.ribs.scguns.client.handler.SoundHandler;
import top.ribs.scguns.client.handler.TurretBulletTrailRenderingHandler;
import top.ribs.scguns.client.render.block.AutoTurretRenderer;
import top.ribs.scguns.client.render.block.BasicTurretRenderer;
import top.ribs.scguns.client.render.block.EnemyTurretRenderer;
import top.ribs.scguns.client.render.block.GunShelfRenderer;
import top.ribs.scguns.client.render.block.MaceratorRenderer;
import top.ribs.scguns.client.render.block.MechanicalPressRenderer;
import top.ribs.scguns.client.render.block.NitroKegRenderer;
import top.ribs.scguns.client.render.block.PolarGeneratorRenderer;
import top.ribs.scguns.client.render.block.PowderKegRenderer;
import top.ribs.scguns.client.render.block.PoweredMaceratorRenderer;
import top.ribs.scguns.client.render.block.PoweredMechanicalPressRenderer;
import top.ribs.scguns.client.render.block.ShotgunTurretRenderer;
import top.ribs.scguns.client.render.block.SniperTurretRenderer;
import top.ribs.scguns.client.render.curios.AirCanisterRenderer;
import top.ribs.scguns.client.render.curios.AmmoBoxRenderer;
import top.ribs.scguns.client.render.entity.TurretProjectileRenderer;
import top.ribs.scguns.client.render.gun.ModelOverrides;
import top.ribs.scguns.client.render.gun.model.ArcWorkerModel;
import top.ribs.scguns.client.render.gun.model.AstellaModel;
import top.ribs.scguns.client.render.gun.model.AuvtomagModel;
import top.ribs.scguns.client.render.gun.model.BaskerModel;
import top.ribs.scguns.client.render.gun.model.BigBoreModel;
import top.ribs.scguns.client.render.gun.model.BirdfeederModel;
import top.ribs.scguns.client.render.gun.model.BlasphemyModel;
import top.ribs.scguns.client.render.gun.model.BlooperModel;
import top.ribs.scguns.client.render.gun.model.BlunderbussModel;
import top.ribs.scguns.client.render.gun.model.BombLanceModel;
import top.ribs.scguns.client.render.gun.model.BoomstickModel;
import top.ribs.scguns.client.render.gun.model.BrawlerModel;
import top.ribs.scguns.client.render.gun.model.BruiserModel;
import top.ribs.scguns.client.render.gun.model.CallwellConversionModel;
import top.ribs.scguns.client.render.gun.model.CallwellModel;
import top.ribs.scguns.client.render.gun.model.CallwellTerminalModel;
import top.ribs.scguns.client.render.gun.model.CarapiceModel;
import top.ribs.scguns.client.render.gun.model.CogloaderModel;
import top.ribs.scguns.client.render.gun.model.CombatShotgunModel;
import top.ribs.scguns.client.render.gun.model.Cr4kMiningLaserModel;
import top.ribs.scguns.client.render.gun.model.CrusaderModel;
import top.ribs.scguns.client.render.gun.model.CycloneModel;
import top.ribs.scguns.client.render.gun.model.DarkMatterModel;
import top.ribs.scguns.client.render.gun.model.DefenderPistolModel;
import top.ribs.scguns.client.render.gun.model.DoubletModel;
import top.ribs.scguns.client.render.gun.model.DozierRLModel;
import top.ribs.scguns.client.render.gun.model.DrillConversionModel;
import top.ribs.scguns.client.render.gun.model.DrillModel;
import top.ribs.scguns.client.render.gun.model.EarthsCorpseModel;
import top.ribs.scguns.client.render.gun.model.Echoes2Model;
import top.ribs.scguns.client.render.gun.model.FencerCarabineModel;
import top.ribs.scguns.client.render.gun.model.FencerThumperModel;
import top.ribs.scguns.client.render.gun.model.FlayedGodModel;
import top.ribs.scguns.client.render.gun.model.FlintlockPistolModel;
import top.ribs.scguns.client.render.gun.model.FloundergatModel;
import top.ribs.scguns.client.render.gun.model.ForlornHopeModel;
import top.ribs.scguns.client.render.gun.model.FreyrModel;
import top.ribs.scguns.client.render.gun.model.GaleModel;
import top.ribs.scguns.client.render.gun.model.GattalerModel;
import top.ribs.scguns.client.render.gun.model.GaussRifleModel;
import top.ribs.scguns.client.render.gun.model.GrandleModel;
import top.ribs.scguns.client.render.gun.model.GrandleOgModel;
import top.ribs.scguns.client.render.gun.model.GreaserSmgModel;
import top.ribs.scguns.client.render.gun.model.GyrojetPistolModel;
import top.ribs.scguns.client.render.gun.model.HammerGlModel;
import top.ribs.scguns.client.render.gun.model.HandcannonPistolModel;
import top.ribs.scguns.client.render.gun.model.HomemakerModel;
import top.ribs.scguns.client.render.gun.model.HowlerConversionModel;
import top.ribs.scguns.client.render.gun.model.HowlerModel;
import top.ribs.scguns.client.render.gun.model.HullbreakerModel;
import top.ribs.scguns.client.render.gun.model.HyperbariaModel;
import top.ribs.scguns.client.render.gun.model.InertialModel;
import top.ribs.scguns.client.render.gun.model.InquisitorModel;
import top.ribs.scguns.client.render.gun.model.IronJavelinModel;
import top.ribs.scguns.client.render.gun.model.IronSpearModel;
import top.ribs.scguns.client.render.gun.model.JackhammerModel;
import top.ribs.scguns.client.render.gun.model.JrWristbreakerModel;
import top.ribs.scguns.client.render.gun.model.KalaskahModel;
import top.ribs.scguns.client.render.gun.model.Killer23Model;
import top.ribs.scguns.client.render.gun.model.KilnGunModel;
import top.ribs.scguns.client.render.gun.model.KrauserModel;
import top.ribs.scguns.client.render.gun.model.LaserMusketModel;
import top.ribs.scguns.client.render.gun.model.LibertasModel;
import top.ribs.scguns.client.render.gun.model.LlrDirectorModel;
import top.ribs.scguns.client.render.gun.model.LockewoodModel;
import top.ribs.scguns.client.render.gun.model.LocustModel;
import top.ribs.scguns.client.render.gun.model.LoneWonderModel;
import top.ribs.scguns.client.render.gun.model.LongarmModel;
import top.ribs.scguns.client.render.gun.model.M22WaltzModel;
import top.ribs.scguns.client.render.gun.model.M3CarabineModel;
import top.ribs.scguns.client.render.gun.model.M3MarksmanModel;
import top.ribs.scguns.client.render.gun.model.MakMkIIModel;
import top.ribs.scguns.client.render.gun.model.MakeshiftRifleModel;
import top.ribs.scguns.client.render.gun.model.MangalitsaModel;
import top.ribs.scguns.client.render.gun.model.MarlinModel;
import top.ribs.scguns.client.render.gun.model.Mas55Model;
import top.ribs.scguns.client.render.gun.model.MasPeddlerModel;
import top.ribs.scguns.client.render.gun.model.MicinaModel;
import top.ribs.scguns.client.render.gun.model.MinksyModel;
import top.ribs.scguns.client.render.gun.model.Mk43RifleModel;
import top.ribs.scguns.client.render.gun.model.MokovaModel;
import top.ribs.scguns.client.render.gun.model.MusketModel;
import top.ribs.scguns.client.render.gun.model.NailerModel;
import top.ribs.scguns.client.render.gun.model.NervepinchModel;
import top.ribs.scguns.client.render.gun.model.NewbornCystModel;
import top.ribs.scguns.client.render.gun.model.NiamiModel;
import top.ribs.scguns.client.render.gun.model.Osgood50Model;
import top.ribs.scguns.client.render.gun.model.PaxModel;
import top.ribs.scguns.client.render.gun.model.PlasgunModel;
import top.ribs.scguns.client.render.gun.model.PlasmabussModel;
import top.ribs.scguns.client.render.gun.model.PrimaMateriaModel;
import top.ribs.scguns.client.render.gun.model.PrushGunModel;
import top.ribs.scguns.client.render.gun.model.PulsarModel;
import top.ribs.scguns.client.render.gun.model.PyroclasticFlowModel;
import top.ribs.scguns.client.render.gun.model.RailworkerModel;
import top.ribs.scguns.client.render.gun.model.RatKingAndQueenModel;
import top.ribs.scguns.client.render.gun.model.RaygunModel;
import top.ribs.scguns.client.render.gun.model.RedRaydarModel;
import top.ribs.scguns.client.render.gun.model.RepeatingMusketModel;
import top.ribs.scguns.client.render.gun.model.RgJigsawModel;
import top.ribs.scguns.client.render.gun.model.RibsGloryModel;
import top.ribs.scguns.client.render.gun.model.RocketRifleModel;
import top.ribs.scguns.client.render.gun.model.RustyGnatModel;
import top.ribs.scguns.client.render.gun.model.SaketiniIronPortModel;
import top.ribs.scguns.client.render.gun.model.SaketiniModel;
import top.ribs.scguns.client.render.gun.model.ScrapperModel;
import top.ribs.scguns.client.render.gun.model.ScratchesModel;
import top.ribs.scguns.client.render.gun.model.SculkResonatorModel;
import top.ribs.scguns.client.render.gun.model.SequoiaModel;
import top.ribs.scguns.client.render.gun.model.ShardCullerModel;
import top.ribs.scguns.client.render.gun.model.ShellurkerModel;
import top.ribs.scguns.client.render.gun.model.SoulDrummerModel;
import top.ribs.scguns.client.render.gun.model.SpirulidaModel;
import top.ribs.scguns.client.render.gun.model.SpitfireModel;
import top.ribs.scguns.client.render.gun.model.SterilizerModel;
import top.ribs.scguns.client.render.gun.model.StiggModel;
import top.ribs.scguns.client.render.gun.model.StilettoModel;
import top.ribs.scguns.client.render.gun.model.SuperShotgunModel;
import top.ribs.scguns.client.render.gun.model.TerraIncognitaModel;
import top.ribs.scguns.client.render.gun.model.TeslockRifleModel;
import top.ribs.scguns.client.render.gun.model.ThunderheadModel;
import top.ribs.scguns.client.render.gun.model.TlRunnerModel;
import top.ribs.scguns.client.render.gun.model.TrenchurModel;
import top.ribs.scguns.client.render.gun.model.TriquetraModel;
import top.ribs.scguns.client.render.gun.model.TrottersModel;
import top.ribs.scguns.client.render.gun.model.TruantModel;
import top.ribs.scguns.client.render.gun.model.TurnpikeModel;
import top.ribs.scguns.client.render.gun.model.UltraKnightHawkModel;
import top.ribs.scguns.client.render.gun.model.UmaxPistolModel;
import top.ribs.scguns.client.render.gun.model.UppercutModel;
import top.ribs.scguns.client.render.gun.model.ValoraModel;
import top.ribs.scguns.client.render.gun.model.VenturiModel;
import top.ribs.scguns.client.render.gun.model.VulcanicRepeaterModel;
import top.ribs.scguns.client.render.gun.model.WaltzConversionModel;
import top.ribs.scguns.client.render.gun.model.WeevilModel;
import top.ribs.scguns.client.render.gun.model.WhispersModel;
import top.ribs.scguns.client.render.gun.model.WhistlerModel;
import top.ribs.scguns.client.render.gun.model.WhizzbangerModel;
import top.ribs.scguns.client.render.gun.model.WinnieMillendModel;
import top.ribs.scguns.client.render.gun.model.WinnieModel;
import top.ribs.scguns.client.render.gun.model.Zilk45Model;
import top.ribs.scguns.client.screen.AmmoModuleScreen;
import top.ribs.scguns.client.screen.AttachmentScreen;
import top.ribs.scguns.client.screen.AutoTurretScreen;
import top.ribs.scguns.client.screen.BasicTurretScreen;
import top.ribs.scguns.client.screen.CryoniterScreen;
import top.ribs.scguns.client.screen.ExoSuitScreen;
import top.ribs.scguns.client.screen.GunBenchScreen;
import top.ribs.scguns.client.screen.LightningBatteryScreen;
import top.ribs.scguns.client.screen.MaceratorScreen;
import top.ribs.scguns.client.screen.MechanicalPressScreen;
import top.ribs.scguns.client.screen.ModMenuTypes;
import top.ribs.scguns.client.screen.PolarGeneratorScreen;
import top.ribs.scguns.client.screen.PoweredMaceratorScreen;
import top.ribs.scguns.client.screen.PoweredMechanicalPressScreen;
import top.ribs.scguns.client.screen.ShellCatcherModuleScreen;
import top.ribs.scguns.client.screen.ShotgunTurretScreen;
import top.ribs.scguns.client.screen.SniperTurretScreen;
import top.ribs.scguns.client.screen.SupplyScampScreen;
import top.ribs.scguns.client.screen.ThermolithScreen;
import top.ribs.scguns.client.screen.VentCollectorScreen;
import top.ribs.scguns.client.util.PropertyHelper;
import top.ribs.scguns.debug.IEditorMenu;
import top.ribs.scguns.debug.client.screen.EditorScreen;
import top.ribs.scguns.entity.client.AdjudicatorRenderer;
import top.ribs.scguns.entity.client.BlundererRenderer;
import top.ribs.scguns.entity.client.CogKnightRenderer;
import top.ribs.scguns.entity.client.CogMinionRenderer;
import top.ribs.scguns.entity.client.DissidentRenderer;
import top.ribs.scguns.entity.client.EnemyProjectileRenderer;
import top.ribs.scguns.entity.client.FinforcerRenderer;
import top.ribs.scguns.entity.client.HiveRenderer;
import top.ribs.scguns.entity.client.HornlinRenderer;
import top.ribs.scguns.entity.client.MotherGhastRenderer;
import top.ribs.scguns.entity.client.PraetorRenderer;
import top.ribs.scguns.entity.client.RaidFlareRenderer;
import top.ribs.scguns.entity.client.ScampRocketRenderer;
import top.ribs.scguns.entity.client.ScampTankRenderer;
import top.ribs.scguns.entity.client.ScamplerRenderer;
import top.ribs.scguns.entity.client.SignalBeaconRenderer;
import top.ribs.scguns.entity.client.SkyCarrierRenderer;
import top.ribs.scguns.entity.client.SubjugatorRenderer;
import top.ribs.scguns.entity.client.SulfurGasCloudRenderer;
import top.ribs.scguns.entity.client.SulfurheadRenderer;
import top.ribs.scguns.entity.client.SupplyScampRenderer;
import top.ribs.scguns.entity.client.SwarmRenderer;
import top.ribs.scguns.entity.client.TheMerchantRenderer;
import top.ribs.scguns.entity.client.TraumaHookRenderer;
import top.ribs.scguns.entity.client.TraumaUnitRenderer;
import top.ribs.scguns.entity.client.ViventrumRenderer;
import top.ribs.scguns.entity.client.ZombifiedHornlinRenderer;
import top.ribs.scguns.entity.monster.BeaconProjectileEntity;
import top.ribs.scguns.init.ModBlockEntities;
import top.ribs.scguns.init.ModBlocks;
import top.ribs.scguns.init.ModContainers;
import top.ribs.scguns.init.ModEntities;
import top.ribs.scguns.init.ModItems;
import top.ribs.scguns.init.ModMuzzleFlashes;
import top.ribs.scguns.item.AmmoBoxItem;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.item.animated.ExoSuitItem;
import top.ribs.scguns.network.PacketHandler;
import top.ribs.scguns.network.message.C2SMessageAttachments;
import top.ribs.scguns.network.message.C2SMessageMeleeAttack;
import top.ribs.scguns.network.message.C2SMessageSwapAmmo;
import top.ribs.scguns.network.message.C2SMessageToggleExoSuitPower;
import top.ribs.scguns.network.message.C2SMessageUtilityAction;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

@EventBusSubscriber(
   modid = "scguns",
   value = {Dist.CLIENT}
)
public class ClientHandler {
   private static Field mouseOptionsField;
   private static long lastAmmoSwapTime = 0L;
   private static final long SWAP_COOLDOWN_MS = 1000L;

   public ClientHandler() {
      super();
   }

   public static void registerClientHandlers(IEventBus bus) {
      // 1.21: menu screens are registered through a mod bus event
      bus.addListener(ClientHandler::registerMenuScreens);
      FrameworkClientAPI.registerDataLoader(MetaLoader.getInstance());
      bus.addListener(KeyBinds::registerKeyMappings);
      bus.addListener(CrosshairHandler::onConfigReload);
      bus.addListener(ClientHandler::onRegisterReloadListener);
      bus.addListener(ClientHandler::registerAdditional);
      bus.addListener(ClientHandler::onClientSetup);
      // Gun bench recipe book: our own RecipeBookType/RecipeBookCategories (declared in
      // META-INF/enumextensions.json) are wired up to the recipes and tabs here.
      bus.addListener(top.ribs.scguns.client.recipebook.GunBenchRecipeBookCategories::onRegisterRecipeBookCategories);
      NeoForge.EVENT_BUS.register(HUDRenderHandler.class);
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent.Post event) {
      {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && mc.screen == null && KeyBinds.KEY_MELEE.consumeClick()) {
            ItemStack heldItem = mc.player.getMainHandItem();
            if (heldItem.getItem() instanceof GunItem && !MeleeAttackHandler.isMeleeOnCooldown(mc.player, heldItem)) {
               PacketHandler.getPlayChannel().sendToServer(new C2SMessageMeleeAttack());
            }
         }
      }
   }

   private static void onClientSetup(FMLClientSetupEvent event) {
      event.enqueueWork(
         () -> Minecraft.getInstance()
               .getTextureManager()
               .register(ResourceLocation.parse("textures/entity/beacon_beam.png"), new SimpleTexture(ResourceLocation.parse("textures/entity/beacon_beam.png")))
      );
      EntityRenderers.register((EntityType)ModEntities.PRIMED_POWDER_KEG.get(), PowderKegRenderer::new);
      EntityRenderers.register((EntityType)ModEntities.PRIMED_NITRO_KEG.get(), NitroKegRenderer::new);
      BlockEntityRenderers.register((BlockEntityType)ModBlockEntities.MACERATOR.get(), MaceratorRenderer::new);
      BlockEntityRenderers.register((BlockEntityType)ModBlockEntities.GUN_SHELF_BLOCK_ENTITY.get(), GunShelfRenderer::new);
      BlockEntityRenderers.register((BlockEntityType)ModBlockEntities.POWERED_MACERATOR.get(), PoweredMaceratorRenderer::new);
      BlockEntityRenderers.register((BlockEntityType)ModBlockEntities.MECHANICAL_PRESS.get(), MechanicalPressRenderer::new);
      BlockEntityRenderers.register((BlockEntityType)ModBlockEntities.BASIC_TURRET.get(), BasicTurretRenderer::new);
      BlockEntityRenderers.register((BlockEntityType)ModBlockEntities.ENEMY_TURRET.get(), EnemyTurretRenderer::new);
      BlockEntityRenderers.register((BlockEntityType)ModBlockEntities.AUTO_TURRET.get(), AutoTurretRenderer::new);
      BlockEntityRenderers.register((BlockEntityType)ModBlockEntities.SHOTGUN_TURRET.get(), ShotgunTurretRenderer::new);
      BlockEntityRenderers.register((BlockEntityType)ModBlockEntities.SNIPER_TURRET.get(), SniperTurretRenderer::new);
      BlockEntityRenderers.register((BlockEntityType)ModBlockEntities.POWERED_MECHANICAL_PRESS.get(), PoweredMechanicalPressRenderer::new);
      BlockEntityRenderers.register((BlockEntityType)ModBlockEntities.POLAR_GENERATOR.get(), PolarGeneratorRenderer::new);
      ItemBlockRenderTypes.setRenderLayer((Block)ModBlocks.PLASMA_LANTERN.get(), RenderType.cutout());
      ItemBlockRenderTypes.setRenderLayer((Block)ModBlocks.NITER_GLASS.get(), RenderType.translucent());
      ItemBlockRenderTypes.setRenderLayer((Block)ModBlocks.WHITE_NITER_GLASS.get(), RenderType.translucent());
      ItemBlockRenderTypes.setRenderLayer((Block)ModBlocks.RED_NITER_GLASS.get(), RenderType.translucent());
      ItemBlockRenderTypes.setRenderLayer((Block)ModBlocks.GREEN_NITER_GLASS.get(), RenderType.translucent());
      ItemBlockRenderTypes.setRenderLayer((Block)ModBlocks.BLUE_NITER_GLASS.get(), RenderType.translucent());
      ItemBlockRenderTypes.setRenderLayer((Block)ModBlocks.YELLOW_NITER_GLASS.get(), RenderType.translucent());
      ItemBlockRenderTypes.setRenderLayer((Block)ModBlocks.ORANGE_NITER_GLASS.get(), RenderType.translucent());
      ItemBlockRenderTypes.setRenderLayer((Block)ModBlocks.PURPLE_NITER_GLASS.get(), RenderType.translucent());
      ItemBlockRenderTypes.setRenderLayer((Block)ModBlocks.BLACK_NITER_GLASS.get(), RenderType.translucent());
      ItemBlockRenderTypes.setRenderLayer((Block)ModBlocks.PINK_NITER_GLASS.get(), RenderType.translucent());
      ItemBlockRenderTypes.setRenderLayer((Block)ModBlocks.BROWN_NITER_GLASS.get(), RenderType.translucent());
      ItemBlockRenderTypes.setRenderLayer((Block)ModBlocks.CYAN_NITER_GLASS.get(), RenderType.translucent());
      ItemBlockRenderTypes.setRenderLayer((Block)ModBlocks.LIGHT_BLUE_NITER_GLASS.get(), RenderType.translucent());
      ItemBlockRenderTypes.setRenderLayer((Block)ModBlocks.LIME_NITER_GLASS.get(), RenderType.translucent());
      ItemBlockRenderTypes.setRenderLayer((Block)ModBlocks.MAGENTA_NITER_GLASS.get(), RenderType.translucent());
      ItemBlockRenderTypes.setRenderLayer((Block)ModBlocks.GRAY_NITER_GLASS.get(), RenderType.translucent());
      ItemBlockRenderTypes.setRenderLayer((Block)ModBlocks.LIGHT_GRAY_NITER_GLASS.get(), RenderType.translucent());
      ItemBlockRenderTypes.setRenderLayer((Block)ModBlocks.CHARGED_AMETHYST_RELAY.get(), RenderType.cutout());
      ItemBlockRenderTypes.setRenderLayer((Block)ModBlocks.FAKE_SOUL_FIRE.get(), RenderType.cutout());
      registerAmmoCountProperty((Item)ModItems.PISTOL_AMMO_BOX.get());
      registerAmmoCountProperty((Item)ModItems.RIFLE_AMMO_BOX.get());
      registerAmmoCountProperty((Item)ModItems.SHOTGUN_AMMO_BOX.get());
      registerAmmoCountProperty((Item)ModItems.MAGNUM_AMMO_BOX.get());
      registerAmmoCountProperty((Item)ModItems.ENERGY_AMMO_BOX.get());
      registerAmmoCountProperty((Item)ModItems.ROCKET_AMMO_BOX.get());
      registerAmmoCountProperty((Item)ModItems.SPECIAL_AMMO_BOX.get());
      registerAmmoCountProperty((Item)ModItems.EMPTY_CASING_POUCH.get());
      registerAmmoCountProperty((Item)ModItems.DISHES_POUCH.get());
      registerAmmoCountProperty((Item)ModItems.ROCK_POUCH.get());
      registerAmmoCountProperty((Item)ModItems.CREATIVE_AMMO_BOX.get());
      // PlayerModelHandler is registered once, with the other client handlers at
      // the end of this method; a second registration here made both of its
      // @SubscribeEvent methods fire twice per event.
      EntityRenderers.register((EntityType)ModEntities.COG_MINION.get(), CogMinionRenderer::new);
      EntityRenderers.register((EntityType)ModEntities.COG_KNIGHT.get(), CogKnightRenderer::new);
      EntityRenderers.register((EntityType)ModEntities.SKY_CARRIER.get(), SkyCarrierRenderer::new);
      EntityRenderers.register((EntityType)ModEntities.SUPPLY_SCAMP.get(), SupplyScampRenderer::new);
      EntityRenderers.register((EntityType)ModEntities.HIVE.get(), HiveRenderer::new);
      EntityRenderers.register((EntityType)ModEntities.SWARM.get(), SwarmRenderer::new);
      EntityRenderers.register((EntityType)ModEntities.DISSIDENT.get(), DissidentRenderer::new);
      EntityRenderers.register((EntityType)ModEntities.VIVENTRUM.get(), ViventrumRenderer::new);
      EntityRenderers.register((EntityType)ModEntities.HORNLIN.get(), HornlinRenderer::new);
      EntityRenderers.register((EntityType)ModEntities.ZOMBIFIED_HORNLIN.get(), ZombifiedHornlinRenderer::new);
      EntityRenderers.register((EntityType)ModEntities.TRAUMA_UNIT.get(), TraumaUnitRenderer::new);
      EntityRenderers.register((EntityType)ModEntities.THE_MERCHANT.get(), TheMerchantRenderer::new);
      EntityRenderers.register((EntityType)ModEntities.SULFUR_GAS_CLOUD.get(), SulfurGasCloudRenderer::new);
      EntityRenderers.register((EntityType)ModEntities.BLUNDERER.get(), BlundererRenderer::new);
      EntityRenderers.register((EntityType)ModEntities.ADJUDICATOR.get(), AdjudicatorRenderer::new);
      EntityRenderers.register((EntityType)ModEntities.SUBJUGATOR.get(), SubjugatorRenderer::new);
      EntityRenderers.register((EntityType)ModEntities.SIGNAL_BEACON.get(), SignalBeaconRenderer::new);
      EntityRenderers.register((EntityType)ModEntities.ENEMY_PROJECTILE.get(), EnemyProjectileRenderer::new);
      EntityRenderers.register((EntityType)ModEntities.TRAUMA_HOOK.get(), TraumaHookRenderer::new);
      EntityRenderers.register((EntityType)ModEntities.SCAMP_TANK.get(), ScampTankRenderer::new);
      EntityRenderers.register((EntityType)ModEntities.SCAMP_ROCKET.get(), ScampRocketRenderer::new);
      EntityRenderers.register((EntityType)ModEntities.SCAMPLER.get(), ScamplerRenderer::new);
      EntityRenderers.register((EntityType)ModEntities.RAID_FLARE.get(), RaidFlareRenderer::new);
      EntityRenderers.register((EntityType)ModEntities.MOTHER_GHAST.get(), MotherGhastRenderer::new);
      EntityRenderers.register((EntityType)ModEntities.FINFORCER.get(), FinforcerRenderer::new);
      EntityRenderers.register((EntityType)ModEntities.PRAETOR.get(), PraetorRenderer::new);
      EntityRenderers.register((EntityType)ModEntities.SULFURHEAD.get(), SulfurheadRenderer::new);
      EntityRenderers.register((EntityType)ModEntities.BEACON_PROJECTILE.get(), context -> new EntityRenderer<BeaconProjectileEntity>(context) {
            public ResourceLocation getTextureLocation(BeaconProjectileEntity entity) {
               return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/beacon.png");
            }
         });
      EntityRenderers.register((EntityType)ModEntities.TURRET_PROJECTILE.get(), TurretProjectileRenderer::new);
      CuriosRendererRegistry.register((Item)ModItems.PISTOL_AMMO_BOX.get(), AmmoBoxRenderer::new);
      CuriosRendererRegistry.register((Item)ModItems.RIFLE_AMMO_BOX.get(), AmmoBoxRenderer::new);
      CuriosRendererRegistry.register((Item)ModItems.SHOTGUN_AMMO_BOX.get(), AmmoBoxRenderer::new);
      CuriosRendererRegistry.register((Item)ModItems.MAGNUM_AMMO_BOX.get(), AmmoBoxRenderer::new);
      CuriosRendererRegistry.register((Item)ModItems.ENERGY_AMMO_BOX.get(), AmmoBoxRenderer::new);
      CuriosRendererRegistry.register((Item)ModItems.ROCKET_AMMO_BOX.get(), AmmoBoxRenderer::new);
      CuriosRendererRegistry.register((Item)ModItems.SPECIAL_AMMO_BOX.get(), AmmoBoxRenderer::new);
      CuriosRendererRegistry.register((Item)ModItems.EMPTY_CASING_POUCH.get(), AmmoBoxRenderer::new);
      CuriosRendererRegistry.register((Item)ModItems.DISHES_POUCH.get(), AmmoBoxRenderer::new);
      CuriosRendererRegistry.register((Item)ModItems.ROCK_POUCH.get(), AmmoBoxRenderer::new);
      CuriosRendererRegistry.register((Item)ModItems.CREATIVE_AMMO_BOX.get(), AmmoBoxRenderer::new);
      CuriosRendererRegistry.register((Item)ModItems.AIR_CANISTER.get(), AirCanisterRenderer::new);
      CuriosRendererRegistry.register((Item)ModItems.REINFORCED_AIR_CANISTER.get(), AirCanisterRenderer::new);
      CuriosRendererRegistry.register((Item)ModItems.CREATIVE_AIR_CANISTER.get(), AirCanisterRenderer::new);
      event.enqueueWork(ModMuzzleFlashes::init);
      event.enqueueWork(ClientHandler::setup);
   }

   public static void setup() {
      NeoForge.EVENT_BUS.register(AimingHandler.get());
      NeoForge.EVENT_BUS.register(BulletTrailRenderingHandler.get());
      NeoForge.EVENT_BUS.register(TurretBulletTrailRenderingHandler.get());
      NeoForge.EVENT_BUS.register(CrosshairHandler.get());
      NeoForge.EVENT_BUS.register(GunRenderingHandler.get());
      NeoForge.EVENT_BUS.register(RecoilHandler.get());
      NeoForge.EVENT_BUS.register(ReloadHandler.get());
      NeoForge.EVENT_BUS.register(ShootingHandler.get());
      NeoForge.EVENT_BUS.register(SoundHandler.get());
      NeoForge.EVENT_BUS.register(new PlayerModelHandler());
      NeoForge.EVENT_BUS.register(new EntityMuzzleFlashHandler());
      if (ScorchedGuns.controllableLoaded) {
         ControllerHandler.init();
      }

      setupRenderLayers();
      registerModelOverrides();
   }

   private static void setupRenderLayers() {
   }

   private static void registerModelOverrides() {
      ModelOverrides.register((Item)ModItems.EARTHS_CORPSE.get(), new EarthsCorpseModel());
      ModelOverrides.register((Item)ModItems.FLAYED_GOD.get(), new FlayedGodModel());
      ModelOverrides.register((Item)ModItems.NERVEPINCH.get(), new NervepinchModel());
      ModelOverrides.register((Item)ModItems.RAT_KING_AND_QUEEN.get(), new RatKingAndQueenModel());
      ModelOverrides.register((Item)ModItems.LOCUST.get(), new LocustModel());
      ModelOverrides.register((Item)ModItems.STERILIZER.get(), new SterilizerModel());
      ModelOverrides.register((Item)ModItems.NEWBORN_CYST.get(), new NewbornCystModel());
      ModelOverrides.register((Item)ModItems.LONE_WONDER.get(), new LoneWonderModel());
      ModelOverrides.register((Item)ModItems.CARAPICE.get(), new CarapiceModel());
      ModelOverrides.register((Item)ModItems.SHELLURKER.get(), new ShellurkerModel());
      ModelOverrides.register((Item)ModItems.FENCER_CARABINE.get(), new FencerCarabineModel());
      ModelOverrides.register((Item)ModItems.FENCER_THUMPER.get(), new FencerThumperModel());
      ModelOverrides.register((Item)ModItems.ECHOES_2.get(), new Echoes2Model());
      ModelOverrides.register((Item)ModItems.RAYGUN.get(), new RaygunModel());
      ModelOverrides.register((Item)ModItems.SCULK_RESONATOR.get(), new SculkResonatorModel());
      ModelOverrides.register((Item)ModItems.BLASPHEMY.get(), new BlasphemyModel());
      ModelOverrides.register((Item)ModItems.WHISPERS.get(), new WhispersModel());
      ModelOverrides.register((Item)ModItems.PYROCLASTIC_FLOW.get(), new PyroclasticFlowModel());
      ModelOverrides.register((Item)ModItems.FREYR.get(), new FreyrModel());
      ModelOverrides.register((Item)ModItems.VULCANIC_REPEATER.get(), new VulcanicRepeaterModel());
      ModelOverrides.register((Item)ModItems.SCRATCHES.get(), new ScratchesModel());
      ModelOverrides.register((Item)ModItems.OSGOOD_50.get(), new Osgood50Model());
      ModelOverrides.register((Item)ModItems.KILN_GUN.get(), new KilnGunModel());
      ModelOverrides.register((Item)ModItems.GALE.get(), new GaleModel());
      ModelOverrides.register((Item)ModItems.WALTZ_CONVERSION.get(), new WaltzConversionModel());
      ModelOverrides.register((Item)ModItems.UMAX_PISTOL.get(), new UmaxPistolModel());
      ModelOverrides.register((Item)ModItems.SPITFIRE.get(), new SpitfireModel());
      ModelOverrides.register((Item)ModItems.SHARD_CULLER.get(), new ShardCullerModel());
      ModelOverrides.register((Item)ModItems.GATTALER.get(), new GattalerModel());
      ModelOverrides.register((Item)ModItems.CR4K_MINING_LASER.get(), new Cr4kMiningLaserModel());
      ModelOverrides.register((Item)ModItems.THUNDERHEAD.get(), new ThunderheadModel());
      ModelOverrides.register((Item)ModItems.GYROJET_PISTOL.get(), new GyrojetPistolModel());
      ModelOverrides.register((Item)ModItems.DARK_MATTER.get(), new DarkMatterModel());
      ModelOverrides.register((Item)ModItems.DOZIER_RL.get(), new DozierRLModel());
      ModelOverrides.register((Item)ModItems.SUPER_SHOTGUN.get(), new SuperShotgunModel());
      ModelOverrides.register((Item)ModItems.BOMB_LANCE.get(), new BombLanceModel());
      ModelOverrides.register((Item)ModItems.VENTURI.get(), new VenturiModel());
      ModelOverrides.register((Item)ModItems.RED_RAYDAR.get(), new RedRaydarModel());
      ModelOverrides.register((Item)ModItems.MK43_RIFLE.get(), new Mk43RifleModel());
      ModelOverrides.register((Item)ModItems.PLASGUN.get(), new PlasgunModel());
      ModelOverrides.register((Item)ModItems.REPEATING_MUSKET.get(), new RepeatingMusketModel());
      ModelOverrides.register((Item)ModItems.ULTRA_KNIGHT_HAWK.get(), new UltraKnightHawkModel());
      ModelOverrides.register((Item)ModItems.SEQUOIA.get(), new SequoiaModel());
      ModelOverrides.register((Item)ModItems.LASER_MUSKET.get(), new LaserMusketModel());
      ModelOverrides.register((Item)ModItems.PLASMABUSS.get(), new PlasmabussModel());
      ModelOverrides.register((Item)ModItems.JACKHAMMER.get(), new JackhammerModel());
      ModelOverrides.register((Item)ModItems.JR_WRISTBREAKER.get(), new JrWristbreakerModel());
      ModelOverrides.register((Item)ModItems.KILLER_23.get(), new Killer23Model());
      ModelOverrides.register((Item)ModItems.HOMEMAKER.get(), new HomemakerModel());
      ModelOverrides.register((Item)ModItems.RIBS_GLORY.get(), new RibsGloryModel());
      ModelOverrides.register((Item)ModItems.PAX.get(), new PaxModel());
      ModelOverrides.register((Item)ModItems.PULSAR.get(), new PulsarModel());
      ModelOverrides.register((Item)ModItems.HOWLER.get(), new HowlerModel());
      ModelOverrides.register((Item)ModItems.HOWLER_CONVERSION.get(), new HowlerConversionModel());
      ModelOverrides.register((Item)ModItems.BIG_BORE.get(), new BigBoreModel());
      ModelOverrides.register((Item)ModItems.ARC_WORKER.get(), new ArcWorkerModel());
      ModelOverrides.register((Item)ModItems.FLINTLOCK_PISTOL.get(), new FlintlockPistolModel());
      ModelOverrides.register((Item)ModItems.HANDCANNON.get(), new HandcannonPistolModel());
      ModelOverrides.register((Item)ModItems.MUSKET.get(), new MusketModel());
      ModelOverrides.register((Item)ModItems.BLUNDERBUSS.get(), new BlunderbussModel());
      ModelOverrides.register((Item)ModItems.LONGARM.get(), new LongarmModel());
      ModelOverrides.register((Item)ModItems.DOUBLET.get(), new DoubletModel());
      ModelOverrides.register((Item)ModItems.LIBERTAS.get(), new LibertasModel());
      ModelOverrides.register((Item)ModItems.TESLOCK_RIFLE.get(), new TeslockRifleModel());
      ModelOverrides.register((Item)ModItems.ZILK_45.get(), new Zilk45Model());
      ModelOverrides.register((Item)ModItems.SPIRULIDA.get(), new SpirulidaModel());
      ModelOverrides.register((Item)ModItems.WHISTLER.get(), new WhistlerModel());
      ModelOverrides.register((Item)ModItems.ASTELLA.get(), new AstellaModel());
      ModelOverrides.register((Item)ModItems.BRAWLER.get(), new BrawlerModel());
      ModelOverrides.register((Item)ModItems.FLOUNDERGAT.get(), new FloundergatModel());
      ModelOverrides.register((Item)ModItems.HULLBREAKER.get(), new HullbreakerModel());
      ModelOverrides.register((Item)ModItems.SAKETINI.get(), new SaketiniModel());
      ModelOverrides.register((Item)ModItems.SAKETINI_IRONPORT.get(), new SaketiniIronPortModel());
      ModelOverrides.register((Item)ModItems.CALLWELL.get(), new CallwellModel());
      ModelOverrides.register((Item)ModItems.WHIZZBANGER.get(), new WhizzbangerModel());
      ModelOverrides.register((Item)ModItems.WINNIE.get(), new WinnieModel());
      ModelOverrides.register((Item)ModItems.WINNIE_MILLEND.get(), new WinnieMillendModel());
      ModelOverrides.register((Item)ModItems.SCRAPPER.get(), new ScrapperModel());
      ModelOverrides.register((Item)ModItems.MAKESHIFT_RIFLE.get(), new MakeshiftRifleModel());
      ModelOverrides.register((Item)ModItems.BOOMSTICK.get(), new BoomstickModel());
      ModelOverrides.register((Item)ModItems.RUSTY_GNAT.get(), new RustyGnatModel());
      ModelOverrides.register((Item)ModItems.BRUISER.get(), new BruiserModel());
      ModelOverrides.register((Item)ModItems.LLR_DIRECTOR.get(), new LlrDirectorModel());
      ModelOverrides.register((Item)ModItems.BIRDFEEDER.get(), new BirdfeederModel());
      ModelOverrides.register((Item)ModItems.NAILER.get(), new NailerModel());
      ModelOverrides.register((Item)ModItems.TURNPIKE.get(), new TurnpikeModel());
      ModelOverrides.register((Item)ModItems.TRUANT.get(), new TruantModel());
      ModelOverrides.register((Item)ModItems.HAMMER_GL.get(), new HammerGlModel());
      ModelOverrides.register((Item)ModItems.HYPERBARIA.get(), new HyperbariaModel());
      ModelOverrides.register((Item)ModItems.BLOOPER.get(), new BlooperModel());
      ModelOverrides.register((Item)ModItems.TRIQUETRA.get(), new TriquetraModel());
      ModelOverrides.register((Item)ModItems.BASKER.get(), new BaskerModel());
      ModelOverrides.register((Item)ModItems.WEEVIL.get(), new WeevilModel());
      ModelOverrides.register((Item)ModItems.TL_RUNNER.get(), new TlRunnerModel());
      ModelOverrides.register((Item)ModItems.KALASKAH.get(), new KalaskahModel());
      ModelOverrides.register((Item)ModItems.MOKOVA.get(), new MokovaModel());
      ModelOverrides.register((Item)ModItems.MAK_MKII.get(), new MakMkIIModel());
      ModelOverrides.register((Item)ModItems.STIGG.get(), new StiggModel());
      ModelOverrides.register((Item)ModItems.TERRA_INCOGNITA.get(), new TerraIncognitaModel());
      ModelOverrides.register((Item)ModItems.MARLIN.get(), new MarlinModel());
      ModelOverrides.register((Item)ModItems.IRON_SPEAR.get(), new IronSpearModel());
      ModelOverrides.register((Item)ModItems.IRON_JAVELIN.get(), new IronJavelinModel());
      ModelOverrides.register((Item)ModItems.M3_CARABINE.get(), new M3CarabineModel());
      ModelOverrides.register((Item)ModItems.M3_MARKSMAN.get(), new M3MarksmanModel());
      ModelOverrides.register((Item)ModItems.STILETTO.get(), new StilettoModel());
      ModelOverrides.register((Item)ModItems.LOCKEWOOD.get(), new LockewoodModel());
      ModelOverrides.register((Item)ModItems.RG_JIGSAW.get(), new RgJigsawModel());
      ModelOverrides.register((Item)ModItems.GREASER_SMG.get(), new GreaserSmgModel());
      ModelOverrides.register((Item)ModItems.DRILL.get(), new DrillModel());
      ModelOverrides.register((Item)ModItems.DRILL_CONVERSION.get(), new DrillConversionModel());
      ModelOverrides.register((Item)ModItems.DEFENDER_PISTOL.get(), new DefenderPistolModel());
      ModelOverrides.register((Item)ModItems.COMBAT_SHOTGUN.get(), new CombatShotgunModel());
      ModelOverrides.register((Item)ModItems.AUVTOMAG.get(), new AuvtomagModel());
      ModelOverrides.register((Item)ModItems.GAUSS_RIFLE.get(), new GaussRifleModel());
      ModelOverrides.register((Item)ModItems.ROCKET_RIFLE.get(), new RocketRifleModel());
      ModelOverrides.register((Item)ModItems.PRUSH_GUN.get(), new PrushGunModel());
      ModelOverrides.register((Item)ModItems.RAILWORKER.get(), new RailworkerModel());
      ModelOverrides.register((Item)ModItems.INERTIAL.get(), new InertialModel());
      ModelOverrides.register((Item)ModItems.INQUISITOR.get(), new InquisitorModel());
      ModelOverrides.register((Item)ModItems.COGLOADER.get(), new CogloaderModel());
      ModelOverrides.register((Item)ModItems.GRANDLE.get(), new GrandleModel());
      ModelOverrides.register((Item)ModItems.GRANDLE_OG.get(), new GrandleOgModel());
      ModelOverrides.register((Item)ModItems.UPPERCUT.get(), new UppercutModel());
      ModelOverrides.register((Item)ModItems.MAS_55.get(), new Mas55Model());
      ModelOverrides.register((Item)ModItems.MAS_PEDDLER.get(), new MasPeddlerModel());
      ModelOverrides.register((Item)ModItems.CYCLONE.get(), new CycloneModel());
      ModelOverrides.register((Item)ModItems.SOUL_DRUMMER.get(), new SoulDrummerModel());
      ModelOverrides.register((Item)ModItems.VALORA.get(), new ValoraModel());
      ModelOverrides.register((Item)ModItems.KRAUSER.get(), new KrauserModel());
      ModelOverrides.register((Item)ModItems.M22_WALTZ.get(), new M22WaltzModel());
      ModelOverrides.register((Item)ModItems.TRENCHUR.get(), new TrenchurModel());
      ModelOverrides.register((Item)ModItems.MICINA.get(), new MicinaModel());
      ModelOverrides.register((Item)ModItems.MINKSY.get(), new MinksyModel());
      ModelOverrides.register((Item)ModItems.MANGALITSA.get(), new MangalitsaModel());
      ModelOverrides.register((Item)ModItems.NIAMI.get(), new NiamiModel());
      ModelOverrides.register((Item)ModItems.CRUSADER.get(), new CrusaderModel());
      ModelOverrides.register((Item)ModItems.CALLWELL_CONVERSION.get(), new CallwellConversionModel());
      ModelOverrides.register((Item)ModItems.CALLWELL_TERMINAL.get(), new CallwellTerminalModel());
      ModelOverrides.register((Item)ModItems.FORLORN_HOPE.get(), new ForlornHopeModel());
      ModelOverrides.register((Item)ModItems.PRIMA_MATERIA.get(), new PrimaMateriaModel());
      ModelOverrides.register((Item)ModItems.TROTTERS.get(), new TrottersModel());
   }

   @SubscribeEvent
   public static void onScreenInit(Post event) {
      if (event.getScreen() instanceof MouseSettingsScreen) {
         MouseSettingsScreen screen = (MouseSettingsScreen)event.getScreen();
         if (mouseOptionsField == null) {
            mouseOptionsField = ObfuscationReflectionHelper.findField(MouseSettingsScreen.class, "list");
            mouseOptionsField.setAccessible(true);
         }

         try {
            OptionsList e = (OptionsList)mouseOptionsField.get(screen);
         } catch (IllegalAccessException var3) {
            var3.printStackTrace();
         }
      }
   }

   @SubscribeEvent
   public static void onKeyPressed(Key event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.screen == null && event.getAction() == 1) {
         if (KeyBinds.KEY_ATTACHMENTS.isDown()) {
            PacketHandler.getPlayChannel().sendToServer(new C2SMessageAttachments());
         }

         if (KeyBinds.KEY_SWAP_AMMO.isDown()) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastAmmoSwapTime >= 1000L) {
               lastAmmoSwapTime = currentTime;
               PacketHandler.getPlayChannel().sendToServer(new C2SMessageSwapAmmo());
            }
         }

         if (hasAnyExoSuitEquipped(mc.player)) {
            if (KeyBinds.KEY_ENABLE_EXO_HELMET.consumeClick()) {
               PacketHandler.getPlayChannel().sendToServer(new C2SMessageToggleExoSuitPower(C2SMessageToggleExoSuitPower.PowerType.HELMET_HUD));
            }

            if (KeyBinds.KEY_ENABLE_EXO_BOOTS.consumeClick()) {
               PacketHandler.getPlayChannel().sendToServer(new C2SMessageToggleExoSuitPower(C2SMessageToggleExoSuitPower.PowerType.BOOTS_MOBILITY));
            }

            if (KeyBinds.KEY_ENABLE_EXO_CHESTPLATE.consumeClick()) {
               PacketHandler.getPlayChannel().sendToServer(new C2SMessageUtilityAction());
            }
         }
      }
   }

   private static boolean hasAnyExoSuitEquipped(Player player) {
      for (ItemStack armorStack : player.getArmorSlots()) {
         if (armorStack.getItem() instanceof ExoSuitItem) {
            return true;
         }
      }

      return false;
   }

   public static void onRegisterReloadListener(RegisterClientReloadListenersEvent event) {
      event.registerReloadListener((ResourceManagerReloadListener)manager -> PropertyHelper.resetCache());
   }

   public static void registerAdditional(RegisterAdditional event) {
   }

   public static Screen createEditorScreen(IEditorMenu menu) {
      return new EditorScreen(Minecraft.getInstance().screen, menu);
   }

   private static void registerAmmoCountProperty(Item item) {
      ItemProperties.register(item, ResourceLocation.parse("ammo_count"), (stack, world, entity, seed) -> {
         int totalItemCount = AmmoBoxItem.getTotalItemCount(stack);
         int maxItemCount = AmmoBoxItem.getMaxItemCount(stack);
         if (totalItemCount == 0) {
            return 0.0F;
         } else if (totalItemCount <= maxItemCount / 3) {
            return 0.33F;
         } else {
            return totalItemCount <= 2 * maxItemCount / 3 ? 0.66F : 1.0F;
         }
      });
   }

   /**
    * 1.21 made MenuScreens.register private; NeoForge exposes the
    * RegisterMenuScreensEvent for mods to hook into instead. The 1.20.1
    * {@code registerScreenFactories()} helper that called it is gone: the
    * attachment screen is registered here with every other mod screen.
    */
   public static void registerMenuScreens(RegisterMenuScreensEvent event) {
      event.register(ModMenuTypes.MACERATOR_MENU.get(), MaceratorScreen::new);
      event.register(ModMenuTypes.POWERED_MACERATOR_MENU.get(), PoweredMaceratorScreen::new);
      event.register(ModMenuTypes.LIGHTING_BATTERY_MENU.get(), LightningBatteryScreen::new);
      event.register(ModMenuTypes.SUPPLY_SCAMP_MENU.get(), SupplyScampScreen::new);
      event.register(ModMenuTypes.SHELL_CATCHER_MODULE.get(), ShellCatcherModuleScreen::new);
      event.register(ModMenuTypes.AMMO_MODULE.get(), AmmoModuleScreen::new);
      event.register(ModMenuTypes.BASIC_TURRET_MENU.get(), BasicTurretScreen::new);
      event.register(ModMenuTypes.AUTO_TURRET_MENU.get(), AutoTurretScreen::new);
      event.register(ModMenuTypes.SNIPER_TURRET_MENU.get(), SniperTurretScreen::new);
      event.register(ModMenuTypes.SHOTGUN_TURRET_MENU.get(), ShotgunTurretScreen::new);
      event.register(ModMenuTypes.VENT_COLLECTOR_MENU.get(), VentCollectorScreen::new);
      event.register(ModMenuTypes.MECHANICAL_PRESS_MENU.get(), MechanicalPressScreen::new);
      event.register(ModMenuTypes.POWERED_MECHANICAL_PRESS_MENU.get(), PoweredMechanicalPressScreen::new);
      event.register(ModMenuTypes.POLAR_GENERATOR_MENU.get(), PolarGeneratorScreen::new);
      event.register(ModMenuTypes.EXOSUIT_MENU.get(), ExoSuitScreen::new);
      event.register(ModMenuTypes.CRYONITER_MENU.get(), CryoniterScreen::new);
      event.register(ModMenuTypes.THERMOLITH_MENU.get(), ThermolithScreen::new);
      event.register(ModMenuTypes.GUN_BENCH.get(), GunBenchScreen::new);
      event.register(ModContainers.ATTACHMENTS.get(), AttachmentScreen::new);
   }
}
