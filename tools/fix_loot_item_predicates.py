"""Rewrite the 0.5.5-era item predicates in our loot tables into the 1.21 shape.

In 1.20 the "is the tool enchanted with silk touch" test was written as

    "predicate": { "enchantments": [ ... ] }

1.20.5 moved item enchantments under the component-based item predicate, so 1.21 wants

    "predicate": { "predicates": { "minecraft:enchantments": [ ... ] } }

Mojang's codecs ignore unknown fields, so the old shape is not an error - it parses into an empty
predicate, and an empty predicate matches everything. Every silk-touch branch therefore always fired:
the supply crate dropped itself instead of ammo, and 28 ore/glass tables handed out their block with
no silk touch (HANDOFF section 55).

Usage: python tools/fix_loot_item_predicates.py [--write]
"""
import json
import pathlib
import sys

ROOT = pathlib.Path(r"E:\mod\scgun-0.5.5-1.21.1-neoforge")
TABLES = ROOT / "src/main/resources/data/scguns/loot_table"
ENCHANT_KEY = "minecraft:enchantments"


def rewrite(node):
    """Move a legacy `enchantments` predicate under `predicates`, in place. Returns True if changed."""
    changed = False
    if isinstance(node, dict):
        predicate = node.get("predicate")
        if isinstance(predicate, dict) and "enchantments" in predicate and "predicates" not in predicate:
            enchantments = predicate.pop("enchantments")
            predicate["predicates"] = {ENCHANT_KEY: enchantments}
            changed = True
        for value in node.values():
            changed |= rewrite(value)
    elif isinstance(node, list):
        for value in node:
            changed |= rewrite(value)
    return changed


def main():
    write = "--write" in sys.argv
    touched = []
    for path in sorted(TABLES.rglob("*.json")):
        raw = path.read_text(encoding="utf-8")
        if '"predicate"' not in raw or '"enchantments"' not in raw:
            continue
        data = json.loads(raw)
        if rewrite(data):
            touched.append(path)
            if write:
                path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

    print("%d loot table(s) with a legacy item predicate%s" % (len(touched), "" if write else " (dry run)"))
    for path in touched:
        print("   ", path.relative_to(ROOT))
    return 0


if __name__ == "__main__":
    sys.exit(main())
