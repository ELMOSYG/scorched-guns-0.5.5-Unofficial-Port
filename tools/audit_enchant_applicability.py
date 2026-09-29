"""Enchantment applicability 0.5.5 wrote in Java has to exist as data on 1.21.1.

0.5.5's GunItem overrode `canApplyAtEnchantingTable`. 1.21.1 deleted that method: whether an
enchantment can go on an item is now decided entirely by the enchantment's own `supported_items` tag
(read from the vanilla enchantment JSONs), plus the item's `isEnchantable`/`isBookEnchantable`. So
every rule that override expressed has to be re-expressed as a tag, and one of them was not:

  0.5.5:
      return !stack.is(ModTags.Items.MINING_GUN)
          || enchantment != Enchantments.BLOCK_FORTUNE && enchantment != Enchantments.SILK_TOUCH
          ? super.canApplyAtEnchantingTable(stack, enchantment)
          : true;

  i.e. a mining gun always accepts Fortune and Silk Touch. In 1.21.1 both of those declare
      "supported_items": "#minecraft:enchantable/mining_loot"
  which by default is only #axes, #pickaxes, #shovels and #hoes, so without a tag of our own the
  enchanting table silently stops offering them on the mining guns - which is what the player
  reported.

Efficiency is deliberately NOT included: 0.5.5 let the superclass decide for it, and
EnchantmentCategory.DIGGER.canEnchant is `item instanceof DiggerItem`, false for a gun, so 0.5.5
never allowed it either.

usage: python tools/audit_enchant_applicability.py
"""
from __future__ import annotations

import json
import os
import re
import sys

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
DATA = os.path.join(ROOT, "src", "main", "resources", "data")
GUN_ITEM = os.path.join(ROOT, "src", "main", "java", "top", "ribs", "scguns", "item", "GunItem.java")
MINING_TAG = os.path.join(DATA, "scguns", "tags", "item", "mining_gun.json")
LOOT_TAG = os.path.join(DATA, "minecraft", "tags", "item", "enchantable", "mining_loot.json")

# The tag 1.21.1 ships for Fortune and Silk Touch's `supported_items`, read out of the vanilla
# enchantment JSONs in the client jar.
VANILLA_MINING_LOOT = "minecraft:enchantable/mining_loot"


def values(path: str) -> list:
    try:
        return json.loads(open(path, encoding="utf-8").read()).get("values", [])
    except OSError:
        return []


def main() -> int:
    problems: list[str] = []

    if not os.path.isfile(LOOT_TAG):
        problems.append("data/minecraft/tags/item/enchantable/%s.json is missing, so Fortune and Silk "
                        "Touch cannot be applied to anything but vanilla tools"
                        % VANILLA_MINING_LOOT.split("/")[-1])
    else:
        entries = values(LOOT_TAG)
        if "#scguns:mining_gun" not in entries:
            problems.append("the %s tag does not include #scguns:mining_gun, so 0.5.5's "
                            "canApplyAtEnchantingTable rule - mining guns accept Fortune and Silk "
                            "Touch - is gone" % VANILLA_MINING_LOOT)
        raw = open(LOOT_TAG, encoding="utf-8").read()
        if '"replace": true' in raw:
            problems.append("the %s tag replaces the vanilla one, which would take Fortune and Silk "
                            "Touch away from every vanilla tool" % VANILLA_MINING_LOOT)

    if not os.path.isfile(MINING_TAG):
        problems.append("data/scguns/tags/item/mining_gun.json is missing, so the tag the enchanting "
                        "entry above refers to does not exist")
    else:
        if "#scguns:mining_gun" in values(LOOT_TAG) and not values(MINING_TAG):
            problems.append("the mining gun tag is empty, so the entry in %s resolves to nothing"
                            % VANILLA_MINING_LOOT)
        if json.loads(open(MINING_TAG, encoding="utf-8").read()).get("replace") is True:
            problems.append("the mining gun tag replaces its contents instead of merging")

    # The rule has to stay documented where the reader will look for it.
    gun = open(GUN_ITEM, encoding="utf-8", errors="replace").read() if os.path.isfile(GUN_ITEM) else ""
    if "MINING_GUN" not in gun:
        problems.append("GunItem no longer knows about the mining gun tag at all, so isBookEnchantable's "
                        "special case for it is gone too")
    elif not re.search(r"MINING_GUN\s*\)\s*\?\s*true", gun):
        problems.append("GunItem.isBookEnchantable no longer lets a mining gun take any book")

    for problem in problems:
        print("BROKEN %s" % problem)
    if problems:
        print("\n%d problem(s)" % len(problems))
        return 1
    print("0 problem(s): the mining guns carry the enchantments 0.5.5 let them carry")
    return 0


if __name__ == "__main__":
    sys.exit(main())
