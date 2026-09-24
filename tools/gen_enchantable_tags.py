"""Generate the vanilla enchantable item tags for the mod's armour.

In 1.20.1 an enchantment's EnchantmentCategory decided what it could go on, and ArmorItem subclasses
matched ARMOR automatically - so modded armour was enchantable and loot tables that roll
enchant_with_levels gave enchanted gear. 1.21 moved that decision into vanilla's item tags, and the
port carried none, so every enchantment found nothing compatible: the rolled armour came out bare
(HANDOFF section 56).

The tags are written with replace=false so they merge with vanilla's own rather than replacing them.

Usage: python tools/gen_enchantable_tags.py [--write]
"""
import pathlib
import re
import sys

ROOT = pathlib.Path(r"E:\mod\scgun-0.5.5-1.21.1-neoforge")
ITEMS = ROOT / "src/main/java/top/ribs/scguns/init/ModItems.java"
OUT = ROOT / "src/main/resources/data/minecraft/tags/item/enchantable"

SLOTS = {
    "helmet": "head_armor", "helm": "head_armor", "hat": "head_armor",
    "respirator": "head_armor", "mask": "head_armor",
    "chestplate": "chest_armor", "coat": "chest_armor", "ridgetop": "chest_armor",
    "leggings": "leg_armor", "pants": "leg_armor",
    "boots": "foot_armor",
}


def armour_items() -> list[str]:
    """Item ids whose registration constructs an armour item."""
    text = ITEMS.read_text(encoding="utf-8")
    names = []
    for match in re.finditer(r'register\(\s*"([a-z0-9_]+)"\s*,\s*\(\)\s*->\s*new\s+(\w+)\s*\(', text):
        name, cls = match.group(1), match.group(2)
        if "ArmorItem" in cls or cls == "ExoSuitItem" or cls.endswith("ArmorItem"):
            names.append(name)
    return sorted(set(names))


def durable_items() -> list[str]:
    """Item ids whose registration mentions a durability - guns, attachments, tools, molds.

    1.20.1's EnchantmentCategory.BREAKABLE matched simply on Item#canBeDepleted, so every durable mod
    item could take Unbreaking and Mending. 1.21 asks whether the item is in this tag instead, which
    is why an attachment could not carry the Mending that would let it be repaired (HANDOFF 57).
    """
    text = ITEMS.read_text(encoding="utf-8")
    blocks = re.split(r'(?=register\(\s*")', text)
    names = []
    for block in blocks:
        match = re.match(r'register\(\s*"([a-z0-9_]+)"', block)
        if not match:
            continue
        # the constructor expression up to the next registration
        head = block.split("register(", 2)[-1][:400]
        if "durability(" in head or ".durability" in head:
            names.append(match.group(1))
    return sorted(set(names))


def write_tag(path: pathlib.Path, values: list[str]) -> None:
    import json
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps({"replace": False, "values": values}, indent=2) + "\n", encoding="utf-8")


def main():
    write = "--write" in sys.argv
    items = armour_items()
    durable = durable_items()
    by_slot: dict[str, list[str]] = {slot: [] for slot in sorted(set(SLOTS.values()))}
    unslotted = []
    for name in items:
        suffix = name.rsplit("_", 1)[-1]
        slot = SLOTS.get(suffix)
        if slot:
            by_slot[slot].append("scguns:" + name)
        else:
            unslotted.append(name)

    plan = {
        "armor": ["scguns:" + i for i in items],
        **{slot: sorted(full) for slot, full in by_slot.items() if full},
        # Unbreaking/Mending, Curse of Binding and Curse of Vanishing are keyed on these, and vanilla
        # armour sits in all of them, so modded armour did too back when categories decided. The
        # durability tag is broader on purpose: 1.20.1's BREAKABLE matched every item with durability,
        # which is what lets an attachment carry Mending and be repaired (HANDOFF section 57).
        "durability": sorted("scguns:" + i for i in set(items) | set(durable)),
        "equippable": ["scguns:" + i for i in items],
        "vanishing": sorted("scguns:" + i for i in set(items) | set(durable)),
    }

    print("%d armour item(s), %d durable item(s) found" % (len(items), len(durable)))
    for tag, values in sorted(plan.items()):
        print("  %-14s %3d" % (tag, len(values)))
    if unslotted:
        print("  (no slot matched, armour tag only): %s" % unslotted)

    if write:
        for tag, values in plan.items():
            write_tag(OUT / (tag + ".json"), values)
        print("wrote %d tag(s) to %s" % (len(plan), OUT))
    else:
        print("dry run - pass --write to write them")
    return 0


if __name__ == "__main__":
    sys.exit(main())
