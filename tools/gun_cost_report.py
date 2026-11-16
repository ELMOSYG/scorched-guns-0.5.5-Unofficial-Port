"""Exact bill of materials per gun, from the gun bench slot layout.

Part costs are read from the mod's own recipes and written out here explicitly, because the
generic recipe walker cannot tell "one iron block pressed into two gun parts" from "one iron
ingot" without knowing which machine recipe is the real path:

    firing_unit          iron 1   + 晶碳 1   + 1 iron nugget
    rapid_firing_unit    iron 1   + 晶碳 1   + 2 晶碳 nuggets + 1 iron nugget
    gun_parts            iron 4.5            (press: 1 iron block -> 2)
    heavy_gun_parts      晶碳 4.5            (press: 1 anthralite block -> 2)
    gun_barrel           晶碳 3
    heavy_gun_barrel     晶碳 5
    gun_grip             planks 2 + 1 iron nugget
    gun_magazine         iron 2   + 晶碳 1/3
    iron_gun_frame       iron 6
    diamond_steel_gun_frame  diamond steel 6
    stone_gun_barrel     stone 3

Usage:
    python tools/gun_cost_report.py            # one line per gun, grouped by blueprint tier
    python tools/gun_cost_report.py --tier diamond_steel
    python tools/gun_cost_report.py --markdown # table for the docs
"""
from __future__ import annotations

import argparse
import json
import os
import statistics
import sys
from collections import Counter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
GUNS = os.path.join(ROOT, "src", "main", "resources", "data", "scguns", "recipe", "guns")

NUGGET = 1 / 9

PART_COST = {
    "scguns:firing_unit": {"iron": 1 + NUGGET, "anthralite": 1},
    "scguns:rapid_firing_unit": {"iron": 1 + NUGGET, "anthralite": 1 + 2 * NUGGET},
    "scguns:gun_parts": {"iron": 4.5},
    "scguns:heavy_gun_parts": {"anthralite": 4.5},
    "scguns:gun_barrel": {"anthralite": 3},
    "scguns:heavy_gun_barrel": {"anthralite": 5},
    "scguns:gun_grip": {"planks": 2, "iron": NUGGET},
    "scguns:gun_magazine": {"iron": 2, "anthralite": 1 / 3},
    "scguns:iron_gun_frame": {"iron": 6},
    "scguns:diamond_steel_gun_frame": {"diamond_steel": 6},
    "scguns:treated_brass_gun_frame": {"treated_brass": 6},
    "scguns:treated_iron_gun_frame": {"treated_iron": 6},
    "scguns:copper_gun_frame": {"copper": 6},
    "scguns:scorched_gun_frame": {"scorched": 6},
    "scguns:empty_tank": {"iron": 0.5, "bucket": 0.5},
    "scguns:wooden_stock": {"planks": 4, "anthralite": 1},
    "scguns:stone_gun_barrel": {"stone": 3},
}

RAW = {
    "minecraft:iron_ingot": "iron", "minecraft:iron_block": "iron_block",
    "minecraft:iron_nugget": "iron_nugget", "#minecraft:planks": "planks",
    "scguns:anthralite_ingot": "anthralite", "scguns:anthralite_block": "anthralite_block",
    "scguns:anthralite_nugget": "anthralite_nugget",
    "scguns:diamond_steel_ingot": "diamond_steel", "scguns:diamond_steel_block": "diamond_steel_block",
    "scguns:treated_brass_ingot": "treated_brass", "scguns:treated_brass_block": "treated_brass_block",
    "scguns:treated_iron_ingot": "treated_iron",
    "minecraft:flint": "flint", "minecraft:cobblestone": "cobblestone",
    "minecraft:stone": "stone", "minecraft:smooth_stone": "stone",
    "minecraft:copper_ingot": "copper", "minecraft:crimson_planks": "planks",
    "minecraft:gold_ingot": "gold", "#minecraft:wool": "wool",
    "minecraft:shulker_shell": "shulker_shell", "minecraft:sculk_sensor": "sculk_sensor",
    "minecraft:echo_shard": "echo_shard", "minecraft:redstone": "redstone",
    "minecraft:amethyst_shard": "amethyst_shard",
}

TIER_NAMES = {
    "antique": "蓝图 - 古典", "copper": "蓝图 - 锈岭", "iron": "蓝图 - 联邦",
    "frontier": "蓝图 - 边疆", "treated_brass": "蓝图 - COG", "diamond_steel": "蓝图 - 阿斯加里安（钻石钢）",
    "wrecker": "蓝图 - 扫荡", "ocean": "蓝图 - 巨鲸", "deep_dark": "蓝图 - 深暗",
    "piglin": "蓝图 - 猪灵", "end": "蓝图 - 末地", "exo_suit": "蓝图 - 外骨骼",
    "scorched": "蓝图 - 赤焦（毕业档）",
}

