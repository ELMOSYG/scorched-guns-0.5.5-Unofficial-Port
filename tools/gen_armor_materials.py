"""Convert ModArmorMaterials (a Forge ArmorMaterial enum) to 1.21.

1.21 turned ArmorMaterial into a datapack registry entry, so the enum data moves
to `data/scguns/armor_material/<name>.json` and the class keeps
DeferredHolder<ArmorMaterial, ArmorMaterial> constants - a DeferredHolder is a
Holder, which is exactly what ArmorItem wants.

usage: python tools/gen_armor_materials.py [--write]
"""
from __future__ import annotations

import json
import os
import re
import sys

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java", "top", "ribs", "scguns", "init", "ModArmorMaterials.java")
OUT_DIR = os.path.join(ROOT, "src", "main", "resources", "data", "scguns", "armor_material")

ENTRY = re.compile(
    r"(\w+)\(\s*\"(\w+)\"\s*,\s*(\d+)\s*,\s*new int\[\]\s*\{\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*\}\s*,"
    r"\s*(\d+)\s*,\s*(SoundEvents\.\w+)\s*,\s*([\d.]+)F\s*,\s*([\d.]+)F\s*,\s*(.*?)\)\s*,",
    re.S,
)

SOUND_ID = {
    "ARMOR_EQUIP_LEATHER": "minecraft:item.armor.equip_leather",
    "ARMOR_EQUIP_CHAIN": "minecraft:item.armor.equip_chain",
    "ARMOR_EQUIP_IRON": "minecraft:item.armor.equip_iron",
    "ARMOR_EQUIP_GOLD": "minecraft:item.armor.equip_gold",
    "ARMOR_EQUIP_DIAMOND": "minecraft:item.armor.equip_diamond",
    "ARMOR_EQUIP_NETHERITE": "minecraft:item.armor.equip_netherite",
    "ARMOR_EQUIP_TURTLE": "minecraft:item.armor.equip_turtle",
    "ARMOR_EQUIP_GENERIC": "minecraft:item.armor.equip_generic",
}


def repair_of(tail: str) -> dict:
    # ModItems must be tested first: "ModItems.X" also contains "Items.X"
    mod_item = re.search(r"ModItems\.(\w+)\.get\(\)", tail)
    if mod_item:
        return {"item": "scguns:" + mod_item.group(1).lower()}
    item = re.search(r"(?<!Mod)Items\.(\w+)", tail)
    if item:
        return {"item": "minecraft:" + item.group(1).lower()}
    tag = re.search(r"ModTags\.\w+\.(\w+)", tail)
    if tag:
        return {"tag": "scguns:" + tag.group(1).lower()}
    tag = re.search(r"ItemTags\.(\w+)", tail)
    if tag:
        return {"tag": "minecraft:" + tag.group(1).lower()}
    return {"item": "minecraft:leather"}


def main() -> None:
    write = "--write" in sys.argv
    text = open(SRC, encoding="utf-8", errors="replace").read()
    entries = []
    for m in ENTRY.finditer(text):
        const, name, durability, boots, leggings, chest, helmet, ench, sound, tough, kb, tail = m.groups()
        entries.append({
            "const": const,
            "name": name,
            "durability": int(durability),
            "defense": {"boots": int(boots), "leggings": int(leggings), "chestplate": int(chest),
                        "helmet": int(helmet), "body": int(chest)},
            "enchantment_value": int(ench),
            "equip_sound": SOUND_ID.get(sound.split(".")[-1], "minecraft:item.armor.equip_generic"),
            "toughness": float(tough),
            "knockback_resistance": float(kb),
            "repair_ingredient": repair_of(tail),
        })
    print(f"parsed {len(entries)} armor materials")
    for e in entries:
        print(f"  {e['const']:14s} {e['name']:16s} dur={e['durability']:4d} def={e['defense']} "
              f"ench={e['enchantment_value']:3d} tough={e['toughness']} kb={e['knockback_resistance']} "
              f"repair={e['repair_ingredient']}")
    if not write:
        return
    if not entries:
        # This script parses the Forge enum shape out of ModArmorMaterials, but it
        # also rewrites that file into the 1.21 `material("name")` form. Running it
        # a second time therefore parses nothing, and writing would replace the
        # class with an empty body. Refuse instead of destroying it.
        raise SystemExit("refusing to write: no armor-material entries parsed; "
                         "ModArmorMaterials is already in its generated 1.21 form. "
                         "Add new materials as JSON + a material(\"name\") constant.")
    os.makedirs(OUT_DIR, exist_ok=True)
    for e in entries:
        data = {
            "layers": [{"texture": "scguns:" + e["name"]}],
            "durability": e["durability"],
            "defense": e["defense"],
            "enchantment_value": e["enchantment_value"],
            "equip_sound": e["equip_sound"],
            "toughness": e["toughness"],
            "knockback_resistance": e["knockback_resistance"],
            "repair_ingredient": e["repair_ingredient"],
        }
        with open(os.path.join(OUT_DIR, e["name"] + ".json"), "w", encoding="utf-8") as fh:
            json.dump(data, fh, indent=2)
            fh.write("\n")
    constants = "\n".join(
        '    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> %s = material("%s");'
        % (e["const"], e["name"]) for e in entries)
    source = '''package top.ribs.scguns.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorMaterial;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Scorched Guns armor materials.
 *
 * <p>1.21 made ArmorMaterial a datapack registry entry (a record with the layer
 * list, defense map, enchantment value, equip sound, toughness, knockback
 * resistance and repair ingredient), so the values live in
 * {@code data/scguns/armor_material/<name>.json}. The constants are deferred
 * holders bound when the datapack loads; a DeferredHolder is a Holder, which is
 * what ArmorItem expects.</p>
 */
public final class ModArmorMaterials {
%s

    private ModArmorMaterials() {
    }

    private static DeferredHolder<ArmorMaterial, ArmorMaterial> material(String name) {
        return DeferredHolder.create(Registries.ARMOR_MATERIAL,
                ResourceLocation.fromNamespaceAndPath("scguns", name));
    }
}
''' % constants
    with open(SRC, "w", encoding="utf-8", newline="\n") as fh:
        fh.write(source)
    print(f"wrote {len(entries)} armor material files and rewrote ModArmorMaterials")


if __name__ == "__main__":
    main()
