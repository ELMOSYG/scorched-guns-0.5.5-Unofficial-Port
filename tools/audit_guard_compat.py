"""The Guard Villagers integration must not be able to break a server that does not have that mod.

Guard Villagers is optional: most players do not have it, and a compat that names one of its classes from
host code fails at *link* time on their server (`NoClassDefFoundError` when the instruction is first
executed), which is a crash at spawn time, not a missing feature. HANDOFF section 82 wires the
integration the other way round, and this audit pins that shape:

  1. `tallestegg.guardvillagers` appears in exactly **one** file, `GuardFriendlyRules`, which is reached
     only behind `GuardVillagersCompat.isGuard(...)` - a registry-id comparison that cannot be true
     without the mod. Everything else - the equip hook, the gun goal, the projectile filter, the damage
     handler - is guard-class-free and therefore safe to load and call on any server.
  2. Guards are recognised by the same id that the data file uses, so
     `data/scguns/entity/gunner_mobs.json` really does hand them a gun: the constant and the JSON key must
     agree, or the compat silently does nothing.
  3. The equip hook is reached: `GunnerMobSpawner.onEntityJoinWorld` calls it, the guard's own goal counts
     as "has gun AI" (otherwise `reassessWeaponGoal` would also give a guard the hostile raider AI), and a
     guard's shots are filtered out of both projectile hit searches.
  4. The optional dependency is declared where players can see it, and the guard accuracy is configurable.

Usage:
    python tools/audit_guard_compat.py
    python tools/audit_guard_compat.py --selftest   # must FAIL against the pre-compat revision
"""

import json
import pathlib
import re
import subprocess
import sys

SOURCE = pathlib.Path("src/main/java")
PACKAGE = SOURCE / "top/ribs/scguns"
COMPAT = PACKAGE / "compat/guardvillagers"
SPAWNER = PACKAGE / "config/GunnerMobSpawner.java"
PROJECTILE = PACKAGE / "entity/projectile/ProjectileEntity.java"
CONFIG = PACKAGE / "Config.java"
GUNNER_JSON = pathlib.Path("src/main/resources/data/scguns/entity/gunner_mobs.json")
MODS_TOML = pathlib.Path("src/main/templates/META-INF/neoforge.mods.toml")
MIXIN_CONFIG = pathlib.Path("src/main/resources/scguns.mixins.json")

RELATIVE_PACKAGE = "src/main/java/top/ribs/scguns"
GUARD_PACKAGE = "tallestegg.guardvillagers"
GUARD_ID = "guardvillagers:guard"

# The revision this was written against: HANDOFF section 81, before the guard compat existed.
PRE_FIX_REVISION = "8863bb4"


def strip_comments(text):
    text = re.sub(r"/\*.*?\*/", lambda m: re.sub(r"[^\n]", " ", m.group(0)), text, flags=re.S)
    return re.sub(r"//[^\n]*", "", text)


def method_body(text, name):
    """The body of a method, by brace matching - for asking what a specific method does or does not call."""
    match = re.search(r"\b%s\s*\([^)]*\)\s*\{" % re.escape(name), text, re.S)
    if not match:
        return ""
    depth, i = 0, match.end() - 1
    while i < len(text):
        if text[i] == "{":
            depth += 1
        elif text[i] == "}":
            depth -= 1
            if depth == 0:
                return text[match.end():i]
        i += 1
    return ""


def guard_class_files(files):
    """Files that name a Guard Villagers class, relative to the scguns package."""
    return sorted(name for name, text in files.items()
                  if GUARD_PACKAGE in strip_comments(text))


