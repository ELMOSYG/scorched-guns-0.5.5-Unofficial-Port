package top.ribs.scguns;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.EnumValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;
import org.apache.commons.lang3.tuple.Pair;
import top.ribs.scguns.client.SwayType;
import top.ribs.scguns.client.render.crosshair.DotRenderMode;
import top.ribs.scguns.client.screen.ButtonAlignment;

public class Config {
   static final ModConfigSpec clientSpec;
   public static final Config.Client CLIENT;
   static final ModConfigSpec commonSpec;
   public static final Config.Common COMMON;
   static final ModConfigSpec serverSpec;
   public static final Config.Server SERVER;

   public Config() {
      super();
   }

   public static void saveClientConfig() {
      clientSpec.save();
   }

   /**
    * A client-only option read from code that can also run on a <b>server</b> (HANDOFF section 81).
    *
    * <p>The dedicated server never loads {@code scguns-client.toml}, and NeoForge throws for that:
    * {@code ConfigValue.get()} -&gt; {@code getRaw()} -&gt;
    * {@code Preconditions.checkState(loadedConfig != null, "Cannot get config value before config is
    * loaded.")}. Forge 1.20.1 returned the default instead, so 0.5.5 could read a client option from
    * server code harmlessly - which is why the port crashed where the original did not. Inside
    * {@code AIGunEvent.performGunAttack} that throw happened <b>before</b>
    * {@code GunAttackGoal.consumeAmmo}, so a gunner's magazine never went down, the AI never reached its
    * reload branch, and the server died with "Ticking entity".</p>
    *
    * <p>Everything that reads {@link #CLIENT} outside the {@code client} package must go through here;
    * on a client - and in single player, where the client half is loaded in the same JVM - the real value
    * is returned, and on a dedicated server the option's default is, which is what Forge used to
    * return.</p>
    */
   public static <T> T clientOr(ConfigValue<T> value) {
      return clientSpec.isLoaded() ? value.get() : value.getDefault();
   }

   static {
      Pair<Config.Client, ModConfigSpec> clientSpecPair = new Builder().configure(Config.Client::new);
      clientSpec = (ModConfigSpec)clientSpecPair.getRight();
      CLIENT = (Config.Client)clientSpecPair.getLeft();
      Pair<Config.Common, ModConfigSpec> commonSpecPair = new Builder().configure(Config.Common::new);
      commonSpec = (ModConfigSpec)commonSpecPair.getRight();
      COMMON = (Config.Common)commonSpecPair.getLeft();
      Pair<Config.Server, ModConfigSpec> serverSpecPair = new Builder().configure(Config.Server::new);
      serverSpec = (ModConfigSpec)serverSpecPair.getRight();
      SERVER = (Config.Server)serverSpecPair.getLeft();
   }

   public static class AggroMobs {
      public final BooleanValue enabled;
      public final DoubleValue unsilencedRange;
      public final DoubleValue aggroChance;
      public final DoubleValue chainAggroChance;
      public final DoubleValue chainAggroRadius;
      public final ConfigValue<List<? extends String>> exemptEntities;

      public AggroMobs(Builder builder) {
         super();
         builder.comment("Properties relating to mob aggression from gunfire").push("aggro_mobs");
         this.enabled = builder.comment("If true, hostile mobs may become aggressive when guns are fired nearby.").define("enabled", true);
         this.unsilencedRange = builder.comment("Range in which hostile mobs may detect and react to unsilenced gunfire.")
            .defineInRange("unsilencedRange", 20.0, 0.0, Double.MAX_VALUE);
         this.aggroChance = builder.comment("Chance (0.0 to 1.0) that a hostile mob will target the shooter when detecting gunfire.")
            .defineInRange("aggroChance", 0.25, 0.0, 1.0);
         this.chainAggroChance = builder.comment("Chance (0.0 to 1.0) that nearby mobs of the same type will also aggro when one becomes hostile.")
            .defineInRange("chainAggroChance", 0.5, 0.0, 1.0);
         this.chainAggroRadius = builder.comment("Radius in which chain aggro effects can spread to similar mobs.")
            .defineInRange("chainAggroRadius", 8.0, 0.0, 32.0);
         this.exemptEntities = builder.comment(
               "LEGACY: Entities that will not aggro from gunfire. Use the 'scguns:aggro_from_guns' entity tag for better mod compatibility."
            )
            .defineList("exemptMobs", Collections.emptyList(), o -> true);
         builder.pop();
      }
   }

   public static class Blind {
      public final Config.EffectCriteria criteria;
      public final BooleanValue blindMobs;

      public Blind(Builder builder) {
         super();
         builder.comment("Blinding properties of stun grenades").push("blind");
         this.criteria = new Config.EffectCriteria(builder, 15.0, 220, 10, 170.0, 0.75, true);
         this.blindMobs = builder.comment("If true, hostile mobs will be unable to target entities while they are blinded by a stun grenade.")
            .define("blindMobs", true);
         builder.pop();
      }
   }

   public static class Client {
      public final Config.Sounds sounds;
      public final Config.Display display;
      public final Config.Particle particle;
      public final Config.Controls controls;
      public final Config.Experimental experimental;
      public final BooleanValue hideConfigButton;
      public final EnumValue<ButtonAlignment> buttonAlignment;

