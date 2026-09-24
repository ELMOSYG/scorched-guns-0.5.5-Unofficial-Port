"""Convert the 0.5.5 code-defined enchantments into 1.21 data driven ones.

1.21 made Enchantment a datapack registry entry: the old `new Enchantment(rarity,
category, slots)` constructors and `getMaxLevel()/getMinCost()` overrides are gone.
Each enchantment class in 0.5.5 is a thin constructor, so the definition moves to
`data/scguns/enchantment/<name>.json` and ModEnchantments keeps only ResourceKeys.

usage: python tools/gen_enchantments.py [--write]
"""
from __future__ import annotations

import json
import os
import re
import sys

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
# 1.21 made the definitions data driven, so our repo only still has CorrodedEnchantment (the one
# that needs behaviour). The originals therefore come from the 0.5.5 reference tree; the local
# directory is only a fallback for when that tree is not around.
REF_ENCH_DIR = r"E:\mod\SG2-1.21\.sg055_deobf\top\ribs\scguns\enchantment"
ENCH_DIR = REF_ENCH_DIR if os.path.isdir(REF_ENCH_DIR) else os.path.join(
    ROOT, "src", "main", "java", "top", "ribs", "scguns", "enchantment")
OUT_DIR = os.path.join(ROOT, "src", "main", "resources", "data", "scguns", "enchantment")
TAG_DIR = os.path.join(ROOT, "src", "main", "resources", "data", "scguns", "tags", "item", "enchantable")
ITEM_TAG_DIR = os.path.join(ROOT, "src", "main", "resources", "data", "scguns", "tags", "item")
ENCH_TAG_DIR = os.path.join(ROOT, "src", "main", "resources", "data", "scguns", "tags",
                            "enchantment", "exclusive_set")
GUNS_DIR = os.path.join(ROOT, "src", "main", "resources", "data", "scguns", "guns")

WEIGHT = {"COMMON": 10, "UNCOMMON": 5, "RARE": 2, "VERY_RARE": 1}
SLOT_MAP = {
    "MAINHAND": "mainhand", "OFFHAND": "offhand", "HAND": "hand",
    "HEAD": "head", "CHEST": "chest", "LEGS": "legs", "FEET": "feet",
    "BODY": "body",
}
# 0.5.5's EnchantmentCategory -> the 1.21 item tag that means the same thing.
#   GUN / WATER_PROOF_COMPATIBLE            : every GunItem
#   BAYONET                                 : every BayonetItem
#   WEAPON (vanilla, used by corroded)      : swords and axes - sharp_weapon, not weapon, because
#                                             weapon also covers the mace, which 0.5.5 never had
#   the three *_COMPATIBLE ones are "every gun except <tag>" and are materialised by
#   DIFFERENCE_TAGS below, because data cannot say "except"
CATEGORY_TAG = {
    "GUN": "#scguns:enchantable/guns",
    "SEMI_AUTO_GUN": "#scguns:enchantable/guns",
    "WATER_PROOF_COMPATIBLE": "#scguns:enchantable/guns",
    "TRIGGER_FINGER_COMPATIBLE": "#scguns:enchantable/trigger_finger",
    "SHELL_CATCHER_COMPATIBLE": "#scguns:enchantable/shell_catcher",
    "COLLATERAL_COMPATIBLE": "#scguns:enchantable/collateral",
    "BAYONET": "#scguns:enchantable/bayonets",
    "WEAPON": "#minecraft:enchantable/sharp_weapon",
}
# generated tag -> (base tag, tag whose members are removed)
DIFFERENCE_TAGS = {
    "enchantable/trigger_finger": ("enchantable/guns", "single_shot"),
    "enchantable/shell_catcher": ("enchantable/guns", "does_not_eject_casings"),
    "enchantable/collateral": ("enchantable/guns", "non_collateral"),
}


