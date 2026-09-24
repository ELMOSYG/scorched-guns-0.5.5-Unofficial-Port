package com.scg2tlm.elmomod;


import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.common.Mod;

// No @EventBusSubscriber here: this class only holds the config spec (ExampleMod registers it on
// the mod's container). NeoForge refuses to auto-register a class without @SubscribeEvent methods
// ("has no @SubscribeEvent methods, but register was called anyway"), where Forge silently ignored
// it - keeping the annotation crashed mod loading.
public class SCG2TLMConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.DoubleValue GUN_ACCURACY;
    public static final ModConfigSpec.DoubleValue GUN_RANGE;

    public static final ModConfigSpec.DoubleValue GRENADE_RADIUS;
    public static final ModConfigSpec.IntValue GRENADE_COOLDOWN;
    public static final ModConfigSpec.IntValue GRENADE_POST_THROW_TIME;

    public static final ModConfigSpec.DoubleValue MELEE_RANGE;
    public static final ModConfigSpec.DoubleValue MELEE_SPEED;
    public static final ModConfigSpec.BooleanValue BAYONET_MELEE;
    public static final ModConfigSpec.IntValue FIRING_INTERVAL;
    public static final ModConfigSpec.BooleanValue LINE_INFANTRY;
    public static final ModConfigSpec.IntValue VOLLEY_INTERVAL;
    public static final ModConfigSpec.IntValue FORMATION_SIZE;
    public static final ModConfigSpec.DoubleValue FORMATION_SPACING;

    public static final ModConfigSpec.IntValue TARGET_LOST_SIGHT_TICKS;
    public static final ModConfigSpec.IntValue TARGET_SEARCH_TICKS;
    public static final ModConfigSpec.DoubleValue TARGET_SEARCH_RANGE;
    public static final ModConfigSpec.BooleanValue TARGET_SEARCH_INTERRUPTIBLE;
    public static final ModConfigSpec.BooleanValue TARGET_SEARCH_RESUME;

    public static final ModConfigSpec.BooleanValue RELOAD_FREE_AMMO;
    public static final ModConfigSpec.ConfigValue<String> RELOAD_SOUND;
    public static final ModConfigSpec.ConfigValue<String> RELOAD_END_SOUND;
    public static final ModConfigSpec.DoubleValue FOLLOW_LEASH_RADIUS;
    public static final ModConfigSpec.BooleanValue IDLE_RELOAD;
    /** Ticks a maid must have no enemy before the idle reload starts (default 100 = 5 seconds). */
    public static final ModConfigSpec.IntValue IDLE_RELOAD_DELAY;

    public static final ModConfigSpec.DoubleValue SNIPER_SEARCH_RADIUS;
    public static final ModConfigSpec.DoubleValue RIFLE_MAGNUM_SEARCH_RADIUS;
    public static final ModConfigSpec.DoubleValue SMG_SHOTGUN_PISTOL_SEARCH_RADIUS;
    public static final ModConfigSpec.DoubleValue PISTOL_SEARCH_RADIUS;
    public static final ModConfigSpec.DoubleValue SHOTGUN_SEARCH_RADIUS;
    public static final ModConfigSpec.DoubleValue FLAMETHROWER_SEARCH_RADIUS;

    public static final ModConfigSpec.BooleanValue LEAD_ALWAYS;
    public static final ModConfigSpec.DoubleValue LEAD_WEIGHT;

    public static final ModConfigSpec.BooleanValue ENABLE_PLAYER_FACTION;
    public static final ModConfigSpec.BooleanValue PREVENT_FACTION_FRIENDLY_FIRE;

    public static final ModConfigSpec.BooleanValue BULLET_RAISES_SHIELD;

    public static final ModConfigSpec.BooleanValue NATIVE_SCGUNS_AI;
    public static final ModConfigSpec.ConfigValue<String> NATIVE_SCGUNS_AI_TYPE;
    public static final ModConfigSpec.DoubleValue NATIVE_SCGUNS_AI_SPEED;
    public static final ModConfigSpec.IntValue NATIVE_SCGUNS_AI_DIFFICULTY;

    static {
        BUILDER.push("combat");

        BUILDER.comment("Accuracy for maid gun attacks (higher = more accurate). Spread = 5 * difficulty / this_value.");
        GUN_ACCURACY = BUILDER.defineInRange("gun_accuracy", 3.5, 0.0, 20.0);

        BUILDER.comment("Default gun engagement range in blocks");
        GUN_RANGE = BUILDER.defineInRange("gun_range", 64.0, 8.0, 256.0);

        BUILDER.comment("Grenade engagement radius in blocks");
        GRENADE_RADIUS = BUILDER.defineInRange("grenade_radius", 24.0, 4.0, 128.0);

        BUILDER.comment("Ticks between grenade throws (20 ticks = 1 second)");
        GRENADE_COOLDOWN = BUILDER.defineInRange("grenade_cooldown", 40, 10, 200);

        BUILDER.comment("Ticks to flee after throwing a grenade");
        GRENADE_POST_THROW_TIME = BUILDER.defineInRange("grenade_post_throw_time", 20, 0, 100);

        BUILDER.comment("Max distance for gun-butt melee attack in blocks");
        MELEE_RANGE = BUILDER.defineInRange("melee_range", 3.0, 1.0, 10.0);

        BUILDER.comment("Movement speed multiplier when closing for melee");
        MELEE_SPEED = BUILDER.defineInRange("melee_speed", 0.8, 0.1, 2.0);

        BUILDER.comment("When enabled, maids with a bayonet-equipped gun perform bayonet melee attacks with animation when the target is in arm's reach");
        BAYONET_MELEE = BUILDER.define("bayonet_melee", true);

        BUILDER.comment("Extra ticks added between each shot (0 = fire at the gun's natural rate, 20 = 1 shot per second). Higher values reduce ammo consumption");
        FIRING_INTERVAL = BUILDER.defineInRange("firing_interval", 0, 0, 200);

        BUILDER.comment("Line infantry mode: maids wielding antique weapons stand still, form a line formation around their owner facing the enemy, and fire in synchronized volleys");
        LINE_INFANTRY = BUILDER.define("line_infantry", false);

        BUILDER.comment("Ticks between each volley (20 ticks = 1 second). All line-infantry maids fire together on this cadence");
        VOLLEY_INTERVAL = BUILDER.defineInRange("volley_interval", 40, 10, 200);

        BUILDER.comment("Maximum number of maids in the line formation");
        FORMATION_SIZE = BUILDER.defineInRange("formation_size", 8, 1, 32);

        BUILDER.comment("Horizontal spacing in blocks between maids in the line formation");
        FORMATION_SPACING = BUILDER.defineInRange("formation_spacing", 2.5, 1.0, 8.0);

        BUILDER.comment("Ticks a maid keeps a target after losing sight of it before deciding what to do (20 ticks = 1 second). The target is only dropped after this delay, so brief cover flickers do not break the lock");
        TARGET_LOST_SIGHT_TICKS = BUILDER.defineInRange("target_lost_sight_ticks", 60, 20, 600);

        BUILDER.comment("Ticks a maid spends walking to the target's last seen position before giving up (0 = give up immediately instead of investigating). This only happens when NO other enemy is visible - if another enemy is in sight the maid drops the hidden target right away and switches to it. Default 400 = 20 seconds");
        TARGET_SEARCH_TICKS = BUILDER.defineInRange("target_search_ticks", 400, 0, 1200);

        BUILDER.comment("Max distance in blocks a maid may walk to the target's last seen position when investigating");
        TARGET_SEARCH_RANGE = BUILDER.defineInRange("target_search_range", 24.0, 4.0, 128.0);

        BUILDER.comment("Whether a new visible enemy may interrupt the search for a hidden target (the maid drops the hidden target and engages the new one). Disable to make the maid finish or give up the search first");
        TARGET_SEARCH_INTERRUPTIBLE = BUILDER.define("target_search_interruptible", true);

        BUILDER.comment("When the search IS interrupted by a new enemy, remember that target's last seen position and the remaining search time, then go back to search it once the fight is over (CQB room clearing). Disable to simply forget the old target");
        TARGET_SEARCH_RESUME = BUILDER.define("target_search_resume", true);

        BUILDER.pop();
        BUILDER.push("reload");

        BUILDER.comment("When enabled, maid reloads do not consume backpack ammo and ignore the need for ammo entirely");
        RELOAD_FREE_AMMO = BUILDER.define("reload_free_ammo", false);

        BUILDER.comment("Sound event played when a maid STARTS reloading (empty = fall back to SC2's own rule:",
                "the gun data's sounds.reload field, skipped entirely for AnimatedGunItem guns).",
                "Default is SC2's gauss reload, because the per-gun reload field in the data is a placeholder",
                "anyway (132 of SC2's 141 guns and 24 of cnc's 27 guns just say scguns:item.gauss.reload,",
                "and cnc's three energy guns say the FLAMETHROWER one, which is what users heard).",
                "  scguns:item.gauss.reload   (default)",
                "  scguns:item.pistol.reload",
                "  scguns:item.flamethrower.reload");
        RELOAD_SOUND = BUILDER.define("reload_sound", "scguns:item.gauss.reload");

        BUILDER.comment("Sound event played when a maid FINISHES reloading - i.e. the 上膛 / chambering sound (empty = silent).",
                "Default is SC2's bolt sound (拉栓). The sound SC2 uses for the 'cock' field",
                "(scguns:item.pistol.cock) is a reload recording, so it sounds wrong at the end of a reload.",
                "Other unused-by-SC2 candidates:",
                "  scguns:item.bolt.bolt              (拉栓, longer/fuller bolt cycle)",
                "  scguns:item.bolt_pull.bolt_pull    (拉栓, shorter)",
                "  scguns:item.bolt_release.bolt_release (放栓, pushing the bolt back in)",
                "  scguns:item.rack.rack              (上膛, mechanical rack)",
                "  scguns:item.reload_end.reload_end  (装填完毕, SC2's generic completion cue)",
                "  scguns:item.mag_in.mag_in / scguns:item.mag_out.mag_out (弹匣)");
        RELOAD_END_SOUND = BUILDER.define("reload_end_sound", "scguns:item.bolt.bolt");

        BUILDER.comment("Follow-mode leash radius in blocks (0 = disabled).",
                "TLM's Home Mode (留守/待命) is NOT affected - this only applies while the maid is following her owner.",
                "Beyond this distance from her owner the maid walks back towards him instead of chasing the target;",
                "she keeps shooting while doing so (firing is a separate behavior and does not require standing still).");
        FOLLOW_LEASH_RADIUS = BUILDER.defineInRange("follow_leash_radius", 8.0, 0.0, 64.0);

        BUILDER.comment("When enabled, a maid with no enemy around tops up her magazine by reloading",
                "(only if the magazine is not full and she carries the matching ammo).",
                "Reloading is normally driven by the shooting behavior, which requires an attack target,",
                "so without this she never refills between fights.");
        IDLE_RELOAD = BUILDER.define("idle_reload", true);

        BUILDER.comment("How long a maid must have NO enemy before the idle reload above starts, in ticks (20 = 1 second).",
                "Default 100 = 5 seconds. The idle behavior only runs while she has no attack target, so this",
                "measures exactly 'time since the enemies went away' - and any new enemy preempting the behavior",
                "restarts the wait. Set to 0 to reload the moment the fight ends, which is what the original mod did",
                "(it fired the instant the target was lost, i.e. the maid turned away from a corpse already reloading).");
        IDLE_RELOAD_DELAY = BUILDER.defineInRange("idle_reload_delay_ticks", 100, 0, 1200);

        BUILDER.pop();
        BUILDER.push("search_radii");

        BUILDER.comment("Target search radius for sniper rifles");
        SNIPER_SEARCH_RADIUS = BUILDER.defineInRange("sniper_search_radius", 96.0, 8.0, 512.0);

        BUILDER.comment("Target search radius for rifles and magnums");
        RIFLE_MAGNUM_SEARCH_RADIUS = BUILDER.defineInRange("rifle_magnum_search_radius", 64.0, 8.0, 256.0);

        BUILDER.comment("Target search radius for SMGs",
                "(the config key still lists shotgun/pistol for backwards compatibility -",
                " shotguns, flamethrowers and pistols now have their own entries)");
        SMG_SHOTGUN_PISTOL_SEARCH_RADIUS = BUILDER.defineInRange("smg_shotgun_pistol_search_radius", 32.0, 8.0, 128.0);

        BUILDER.comment("Target search radius for pistols / magnum-class handguns",
                "Same rule as the other entries: this also caps the firing distance.");
        PISTOL_SEARCH_RADIUS = BUILDER.defineInRange("pistol_search_radius", 16.0, 4.0, 64.0);

        BUILDER.comment("Target search radius for shotguns (also the shotgun-class specials:",
                " plasmabuss / libertas are classified as shotguns)",
                "This value ALSO caps how far a maid keeps firing with that weapon:",
                " max firing distance = min(gun_range, the class radius here).");
        SHOTGUN_SEARCH_RADIUS = BUILDER.defineInRange("shotgun_search_radius", 16.0, 4.0, 64.0);

        BUILDER.comment("Target search radius for flamethrowers and shock weapons",
                "Same rule applies: this caps the firing distance too.");
        FLAMETHROWER_SEARCH_RADIUS = BUILDER.defineInRange("flamethrower_search_radius", 10.0, 4.0, 48.0);

        BUILDER.comment("When enabled, maid gun AI always leads moving targets (SCG only does this on Hard difficulty)");
        LEAD_ALWAYS = BUILDER.define("lead_prediction_always", true);

        BUILDER.comment("How much the lead direction is mixed into the aim (SCG default 0.3). 1.0 = full prediction");
        LEAD_WEIGHT = BUILDER.defineInRange("lead_prediction_weight", 0.3, 0.0, 1.0);

        BUILDER.comment("SCG Extra 联动：把玩家和女仆编入独立阵营 \"player\"，让 SCG Extra 阵营怪主动攻击它们");
        ENABLE_PLAYER_FACTION = BUILDER.define("enable_player_faction", true);

        BUILDER.comment("SCG Extra 联动：同一阵营的怪互相射击时，子弹直接穿过友军不造成友伤（玩家/女仆的弹体不受影响）");
        PREVENT_FACTION_FRIENDLY_FIRE = BUILDER.define("prevent_faction_friendly_fire", true);

        BUILDER.pop();

        BUILDER.push("shield");

        BUILDER.comment("Raise the shield when taking BULLET damage, not just projectile damage.",
                "SC2 bullets use their own damage type scguns:bullet, which is NOT in minecraft:is_projectile",
                "(the SC2 jar ships no data/minecraft/tags/damage_type at all), so TLM's passive shield raise",
                "never fires in gunfights. Enabled = a maid with a usable shield raises it when a bullet hits her,",
                "just like an arrow would.",
                "Note: two-handed guns (rifle / minigun / rpg) never use the shield at all - see canUseShield.");
        BULLET_RAISES_SHIELD = BUILDER.define("bullet_raises_shield", true);

        BUILDER.pop();

        BUILDER.push("scguns_native_ai");

        BUILDER.comment("EXPERIMENTAL: hand the maid over to Scorched Guns' OWN gunner AI.",
                "When ON, our gun task stops registering its own combat behaviors and instead attaches",
                "scguns' GunAttackGoal to the maid - the same goal SC2 gunner mobs use",
                "(aiming, bursts, reloading, strafing, keeping ideal/min range, accuracy by personality).",
                "Target picking is still done by our task; ATTACK_TARGET is mirrored to Mob#setTarget",
                "because GunAttackGoal reads getTarget().",
                "Only affects maids assigned OUR gun task (scg2tlm:gun_attack). Default OFF.");
        NATIVE_SCGUNS_AI = BUILDER.define("enable_scguns_native_ai", false);

        BUILDER.comment("Which SC2 gunner personality GunAttackGoal should use:",
                "TACTICAL / SMART / DEFAULT / RECKLESS / COWARD (SC2's AIType enum, case-insensitive).",
                "TACTICAL = most accurate, RECKLESS = gets closest and shoots fastest, COWARD = keeps distance.");
        NATIVE_SCGUNS_AI_TYPE = BUILDER.define("scguns_native_ai_type", "tactical");

        BUILDER.comment("Movement speed modifier passed to GunAttackGoal (SC2 gunner mobs use 1.0).");
        NATIVE_SCGUNS_AI_SPEED = BUILDER.defineInRange("scguns_native_ai_speed", 1.0, 0.2, 2.0);

        BUILDER.comment("Difficulty value passed to GunAttackGoal (SC2 gunner mobs use 3).",
                "It only feeds the accuracy formula: accuracy = base(personality) * (1 + (difficulty - 1) * 0.3).");
        NATIVE_SCGUNS_AI_DIFFICULTY = BUILDER.defineInRange("scguns_native_ai_difficulty", 3, 0, 3);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