      public Client(Builder builder) {
         super();
         builder.push("client");
         this.sounds = new Config.Sounds(builder);
         this.display = new Config.Display(builder);
         this.particle = new Config.Particle(builder);
         this.controls = new Config.Controls(builder);
         this.experimental = new Config.Experimental(builder);
         builder.pop();
         this.hideConfigButton = builder.comment("If enabled, hides the config button from the backpack screen").define("hideConfigButton", false);
         this.buttonAlignment = builder.comment("The alignment of the buttons in the backpack inventory screen")
            .defineEnum("buttonAlignment", ButtonAlignment.RIGHT);
      }
   }

   public static class Common {
      public final Config.Gameplay gameplay;
      public final Config.Network network;
      public final Config.GunnerMobs gunnerMobs;
      public final Config.AggroMobs aggroMobs;
      public final Config.FleeingMobs fleeingMobs;
      public final Config.Grenades grenades;
      public final Config.StunGrenades stunGrenades;
      public final Config.ProjectileSpread projectileSpread;
      public final Config.Gunsmith Gunsmith;
      public final Config.ExoSuitCores exoSuitCores;
      public final Config.Raids raids;

      public Common(Builder builder) {
         super();
         builder.push("common");
         this.gameplay = new Config.Gameplay(builder);
         this.network = new Config.Network(builder);
         this.gunnerMobs = new Config.GunnerMobs(builder);
         this.aggroMobs = new Config.AggroMobs(builder);
         this.fleeingMobs = new Config.FleeingMobs(builder);
         this.grenades = new Config.Grenades(builder);
         this.stunGrenades = new Config.StunGrenades(builder);
         this.projectileSpread = new Config.ProjectileSpread(builder);
         this.Gunsmith = new Config.Gunsmith(builder);
         this.exoSuitCores = new Config.ExoSuitCores(builder);
         this.raids = new Config.Raids(builder);
         builder.pop();
      }
   }

   public static class Controls {
      public final DoubleValue aimDownSightSensitivity;
      public final BooleanValue flipControls;

      public Controls(Builder builder) {
         super();
         builder.comment("Properties relating to controls").push("controls");
         this.aimDownSightSensitivity = builder.comment(
               "A value to multiple the mouse sensitivity by when aiming down weapon sights. Go to (Options > Controls > Mouse Settings > ADS Sensitivity) in game to change this!"
            )
            .defineInRange("aimDownSightSensitivity", 0.75, 0.0, 1.0);
         this.flipControls = builder.comment(
               "When enabled, switches the shoot and aim controls of weapons. Due to technical reasons, you won't be able to use offhand items if you enable this setting."
            )
            .define("flipControls", false);
         builder.pop();
      }
   }

   public static class Deafen {
      public final Config.EffectCriteria criteria;
      public final BooleanValue panicMobs;

      public Deafen(Builder builder) {
         super();
         builder.comment("Deafening properties of stun grenades").push("deafen");
         this.criteria = new Config.EffectCriteria(builder, 15.0, 280, 100, 360.0, 0.75, false);
         this.panicMobs = builder.comment("If true, peaceful mobs will panic upon being deafened by a stun grenade.").define("panicMobs", true);
         builder.pop();
      }
   }

   public static class Display {
      public final BooleanValue oldAnimations;
      public final ConfigValue<String> crosshair;
      public final BooleanValue displayGunInfo;
      public final BooleanValue immersiveGunInfo;
      public final BooleanValue cinematicGunEffects;
      public final BooleanValue cooldownIndicator;
      public final BooleanValue weaponSway;
      public final DoubleValue swaySensitivity;
      public final EnumValue<SwayType> swayType;
      public final BooleanValue cameraRollEffect;
      public final DoubleValue cameraRollAngle;
      public final BooleanValue restrictCameraRollToWeapons;
      public final BooleanValue sprintAnimation;
      public final DoubleValue bobbingIntensity;
      public final BooleanValue fireLights;
      public final BooleanValue puritySeals;
      public final DoubleValue dynamicCrosshairBaseSpread;
      public final DoubleValue dynamicCrosshairSpreadMultiplier;
      public final DoubleValue dynamicCrosshairReactivity;
      public final EnumValue<DotRenderMode> dynamicCrosshairDotMode;
      public final BooleanValue onlyRenderDotWhileAiming;
      public final DoubleValue dynamicCrosshairDotThreshold;
      public final DoubleValue dynamicCrosshairMaxScale;
      public final BooleanValue renderArms;
      public final BooleanValue enablePerformanceSulfurCloud;
      public final BooleanValue showProgressionMessages;

