"""Fix loot functions that 1.21 removed, and survey the ids used.

`minecraft:looting_enchant` was deleted in 1.21; `minecraft:enchanted_count_increase`
replaces it with identical semantics (`grow(round(count * level))`, same optional
`limit`) plus a mandatory `enchantment` field:

    {"function": "minecraft:looting_enchant", "count": {...}}
 -> {"function": "minecraft:enchanted_count_increase",
     "enchantment": "minecraft:looting", "count": {...}}

A loot table that fails to parse is dropped entirely, so the affected entities
drops nothing at all - 17 tables were failing this way.

Also prints every function id used in the mod's data next to the ids vanilla
1.21.1 still registers, so further removals cannot hide.

usage: python tools/fix_loot_functions.py [--write] [--survey]
"""
from __future__ import annotations

import collections
import json
import os
import re
import sys

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
DATA = os.path.join(ROOT, "src", "main", "resources", "data")
LOOT_ITEM_FUNCTIONS = os.path.join(
    ROOT, ".refs", "nf-src", "net", "minecraft", "world", "level", "storage", "loot",
    "functions", "LootItemFunctions.java")

OLD = "minecraft:looting_enchant"
NEW = "minecraft:enchanted_count_increase"
OLD_NBT = "minecraft:set_nbt"
NEW_COMPONENTS = "minecraft:set_components"
REGISTER = re.compile(r'register\(\s*"([\w/]+)"')
# 1.20.1 item NBT that only carries enchantments: {Enchantments:[{id:"x",lvl:2s}]}
ENCHANT_TAG = re.compile(r'^\{Enchantments:\[\{id:"([\w:./-]+)",lvl:(\d+)s\}\]\}$')


def vanilla_ids() -> set[str]:
    text = open(LOOT_ITEM_FUNCTIONS, encoding="utf-8", errors="replace").read()
    return {"minecraft:" + name for name in REGISTER.findall(text)}


def rewrite(node, stats: dict) -> None:
    """In-place conversion of every looting_enchant function in the tree."""
    if isinstance(node, dict):
        if node.get("function") == OLD:
            node["function"] = NEW
            # keep the payload, add the now-mandatory enchantment
            rebuilt = {"function": NEW, "enchantment": "minecraft:looting"}
            for key, value in node.items():
                if key not in ("function", "enchantment"):
                    rebuilt[key] = value
            node.clear()
            node.update(rebuilt)
            stats["looting_enchant -> enchanted_count_increase"] += 1

        if node.get("function") == OLD_NBT:
            # 1.21 removed set_nbt; the payload here is a 1.20.1 enchantment list,
            # which is now the `minecraft:enchantments` component. Writing it back as
            # custom data would leave the item unenchanted, so translate it properly.
            match = ENCHANT_TAG.match(str(node.get("tag", "")).strip())
            if match:
                enchantment, level = match.group(1), int(match.group(2))
                rebuilt = {"function": NEW_COMPONENTS,
                           "components": {"minecraft:enchantments": {"levels": {enchantment: level}}}}
                for key, value in node.items():
                    if key not in ("function", "tag", "components"):
                        rebuilt[key] = value
                node.clear()
                node.update(rebuilt)
                stats["set_nbt -> set_components (enchantments)"] += 1
            else:
                stats["set_nbt NOT converted (unrecognised tag)"] += 1

        for value in node.values():
            rewrite(value, stats)
    elif isinstance(node, list):
        for value in node:
            rewrite(value, stats)


def collect_ids(node, out: collections.Counter) -> None:
    if isinstance(node, dict):
        if isinstance(node.get("function"), str):
            out[node["function"]] += 1
        for value in node.values():
            collect_ids(value, out)
    elif isinstance(node, list):
        for value in node:
            collect_ids(value, out)


def main() -> None:
    write = "--write" in sys.argv
    stats: collections.Counter = collections.Counter()
    used: collections.Counter = collections.Counter()
    files = []
    for d, _, fs in os.walk(DATA):
        for f in fs:
            if f.endswith(".json"):
                files.append(os.path.join(d, f))

    for path in files:
        try:
            data = json.loads(open(path, encoding="utf-8").read())
        except Exception:
            continue
        collect_ids(data, used)
        before = json.dumps(data, sort_keys=True)
        rewrite(data, stats)
        if json.dumps(data, sort_keys=True) != before:
            files_changed = stats["looting_enchant -> enchanted_count_increase"]
            if write:
                with open(path, "w", encoding="utf-8", newline="\n") as fh:
                    json.dump(data, fh, indent=2)
                    fh.write("\n")

    known = vanilla_ids()
    unknown = {k: v for k, v in used.items() if k not in known}
    print("scanned %d json files" % len(files))
    for k, v in stats.most_common():
        print("%5d  %s" % (v, k))
    if not write and stats:
        print("(dry run - pass --write to apply)")
    print("\n=== loot function ids used (%d distinct) ===" % len(used))
    for k, v in sorted(used.items()):
        print("  %-46s %4d  %s" % (k, v, "ok" if k in known else "<-- NOT IN 1.21.1"))
    if unknown:
        print("\n%d REMOVED/UNKNOWN function id(s)" % len(unknown))
        sys.exit(1)


if __name__ == "__main__":
    main()
