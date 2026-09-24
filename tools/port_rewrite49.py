"""Forty-ninth-stage: give the armor items their durability back.

0.5.5's `ArmorItem` took its durability from the material
(`ArmorMaterial#getDurabilityForType`). 1.21.1 moved that onto the item, so vanilla
writes `new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(33))`
and `ArmorMaterial` carries no durability at all. The port kept `new Properties()`,
which leaves every piece with durability 0.

    new XArmorItem(ModArmorMaterials.MAT, Type.HELMET, new Properties())
 -> new XArmorItem(ModArmorMaterials.MAT, Type.HELMET,
                   ModArmorMaterials.durability(new Properties(), ModArmorMaterials.MAT, Type.HELMET))

The factors are the ones the 0.5.5 enum used, held by `ModArmorMaterials`.

Idempotent: the rewritten text no longer matches the pattern.

usage: python tools/port_rewrite49.py
"""
from __future__ import annotations

import os
import re
import sys

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
ITEMS = os.path.join(ROOT, "src", "main", "java", "top", "ribs", "scguns", "init", "ModItems.java")

PATTERN = re.compile(r"(new \w+\(ModArmorMaterials\.(\w+), Type\.(\w+), )new Properties\(\)\)")


def process(text: str) -> tuple[str, int]:
    def repl(m: re.Match) -> str:
        head, material, type_ = m.group(1), m.group(2), m.group(3)
        return ("%sModArmorMaterials.durability(new Properties(), ModArmorMaterials.%s, Type.%s))"
                % (head, material, type_))
    return PATTERN.subn(repl, text)


def main() -> None:
    text = open(ITEMS, encoding="utf-8", errors="replace").read()
    new, count = process(text)
    if new != text:
        with open(ITEMS, "w", encoding="utf-8", newline="") as fh:
            fh.write(new)
    print("rewrote %d armor item construction(s)" % count)


SELFTEST = [
    ('"anthralite_helmet", () -> new AnthraliteArmorItem(ModArmorMaterials.ANTHRALITE, Type.HELMET, new Properties())',
     'new AnthraliteArmorItem(ModArmorMaterials.ANTHRALITE, Type.HELMET, '
     'ModArmorMaterials.durability(new Properties(), ModArmorMaterials.ANTHRALITE, Type.HELMET))'),
]


def selftest() -> None:
    for text, expected in SELFTEST:
        got, count = process(text)
        assert count == 1, count
        assert expected in got, got
        again, count2 = process(got)
        assert count2 == 0 and again == got, "not idempotent"
        print("ok   %s" % expected[:78])
    print("port_rewrite49 self-test passed (and is idempotent)")


if __name__ == "__main__":
    if "--selftest" in sys.argv:
        selftest()
    else:
        main()