      public Display(Builder builder) {
         super();
         builder.comment("Configuration for display related options").push("display");
         this.oldAnimations = builder.comment(
               "If true, uses the old animation poses for weapons. This is only for nostalgic reasons and not recommended to switch back."
            )
            .define("oldAnimations", false);
         this.crosshair = builder.comment(
               "The custom crosshair to use for weapons. Go to (Options > Controls > Mouse Settings > Crosshair) in game to change this!"
            )
            .define("crosshair", "scguns:dynamic");
         this.cooldownIndicator = builder.comment("If enabled, renders a cooldown indicator to make it easier to learn when you fire again.",
               "Kept for config compatibility with 0.5.5, but nothing draws it - and 0.5.5 did not either:",
               "its copy built its own GuiGraphics and never flushed it, so the vertices were never submitted",
               "and the bar was invisible in game (HANDOFF section 48). The option is inert here as well.")
            .define("cooldownIndicator", true);
         this.weaponSway = builder.comment(
               "If enabled, the weapon will sway when the player moves their look direction. This does not affect aiming and is only visual."
            )
            .define("weaponSway", true);
         this.swaySensitivity = builder.comment(
               "The sensistivity of the visual weapon sway when the player moves their look direciton. The higher the value the more sway."
            )
            .defineInRange("swaySensitivity", 0.3, 0.0, 1.0);
         this.swayType = builder.comment("The animation to use for sway. Directional follows the camera better while Drag is more immersive")
            .defineEnum("swayType", SwayType.DRAG);
         this.cameraRollEffect = builder.comment("If enabled, the camera will roll when strafing while holding a gun. This creates a more immersive feeling.")
            .define("cameraRollEffect", true);
         this.cameraRollAngle = builder.comment("When Camera Roll Effect is enabled, this is the absolute maximum angle the roll on the camera can approach.")
            .defineInRange("cameraRollAngle", 1.5, 0.0, 45.0);
         this.restrictCameraRollToWeapons = builder.comment("When enabled, the Camera Roll Effect is only applied when holding a weapon.")
            .define("restrictCameraRollToWeapons", true);
         this.sprintAnimation = builder.comment(
               "Enables the sprinting animation on weapons for better immersion. This only applies to weapons that support a sprinting animation."
            )
            .define("sprintingAnimation", true);
         this.displayGunInfo = builder.comment(
               "If enabled, renders a HUD element displaying the gun's ammo count and ammo capacity, as well as pulse weapon charge."
            )
            .define("displayGunInfo", true);
         this.immersiveGunInfo = builder.comment("If enabled, the HUD will display when inspecting the gun.").define("immersiveGunInfo", false);
         this.bobbingIntensity = builder.comment("The intensity of the custom bobbing animation while holding a gun")
            .defineInRange("bobbingIntensity", 1.0, 0.0, 2.0);
         this.cinematicGunEffects = builder.comment("If enabled, enables cinematic camera effects on guns ").define("cinematicGunEffects", true);
         this.fireLights = builder.comment("If enabled, enables light sources when firing guns").define("fireLights", true);
         this.puritySeals = builder.comment("If enabled, enables purity seals on weapons").define("puritySeals", true);
         this.dynamicCrosshairBaseSpread = builder.comment("The resting size of the Dynamic Crosshair when spread is zero.")
            .defineInRange("dynamicCrosshairBaseSpread", 1.0, 0.0, 5.0);
         this.dynamicCrosshairSpreadMultiplier = builder.comment("The bloom factor of the Dynamic Crosshair when spread increases.")
            .defineInRange("dynamicCrosshairSpreadMultiplier", 1.0, 1.0, 1.5);
         this.dynamicCrosshairReactivity = builder.comment("How reactive the Dynamic Crosshair is to shooting.")
            .defineInRange("dynamicCrosshairReactivity", 2.0, 0.0, 10.0);
         this.dynamicCrosshairDotMode = builder.comment(
               "The rendering mode used for the Dynamic Crosshair's center dot. At Min Spread will only render the dot when gun spread is stable."
            )
            .defineEnum("dynamicCrosshairDotMode", DotRenderMode.AT_MIN_SPREAD);
         this.onlyRenderDotWhileAiming = builder.comment(
               "If true, the Dynamic Crosshair's center dot will only render while aiming. Obeys dynamicCrosshairDotMode, and has no effect when mode is set to Never."
            )
            .define("onlyRenderDotWhileAiming", true);
         this.dynamicCrosshairDotThreshold = builder.comment(
               "The threshold of spread (including modifiers) below which the Dynamic Crosshair's center dot is rendered. Affects the At Min Spread and Threshold modes only."
            )
            .defineInRange("dynamicCrosshairDotThreshold", 0.8, 0.0, 90.0);
         this.dynamicCrosshairMaxScale = builder.comment("The maximum scale factor for the dynamic crosshair when spread is high")
            .defineInRange("dynamicCrosshairMaxScale", 8.0, 1.0, 20.0);
         this.renderArms = builder.comment("If true, renders the player's arms when holding a gun").define("renderArms", true);
         this.enablePerformanceSulfurCloud = builder.comment(
               "If true, enables a performance mode for the sulfur smoke particle which reduces the max number of particles."
            )
            .define("enablePerformanceSulfurCloud", false);
         this.showProgressionMessages = builder.comment("If enabled, shows messages when unlocking new gun tiers and raid levels")
            // 移植有意偏离 0.5.5：原本默认 false，玩家反馈"等级提升没有任何提示"（HANDOFF 40）。
         .define("showProgressionMessages", true);
         builder.pop();
      }
   }

   public static class EffectCriteria {
      public final DoubleValue radius;
      public final IntValue durationMax;
      public final IntValue durationMin;
      public final DoubleValue angleEffect;
      public final DoubleValue angleAttenuationMax;
      public final BooleanValue raytraceOpaqueBlocks;