def parse(path: str) -> dict:
    text = open(path, encoding="utf-8", errors="replace").read()
    name = os.path.basename(path)[:-len("Enchantment.java")] if path.endswith("Enchantment.java") else None
    info = {"class": os.path.basename(path)[:-5], "file": path}
    m = re.search(r"super\(Rarity\.(\w+)", text)
    info["rarity"] = m.group(1) if m else "COMMON"
    m = re.search(r"EnchantmentTypes\.(\w+)", text) or re.search(r"EnchantmentCategory\.(\w+)", text)
    info["category"] = m.group(1) if m else "GUN"
    info["slots"] = re.findall(r"EquipmentSlot\.(\w+)", text)
    m = re.search(r"getMaxLevel\(\)\s*\{\s*return\s+(\d+);", text)
    info["max_level"] = int(m.group(1)) if m else 1
    m = re.search(r"getMinCost\(int level\)\s*\{\s*return\s+([^;]+);", text)
    info["min_cost"] = m.group(1).strip() if m else None
    m = re.search(r"getMaxCost\(int level\)\s*\{\s*return\s+([^;]+);", text)
    info["max_cost"] = m.group(1).strip() if m else None
    info["exclusive_damage"] = "DamageEnchantment" in text or "!(other instanceof" in text
    # 0.5.5's GunEnchantment.checkCompatibility: enchantments of the same Type are mutually
    # exclusive. CorrodedEnchantment is a plain Enchantment (no Type) and carries its own vanilla
    # damage conflict instead.
    m = re.search(r"GunEnchantment\.Type\.(\w+)", text)
    info["type"] = m.group(1) if m else None
    return info


def tag_members(name: str, _seen=None) -> list[str]:
    """Item ids listed by a (possibly nested) tag in our own datapack."""
    _seen = _seen or set()
    if name in _seen:
        return []
    _seen.add(name)
    path = os.path.join(ITEM_TAG_DIR, name + ".json")
    if not os.path.exists(path):
        print("  !! tag missing: %s" % name)
        return []
    values = json.load(open(path, encoding="utf-8")).get("values", [])
    out = []
    for value in values:
        if isinstance(value, dict):
            value = value.get("id", "")
        if value.startswith("#"):
            out.extend(tag_members(value[1:].split(":", 1)[-1], _seen))
        elif value:
            out.append(value)
    return out


def cost(expr: str | None, default_base: int, default_per: int) -> dict:
    """Translate a simple linear `a + b * (level - 1)` cost formula."""
    if not expr:
        return {"base": default_base, "per_level_above_first": default_per}
    m = re.fullmatch(r"(\d+)\s*\+\s*(\d+)\s*\*\s*\(?\s*level\s*-\s*1\s*\)?", expr)
    if m:
        return {"base": int(m.group(1)), "per_level_above_first": int(m.group(2))}
    m = re.fullmatch(r"(\d+)\s*\*\s*level", expr)
    if m:
        step = int(m.group(1))
        return {"base": step, "per_level_above_first": step}
    return {"base": default_base, "per_level_above_first": default_per}


def registered_names() -> tuple[dict[str, str], dict[str, str]]:
    """ModEnchantments: (class simple name -> id, CONSTANT_NAME -> id).

    The port registers ResourceKeys now - the definitions are data - so the old
    `register("x", SomeEnchantment::new)` pattern is gone and `ResourceKey<Enchantment> SOME_NAME =
    key("some_id")` is what to read. Reading only the old pattern silently invents ids: it produced
    water_proof.json next to the real waterproof.json (HANDOFF section 50).
    """
    path = os.path.join(ROOT, "src", "main", "java", "top", "ribs", "scguns", "init", "ModEnchantments.java")
    text = open(path, encoding="utf-8", errors="replace").read()
    by_class = {cls: name for name, cls in re.findall(r'register\("(\w+)",\s*(\w+)::new\)', text)}
    by_constant = dict(re.findall(r'ResourceKey<Enchantment>\s+(\w+)\s*=\s*key\("(\w+)"\)', text))
    return by_class, by_constant


def screaming_snake(name: str) -> str:
    return re.sub(r"(?<!^)(?=[A-Z])", "_", name).upper()


