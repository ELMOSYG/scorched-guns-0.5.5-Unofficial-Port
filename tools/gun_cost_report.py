"""Bill of materials for Scorched Guns guns, expanded to raw items.

Mechanics this script encodes (read from the mod's own code and data):
  * scguns:gun_bench recipes name 10 module slots plus a blueprint slot. GunBenchMenu.consumeIngredients
    iterates only the module slots, so THE BLUEPRINT IS NOT CONSUMED - it is a one-time unlock and is
    excluded from the per-gun bill.
  * Parts are ordinary crafting recipes (firing_unit, gun_barrel, gun_grip, gun_magazine) except
    gun_parts / heavy_gun_parts, which come from scguns:mechanical_pressing (1 iron block -> 2).
  * Molds (`mold` key) are not consumed by the press, so they are excluded too.

Usage:
    python tools/gun_cost_report.py --bom [--detail NAME] [--ammo] [--include-scorched]
"""
from __future__ import annotations

import argparse
import json
import os
import statistics
import sys
from collections import Counter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA = os.path.join(ROOT, "src", "main", "resources", "data")
NUGGETS_PER_INGOT = 9


def load(path):
    with open(path, encoding="utf-8") as fh:
        return json.load(fh)


class Recipes:
    def __init__(self):
        self.by_item: dict[str, list[dict]] = {}
        for base, _, files in os.walk(DATA):
            for name in files:
                if not name.endswith(".json"):
                    continue
                path = os.path.join(base, name)
                rel = os.path.relpath(path, DATA).replace("\\", "/")
                if not rel.startswith("scguns/recipe/"):
                    continue
                tail = rel.split("scguns/recipe/", 1)[1]
                # third-party machine variants are alternatives we do not assume; the mod's own
                # press recipes stay, because for gun_parts they are the only way to make the item
                if tail.startswith(("create/", "immersiveengineering/", "mekanism/")):
                    continue
                try:
                    recipe = load(path)
                except Exception:
                    continue
                out = recipe.get("result") or {}
                item = out.get("id") or out.get("item")
                if item:
                    self.by_item.setdefault(item, []).append({"recipe": recipe, "rel": rel})

    def pick(self, item):
        entries = self.by_item.get(item) or []
        if not entries:
            return None
        # smelting first: an ingot must be traced back to its ore, not to a 9-ingot block recipe
        for kind in ("minecraft:smelting", "minecraft:blasting", "scguns:mechanical_pressing",
                     "minecraft:crafting_shaped", "minecraft:crafting_shapeless",
                     "minecraft:stonecutting"):
            for entry in entries:
                if entry["recipe"].get("type") == kind:
                    return entry["recipe"]
        return entries[0]["recipe"]


def ingredient_entries(recipe) -> list:
    kind = recipe.get("type", "")
    if kind == "minecraft:crafting_shaped":
        key, pattern = recipe.get("key", {}), recipe.get("pattern", [])
        return [key[s] for row in pattern for s in row if s in key]
    if kind == "scguns:gun_bench":
        return list(recipe.get("ingredients", {}).values())
    source = recipe.get("ingredients") or []
    return source if isinstance(source, list) else [source]


def piece(value):
    if isinstance(value, list):
        return piece(value[0]) if value else (0, "?")
    if not isinstance(value, dict):
        return (0, "?")
    if "item" in value:
        return (value.get("count", 1), value["item"])
    if "tag" in value:
        return (value.get("count", 1), "#" + value["tag"])
    return (0, "?")


def expand(item, count, recipes: Recipes, out: Counter, depth=0):
    # nine nuggets make an ingot: fold them so the bill reads in ingots
    if item in ("minecraft:iron_nugget", "scguns:anthralite_nugget"):
        out["minecraft:iron_ingot" if item == "minecraft:iron_nugget" else "scguns:anthralite_ingot"] += \
            count / NUGGETS_PER_INGOT
        return
    if depth > 6:
        out[item] += count
        return
    recipe = recipes.pick(item)
    crafted = 1
    if recipe is not None:
        crafted = max(1, (recipe.get("result") or {}).get("count", 1))
    if recipe is None:
        out[item] += count
        return
    for entry in ingredient_entries(recipe):
        sub_count, sub_item = piece(entry)
        expand(sub_item, sub_count * count / crafted, recipes, out, depth + 1)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--bom", action="store_true")
    parser.add_argument("--detail")
    parser.add_argument("--ammo", action="store_true")
    parser.add_argument("--include-scorched", action="store_true")
    args = parser.parse_args()

    recipes = Recipes()
    guns = []
    for item, entries in recipes.by_item.items():
        bench = next((e["recipe"] for e in entries if e["recipe"].get("type") == "scguns:gun_bench"), None)
        if not bench:
            continue
        slots = bench.get("ingredients", {})
        blueprint = (slots.get("blueprint") or {}).get("item", "?")
        if not args.include_scorched and blueprint == "scguns:scorched_blueprint":
            continue
        bom: Counter = Counter()
        for slot, value in slots.items():
            if slot == "blueprint":
                continue
            count, target = piece(value)
            expand(target, count, recipes, bom)
        guns.append((item, blueprint, bom, slots))

    if args.detail:
        for item, blueprint, bom, slots in [g for g in guns if args.detail.lower() in g[0].lower()][:3]:
            print("\n== %s  (blueprint %s, not consumed)" % (item, blueprint))
            for slot, value in slots.items():
                count, target = piece(value)
                print("   %-18s %-34s x%d" % (slot, target, count))
            for key, value in sorted(bom.items(), key=lambda kv: -kv[1]):
                print("   -> %-46s %6.2f" % (key, value))
        return 0

    if args.ammo:
        for item in ("minecraft:gunpowder", "scguns:gunpowder_dust", "scguns:standard_bullet",
                     "scguns:small_copper_casing", "scguns:medium_copper_casing",
                     "scguns:standard_copper_round", "scguns:shotgun_shell", "scguns:buckshot"):
            recipe = recipes.pick(item)
            if recipe is None:
                continue
            out = (recipe.get("result") or {}).get("count", 1)
            print("%-34s -> %-34s x%d" % (item, ", ".join("%s x%d" % piece(piece_) for piece_ in ingredient_entries(recipe)), out))
        return 0

    print("guns (scorched endgame tier excluded): %d\n" % len(guns))
    print("%-30s %8s %8s %6s  %s" % ("gun", "iron", "anthralite", "planks", "other"))
    for item, blueprint, bom, _ in sorted(guns, key=lambda g: sum(g[2].values())):
        iron = bom.get("minecraft:iron_ingot", 0)
        anth = bom.get("scguns:anthralite_ingot", 0)
        planks = sum(v for k, v in bom.items() if "planks" in k or k == "#minecraft:planks")
        other = ", ".join("%s x%.2g" % (k.replace("minecraft:", "").replace("scguns:", ""), v)
                          for k, v in bom.items()
                          if k not in ("minecraft:iron_ingot", "scguns:anthralite_ingot")
                          and "planks" not in k)
        print("%-30s %8.2f %8.2f %6.1f  %s" % (item.replace("scguns:", ""), iron, anth, planks, other[:70]))

    tiers = {}
    for item, blueprint, bom, _ in guns:
        tiers.setdefault(blueprint, []).append(sum(bom.values()))
    print("\nmedian bill per blueprint tier (items of raw material):")
    for tier, totals in sorted(tiers.items(), key=lambda kv: statistics.median(kv[1])):
        print("  %-30s n=%2d median %6.1f" % (tier.replace("scguns:", ""), len(totals), statistics.median(totals)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