      public EffectCriteria(
         Builder builder, double radius, int durationMax, int durationMin, double angleEffect, double angleAttenuationMax, boolean raytraceOpaqueBlocks
      ) {
         super();
         builder.push("effect_criteria");
         this.radius = builder.comment("Grenade must be no more than this many meters away to have an effect.")
            .defineInRange("radius", radius, 0.0, Double.MAX_VALUE);
         this.durationMax = builder.comment(
               "Effect will have this duration (in ticks) if the grenade is directly at the player's eyes while looking directly at it."
            )
            .defineInRange("durationMax", durationMax, 0, Integer.MAX_VALUE);
         this.durationMin = builder.comment(
               "Effect will have this duration (in ticks) if the grenade is the maximum distance from the player's eyes while looking directly at it."
            )
            .defineInRange("durationMin", durationMin, 0, Integer.MAX_VALUE);
         this.angleEffect = builder.comment(
               "Angle between the eye/looking direction and the eye/grenade direction must be no more than half this many degrees to have an effect."
            )
            .defineInRange("angleEffect", angleEffect, 0.0, 360.0);
         this.angleAttenuationMax = builder.comment(
               "After duration is attenuated by distance, it will be further attenuated depending on the angle (in degrees) between the eye/looking direction and the eye/grenade direction. This is done by multiplying it by 1 (no attenuation) if the angle is 0; and by this value if the angle is the maximum within the angle of effect."
            )
            .defineInRange("angleAttenuationMax", angleAttenuationMax, 0.0, 1.0);
         this.raytraceOpaqueBlocks = builder.comment(
               "If true, the effect is only applied if the line between the eyes and the grenade does not intersect any non-liquid blocks with an opacity greater than 0."
            )
            .define("raytraceOpaqueBlocks", raytraceOpaqueBlocks);
         builder.pop();
      }
   }

   public static class ExoSuitCores {
      public final IntValue basicCoreCapacity;
      public final IntValue advancedCoreCapacity;
      public final IntValue eliteCoreCapacity;

      public ExoSuitCores(Builder builder) {
         super();
         builder.comment("Properties relating to ExoSuit Core capacities").push("exosuit_cores");
         this.basicCoreCapacity = builder.comment("Energy capacity for Basic ExoSuit Core").defineInRange("basicCoreCapacity", 30000, 1000, 100000);
         this.advancedCoreCapacity = builder.comment("Energy capacity for Advanced ExoSuit Core").defineInRange("advancedCoreCapacity", 60000, 1000, 100000);
         this.eliteCoreCapacity = builder.comment("Energy capacity for Elite ExoSuit Core").defineInRange("eliteCoreCapacity", 70000, 1000, 100000);
         builder.pop();
      }
   }

   public static class Experimental {
      public Experimental(Builder builder) {
         super();
         builder.comment("Experimental options").push("experimental");
         builder.pop();
      }
   }

   public static class FleeingMobs {
      public final BooleanValue enabled;
      public final DoubleValue silencedRange;
      public final DoubleValue unsilencedRange;
      public final ConfigValue<List<? extends String>> fleeingEntities;

      public FleeingMobs(Builder builder) {
         super();
         builder.comment("Properties relating to mob fleeing").push("fleeing_mobs");
         this.enabled = builder.comment("If true, nearby mobs will flee from the firing of guns.").define("enabled", true);
         this.unsilencedRange = builder.comment("Any mobs within a sphere of this radius will flee from the shooter of an unsilenced gun.")
            .defineInRange("unsilencedRange", 20.0, 0.0, Double.MAX_VALUE);
         this.silencedRange = builder.comment("Any mobs within a sphere of this radius will flee from the shooter of a silenced gun.")
            .defineInRange("silencedRange", 5.0, 0.0, Double.MAX_VALUE);
         this.fleeingEntities = builder.comment(
               "LEGACY: Additional entities that will flee from gunshots. Animals are automatically detected. Use the 'scguns:fleeing_from_guns' entity tag instead for better mod compatibility."
            )
            .defineList("fleeingEntities", Arrays.asList("minecraft:villager"), o -> o instanceof String);
         builder.pop();
      }
   }

   public static class Gameplay {
      public final Config.Griefing griefing;
      public final BooleanValue enableFirePlacement;
      public final BooleanValue enableGunDamage;
      public final BooleanValue enableAttachmentDamage;
      public final BooleanValue spawnCasings;
      public final DoubleValue growBoundingBoxAmount;
      public final BooleanValue enableHeadShots;
      public final DoubleValue headShotDamageMultiplier;
      public final BooleanValue ignoreLeaves;
      public final DoubleValue knockbackStrength;
      public final BooleanValue improvedHitboxes;
      public final DoubleValue enemyBulletDamage;
      public final DoubleValue ammoBoxCapacityMultiplier;
      public final IntValue energyProductionRate;
      public final BooleanValue drawAnimation;
      public final BooleanValue toggleADS;
      public final DoubleValue globalDamageMultiplier;
      public final DoubleValue globalTurretDamageMultiplier;
      public final BooleanValue disableVillagerSpawning;
      public final DoubleValue dissidentSpawnChance;
      public final BooleanValue enableAutoReload;
      public final BooleanValue enableSculkPurification;
      public final IntValue playerGunfireVolume;
      public final IntValue mobGunfireVolume;
      public final DoubleValue mobFireRateMultiplier;
      public final DoubleValue mobBurstDelayMultiplier;
      public final DoubleValue mobGunDamageMultiplier;
      public final DoubleValue cogBeaconSpawnChance;
      public final DoubleValue physicsStructureImpulse;
      public final DoubleValue physicsStructureMaxSpeed;
      public final DoubleValue physicsStructureMaxSpin;