COLUMNS = ("iron", "anthralite", "diamond_steel", "treated_brass", "treated_iron", "copper", "planks", "stone")
LABELS = {"iron": "铁", "anthralite": "晶碳", "diamond_steel": "钻石钢", "treated_brass": "黄铜",
          "treated_iron": "硬化铁", "copper": "铜", "planks": "木板", "stone": "石料"}


def slot_target(value):
    if isinstance(value, list):
        return slot_target(value[0]) if value else "?"
    if not isinstance(value, dict):
        return "?"
    return value.get("item") or ("#" + value["tag"] if "tag" in value else "?")


def bill(recipe) -> tuple[Counter, Counter]:
    counted: Counter = Counter()
    unknown: Counter = Counter()
    for slot, value in recipe.get("ingredients", {}).items():
        if slot == "blueprint":
            continue
        target = slot_target(value)
        if target in PART_COST:
            for key, amount in PART_COST[target].items():
                counted[key] += amount
        elif target in RAW:
            counted[RAW[target]] += 1
        else:
            unknown[target] += 1
    # blocks and nuggets folded into their base unit
    if counted["iron_block"]:
        counted["iron"] += counted.pop("iron_block") * 9
    if counted["iron_nugget"]:
        counted["iron"] += counted.pop("iron_nugget") * NUGGET
    if counted["anthralite_block"]:
        counted["anthralite"] += counted.pop("anthralite_block") * 9
    if counted["anthralite_nugget"]:
        counted["anthralite"] += counted.pop("anthralite_nugget") * NUGGET
    if counted["diamond_steel_block"]:
        counted["diamond_steel"] += counted.pop("diamond_steel_block") * 9
    if counted["treated_brass_block"]:
        counted["treated_brass"] += counted.pop("treated_brass_block") * 9
    return counted, unknown


def load_guns():
    rows = []
    for name in sorted(os.listdir(GUNS)):
        if not name.endswith(".json"):
            continue
        recipe = json.load(open(os.path.join(GUNS, name), encoding="utf-8"))
        if recipe.get("type") != "scguns:gun_bench":
            continue
        gun = recipe["result"]["id"]
        tier = (recipe.get("ingredients", {}).get("blueprint") or {}).get("item", "").replace(
            "scguns:", "").replace("_blueprint", "")
        counted, unknown = bill(recipe)
        rows.append({"gun": gun.replace("scguns:", ""), "tier": tier, "cost": counted, "other": unknown})
    rows.sort(key=lambda r: (r["tier"], r["gun"]))
    return rows


def line(row) -> str:
    cost = row["cost"]
    cells = []
    for key in COLUMNS:
        value = cost.get(key, 0)
        if value:
            cells.append("%s %.1f" % (LABELS[key], value))
    for item, amount in row["other"].items():
        cells.append("%s×%d" % (item.replace("scguns:", "").replace("minecraft:", ""), amount))
    return " | ".join(cells) if cells else "—"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--tier")
    parser.add_argument("--markdown", action="store_true")
    args = parser.parse_args()

    rows = load_guns()
    if args.tier:
        rows = [r for r in rows if r["tier"] == args.tier]

    tiers: dict[str, list] = {}
    for row in rows:
        tiers.setdefault(row["tier"], []).append(row)

    for tier, members in tiers.items():
        print("\n### %s（%s）" % (TIER_NAMES.get(tier, tier), tier))
        if args.markdown:
            print("\n| 枪 | " + " | ".join(LABELS[k] for k in COLUMNS) + " | 其它 |")
            print("|---|---|---|---|---|---|---|---|---|")
            for row in members:
                values = ["%.1f" % row["cost"].get(k, 0) if row["cost"].get(k, 0) else "" for k in COLUMNS]
                other = ", ".join("%s×%d" % (i.replace("scguns:", "").replace("minecraft:", ""), n)
                                  for i, n in row["other"].items())
                print("| %s | %s | %s |" % (row["gun"], " | ".join(values), other))
        else:
            for row in members:
                print("  %-26s %s" % (row["gun"], line(row)))

    print("\n### 每档中位数（铁 / 晶碳 / 钻石钢）")
    for tier, members in sorted(tiers.items(), key=lambda kv: statistics.median(
            m["cost"].get("iron", 0) + m["cost"].get("anthralite", 0) for m in kv[1])):
        iron = statistics.median(m["cost"].get("iron", 0) for m in members)
        anth = statistics.median(m["cost"].get("anthralite", 0) for m in members)
        dst = statistics.median(m["cost"].get("diamond_steel", 0) for m in members)
        print("  %-34s n=%2d  铁 %5.1f  晶碳 %5.1f  钻石钢 %4.1f" % (TIER_NAMES.get(tier, tier),
                                                                   len(members), iron, anth, dst))
    return 0


if __name__ == "__main__":
    sys.exit(main())
