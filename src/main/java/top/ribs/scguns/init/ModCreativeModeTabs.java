package top.ribs.scguns.init;


import top.ribs.scguns.util.NbtHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTab.Output;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import top.ribs.scguns.item.AirCanisterItem;
import top.ribs.scguns.item.EnergyGunItem;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.item.exosuit.ExoSuitCoreItem;

public class ModCreativeModeTabs {
   public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "scguns");
   public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SCORCHED_GUNS_TAB = CREATIVE_MODE_TABS.register(
      "scorched_guns_tab",
      () -> CreativeModeTab.builder()
            .icon(() -> new ItemStack((ItemLike)ModItems.M3_CARABINE.get()))
            .title(Component.translatable("creativetab.scorched_guns_tab"))
            .displayItems((pParameters, pOutput) -> {
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.FLINTLOCK_PISTOL.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.HANDCANNON.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.MUSKET.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.BLUNDERBUSS.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.DOUBLET.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.REPEATING_MUSKET.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.LONGARM.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.FENCER_CARABINE.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.FENCER_THUMPER.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.PAX.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.WINNIE.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.WINNIE_MILLEND.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.RED_RAYDAR.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.CALLWELL_CONVERSION.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.CALLWELL.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.CALLWELL_TERMINAL.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.SAKETINI.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.SAKETINI_IRONPORT.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.KILN_GUN.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.BIG_BORE.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.LASER_MUSKET.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.PLASMABUSS.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.TESLOCK_RIFLE.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.SCRAPPER.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.RUSTY_GNAT.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.UMAX_PISTOL.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.MAKESHIFT_RIFLE.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.BOOMSTICK.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.BRUISER.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.LLR_DIRECTOR.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.BIRDFEEDER.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.WHISTLER.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.BLOOPER.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.ARC_WORKER.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.DEFENDER_PISTOL.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.TRENCHUR.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.GREASER_SMG.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.M3_CARABINE.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.M3_MARKSMAN.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.COMBAT_SHOTGUN.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.VENTURI.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.IRON_JAVELIN.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.IRON_SPEAR.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.AUVTOMAG.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.PULSAR.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.GYROJET_PISTOL.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.BRAWLER.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.CRUSADER.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.MK43_RIFLE.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.TRIQUETRA.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.ROCKET_RIFLE.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.ULTRA_KNIGHT_HAWK.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.FLOUNDERGAT.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.HYPERBARIA.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.MARLIN.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.BOMB_LANCE.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.HULLBREAKER.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.SEQUOIA.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.SPIRULIDA.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.MOKOVA.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.MAK_MKII.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.STILETTO.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.RAILWORKER.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.TURNPIKE.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.KILLER_23.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.HOMEMAKER.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.KALASKAH.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.BASKER.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.TL_RUNNER.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.STIGG.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.WHIZZBANGER.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.KRAUSER.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.SOUL_DRUMMER.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.VALORA.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.UPPERCUT.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.MICINA.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.PRUSH_GUN.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.DRILL.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.DRILL_CONVERSION.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.LOCKEWOOD.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.ZILK_45.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.RG_JIGSAW.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.NAILER.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.INERTIAL.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.MINKSY.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.MAS_PEDDLER.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.MAS_55.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.INQUISITOR.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.PLASGUN.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.TRUANT.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.CYCLONE.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.SHARD_CULLER.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.M22_WALTZ.get());
               ModCreativeModeTabs.CreativeTabHelper.addGunOrEnergyWeaponWithFullResources(pOutput, (Item)ModItems.WALTZ_CONVERSION.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.OSGOOD_50.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.GRANDLE_OG.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.GRANDLE.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.COGLOADER.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.GALE.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.JR_WRISTBREAKER.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.JACKHAMMER.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.HOWLER.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.HOWLER_CONVERSION.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.GAUSS_RIFLE.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.LIBERTAS.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.NIAMI.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.HAMMER_GL.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.SPITFIRE.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.GATTALER.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.THUNDERHEAD.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.SCRATCHES.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.CR4K_MINING_LASER.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.DOZIER_RL.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.BLASPHEMY.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.PYROCLASTIC_FLOW.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.FREYR.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.MANGALITSA.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.VULCANIC_REPEATER.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.TROTTERS.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.SUPER_SHOTGUN.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.WHISPERS.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.ECHOES_2.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.SCULK_RESONATOR.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.FORLORN_HOPE.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.CARAPICE.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.SHELLURKER.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.WEEVIL.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.DARK_MATTER.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.LONE_WONDER.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.RAYGUN.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.PRIMA_MATERIA.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.RAT_KING_AND_QUEEN.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.LOCUST.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.STERILIZER.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.NEWBORN_CYST.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.EARTHS_CORPSE.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.FLAYED_GOD.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.NERVEPINCH.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.TERRA_INCOGNITA.get());
               ModCreativeModeTabs.CreativeTabHelper.addItemWithFullAmmo(pOutput, (Item)ModItems.ASTELLA.get());
            })
            .build()
   );
   public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SCORCHED_ITEMS_TAB = CREATIVE_MODE_TABS.register(
      "scorched_items_tab",
      () -> CreativeModeTab.builder()
            .icon(() -> new ItemStack((ItemLike)ModItems.ANTHRALITE_INGOT.get()))
            .title(Component.translatable("creativetab.scorched_items_tab"))
            .displayItems((pParameters, pOutput) -> {
               pOutput.accept((ItemLike)ModItems.ANTHRALITE_PICKAXE.get());
               pOutput.accept((ItemLike)ModItems.ANTHRALITE_AXE.get());
               pOutput.accept((ItemLike)ModItems.ANTHRALITE_SHOVEL.get());
               pOutput.accept((ItemLike)ModItems.ANTHRALITE_HOE.get());
               pOutput.accept((ItemLike)ModItems.ANTHRALITE_SWORD.get());
               if (ModCompat.isFarmersDelightLoaded()) {
                  pOutput.accept((ItemLike)ModItems.ANTHRALITE_KNIFE.get());
               }

               if (ModCompat.isCreateIronworksLoaded()) {
                  pOutput.accept((ItemLike)ModItems.ANTHRALITE_HAMMER.get());
                  pOutput.accept((ItemLike)ModItems.ANTHRALITE_PAXEL.get());
               }

               pOutput.accept((ItemLike)ModItems.ANTHRALITE_HELMET.get());
               pOutput.accept((ItemLike)ModItems.ANTHRALITE_RESPIRATOR.get());
               pOutput.accept((ItemLike)ModItems.ANTHRALITE_CHESTPLATE.get());
               pOutput.accept((ItemLike)ModItems.ANTHRALITE_LEGGINGS.get());
               pOutput.accept((ItemLike)ModItems.ANTHRALITE_BOOTS.get());
               pOutput.accept((ItemLike)ModItems.RIDGETOP.get());
               pOutput.accept((ItemLike)ModItems.ADRIEN_HELM.get());
               pOutput.accept((ItemLike)ModItems.ADRIEN_CHESTPLATE.get());
               pOutput.accept((ItemLike)ModItems.ADRIEN_LEGGINGS.get());
               pOutput.accept((ItemLike)ModItems.ADRIEN_BOOTS.get());
               pOutput.accept((ItemLike)ModItems.BRASS_MASK.get());
               pOutput.accept((ItemLike)ModItems.IRON_MASK.get());
               pOutput.accept((ItemLike)ModItems.SCRAP_HELMET.get());
               pOutput.accept((ItemLike)ModItems.SCRAP_CHESTPLATE.get());
               pOutput.accept((ItemLike)ModItems.SCRAP_LEGGINGS.get());
               pOutput.accept((ItemLike)ModItems.SCRAP_BOOTS.get());
               pOutput.accept((ItemLike)ModItems.REDCOAT_HAT.get());
               pOutput.accept((ItemLike)ModItems.REDCOAT_COAT.get());
               pOutput.accept((ItemLike)ModItems.REDCOAT_PANTS.get());
               pOutput.accept((ItemLike)ModItems.REDCOAT_BOOTS.get());
               pOutput.accept((ItemLike)ModItems.WARAXE.get());
               pOutput.accept((ItemLike)ModItems.COG_MACE.get());
               pOutput.accept((ItemLike)ModItems.COG_KNIGHT_HELMET.get());
               pOutput.accept((ItemLike)ModItems.COG_KNIGHT_CHESTPLATE.get());
               pOutput.accept((ItemLike)ModItems.COG_KNIGHT_LEGGINGS.get());
               pOutput.accept((ItemLike)ModItems.COG_KNIGHT_BOOTS.get());
               pOutput.accept((ItemLike)ModItems.EXO_SUIT_HELMET.get());
               pOutput.accept((ItemLike)ModItems.EXO_SUIT_CHESTPLATE.get());
               pOutput.accept((ItemLike)ModItems.EXO_SUIT_LEGGINGS.get());
               pOutput.accept((ItemLike)ModItems.EXO_SUIT_BOOTS.get());
               pOutput.accept((ItemLike)ModItems.NETHERITE_RESPIRATOR.get());
               pOutput.accept((ItemLike)ModItems.DIAMOND_STEEL_HELMET.get());
               pOutput.accept((ItemLike)ModItems.DIAMOND_STEEL_CHESTPLATE.get());
               pOutput.accept((ItemLike)ModItems.DIAMOND_STEEL_LEGGINGS.get());
               pOutput.accept((ItemLike)ModItems.DIAMOND_STEEL_BOOTS.get());
               pOutput.accept((ItemLike)ModItems.TREATED_BRASS_HELMET.get());
               pOutput.accept((ItemLike)ModItems.TREATED_BRASS_CHESTPLATE.get());
               pOutput.accept((ItemLike)ModItems.TREATED_BRASS_LEGGINGS.get());
               pOutput.accept((ItemLike)ModItems.TREATED_BRASS_BOOTS.get());
               pOutput.accept((ItemLike)ModItems.REPAIR_KIT.get());
               pOutput.accept((ItemLike)ModItems.COMPOSITE_FILTER.get());
               pOutput.accept((ItemLike)ModItems.RANGE_FINDER.get());
               pOutput.accept((ItemLike)ModItems.METAL_DETECTOR.get());
               ModCreativeModeTabs.CreativeTabHelper.addExoSuitCoreItemWithFullEnergy(pOutput, (Item)ModItems.EXO_SUIT_CORE.get());
               ModCreativeModeTabs.CreativeTabHelper.addExoSuitCoreItemWithFullEnergy(pOutput, (Item)ModItems.ADVANCED_EXO_SUIT_CORE.get());
               pOutput.accept((ItemLike)ModItems.NIGHT_VISION_MODULE.get());
               pOutput.accept((ItemLike)ModItems.TARGET_TRACKER_MODULE.get());
               pOutput.accept((ItemLike)ModItems.GAS_MASK_MODULE.get());
               pOutput.accept((ItemLike)ModItems.REBREATHER_MODULE.get());
               pOutput.accept((ItemLike)ModItems.JETPACK_MODULE.get());
               pOutput.accept((ItemLike)ModItems.RABBIT_MODULE.get());
               pOutput.accept((ItemLike)ModItems.SUIT_GREASE.get());
               pOutput.accept((ItemLike)ModItems.TENSION_SPRING.get());
               pOutput.accept((ItemLike)ModItems.SHOCK_ABSORBER.get());
               pOutput.accept((ItemLike)ModItems.ARMOR_PLATE.get());
               pOutput.accept((ItemLike)ModItems.PAULDRON.get());
               pOutput.accept((ItemLike)ModItems.HEAVY_ARMOR_PLATE.get());
               pOutput.accept((ItemLike)ModItems.HEAVY_PAULDRON.get());
               pOutput.accept((ItemLike)ModItems.ARMOR_POUCHES.get());
               pOutput.accept((ItemLike)ModItems.HEAVY_ARMOR_POUCHES.get());
               pOutput.accept((ItemLike)ModItems.PEBBLES.get());
               pOutput.accept((ItemLike)ModItems.SULFUR_CHUNK.get());
               pOutput.accept((ItemLike)ModItems.SULFUR_DUST.get());
               pOutput.accept((ItemLike)ModItems.RAW_PHOSPHOR.get());
               pOutput.accept((ItemLike)ModItems.PHOSPHOR_DUST.get());
               pOutput.accept((ItemLike)ModItems.NITER_DUST.get());
               pOutput.accept((ItemLike)ModItems.BAT_GUANO.get());
               pOutput.accept((ItemLike)ModItems.GUNPOWDER_DUST.get());
               pOutput.accept((ItemLike)ModItems.AUREOUS_SLAG.get());
               pOutput.accept((ItemLike)ModItems.SHEOL.get());
               pOutput.accept((ItemLike)ModItems.SHEOL_DUST.get());
               pOutput.accept((ItemLike)ModItems.PEAL.get());
               pOutput.accept((ItemLike)ModItems.PEAL_DUST.get());
               pOutput.accept((ItemLike)ModItems.BUCKSHOT.get());
               pOutput.accept((ItemLike)ModItems.FLECHETTE.get());
               pOutput.accept((ItemLike)ModItems.NEEDLE.get());
               pOutput.accept((ItemLike)ModItems.NITRO_BUCKSHOT.get());
               pOutput.accept((ItemLike)ModItems.VEHEMENT_COAL.get());
               pOutput.accept((ItemLike)ModItems.NITRO_POWDER.get());
               pOutput.accept((ItemLike)ModItems.NITRO_POWDER_DUST.get());
               pOutput.accept((ItemLike)ModItems.PLASMA.get());
               pOutput.accept((ItemLike)ModItems.PLASMA_NUGGET.get());
               pOutput.accept((ItemLike)ModItems.SCORCHED_BLEND.get());
               pOutput.accept((ItemLike)ModItems.SCORCHED_INGOT.get());
               pOutput.accept((ItemLike)ModItems.RAW_ANTHRALITE.get());
               if (ModCompat.isCreateLoaded()) {
                  pOutput.accept((ItemLike)ModItems.CRUSHED_RAW_ANTHRALITE.get());
               }

               if (ModCompat.isIELoaded() || ModCompat.isMekanismLoaded()) {
                  pOutput.accept((ItemLike)ModItems.ANTHRALITE_DUST.get());
               }

               if (ModCompat.isMekanismLoaded()) {
                  pOutput.accept((ItemLike)ModItems.DIRTY_DUST_ANTHRALITE.get());
                  pOutput.accept((ItemLike)ModItems.CLUMP_ANTHRALITE.get());
                  pOutput.accept((ItemLike)ModItems.SHARD_ANTHRALITE.get());
               }

               pOutput.accept((ItemLike)ModItems.ANTHRALITE_INGOT.get());
               pOutput.accept((ItemLike)ModItems.ANTHRALITE_NUGGET.get());
               pOutput.accept((ItemLike)ModItems.ANCIENT_BRASS.get());
               pOutput.accept((ItemLike)ModItems.TREATED_IRON_BLEND.get());
               pOutput.accept((ItemLike)ModItems.TREATED_IRON_INGOT.get());
               pOutput.accept((ItemLike)ModItems.TREATED_IRON_NUGGET.get());
               pOutput.accept((ItemLike)ModItems.TREATED_BRASS_BLEND.get());
               pOutput.accept((ItemLike)ModItems.TREATED_BRASS_INGOT.get());
               pOutput.accept((ItemLike)ModItems.DIAMOND_STEEL_BLEND.get());
               pOutput.accept((ItemLike)ModItems.DEPLETED_DIAMOND_STEEL_INGOT.get());
               pOutput.accept((ItemLike)ModItems.DIAMOND_STEEL_INGOT.get());
               pOutput.accept((ItemLike)ModItems.NETHERITE_SCRAP_CHUNK.get());
               pOutput.accept((ItemLike)ModItems.NETHER_STAR_FRAGMENT.get());
               pOutput.accept((ItemLike)ModItems.STANDARD_BULLET.get());
               pOutput.accept((ItemLike)ModItems.ADVANCED_BULLET.get());
               pOutput.accept((ItemLike)ModItems.BLANK_MOLD.get());
               pOutput.accept((ItemLike)ModItems.SMALL_CASING_MOLD.get());
               pOutput.accept((ItemLike)ModItems.MEDIUM_CASING_MOLD.get());
               pOutput.accept((ItemLike)ModItems.LARGE_CASING_MOLD.get());
               pOutput.accept((ItemLike)ModItems.BULLET_MOLD.get());
               pOutput.accept((ItemLike)ModItems.GUN_PARTS_MOLD.get());
               pOutput.accept((ItemLike)ModItems.DISC_MOLD.get());
               pOutput.accept((ItemLike)ModItems.TURRET_PLATFORM.get());
               pOutput.accept((ItemLike)ModItems.WHITE_FLAG.get());
               pOutput.accept((ItemLike)ModItems.FLARE_PISTOL.get());
               pOutput.accept((ItemLike)ModItems.ANTIQUE_FLARE.get());
               pOutput.accept((ItemLike)ModItems.FRONTIER_FLARE.get());
               pOutput.accept((ItemLike)ModItems.COPPER_FLARE.get());
               pOutput.accept((ItemLike)ModItems.IRON_FLARE.get());
               pOutput.accept((ItemLike)ModItems.WRECKER_FLARE.get());
               pOutput.accept((ItemLike)ModItems.OCEAN_FLARE.get());
               pOutput.accept((ItemLike)ModItems.GOLD_FLARE.get());
               pOutput.accept((ItemLike)ModItems.SCULK_FLARE.get());
               pOutput.accept((ItemLike)ModItems.TREATED_BRASS_FLARE.get());
               pOutput.accept((ItemLike)ModItems.DIAMOND_STEEL_FLARE.get());
               pOutput.accept((ItemLike)ModItems.BLUEPRINT_SCRAP.get());
               pOutput.accept((ItemLike)ModItems.ANTIQUE_BLUEPRINT.get());
               pOutput.accept((ItemLike)ModItems.FRONTIER_BLUEPRINT.get());
               pOutput.accept((ItemLike)ModItems.COPPER_BLUEPRINT.get());
               pOutput.accept((ItemLike)ModItems.IRON_BLUEPRINT.get());
               pOutput.accept((ItemLike)ModItems.WRECKER_BLUEPRINT.get());
               pOutput.accept((ItemLike)ModItems.TREATED_BRASS_BLUEPRINT.get());
               pOutput.accept((ItemLike)ModItems.DIAMOND_STEEL_BLUEPRINT.get());
               pOutput.accept((ItemLike)ModItems.OCEAN_BLUEPRINT.get());
               pOutput.accept((ItemLike)ModItems.PIGLIN_BLUEPRINT.get());
               pOutput.accept((ItemLike)ModItems.DEEP_DARK_BLUEPRINT.get());
               pOutput.accept((ItemLike)ModItems.END_BLUEPRINT.get());
               pOutput.accept((ItemLike)ModItems.SCORCHED_BLUEPRINT.get());
               pOutput.accept((ItemLike)ModItems.EXO_SUIT_BLUEPRINT.get());
               pOutput.accept((ItemLike)ModItems.RUSTY_MEDAL.get());
               pOutput.accept((ItemLike)ModItems.LABOR_TROPHY.get());
               pOutput.accept((ItemLike)ModItems.SNAPPED_COGWHEEL.get());
               pOutput.accept((ItemLike)ModItems.LEVIATHAN_TOOTH.get());
               pOutput.accept((ItemLike)ModItems.CERIMONIAL_COD.get());
               pOutput.accept((ItemLike)ModItems.COG_HEART.get());
               pOutput.accept((ItemLike)ModItems.GOLD_IDOL.get());
               pOutput.accept((ItemLike)ModItems.SCULK_TOME.get());
               pOutput.accept((ItemLike)ModItems.SHULKER_CORE.get());
               pOutput.accept((ItemLike)ModItems.SMALL_COPPER_CASING.get());
               pOutput.accept((ItemLike)ModItems.MEDIUM_COPPER_CASING.get());
               pOutput.accept((ItemLike)ModItems.SMALL_IRON_CASING.get());
               pOutput.accept((ItemLike)ModItems.LARGE_IRON_CASING.get());
               pOutput.accept((ItemLike)ModItems.EMPTY_CELL.get());
               pOutput.accept((ItemLike)ModItems.SMALL_DIAMOND_STEEL_CASING.get());
               pOutput.accept((ItemLike)ModItems.MEDIUM_DIAMOND_STEEL_CASING.get());
               pOutput.accept((ItemLike)ModItems.SMALL_BRASS_CASING.get());
               pOutput.accept((ItemLike)ModItems.MEDIUM_BRASS_CASING.get());
               pOutput.accept((ItemLike)ModItems.LARGE_BRASS_CASING.get());
               pOutput.accept((ItemLike)ModItems.SHULKER_CASING.get());
               pOutput.accept((ItemLike)ModItems.POWDER_AND_BALL.get());
               pOutput.accept((ItemLike)ModItems.GRAPESHOT.get());
               pOutput.accept((ItemLike)ModItems.COMPACT_COPPER_ROUND.get());
               pOutput.accept((ItemLike)ModItems.STANDARD_COPPER_ROUND.get());
               pOutput.accept((ItemLike)ModItems.RAMROD_ROUND.get());
               pOutput.accept((ItemLike)ModItems.FROG_DART.get());
               pOutput.accept((ItemLike)ModItems.HOG_ROUND.get());
               pOutput.accept((ItemLike)ModItems.COMPACT_ADVANCED_ROUND.get());
               pOutput.accept((ItemLike)ModItems.ADVANCED_ROUND.get());
               pOutput.accept((ItemLike)ModItems.SHATTER_ROUND.get());
               pOutput.accept((ItemLike)ModItems.KRAHG_ROUND.get());
               pOutput.accept((ItemLike)ModItems.BEOWULF_ROUND.get());
               pOutput.accept((ItemLike)ModItems.GIBBS_ROUND.get());
               pOutput.accept((ItemLike)ModItems.SHOTGUN_SHELL.get());
               pOutput.accept((ItemLike)ModItems.BLAZE_FUEL.get());
               pOutput.accept((ItemLike)ModItems.BEARPACK_SHELL.get());
               pOutput.accept((ItemLike)ModItems.SHOCK_CELL.get());
               pOutput.accept((ItemLike)ModItems.ENERGY_CELL.get());
               pOutput.accept((ItemLike)ModItems.SCULK_CELL.get());
               pOutput.accept((ItemLike)ModItems.SHULKSHOT.get());
               pOutput.accept((ItemLike)ModItems.SYRINGE.get());
               pOutput.accept((ItemLike)ModItems.MICROJET.get());
               pOutput.accept((ItemLike)ModItems.HE_GRENADE_ROUND.get());
               pOutput.accept((ItemLike)ModItems.FIRE_GRENADE_ROUND.get());
               pOutput.accept((ItemLike)ModItems.GAS_GRENADE_ROUND.get());
               pOutput.accept((ItemLike)ModItems.BOUNCY_GRENADE_ROUND.get());
               pOutput.accept((ItemLike)ModItems.ROCKET.get());
               pOutput.accept((ItemLike)ModItems.OSBORNE_SLUG.get());
               pOutput.accept((ItemLike)ModItems.PISTOL_AMMO_BOX.get());
               pOutput.accept((ItemLike)ModItems.RIFLE_AMMO_BOX.get());
               pOutput.accept((ItemLike)ModItems.SHOTGUN_AMMO_BOX.get());
               pOutput.accept((ItemLike)ModItems.MAGNUM_AMMO_BOX.get());
               pOutput.accept((ItemLike)ModItems.ENERGY_AMMO_BOX.get());
               pOutput.accept((ItemLike)ModItems.ROCKET_AMMO_BOX.get());
               pOutput.accept((ItemLike)ModItems.SPECIAL_AMMO_BOX.get());
               pOutput.accept((ItemLike)ModItems.CREATIVE_AMMO_BOX.get());
               pOutput.accept((ItemLike)ModItems.EMPTY_CASING_POUCH.get());
               pOutput.accept((ItemLike)ModItems.DISHES_POUCH.get());
               pOutput.accept((ItemLike)ModItems.ROCK_POUCH.get());
               ModCreativeModeTabs.CreativeTabHelper.addAirCanisterWithFullAir(pOutput, (Item)ModItems.AIR_CANISTER.get());
               ModCreativeModeTabs.CreativeTabHelper.addAirCanisterWithFullAir(pOutput, (Item)ModItems.REINFORCED_AIR_CANISTER.get());
               ModCreativeModeTabs.CreativeTabHelper.addAirCanisterWithFullAir(pOutput, (Item)ModItems.CREATIVE_AIR_CANISTER.get());
               pOutput.accept((ItemLike)ModItems.COPPER_GUN_FRAME.get());
               pOutput.accept((ItemLike)ModItems.IRON_GUN_FRAME.get());
               pOutput.accept((ItemLike)ModItems.TREATED_IRON_GUN_FRAME.get());
               pOutput.accept((ItemLike)ModItems.TREATED_BRASS_GUN_FRAME.get());
               pOutput.accept((ItemLike)ModItems.DIAMOND_STEEL_GUN_FRAME.get());
               pOutput.accept((ItemLike)ModItems.SCORCHED_GUN_FRAME.get());
               pOutput.accept((ItemLike)ModItems.GUN_PARTS.get());
               pOutput.accept((ItemLike)ModItems.HEAVY_GUN_PARTS.get());
               pOutput.accept((ItemLike)ModItems.FIRING_UNIT.get());
               pOutput.accept((ItemLike)ModItems.RAPID_FIRING_UNIT.get());
               pOutput.accept((ItemLike)ModItems.STONE_GUN_BARREL.get());
               pOutput.accept((ItemLike)ModItems.GUN_BARREL.get());
               pOutput.accept((ItemLike)ModItems.HEAVY_GUN_BARREL.get());
               pOutput.accept((ItemLike)ModItems.GUN_GRIP.get());
               pOutput.accept((ItemLike)ModItems.GUN_MAGAZINE.get());
               pOutput.accept((ItemLike)ModItems.CHARGED_AMETHYST_SHARD.get());
               pOutput.accept((ItemLike)ModItems.EMPTY_TANK.get());
               pOutput.accept((ItemLike)ModItems.EMPTY_CORE.get());
               pOutput.accept((ItemLike)ModItems.ENERGY_CORE.get());
               pOutput.accept((ItemLike)ModItems.DEPLETED_ENERGY_CORE.get());
               pOutput.accept((ItemLike)ModItems.PLASMA_CORE.get());
               pOutput.accept((ItemLike)ModItems.EMPTY_BLASPHEMY.get());
               pOutput.accept((ItemLike)ModItems.COPPER_DISC.get());
               pOutput.accept((ItemLike)ModItems.GRENADE.get());
               pOutput.accept((ItemLike)ModItems.STUN_GRENADE.get());
               pOutput.accept((ItemLike)ModItems.SHOTBALL.get());
               pOutput.accept((ItemLike)ModItems.MOLOTOV_COCKTAIL.get());
               pOutput.accept((ItemLike)ModItems.HELLFIRE_BOMB.get());
               pOutput.accept((ItemLike)ModItems.CHOKE_BOMB.get());
               pOutput.accept((ItemLike)ModItems.SWARM_BOMB.get());
               pOutput.accept((ItemLike)ModItems.NAIL_BOMB.get());
               pOutput.accept((ItemLike)ModItems.GAS_GRENADE.get());
               pOutput.accept((ItemLike)ModItems.BEACON_GRENADE.get());
               pOutput.accept((ItemLike)ModItems.COLD_PACK.get());
               pOutput.accept((ItemLike)ModItems.BASIC_POULTICE.get());
               pOutput.accept((ItemLike)ModItems.HONEY_SULFUR_POULTICE.get());
               pOutput.accept((ItemLike)ModItems.ENCHANTED_BANDAGE.get());
               pOutput.accept((ItemLike)ModItems.DRAGON_SALVE.get());
               pOutput.accept((ItemLike)ModItems.WEIRD_FLESH.get());
               pOutput.accept((ItemLike)ModItems.REFLEX_SIGHT.get());
               pOutput.accept((ItemLike)ModItems.LASER_SIGHT.get());
               pOutput.accept((ItemLike)ModItems.MEDIUM_SCOPE.get());
               pOutput.accept((ItemLike)ModItems.LONG_SCOPE.get());
               pOutput.accept((ItemLike)ModItems.LIGHT_STOCK.get());
               pOutput.accept((ItemLike)ModItems.WEIGHTED_STOCK.get());
               pOutput.accept((ItemLike)ModItems.WOODEN_STOCK.get());
               pOutput.accept((ItemLike)ModItems.BUMP_STOCK.get());
               pOutput.accept((ItemLike)ModItems.SILENCER.get());
               pOutput.accept((ItemLike)ModItems.ADVANCED_SILENCER.get());
               pOutput.accept((ItemLike)ModItems.EXTENDED_BARREL.get());
               pOutput.accept((ItemLike)ModItems.MUZZLE_BRAKE.get());
               pOutput.accept((ItemLike)ModItems.LIGHT_GRIP.get());
               pOutput.accept((ItemLike)ModItems.VERTICAL_GRIP.get());
               pOutput.accept((ItemLike)ModItems.IRON_BAYONET.get());
               pOutput.accept((ItemLike)ModItems.ANTHRALITE_BAYONET.get());
               pOutput.accept((ItemLike)ModItems.DIAMOND_BAYONET.get());
               pOutput.accept((ItemLike)ModItems.NETHERITE_BAYONET.get());
               pOutput.accept((ItemLike)ModItems.EXTENDED_MAG.get());
               pOutput.accept((ItemLike)ModItems.SPEED_MAG.get());
               pOutput.accept((ItemLike)ModItems.MASS_PRODUCTION_MUSIC_DISC.get());
               pOutput.accept((ItemLike)ModItems.MASS_DESTRUCTION_MUSIC_DISC.get());
               pOutput.accept((ItemLike)ModItems.MASS_DESTRUCTION_EXTENDED_MUSIC_DISC.get());
               pOutput.accept((ItemLike)ModItems.TEAM_LOG.get());
               pOutput.accept((ItemLike)ModItems.ENEMY_LOG.get());
               pOutput.accept((ItemLike)ModItems.SCAMP_PACKAGE.get());
               pOutput.accept((ItemLike)ModItems.VIVENTRUM_PACKAGE.get());
               pOutput.accept((ItemLike)ModItems.COG_LOCATOR.get());
               pOutput.accept((ItemLike)ModItems.THE_PACT.get());
               pOutput.accept((ItemLike)ModItems.VICIOUS_ACID_BUCKET.get());
               pOutput.accept((ItemLike)ModItems.COG_KNIGHT_SPAWN_EGG.get());
               pOutput.accept((ItemLike)ModItems.TRAUMA_UNIT_SPAWN_EGG.get());
               pOutput.accept((ItemLike)ModItems.COG_MINION_SPAWN_EGG.get());
               pOutput.accept((ItemLike)ModItems.SKY_CARRIER_SPAWN_EGG.get());
               pOutput.accept((ItemLike)ModItems.SUPPLY_SCAMP_SPAWN_EGG.get());
               pOutput.accept((ItemLike)ModItems.DISSIDENT_SPAWN_EGG.get());
               pOutput.accept((ItemLike)ModItems.PRAETOR_SPAWN_EGG.get());
               pOutput.accept((ItemLike)ModItems.SULFURHEAD_SPAWN_EGG.get());
               pOutput.accept((ItemLike)ModItems.ADJUDICATOR_SPAWN_EGG.get());
               pOutput.accept((ItemLike)ModItems.SUBJUGATOR_SPAWN_EGG.get());
               pOutput.accept((ItemLike)ModItems.VIVENTRUM_SPAWN_EGG.get());
               pOutput.accept((ItemLike)ModItems.BLUNDERER_SPAWN_EGG.get());
               pOutput.accept((ItemLike)ModItems.HORNLIN_SPAWN_EGG.get());
               pOutput.accept((ItemLike)ModItems.ZOMBIFIED_HORNLIN_SPAWN_EGG.get());
               pOutput.accept((ItemLike)ModItems.MOTHER_GHAST_SPAWN_EGG.get());
               pOutput.accept((ItemLike)ModItems.FINFORCER_SPAWN_EGG.get());
               pOutput.accept((ItemLike)ModItems.HIVE_SPAWN_EGG.get());
               pOutput.accept((ItemLike)ModItems.SWARM_SPAWN_EGG.get());
               pOutput.accept((ItemLike)ModItems.SIGNAL_BEACON_SPAWN_EGG.get());
               pOutput.accept((ItemLike)ModItems.SCAMP_TANK_SPAWN_EGG.get());
               pOutput.accept((ItemLike)ModItems.SCAMPLER_SPAWN_EGG.get());
            })
            .build()
   );
   public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SCORCHED_BLOCKS_TAB = CREATIVE_MODE_TABS.register(
      "scorched_blocks_tab",
      () -> CreativeModeTab.builder()
            .icon(() -> new ItemStack((ItemLike)ModBlocks.ANTHRALITE_BLOCK.get()))
            .title(Component.translatable("creativetab.scorched_blocks_tab"))
            .displayItems((pParameters, pOutput) -> {
               pOutput.accept((ItemLike)ModBlocks.ANTHRALITE_ORE.get());
               pOutput.accept((ItemLike)ModBlocks.DEEPSLATE_ANTHRALITE_ORE.get());
               pOutput.accept((ItemLike)ModBlocks.ANTHRALITE_BLOCK.get());
               pOutput.accept((ItemLike)ModBlocks.RAW_ANTHRALITE_BLOCK.get());
               pOutput.accept((ItemLike)ModBlocks.SULFUR_ORE.get());
               pOutput.accept((ItemLike)ModBlocks.DEEPSLATE_SULFUR_ORE.get());
               pOutput.accept((ItemLike)ModBlocks.NETHER_SULFUR_ORE.get());
               pOutput.accept((ItemLike)ModBlocks.VEHEMENT_COAL_ORE.get());
               pOutput.accept((ItemLike)ModBlocks.RICH_PHOSPHORITE.get());
               pOutput.accept((ItemLike)ModBlocks.RAW_PHOSPHOR_BLOCK.get());
               pOutput.accept((ItemLike)ModBlocks.SULFUR_BLOCK.get());
               pOutput.accept((ItemLike)ModBlocks.ANCIENT_BRASS_BLOCK.get());
               pOutput.accept((ItemLike)ModBlocks.TREATED_IRON_BLOCK.get());
               pOutput.accept((ItemLike)ModBlocks.TREATED_BRASS_BLOCK.get());
               pOutput.accept((ItemLike)ModBlocks.DIAMOND_STEEL_BLOCK.get());
               pOutput.accept((ItemLike)ModBlocks.VEHEMENT_COAL_BLOCK.get());
               pOutput.accept((ItemLike)ModBlocks.PLASMA_BLOCK.get());
               pOutput.accept((ItemLike)ModBlocks.SCORCHED_BLOCK.get());
               pOutput.accept((ItemLike)ModBlocks.NITER_BLOCK.get());
               pOutput.accept((ItemLike)ModBlocks.NITER_GLASS.get());
               pOutput.accept((ItemLike)ModBlocks.WHITE_NITER_GLASS.get());
               pOutput.accept((ItemLike)ModBlocks.LIGHT_GRAY_NITER_GLASS.get());
               pOutput.accept((ItemLike)ModBlocks.GRAY_NITER_GLASS.get());
               pOutput.accept((ItemLike)ModBlocks.BLACK_NITER_GLASS.get());
               pOutput.accept((ItemLike)ModBlocks.BROWN_NITER_GLASS.get());
               pOutput.accept((ItemLike)ModBlocks.RED_NITER_GLASS.get());
               pOutput.accept((ItemLike)ModBlocks.ORANGE_NITER_GLASS.get());
               pOutput.accept((ItemLike)ModBlocks.YELLOW_NITER_GLASS.get());
               pOutput.accept((ItemLike)ModBlocks.LIME_NITER_GLASS.get());
               pOutput.accept((ItemLike)ModBlocks.GREEN_NITER_GLASS.get());
               pOutput.accept((ItemLike)ModBlocks.CYAN_NITER_GLASS.get());
               pOutput.accept((ItemLike)ModBlocks.LIGHT_BLUE_NITER_GLASS.get());
               pOutput.accept((ItemLike)ModBlocks.BLUE_NITER_GLASS.get());
               pOutput.accept((ItemLike)ModBlocks.PURPLE_NITER_GLASS.get());
               pOutput.accept((ItemLike)ModBlocks.MAGENTA_NITER_GLASS.get());
               pOutput.accept((ItemLike)ModBlocks.PINK_NITER_GLASS.get());
               pOutput.accept((ItemLike)ModBlocks.PHOSPHORITE.get());
               pOutput.accept((ItemLike)ModBlocks.SMOOTH_PHOSPHORITE.get());
               pOutput.accept((ItemLike)ModBlocks.POLISHED_PHOSPHORITE.get());
               pOutput.accept((ItemLike)ModBlocks.PHOSPHORITE_BRICKS.get());
               pOutput.accept((ItemLike)ModBlocks.CRACKED_PHOSPHORITE_BRICKS.get());
               pOutput.accept((ItemLike)ModBlocks.PHOSPHORITE_BRICK_STAIRS.get());
               pOutput.accept((ItemLike)ModBlocks.PHOSPHORITE_BRICK_SLAB.get());
               pOutput.accept((ItemLike)ModBlocks.PHOSPHORITE_BRICK_WALL.get());
               pOutput.accept((ItemLike)ModBlocks.ASGHARIAN_BRICKS.get());
               pOutput.accept((ItemLike)ModBlocks.ASGHARIAN_BRICK_STAIRS.get());
               pOutput.accept((ItemLike)ModBlocks.ASGHARIAN_BRICK_SLAB.get());
               pOutput.accept((ItemLike)ModBlocks.ASGHARIAN_BRICK_WALL.get());
               pOutput.accept((ItemLike)ModBlocks.ASGHARIAN_PILLAR.get());
               pOutput.accept((ItemLike)ModBlocks.CRACKED_ASGHARIAN_BRICKS.get());
               pOutput.accept((ItemLike)ModBlocks.MOSSY_ASGHARIAN_BRICKS.get());
               pOutput.accept((ItemLike)ModBlocks.CHISELED_ASGHARIAN_BRICKS.get());
               pOutput.accept((ItemLike)ModBlocks.POLISHED_ASGHARIAN_PANEL.get());
               pOutput.accept((ItemLike)ModBlocks.ASGHARIAN_TILES.get());
               pOutput.accept((ItemLike)ModBlocks.CRACKED_ASGHARIAN_TILES.get());
               pOutput.accept((ItemLike)ModBlocks.MOSSY_ASGHARIAN_TILES.get());
               pOutput.accept((ItemLike)ModBlocks.REINFORCED_ASGHARIAN_TILES.get());
               pOutput.accept((ItemLike)ModBlocks.ANTHRALITE_PLATES.get());
               pOutput.accept((ItemLike)ModBlocks.ANTHRALITE_TILES.get());
               pOutput.accept((ItemLike)ModBlocks.ANTHRALITE_TILES_STAIRS.get());
               pOutput.accept((ItemLike)ModBlocks.ANTHRALITE_TILES_SLAB.get());
               pOutput.accept((ItemLike)ModBlocks.CUT_ANTHRALITE.get());
               pOutput.accept((ItemLike)ModBlocks.CUT_ANTHRALITE_STAIRS.get());
               pOutput.accept((ItemLike)ModBlocks.CUT_ANTHRALITE_SLAB.get());
               pOutput.accept((ItemLike)ModBlocks.ANTHRALITE_LAMP.get());
               pOutput.accept((ItemLike)ModBlocks.ANTHRALITE_PILLAR.get());
               pOutput.accept((ItemLike)ModBlocks.CHISELED_ANTHRALITE_BLOCK.get());
               pOutput.accept((ItemLike)ModBlocks.ANTHRALITE_GRATE.get());
               pOutput.accept((ItemLike)ModBlocks.ANTHRALITE_GRATE_PANE.get());
               pOutput.accept((ItemLike)ModBlocks.CHISELED_TREATED_IRON_BLOCK.get());
               pOutput.accept((ItemLike)ModBlocks.TREATED_IRON_PLATES.get());
               pOutput.accept((ItemLike)ModBlocks.CUT_TREATED_IRON.get());
               pOutput.accept((ItemLike)ModBlocks.CUT_TREATED_IRON_STAIRS.get());
               pOutput.accept((ItemLike)ModBlocks.CUT_TREATED_IRON_SLAB.get());
               pOutput.accept((ItemLike)ModBlocks.TREATED_IRON_LAMP.get());
               pOutput.accept((ItemLike)ModBlocks.TREATED_IRON_BARS.get());
               pOutput.accept((ItemLike)ModBlocks.TREATED_IRON_GRATE.get());
               pOutput.accept((ItemLike)ModBlocks.TREATED_IRON_GRATE_PANE.get());
               pOutput.accept((ItemLike)ModBlocks.TREATED_BRASS_PLATES.get());
               pOutput.accept((ItemLike)ModBlocks.CUT_TREATED_BRASS.get());
               pOutput.accept((ItemLike)ModBlocks.CUT_TREATED_BRASS_STAIRS.get());
               pOutput.accept((ItemLike)ModBlocks.CUT_TREATED_BRASS_SLAB.get());
               pOutput.accept((ItemLike)ModBlocks.TREATED_BRASS_TILES.get());
               pOutput.accept((ItemLike)ModBlocks.TREATED_BRASS_TILES_STAIRS.get());
               pOutput.accept((ItemLike)ModBlocks.TREATED_BRASS_TILES_SLAB.get());
               pOutput.accept((ItemLike)ModBlocks.CHISELED_TREATED_BRASS_BLOCK.get());
               pOutput.accept((ItemLike)ModBlocks.TREATED_BRASS_LAMP.get());
               pOutput.accept((ItemLike)ModBlocks.TREATED_BRASS_GRATE.get());
               pOutput.accept((ItemLike)ModBlocks.TREATED_BRASS_GRATE_PANE.get());
               pOutput.accept((ItemLike)ModBlocks.DIAMOND_STEEL_PANEL.get());
               pOutput.accept((ItemLike)ModBlocks.CHISELED_DIAMOND_STEEL_BLOCK.get());
               pOutput.accept((ItemLike)ModBlocks.DIAMOND_STEEL_TILES.get());
               pOutput.accept((ItemLike)ModBlocks.DIAMOND_STEEL_TILES_STAIRS.get());
               pOutput.accept((ItemLike)ModBlocks.DIAMOND_STEEL_TILES_SLAB.get());
               pOutput.accept((ItemLike)ModBlocks.CUT_DIAMOND_STEEL.get());
               pOutput.accept((ItemLike)ModBlocks.CUT_DIAMOND_STEEL_STAIRS.get());
               pOutput.accept((ItemLike)ModBlocks.CUT_DIAMOND_STEEL_SLAB.get());
               pOutput.accept((ItemLike)ModBlocks.DIAMOND_STEEL_LAMP.get());
               pOutput.accept((ItemLike)ModBlocks.DIAMOND_STEEL_PILLAR.get());
               pOutput.accept((ItemLike)ModBlocks.DIAMOND_STEEL_BARS.get());
               pOutput.accept((ItemLike)ModBlocks.DIAMOND_STEEL_GRATE.get());
               pOutput.accept((ItemLike)ModBlocks.DIAMOND_STEEL_GRATE_PANE.get());
               pOutput.accept((ItemLike)ModBlocks.SANDBAG.get());
               pOutput.accept((ItemLike)ModBlocks.GUANO_CANDLE.get());
               pOutput.accept((ItemLike)ModBlocks.SUPPLY_CRATE.get());
               pOutput.accept((ItemLike)ModBlocks.POWDER_KEG.get());
               pOutput.accept((ItemLike)ModBlocks.NITRO_KEG.get());
               pOutput.accept((ItemLike)ModBlocks.PENETRATOR.get());
               pOutput.accept((ItemLike)ModBlocks.ADVANCED_COMPOSTER.get());
               pOutput.accept((ItemLike)ModBlocks.GEOTHERMAL_VENT.get());
               pOutput.accept((ItemLike)ModBlocks.SULFUR_VENT.get());
               pOutput.accept((ItemLike)ModBlocks.VENT_COLLECTOR.get());
               pOutput.accept((ItemLike)ModBlocks.CRYONITER.get());
               pOutput.accept((ItemLike)ModBlocks.THERMOLITH.get());
               pOutput.accept((ItemLike)ModBlocks.POLAR_GENERATOR.get());
               pOutput.accept((ItemLike)ModBlocks.LIGHTNING_BATTERY.get());
               pOutput.accept((ItemLike)ModBlocks.LIGHTNING_ROD_CONNECTOR.get());
               pOutput.accept((ItemLike)ModBlocks.GUN_BENCH.get());
               pOutput.accept((ItemLike)ModBlocks.MACERATOR.get());
               pOutput.accept((ItemLike)ModBlocks.MECHANICAL_PRESS.get());
               pOutput.accept((ItemLike)ModBlocks.POWERED_MACERATOR.get());
               pOutput.accept((ItemLike)ModBlocks.POWERED_MECHANICAL_PRESS.get());
               pOutput.accept((ItemLike)ModBlocks.GUN_SHELF.get());
               pOutput.accept((ItemLike)ModBlocks.AMMO_BOX.get());
               pOutput.accept((ItemLike)ModBlocks.PLASMA_LANTERN.get());
               pOutput.accept((ItemLike)ModBlocks.CHARGED_AMETHYST_RELAY.get());
               pOutput.accept((ItemLike)ModBlocks.SHOCK_COIL.get());
               pOutput.accept((ItemLike)ModBlocks.MINE_UNIT.get());
               pOutput.accept((ItemLike)ModBlocks.BASIC_TURRET.get());
               pOutput.accept((ItemLike)ModBlocks.AUTO_TURRET.get());
               pOutput.accept((ItemLike)ModBlocks.SHOTGUN_TURRET.get());
               pOutput.accept((ItemLike)ModBlocks.SNIPER_TURRET.get());
               pOutput.accept((ItemLike)ModBlocks.HOSTILE_TURRET_TARGETING_BLOCK.get());
               pOutput.accept((ItemLike)ModBlocks.PLAYER_TURRET_TARGETING_BLOCK.get());
               pOutput.accept((ItemLike)ModBlocks.TURRET_TARGETING_BLOCK.get());
               pOutput.accept((ItemLike)ModBlocks.FIRE_RATE_TURRET_MODULE.get());
               pOutput.accept((ItemLike)ModBlocks.DAMAGE_TURRET_MODULE.get());
               pOutput.accept((ItemLike)ModBlocks.RANGE_TURRET_MODULE.get());
               pOutput.accept((ItemLike)ModBlocks.SHELL_CATCHER_TURRET_MODULE.get());
               pOutput.accept((ItemLike)ModBlocks.AMMO_TURRET_MODULE.get());
               pOutput.accept((ItemLike)ModBlocks.ENEMY_TURRET.get());
               pOutput.accept((ItemLike)ModBlocks.MOB_TRAP.get());
               pOutput.accept((ItemLike)ModBlocks.FELIX_MEMORIAL.get());
            })
            .build()
   );

   public ModCreativeModeTabs() {
      super();
   }

   public static void register(IEventBus eventBus) {
      CREATIVE_MODE_TABS.register(eventBus);
   }

   public static class CreativeTabHelper {
      public CreativeTabHelper() {
         super();
      }

      public static void addItemWithFullAmmo(Output output, Item item) {
         if (item instanceof GunItem gunItem) {
            ItemStack stack = new ItemStack(gunItem);
            NbtHelper.getOrCreateTag(stack).putInt("AmmoCount", gunItem.getGun().getReloads().getMaxAmmo());
            output.accept(stack);
         } else {
            output.accept(item);
         }
      }

      public static void addGunOrEnergyWeaponWithFullResources(Output output, Item item) {
         ItemStack stack = new ItemStack(item);
         if (item instanceof GunItem gunItem) {
            NbtHelper.getOrCreateTag(stack).putInt("AmmoCount", gunItem.getGun().getReloads().getMaxAmmo());
         }

         if (item instanceof EnergyGunItem energyGunItem) {
            NbtHelper.getOrCreateTag(stack).putInt("Energy", energyGunItem.getMaxEnergyStored(stack));
         }

         output.accept(stack);
      }

      public static void addAirCanisterWithFullAir(Output output, Item item) {
         if (item instanceof AirCanisterItem airCanisterItem) {
            ItemStack stack = new ItemStack(airCanisterItem);
            NbtHelper.getOrCreateTag(stack).putInt("AirStored", airCanisterItem.getMaxAirStored(stack));
            output.accept(stack);
         } else {
            output.accept(item);
         }
      }

      public static void addExoSuitCoreItemWithFullEnergy(Output output, Item item) {
         if (item instanceof ExoSuitCoreItem exoSuitCoreItem) {
            ItemStack stack = new ItemStack(exoSuitCoreItem);
            NbtHelper.getOrCreateTag(stack).putInt("Energy", exoSuitCoreItem.getMaxEnergyStored(stack));
            output.accept(stack);
         } else {
            output.accept(item);
         }
      }
   }
}
