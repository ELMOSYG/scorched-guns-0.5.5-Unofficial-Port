"""A gunner's gun spawns worn, the same way the mod's own equipment files already did it (HANDOFF 82.31).

The player noticed that a gunner spawned from an egg carried a factory-new gun, while other mobs seemed to have
worn ones. Both observations are right, and they describe two different equipment paths:

  * data/scguns/entity/equipment/*.json gives the mod's own mobs (adjudicator, blunderer, cog knight, ...) a gun
    with "min_durability": 0.2 - "max_durability": 0.6, applied by EntityEquipmentConfig.
  * everything equipped from code - thematic gunners, progression gunners, Guard Villagers guards - builds a
    fresh ItemStack and never touches durability. Firing does not wear a mob's gun either (there is no
    hurtAndBreak in the mob fire path), so nothing ever brought one down.

Rules:

  1. the durability range is a config option, so it can be tuned like the JSON files can,
  2. the roll clamps into range and leaves items that cannot take damage alone,
  3. the code-driven gun path calls it,
  4. the JSON-driven path does NOT - those files state their own range, and a second roll would throw it away.

usage: python tools/audit_mob_gun_durability.py
"""
from __future__ import annotations

import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "src", "main", "java", "top", "ribs", "scguns")
CONFIG = os.path.join(SRC, "Config.java")
UTIL = os.path.join(SRC, "util", "MobGunDurability.java")
SPAWNER = os.path.join(SRC, "config", "GunnerMobSpawner.java")
EQUIPMENT = os.path.join(SRC, "config", "EntityEquipmentConfig.java")

KEYS = ("mobGunMinDurability", "mobGunMaxDurability")


def strip_comments(text: str) -> str:
    """Blank out comments, aware of string literals.

    A `/*` inside a string is not a comment. The config comment this audit checks against names a file glob -
    `data/scguns/entity/equipment/*.json` - and a stripper that ignores string literals treats that `/*` as the
    start of a block comment and deletes the code after it, which reads as "the option does not exist".
    """
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

    config = strip_comments(open(CONFIG, encoding="utf-8", errors="replace").read())
    spawner = strip_comments(open(SPAWNER, encoding="utf-8", errors="replace").read())
    equipment = strip_comments(open(EQUIPMENT, encoding="utf-8", errors="replace").read())

    # 1. config options
    for key in KEYS:
        if not re.search(r'defineInRange\(\s*"%s"' % key, config):
            problems.append('Config has no "%s" option' % key)
    lo = re.search(r'defineInRange\(\s*"mobGunMinDurability"\s*,\s*([\d.]+)', config)
    hi = re.search(r'defineInRange\(\s*"mobGunMaxDurability"\s*,\s*([\d.]+)', config)
    if lo and hi:
        low, high = float(lo.group(1)), float(hi.group(1))
        if not 0.0 <= low < high <= 1.0:
            problems.append("the durability defaults (%s - %s) are not a fraction below 1.0" % (low, high))
        elif high >= 1.0:
            problems.append("a maximum of 1.0 would leave guns at full durability, which is the reported bug")

    # 2. the roll itself
    if not os.path.isfile(UTIL):
        problems.append("util/MobGunDurability.java is missing")
    else:
        roll = method_body(strip_comments(open(UTIL, encoding="utf-8", errors="replace").read()),
                           "public static ItemStack roll(")
        if not roll:
            problems.append("MobGunDurability has no roll method")
        else:
            if "isDamageableItem()" not in roll:
                problems.append("roll does not skip items that cannot take damage")
            # both options, not just "the config is mentioned somewhere": hard-coding one end of the range still
            # leaves that end out of the player's control
            for key in KEYS:
                if key not in roll:
                    problems.append("roll does not read the %s option, so that end of the range is hard-coded"
                                    % key)
            if "clamp" not in roll:
                problems.append("roll does not clamp the damage value into range")

    # 3. the code-driven path calls it
    create = method_body(spawner, "private static ItemStack createModifiedGun(")
    if not create:
        problems.append("GunnerMobSpawner has no createModifiedGun")
    elif "MobGunDurability.roll(" not in create:
        problems.append("the code-driven gun path does not roll durability, so gunners, guards and progression "
                        "gunners spawn with factory-new guns")

    # 4. the JSON-driven path must not
    if "MobGunDurability" in equipment:
        problems.append("EntityEquipmentConfig rolls durability itself; those files state their own range per "
                        "entry and a second roll discards it")

    print("=== mob gun durability ===")
    print("  config range                           %s"
          % ("%s - %s" % (lo.group(1), hi.group(1)) if lo and hi else "MISSING"))
    print("  code-driven path rolls it              %s"
          % ("yes" if create and "MobGunDurability.roll(" in create else "NO"))
    print("  JSON-driven path untouched             %s" % ("yes" if "MobGunDurability" not in equipment else "NO"))
    print("")
    if problems:
        for problem in problems:
            print("BROKEN %s" % problem)
        print("\n%d problem(s)" % len(problems))
        return 1
    print("0 problem(s): a gunner's gun is worn on spawn, and the JSON-driven path keeps its own numbers")
    return 0


if __name__ == "__main__":
    sys.exit(main())