      public Gameplay(Builder builder) {
         super();
         builder.comment("Properties relating to gameplay").push("gameplay");
         this.griefing = new Config.Griefing(builder);
         this.drawAnimation = builder.comment("If true, enables the draw animation for weapons").define("drawAnimation", true);
         this.ammoBoxCapacityMultiplier = builder.comment("Multiplier to adjust the capacity of all ammo boxes.")
            .defineInRange("ammoBoxCapacityMultiplier", 1.0, 0.1, 100.0);
         this.enableGunDamage = builder.comment("If true, guns will be damageable and can break, coward if you toggle this").define("enableGunDamage", true);
         this.enableAttachmentDamage = builder.comment("If true, gun attachments will be damageable and can break, also a coward")
            .define("enableAttachmentDamage", true);
         this.spawnCasings = builder.comment("Set to false to disable the spawning of casing items when firing guns.").define("gameplay.spawnCasings", true);
         this.physicsStructureImpulse = builder.comment(
               "How hard a Scorched Guns projectile shoves a physics structure (Sable sub-level) it hits.",
               "The value is measured in PLAYER PUNCHES: 1.0 means a 10-damage round pushes exactly like an",
               "empty-handed punch, 0.5 half as hard, 2.0 twice as hard. The impulse is applied at the point",
               "of impact, so shots also make the structure spin. Set to 0 to disable the reaction entirely",
               "(shots then behave exactly like they do without a physics mod)."
            )
            .defineInRange("physicsStructureImpulse", 1.0, 0.0, 100.0);
         this.physicsStructureMaxSpeed = builder.comment(
               "Speed limit (blocks/second) that gunfire may push a physics structure up to. Together with a",
               "per-hit cap this is what keeps a burst of fire from launching a contraption out of the world.",
               "One punch adds about 0.26 blocks/second to a 242-unit-mass structure, so without this limit",
               "sustained automatic fire would keep accelerating it forever."
            )
            .defineInRange("physicsStructureMaxSpeed", 4.0, 0.0, 100.0);
         this.physicsStructureMaxSpin = builder.comment(
               "Spin limit (radians/second) that gunfire may give a physics structure. Sable's punch -",
               "and therefore this mod - applies its force at the shooter's own position, so a long shot",
               "has a long lever arm; without a limit a distant shot would spin a light structure",
               "violently even though its push is capped. The induced spin is computed from the",
               "structure's own inertia tensor, so this only ever binds outside punch range."
            )
            .defineInRange("physicsStructureMaxSpin", 3.0, 0.0, 100.0);
         this.growBoundingBoxAmount = builder.comment(
               "The extra amount to expand an entity's bounding box when checking for projectile collision. Setting this value higher will make it easier to hit entities"
            )
            .defineInRange("growBoundingBoxAmount", 0.3, 0.0, 1.0);
         this.enableHeadShots = builder.comment(
               "Enables the check for head shots for players. Projectiles that hit the head of a player will have increased damage."
            )
            .define("enableHeadShots", true);
         this.headShotDamageMultiplier = builder.comment("The value to multiply the damage by if projectile hit the players head")
            .defineInRange("headShotDamageMultiplier", 1.25, 1.0, Double.MAX_VALUE);
         this.ignoreLeaves = builder.comment("If true, projectiles will ignore leaves when checking for collision").define("ignoreLeaves", true);
         this.knockbackStrength = builder.comment(
               "Sets the strength of knockback when shot by a bullet projectile. Knockback must be enabled for this to take effect. If value is equal to zero, knockback will use default minecraft value"
            )
            .defineInRange("knockbackStrength", 0.15, 0.0, 1.0);
         this.improvedHitboxes = builder.comment(
               "If true, improves the accuracy of weapons by considering the ping of the player. This has no affect on singleplayer. This will add a little overhead if enabled."
            )
            .define("improvedHitboxes", false);
         this.enemyBulletDamage = builder.comment("Damage dealt by the Enemy Guns").defineInRange("enemyBulletDamage", 4.5, 0.0, Double.MAX_VALUE);
         this.energyProductionRate = builder.comment("Energy produced per tick by the Polar Generator. Adjust this value to balance the generator's output.")
            .defineInRange("energyProductionRate", 50, 1, Integer.MAX_VALUE);
         this.toggleADS = builder.comment("If true, guns will toggle ADS mode.").define("toggleADS", false);
         this.globalDamageMultiplier = builder.comment(
               "Global multiplier for all gun damage. 1.0 = normal damage, 0.5 = half damage, 2.0 = double damage. Affects all projectile damage from guns."
            )
            .defineInRange("globalDamageMultiplier", 1.0, 0.01, 100.0);
         this.globalTurretDamageMultiplier = builder.comment(
               "Global multiplier for all turret gun damage. 1.0 = normal damage, 0.5 = half damage, 2.0 = double damage. Affects all projectile damage from turrets."
            )
            .defineInRange("globalTurretDamageMultiplier", 1.0, 0.01, 100.0);
         this.disableVillagerSpawning = builder.comment(
               "If true, the Brass Mask ritual will never spawn villagers. When disabled, it will spawn Dissidents instead based on the spawn chance below."
            )
            .define("disableVillagerSpawning", false);
         this.dissidentSpawnChance = builder.comment(
               "When villager spawning is disabled, this is the chance (0.0 to 1.0) that a Dissident will spawn. If a Dissident doesn't spawn, nothing will be created from the ritual."
            )
            .defineInRange("dissidentSpawnChance", 0.1, 0.0, 1.0);
         this.enableAutoReload = builder.comment("If true, guns will automatically start reloading when fired with an empty magazine if ammo is available")
            .define("enableAutoReload", true);
         this.enableFirePlacement = builder.comment("If true, allows flamethrowers to place fire on blocks").define("enableFirePlacement", true);
         this.enableSculkPurification = builder.comment("If true, allows flamethrowers weapons to purify sculk blocks").define("enableSculkPurification", true);
         this.playerGunfireVolume = builder.comment("The volume for Player Gunfire. Default is 8").defineInRange("playerGunfireVolume", 8, 1, 10);
         this.mobGunfireVolume = builder.comment("The volume for Mob Gunfire. Default is 8").defineInRange("mobGunfireVolume", 8, 1, 10);
         this.mobFireRateMultiplier = builder.comment(
               "Global multiplier for mob fire rate. 1.0 = normal speed, 0.5 = faster (half delay), 2.0 = slower (double delay). Lower values = faster shooting."
            )
            .defineInRange("mobFireRateMultiplier", 1.0, 0.1, 5.0);
         this.mobBurstDelayMultiplier = builder.comment(
               "Multiplier for delay between bursts. Higher = longer delays between bursts. 1.0 = normal, 2.0 = double delay."
            )
            .defineInRange("mobBurstDelayMultiplier", 1.0, 0.1, 5.0);
         this.mobGunDamageMultiplier = builder.comment(
               "Global multiplier for mob gun damage. 1.0 = normal damage, 0.5 = half damage, 2.0 = double damage. Affects all projectile damage from mobs."
            )
            .defineInRange("mobGunDamageMultiplier", 1.0, 0.01, 100.0);
         this.cogBeaconSpawnChance = builder.comment(
               "Chance (0.0 to 1.0) that Cog enemies will spawn a Signal Beacon on death. Set to 0.0 to disable beacon spawning from Cog deaths."
            )
            .defineInRange("cogBeaconSpawnChance", 0.15, 0.0, 1.0);
         builder.pop();
      }
   }

