"""Progression messages must say something true, and mob spawning must not (HANDOFF 82.29, 83).

The unlock message read: "你获得了【铁】的枪械！ / 现在可能会出现【铜、边疆、古典】的敌人和袭击！" - two problems in
one line. It named the tiers *below* the one just obtained, and it promised raids for a gun tier.

  * the message listed `previousTierIds` only, leaving out the tier just obtained. Fixing that by
    making `getAvailableMobTiers()` include itself was the wrong repair: `GunnerMobSpawner` reads
    that same list, so mobs started carrying the player's newest tier the instant it was reached -
    the world kept pace with the player step for step, which is not what a progression is for
    (0.5.5 deliberately lagged: at 古典 the list is empty and no gunner spawns at all). The two
    questions now have their own methods, and this audit exists to stop them being merged again.
  * the sentence attached raids to tier names. Raids are not tier-bound - they are named on their own line,
    from the raid level - so "【铜】的袭击" claims something that does not exist.

Rules:

  1. `getAvailableMobTiers()` (the spawner's list) is the previous tiers only - 0.5.5's rule, and
     the reason a player at 古典 sees no armed mobs,
  2. `getAvailableMobTiersNewestFirst()` (the message's list) includes this tier, guards the level 0
     "none" tier, and sorts by level descending,
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
SPAWNER = os.path.join(SRC, "config", "GunnerMobSpawner.java")
COMMANDS = os.path.join(SRC, "init", "ModCommands.java")
LANG = os.path.join(ROOT, "src", "main", "resources", "assets", "scguns", "lang")

ENEMIES_KEY = "progression.scguns.enemies_can_spawn"
OLD_KEY = "progression.scguns.enemies_and_raids_can_spawn"
# Words that would promise raids inside a sentence about gun tiers.
RAID_WORDS = ("raid", "袭击")


def strip_comments(text: str) -> str:
    """Blank out comments, aware of string literals.

    A `/*` inside a string is not a comment: this codebase has config comments naming file globs such as
    `data/scguns/entity/equipment/*.json`, and a stripper that ignores string literals treats that `/*` as a
    block comment start and deletes the code after it. That reads as "the thing does not exist" - a false
    negative, which is the one failure an audit must not have.
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

    tier = strip_comments(open(TIER, encoding="utf-8", errors="replace").read())
    event = strip_comments(open(EVENT, encoding="utf-8", errors="replace").read())
    spawner = strip_comments(open(SPAWNER, encoding="utf-8", errors="replace").read())
    commands = strip_comments(open(COMMANDS, encoding="utf-8", errors="replace").read())
    en = json.load(open(os.path.join(LANG, "en_us.json"), encoding="utf-8"))
    zh = json.load(open(os.path.join(LANG, "zh_cn.json"), encoding="utf-8"))

    # 1. the spawner's list must stay 0.5.5's: previous tiers only, and no `this`
    available = method_body(tier, "public List<GunTier> getAvailableMobTiers(")
    if not available:
        problems.append("GunTier has no getAvailableMobTiers")
    elif "tiers.add(this)" in available:
        problems.append("getAvailableMobTiers adds the tier itself again: GunnerMobSpawner reads this "
                        "list, so mobs would carry the player's newest tier the moment it is reached "
                        "instead of lagging a tier behind as in 0.5.5")
    if available and "previousTierIds" not in available:
        problems.append("getAvailableMobTiers no longer derives from previousTierIds")

    # 1b. and the spawner has to be reading that list, not the message's ordering
    if "getAvailableMobTiers()" not in spawner:
        problems.append("GunnerMobSpawner no longer reads getAvailableMobTiers(): whatever it reads now "
                        "decides which guns mobs spawn with")
    if "getAvailableMobTiersNewestFirst" in spawner:
        problems.append("GunnerMobSpawner reads getAvailableMobTiersNewestFirst, which is a sorted copy of "
                        "the same list - the spawner weights it by position, so sorting would make the "
                        "oldest tier the most common pick")
    # The two diagnostic commands print what mobs can carry, so they follow the spawner.
    for command_body in ("executeCheckProgression", "executeInfoTier"):
        body = method_body(commands, "private static int %s(" % command_body)
        if body and "getAvailableMobTiersNewestFirst" in body:
            problems.append("/scguns %s reports the message's ordering, so it would not match the "
                            "spawner's own pick order" % command_body.replace("execute", ""))

    # 2. the notice's list is the SAME set, only ordered for a sentence - and it must not add the
    #    tier just obtained. The player saw that on screen: at the antique tier the notice promised
    #    "【antique】 enemies may now appear" while the spawner, whose previous-tier list is empty
    #    there, spawns no armed mob at all.
    newest_first = method_body(tier, "public List<GunTier> getAvailableMobTiersNewestFirst(")
    if not newest_first:
        problems.append("no getAvailableMobTiersNewestFirst helper: the notice would have to print the "
                        "spawner's declared order, which is only coincidentally newest first")
    else:
        if "tiers.add(this)" in newest_first:
            problems.append("getAvailableMobTiersNewestFirst adds the tier just obtained: the notice would "
                            "promise mobs carrying the newest tier, which the spawner never creates - "
                            "mobs lag a tier behind the player")
        if "getAvailableMobTiers()" not in newest_first:
            problems.append("getAvailableMobTiersNewestFirst no longer derives from getAvailableMobTiers(), so "
                            "the notice and the spawner can disagree about what a mob may carry")
        if "getLevel" not in newest_first or "reversed" not in newest_first:
            problems.append("getAvailableMobTiersNewestFirst does not sort by level descending")
    unlock = method_body(event, "public static void sendTierUnlockedMessage(")
    # The line must be skipped when there is nothing to announce, not printed with an empty list.
    if unlock and not re.search(r"if\s*\(\s*!\s*\w+\s*\.isEmpty\(\)\s*\)", unlock):
        problems.append("the notice does not skip the enemies line when the list is empty: at the first tier "
                        "nothing armed spawns, so the line must be absent rather than empty")
    if unlock and "getAvailableMobTiersNewestFirst(" not in unlock:
        problems.append("the notice does not read the newest-first ordering of the spawner's list")
    if unlock and re.search(r"getAvailableMobTiers\(\)", unlock):
        problems.append("the notice reads the spawner's unsorted list: its declared order is only "
                        "coincidentally newest first, so the sentence could read backwards")

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
    print("  spawner list (previous tiers only)     %s"
          % ("yes" if available and "tiers.add(this)" not in available else "NO - includes itself"))
    print("  message list (same tiers, ordered)     %s"
          % ("yes" if newest_first and "tiers.add(this)" not in newest_first else "NO"))
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
    print("0 problem(s): mobs lag a tier behind the player as in 0.5.5, the notice lists exactly the "
          "tiers a gunner can now carry, and it mentions no raids")
    return 0


if __name__ == "__main__":
    sys.exit(main())
