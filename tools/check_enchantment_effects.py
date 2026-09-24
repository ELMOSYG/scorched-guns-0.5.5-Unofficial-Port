"""Line up each mod enchantment's description with where its effect is actually read.

Descriptions live in the lang files; effects live in code that asks for the enchantment's level. A
description that promises something no code reads is the failure mode this looks for, so for every
enchantment it prints the text and every place the 0.5.5 reference and this port look the level up.

Usage: python tools/check_enchantment_effects.py
"""
import json
import pathlib
import re
import sys

REPO = pathlib.Path(r"E:\mod\scgun-0.5.5-1.21.1-neoforge")
REF = pathlib.Path(r"E:\mod\SG2-1.21\.sg055_deobf\top\ribs\scguns")
EN = REPO / "src/main/resources/assets/scguns/lang/en_us.json"
ZH = REPO / "src/main/resources/assets/scguns/lang/zh_cn.json"
OUT = REPO / "build-logs/enchantment-effects-review.txt"

# class simple name (0.5.5) -> enchantment id (registered)
PAIRS = {
    "Accelerator": "accelerator", "Banzai": "banzai", "Collateral": "collateral",
    "Corroded": "corroded", "ElementalPop": "elemental_pop", "GunRust": "gun_rust",
    "HeavyShot": "heavy_shot", "HotBarrel": "hot_barrel", "Lightweight": "lightweight",
    "Puncturing": "puncturing", "QuickHands": "quick_hands", "Reclaimed": "reclaimed",
    "ShellCatcher": "shell_catcher", "TriggerFinger": "trigger_finger",
    "WaterProof": "waterproof",
}

USE = re.compile(r"(ScEnchants|EnchantmentHelper|getEnchantments|Enchantment\.|ModEnchantments|"
                 r"EnchantmentTypes|enchantment|Enchantments\.)")


def references(root, tokens):
    """Places where any of *tokens* appears in a source tree.

    Three spellings matter: the 0.5.5 class name, the ModEnchantments constant
    (ModEnchantments.RECLAIMED) and the registered id string. Searching only the class name gives
    false "never read" hits for this port, because its ModEnchantments holds ResourceKeys instead of
    classes and the effect is fetched through the constant.
    """
    hits = []
    for path in sorted(root.rglob("*.java")):
        text = path.read_text(encoding="utf-8", errors="replace")
        for number, line in enumerate(text.splitlines(), 1):
            if any(token in line for token in tokens):
                hits.append("%s:%d %s" % (path.relative_to(root), number, line.strip()[:110]))
    return [h for h in hits if not re.search(r"(import|register\()", h)]


def screaming(ench_id):
    return ench_id.upper()


def main():
    en = json.loads(EN.read_text(encoding="utf-8"))
    zh = json.loads(ZH.read_text(encoding="utf-8"))

    lines = []
    problems = 0
    for simple, ench_id in sorted(PAIRS.items(), key=lambda kv: kv[1]):
        key = "enchantment.scguns.%s.desc" % ench_id
        text_en = en.get(key, "(no description key)")
        text_zh = zh.get(key, "(no zh description key)")
        ref_hits = references(REF, [simple, screaming(ench_id)])
        port_hits = references(REPO / "src/main/java/top/ribs/scguns", [simple, screaming(ench_id)])
        lines.append("=" * 100)
        lines.append("%s  (%s)" % (ench_id, simple))
        lines.append("  en: %s" % text_en)
        lines.append("  zh: %s" % text_zh)
        lines.append("  0.5.5 uses it in %d place(s):" % len(ref_hits))
        lines.extend("      " + h for h in ref_hits[:6])
        lines.append("  port uses it in %d place(s):" % len(port_hits))
        lines.extend("      " + h for h in port_hits[:6])
        if ref_hits and not port_hits:
            lines.append("  !! 0.5.5 reads this enchantment and the port never does")
            problems += 1
        elif len(ref_hits) != len(port_hits):
            lines.append("  ?? reference count differs: 0.5.5 %d vs port %d (may be a rename)"
                         % (len(ref_hits), len(port_hits)))

    lines.append("=" * 100)
    lines.append("%d enchantment(s) with a description whose effect is never read by the port"
                 % problems)
    OUT.write_text("\n".join(lines), encoding="utf-8")
    print("wrote %s" % OUT)
    print("%d enchantment(s) never read by the port" % problems)
    return 0


if __name__ == "__main__":
    sys.exit(main())