   public static class Grenades {
      public final DoubleValue explosionRadius;

      public Grenades(Builder builder) {
         super();
         builder.comment("Properties relating to grenades").push("grenades");
         this.explosionRadius = builder.comment("The max distance which the explosion is effective to")
            .defineInRange("explosionRadius", 5.0, 0.0, Double.MAX_VALUE);
         builder.pop();
      }
   }

   public static class Griefing {
      public final BooleanValue enableBlockRemovalOnExplosions;
      public final BooleanValue enableMobExplosionBlockRemoval;
      public final BooleanValue enableGlassBreaking;
      public final BooleanValue fragileBlockDrops;
      public final DoubleValue fragileBaseBreakChance;
      public final BooleanValue setFireToBlocks;
      public final BooleanValue enableBlockBreaking;
      public final BooleanValue enableBeamMining;

      public Griefing(Builder builder) {
         super();
         builder.comment("Properties related to gun griefing").push("griefing");
         this.enableBlockRemovalOnExplosions = builder.comment("If enabled, allows block removal on explosions")
            .define("enableBlockRemovalOnExplosions", false);
         this.enableMobExplosionBlockRemoval = builder.comment(
               "If enabled, allows block removal on explosions from mob weapons. Only applies if enableBlockRemovalOnExplosions is also true"
            )
            .define("enableMobExplosionBlockRemoval", false);
         this.enableGlassBreaking = builder.comment("If enabled, allows guns to shoot out glass and other fragile objects").define("enableGlassBreaking", true);
         this.fragileBlockDrops = builder.comment("If enabled, fragile blocks will drop their loot when broken").define("fragileBlockDrops", true);
         this.fragileBaseBreakChance = builder.comment(
               "The base chance that a fragile block is broken when impacted by a bullet. The hardness of a block will scale this value; the harder the block, the lower the final calculated chance will be."
            )
            .defineInRange("fragileBlockBreakChance", 1.0, 0.0, 1.0);
         this.setFireToBlocks = builder.comment("If true, allows guns enchanted with Fire Starter to light and spread fires on blocks")
            .define("setFireToBlocks", true);
         this.enableBlockBreaking = builder.comment("If true, allows heavy rounds to break blocks").define("enableBlockBreaking", true);
         this.enableBeamMining = builder.comment("If true, allows beam weapons to mine blocks").define("enableBeamMining", true);
         builder.pop();
      }
   }

   public static class GunnerMobs {
      public final BooleanValue gunnerMobSpawning;
      public final DoubleValue gunnerSpawnChance;
      public final BooleanValue scaleToDifficulty;
      public final BooleanValue eliteSpawning;
      public final DoubleValue eliteChance;

