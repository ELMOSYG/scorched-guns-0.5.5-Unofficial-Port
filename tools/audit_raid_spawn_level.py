"""A raid must be placed **on the surface**, next to the player - never underground.

Two rounds of bugs live here (HANDOFF sections 75 and 76):

* 0.5.5 decided "surface or cave" with `playerY < 50` and, when that said underground, walked a cave
  search **from -5 upwards** - so a player standing in the open at y < 50 (a canyon floor, a deep valley,
  diving in an ocean) got the raid placed in a cave below them, and a player in a *shallow* cave at
  y > 50 got a raid on the surface far above them.
* Searching around the player's own level fixed those, but a player who was mining still got the raid in
  a cave pocket they could not find. The rule asked for is blunt: **surface only**.

So the raid now asks each candidate column for its own ground level (the heightmap, which can never point
into a cave - a cave ceiling is itself motion blocking) and requires that spot to be standable and open to
the sky. The only exception is a dimension with a roof (the Nether), whose heightmap is its bedrock
ceiling; there the raid follows the player's own level instead.

This audit fails when the file:
  1. decides "underground" from a bare Y threshold again (`playerY < 50`);
  2. brings back the cave search and its -5-first order (`findNearestValidCaveSpawn` / `yOffset = -5`);
  3. does not restrict the overworld to the surface (`findSurfaceSpawn` / `canSeeSky`);
  4. drops the roofed-dimension fallback (`findSpawnAtPlayerLevel` / `dimensionType().hasCeiling()`).

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
# (HANDOFF section 75) instead of on the surface. A fixed id is used on purpose - comparing against HEAD
# makes the selftest a no-op the moment the fix is committed (found while writing this audit, and now the
# rule for every --selftest in this tree).
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
    if "dimensionType().hasCeiling()" not in spawn or "findSpawnAtPlayerLevel" not in spawn:
        problems.append("no roofed-dimension fallback: in the Nether the heightmap is the bedrock ceiling")

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

    if "findSpawnAtPlayerLevel" not in text or "SPAWN_Y_WINDOW" not in text:
        problems.append("the roofed-dimension search lost its bounded vertical window")

    # The natural (nightly) raid must not fire while the player is under the surface (HANDOFF 77).
    nightly = method_body(text, "checkForNightlyRaidSpawn")
    if "isUnderground(" not in nightly:
        problems.append("the nightly raid does not check whether the player is underground, so it still "
                        "fires while they are mining")
    underground = method_body(text, "isUnderground")
    if not underground:
        problems.append("isUnderground is missing")
    else:
        if "getHeightmapPos" not in underground:
            problems.append("isUnderground does not compare the player against the column's surface")
        if "UNDERGROUND_MARGIN" not in underground:
            problems.append("isUnderground has no margin, so a player indoors (roof above them) or "
                            "swimming at the ocean surface counts as underground and never gets a raid")

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
        "findSurfaceSpawn is missing",
        "no roofed-dimension fallback",
        "the nightly raid does not check whether the player is underground",
    ]
    found = check(before)
    print("selftest: revision %s reports %d problem(s)" % (PRE_FIX_REVISION, len(found)))
    for problem in found:
        print("   %s" % problem)
    missing = [e for e in expected if not any(e in f for f in found)]
    if missing:
        for entry in missing:
            print("selftest MISSING: %s" % entry)
        print("selftest FAILED: the audit does not catch the placement bug it exists for")
        return 1
    print("selftest OK: the pre-surface-only placement is detected")
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
        print("%d problem(s): a raid can be placed underground or away from the player" % len(problems))
        return 1
    print("0 problem(s): raids are placed on the surface, next to the player")
    return 0


if __name__ == "__main__":
    sys.exit(main())
