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

    named = guard_class_files(files)
    if named != ["compat/guardvillagers/GuardFriendlyRules.java"]:
        problems.append("Guard Villagers classes are named in %s - only GuardFriendlyRules may, because it "
                        "is reached behind the guard-id check; anything else can fail to link on a server "
                        "without the mod" % (named or "no file"))

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
    if "setFlags(" in goal:
        problems.append("the guard gun goal reserves goal flags again, which blocks Guard Villagers' own "
                        "movement goals for as long as the guard has a target")
    # Movement belongs to Guard Villagers: this goal aims and fires, nothing else. Steering the navigation
    # (approach, back away, step aside) fights the guard's own goals and is what made an armed guard behave
    # like a raider in the first place.
    for needle in ("getNavigation(", "getMoveControl("):
        if needle in goal:
            problems.append("the guard gun goal steers movement again (%s); Guard Villagers' own goals own "
                            "the guard's movement" % needle)

    events = strip_comments(files.get("compat/guardvillagers/GuardVillagersEvents.java") or "")
    for needle in ("LivingIncomingDamageEvent", "LivingKnockBackEvent", "GuardVillagersCompat.isFriendlyShot"):
        if needle not in events:
            problems.append("the friendly-fire handler no longer covers %s" % needle)

    if "AIGunEvent.performGunAttack" not in goal:
        problems.append("the guard gun goal does not fire through AIGunEvent, so a guard's shots would "
                        "not use the mod's projectile path")
    if GUARD_PACKAGE in goal:
        problems.append("the guard gun goal names a Guard Villagers class, which forces that class to load")
    # Cadence (HANDOFF section 82): a guard's rhythm has to come from the same config the mod's own
    # gunners use - otherwise a server that slows its gunners down sees no change in the guards, and a
    # guard with a 2-tick semi-automatic fires ten shots a second, because the raw rate is a trigger
    # interval and not a full-auto one.
    for needle, what in (("mobFireRateMultiplier", "the mob fire rate multiplier"),
                         ("mobBurstDelayMultiplier", "the burst delay multiplier"),
                         ("burstResetTimer", "the pause between bursts")):
        if needle not in goal:
            problems.append("the guard gun goal ignores %s, so a guard does not shoot at the cadence the "
                            "mod's own gunners use" % what)
    if re.search(r"Math\.max\(\s*10\s*,\s*Math\.min\(.*getReloadTimer", goal):
        problems.append("the guard's reload is clamped to 10..40 ticks again: it has to use the gun's own "
                        "reload time like every other gunner")

    config = strip_comments(files.get("Config.java") or "")
    if "guard_gun_accuracy" not in config:
        problems.append("the guard accuracy is not configurable (no compat.guard_gun_accuracy option)")

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