      public GunnerMobs(Builder builder) {
         super();
         builder.comment("Gun Mob Spawning Configuration").push("gunner_config");
         this.gunnerMobSpawning = builder.comment("If enabled, mobs will have a chance to spawn with guns.").define("gunnerMobSpawning", true);
         this.gunnerSpawnChance = builder.comment(
               new String[]{
                  "Base chance for progression-based gunner mobs to spawn (0.0 = never, 1.0 = always).",
                  "This is the spawn chance on NORMAL difficulty. Thematic mobs use their own config."
               }
            )
            .defineInRange("gunnerSpawnChance", 0.3, 0.0, 1.0);
         this.scaleToDifficulty = builder.comment(
               new String[]{"If enabled, spawn chances scale with game difficulty.", "PEACEFUL: 50% of base chance, EASY: 75%, NORMAL: 100%, HARD: 150%"}
            )
            .define("scaleToDifficulty", true);
         this.eliteSpawning = builder.comment("If enabled, gunner mobs will have a chance to spawn as Elites with better armor.").define("eliteSpawning", true);
         this.eliteChance = builder.comment(
               new String[]{"Base chance for Elite Gunners to spawn (0.0 = never, 1.0 = always).", "This is the spawn chance on NORMAL difficulty."}
            )
            .defineInRange("eliteChance", 0.2, 0.0, 1.0);
         builder.pop();
      }
   }

   public static class Gunsmith {
      public Gunsmith(Builder builder) {
         super();
         builder.comment("Properties relating to gunsmith").push("gunsmith");
         builder.pop();
      }
   }

   public static class Network {
      public final DoubleValue projectileTrackingRange;

      public Network(Builder builder) {
         super();
         builder.comment("Properties relating to network").push("network");
         this.projectileTrackingRange = builder.comment(
               "The distance players need to be within to be able to track new projectiles trails. Higher values means you can see projectiles from that start from further away."
            )
            .defineInRange("projectileTrackingRange", 200.0, 1.0, Double.MAX_VALUE);
         builder.pop();
      }
   }

   public static class Particle {
      public final IntValue bulletHoleLifeMin;
      public final IntValue bulletHoleLifeMax;
      public final DoubleValue bulletHoleFadeThreshold;
      public final BooleanValue enableBlood;
      public final DoubleValue impactParticleDistance;
      public final BooleanValue enableWaterImpactParticles;
      public final BooleanValue enableLavaImpactParticles;

      public Particle(Builder builder) {
         super();
         builder.comment("Properties relating to particles").push("particle");
         this.bulletHoleLifeMin = builder.comment("The minimum duration in ticks before bullet holes will disappear")
            .defineInRange("bulletHoleLifeMin", 150, 0, Integer.MAX_VALUE);
         this.bulletHoleLifeMax = builder.comment("The maximum duration in ticks before bullet holes will disappear")
            .defineInRange("bulletHoleLifeMax", 200, 0, Integer.MAX_VALUE);
         this.bulletHoleFadeThreshold = builder.comment(
               "The percentage of the maximum life that must pass before particles begin fading away. 0 makes the particles always fade and 1 removes facing completely"
            )
            .defineInRange("bulletHoleFadeThreshold", 0.98, 0.0, 1.0);
         this.enableBlood = builder.comment("If true, blood will will spawn from entities that are hit from a projectile").define("enableBlood", true);
         this.impactParticleDistance = builder.comment("The maximum distance impact particles can be seen from the player")
            .defineInRange("impactParticleDistance", 32.0, 0.0, 64.0);
         this.enableWaterImpactParticles = builder.comment("If true, particles will spawn when projectiles impact water")
            .define("enableWaterImpactParticles", true);
         this.enableLavaImpactParticles = builder.comment("If true, particles will spawn when projectiles impact lava")
            .define("enableLavaImpactParticles", true);
         builder.pop();
      }
   }

   public static class ProjectileSpread {
      public final IntValue spreadThreshold;
      public final IntValue maxCount;

      public ProjectileSpread(Builder builder) {
         super();
         builder.comment("Properties relating to projectile spread").push("projectile_spread");
         this.spreadThreshold = builder.comment(
               "The amount of time in milliseconds before logic to apply spread is skipped. The value indicates a reasonable amount of time before a weapon is considered stable again."
            )
            .defineInRange("spreadThreshold", 300, 0, 1000);
         this.maxCount = builder.comment(
               "The amount of times a player has to shoot within the spread threshold before the maximum amount of spread is applied. Setting the value higher means it will take longer for the spread to be applied."
            )
            .defineInRange("maxCount", 10, 1, Integer.MAX_VALUE);
         builder.pop();
      }
   }

   public static class Raids {
      public final BooleanValue raidsEnabled;
      public final DoubleValue nightlyRaidChance;
      public final IntValue minDaysBetweenRaids;
      public final IntValue raidTimeoutMinutes;

      public Raids(Builder builder) {
         super();
         builder.comment("Mini-Raid Configuration").push("raids");
         this.raidsEnabled = builder.comment("If enabled, mini-raids can spawn at night based on player progression.").define("raidsEnabled", true);
         this.nightlyRaidChance = builder.comment(
               new String[]{
                  "Chance each night for a raid to spawn near a valid player (0.0 = never, 1.0 = always).",
                  "Set to 0.0 to effectively disable raids without turning off the system entirely."
               }
            )
            .defineInRange("nightlyRaidChance", 0.2, 0.0, 1.0);
         this.minDaysBetweenRaids = builder.comment(
               new String[]{
                  "Minimum number of days that must pass between natural raid spawns.",
                  "Set to 0 to allow raids every night (if chance succeeds).",
                  "Set to 1 to allow raids every other night.",
                  "Set to 2 or higher to space out raids further."
               }
            )
            .defineInRange("minDaysBetweenRaids", 2, 0, 100);
         this.raidTimeoutMinutes = builder.comment(
               new String[]{"Time in minutes before a raid automatically fails if the boss is not defeated.", "Default: 10 minutes"}
            )
            .defineInRange("raidTimeoutMinutes", 20, 0, 60);
         builder.pop();
      }
   }

