package top.ribs.scguns.init;


import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModSounds {
   public static final DeferredRegister<SoundEvent> REGISTER = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, "scguns");
   public static final DeferredHolder<SoundEvent, SoundEvent> LEVER = register("item.lever.lever");
   public static final DeferredHolder<SoundEvent, SoundEvent> MAG_IN = register("item.mag_in.mag_in");
   public static final DeferredHolder<SoundEvent, SoundEvent> MAG_OUT = register("item.mag_out.mag_out");
   public static final DeferredHolder<SoundEvent, SoundEvent> RELOAD_END = register("item.reload_end.reload_end");
   public static final DeferredHolder<SoundEvent, SoundEvent> GUN_RUSTLE = register("item.gun_rustle.gun_rustle");
   public static final DeferredHolder<SoundEvent, SoundEvent> HISS = register("item.gun_sounds.hiss");
   public static final DeferredHolder<SoundEvent, SoundEvent> METAL = register("item.gun_sounds.metal");
   public static final DeferredHolder<SoundEvent, SoundEvent> PUMP = register("item.gun_sounds.pump");
   public static final DeferredHolder<SoundEvent, SoundEvent> PUMP_HALF = register("item.gun_sounds.pump_half");
   public static final DeferredHolder<SoundEvent, SoundEvent> INSERT = register("item.gun_sounds.insert");
   public static final DeferredHolder<SoundEvent, SoundEvent> BOLT_PULL = register("item.bolt_pull.bolt_pull");
   public static final DeferredHolder<SoundEvent, SoundEvent> BOLT_RELEASE = register("item.bolt_release.bolt_release");
   public static final DeferredHolder<SoundEvent, SoundEvent> RACK = register("item.rack.rack");
   public static final DeferredHolder<SoundEvent, SoundEvent> BOLT = register("item.bolt.bolt");
   public static final DeferredHolder<SoundEvent, SoundEvent> SLAP = register("item.slap.slap");
   public static final DeferredHolder<SoundEvent, SoundEvent> MASS_PRODUCTION = register("mass_production");
   public static final DeferredHolder<SoundEvent, SoundEvent> MASS_DESTRUCTION = register("mass_destruction");
   public static final DeferredHolder<SoundEvent, SoundEvent> MASS_DESTRUCTION_EXTENDED = register("mass_destruction_extended");
   public static final DeferredHolder<SoundEvent, SoundEvent> COPPER_GUN_JAM = register("item.rusty_gnat.copper_jam");
   public static final DeferredHolder<SoundEvent, SoundEvent> BLACKPOWDER_FIRE = register("item.blackpowder.fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> RAIL_FIRE = register("item.rail.fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> PING = register("item.ping.ping");
   public static final DeferredHolder<SoundEvent, SoundEvent> JETPACK = register("item.jetpack.fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> JETPACK_LOOP = register("item.jetpack.loop");
   public static final DeferredHolder<SoundEvent, SoundEvent> CARABINE_FIRE = register("item.carabine.fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> NEW_RIFLE_FIRE = register("item.new_rifle.fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> NEW_RIFLE_FIRE_2 = register("item.new_rifle.fire_2");
   public static final DeferredHolder<SoundEvent, SoundEvent> OLD_RIFLE_FIRE = register("item.old_rifle.fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> HEAVY_RIFLE_2_FIRE = register("item.heavy_rifle_2.fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_GUN_FIRE = register("item.machine_gun.fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> WITHER = register("item.wither.wither");
   public static final DeferredHolder<SoundEvent, SoundEvent> AIRGUN_FIRE = register("item.airgun.fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> AIRGUN_FIRE_2 = register("item.airgun.fire_2");
   public static final DeferredHolder<SoundEvent, SoundEvent> BEAM_FIRE = register("item.beam.fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> BRUISER_SILENCED_FIRE = register("item.bruiser.silenced_fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> MAKESHIFT_RIFLE_FIRE = register("item.makeshift_rifle.fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> REVOLVER_FIRE = register("item.revolver.fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> COWBOY_FIRE = register("item.cowboy.fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> SHOCK_FIRE = register("item.shock.fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> SHULKER_FIRE = register("item.shulker.fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> SCULK_FIRE = register("item.sculk.fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> SCORCHED_SNIPER_FIRE = register("item.scorched_sniper.fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> SCORCHED_RIFLE_FIRE = register("item.scorched_rifle.fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> RUSTY_GNAT_FIRE = register("item.rusty_gnat.fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> RUSTY_GNAT_SILENCED_FIRE = register("item.rusty_gnat.silenced_fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> BRASS_SHOTGUN_FIRE = register("item.brass_shotgun.fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> BOOMSTICK_FIRE = register("item.boomstick.fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> BOOMSTICK_SILENCED_FIRE = register("item.boomstick.silenced_fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> COMBAT_SHOTGUN_FIRE = register("item.combat_shotgun.fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> COMBAT_SHOTGUN_SILENCED_FIRE = register("item.combat_shotgun.silenced_fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> BRUISER_FIRE = register("item.bruiser.fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> CANNON_FIRE = register("item.cannon.fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> CANNON_RELOAD = register("item.cannon.reload");
   public static final DeferredHolder<SoundEvent, SoundEvent> MAKESHIFT_RIFLE_SILENCED_FIRE = register("item.makeshift_rifle.silenced_fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> MAKESHIFT_RIFLE_COCK = register("item.makeshift_rifle.cock");
   public static final DeferredHolder<SoundEvent, SoundEvent> SCRAPPER_FIRE = register("item.scrapper.fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> GREASER_SMG_FIRE = register("item.greaser_smg.fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> GYROJET_FIRE = register("item.gyrojet.fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> IRON_RIFLE_FIRE = register("item.iron_rifle.fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> HEAVY_RIFLE_FIRE = register("item.heavy_rifle.fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> BRASS_PISTOL_FIRE = register("item.brass_pistol.fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> PLASMA_FIRE = register("item.plasma.fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> PLASMA_FIRE_2 = register("item.plasma.fire_2");
   public static final DeferredHolder<SoundEvent, SoundEvent> PLASMA_SHOTGUN_FIRE = register("item.plasma_shotgun.fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> GAUSS_FIRE = register("item.gauss.fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> ROCKET_FIRE = register("item.rocket.fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> ROCKET_RIFLE_FIRE = register("item.rocket_rifle.fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> ROCKET_RIFLE_FIRE_2 = register("item.rocket_rifle.fire_2");
   public static final DeferredHolder<SoundEvent, SoundEvent> BRASS_REVOLVER = register("item.brass_revolver.fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> GAUSS_PRE_FIRE = register("item.gauss.pre_fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> GAUSS_RELOAD = register("item.gauss.reload");
   public static final DeferredHolder<SoundEvent, SoundEvent> RAYGUN_FIRE = register("item.raygun.fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> FLAMETHROWER_FIRE = register("item.flamethrower.fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> FLAMETHROWER_FIRE_2 = register("item.flamethrower.fire_2");
   public static final DeferredHolder<SoundEvent, SoundEvent> FLAMETHROWER_PRE_FIRE = register("item.flamethrower.pre_fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> FLAMETHROWER_RELOAD = register("item.flamethrower.reload");
   public static final DeferredHolder<SoundEvent, SoundEvent> LASER_FIRE = register("item.laser.fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> LASER_FIRE_2 = register("item.laser.fire_2");
   public static final DeferredHolder<SoundEvent, SoundEvent> HEAVIER_FIRE = register("item.heavier_rifle.fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> IRON_PISTOL_FIRE = register("item.iron_pistol.fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_PISTOL_RELOAD = register("item.pistol.reload");
   public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_PISTOL_COCK = register("item.pistol.cock");
   public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_GRENADE_PIN = register("item.grenade.pin");
   public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_STUN_GRENADE_EXPLOSION = register("entity.stun_grenade.explosion");
   public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_STUN_GRENADE_RING = register("entity.stun_grenade.ring");
   public static final DeferredHolder<SoundEvent, SoundEvent> UI_WEAPON_ATTACH = register("ui.weapon.attach");
   public static final DeferredHolder<SoundEvent, SoundEvent> SCAMP_HURT = register("entity.scamp.hurt");
   public static final DeferredHolder<SoundEvent, SoundEvent> SCAMP_DIE = register("entity.scamp.die");
   public static final DeferredHolder<SoundEvent, SoundEvent> DISSIDENT_HURT = register("entity.dissident.hurt");
   public static final DeferredHolder<SoundEvent, SoundEvent> DISSIDENT_DIE = register("entity.dissident.die");
   public static final DeferredHolder<SoundEvent, SoundEvent> DISSIDENT_IDLE = register("entity.dissident.idle");
   public static final DeferredHolder<SoundEvent, SoundEvent> PRAETOR_HURT = register("entity.praetor.hurt");
   public static final DeferredHolder<SoundEvent, SoundEvent> PRAETOR_DIE = register("entity.praetor.die");
   public static final DeferredHolder<SoundEvent, SoundEvent> PRAETOR_IDLE = register("entity.praetor.idle");
   public static final DeferredHolder<SoundEvent, SoundEvent> PRAETOR_ROAR = register("entity.praetor.roar");
   public static final DeferredHolder<SoundEvent, SoundEvent> SULFURHEAD_HURT = register("entity.sulfurhead.hurt");
   public static final DeferredHolder<SoundEvent, SoundEvent> SULFURHEAD_DIE = register("entity.sulfurhead.die");
   public static final DeferredHolder<SoundEvent, SoundEvent> SULFURHEAD_IDLE = register("entity.sulfurhead.idle");
   public static final DeferredHolder<SoundEvent, SoundEvent> BULLET_FLYBY = register("bullet.flyby1");

   public ModSounds() {
      super();
   }

   private static DeferredHolder<SoundEvent, SoundEvent> register(String key) {
      return REGISTER.register(key, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scguns", key)));
   }
}
