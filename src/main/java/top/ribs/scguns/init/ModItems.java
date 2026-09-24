package top.ribs.scguns.init;


import net.minecraft.core.registries.BuiltInRegistries;
import java.lang.reflect.Constructor;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.Item.Properties;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import top.ribs.scguns.ScorchedGuns;
import top.ribs.scguns.common.Attachments;
import top.ribs.scguns.common.GunModifiers;
import top.ribs.scguns.item.AirCanisterItem;
import top.ribs.scguns.item.AmmoItem;
import top.ribs.scguns.item.AnthraliteHammerItem;
import top.ribs.scguns.item.AnthralitePaxelItem;
import top.ribs.scguns.item.BarrelItem;
import top.ribs.scguns.item.BatGuanoItem;
import top.ribs.scguns.item.BayonetItem;
import top.ribs.scguns.item.BeaconGrenadeItem;
import top.ribs.scguns.item.BlueprintItem;
import top.ribs.scguns.item.ChokeBombItem;
import top.ribs.scguns.item.CogLocatorItem;
import top.ribs.scguns.item.CogMaceItem;
import top.ribs.scguns.item.ColdPackItem;
import top.ribs.scguns.item.CreativeAirCanisterItem;
import top.ribs.scguns.item.DepletedDiamondSteelItem;
import top.ribs.scguns.item.EmptyBlasphemyItem;
import top.ribs.scguns.item.EnemyLogItem;
import top.ribs.scguns.item.ExtendedBarrelItem;
import top.ribs.scguns.item.FlarePistolItem;
import top.ribs.scguns.item.FuelAmmoItem;
import top.ribs.scguns.item.FuelItem;
import top.ribs.scguns.item.GasGrenadeItem;
import top.ribs.scguns.item.GlintedBlueprintItem;
import top.ribs.scguns.item.GlintedHealingBandageItem;
import top.ribs.scguns.item.GrenadeItem;
import top.ribs.scguns.item.HealingBandageItem;
import top.ribs.scguns.item.HellfireBombItem;
import top.ribs.scguns.item.LaserSightItem;
import top.ribs.scguns.item.MagazineItem;
import top.ribs.scguns.item.MetalDetectorItem;
import top.ribs.scguns.item.MoldItem;
import top.ribs.scguns.item.MolotovCocktailItem;
import top.ribs.scguns.item.NailBombItem;
import top.ribs.scguns.item.NetherStarFragmentItem;
import top.ribs.scguns.item.NiterDustItem;
import top.ribs.scguns.item.PhosphorItem;
import top.ribs.scguns.item.RaidFlareItem;
import top.ribs.scguns.item.RangeFinderItem;
import top.ribs.scguns.item.ScampPackageItem;
import top.ribs.scguns.item.ScopeItem;
import top.ribs.scguns.item.ScorchedItem;
import top.ribs.scguns.item.StockItem;
import top.ribs.scguns.item.StunGrenadeItem;
import top.ribs.scguns.item.SulfurDustItem;
import top.ribs.scguns.item.SwarmBombItem;
import top.ribs.scguns.item.TeamLogItem;
import top.ribs.scguns.item.ThePactItem;
import top.ribs.scguns.item.ThrowableShotballItem;
import top.ribs.scguns.item.TooltipAmmo;
import top.ribs.scguns.item.TooltipItem;
import top.ribs.scguns.item.UnderBarrelItem;
import top.ribs.scguns.item.ViventrumPackageItem;
import top.ribs.scguns.item.WaraxeItem;
import top.ribs.scguns.item.WeirdFleshItem;
import top.ribs.scguns.item.WhiteFlagItem;
import top.ribs.scguns.item.ammo_boxes.CreativeAmmoBoxItem;
import top.ribs.scguns.item.ammo_boxes.DishesPouch;
import top.ribs.scguns.item.ammo_boxes.EmptyCasingPouchItem;
import top.ribs.scguns.item.ammo_boxes.EnergyAmmoBoxItem;
import top.ribs.scguns.item.ammo_boxes.MagnumAmmoBoxItem;
import top.ribs.scguns.item.ammo_boxes.PistolAmmoBoxItem;
import top.ribs.scguns.item.ammo_boxes.RifleAmmoBoxItem;
import top.ribs.scguns.item.ammo_boxes.RockPouch;
import top.ribs.scguns.item.ammo_boxes.RocketAmmoBoxItem;
import top.ribs.scguns.item.ammo_boxes.ShotgunAmmoBoxItem;
import top.ribs.scguns.item.ammo_boxes.SpecialAmmoBoxItem;
import top.ribs.scguns.item.animated.AdrienArmorItem;
import top.ribs.scguns.item.animated.AnimatedAirGunItem;
import top.ribs.scguns.item.animated.AnimatedDiamondSteelAirGunItem;
import top.ribs.scguns.item.animated.AnimatedDiamondSteelGunItem;
import top.ribs.scguns.item.animated.AnimatedDiamondSteelUnderWaterGunItem;
import top.ribs.scguns.item.animated.AnimatedDualWieldGunItem;
import top.ribs.scguns.item.animated.AnimatedGunItem;
import top.ribs.scguns.item.animated.AnimatedScorchedGunItem;
import top.ribs.scguns.item.animated.AnimatedSculkGunItem;
import top.ribs.scguns.item.animated.AnimatedUnderWaterGunItem;
import top.ribs.scguns.item.animated.AnthraliteArmorItem;
import top.ribs.scguns.item.animated.AnthraliteGasMaskArmorItem;
import top.ribs.scguns.item.animated.BrassMaskArmorItem;
import top.ribs.scguns.item.animated.CogKnightArmorItem;
import top.ribs.scguns.item.animated.DiamondSteelArmorItem;
import top.ribs.scguns.item.animated.ExoSuitItem;
import top.ribs.scguns.item.animated.IronMaskArmorItem;
import top.ribs.scguns.item.animated.NetheriteGasMaskArmorItem;
import top.ribs.scguns.item.animated.NonUnderwaterAnimatedGunItem;
import top.ribs.scguns.item.animated.RedcoatArmorItem;
import top.ribs.scguns.item.animated.RidgetopArmorItem;
import top.ribs.scguns.item.animated.ScrapArmorItem;
import top.ribs.scguns.item.animated.TreatedBrassArmorItem;
import top.ribs.scguns.item.attachment.impl.Barrel;
import top.ribs.scguns.item.attachment.impl.Magazine;
import top.ribs.scguns.item.attachment.impl.Stock;
import top.ribs.scguns.item.attachment.impl.UnderBarrel;
import top.ribs.scguns.item.exosuit.DamageableUpgradeItem;
import top.ribs.scguns.item.exosuit.ExoSuitCoreItem;
import top.ribs.scguns.item.exosuit.GasMaskModuleItem;
import top.ribs.scguns.item.exosuit.JetpackModuleItem;
import top.ribs.scguns.item.exosuit.NightVisionModuleItem;
import top.ribs.scguns.item.exosuit.RabbitModuleItem;
import top.ribs.scguns.item.exosuit.RebreatherModuleItem;
import top.ribs.scguns.item.exosuit.TargetTrackerModuleItem;
import top.ribs.scguns.item.exosuit.UpgradeItem;