   public static class Server {
      public final IntValue alphaOverlay;
      public final IntValue alphaFadeThreshold;
      public final DoubleValue soundPercentage;
      public final IntValue soundFadeThreshold;
      public final DoubleValue ringVolume;
      public final DoubleValue gunShotMaxDistance;
      public final DoubleValue reloadMaxDistance;
      public final BooleanValue enableCameraRecoil;
      public final IntValue cooldownThreshold;
      public final Config.Server.Experimental experimental;

      public Server(Builder builder) {
         super();
         builder.push("server");
         builder.comment("Stun Grenade related properties").push("grenade");
         this.alphaOverlay = builder.comment(
               "After the duration drops to this many ticks, the transparency of the overlay when blinded will gradually fade to 0 alpha."
            )
            .defineInRange("alphaOverlay", 255, 0, 255);
         this.alphaFadeThreshold = builder.comment("Transparency of the overlay when blinded will be this alpha value, before eventually fading to 0 alpha.")
            .defineInRange("alphaFadeThreshold", 40, 0, Integer.MAX_VALUE);
         this.soundPercentage = builder.comment("Volume of most game sounds when deafened will play at this percent, before eventually fading back to %100.")
            .defineInRange("soundPercentage", 0.05, 0.0, 1.0);
         this.soundFadeThreshold = builder.comment(
               "After the duration drops to this many ticks, the ringing volume will gradually fade to 0 and other sound volumes will fade back to %100."
            )
            .defineInRange("soundFadeThreshold", 90, 0, Integer.MAX_VALUE);
         this.ringVolume = builder.comment("Volume of the ringing sound when deafened will play at this volume, before eventually fading to 0.")
            .defineInRange("ringVolume", 1.0, 0.0, 1.0);
         builder.pop();
         builder.comment("Audio properties").push("audio");
         this.gunShotMaxDistance = builder.comment("The maximum distance weapons can be heard by players.")
            .defineInRange("gunShotMaxDistance", 100.0, 0.0, Double.MAX_VALUE);
         this.reloadMaxDistance = builder.comment("The maximum distance reloading can be heard by players.")
            .defineInRange("reloadMaxDistance", 24.0, 0.0, Double.MAX_VALUE);
         builder.pop();
         this.enableCameraRecoil = builder.comment("If true, enables camera recoil when firing a weapon").define("enableCameraRecoil", true);
         this.cooldownThreshold = builder.comment(
               "The maximum amount of cooldown time remaining before the server will accept another shoot packet from a client. This allows for a litle slack since the server may be lagging"
            )
            .defineInRange("cooldownThreshold", 0, 75, 1000);
         this.experimental = new Config.Server.Experimental(builder);
         builder.pop();
      }

      public static class Experimental {
         public final BooleanValue forceDyeableAttachments;

         public Experimental(Builder builder) {
            super();
            builder.push("experimental");
            this.forceDyeableAttachments = builder.comment(
                  "Forces all attachments to be dyeable regardless if they have an affect on the model. This is useful if your server uses custom models for attachments and the models have dyeable elements"
               )
               .define("forceDyeableAttachments", false);
            builder.pop();
         }
      }
   }

   public static class Sounds {
      public final BooleanValue playSoundWhenHeadshot;
      public final ConfigValue<String> headshotSound;
      public final BooleanValue playSoundWhenCritical;
      public final ConfigValue<String> criticalSound;
      public final DoubleValue impactSoundDistance;

      public Sounds(Builder builder) {
         super();
         builder.comment("Control sounds triggered by guns").push("sounds");
         this.playSoundWhenHeadshot = builder.comment("If true, a sound will play when you successfully hit a headshot on a entity with a gun")
            .define("playSoundWhenHeadshot", true);
         this.headshotSound = builder.comment("The sound to play when a headshot occurs").define("headshotSound", "minecraft:entity.player.attack.knockback");
         this.playSoundWhenCritical = builder.comment("If true, a sound will play when you successfully hit a critical on a entity with a gun")
            .define("playSoundWhenCritical", true);
         this.criticalSound = builder.comment("The sound to play when a critical occurs").define("criticalSound", "minecraft:entity.player.attack.crit");
         this.impactSoundDistance = builder.comment("The maximum distance impact sounds from bullet can be heard")
            .defineInRange("impactSoundDistance", 32.0, 0.0, 32.0);
         builder.pop();
      }
   }

   public static class StunGrenades {
      public final Config.Blind blind;
      public final Config.Deafen deafen;

      public StunGrenades(Builder builder) {
         super();
         builder.comment("Properties relating to stun grenades").push("stun_grenades");
         this.blind = new Config.Blind(builder);
         this.deafen = new Config.Deafen(builder);
         builder.pop();
      }
   }
}