def check(files, gunner_json, mods_toml, mixin_config_text=""):
    problems = []
    for required in ("compat/guardvillagers/GuardVillagersCompat.java",
                     "compat/guardvillagers/GuardFriendlyRules.java",
                     "compat/guardvillagers/GuardGunAttackGoal.java",
                     "compat/guardvillagers/GuardVillagersEvents.java"):
        if required not in files:
            problems.append("%s is missing, so part of the guard integration is not there" % required)

    # Allowed to name a Guard Villagers class: GuardFriendlyRules (reached only behind the guard-id check),
    # the guard mixins under mixin/common/compat/guardvillagers/ and mixin/client/compat/guardvillagers/
    # (both gated in MixinPlugin, so they never apply without the mod) and MixinPlugin itself (which probes
    # the mod list by name). Anything else can fail to link on a server without the mod.
    allowed = ("compat/guardvillagers/GuardFriendlyRules.java",
               "mixin/MixinPlugin.java")
    named = [name for name in guard_class_files(files)
             if name not in allowed
             and not name.startswith("mixin/common/compat/guardvillagers/")
             and not name.startswith("mixin/client/compat/guardvillagers/")]
    if named:
        problems.append("Guard Villagers classes are named in %s - only GuardFriendlyRules, the gated guard "
                        "mixins and MixinPlugin's probe may" % (named or "no file"))

    compat = strip_comments(files.get("compat/guardvillagers/GuardVillagersCompat.java") or "")
    if f'fromNamespaceAndPath("guardvillagers", "guard")' not in compat:
        problems.append("GuardVillagersCompat no longer knows the guard entity id")
    if "isGuard(" not in compat or "getKey(entity.getType())" not in compat:
        problems.append("the guard test is no longer a registry-id comparison, so it may need the Guard "
                        "class to run")
    if "isFriendlyShot" not in compat:
        problems.append("GuardVillagersCompat has no isFriendlyShot, so the projectile and damage paths "
                        "have no safe hook")
    if "GuardFriendlyRules" not in compat:
        problems.append("the guarded indirection into GuardFriendlyRules is gone")

    # The data file and the constant have to agree.
    if GUARD_ID not in gunner_json:
        problems.append("%s has no %s entry, so a guard is recognised but never equipped"
                        % (GUNNER_JSON.name, GUARD_ID))
    else:
        try:
            mobs = json.loads(gunner_json)["mobs"]
        except (ValueError, KeyError) as error:
            problems.append("%s is not readable as a gunner mob config (%s)" % (GUNNER_JSON.name, error))
            mobs = {}
        data = mobs.get(GUARD_ID)
        if not data:
            problems.append("%s has no %s key in \"mobs\"" % (GUNNER_JSON.name, GUARD_ID))
        else:
            if not data.get("weapons"):
                problems.append("the %s entry has no weapon list" % GUARD_ID)
            if data.get("spawn_chance", 0) <= 0:
                problems.append("the %s entry can never spawn with a gun (spawn_chance <= 0)" % GUARD_ID)

    for relative, needles in (
        ("config/GunnerMobSpawner.java", ["GuardVillagersCompat.equipGuardGun", "GuardGunAttackGoal"]),
    ):
        text = strip_comments(files.get(relative) or "")
        for needle in needles:
            if needle not in text:
                problems.append("%s never reaches %s, so part of the compat is dead code"
                                % (relative, needle))

    # The friendly-fire gate has to sit on the projectile funnel (HANDOFF section 82). The layers that do
    # NOT work on this mod's projectiles, in the order the reference implementations tried them:
    #   * vanilla Projectile.canHitEntity - SCG's ProjectileEntity is not a vanilla Projectile;
    #   * the base onHitEntity - ~25 subclasses override it without calling super;
    #   * findEntityOnPath/findEntitiesOnPath - LightningProjectileEntity and ShotballProjectileEntity
    #     run their own searches, so a filter there misses them.
    # getHitResult is the funnel every path goes through and nothing overrides.
    mixin = "mixin/common/compat/guardvillagers/GuardProjectileHitMixin.java"
    text = strip_comments(files.get(mixin) or "")
    if not text:
        problems.append("%s is missing, so a guard's shots hit whatever is behind the target" % mixin)
    else:
        if "getHitResult" not in text:
            problems.append("%s no longer hooks getHitResult, the one funnel every projectile path uses"
                            % mixin)
        if "GuardVillagersCompat.isFriendlyShot" not in text:
            problems.append("%s does not ask GuardVillagersCompat, so the gate can disagree with the "
                            "equip path" % mixin)
        if GUARD_PACKAGE in text:
            problems.append("%s names a Guard Villagers class; it targets the mod's own projectile and "
                            "must stay loadable without the mod" % mixin)

    mixin_config = {}
    try:
        mixin_config = json.loads(mixin_config_text) if mixin_config_text else {}
    except ValueError as error:
        problems.append("%s is not readable as JSON (%s)" % (MIXIN_CONFIG.name, error))
    if "common.compat.guardvillagers.GuardProjectileHitMixin" not in mixin_config.get("mixins", []):
        problems.append("scguns.mixins.json does not list GuardProjectileHitMixin, so the gate is never "
                        "applied")

    projectile = strip_comments(files.get("entity/projectile/ProjectileEntity.java") or "")
    if "isFriendlyShot" in projectile:
        problems.append("ProjectileEntity filters friendly shots again - that layer misses the projectiles "
                        "with their own entity search and reads as coverage it does not provide; the "
                        "mixin's getHitResult hook is the one that covers every path")

    spawner = strip_comments(files.get("config/GunnerMobSpawner.java") or "")
    goal = strip_comments(files.get("compat/guardvillagers/GuardGunAttackGoal.java") or "")
    has_goal = re.search(r"hasGunAttackGoal.*?instanceof GuardGunAttackGoal", spawner, re.S)
    if not has_goal:
        problems.append("hasGunAttackGoal does not count GuardGunAttackGoal, so a guard would also be "
                        "given the hostile raider AI")
    if "GuardVillagersCompat.isGuard(mob)" not in spawner:
        problems.append("the equip path no longer checks the guard id")
    # A guard must keep its own follow range: extending it to a raider's 64 blocks sends armed guards
    # chasing away from the village they defend (HANDOFF section 82.7).
    guard_equip = method_body(spawner, "equipGuardGun")
    if "resetFollowRange(" not in guard_equip:
        problems.append("arming a guard does not reset its follow range, so it keeps (or gains) the "
                        "raider range and wanders off")
    # The gun goal must not reserve MOVE|LOOK: while it runs, the goal selector cannot start any other
    # goal needing those flags, which silently disabled Guard Villagers' melee, patrol, checkpoint,
    # return-to-village, door and stroll goals - the gun AI "taking over" the guard's AI.
    if "setFlags(" not in goal:
        problems.append("the guard gun goal no longer reserves MOVE and LOOK, so it cannot position itself: "
                        "Guard Villagers' own goals share the navigation and win it, and the guard fires "
                        "from wherever it happens to be (HANDOFF 82.15)")
    if "getNavigation(" not in goal:
        problems.append("the guard gun goal never steers movement, so it has no way to hold the gun's range "
                        "or back out of melee (HANDOFF 82.15)")

    events = strip_comments(files.get("compat/guardvillagers/GuardVillagersEvents.java") or "")
    for needle in ("LivingIncomingDamageEvent", "LivingKnockBackEvent", "GuardVillagersCompat.isFriendlyShot"):
        if needle not in events:
            problems.append("the friendly-fire handler no longer covers %s" % needle)

    # Guards fight with their own dedicated AI (HANDOFF section 82.15). This reverses 82.9 deliberately: the
    # raider AI takes no flags, so Guard Villagers' goals keep the navigation and the guard never positions
    # itself - the player's "its gunner AI is not smart". The guard AI owns its movement instead, like
    # vanilla's MeleeAttackGoal does, and everything that is about the *shot* still comes from the shared
    # pipeline rather than being hand-rolled again.
    if not re.search(r"class GuardGunAttackGoal[^\{]*extends\s+Goal", goal):
        problems.append("the guard gun goal is not a standalone Goal any more; a guard is supposed to fight "
                        "with its own AI (HANDOFF 82.15)")
    if re.search(r"class GuardGunAttackGoal[^\{]*extends\s+GunAttackGoal", goal):
        problems.append("the guard gun goal extends the raider AI again, whose missing flags are exactly why "
                        "a guard could not position itself (HANDOFF 82.15)")
    if GUARD_PACKAGE in goal:
        problems.append("the guard gun goal names a Guard Villagers class, which forces that class to load")
    if "MobGunFire.fire(" not in goal or "MobGunFire.fireInterval(" not in goal:
        problems.append("the guard goal does not fire through MobGunFire, so guards hand-roll the shot again "
                        "(rate chain, ammo rules, sound, casing)")
    if "performGunAttack" in goal or "consumeAmmo" in goal:
        problems.append("the guard goal spawns projectiles or spends ammo itself; both belong to MobGunFire")
    # The two bugs the reference goal carries, which must not come back with it.
    if re.search(r"\.\s*tick\s*\(\s*\)", goal):
        problems.append("the guard goal ticks its own projectiles inline, which is the section 82.11 shotgun "
                        "stutter (26 pellets, 26 full ticks inside one call)")
    if re.search(r"rate\s*/\s*50|/\s*50", goal):
        problems.append("the guard goal converts the fire rate by dividing by 50, the section 82.6 bug (a "
                        "0.20.1 millisecond value read as ticks)")
    if "getIdealAttackRange" not in goal:
        problems.append("the guard goal no longer reads the gun's ideal range, so every guard fights at one "
                        "fixed distance whatever it is holding")
    if "AmmoCount" not in goal:
        problems.append("the guard goal no longer reloads, so a guard fires one magazine and then stands "
                        "there (section 81)")
    if "isFriendlyShot" not in goal:
        problems.append("the guard goal no longer checks for an ally in the line of fire, so it shoots "
                        "through the villager it is protecting")

    gun_goal = strip_comments(files.get("entity/ai/GunAttackGoal.java") or "")
    if "MobGunFire.fire(" not in gun_goal or "MobGunFire.fireInterval(" not in gun_goal:
        problems.append("GunAttackGoal does not fire through MobGunFire, so mobs hand-roll the shot again "
                        "(rate chain, ammo rules, sound, casing) - and guards inherit that")
    if "performGunAttack" in gun_goal or "consumeAmmo" in gun_goal:
        problems.append("GunAttackGoal spawns projectiles or spends ammo itself; both belong to MobGunFire")
    if "setFlags(" in gun_goal:
        problems.append("GunAttackGoal reserves goal flags, which blocks the other goals of every mob that "
                        "uses it - including a guard's own AI")
    # A guard must not charge in while armed: the gun AI fights at the gun's range, and the melee goal
    # calling the navigation at the same time used to win that race every tick.
    melee = strip_comments(files.get("mixin/common/compat/guardvillagers/GuardMeleeGoalMixin.java") or "")
    if "canUse" not in melee or "GunItem" not in melee:
        problems.append("the guard melee mixin no longer suppresses melee for a guard holding a gun, so an "
                        "armed guard charges into melee again")
    if "common.compat.guardvillagers.GuardMeleeGoalMixin" not in mixin_config.get("mixins", []):
        problems.append("scguns.mixins.json does not list GuardMeleeGoalMixin, so an armed guard charges "
                        "into melee again")

    # The shared firing pipeline itself: it has to do what the player's own path does (HANDOFF section
    # 82.8) - the list of things the maid compat had to rebuild by hand for maids, and which a mob's gun
    # silently skipped before this existed.
    mobfire = strip_comments(files.get("entity/ai/MobGunFire.java") or "")
    if not mobfire:
        problems.append("entity/ai/MobGunFire.java is missing - the shared mob firing pipeline is gone")
    else:
        for needle, what in (
            ("GunEnchantmentHelper.getRate", "the fire rate chain (enchantments, then attachments)"),
            ("mobFireRateMultiplier", "the mob fire rate multiplier"),
            ('getBoolean("IgnoreAmmo")', "the IgnoreAmmo rule"),
            ("ModEnchantments.RECLAIMED", "the ghost round rule"),
            ("ejectsCasing", "the casing rule"),
            ("isSilencedFire", "the silenced fire sound"),
        ):
            if needle not in mobfire:
                problems.append("MobGunFire no longer applies %s, so a mob's gun stops behaving like a "
                                "player's" % what)

    config = strip_comments(files.get("Config.java") or "")
    if "guard_gun_accuracy" not in config:
        problems.append("the guard accuracy is not configurable (no compat.guard_gun_accuracy option)")

    # Section 82.14: the three pieces of the reference 1.21.1 port's guard compat that this port was
    # missing. Each one is narrow on purpose, and each one has a rule here because each one can be
    # "simplified" back into a bug.
    hand = strip_comments(files.get("mixin/common/compat/guardvillagers/GuardRangedAttackMixin.java") or "")
    if not hand:
        problems.append("GuardRangedAttackMixin is missing, so a gun-armed guard still runs Guard "
                        "Villagers' crossbow attack against the gun in its hand")
    else:
        for needle, what in (
            ("performRangedAttack", "the crossbow attack call"),
            ("isHoldingGun", "the shared reflective hand check"),
            ("ci.cancel()", "the cancel that stops the crossbow path"),
        ):
            if needle not in hand:
                problems.append("GuardRangedAttackMixin no longer covers %s" % what)
        # An entity type in an injector signature is resolved while Mixin prepares configs, which is what
        # made the game unstartable in section 82.10.
        if re.search(r"scguns\$\w+\s*\([^)]*\b(LivingEntity|Mob|PathfinderMob|Entity)\b", hand):
            problems.append("GuardRangedAttackMixin names an entity type in its injector signature, which "
                            "is what loaded LivingEntity too early in section 82.10")

    kick = strip_comments(files.get("mixin/common/compat/guardvillagers/GuardKickGoalMixin.java") or "")
    if not kick:
        problems.append("GuardKickGoalMixin is missing, so a gun-armed guard can no longer kick a target "
                        "that reaches it (Guard Villagers gates its kick on Item.useOnRelease, which a gun "
                        "is not)")
    else:
        # The reference bypasses Guard Villagers' whole decision (`!guard.isBlocking()`), which throws away
        # kickCoolDown and makes the guard kick every tick. The cooldown has to stay in the decision.
        if "kickCoolDown" not in kick:
            problems.append("GuardKickGoalMixin no longer reads kickCoolDown, so an armed guard kicks every "
                            "tick something stays within reach instead of on Guard Villagers' cadence")
        if "isBlocking" not in kick:
            problems.append("GuardKickGoalMixin no longer keeps Guard Villagers' isBlocking condition")
        if "2.5F" not in kick:
            problems.append("GuardKickGoalMixin no longer keeps Guard Villagers' 2.5 block kick reach")

    equipment = strip_comments(files.get("mixin/common/compat/guardvillagers/GuardEquipmentMixin.java") or "")
    if not equipment:
        problems.append("GuardEquipmentMixin is missing, so Guard Villagers' own equipment can replace an "
                        "armed guard's gun through canReplaceCurrentItem")
    else:
        if "canReplaceCurrentItem" not in equipment:
            problems.append("GuardEquipmentMixin no longer hooks canReplaceCurrentItem")
        if "isGuard" not in equipment:
            problems.append("GuardEquipmentMixin is not scoped to guards, so it changes the equipment rules "
                            "of every mob in the game for the sake of one optional mod")

    for mixin_name in ("GuardRangedAttackMixin", "GuardKickGoalMixin", "GuardEquipmentMixin"):
        if "common.compat.guardvillagers." + mixin_name not in mixin_config.get("mixins", []):
            problems.append("scguns.mixins.json does not list %s, so it never applies" % mixin_name)

    # A guard's gun is a spawn-time thing (HANDOFF section 82.16). The compat has to put the gun back when
    # Guard Villagers' own equipment lands on top of it a few ticks after the join, but re-arming on every
    # equipment change - including an empty main hand, i.e. a player taking the gun - makes the compat an
    # infinite gun dispenser, which is what the player reported.
    equipment_change = method_body(spawner, "onLivingEquipmentChange")
    if "getTo().isEmpty()" not in equipment_change:
        problems.append("the equipment-change hook does not check whether the replacement is empty, so "
                        "taking a guard's gun hands it another one")
    rearm = method_body(compat, "rearmReplacedGuardGun")
    if "tickCount" not in rearm:
        problems.append("rearming a guard is not limited to the ticks around its spawn, so a guard re-arms "
                        "long after the player disarmed it")
    # The join event also fires when a chunk brings an entity back, with a fresh instance and therefore a
    # fresh spawn roll. Arming there would let a player get a new gun by unloading the chunk and returning.
    join_hook = method_body(spawner, "onEntityJoinWorld")
    if "loadedFromDisk()" not in join_hook:
        problems.append("the join hook arms a guard that was only loaded from disk, so a taken gun comes "
                        "back after a chunk reload")

    if "isHoldingGun" not in compat:
        problems.append("GuardVillagersCompat has no isHoldingGun, so the mixins inside Guard Villagers' "
                        "classes have no entity-type-free way to read the hand")

    # Section 82.17: a guard holding a gun uses the player's own gun holding animation. Guard Villagers'
    # GuardModel overrides HumanoidModel#setupAnim(LivingEntity, ...), which is where the generic mob pose
    # lives, so nothing ever posed a guard; the player's pose entry point is now widened to LivingEntity so a
    # guard - whose model is a HumanoidModel with the same skeleton - can call it.
    pose_mixin = "mixin/client/compat/guardvillagers/GuardModelGunPoseMixin.java"
    pose_text = strip_comments(files.get(pose_mixin) or "")
    if not pose_text:
        problems.append("%s is missing, so a guard holds a gun with whatever arm angles Guard Villagers' own "
                        "model animation left behind" % pose_mixin)
    else:
        if "applyPlayerModelRotation" not in pose_text:
            problems.append("the guard pose mixin does not go through the player's own pose entry point, so "
                            "a guard animates with hand written angles again")
        if "GuardModel" not in pose_text:
            problems.append("the guard pose mixin no longer targets Guard Villagers' guard model")
    if "client.compat.guardvillagers.GuardModelGunPoseMixin" not in mixin_config.get("client", []):
        problems.append("scguns.mixins.json does not list GuardModelGunPoseMixin in its client section, so a "
                        "guard is never posed")
    pose_entry = strip_comments(files.get("client/render/pose/WeaponPose.java") or "")
    if "applyPlayerModelRotation(LivingEntity" not in pose_entry:
        problems.append("WeaponPose#applyPlayerModelRotation no longer takes a LivingEntity, so a guard "
                        "cannot use the player's animation at all")
    if "isThirdPersonMeleeAttacking" in pose_entry and "instanceof Player" not in pose_entry:
        problems.append("the pose reads the local player's melee state without checking for a player, so a "
                        "player swinging a weapon drags every armed guard into the melee pose")

    # A guard armed by anything other than this config's data file still has to get the guard's gun AI and
    # keep its own follow range (section 82.14): reassessWeaponGoal used to hand it the raider's AI and the
    # raider's 64 block follow range, which sends it away from the village it defends.
    reassess = method_body(spawner, "reassessWeaponGoal")
    if "isGuard(" not in reassess:
        problems.append("reassessWeaponGoal does not route guards to the guard gun AI, so a guard armed by "
                        "a command or another mod gets the hostile raider AI")
    elif "GuardGunAttackGoal" not in reassess or "resetFollowRange(" not in reassess:
        problems.append("the guard branch of reassessWeaponGoal no longer installs GuardGunAttackGoal and "
                        "resets the follow range")

    toml = strip_comments(mods_toml) if mods_toml else ""
    if "guardvillagers" not in toml:
        problems.append("neoforge.mods.toml does not mention guardvillagers, so the optional integration "
                        "is invisible to players and to NeoForge's ordering")

    return problems


