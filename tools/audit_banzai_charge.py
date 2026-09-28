"""The bayonet charge is defined by its config section, and its mechanic must still hold
(HANDOFF sections 82.23 and 82.32).

Two things are guarded here.

First, the mechanic itself. 0.5.5 sustained a charge on the sprint flag in two places, and the 50 ms ticker
cancelled on `!isSprinting()` before `handleBanzaiMode` - with its knockback grace period - could ever run, so
a charge ended at the first wall it ran into. Vanilla drops that flag on every block collision, in water and
when the food bar empties. The fix moved the decision into `handleBanzaiMode`, evaluated the wall impact before
the sustain check, tolerated a lost flag while the player keeps running, and replaced the ticker per charge
instead of stacking one task per charge.

Second, where those numbers live. The player judged the mechanic poor and asked for a config section to change
it in, so every number the charge used - three damage scaling factors, two grace periods, two radii, a wall
check and the "still running" threshold - is now an option under `bayonet_charge` (HANDOFF 82.32). The rules
below therefore check the section for the knobs, the handler for reads of them, that no hard-coded charge
constant survives to shadow them, and that the switches are actually consulted.

usage: python tools/audit_banzai_charge.py
"""
from __future__ import annotations

import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "src", "main", "java", "top", "ribs", "scguns")
PACKET = os.path.join(SRC, "network", "message", "C2SMessageMeleeAttack.java")
HANDLER = os.path.join(SRC, "client", "handler", "MeleeAttackHandler.java")
CONFIG = os.path.join(SRC, "Config.java")

VANILLA_SPRINT_RETRIGGER_TICKS = 7

# Every number the mechanic used to hard-code, as an option under bayonet_charge.
KNOBS = (
    "enabled",
    "requireSprintToStart",
    "singleTargetStab",
    "damageRadius",
    "hitRadius",
    "damageIntervalTicks",
    "damageScalingLevel1",
    "damageScalingLevel2",
    "damageScalingLevel3",
    "executeEnabled",
    "executeHealthThreshold",
    "knockPlayerBackOnHit",
    "endChargeOnHit",
    "knockbackGraceTicks",
    "sprintLossToleranceTicks",
    "minimumForwardSpeed",
    "wallImpactEnabled",
    "wallImpactCooldownTicks",
    "wallCheckDistance",
    "wallCheckSpreadDegrees",
)

# Constants that must be gone: their values are config now, and a leftover would silently shadow it.
RETIRED_CONSTANTS = (
    "BANZAI_SCALING_FACTORS",
    "KNOCKBACK_GRACE_PERIOD_TICKS",
    "BANZAI_SPRINT_LOST_TOLERANCE_TICKS",
    "BANZAI_MIN_FORWARD_SPEED",
    "WALL_COLLISION_COOLDOWN_TICKS",
    "WALL_CHECK_DISTANCE",
    "WALL_CHECK_ANGLES",
    "BANZAI_DAMAGE_COOLDOWN_TICKS",
    "BANZAI_AOE_RADIUS",
)


def strip_comments(text: str) -> str:
    """Blank out comments, aware of string literals (a `/*` inside a string is not a comment)."""
    out = []
    i = 0
    n = len(text)
    while i < n:
        c = text[i]
        if c == '"':
            out.append(c)
            i += 1
            while i < n:
                if text[i] == "\\":
                    out.append(text[i:i + 2])
                    i += 2
                    continue
                out.append(text[i])
                if text[i] == '"':
                    i += 1
                    break
                i += 1
        elif c == "/" and i + 1 < n and text[i + 1] == "/":
            while i < n and text[i] != "\n":
                i += 1
        elif c == "/" and i + 1 < n and text[i + 1] == "*":
            i += 2
            while i + 1 < n and not (text[i] == "*" and text[i + 1] == "/"):
                if text[i] == "\n":
                    out.append("\n")
                i += 1
            i += 2
        else:
            out.append(c)
            i += 1
    return "".join(out)


def method_body(text: str, signature: str) -> str:
    at = text.find(signature)
    if at < 0:
        return ""
    start = text.find("{", at)
    if start < 0:
        return ""
    depth = 0
    for i in range(start, len(text)):
        if text[i] == "{":
            depth += 1
        elif text[i] == "}":
            depth -= 1
            if depth == 0:
                return text[start:i + 1]
    return ""


