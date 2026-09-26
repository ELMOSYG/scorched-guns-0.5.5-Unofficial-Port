"""A raid must be placed on the player's own level, not in a cave under their feet.

0.5.5 decided "surface or cave" with `playerY < 50` and, when that said underground, walked a cave
search **from -5 upwards**:

    for (int yOffset = -5; yOffset <= 5; yOffset++) { ... }

so a player standing in the open at y < 50 - a canyon floor, a deep valley, diving in an ocean - got the
raid placed in a cave below them (the reported bug: "raids sometimes spawn underground, in the cave
layer"), while a player in a *shallow* cave at y > 50 got a raid on the surface far above them. Both are
the same mistake: the player's own Y is the only thing that matters, and it should be tried first.

The fix searches each candidate column around the player's Y (±SPAWN_Y_WINDOW), falls back to the
column's own ground level, and only accepts that fallback when it is within the same window - so a
canyon rim or a mountain top above the player is never chosen.

This audit fails when the file:
  1. decides "underground" from a bare Y threshold again (`playerY < 50`);
  2. brings back the cave search and its -5-first order (`findNearestValidCaveSpawn` / `yOffset = -5`);
  3. does not try the player's own level before anything else (`findSpawnAtPlayerLevel`);
  4. accepts a fallback outside the player's Y window.

Usage:
    python tools/audit_raid_spawn_level.py
    python tools/audit_raid_spawn_level.py --selftest   # must FAIL against the pre-fix source
"""

import pathlib
import re
import subprocess
import sys

SOURCE = pathlib.Path("src/main/java/top/ribs/scguns/entity/raid/RaidManager.java")
RELATIVE = "src/main/java/top/ribs/scguns/entity/raid/RaidManager.java"


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


def strip_comments(text):
    """Comments describe the old behaviour on purpose - never let them count as code (the first version
    of this audit flagged its own explanation of `playerY < 50`)."""
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    return re.sub(r"//[^\n]*", "", text)


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
    if "findSpawnAtPlayerLevel" not in spawn:
        problems.append("findRaidSpawnLocation does not search around the player's own level")
    if "SPAWN_Y_WINDOW" not in text:
        problems.append("no SPAWN_Y_WINDOW: the vertical search is unbounded")

    at_level = method_body(text, "findSpawnAtPlayerLevel")
    if not at_level:
        problems.append("findSpawnAtPlayerLevel is missing")
    else:
        if "playerY" not in at_level.split("\n")[1] if len(at_level.split("\n")) > 1 else True:
            problems.append("findSpawnAtPlayerLevel does not try the player's own Y first")
        if "Math.abs(ground.getY() - playerY) <= SPAWN_Y_WINDOW" not in at_level:
            problems.append("the ground-level fallback is not clamped to the player's Y window")

    return problems


def selftest():
    try:
        before = subprocess.run(["git", "show", "HEAD:%s" % RELATIVE],
                                capture_output=True, text=True, check=True).stdout
    except (subprocess.CalledProcessError, FileNotFoundError) as error:
        print("selftest: cannot read HEAD:%s (%s)" % (RELATIVE, error))
        return 1

    expected = [
        "playerY < 50",
        "the cave search is back",
        "a spawn search again starts 5 blocks below the player",
        "findRaidSpawnLocation does not search around the player's own level",
    ]
    found = check(before)
    print("selftest: pre-fix source reports %d problem(s)" % len(found))
    for problem in found:
        print("   %s" % problem)
    missing = [e for e in expected if not any(e in f for f in found)]
    if missing:
        for entry in missing:
            print("selftest MISSING: %s" % entry)
        print("selftest FAILED: the audit does not catch the placement bug it exists for")
        return 1
    print("selftest OK: the pre-fix raid placement bug is detected")
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
        print("%d problem(s): a raid can be placed away from the player's level" % len(problems))
        return 1
    print("0 problem(s): raids are placed on the player's own level")
    return 0


if __name__ == "__main__":
    sys.exit(main())