def main() -> None:
    write = "--write" in sys.argv
    names, by_constant = registered_names()
    infos = []
    for fn in sorted(os.listdir(ENCH_DIR)):
        if not fn.endswith("Enchantment.java") or fn in ("GunEnchantment.java",):
            continue
        parsed = parse(os.path.join(ENCH_DIR, fn))
        simple = parsed["class"]
        stem = simple[:-len("Enchantment")] if simple.endswith("Enchantment") else simple
        fallback = re.sub(r"(?<!^)(?=[A-Z])", "_", stem).lower()
        parsed["id"] = (names.get(simple)
                        or by_constant.get(screaming_snake(stem))
                        or fallback)
        if simple not in names and screaming_snake(stem) not in by_constant:
            print(f"  !! no registration found for {simple}, using {parsed['id']}")
        infos.append(parsed)

    os.makedirs(OUT_DIR, exist_ok=True)
    os.makedirs(TAG_DIR, exist_ok=True)
    for info in infos:
        tag = CATEGORY_TAG.get(info["category"], "#scguns:enchantable/guns")
        slots = [SLOT_MAP.get(s, "mainhand") for s in info["slots"]] or ["mainhand"]
        min_cost = cost(info["min_cost"], 1, 10)
        m = re.fullmatch(r"(?:this|super)\.getMinCost\(level\)\s*\+\s*(\d+)", info["max_cost"] or "")
        if m:
            max_cost = {"base": min_cost["base"] + int(m.group(1)),
                        "per_level_above_first": min_cost["per_level_above_first"]}
        else:
            max_cost = cost(info["max_cost"], 21, 10)
        data = {
            "description": {"translate": f"enchantment.scguns.{info['id']}"},
            "supported_items": tag,
            "primary_items": tag,
            "weight": WEIGHT.get(info["rarity"], 5),
            "max_level": info["max_level"],
            "min_cost": min_cost,
            "max_cost": max_cost,
            "anvil_cost": 2,
            "slots": slots,
            "effects": {},
        }
        if info["exclusive_damage"]:
            data["exclusive_set"] = "#minecraft:exclusive_set/damage"
        elif info["type"]:
            # Every member of the group declares the same tag, itself included - that is how
            # vanilla's minecraft:exclusive_set/damage works, and what makes the exclusion mutual.
            data["exclusive_set"] = "#scguns:exclusive_set/%s" % info["type"].lower()
        info["json"] = data

    if write:
        for info in infos:
            with open(os.path.join(OUT_DIR, info["id"] + ".json"), "w", encoding="utf-8") as fh:
                json.dump(info["json"], fh, indent=2)
                fh.write("\n")
        guns = sorted(f[:-5] for f in os.listdir(GUNS_DIR) if f.endswith(".json"))
        with open(os.path.join(TAG_DIR, "guns.json"), "w", encoding="utf-8") as fh:
            json.dump({"replace": False, "values": [f"scguns:{g}" for g in guns]}, fh, indent=2)
            fh.write("\n")

        # "every gun except <tag>" - data has no negation, so the difference is written out.
        os.makedirs(TAG_DIR, exist_ok=True)
        for out_name, (base, excluded) in DIFFERENCE_TAGS.items():
            members = [m for m in tag_members(base) if m not in set(tag_members(excluded))]
            target = os.path.join(ITEM_TAG_DIR, out_name + ".json")
            with open(target, "w", encoding="utf-8") as fh:
                json.dump({"replace": False, "values": sorted(members)}, fh, indent=2)
                fh.write("\n")
            print("  %-34s %3d members  (%s minus %s)"
                  % (out_name, len(members), base, excluded))

        os.makedirs(ENCH_TAG_DIR, exist_ok=True)
        by_type: dict[str, list[str]] = {}
        for info in infos:
            if info["type"]:
                by_type.setdefault(info["type"], []).append(info["id"])
        for type_name, ids in sorted(by_type.items()):
            target = os.path.join(ENCH_TAG_DIR, type_name.lower() + ".json")
            with open(target, "w", encoding="utf-8") as fh:
                json.dump({"replace": False, "values": sorted("scguns:" + i for i in ids)}, fh, indent=2)
                fh.write("\n")
            print("  exclusive_set/%-18s %s" % (type_name.lower(), sorted(ids)))
        bayonets = ["scguns:diamond_bayonet", "scguns:iron_bayonet", "scguns:netherite_bayonet"]
        with open(os.path.join(TAG_DIR, "bayonets.json"), "w", encoding="utf-8") as fh:
            json.dump({"replace": False, "values": bayonets}, fh, indent=2)
            fh.write("\n")
        print(f"wrote {len(infos)} enchantment definitions + {len(guns)} gun tag entries")
    else:
        for info in infos:
            print(f"{info['id']:16s} rarity={info['rarity']:10s} cat={info['category']:26s} "
                  f"max={info['max_level']} slots={info['slots']} min={info['min_cost']} "
                  f"max={info['max_cost']} excl={info['exclusive_damage']}")


if __name__ == "__main__":
    main()