def current_files():
    files = {}
    for path in PACKAGE.rglob("*.java"):
        files[path.relative_to(PACKAGE).as_posix()] = path.read_text(encoding="utf-8", errors="replace")
    return files


def git_files(revision):
    listing = subprocess.run(["git", "ls-tree", "-r", "--name-only", revision, RELATIVE_PACKAGE],
                             capture_output=True, text=True, encoding="utf-8", check=True).stdout.split()
    files = {}
    for full in listing:
        if not full.endswith(".java"):
            continue
        files[full[len(RELATIVE_PACKAGE) + 1:]] = subprocess.run(
            ["git", "show", "%s:%s" % (revision, full)],
            capture_output=True, text=True, encoding="utf-8", check=True).stdout
    return files


def git_text(revision, path):
    return subprocess.run(["git", "show", "%s:%s" % (revision, path)],
                          capture_output=True, text=True, encoding="utf-8", check=True).stdout


def selftest():
    try:
        files = git_files(PRE_FIX_REVISION)
        gunner_json = git_text(PRE_FIX_REVISION, GUNNER_JSON.as_posix())
        mods_toml = git_text(PRE_FIX_REVISION, MODS_TOML.as_posix())
        mixin_config_text = git_text(PRE_FIX_REVISION, MIXIN_CONFIG.as_posix())
    except (subprocess.CalledProcessError, FileNotFoundError) as error:
        print("selftest: cannot read revision %s (%s)" % (PRE_FIX_REVISION, error))
        return 1

    found = check(files, gunner_json, mods_toml, mixin_config_text)
    expected = [
        "GuardVillagersCompat.java is missing",       # the compat does not exist yet
        "no guardvillagers:guard entry",
        "never reaches GuardVillagersCompat.equipGuardGun",
        "does not count GuardGunAttackGoal",
        "guard_gun_accuracy",
        "neoforge.mods.toml does not mention guardvillagers",
    ]
    print("selftest: revision %s reports %d problem(s)" % (PRE_FIX_REVISION, len(found)))
    for problem in found:
        print("   %s" % problem)
    missing = [e for e in expected if not any(e in f for f in found)]
    if missing:
        for entry in missing:
            print("selftest MISSING: %s" % entry)
        print("selftest FAILED: the audit does not notice a missing guard compat")
        return 1
    print("selftest OK: a missing integration, a stale guard id and a dead hook are all detected")
    return 0


def main():
    if "--selftest" in sys.argv:
        return selftest()
    if not COMPAT.is_dir():
        print("FAIL: %s is missing" % COMPAT)
        return 1
    problems = check(current_files(), GUNNER_JSON.read_text(encoding="utf-8"),
                     MODS_TOML.read_text(encoding="utf-8") if MODS_TOML.is_file() else "",
                     MIXIN_CONFIG.read_text(encoding="utf-8") if MIXIN_CONFIG.is_file() else "")
    for problem in problems:
        print("  %s" % problem)
    if problems:
        print("%d problem(s): the guard compat can break a server without Guard Villagers" % len(problems))
        return 1
    print("0 problem(s): the guard integration is optional-safe, wired end to end and configurable")
    return 0


if __name__ == "__main__":
    sys.exit(main())
