"""Progression messages must say something true (HANDOFF 82.29).

The unlock message read: "你获得了【铁】的枪械！ / 现在可能会出现【铜、边疆、古典】的敌人和袭击！" - two problems in
one line. It named the tiers *below* the one just obtained, and it promised raids for a gun tier.

  * `GunTier.getAvailableMobTiers()` returned `previousTierIds` only, leaving the tier itself out. The spawner
    reads that same list, so a gunner could never be equipped with the player's newest tier: the newest tier
    was permanently unreachable, and the sentence announced the tier below instead.
  * the sentence attached raids to tier names. Raids are not tier-bound - they are named on their own line,
    from the raid level - so "【铜】的袭击" claims something that does not exist.

Rules:

  1. the mob tier list includes the tier itself, so the newest tier is reachable and the message can name it,
  2. the message lists tiers newest first, and separately from the spawner's own ordering (which puts the
     newest last so it stays the rare pick),
  3. the enemies sentence mentions enemies only - neither its key nor its text may promise raids,
  4. the raid line announces only raids this tier actually adds, so a tier that unlocks none says nothing.

usage: python tools/audit_progression_messages.py
"""
from __future__ import annotations

import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "src", "main", "java", "top", "ribs", "scguns")
TIER = os.path.join(SRC, "entity", "player", "GunTier.java")
EVENT = os.path.join(SRC, "event", "GunProgressionEventHandler.java")
LANG = os.path.join(ROOT, "src", "main", "resources", "assets", "scguns", "lang")

ENEMIES_KEY = "progression.scguns.enemies_can_spawn"
OLD_KEY = "progression.scguns.enemies_and_raids_can_spawn"
# Words that would promise raids inside a sentence about gun tiers.
RAID_WORDS = ("raid", "袭击")


def strip_comments(text: str) -> str:
    out = []
    i = 0
    n = len(text)
    while i < n:
        c = text[i]
        if c == "/" and i + 1 < n and text[i + 1] == "/":
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

    tier = strip_comments(open(TIER, encoding="utf-8", errors="replace").read())
    event = strip_comments(open(EVENT, encoding="utf-8", errors="replace").read())
    en = json.load(open(os.path.join(LANG, "en_us.json"), encoding="utf-8"))
    zh = json.load(open(os.path.join(LANG, "zh_cn.json"), encoding="utf-8"))

    # 1. the tier itself has to be in its own list
    available = method_body(tier, "public List<GunTier> getAvailableMobTiers(")
    if not available:
        problems.append("GunTier has no getAvailableMobTiers")
    else:
        if "tiers.add(this)" not in available:
            problems.append("getAvailableMobTiers still returns only the previous tiers: the newest tier can "
                            "never be equipped on a gunner, and the unlock message names the tier below the "
                            "one just obtained")
        if not re.search(r"level\s*>\s*0", available):
            problems.append("the tier itself is added without a level guard, so the level 0 \"none\" tier would "
                            "be offered to the spawner")

    # 2. message order is its own thing
    newest_first = method_body(tier, "public List<GunTier> getAvailableMobTiersNewestFirst(")
    if not newest_first:
        problems.append("no getAvailableMobTiersNewestFirst helper: the message would print the spawner's order, "
                        "which puts the newest tier last")
    elif "getLevel" not in newest_first or "reversed" not in newest_first:
        problems.append("getAvailableMobTiersNewestFirst does not sort by level descending")
    unlock = method_body(event, "public static void sendTierUnlockedMessage(")
    if unlock and "getAvailableMobTiersNewestFirst(" not in unlock:
        problems.append("the unlock message does not use the newest-first list")

    # 3. enemies only
    if unlock and ENEMIES_KEY not in unlock:
        problems.append("the unlock message does not use %s" % ENEMIES_KEY)
    for name, data in (("en_us", en), ("zh_cn", zh)):
        if OLD_KEY in data:
            problems.append("%s still defines %s" % (name, OLD_KEY))
        if ENEMIES_KEY not in data:
            problems.append("%s does not define %s" % (name, ENEMIES_KEY))
            continue
        text = data[ENEMIES_KEY]
        for word in RAID_WORDS:
            if word in text:
                problems.append('%s: "%s" still promises raids in a sentence about gun tiers' % (name, text))

    # 4. the raid line only reports what is new
    raids = method_body(event, "private static void sendRaidUnlockedMessage(")
    if not raids:
        problems.append("no sendRaidUnlockedMessage")
    else:
        if "getPreviousTierIds()" not in raids:
            problems.append("the raid line does not look at the previous tiers at all")
        # The bound has to come from the previous tiers' own raid levels. A constant (0, or a sentinel) would
        # pass a check that only asks whether the name appears, while making every raid look new again.
        if not re.search(r"previousRaidLevel\s*=\s*Math\.max\([^;]*getRaidLevel\(\)", raids):
            problems.append("the previous raid level is not derived from the previous tiers' getRaidLevel(): a "
                            "constant makes every raid look newly unlocked, which is the repetition bug")
        if not re.search(r"level\s*>\s*previousRaidLevel", raids):
            problems.append("raids are not filtered to those above the previous level")
        if "newRaids.isEmpty()" not in raids:
            problems.append("the raid line does not skip itself when the tier adds no raid")

    print("=== progression messages ===")
    print("  mob tier list includes itself          %s"
          % ("yes" if available and "tiers.add(this)" in available else "NO"))
    print("  message order                          %s"
          % ("newest first" if newest_first else "spawner order (wrong for a message)"))
    print("  enemies sentence                       %s"
          % (repr(en.get(ENEMIES_KEY)) if ENEMIES_KEY in en else "MISSING"))
    print("  raid line                              %s"
          % ("only what is new" if raids and "previousRaidLevel" in raids else "repeats the level's list"))
    print("")
    if problems:
        for problem in problems:
            print("BROKEN %s" % problem)
        print("\n%d problem(s)" % len(problems))
        return 1
    print("0 problem(s): the unlock message names the tier just obtained, and mentions no raids")
    return 0


if __name__ == "__main__":
    sys.exit(main())
