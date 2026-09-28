"""Simulate the three progression chat lines for each tier, reading the real tables and lang file.

Prints what a player will actually see after the fix in HANDOFF 82.29, so the wording can be checked without
levelling a character. Pre-fix output for frontier was "现在可能会出现【古典】的敌人和袭击！" - the tier below
the one just obtained - plus a raid line repeating what antique already announced.
"""
import glob
import json
import re

SRC = "src/main/java/top/ribs/scguns/entity/player/GunTiers.java"
LANG = "src/main/resources/assets/scguns/lang/zh_cn.json"

tiers = {}
order = []
for line in open(SRC, encoding="utf-8"):
    m = re.search(r'register\("(\w+)",\s*(\d+),\s*(?:null|"[^"]*"),\s*(\d+)\)', line)
    if m:
        tiers[m.group(1)] = {"level": int(m.group(2)), "raid": int(m.group(3)), "prev": []}
        order.append(m.group(1))
    if order:
        for prev in re.findall(r'addPreviousTier\("(\w+)"\)', line):
            tiers[order[-1]]["prev"].append(prev)

lang = json.load(open(LANG, encoding="utf-8"))


def tier_name(tier):
    return lang.get("gun_tier.scguns." + tier, "?" + tier)


raids = {}
for path in glob.glob("src/main/resources/data/scguns/raids/*.json"):
    data = json.load(open(path, encoding="utf-8"))
    level = data.get("raid_level")
    if level is not None:
        raids.setdefault(level, []).append(data["raid_id"])

unlock = lang["progression.scguns.tier_unlocked"]
mobs = lang["progression.scguns.enemies_can_spawn"]
raid_line = lang["progression.scguns.raids_can_spawn"]
sep = lang["progression.scguns.list_separator"]


def raid_name(raid):
    return lang.get("raid.scguns." + raid, raid)


print("放出来的三行（修复后）：")
for tier in order:
    info = tiers[tier]
    if info["level"] == 0:
        continue
    available = [p for p in info["prev"]] + [tier]
    # mirrors GunTier#getAvailableMobTiersNewestFirst: sorted by level, descending
    available.sort(key=lambda name: -tiers[name]["level"])
    previous_max = max([tiers[p]["raid"] for p in info["prev"]] or [0])
    new_raids = [r for level in range(previous_max + 1, info["raid"] + 1) for r in raids.get(level, [])]

    print("")
    print("  解锁【%s】" % tier_name(tier))
    print("    " + unlock.replace("%s", tier_name(tier)))
    print("    " + mobs.replace("%s", sep.join(tier_name(t) for t in available)))
    if new_raids:
        print("    " + raid_line.replace("%s", sep.join(raid_name(r) for r in new_raids)))
    else:
        print("    （本层级没有新突袭，这一行不再出现）")
