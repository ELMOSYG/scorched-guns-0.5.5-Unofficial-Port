"""Where raids may spawn, and when the automatic (nightly) raid may start at all.

Three rounds of player feedback live here (HANDOFF sections 75, 76 and 78):

* 0.5.5 decided "surface or cave" with `playerY < 50` and, when that said underground, walked a cave
  search **from -5 upwards** - so a player standing in the open at y < 50 got the raid placed in a cave
  below them, and a player who was mining got one in a pocket they could not find.
* Raids are therefore now placed **on the surface only**: the column's own ground level (the heightmap,
  which can never point into a cave - a cave ceiling is itself motion blocking), standable and open to
  the sky. No dimension gets an exception for having a roof; nothing places mobs at the player's level.
* The **natural** nightly raid additionally refuses to start for a player **below sea level** - and only
  that. It is deliberately differentiated from the phantom rule (`PlayerSpawnPhantomsEvent`), which also
  demands `canSeeSky`: a roof over the player's head must not stop a raid here.

This audit fails when the file:
  1. decides "underground" from a bare Y threshold again (`playerY < 50`);
  2. brings back the cave search (`findNearestValidCaveSpawn` / `yOffset = -5`);
  3. does not restrict placement to the surface (`findSurfaceSpawn` / `canSeeSky` / `isStandableSpawn`);
  4. brings back the player-level placement mechanism (`findSpawnAtPlayerLevel`, a `hasCeiling` exception);
  5. drops the sea-level gate on the nightly raid, or makes it phantom-like again (`canSeeSky` inside
     `canGetNaturalRaid`).

Usage:
    python tools/audit_raid_spawn_level.py
    python tools/audit_raid_spawn_level.py --selftest   # must FAIL against the pre-fix revision
"""

import pathlib
import re
import subprocess
import sys

SOURCE = pathlib.Path("src/main/java/top/ribs/scguns/entity/raid/RaidManager.java")
RELATIVE = "src/main/java/top/ribs/scguns/entity/raid/RaidManager.java"
# The revision these checks were written against: it still placed raids on the player's own level
# (HANDOFF section 75) instead of on the surface, and had no natural-raid gate at all. A fixed id, not
# HEAD: comparing against HEAD makes a selftest pass as soon as the fix is committed.
PRE_FIX_REVISION = "e9d8e03"


def strip_comments(text):
    """Comments describe the old behaviour on purpose - never let them count as code (the first version
    of this audit flagged its own explanation of `playerY < 50`)."""
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    return re.sub(r"//[^\n]*", "", text)


def method_body(text, name):
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


def check(text):
    text = strip_comments(text)
    problems = []

    if re.search(r"playerY\s*<\s*50", text):
        problems.append("still guesses 'underground' from a bare Y threshold (playerY < 50)")
    if "findNearestValidCaveSpawn" in text:
        problems.append("the cave search is back - it walked from -5 upwards, preferring a cave under the "
                        "player over the spot they are standing on")
    if re.search(r"yOffset\s*=\s*-5", text):
        problems.append("a spawn search again starts 5 blocks below the player")

    spawn = method_body(text, "findRaidSpawnLocation")
    if "findSurfaceSpawn" not in spawn:
        problems.append("findRaidSpawnLocation does not place the raid on the surface, so a mining player "
                        "can get a raid in a cave they cannot find")
    if "hasCeiling" in spawn:
        problems.append("placement is back to making an exception for roofed dimensions")
    if "findSpawnAtPlayerLevel" in text or "SPAWN_Y_WINDOW" in text:
        problems.append("the player-level placement mechanism is back - raids must never be placed at the "
                        "player's own level underground")

    surface = method_body(text, "findSurfaceSpawn")
    if not surface:
        problems.append("findSurfaceSpawn is missing")
    else:
        if "getHeightmapPos" not in surface:
            problems.append("findSurfaceSpawn does not use the column's own ground level")
        if "canSeeSky" not in surface:
            problems.append("findSurfaceSpawn accepts covered spots (under a canopy, an overhang or a roof)")
        if "isStandableSpawn" not in surface:
            problems.append("findSurfaceSpawn does not require a standable spot")

    nightly = method_body(text, "checkForNightlyRaidSpawn")
    if "canGetNaturalRaid(" not in nightly:
        problems.append("the nightly raid has no gate, so it still fires for a player who is mining")

    natural = method_body(text, "canGetNaturalRaid")
    if not natural:
        problems.append("canGetNaturalRaid is missing")
    else:
        if "getSeaLevel" not in natural:
            problems.append("the natural-raid gate does not compare the player against sea level")
        if "canSeeSky" in natural:
            problems.append("the natural-raid gate is phantom-like again: a roof over the player's head "
                            "must not stop a raid")

    return problems


def selftest():
    try:
        before = subprocess.run(["git", "show", "%s:%s" % (PRE_FIX_REVISION, RELATIVE)],
                                capture_output=True, text=True, check=True).stdout
    except (subprocess.CalledProcessError, FileNotFoundError) as error:
        print("selftest: cannot read %s:%s (%s)" % (PRE_FIX_REVISION, RELATIVE, error))
        return 1

    expected = [
        "findRaidSpawnLocation does not place the raid on the surface",
        "the player-level placement mechanism is back",
        "findSurfaceSpawn is missing",
        "the nightly raid has no gate",
        "canGetNaturalRaid is missing",
    ]
    found = check(before)
    print("selftest: revision %s reports %d problem(s)" % (PRE_FIX_REVISION, len(found)))
    for problem in found:
        print("   %s" % problem)
    missing = [e for e in expected if not any(e in f for f in found)]
    if missing:
        for entry in missing:
            print("selftest MISSING: %s" % entry)
        print("selftest FAILED: the audit does not catch the placement bugs it exists for")
        return 1
    print("selftest OK: the pre-surface-only placement and the missing gate are detected")
    return 0


def main():
    if "--selftest" in sys.argv:
        return selftest()
    if not SOURCE.exists():
        print("FAIL: %s is missing" % SOURCE)
        return 1
    problems = check(SOURCE.read_text(encoding="utf-8"))
    for problem in problems:
        print("  %s" % problem)
    if problems:
        print("%d problem(s): a raid can be placed underground, or start while the player is mining"
              % len(problems))
        return 1
    print("0 problem(s): raids spawn on the surface, and the nightly raid respects sea level")
    return 0


if __name__ == "__main__":
    sys.exit(main())