public class ModItems {
   public static final DeferredRegister<Item> REGISTER = DeferredRegister.create(BuiltInRegistries.ITEM, "scguns");
   public static DeferredHolder<Item, Item> ANTHRALITE_KNIFE;
   public static DeferredHolder<Item, Item> ANTHRALITE_HAMMER;
   public static DeferredHolder<Item, Item> ANTHRALITE_PAXEL;
   public static final DeferredHolder<Item, AnimatedGunItem> SCRATCHES = REGISTER.register(
      "scratches",
      () -> new AnimatedAirGunItem(
            new Properties().stacksTo(1).durability(1400),
            "scratches",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> GALE = REGISTER.register(
      "gale",
      () -> new AnimatedAirGunItem(
            new Properties().stacksTo(1).durability(1400),
            "gale",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> UMAX_PISTOL = REGISTER.register(
      "umax_pistol",
      () -> new AnimatedAirGunItem(
            new Properties().stacksTo(1).durability(400),
            "umax_pistol",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> VENTURI = REGISTER.register(
      "venturi",
      () -> new AnimatedAirGunItem(
            new Properties().stacksTo(1).durability(800),
            "venturi",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> RED_RAYDAR = REGISTER.register(
      "red_raydar",
      () -> new AnimatedAirGunItem(
            new Properties().stacksTo(1).durability(200),
            "red_raydar",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> M3_CARABINE = REGISTER.register(
      "m3_carabine",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(800),
            "m3_carabine",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> M3_MARKSMAN = REGISTER.register(
      "m3_marksman",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(800),
            "m3_marksman",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> MAKESHIFT_RIFLE = REGISTER.register(
      "makeshift_rifle",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(600),
            "makeshift_rifle",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedDiamondSteelGunItem> LOCKEWOOD = REGISTER.register(
      "lockewood",
      () -> new AnimatedDiamondSteelGunItem(
            new Properties().stacksTo(1).durability(1350),
            "lockewood",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedDiamondSteelUnderWaterGunItem> ZILK_45 = REGISTER.register(
      "zilk_45",
      () -> new AnimatedDiamondSteelUnderWaterGunItem(
            new Properties().stacksTo(1).durability(1350),
            "zilk_45",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedDiamondSteelGunItem> TRUANT = REGISTER.register(
      "truant",
      () -> new AnimatedDiamondSteelGunItem(
            new Properties().stacksTo(1).durability(1350),
            "truant",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedDiamondSteelGunItem> MICINA = REGISTER.register(
      "micina",
      () -> new AnimatedDiamondSteelGunItem(
            new Properties().stacksTo(1).durability(1350),
            "micina",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedDiamondSteelGunItem> MINKSY = REGISTER.register(
      "minksy",
      () -> new AnimatedDiamondSteelGunItem(
            new Properties().stacksTo(1).durability(1350),
            "minksy",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedDiamondSteelGunItem> RG_JIGSAW = REGISTER.register(
      "rg_jigsaw",
      () -> new AnimatedDiamondSteelGunItem(
            new Properties().stacksTo(1).durability(1350),
            "rg_jigsaw",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedDiamondSteelAirGunItem> NAILER = REGISTER.register(
      "nailer",
      () -> new AnimatedDiamondSteelAirGunItem(
            new Properties().stacksTo(1).durability(1400),
            "nailer",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> GRANDLE = REGISTER.register(
      "grandle",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(1400),
            "grandle",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> GRANDLE_OG = REGISTER.register(
      "grandle_og",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(1400),
            "grandle_og",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> DEFENDER_PISTOL = REGISTER.register(
      "defender_pistol",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(800),
            "defender_pistol",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> TRENCHUR = REGISTER.register(
      "trenchur",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(800),
            "trenchur",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> AUVTOMAG = REGISTER.register(
      "auvtomag",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(800),
            "auvtomag",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> GREASER_SMG = REGISTER.register(
      "greaser_smg",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(800),
            "greaser_smg",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> BOOMSTICK = REGISTER.register(
      "boomstick",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(600),
            "boomstick",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedDiamondSteelGunItem> INERTIAL = REGISTER.register(
      "inertial",
      () -> new AnimatedDiamondSteelGunItem(
            new Properties().stacksTo(1).durability(1350),
            "inertial",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedDiamondSteelGunItem> INQUISITOR = REGISTER.register(
      "inquisitor",
      () -> new AnimatedDiamondSteelGunItem(
            new Properties().stacksTo(1).durability(1350),
            "inquisitor",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> M22_WALTZ = REGISTER.register(
      "m22_waltz",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(1400),
            "m22_waltz",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedUnderWaterGunItem> FLOUNDERGAT = REGISTER.register(
      "floundergat",
      () -> new AnimatedUnderWaterGunItem(
            new Properties().stacksTo(1).durability(800),
            "floundergat",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedUnderWaterGunItem> SPIRULIDA = REGISTER.register(
      "spirulida",
      () -> new AnimatedUnderWaterGunItem(
            new Properties().stacksTo(1).durability(800),
            "spirulida",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedUnderWaterGunItem> HULLBREAKER = REGISTER.register(
      "hullbreaker",
      () -> new AnimatedUnderWaterGunItem(
            new Properties().stacksTo(1).durability(512),
            "hullbreaker",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.CANNON_RELOAD.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedDiamondSteelGunItem> KRAUSER = REGISTER.register(
      "krauser",
      () -> new AnimatedDiamondSteelGunItem(
            new Properties().stacksTo(1).durability(800),
            "krauser",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedDiamondSteelGunItem> UPPERCUT = REGISTER.register(
      "uppercut",
      () -> new AnimatedDiamondSteelGunItem(
            new Properties().stacksTo(1).durability(1350),
            "uppercut",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedDiamondSteelGunItem> PRUSH_GUN = REGISTER.register(
      "prush_gun",
      () -> new AnimatedDiamondSteelGunItem(
            new Properties().stacksTo(1).durability(1350),
            "prush_gun",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedDiamondSteelGunItem> SOUL_DRUMMER = REGISTER.register(
      "soul_drummer",
      () -> new AnimatedDiamondSteelGunItem(
            new Properties().stacksTo(1).durability(800),
            "soul_drummer",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedDiamondSteelGunItem> VALORA = REGISTER.register(
      "valora",
      () -> new AnimatedDiamondSteelGunItem(
            new Properties().stacksTo(1).durability(1350),
            "valora",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> RUSTY_GNAT = REGISTER.register(
      "rusty_gnat",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(600),
            "rusty_gnat",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> CALLWELL = REGISTER.register(
      "callwell",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(256),
            "callwell",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> CALLWELL_TERMINAL = REGISTER.register(
      "callwell_terminal",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(256),
            "callwell_terminal",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> CALLWELL_CONVERSION = REGISTER.register(
      "callwell_conversion",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(256),
            "callwell_conversion",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> COMBAT_SHOTGUN = REGISTER.register(
      "combat_shotgun",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(800),
            "combat_shotgun",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> TRIQUETRA = REGISTER.register(
      "triquetra",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(1400),
            "triquetra",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> FLINTLOCK_PISTOL = REGISTER.register(
      "flintlock_pistol",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(256),
            "flintlock_pistol",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, NonUnderwaterAnimatedGunItem> HANDCANNON = REGISTER.register(
      "handcannon",
      () -> new NonUnderwaterAnimatedGunItem(
            new Properties().stacksTo(1).durability(256),
            "handcannon",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, NonUnderwaterAnimatedGunItem> MUSKET = REGISTER.register(
      "musket",
      () -> new NonUnderwaterAnimatedGunItem(
            new Properties().stacksTo(1).durability(256),
            "musket",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, NonUnderwaterAnimatedGunItem> REPEATING_MUSKET = REGISTER.register(
      "repeating_musket",
      () -> new NonUnderwaterAnimatedGunItem(
            new Properties().stacksTo(1).durability(160),
            "repeating_musket",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, NonUnderwaterAnimatedGunItem> BLUNDERBUSS = REGISTER.register(
      "blunderbuss",
      () -> new NonUnderwaterAnimatedGunItem(
            new Properties().stacksTo(1).durability(256),
            "blunderbuss",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, NonUnderwaterAnimatedGunItem> LONGARM = REGISTER.register(
      "longarm",
      () -> new NonUnderwaterAnimatedGunItem(
            new Properties().stacksTo(1).durability(200),
            "longarm",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, NonUnderwaterAnimatedGunItem> FENCER_CARABINE = REGISTER.register(
      "fencer_carabine",
      () -> new NonUnderwaterAnimatedGunItem(
            new Properties().stacksTo(1).durability(180),
            "fencer_carabine",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, NonUnderwaterAnimatedGunItem> FENCER_THUMPER = REGISTER.register(
      "fencer_thumper",
      () -> new NonUnderwaterAnimatedGunItem(
            new Properties().stacksTo(1).durability(180),
            "fencer_thumper",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, NonUnderwaterAnimatedGunItem> DOUBLET = REGISTER.register(
      "doublet",
      () -> new NonUnderwaterAnimatedGunItem(
            new Properties().stacksTo(1).durability(256),
            "doublet",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedDiamondSteelGunItem> MAS_55 = REGISTER.register(
      "mas_55",
      () -> new AnimatedDiamondSteelGunItem(
            new Properties().stacksTo(1).durability(1350),
            "mas_55",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedDiamondSteelGunItem> MAS_PEDDLER = REGISTER.register(
      "mas_peddler",
      () -> new AnimatedDiamondSteelGunItem(
            new Properties().stacksTo(1).durability(1350),
            "mas_peddler",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> COGLOADER = REGISTER.register(
      "cogloader",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(1400),
            "cogloader",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> SAKETINI = REGISTER.register(
      "saketini",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(256),
            "saketini",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> KILN_GUN = REGISTER.register(
      "kiln_gun",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(200),
            "kiln_gun",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            SoundEvents.LEVER_CLICK,
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> SAKETINI_IRONPORT = REGISTER.register(
      "saketini_ironport",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(200),
            "saketini_ironport",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            SoundEvents.LEVER_CLICK,
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> SCRAPPER = REGISTER.register(
      "scrapper",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(600),
            "scrapper",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> BRAWLER = REGISTER.register(
      "brawler",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(800),
            "brawler",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> WINNIE = REGISTER.register(
      "winnie",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(256),
            "winnie",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> WINNIE_MILLEND = REGISTER.register(
      "winnie_millend",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(256),
            "winnie_millend",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> BRUISER = REGISTER.register(
      "bruiser",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(800),
            "bruiser",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedDiamondSteelGunItem> DRILL = REGISTER.register(
      "drill",
      () -> new AnimatedDiamondSteelGunItem(
            new Properties().stacksTo(1).durability(1200),
            "drill",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedDiamondSteelGunItem> DRILL_CONVERSION = REGISTER.register(
      "drill_conversion",
      () -> new AnimatedDiamondSteelGunItem(
            new Properties().stacksTo(1).durability(1200),
            "drill_conversion",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedDiamondSteelGunItem> CYCLONE = REGISTER.register(
      "cyclone",
      () -> new AnimatedDiamondSteelGunItem(
            new Properties().stacksTo(1).durability(1350),
            "cyclone",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedDiamondSteelGunItem> PLASGUN = REGISTER.register(
      "plasgun",
      () -> new AnimatedDiamondSteelGunItem(
            new Properties().stacksTo(1).durability(1350),
            "plasgun",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.HISS.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> ROCKET_RIFLE = REGISTER.register(
      "rocket_rifle",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(800),
            "rocket_rifle",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> MARLIN = REGISTER.register(
      "marlin",
      () -> new AnimatedUnderWaterGunItem(
            new Properties().stacksTo(1).durability(800),
            "marlin",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> GAUSS_RIFLE = REGISTER.register(
      "gauss_rifle",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(1400),
            "gauss_rifle",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.HISS.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> NIAMI = REGISTER.register(
      "niami",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(1400),
            "niami",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> IRON_SPEAR = REGISTER.register(
      "iron_spear",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(800),
            "iron_spear",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> IRON_JAVELIN = REGISTER.register(
      "iron_javelin",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(800),
            "iron_javelin",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> LLR_DIRECTOR = REGISTER.register(
      "llr_director",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(600),
            "llr_director",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> BIRDFEEDER = REGISTER.register(
      "birdfeeder",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(600),
            "birdfeeder",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> WHISTLER = REGISTER.register(
      "whistler",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(600),
            "whistler",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> BLOOPER = REGISTER.register(
      "blooper",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(600),
            "blooper",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> TURNPIKE = REGISTER.register(
      "turnpike",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(540),
            "turnpike",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> STILETTO = REGISTER.register(
      "stiletto",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(540),
            "stiletto",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedAirGunItem> RAILWORKER = REGISTER.register(
      "railworker",
      () -> new AnimatedAirGunItem(
            new Properties().stacksTo(1).durability(540),
            "railworker",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> BASKER = REGISTER.register(
      "basker",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(540),
            "basker",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> KALASKAH = REGISTER.register(
      "kalaskah",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(540),
            "kalaskah",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> MOKOVA = REGISTER.register(
      "mokova",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(540),
            "mokova",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> MAK_MKII = REGISTER.register(
      "mak_mkii",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(540),
            "mak_mkii",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> TL_RUNNER = REGISTER.register(
      "tl_runner",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(540),
            "tl_runner",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> KILLER_23 = REGISTER.register(
      "killer_23",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(540),
            "killer_23",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> HOMEMAKER = REGISTER.register(
      "homemaker",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(540),
            "homemaker",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> RIBS_GLORY = REGISTER.register(
      "ribs_glory",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(2400),
            "ribs_glory",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> STIGG = REGISTER.register(
      "stigg",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(540),
            "stigg",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> PAX = REGISTER.register(
      "pax",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(256),
            "pax",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> BIG_BORE = REGISTER.register(
      "big_bore",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(10),
            "big_bore",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> WHIZZBANGER = REGISTER.register(
      "whizzbanger",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(15),
            "whizzbanger",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> HOWLER = REGISTER.register(
      "howler",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(1400),
            "howler",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> HOWLER_CONVERSION = REGISTER.register(
      "howler_conversion",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(1400),
            "howler_conversion",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> PULSAR = REGISTER.register(
      "pulsar",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(256),
            "pulsar",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> ARC_WORKER = REGISTER.register(
      "arc_worker",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(600),
            "arc_worker",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> LASER_MUSKET = REGISTER.register(
      "laser_musket",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(200),
            "laser_musket",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> TESLOCK_RIFLE = REGISTER.register(
      "teslock_rifle",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(200),
            "teslock_rifle",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> PLASMABUSS = REGISTER.register(
      "plasmabuss",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(200),
            "plasmabuss",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> HAMMER_GL = REGISTER.register(
      "hammer_gl",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(1400),
            "hammer_gl",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> JACKHAMMER = REGISTER.register(
      "jackhammer",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(1400),
            "jackhammer",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> JR_WRISTBREAKER = REGISTER.register(
      "jr_wristbreaker",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(1400),
            "jr_wristbreaker",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> LIBERTAS = REGISTER.register(
      "libertas",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(1400),
            "libertas",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedUnderWaterGunItem> SEQUOIA = REGISTER.register(
      "sequoia",
      () -> new AnimatedUnderWaterGunItem(
            new Properties().stacksTo(1).durability(800),
            "sequoia",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedUnderWaterGunItem> HYPERBARIA = REGISTER.register(
      "hyperbaria",
      () -> new AnimatedUnderWaterGunItem(
            new Properties().stacksTo(1).durability(800),
            "hyperbaria",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> ULTRA_KNIGHT_HAWK = REGISTER.register(
      "ultra_knight_hawk",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(21),
            "ultra_knight_hawk",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> SUPER_SHOTGUN = REGISTER.register(
      "super_shotgun",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(800),
            "super_shotgun",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedUnderWaterGunItem> BOMB_LANCE = REGISTER.register(
      "bomb_lance",
      () -> new AnimatedUnderWaterGunItem(
            new Properties().stacksTo(1).durability(800),
            "bomb_lance",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> DOZIER_RL = REGISTER.register(
      "dozier_rl",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(512),
            "dozier_rl",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> DARK_MATTER = REGISTER.register(
      "dark_matter",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(1600),
            "dark_matter",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> MK43_RIFLE = REGISTER.register(
      "mk43_rifle",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(800),
            "mk43_rifle",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> CRUSADER = REGISTER.register(
      "crusader",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(256),
            "crusader",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> GYROJET_PISTOL = REGISTER.register(
      "gyrojet_pistol",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(800),
            "gyrojet_pistol",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> THUNDERHEAD = REGISTER.register(
      "thunderhead",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(600),
            "thunderhead",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> GATTALER = REGISTER.register(
      "gattaler",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(1000),
            "gattaler",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> CR4K_MINING_LASER = REGISTER.register(
      "cr4k_mining_laser",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(1900),
            "cr4k_mining_laser",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> SHARD_CULLER = REGISTER.register(
      "shard_culler",
      () -> new AnimatedDiamondSteelGunItem(
            new Properties().stacksTo(1).durability(1350),
            "shard_culler",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> SPITFIRE = REGISTER.register(
      "spitfire",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(1400),
            "spitfire",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> WALTZ_CONVERSION = REGISTER.register(
      "waltz_conversion",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(1400),
            "waltz_conversion",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> OSGOOD_50 = REGISTER.register(
      "osgood_50",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(1400),
            "osgood_50",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> FREYR = REGISTER.register(
      "freyr",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(1050),
            "freyr",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> VULCANIC_REPEATER = REGISTER.register(
      "vulcanic_repeater",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(1050),
            "vulcanic_repeater",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> PYROCLASTIC_FLOW = REGISTER.register(
      "pyroclastic_flow",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(1050),
            "pyroclastic_flow",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> MANGALITSA = REGISTER.register(
      "mangalitsa",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(1050),
            "mangalitsa",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> TROTTERS = REGISTER.register(
      "trotters",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(1050),
            "trotters",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> BLASPHEMY = REGISTER.register(
      "blasphemy",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(1050),
            "blasphemy",
            SoundEvents.GENERIC_DRINK,
            SoundEvents.PLAYER_BURP,
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedSculkGunItem> WHISPERS = REGISTER.register(
      "whispers",
      () -> new AnimatedSculkGunItem(
            new Properties().stacksTo(1).durability(1100),
            "whispers",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedSculkGunItem> SCULK_RESONATOR = REGISTER.register(
      "sculk_resonator",
      () -> new AnimatedSculkGunItem(
            new Properties().stacksTo(1).durability(1100),
            "sculk_resonator",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedSculkGunItem> ECHOES_2 = REGISTER.register(
      "echoes_2",
      () -> new AnimatedSculkGunItem(
            new Properties().stacksTo(1).durability(1100),
            "echoes_2",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedSculkGunItem> FORLORN_HOPE = REGISTER.register(
      "forlorn_hope",
      () -> new AnimatedSculkGunItem(
            new Properties().stacksTo(1).durability(1100),
            "forlorn_hope",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> RAYGUN = REGISTER.register(
      "raygun",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(1600),
            "raygun",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> CARAPICE = REGISTER.register(
      "carapice",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(1600),
            "carapice",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> SHELLURKER = REGISTER.register(
      "shellurker",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(1600),
            "shellurker",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> WEEVIL = REGISTER.register(
      "weevil",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(1600),
            "weevil",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> LONE_WONDER = REGISTER.register(
      "lone_wonder",
      () -> new AnimatedGunItem(
            new Properties().stacksTo(1).durability(1600),
            "lone_wonder",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedDualWieldGunItem> RAT_KING_AND_QUEEN = REGISTER.register(
      "rat_king_and_queen",
      () -> new AnimatedDualWieldGunItem(
            new Properties().stacksTo(1).durability(2400),
            "rat_king_and_queen",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> LOCUST = REGISTER.register(
      "locust",
      () -> new AnimatedScorchedGunItem(
            new Properties().stacksTo(1).durability(2400),
            "locust",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> STERILIZER = REGISTER.register(
      "sterilizer",
      () -> new AnimatedScorchedGunItem(
            new Properties().stacksTo(1).durability(2400),
            "sterilizer",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.HISS.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedGunItem> NEWBORN_CYST = REGISTER.register(
      "newborn_cyst",
      () -> new AnimatedScorchedGunItem(
            new Properties().stacksTo(1).durability(2400),
            "newborn_cyst",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedScorchedGunItem> ASTELLA = REGISTER.register(
      "astella",
      () -> new AnimatedScorchedGunItem(
            new Properties().stacksTo(1).durability(2400),
            "astella",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedScorchedGunItem> TERRA_INCOGNITA = REGISTER.register(
      "terra_incognita",
      () -> new AnimatedScorchedGunItem(
            new Properties().stacksTo(1).durability(2400),
            "terra_incognita",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            SoundEvents.PISTON_EXTEND
         )
   );
   public static final DeferredHolder<Item, AnimatedScorchedGunItem> PRIMA_MATERIA = REGISTER.register(
      "prima_materia",
      () -> new AnimatedScorchedGunItem(
            new Properties().stacksTo(1).durability(2400),
            "prima_materia",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedScorchedGunItem> NERVEPINCH = REGISTER.register(
      "nervepinch",
      () -> new AnimatedScorchedGunItem(
            new Properties().stacksTo(1).durability(2400),
            "nervepinch",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedScorchedGunItem> EARTHS_CORPSE = REGISTER.register(
      "earths_corpse",
      () -> new AnimatedScorchedGunItem(
            new Properties().stacksTo(1).durability(2400),
            "earths_corpse",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, AnimatedScorchedGunItem> FLAYED_GOD = REGISTER.register(
      "flayed_god",
      () -> new AnimatedScorchedGunItem(
            new Properties().stacksTo(1).durability(2400),
            "flayed_god",
            (SoundEvent)ModSounds.MAG_OUT.get(),
            (SoundEvent)ModSounds.MAG_IN.get(),
            (SoundEvent)ModSounds.RELOAD_END.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get(),
            (SoundEvent)ModSounds.COPPER_GUN_JAM.get()
         )
   );
   public static final DeferredHolder<Item, Item> WARAXE = REGISTER.register("war_axe", () -> new WaraxeItem(new Properties()));
   // 1.20.1 passed the damage and speed modifiers to the tool constructor
   // (PickaxeItem(Tier, int, float, Properties)). 1.21 moved them into the item's
   // ATTRIBUTE_MODIFIERS component, so a tool built with a bare Properties has NO attributes at
   // all - it hits like an empty hand. The numbers are the ones 0.5.5 used.
   public static final DeferredHolder<Item, PickaxeItem> ANTHRALITE_PICKAXE = REGISTER.register(
      "anthralite_pickaxe",
      () -> new PickaxeItem(ModTiers.ANTHRALITE, new Properties()
         .attributes(PickaxeItem.createAttributes(ModTiers.ANTHRALITE, 1, -2.8F)))
   );
   public static final DeferredHolder<Item, SwordItem> ANTHRALITE_SWORD = REGISTER.register(
      "anthralite_sword",
      () -> new SwordItem(ModTiers.ANTHRALITE, new Properties()
         .attributes(SwordItem.createAttributes(ModTiers.ANTHRALITE, 3, -2.4F)))
   );
   public static final DeferredHolder<Item, AxeItem> ANTHRALITE_AXE = REGISTER.register(
      "anthralite_axe",
      () -> new AxeItem(ModTiers.ANTHRALITE, new Properties()
         .attributes(AxeItem.createAttributes(ModTiers.ANTHRALITE, 5.0F, -3.0F)))
   );
   public static final DeferredHolder<Item, ShovelItem> ANTHRALITE_SHOVEL = REGISTER.register(
      "anthralite_shovel",
      () -> new ShovelItem(ModTiers.ANTHRALITE, new Properties()
         .attributes(ShovelItem.createAttributes(ModTiers.ANTHRALITE, 1.5F, -3.0F)))
   );
   public static final DeferredHolder<Item, HoeItem> ANTHRALITE_HOE = REGISTER.register(
      "anthralite_hoe",
      () -> new HoeItem(ModTiers.ANTHRALITE, new Properties()
         .attributes(HoeItem.createAttributes(ModTiers.ANTHRALITE, -3, -3.0F)))
   );
   public static final DeferredHolder<Item, CogMaceItem> COG_MACE = REGISTER.register(
      "cog_mace", () -> new CogMaceItem(ModTiers.ANCIENT_BRASS, 2, -3.2F, new Properties())
   );
   public static final DeferredHolder<Item, Item> TURRET_PLATFORM = REGISTER.register("turret_platform", () -> new Item(new Properties().stacksTo(16)));
   public static final DeferredHolder<Item, Item> METAL_DETECTOR = REGISTER.register(
      "metal_detector", () -> new MetalDetectorItem(new Properties().stacksTo(1).durability(128).rarity(Rarity.RARE))
   );
   public static final DeferredHolder<Item, Item> RANGE_FINDER = REGISTER.register("range_finder", () -> new RangeFinderItem(new Properties().stacksTo(1)));
   public static final DeferredHolder<Item, Item> ANTHRALITE_HELMET = REGISTER.register(
      "anthralite_helmet", () -> new AnthraliteArmorItem(ModArmorMaterials.ANTHRALITE, Type.HELMET, ModArmorMaterials.durability(new Properties(), ModArmorMaterials.ANTHRALITE, Type.HELMET))
   );
   public static final DeferredHolder<Item, Item> ANTHRALITE_CHESTPLATE = REGISTER.register(
      "anthralite_chestplate", () -> new AnthraliteArmorItem(ModArmorMaterials.ANTHRALITE, Type.CHESTPLATE, ModArmorMaterials.durability(new Properties(), ModArmorMaterials.ANTHRALITE, Type.CHESTPLATE))
   );
   public static final DeferredHolder<Item, Item> ANTHRALITE_LEGGINGS = REGISTER.register(
      "anthralite_leggings", () -> new AnthraliteArmorItem(ModArmorMaterials.ANTHRALITE, Type.LEGGINGS, ModArmorMaterials.durability(new Properties(), ModArmorMaterials.ANTHRALITE, Type.LEGGINGS))
   );
   public static final DeferredHolder<Item, Item> ANTHRALITE_BOOTS = REGISTER.register(
      "anthralite_boots", () -> new AnthraliteArmorItem(ModArmorMaterials.ANTHRALITE, Type.BOOTS, ModArmorMaterials.durability(new Properties(), ModArmorMaterials.ANTHRALITE, Type.BOOTS))
   );
   public static final DeferredHolder<Item, Item> RIDGETOP = REGISTER.register(
      "ridgetop", () -> new RidgetopArmorItem(ArmorMaterials.LEATHER, Type.HELMET, new Properties())
   );
   public static final DeferredHolder<Item, Item> BRASS_MASK = REGISTER.register(
      "brass_mask", () -> new BrassMaskArmorItem(ModArmorMaterials.TREATED_BRASS, Type.HELMET, ModArmorMaterials.durability(new Properties(), ModArmorMaterials.TREATED_BRASS, Type.HELMET))
   );
   public static final DeferredHolder<Item, Item> IRON_MASK = REGISTER.register(
      "iron_mask", () -> new IronMaskArmorItem(ModArmorMaterials.DIAMOND_STEEL, Type.HELMET, ModArmorMaterials.durability(new Properties(), ModArmorMaterials.DIAMOND_STEEL, Type.HELMET))
   );
   public static final DeferredHolder<Item, Item> ADRIEN_HELM = REGISTER.register(
      "adrien_helm", () -> new AdrienArmorItem(ModArmorMaterials.ADRIEN, Type.HELMET, ModArmorMaterials.durability(new Properties(), ModArmorMaterials.ADRIEN, Type.HELMET))
   );
   public static final DeferredHolder<Item, Item> ADRIEN_CHESTPLATE = REGISTER.register(
      "adrien_chestplate", () -> new AdrienArmorItem(ModArmorMaterials.ADRIEN, Type.CHESTPLATE, ModArmorMaterials.durability(new Properties(), ModArmorMaterials.ADRIEN, Type.CHESTPLATE))
   );
   public static final DeferredHolder<Item, Item> ADRIEN_LEGGINGS = REGISTER.register(
      "adrien_leggings", () -> new AdrienArmorItem(ModArmorMaterials.ADRIEN, Type.LEGGINGS, ModArmorMaterials.durability(new Properties(), ModArmorMaterials.ADRIEN, Type.LEGGINGS))
   );
   public static final DeferredHolder<Item, Item> ADRIEN_BOOTS = REGISTER.register(
      "adrien_boots", () -> new AdrienArmorItem(ModArmorMaterials.ADRIEN, Type.BOOTS, ModArmorMaterials.durability(new Properties(), ModArmorMaterials.ADRIEN, Type.BOOTS))
   );
   public static final DeferredHolder<Item, Item> REDCOAT_HAT = REGISTER.register(
      "redcoat_hat", () -> new RedcoatArmorItem(ModArmorMaterials.REDCOAT, Type.HELMET, ModArmorMaterials.durability(new Properties(), ModArmorMaterials.REDCOAT, Type.HELMET))
   );
   public static final DeferredHolder<Item, Item> REDCOAT_COAT = REGISTER.register(
      "redcoat_coat", () -> new RedcoatArmorItem(ModArmorMaterials.REDCOAT, Type.CHESTPLATE, ModArmorMaterials.durability(new Properties(), ModArmorMaterials.REDCOAT, Type.CHESTPLATE))
   );
   public static final DeferredHolder<Item, Item> REDCOAT_PANTS = REGISTER.register(
      "redcoat_pants", () -> new RedcoatArmorItem(ModArmorMaterials.REDCOAT, Type.LEGGINGS, ModArmorMaterials.durability(new Properties(), ModArmorMaterials.REDCOAT, Type.LEGGINGS))
   );
   public static final DeferredHolder<Item, Item> REDCOAT_BOOTS = REGISTER.register(
      "redcoat_boots", () -> new RedcoatArmorItem(ModArmorMaterials.REDCOAT, Type.BOOTS, ModArmorMaterials.durability(new Properties(), ModArmorMaterials.REDCOAT, Type.BOOTS))
   );
   public static final DeferredHolder<Item, Item> SCRAP_HELMET = REGISTER.register(
      "scrap_helmet", () -> new ScrapArmorItem(ModArmorMaterials.SCRAP, Type.HELMET, ModArmorMaterials.durability(new Properties(), ModArmorMaterials.SCRAP, Type.HELMET))
   );
   public static final DeferredHolder<Item, Item> SCRAP_CHESTPLATE = REGISTER.register(
      "scrap_chestplate", () -> new ScrapArmorItem(ModArmorMaterials.SCRAP, Type.CHESTPLATE, ModArmorMaterials.durability(new Properties(), ModArmorMaterials.SCRAP, Type.CHESTPLATE))
   );
   public static final DeferredHolder<Item, Item> SCRAP_LEGGINGS = REGISTER.register(
      "scrap_leggings", () -> new ScrapArmorItem(ModArmorMaterials.SCRAP, Type.LEGGINGS, ModArmorMaterials.durability(new Properties(), ModArmorMaterials.SCRAP, Type.LEGGINGS))
   );
   public static final DeferredHolder<Item, Item> SCRAP_BOOTS = REGISTER.register(
      "scrap_boots", () -> new ScrapArmorItem(ModArmorMaterials.SCRAP, Type.BOOTS, ModArmorMaterials.durability(new Properties(), ModArmorMaterials.SCRAP, Type.BOOTS))
   );
   public static final DeferredHolder<Item, Item> COG_KNIGHT_HELMET = REGISTER.register(
      "cog_knight_helmet", () -> new CogKnightArmorItem(ModArmorMaterials.ANCIENT_BRASS, Type.HELMET, ModArmorMaterials.durability(new Properties(), ModArmorMaterials.ANCIENT_BRASS, Type.HELMET))
   );
   public static final DeferredHolder<Item, Item> COG_KNIGHT_CHESTPLATE = REGISTER.register(
      "cog_knight_chestplate", () -> new CogKnightArmorItem(ModArmorMaterials.ANCIENT_BRASS, Type.CHESTPLATE, ModArmorMaterials.durability(new Properties(), ModArmorMaterials.ANCIENT_BRASS, Type.CHESTPLATE))
   );
   public static final DeferredHolder<Item, Item> COG_KNIGHT_LEGGINGS = REGISTER.register(
      "cog_knight_leggings", () -> new CogKnightArmorItem(ModArmorMaterials.ANCIENT_BRASS, Type.LEGGINGS, ModArmorMaterials.durability(new Properties(), ModArmorMaterials.ANCIENT_BRASS, Type.LEGGINGS))
   );
   public static final DeferredHolder<Item, Item> COG_KNIGHT_BOOTS = REGISTER.register(
      "cog_knight_boots", () -> new CogKnightArmorItem(ModArmorMaterials.ANCIENT_BRASS, Type.BOOTS, ModArmorMaterials.durability(new Properties(), ModArmorMaterials.ANCIENT_BRASS, Type.BOOTS))
   );
   public static final DeferredHolder<Item, Item> EXO_SUIT_HELMET = REGISTER.register(
      "exo_suit_helmet", () -> new ExoSuitItem(ModArmorMaterials.EXO_SUIT, Type.HELMET, ModArmorMaterials.durability(new Properties(), ModArmorMaterials.EXO_SUIT, Type.HELMET))
   );
   public static final DeferredHolder<Item, Item> EXO_SUIT_CHESTPLATE = REGISTER.register(
      "exo_suit_chestplate", () -> new ExoSuitItem(ModArmorMaterials.EXO_SUIT, Type.CHESTPLATE, ModArmorMaterials.durability(new Properties(), ModArmorMaterials.EXO_SUIT, Type.CHESTPLATE))
   );
   public static final DeferredHolder<Item, Item> EXO_SUIT_LEGGINGS = REGISTER.register(
      "exo_suit_leggings", () -> new ExoSuitItem(ModArmorMaterials.EXO_SUIT, Type.LEGGINGS, ModArmorMaterials.durability(new Properties(), ModArmorMaterials.EXO_SUIT, Type.LEGGINGS))
   );
   public static final DeferredHolder<Item, Item> EXO_SUIT_BOOTS = REGISTER.register(
      "exo_suit_boots", () -> new ExoSuitItem(ModArmorMaterials.EXO_SUIT, Type.BOOTS, ModArmorMaterials.durability(new Properties(), ModArmorMaterials.EXO_SUIT, Type.BOOTS))
   );
   public static final DeferredHolder<Item, Item> EXO_SUIT_CORE = REGISTER.register(
      "exo_suit_core", () -> new ExoSuitCoreItem(new Properties().stacksTo(1), ExoSuitCoreItem.CoreTier.BASIC)
   );
   public static final DeferredHolder<Item, Item> ADVANCED_EXO_SUIT_CORE = REGISTER.register(
      "advanced_exo_suit_core", () -> new ExoSuitCoreItem(new Properties().stacksTo(1), ExoSuitCoreItem.CoreTier.ADVANCED)
   );
   public static final DeferredHolder<Item, Item> HEAVY_ARMOR_PLATE = REGISTER.register(
      "heavy_armor_plate", () -> new DamageableUpgradeItem(new Properties().durability(256))
   );
   public static final DeferredHolder<Item, Item> HEAVY_PAULDRON = REGISTER.register(
      "heavy_pauldron", () -> new DamageableUpgradeItem(new Properties().durability(256))
   );
   public static final DeferredHolder<Item, Item> ARMOR_PLATE = REGISTER.register("armor_plate", () -> new DamageableUpgradeItem(new Properties().durability(300)));
   public static final DeferredHolder<Item, Item> PAULDRON = REGISTER.register("pauldron", () -> new DamageableUpgradeItem(new Properties().durability(300)));
   public static final DeferredHolder<Item, Item> NIGHT_VISION_MODULE = REGISTER.register(
      "night_vision_module", () -> new NightVisionModuleItem(new Properties().stacksTo(1).durability(512))
   );
   public static final DeferredHolder<Item, Item> GAS_MASK_MODULE = REGISTER.register(
      "gas_mask_module", () -> new GasMaskModuleItem(new Properties().stacksTo(1).durability(512))
   );
   public static final DeferredHolder<Item, Item> REBREATHER_MODULE = REGISTER.register(
      "rebreather_module", () -> new RebreatherModuleItem(new Properties().stacksTo(1).durability(512))
   );
   public static final DeferredHolder<Item, Item> TARGET_TRACKER_MODULE = REGISTER.register(
      "target_tracker_module", () -> new TargetTrackerModuleItem(new Properties().stacksTo(1).durability(512))
   );
   public static final DeferredHolder<Item, Item> JETPACK_MODULE = REGISTER.register(
      "jetpack_module", () -> new JetpackModuleItem(new Properties().stacksTo(1).durability(512))
   );
   public static final DeferredHolder<Item, Item> ARMOR_POUCHES = REGISTER.register("armor_pouches", () -> new UpgradeItem(new Properties().stacksTo(1)));
   public static final DeferredHolder<Item, Item> HEAVY_ARMOR_POUCHES = REGISTER.register("heavy_armor_pouches", () -> new UpgradeItem(new Properties().stacksTo(1)));
   public static final DeferredHolder<Item, Item> RABBIT_MODULE = REGISTER.register(
      "rabbit_module", () -> new RabbitModuleItem(new Properties().stacksTo(1).durability(512))
   );
   public static final DeferredHolder<Item, Item> SUIT_GREASE = REGISTER.register(
      "suit_grease", () -> new DamageableUpgradeItem(new Properties().stacksTo(1).durability(512))
   );
   public static final DeferredHolder<Item, Item> TENSION_SPRING = REGISTER.register(
      "tension_spring", () -> new DamageableUpgradeItem(new Properties().stacksTo(1).durability(512))
   );
   public static final DeferredHolder<Item, Item> SHOCK_ABSORBER = REGISTER.register(
      "shock_absorber", () -> new DamageableUpgradeItem(new Properties().stacksTo(1).durability(512))
   );
   public static final DeferredHolder<Item, Item> AIR_CANISTER = REGISTER.register("air_canister", () -> new AirCanisterItem(new Properties().stacksTo(1), 1700));
   public static final DeferredHolder<Item, Item> REINFORCED_AIR_CANISTER = REGISTER.register(
      "reinforced_air_canister", () -> new AirCanisterItem(new Properties().stacksTo(1), 3200)
   );
   public static final DeferredHolder<Item, Item> CREATIVE_AIR_CANISTER = REGISTER.register(
      "creative_air_canister", () -> new CreativeAirCanisterItem(new Properties().stacksTo(1).rarity(Rarity.EPIC))
   );
   public static final DeferredHolder<Item, Item> ANTHRALITE_RESPIRATOR = REGISTER.register(
      "anthralite_respirator", () -> new AnthraliteGasMaskArmorItem(ModArmorMaterials.ANTHRALITE, Type.HELMET, ModArmorMaterials.durability(new Properties(), ModArmorMaterials.ANTHRALITE, Type.HELMET))
   );
   public static final DeferredHolder<Item, Item> NETHERITE_RESPIRATOR = REGISTER.register(
      "netherite_respirator", () -> new NetheriteGasMaskArmorItem(ArmorMaterials.NETHERITE, Type.HELMET, new Properties())
   );
   public static final DeferredHolder<Item, Item> DIAMOND_STEEL_HELMET = REGISTER.register(
      "diamond_steel_helmet", () -> new DiamondSteelArmorItem(ModArmorMaterials.DIAMOND_STEEL, Type.HELMET, ModArmorMaterials.durability(new Properties(), ModArmorMaterials.DIAMOND_STEEL, Type.HELMET))
   );
   public static final DeferredHolder<Item, Item> DIAMOND_STEEL_CHESTPLATE = REGISTER.register(
      "diamond_steel_chestplate", () -> new DiamondSteelArmorItem(ModArmorMaterials.DIAMOND_STEEL, Type.CHESTPLATE, ModArmorMaterials.durability(new Properties(), ModArmorMaterials.DIAMOND_STEEL, Type.CHESTPLATE))
   );
   public static final DeferredHolder<Item, Item> DIAMOND_STEEL_LEGGINGS = REGISTER.register(
      "diamond_steel_leggings", () -> new DiamondSteelArmorItem(ModArmorMaterials.DIAMOND_STEEL, Type.LEGGINGS, ModArmorMaterials.durability(new Properties(), ModArmorMaterials.DIAMOND_STEEL, Type.LEGGINGS))
   );
   public static final DeferredHolder<Item, Item> DIAMOND_STEEL_BOOTS = REGISTER.register(
      "diamond_steel_boots", () -> new DiamondSteelArmorItem(ModArmorMaterials.DIAMOND_STEEL, Type.BOOTS, ModArmorMaterials.durability(new Properties(), ModArmorMaterials.DIAMOND_STEEL, Type.BOOTS))
   );
   public static final DeferredHolder<Item, Item> TREATED_BRASS_HELMET = REGISTER.register(
      "treated_brass_helmet", () -> new TreatedBrassArmorItem(ModArmorMaterials.TREATED_BRASS, Type.HELMET, ModArmorMaterials.durability(new Properties(), ModArmorMaterials.TREATED_BRASS, Type.HELMET))
   );
   public static final DeferredHolder<Item, Item> TREATED_BRASS_CHESTPLATE = REGISTER.register(
      "treated_brass_chestplate", () -> new TreatedBrassArmorItem(ModArmorMaterials.TREATED_BRASS, Type.CHESTPLATE, ModArmorMaterials.durability(new Properties(), ModArmorMaterials.TREATED_BRASS, Type.CHESTPLATE))
   );
   public static final DeferredHolder<Item, Item> TREATED_BRASS_LEGGINGS = REGISTER.register(
      "treated_brass_leggings", () -> new TreatedBrassArmorItem(ModArmorMaterials.TREATED_BRASS, Type.LEGGINGS, ModArmorMaterials.durability(new Properties(), ModArmorMaterials.TREATED_BRASS, Type.LEGGINGS))
   );
   public static final DeferredHolder<Item, Item> TREATED_BRASS_BOOTS = REGISTER.register(
      "treated_brass_boots", () -> new TreatedBrassArmorItem(ModArmorMaterials.TREATED_BRASS, Type.BOOTS, ModArmorMaterials.durability(new Properties(), ModArmorMaterials.TREATED_BRASS, Type.BOOTS))
   );
   public static final DeferredHolder<Item, Item> WHITE_FLAG = REGISTER.register("white_flag", () -> new WhiteFlagItem(new Properties().stacksTo(16)));
   public static final DeferredHolder<Item, Item> FLARE_PISTOL = REGISTER.register("flare_pistol", () -> new FlarePistolItem(new Properties().stacksTo(1)));
   public static final DeferredHolder<Item, Item> ANTIQUE_FLARE = REGISTER.register(
      "antique_flare", () -> new RaidFlareItem(new Properties().stacksTo(16), "antique")
   );
   public static final DeferredHolder<Item, Item> FRONTIER_FLARE = REGISTER.register(
      "frontier_flare", () -> new RaidFlareItem(new Properties().stacksTo(16), "frontier")
   );
   public static final DeferredHolder<Item, Item> COPPER_FLARE = REGISTER.register("copper_flare", () -> new RaidFlareItem(new Properties().stacksTo(16), "copper"));
   public static final DeferredHolder<Item, Item> IRON_FLARE = REGISTER.register("iron_flare", () -> new RaidFlareItem(new Properties().stacksTo(16), "iron"));
   public static final DeferredHolder<Item, Item> WRECKER_FLARE = REGISTER.register(
      "wrecker_flare", () -> new RaidFlareItem(new Properties().stacksTo(16), "wrecker")
   );
   public static final DeferredHolder<Item, Item> DIAMOND_STEEL_FLARE = REGISTER.register(
      "diamond_steel_flare", () -> new RaidFlareItem(new Properties().stacksTo(16), "diamond_steel")
   );
   public static final DeferredHolder<Item, Item> TREATED_BRASS_FLARE = REGISTER.register(
      "treated_brass_flare", () -> new RaidFlareItem(new Properties().stacksTo(16), "treated_brass")
   );
   public static final DeferredHolder<Item, Item> GOLD_FLARE = REGISTER.register("gold_flare", () -> new RaidFlareItem(new Properties().stacksTo(16), "piglin"));
   public static final DeferredHolder<Item, Item> SCULK_FLARE = REGISTER.register("sculk_flare", () -> new RaidFlareItem(new Properties().stacksTo(16), "sculk"));
   public static final DeferredHolder<Item, Item> OCEAN_FLARE = REGISTER.register("ocean_flare", () -> new RaidFlareItem(new Properties().stacksTo(16), "ocean"));
   public static final DeferredHolder<Item, Item> RUSTY_MEDAL = REGISTER.register(
      "rusty_medal", () -> new TooltipItem(new Properties(), "item.scguns.rusty_medal.tooltip", "item.scguns.found_in_raids")
   );
   public static final DeferredHolder<Item, Item> LABOR_TROPHY = REGISTER.register(
      "labor_trophy", () -> new TooltipItem(new Properties(), "item.scguns.labor_trophy.tooltip", "item.scguns.found_in_raids")
   );
   public static final DeferredHolder<Item, Item> GOLD_IDOL = REGISTER.register(
      "gold_idol", () -> new TooltipItem(new Properties(), "item.scguns.gold_idol.tooltip", "item.scguns.found_in_raids")
   );
   public static final DeferredHolder<Item, Item> SNAPPED_COGWHEEL = REGISTER.register(
      "snapped_cogwheel", () -> new TooltipItem(new Properties(), "item.scguns.snapped_cogwheel.tooltip", "item.scguns.found_in_raids")
   );
   public static final DeferredHolder<Item, Item> COG_HEART = REGISTER.register(
      "cog_heart", () -> new TooltipItem(new Properties(), "item.scguns.cog_heart.tooltip", "item.scguns.found_in_raids")
   );
   public static final DeferredHolder<Item, Item> SHULKER_CORE = REGISTER.register(
      "shulker_core", () -> new TooltipItem(new Properties(), "item.scguns.shulker_core.tooltip", "item.scguns.shulker_core.tooltip_2")
   );
   public static final DeferredHolder<Item, Item> CERIMONIAL_COD = REGISTER.register(
      "ceremonial_cod", () -> new TooltipItem(new Properties(), "item.scguns.ceremonial_cod.tooltip", "item.scguns.found_in_raids")
   );
   public static final DeferredHolder<Item, Item> SCULK_TOME = REGISTER.register(
      "sculk_tome", () -> new TooltipItem(new Properties(), "item.scguns.sculk_tome.tooltip", "item.scguns.found_in_raids")
   );
   public static final DeferredHolder<Item, Item> LEVIATHAN_TOOTH = REGISTER.register(
      "leviathan_tooth", () -> new TooltipItem(new Properties(), "item.scguns.leviathan_tooth.tooltip", "item.scguns.found_in_raids")
   );
   public static final DeferredHolder<Item, Item> BLUEPRINT_SCRAP = REGISTER.register("blueprint_scrap", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> ANTIQUE_BLUEPRINT = REGISTER.register("antique_blueprint", () -> new BlueprintItem(new Properties().stacksTo(1)));
   public static final DeferredHolder<Item, Item> FRONTIER_BLUEPRINT = REGISTER.register("frontier_blueprint", () -> new BlueprintItem(new Properties().stacksTo(1)));
   public static final DeferredHolder<Item, Item> COPPER_BLUEPRINT = REGISTER.register("copper_blueprint", () -> new BlueprintItem(new Properties().stacksTo(1)));
   public static final DeferredHolder<Item, Item> IRON_BLUEPRINT = REGISTER.register("iron_blueprint", () -> new BlueprintItem(new Properties().stacksTo(1)));
   public static final DeferredHolder<Item, Item> WRECKER_BLUEPRINT = REGISTER.register("wrecker_blueprint", () -> new BlueprintItem(new Properties().stacksTo(1)));
   public static final DeferredHolder<Item, Item> TREATED_BRASS_BLUEPRINT = REGISTER.register(
      "treated_brass_blueprint", () -> new BlueprintItem(new Properties().stacksTo(1))
   );
   public static final DeferredHolder<Item, Item> DIAMOND_STEEL_BLUEPRINT = REGISTER.register(
      "diamond_steel_blueprint", () -> new BlueprintItem(new Properties().stacksTo(1))
   );
   public static final DeferredHolder<Item, Item> PIGLIN_BLUEPRINT = REGISTER.register(
      "piglin_blueprint", () -> new BlueprintItem(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON))
   );
   public static final DeferredHolder<Item, Item> OCEAN_BLUEPRINT = REGISTER.register(
      "ocean_blueprint", () -> new BlueprintItem(new Properties().stacksTo(1).rarity(Rarity.RARE))
   );
   public static final DeferredHolder<Item, Item> DEEP_DARK_BLUEPRINT = REGISTER.register(
      "deep_dark_blueprint", () -> new BlueprintItem(new Properties().stacksTo(1).rarity(Rarity.RARE))
   );
   public static final DeferredHolder<Item, Item> END_BLUEPRINT = REGISTER.register(
      "end_blueprint", () -> new BlueprintItem(new Properties().stacksTo(1).rarity(Rarity.EPIC))
   );
   public static final DeferredHolder<Item, Item> SCORCHED_BLUEPRINT = REGISTER.register(
      "scorched_blueprint", () -> new GlintedBlueprintItem(new Properties().stacksTo(1).rarity(Rarity.EPIC))
   );
   public static final DeferredHolder<Item, Item> EXO_SUIT_BLUEPRINT = REGISTER.register(
      "exo_suit_blueprint", () -> new BlueprintItem(new Properties().stacksTo(1).rarity(Rarity.RARE))
   );
   public static final DeferredHolder<Item, Item> STANDARD_BULLET = REGISTER.register("standard_bullet", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> ADVANCED_BULLET = REGISTER.register("hardened_bullet", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> SYRINGE = REGISTER.register("syringe", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> NITRO_POWDER = REGISTER.register("nitro_powder", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> NITRO_POWDER_DUST = REGISTER.register("nitro_powder_dust", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> NITER_DUST = REGISTER.register("niter_dust", () -> new NiterDustItem(new Properties()));
   public static final DeferredHolder<Item, Item> SHEOL = REGISTER.register("sheol", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> PEAL = REGISTER.register("peal", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> PEAL_DUST = REGISTER.register("peal_dust", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> VEHEMENT_COAL = REGISTER.register("vehement_coal", () -> new FuelItem(new Properties(), 4800));
   public static final DeferredHolder<Item, Item> SHEOL_DUST = REGISTER.register("sheol_dust", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> SULFUR_CHUNK = REGISTER.register("sulfur_chunk", () -> new FuelItem(new Properties(), 800));
   public static final DeferredHolder<Item, Item> COMPOSITE_FILTER = REGISTER.register("composite_filter", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> SULFUR_DUST = REGISTER.register("sulfur_dust", () -> new SulfurDustItem(new Properties()));
   public static final DeferredHolder<Item, Item> BAT_GUANO = REGISTER.register("bat_guano", () -> new BatGuanoItem(new Properties()));
   public static final DeferredHolder<Item, Item> PHOSPHOR_DUST = REGISTER.register("phosphor_dust", () -> new PhosphorItem(new Properties()));
   public static final DeferredHolder<Item, Item> BUCKSHOT = REGISTER.register("buckshot", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> FLECHETTE = REGISTER.register("flechette", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> NEEDLE = REGISTER.register("needle", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> NITRO_BUCKSHOT = REGISTER.register("nitro_buckshot", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> RAW_PHOSPHOR = REGISTER.register("raw_phosphor", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> GUNPOWDER_DUST = REGISTER.register("gunpowder_dust", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> RAW_ANTHRALITE = REGISTER.register("raw_anthralite", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> CRUSHED_RAW_ANTHRALITE = REGISTER.register("crushed_raw_anthralite", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> ANTHRALITE_DUST = REGISTER.register("anthralite_dust", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> CLUMP_ANTHRALITE = REGISTER.register("clump_anthralite", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> SHARD_ANTHRALITE = REGISTER.register("shard_anthralite", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> DIRTY_DUST_ANTHRALITE = REGISTER.register("dirty_dust_anthralite", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> MASS_PRODUCTION_MUSIC_DISC = REGISTER.register(
      "music_disc_mass_production", () -> new Item(ModJukeboxSongs.disc(ModJukeboxSongs.MASS_PRODUCTION))
   );
   public static final DeferredHolder<Item, Item> MASS_DESTRUCTION_MUSIC_DISC = REGISTER.register(
      "music_disc_mass_destruction", () -> new Item(ModJukeboxSongs.disc(ModJukeboxSongs.MASS_DESTRUCTION))
   );
   public static final DeferredHolder<Item, Item> MASS_DESTRUCTION_EXTENDED_MUSIC_DISC = REGISTER.register(
      "music_disc_mass_destruction_extended",
      () -> new Item(ModJukeboxSongs.disc(ModJukeboxSongs.MASS_DESTRUCTION_EXTENDED))
   );
   public static final DeferredHolder<Item, Item> TEAM_LOG = REGISTER.register("team_log", () -> new TeamLogItem(new Properties().stacksTo(1)));
   public static final DeferredHolder<Item, Item> ENEMY_LOG = REGISTER.register("enemy_log", () -> new EnemyLogItem(new Properties().stacksTo(1)));
   public static final DeferredHolder<Item, Item> AUREOUS_SLAG = REGISTER.register("aureous_slag", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> ANTHRALITE_INGOT = REGISTER.register("anthralite_ingot", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> ANTHRALITE_NUGGET = REGISTER.register("anthralite_nugget", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> ANCIENT_BRASS = REGISTER.register("ancient_brass", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> TREATED_IRON_BLEND = REGISTER.register("treated_iron_blend", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> TREATED_IRON_INGOT = REGISTER.register("treated_iron_ingot", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> TREATED_IRON_NUGGET = REGISTER.register("treated_iron_nugget", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> TREATED_BRASS_BLEND = REGISTER.register("treated_brass_blend", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> TREATED_BRASS_INGOT = REGISTER.register("treated_brass_ingot", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> DIAMOND_STEEL_BLEND = REGISTER.register("diamond_steel_blend", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> DEPLETED_DIAMOND_STEEL_INGOT = REGISTER.register(
      "depleted_diamond_steel_ingot", () -> new DepletedDiamondSteelItem(new Properties())
   );
   public static final DeferredHolder<Item, Item> DIAMOND_STEEL_INGOT = REGISTER.register("diamond_steel_ingot", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> SCORCHED_BLEND = REGISTER.register("scorched_blend", () -> new ScorchedItem(new Properties()));
   public static final DeferredHolder<Item, Item> SCORCHED_INGOT = REGISTER.register("scorched_ingot", () -> new ScorchedItem(new Properties()));
   public static final DeferredHolder<Item, Item> CHARGED_AMETHYST_SHARD = REGISTER.register("charged_amethyst_shard", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> EMPTY_TANK = REGISTER.register("empty_tank", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> EMPTY_CORE = REGISTER.register("empty_core", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> ENERGY_CORE = REGISTER.register("energy_core", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> DEPLETED_ENERGY_CORE = REGISTER.register("depleted_energy_core", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> PLASMA_CORE = REGISTER.register("plasma_core", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> NETHER_STAR_FRAGMENT = REGISTER.register("nether_star_fragment", () -> new NetherStarFragmentItem(new Properties()));
   public static final DeferredHolder<Item, Item> EMPTY_BLASPHEMY = REGISTER.register("empty_blasphemy", () -> new EmptyBlasphemyItem(new Properties().stacksTo(1)));
   public static final DeferredHolder<Item, Item> GUN_GRIP = REGISTER.register("gun_grip", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> GUN_BARREL = REGISTER.register("gun_barrel", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> HEAVY_GUN_BARREL = REGISTER.register("heavy_gun_barrel", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> STONE_GUN_BARREL = REGISTER.register("stone_gun_barrel", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> GUN_MAGAZINE = REGISTER.register("gun_magazine", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> GUN_PARTS = REGISTER.register("gun_parts", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> HEAVY_GUN_PARTS = REGISTER.register("heavy_gun_parts", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> FIRING_UNIT = REGISTER.register("firing_unit", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> RAPID_FIRING_UNIT = REGISTER.register("rapid_firing_unit", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> COPPER_GUN_FRAME = REGISTER.register("copper_gun_frame", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> SCORCHED_GUN_FRAME = REGISTER.register("scorched_gun_frame", () -> new ScorchedItem(new Properties()));
   public static final DeferredHolder<Item, Item> TREATED_IRON_GUN_FRAME = REGISTER.register("treated_iron_gun_frame", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> IRON_GUN_FRAME = REGISTER.register("iron_gun_frame", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> TREATED_BRASS_GUN_FRAME = REGISTER.register("treated_brass_gun_frame", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> DIAMOND_STEEL_GUN_FRAME = REGISTER.register("diamond_steel_gun_frame", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> BLANK_MOLD = REGISTER.register("blank_mold", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> SMALL_CASING_MOLD = REGISTER.register(
      "small_casing_mold", () -> new MoldItem(new Properties().stacksTo(1).durability(256))
   );
   public static final DeferredHolder<Item, Item> MEDIUM_CASING_MOLD = REGISTER.register(
      "medium_casing_mold", () -> new MoldItem(new Properties().stacksTo(1).durability(256))
   );
   public static final DeferredHolder<Item, Item> LARGE_CASING_MOLD = REGISTER.register(
      "large_casing_mold", () -> new MoldItem(new Properties().stacksTo(1).durability(128))
   );
   public static final DeferredHolder<Item, Item> BULLET_MOLD = REGISTER.register("bullet_mold", () -> new MoldItem(new Properties().stacksTo(1).durability(256)));
   public static final DeferredHolder<Item, Item> DISC_MOLD = REGISTER.register("disc_mold", () -> new MoldItem(new Properties().stacksTo(1).durability(64)));
   public static final DeferredHolder<Item, Item> GUN_PARTS_MOLD = REGISTER.register("gun_parts_mold", () -> new MoldItem(new Properties().stacksTo(1).durability(32)));
   public static final DeferredHolder<Item, Item> COPPER_DISC = REGISTER.register("copper_disc", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> SMALL_COPPER_CASING = REGISTER.register("small_copper_casing", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> MEDIUM_COPPER_CASING = REGISTER.register("medium_copper_casing", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> SMALL_IRON_CASING = REGISTER.register("small_iron_casing", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> LARGE_IRON_CASING = REGISTER.register("large_iron_casing", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> EMPTY_CELL = REGISTER.register("empty_cell", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> SHULKER_CASING = REGISTER.register("shulker_casing", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> SMALL_DIAMOND_STEEL_CASING = REGISTER.register("small_diamond_steel_casing", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> MEDIUM_DIAMOND_STEEL_CASING = REGISTER.register("medium_diamond_steel_casing", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> SMALL_BRASS_CASING = REGISTER.register("small_brass_casing", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> MEDIUM_BRASS_CASING = REGISTER.register("medium_brass_casing", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> LARGE_BRASS_CASING = REGISTER.register("large_brass_casing", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> POWDER_AND_BALL = REGISTER.register("powder_and_ball", () -> new AmmoItem(new Properties()));
   public static final DeferredHolder<Item, Item> GRAPESHOT = REGISTER.register("grapeshot", () -> new AmmoItem(new Properties()));
   public static final DeferredHolder<Item, Item> COMPACT_COPPER_ROUND = REGISTER.register("compact_copper_round", () -> new AmmoItem(new Properties()));
   public static final DeferredHolder<Item, Item> HOG_ROUND = REGISTER.register("hog_round", () -> new AmmoItem(new Properties()));
   public static final DeferredHolder<Item, Item> STANDARD_COPPER_ROUND = REGISTER.register("standard_copper_round", () -> new AmmoItem(new Properties()));
   public static final DeferredHolder<Item, Item> COMPACT_ADVANCED_ROUND = REGISTER.register("compact_advanced_round", () -> new AmmoItem(new Properties()));
   public static final DeferredHolder<Item, Item> RAMROD_ROUND = REGISTER.register("ramrod_round", () -> new AmmoItem(new Properties()));
   public static final DeferredHolder<Item, Item> ADVANCED_ROUND = REGISTER.register("advanced_round", () -> new AmmoItem(new Properties()));
   public static final DeferredHolder<Item, Item> SHATTER_ROUND = REGISTER.register("shatter_round", () -> new AmmoItem(new Properties()));
   public static final DeferredHolder<Item, Item> KRAHG_ROUND = REGISTER.register("krahg_round", () -> new AmmoItem(new Properties()));
   public static final DeferredHolder<Item, Item> BEOWULF_ROUND = REGISTER.register("beowulf_round", () -> new AmmoItem(new Properties()));
   public static final DeferredHolder<Item, Item> GIBBS_ROUND = REGISTER.register("gibbs_round", () -> new AmmoItem(new Properties()));
   public static final DeferredHolder<Item, Item> SHOTGUN_SHELL = REGISTER.register("shotgun_shell", () -> new AmmoItem(new Properties()));
   public static final DeferredHolder<Item, Item> BEARPACK_SHELL = REGISTER.register("bearpack_shell", () -> new AmmoItem(new Properties()));
   public static final DeferredHolder<Item, Item> BLAZE_FUEL = REGISTER.register(
      "blaze_fuel",
      () -> new FuelAmmoItem(
            new Properties(), EMPTY_TANK, new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0), new MobEffectInstance(MobEffects.WEAKNESS, 100, 0)
         )
   );
   public static final DeferredHolder<Item, Item> FROG_DART = REGISTER.register("frog_dart", () -> new TooltipAmmo(new Properties(), "tooltip.scguns.water"));
   public static final DeferredHolder<Item, Item> SHOTBALL = REGISTER.register("shotball", () -> new ThrowableShotballItem(new Properties()));
   public static final DeferredHolder<Item, Item> ENERGY_CELL = REGISTER.register("energy_cell", () -> new AmmoItem(new Properties()));
   public static final DeferredHolder<Item, Item> SCULK_CELL = REGISTER.register("sculk_cell", () -> new AmmoItem(new Properties()));
   public static final DeferredHolder<Item, Item> SHOCK_CELL = REGISTER.register("shock_cell", () -> new TooltipAmmo(new Properties(), "tooltip.scguns.arcing"));
   public static final DeferredHolder<Item, Item> MICROJET = REGISTER.register("microjet", () -> new TooltipAmmo(new Properties(), "tooltip.scguns.speed_damage"));
   public static final DeferredHolder<Item, Item> SHULKSHOT = REGISTER.register("shulkshot", () -> new TooltipAmmo(new Properties(), "tooltip.scguns.homing"));
   public static final DeferredHolder<Item, Item> UNFINISHED_COMPACT_COPPER_ROUND = REGISTER.register(
      "unfinished_compact_copper_round", () -> new Item(new Properties())
   );
   public static final DeferredHolder<Item, Item> UNFINISHED_HOG_ROUND = REGISTER.register("unfinished_hog_round", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> UNFINISHED_STANDARD_COPPER_ROUND = REGISTER.register(
      "unfinished_standard_copper_round", () -> new Item(new Properties())
   );
   public static final DeferredHolder<Item, Item> UNFINISHED_COMPACT_ADVANCED_ROUND = REGISTER.register(
      "unfinished_compact_advanced_round", () -> new Item(new Properties())
   );
   public static final DeferredHolder<Item, Item> UNFINISHED_RAMROD_ROUND = REGISTER.register("unfinished_ramrod_round", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> UNFINISHED_ADVANCED_ROUND = REGISTER.register("unfinished_advanced_round", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> UNFINISHED_SHATTER_ROUND = REGISTER.register("unfinished_shatter_round", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> UNFINISHED_KRAHG_ROUND = REGISTER.register("unfinished_krahg_round", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> UNFINISHED_BEOWULF_ROUND = REGISTER.register("unfinished_beowulf_round", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> UNFINISHED_GIBBS_ROUND = REGISTER.register("unfinished_gibbs_round", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> UNFINISHED_SHOTGUN_SHELL = REGISTER.register("unfinished_shotgun_shell", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> UNFINISHED_BEARPACK_SHELL = REGISTER.register("unfinished_bearpack_shell", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> UNFINISHED_ENERGY_CELL = REGISTER.register("unfinished_energy_cell", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> UNFINISHED_SCULK_CELL = REGISTER.register("unfinished_sculk_cell", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> UNFINISHED_MICROJET = REGISTER.register("unfinished_microjet", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> UNFINISHED_SHULKSHOT = REGISTER.register("unfinished_shulkshot", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> UNFINISHED_ROCKET = REGISTER.register("unfinished_rocket", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> UNFINISHED_GUN_PARTS = REGISTER.register("unfinished_gun_parts", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> UNFINISHED_HEAVY_GUN_PARTS = REGISTER.register("unfinished_heavy_gun_parts", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> UNFINISHED_PLASMA_CORE = REGISTER.register("unfinished_plasma_core", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> UNFINISHED_OSBORNE_SLUG = REGISTER.register("unfinished_osborne_slug", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> UNFINISHED_FROG_DART = REGISTER.register("unfinished_frog_dart", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> UNFINISHED_HE_GRENADE_ROUND = REGISTER.register("unfinished_he_grenade_round", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> UNFINISHED_GAS_GRENADE_ROUND = REGISTER.register("unfinished_gas_grenade_round", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> UNFINISHED_BOUNCY_GRENADE_ROUND = REGISTER.register(
      "unfinished_bouncy_grenade_round", () -> new Item(new Properties())
   );
   public static final DeferredHolder<Item, Item> UNFINISHED_FIRE_GRENADE_ROUND = REGISTER.register("unfinished_fire_grenade_round", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> ROCKET = REGISTER.register("rocket", () -> new AmmoItem(new Properties().stacksTo(32)));
   public static final DeferredHolder<Item, Item> OSBORNE_SLUG = REGISTER.register("osborne_slug", () -> new AmmoItem(new Properties().stacksTo(4)));
   public static final DeferredHolder<Item, Item> HE_GRENADE_ROUND = REGISTER.register("he_grenade_round", () -> new AmmoItem(new Properties()));
   public static final DeferredHolder<Item, Item> GAS_GRENADE_ROUND = REGISTER.register("gas_grenade_round", () -> new AmmoItem(new Properties()));
   public static final DeferredHolder<Item, Item> BOUNCY_GRENADE_ROUND = REGISTER.register("bouncy_grenade_round", () -> new AmmoItem(new Properties()));
   public static final DeferredHolder<Item, Item> FIRE_GRENADE_ROUND = REGISTER.register("fire_grenade_round", () -> new AmmoItem(new Properties()));
   public static final DeferredHolder<Item, Item> PEBBLES = REGISTER.register("pebbles", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> NETHERITE_SCRAP_CHUNK = REGISTER.register("netherite_scrap_chunk", () -> new ScorchedItem(new Properties()));
   public static final DeferredHolder<Item, Item> PLASMA = REGISTER.register("plasma", () -> new FuelItem(new Properties(), 1800));
   public static final DeferredHolder<Item, Item> PLASMA_NUGGET = REGISTER.register("plasma_nugget", () -> new FuelItem(new Properties(), 360));
   public static final DeferredHolder<Item, Item> PISTOL_AMMO_BOX = REGISTER.register("pistol_ammo_box", () -> new PistolAmmoBoxItem(new Properties().stacksTo(1)));
   public static final DeferredHolder<Item, Item> RIFLE_AMMO_BOX = REGISTER.register("rifle_ammo_box", () -> new RifleAmmoBoxItem(new Properties().stacksTo(1)));
   public static final DeferredHolder<Item, Item> SHOTGUN_AMMO_BOX = REGISTER.register("shotgun_ammo_box", () -> new ShotgunAmmoBoxItem(new Properties().stacksTo(1)));
   public static final DeferredHolder<Item, Item> MAGNUM_AMMO_BOX = REGISTER.register("magnum_ammo_box", () -> new MagnumAmmoBoxItem(new Properties().stacksTo(1)));
   public static final DeferredHolder<Item, Item> ENERGY_AMMO_BOX = REGISTER.register("energy_ammo_box", () -> new EnergyAmmoBoxItem(new Properties().stacksTo(1)));
   public static final DeferredHolder<Item, Item> EMPTY_CASING_POUCH = REGISTER.register(
      "empty_casing_pouch", () -> new EmptyCasingPouchItem(new Properties().stacksTo(1))
   );
   public static final DeferredHolder<Item, Item> ROCKET_AMMO_BOX = REGISTER.register("rocket_ammo_box", () -> new RocketAmmoBoxItem(new Properties().stacksTo(1)));
   public static final DeferredHolder<Item, Item> SPECIAL_AMMO_BOX = REGISTER.register("special_ammo_box", () -> new SpecialAmmoBoxItem(new Properties().stacksTo(1)));
   public static final DeferredHolder<Item, Item> CREATIVE_AMMO_BOX = REGISTER.register(
      "creative_ammo_box", () -> new CreativeAmmoBoxItem(new Properties().stacksTo(1).rarity(Rarity.EPIC))
   );
   public static final DeferredHolder<Item, Item> DISHES_POUCH = REGISTER.register("dishes_pouch", () -> new DishesPouch(new Properties().stacksTo(1)));
   public static final DeferredHolder<Item, Item> ROCK_POUCH = REGISTER.register("rock_pouch", () -> new RockPouch(new Properties().stacksTo(1)));
   public static final DeferredHolder<Item, Item> GRENADE = REGISTER.register("grenade", () -> new GrenadeItem(new Properties().stacksTo(32), 60));
   public static final DeferredHolder<Item, Item> STUN_GRENADE = REGISTER.register("stun_grenade", () -> new StunGrenadeItem(new Properties().stacksTo(32), 72000));
   public static final DeferredHolder<Item, Item> MOLOTOV_COCKTAIL = REGISTER.register(
      "molotov_cocktail", () -> new MolotovCocktailItem(new Properties().stacksTo(32), 72000)
   );
   public static final DeferredHolder<Item, Item> HELLFIRE_BOMB = REGISTER.register("hellfire_bomb", () -> new HellfireBombItem(new Properties().stacksTo(32), 72000));
   public static final DeferredHolder<Item, Item> CHOKE_BOMB = REGISTER.register("choke_bomb", () -> new ChokeBombItem(new Properties().stacksTo(32), 72000));
   public static final DeferredHolder<Item, Item> SWARM_BOMB = REGISTER.register("swarm_bomb", () -> new SwarmBombItem(new Properties().stacksTo(32), 72000));
   public static final DeferredHolder<Item, Item> NAIL_BOMB = REGISTER.register("nail_bomb", () -> new NailBombItem(new Properties().stacksTo(32), 72000));
   public static final DeferredHolder<Item, Item> GAS_GRENADE = REGISTER.register("gas_grenade", () -> new GasGrenadeItem(new Properties().stacksTo(32), 72000));
   public static final DeferredHolder<Item, Item> BEACON_GRENADE = REGISTER.register(
      "beacon_grenade", () -> new BeaconGrenadeItem(new Properties().stacksTo(32), 72000)
   );
   public static final DeferredHolder<Item, Item> WEIRD_FLESH = REGISTER.register("weird_flesh", WeirdFleshItem::new);
   public static final DeferredHolder<Item, Item> BASIC_POULTICE = REGISTER.register(
      "basic_poultice", () -> new HealingBandageItem(new Properties().stacksTo(16), 4, (MobEffectInstance)null)
   );
   public static final DeferredHolder<Item, Item> HONEY_SULFUR_POULTICE = REGISTER.register(
      "honey_sulfur_poultice", () -> new HealingBandageItem(new Properties().stacksTo(16), 8, new MobEffectInstance(MobEffects.REGENERATION, 100, 0))
   );
   public static final DeferredHolder<Item, Item> ENCHANTED_BANDAGE = REGISTER.register(
      "enchanted_bandage",
      () -> new GlintedHealingBandageItem(
            new Properties().stacksTo(16).rarity(Rarity.RARE),
            12,
            new MobEffectInstance(MobEffects.REGENERATION, 100, 1),
            new MobEffectInstance(MobEffects.ABSORPTION, 400, 0)
         )
   );
   public static final DeferredHolder<Item, Item> DRAGON_SALVE = REGISTER.register(
      "dragon_salve",
      () -> new GlintedHealingBandageItem(
            new Properties().stacksTo(16).rarity(Rarity.RARE),
            16,
            new MobEffectInstance(MobEffects.REGENERATION, 200, 1),
            new MobEffectInstance(MobEffects.ABSORPTION, 700, 0)
         )
   );
   public static final DeferredHolder<Item, Item> COLD_PACK = REGISTER.register("cold_pack", () -> new ColdPackItem(new Properties().stacksTo(16)));
   public static final DeferredHolder<Item, Item> LASER_SIGHT = REGISTER.register(
      "laser_sight", () -> new LaserSightItem(Attachments.LASER_SIGHT, new Properties().stacksTo(1).durability(1300))
   );
   public static final DeferredHolder<Item, Item> LONG_SCOPE = REGISTER.register(
      "long_scope", () -> new ScopeItem(Attachments.LONG_SCOPE, new Properties().stacksTo(1).durability(1600))
   );
   public static final DeferredHolder<Item, Item> MEDIUM_SCOPE = REGISTER.register(
      "medium_scope", () -> new ScopeItem(Attachments.MEDIUM_SCOPE, new Properties().stacksTo(1).durability(1400))
   );
   public static final DeferredHolder<Item, Item> REFLEX_SIGHT = REGISTER.register(
      "reflex_sight", () -> new ScopeItem(Attachments.REFLEX_SIGHT, new Properties().stacksTo(1).durability(1300))
   );
   public static final DeferredHolder<Item, Item> LIGHT_STOCK = REGISTER.register(
      "light_stock", () -> new StockItem(Stock.create(GunModifiers.LIGHT_STOCK_MODIFIER), new Properties().stacksTo(1).durability(1350), false)
   );
   public static final DeferredHolder<Item, Item> WEIGHTED_STOCK = REGISTER.register(
      "weighted_stock", () -> new StockItem(Stock.create(GunModifiers.WEIGHTED_STOCK_MODIFIER), new Properties().stacksTo(1).durability(1700))
   );
   public static final DeferredHolder<Item, Item> WOODEN_STOCK = REGISTER.register(
      "wooden_stock", () -> new StockItem(Stock.create(GunModifiers.WOODEN_STOCK_MODIFIER), new Properties().stacksTo(1).durability(1550), false)
   );
   public static final DeferredHolder<Item, Item> BUMP_STOCK = REGISTER.register(
      "bump_stock", () -> new StockItem(Stock.create(GunModifiers.BUMP_STOCK_MODIFIER), new Properties().stacksTo(1).durability(1200), true)
   );
   public static final DeferredHolder<Item, Item> SILENCER = REGISTER.register(
      "silencer", () -> new BarrelItem(Barrel.create(0.0F, GunModifiers.SILENCER_MODIFIER, GunModifiers.SILENCED), new Properties().stacksTo(1).durability(550))
   );
   public static final DeferredHolder<Item, Item> ADVANCED_SILENCER = REGISTER.register(
      "advanced_silencer",
      () -> new BarrelItem(Barrel.create(0.0F, GunModifiers.ADVANCED_SILENCER_MODIFIER, GunModifiers.SILENCED), new Properties().stacksTo(1).durability(1200))
   );
   public static final DeferredHolder<Item, Item> MUZZLE_BRAKE = REGISTER.register(
      "muzzle_brake", () -> new BarrelItem(Barrel.create(0.0F, GunModifiers.MUZZLE_BRAKE_MODIFIER), new Properties().stacksTo(1).durability(1400))
   );
   public static final DeferredHolder<Item, Item> EXTENDED_BARREL = REGISTER.register(
      "extended_barrel", () -> new ExtendedBarrelItem(Barrel.create(0.0F, GunModifiers.EXTENDED_BARREL_MODIFIER), new Properties().stacksTo(1).durability(700))
   );
   public static final DeferredHolder<Item, Item> SLUG_SPLITTER = REGISTER.register(
      "slug_splitter", () -> new BarrelItem(Barrel.create(0.0F, GunModifiers.SLUG_SPLITTER_MODIFIER), new Properties().stacksTo(1).durability(1400))
   );
   public static final DeferredHolder<Item, Item> LIGHT_GRIP = REGISTER.register(
      "light_grip", () -> new UnderBarrelItem(UnderBarrel.create(GunModifiers.LIGHT_RECOIL), new Properties().stacksTo(1).durability(1400))
   );
   public static final DeferredHolder<Item, Item> VERTICAL_GRIP = REGISTER.register(
      "vertical_grip", () -> new UnderBarrelItem(UnderBarrel.create(GunModifiers.REDUCED_RECOIL), new Properties().stacksTo(1).durability(1600))
   );
   public static final DeferredHolder<Item, Item> IRON_BAYONET = REGISTER.register(
      "iron_bayonet", () -> new BayonetItem(UnderBarrel.create(GunModifiers.IRON_BAYONET_DAMAGE), new Properties().stacksTo(1).durability(256), 1.5F, -3.0F)
   );
   public static final DeferredHolder<Item, Item> ANTHRALITE_BAYONET = REGISTER.register(
      "anthralite_bayonet",
      () -> new BayonetItem(UnderBarrel.create(GunModifiers.ANTHRALITE_BAYONET_DAMAGE), new Properties().stacksTo(1).durability(512), 2.0F, -3.0F)
   );
   public static final DeferredHolder<Item, Item> DIAMOND_BAYONET = REGISTER.register(
      "diamond_bayonet",
      () -> new BayonetItem(UnderBarrel.create(GunModifiers.DIAMOND_BAYONET_DAMAGE), new Properties().stacksTo(1).durability(1024), 3.0F, -3.0F)
   );
   public static final DeferredHolder<Item, Item> NETHERITE_BAYONET = REGISTER.register(
      "netherite_bayonet",
      () -> new BayonetItem(UnderBarrel.create(GunModifiers.NETHERITE_BAYONET_DAMAGE), new Properties().stacksTo(1).durability(1550), 4.0F, -3.0F)
   );
   public static final DeferredHolder<Item, Item> EXTENDED_MAG = REGISTER.register(
      "extended_mag", () -> new MagazineItem(Magazine.create(GunModifiers.EXTENDED_MAG_MODIFIER), new Properties().stacksTo(1).durability(1700))
   );
   public static final DeferredHolder<Item, Item> SPEED_MAG = REGISTER.register(
      "speed_mag", () -> new MagazineItem(Magazine.create(GunModifiers.SPEED_MAG_MODIFIER), new Properties().stacksTo(1).durability(1550))
   );
   public static final DeferredHolder<Item, Item> PLUS_P_MAG = REGISTER.register(
      "plus_p_mag",
      () -> new MagazineItem(Magazine.create(GunModifiers.INCREASED_DAMAGE, GunModifiers.PLUS_P_MAG), new Properties().stacksTo(1).durability(1400))
   );
   public static final DeferredHolder<Item, Item> REPAIR_KIT = REGISTER.register("repair_kit", () -> new Item(new Properties()));
   public static final DeferredHolder<Item, Item> SCAMP_PACKAGE = REGISTER.register("scamp_package", () -> new ScampPackageItem(new Properties().stacksTo(1)));
   public static final DeferredHolder<Item, Item> VIVENTRUM_PACKAGE = REGISTER.register(
      "viventrum_package", () -> new ViventrumPackageItem(new Properties().stacksTo(1))
   );
   public static final DeferredHolder<Item, Item> COG_LOCATOR = REGISTER.register(
      "cog_locator", () -> new CogLocatorItem(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON))
   );
   public static final DeferredHolder<Item, Item> THE_PACT = REGISTER.register("the_pact", () -> new ThePactItem(new Properties().stacksTo(1).rarity(Rarity.EPIC)));
   public static final DeferredHolder<Item, Item> VICIOUS_ACID_BUCKET = REGISTER.register(
      "vicious_acid_bucket", () -> new BucketItem(ModFluids.VICIOUS_ACID_SOURCE.get(), new Properties().craftRemainder(Items.BUCKET).stacksTo(1))
   );
   public static final DeferredHolder<Item, Item> COG_MINION_SPAWN_EGG = REGISTER.register(
      "cog_minion_spawn_egg", () -> new DeferredSpawnEggItem(ModEntities.COG_MINION, 7753759, 8355968, new Properties())
   );
   public static final DeferredHolder<Item, Item> COG_KNIGHT_SPAWN_EGG = REGISTER.register(
      "cog_knight_spawn_egg", () -> new DeferredSpawnEggItem(ModEntities.COG_KNIGHT, 16239468, 12553813, new Properties())
   );
   public static final DeferredHolder<Item, Item> TRAUMA_UNIT_SPAWN_EGG = REGISTER.register(
      "trauma_unit_spawn_egg", () -> new DeferredSpawnEggItem(ModEntities.TRAUMA_UNIT, 16239468, 15920363, new Properties())
   );
   public static final DeferredHolder<Item, Item> SKY_CARRIER_SPAWN_EGG = REGISTER.register(
      "sky_carrier_spawn_egg", () -> new DeferredSpawnEggItem(ModEntities.SKY_CARRIER, 16771980, 5197647, new Properties())
   );
   public static final DeferredHolder<Item, Item> SUPPLY_SCAMP_SPAWN_EGG = REGISTER.register(
      "supply_scamp_spawn_egg", () -> new DeferredSpawnEggItem(ModEntities.SUPPLY_SCAMP, 16771980, 10460051, new Properties())
   );
   public static final DeferredHolder<Item, Item> DISSIDENT_SPAWN_EGG = REGISTER.register(
      "dissident_spawn_egg", () -> new DeferredSpawnEggItem(ModEntities.DISSIDENT, 2106408, 11232801, new Properties())
   );
   public static final DeferredHolder<Item, Item> VIVENTRUM_SPAWN_EGG = REGISTER.register(
      "viventrum_spawn_egg", () -> new DeferredSpawnEggItem(ModEntities.VIVENTRUM, 10263194, 1710618, new Properties())
   );
   public static final DeferredHolder<Item, Item> HIVE_SPAWN_EGG = REGISTER.register(
      "hive_spawn_egg", () -> new DeferredSpawnEggItem(ModEntities.HIVE, 10263194, 4670789, new Properties())
   );
   public static final DeferredHolder<Item, Item> SWARM_SPAWN_EGG = REGISTER.register(
      "swarm_spawn_egg", () -> new DeferredSpawnEggItem(ModEntities.SWARM, 5460048, 1381653, new Properties())
   );
   public static final DeferredHolder<Item, Item> HORNLIN_SPAWN_EGG = REGISTER.register(
      "hornlin_spawn_egg", () -> new DeferredSpawnEggItem(ModEntities.HORNLIN, 10639674, 10239849, new Properties())
   );
   public static final DeferredHolder<Item, Item> ZOMBIFIED_HORNLIN_SPAWN_EGG = REGISTER.register(
      "zombified_hornlin_spawn_egg", () -> new DeferredSpawnEggItem(ModEntities.ZOMBIFIED_HORNLIN, 15104371, 10239849, new Properties())
   );
   public static final DeferredHolder<Item, Item> BLUNDERER_SPAWN_EGG = REGISTER.register(
      "blunderer_spawn_egg", () -> new DeferredSpawnEggItem(ModEntities.BLUNDERER, 14737632, 10003106, new Properties())
   );
   public static final DeferredHolder<Item, Item> THE_MERCHANT_SPAWN_EGG = REGISTER.register(
      "the_merchant_spawn_egg", () -> new DeferredSpawnEggItem(ModEntities.THE_MERCHANT, 2106408, 9132587, new Properties())
   );
   public static final DeferredHolder<Item, Item> SIGNAL_BEACON_SPAWN_EGG = REGISTER.register(
      "signal_beacon_spawn_egg", () -> new DeferredSpawnEggItem(ModEntities.SIGNAL_BEACON, 16239468, 15536423, new Properties())
   );
   public static final DeferredHolder<Item, Item> SCAMP_TANK_SPAWN_EGG = REGISTER.register(
      "scamp_tank_spawn_egg", () -> new DeferredSpawnEggItem(ModEntities.SCAMP_TANK, 16771980, 5197647, new Properties())
   );
   public static final DeferredHolder<Item, Item> SCAMPLER_SPAWN_EGG = REGISTER.register(
      "scampler_spawn_egg", () -> new DeferredSpawnEggItem(ModEntities.SCAMPLER, 16771980, 10495783, new Properties())
   );
   public static final DeferredHolder<Item, Item> ADJUDICATOR_SPAWN_EGG = REGISTER.register(
      "adjudicator_spawn_egg", () -> new DeferredSpawnEggItem(ModEntities.ADJUDICATOR, 2106408, 1728105, new Properties())
   );
   public static final DeferredHolder<Item, Item> SUBJUGATOR_SPAWN_EGG = REGISTER.register(
      "subjugator_spawn_egg", () -> new DeferredSpawnEggItem(ModEntities.SUBJUGATOR, 2106408, 1134925, new Properties())
   );
   public static final DeferredHolder<Item, Item> MOTHER_GHAST_SPAWN_EGG = REGISTER.register(
      "mother_ghast_spawn_egg", () -> new DeferredSpawnEggItem(ModEntities.MOTHER_GHAST, 16777215, 0, new Properties())
   );
   public static final DeferredHolder<Item, Item> FINFORCER_SPAWN_EGG = REGISTER.register(
      "finforcer_spawn_egg", () -> new DeferredSpawnEggItem(ModEntities.FINFORCER, 2106408, 4942959, new Properties())
   );
   public static final DeferredHolder<Item, Item> PRAETOR_SPAWN_EGG = REGISTER.register(
      "praetor_spawn_egg", () -> new DeferredSpawnEggItem(ModEntities.PRAETOR, 2106408, 14561293, new Properties())
   );
   public static final DeferredHolder<Item, Item> SULFURHEAD_SPAWN_EGG = REGISTER.register(
      "sulfurhead_spawn_egg", () -> new DeferredSpawnEggItem(ModEntities.SULFURHEAD, 12834422, 15457146, new Properties())
   );

   public ModItems() {
      super();
   }

   /**
    * Farmer's Delight's anthralite knife, built reflectively because Farmer's Delight is an
    * optional dependency: without it {@code KnifeItem} must never be resolved.
    *
    * <p><b>1.21.1 changed the constructor.</b> 1.20.1 had
    * {@code KnifeItem(Tier, float attackDamage, float attackSpeed, Properties)}; 1.21.1 only
    * has {@code KnifeItem(Tier, Properties)} and expects the attack values in the properties
    * - which is exactly what Farmer's Delight's own registration does, through the vanilla
    * helper {@code DiggerItem.createAttributes(tier, 0.5F, -2.0F)} (their
    * {@code ModItems#knifeItem}). Those are the same two numbers 0.5.5 passed to the old
    * constructor, so the item keeps its 0.5.5 damage and speed.</p>
    *
    * <p>Keeping the old four-argument lookup threw {@code NoSuchMethodException} during item
    * registration, i.e. at startup, and surfaced as "Failed to create ANTHRALITE_KNIFE" - a
    * hard crash for anyone with Farmer's Delight installed. Only wiring the jar into
    * {@code libs/} made it observable.</p>
    */
   private static Item createAnthraliteKnife() {
      try {
         Class<?> knifeItemClass = Class.forName("vectorwing.farmersdelight.common.item.KnifeItem");
         Properties properties = new Properties()
            .attributes(DiggerItem.createAttributes(ModTiers.ANTHRALITE, 0.5F, -2.0F));
         Constructor<?> constructor = knifeItemClass.getConstructor(Tier.class, Properties.class);
         return (Item)constructor.newInstance(ModTiers.ANTHRALITE, properties);
      } catch (Exception e) {
         throw new RuntimeException("Failed to create ANTHRALITE_KNIFE", e);
      }
   }

   public static void registerItems() {
      if (ScorchedGuns.farmersDelightLoaded) {
         ANTHRALITE_KNIFE = REGISTER.register("anthralite_knife", ModItems::createAnthraliteKnife);
      }

      if (ScorchedGuns.createIronWorksLoaded) {
         ANTHRALITE_HAMMER = REGISTER.register("anthralite_hammer", () -> new AnthraliteHammerItem(ModTiers.ANTHRALITE, -3.2F, new Properties()));
      }

      if (ScorchedGuns.createIronWorksLoaded) {
         ANTHRALITE_PAXEL = REGISTER.register("anthralite_paxel", () -> new AnthralitePaxelItem(ModTiers.ANTHRALITE, new Properties()));
      }
   }

   public static void register(IEventBus eventBus) {
      REGISTER.register(eventBus);
   }
}