def main() -> int:
    problems: list[str] = []

    packet = strip_comments(open(PACKET, encoding="utf-8", errors="replace").read())
    handler = strip_comments(open(HANDLER, encoding="utf-8", errors="replace").read())
    config = strip_comments(open(CONFIG, encoding="utf-8", errors="replace").read())

    # --- the mechanic's lifecycle (section 82.23) -----------------------------------------------------
    at = packet.find("scheduleAtFixedRate")
    ticker = packet[at:packet.find("\n   }", at)] if at >= 0 else ""
    if "isSprinting" in ticker:
        problems.append("the banzai ticker checks the sprint flag again, so it cancels the charge before "
                        "handleBanzaiMode - and its knockback grace period - can ever run")
    if "handleBanzaiMode" not in ticker:
        problems.append("the banzai ticker no longer drives handleBanzaiMode")
    if "banzaiTask" not in packet or ticker.count(".cancel(") == 0:
        problems.append("the ticker is not cancelled, so every charge leaves another repeating task behind")

    body = method_body(handler, "public static void handleBanzaiMode(")
    if not body:
        problems.append("MeleeAttackHandler has no handleBanzaiMode")
    else:
        if "keepCharging(" not in body:
            problems.append("handleBanzaiMode does not use keepCharging, so a lost sprint flag still ends the "
                            "charge")
        if re.search(r"!\s*player\.isSprinting\(\)", body):
            problems.append("handleBanzaiMode stops on a bare !isSprinting() again")
        wall = body.find("checkForWallCollision(")
        sustain = body.find("keepCharging(")
        if wall < 0:
            problems.append("handleBanzaiMode no longer reacts to a wall impact")
        elif sustain < 0 or wall > sustain:
            problems.append("the sustain check runs before the wall impact, so the charge's own knockback - "
                            "which is what drops the sprint flag - ends it")
        if "isWallImpactEnabled()" not in body:
            problems.append("the wall impact is not behind its own switch")

    # --- the numbers live in the config section now (section 82.32) -----------------------------------
    if not method_body(config, "public BayonetCharge(Builder builder) {"):
        problems.append("Config has no bayonet_charge section")
    for knob in KNOBS:
        if not re.search(r'"%s"' % knob, config):
            problems.append('the bayonet charge has no "%s" option' % knob)
        if knob not in handler:
            problems.append("MeleeAttackHandler never reads bayonetCharge.%s, so that option does nothing" % knob)
    for retired in RETIRED_CONSTANTS:
        if re.search(r"\b%s\b" % retired, handler):
            problems.append("%s is still in MeleeAttackHandler; its value is a config option now, and a "
                            "leftover constant would shadow it" % retired)

    # the two defaults that have to satisfy something outside this mod
    grace = re.search(r'defineInRange\(\s*"knockbackGraceTicks"\s*,\s*(\d+)', config)
    if not grace:
        problems.append("no knockbackGraceTicks default")
    elif int(grace.group(1)) <= VANILLA_SPRINT_RETRIGGER_TICKS:
        problems.append("knockbackGraceTicks defaults to %s, which is not longer than vanilla's %d-tick "
                        "sprintTriggerTime, so a wall bump still ends the charge"
                        % (grace.group(1), VANILLA_SPRINT_RETRIGGER_TICKS))
    speed = re.search(r'defineInRange\(\s*"minimumForwardSpeed"\s*,\s*([\d.]+)', config)
    if not speed:
        problems.append("no minimumForwardSpeed default")
    elif not 0.0 < float(speed.group(1)) < 0.2:
        problems.append("minimumForwardSpeed defaults to %s; a walking player moves about 0.2, so a threshold "
                        "at or above that would end the charge while the player is still running"
                        % speed.group(1))

    # --- the mechanic the player asked for: one target, the gun's melee damage, a recoil, an execution ---
    stab = method_body(handler, "private static void stabWithBayonet(")
    if not stab:
        problems.append("no stabWithBayonet: the charge has no single target attack")
    else:
        if "getMeleeDamage()" not in stab and "meleeDamageOf(" not in stab:
            problems.append("the stab does not use the gun's own melee damage, which is what the mechanic is "
                            "supposed to deal")
        if "Float.MAX_VALUE" not in stab:
            problems.append("the execution is not a kill: it deals ordinary damage instead of an overwhelming "
                            "hit (which is also what keeps loot and kill credit on the player)")
        if "isKnockPlayerBackOnHit()" not in stab:
            problems.append("the stab does not throw the player back behind its own switch")
        if "isEndChargeOnHit()" not in stab:
            problems.append("the stab does not decide whether it spends the charge")
    # the hostile-only check and its two options live in the decision helper, not in the stab itself
    decision = method_body(handler, "private static boolean isExecutionTarget(")
    if not decision:
        problems.append("no isExecutionTarget helper")
    else:
        if "instanceof Enemy" not in decision:
            problems.append("the execution does not check for a hostile mob, so it would kill anything already "
                            "below the threshold - including the player's own pets and villagers")
        if "executeEnabled" not in decision or "executeHealthThreshold" not in decision:
            problems.append("the execution is not behind its two config options")
    # --- both mechanics exist, and the original one is what ships (section 82.34) ----------------------
    mode = re.search(r'\.define\(\s*"singleTargetStab"\s*,\s*(true|false)\s*\)', config)
    if not mode:
        problems.append("no singleTargetStab option: the two mechanics cannot be chosen between")
    elif mode.group(1) != "false":
        problems.append("singleTargetStab defaults to true: the original charge is the shipped mechanic and the "
                        "thrust is meant to be the option")
    if body and "isSingleTargetStab()" not in body:
        problems.append("handleBanzaiMode never consults the mechanic switch")
    if body and "stabWithBayonet(" not in body:
        problems.append("handleBanzaiMode never stabs, so the optional mechanic cannot be reached")
    if body and "areaSweep(" not in body:
        problems.append("handleBanzaiMode never sweeps, so the original area charge is gone")
    sweep = method_body(handler, "private static void areaSweep(")
    if not sweep:
        problems.append("no areaSweep: the original area charge is missing")
    elif "getBanzaiDamageMultiplier(" not in sweep:
        problems.append("the area sweep no longer scales damage with speed, which is part of the original "
                        "mechanic")
    if body and re.search(r"for\s*\(\s*LivingEntity", body):
        problems.append("handleBanzaiMode loops over targets itself; the area sweep belongs in areaSweep")
    if body and "performMeleeAttackOnTarget(" in body:
        problems.append("handleBanzaiMode calls performMeleeAttackOnTarget, the retired area path")
    # the speed scaling belongs to the original mechanic; the thrust deals the gun's own melee damage
    if stab and "getBanzaiDamageMultiplier(" in stab:
        problems.append("the single target stab applies the speed scaling, so it no longer deals the gun's own "
                        "melee damage")
    for level in ("1", "2", "3"):
        factor = re.search(r'defineInRange\(\s*"damageScalingLevel%s"\s*,\s*([\d.]+)' % level, config)
        if not factor:
            problems.append("no damageScalingLevel%s default" % level)
        elif float(factor.group(1)) <= 0.0:
            problems.append("damageScalingLevel%s defaults to %s: the original area charge scales its damage "
                            "with speed, so a zero factor changes the shipped mechanic"
                            % (level, factor.group(1)))

    # --- the switches are wired, not decorative -------------------------------------------------------
    handle = method_body(packet, "public void handle(")
    if handle and "isBanzaiEnabled()" not in handle:
        problems.append("the disabled switch is never consulted, so turning the charge off does nothing")
    elif handle and "handleNormalMeleeAttack(" not in handle:
        problems.append("with the charge switched off there is no fallback attack, so the melee key would do "
                        "nothing at all")
    if handle and "isSprintRequiredToStart()" not in handle:
        problems.append("requireSprintToStart is never consulted")

    # --- saved-data tags stay constants, never literals -----------------------------------------------
    for literal in ("KnockbackGracePeriod", "WallCollisionCooldown", "BanzaiDamageCooldown", "BanzaiSprintLostAt"):
        if handler.count('"%s"' % literal) != 1:
            problems.append('the tag "%s" is used as a literal outside its constant' % literal)

    print("=== bayonet charge ===")
    print("  defined by                          Config.COMMON.bayonetCharge (%d options)" % len(KNOBS))
    print("  ticker re-checks the sprint flag    %s" % ("YES (wrong)" if "isSprinting" in ticker else "no"))
    print("  sustain decision                    %s"
          % ("handleBanzaiMode: wall -> keepCharging -> damage" if body else "MISSING"))
    print("  switches wired                      enabled:%s sprint:%s wall:%s"
          % ("yes" if handle and "isBanzaiEnabled()" in handle else "NO",
             "yes" if handle and "isSprintRequiredToStart()" in handle else "NO",
             "yes" if "isWallImpactEnabled()" in body else "NO"))
    print("")
    if problems:
        for problem in problems:
            print("BROKEN %s" % problem)
        print("\n%d problem(s)" % len(problems))
        return 1
    print("0 problem(s): the charge's numbers all live in its own config section, and the mechanic still holds")
    return 0


if __name__ == "__main__":
    sys.exit(main())
