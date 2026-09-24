"""Nineteenth-stage: leftovers from earlier passes.

  * Ingredient.fromNetwork was skipped by the receiver-based rewrite (static call).
  * ModEnchantments constants are ResourceKeys now, so holder comparisons go
    through ScEnchants.is.
  * BootstapContext was a vanilla typo that 1.21 spells BootstrapContext.
  * render events hand over a DeltaTracker, not a partial tick float.
  * a Capability import whose only "hit" was the word inside getCapability.

usage: python tools/port_rewrite19.py
"""
from __future__ import annotations

import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from port_rewrite import ensure_import  # noqa: E402

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")

AUTHORED = {
    "util/NbtHelper.java", "util/Caps.java", "util/DistHelper.java", "util/MobType.java",
    "util/ScEnchants.java", "util/ScFuels.java", "util/ScEffects.java", "util/ScTrades.java",
    "util/CombatHelper.java", "util/Constants.java",
    "network/FrameworkMessageBridge.java", "network/PacketHandler.java",
    "init/ModCapabilities.java", "init/ModEnchantments.java", "init/ModArmorMaterials.java",
    "init/ModJukeboxSongs.java", "enchantment/CorrodedEnchantment.java",
    "common/recipe/ScRecipeSerializer.java", "common/recipe/LegacyRecipeCodec.java",
    "compat/ShoulderSurfingHelper.java",
}


def process(path: str, text: str) -> tuple[str, dict]:
    stats: dict = {}
    if "Ingredient.fromNetwork(" in text:
        stats["Ingredient.fromNetwork"] = text.count("Ingredient.fromNetwork(")
        text = text.replace("Ingredient.fromNetwork(", "Ingredient.CONTENTS_STREAM_CODEC.decode(")
    if "ModEnchantments." in text and ".get()" in text:
        text, n = re.subn(r"(\w+)\s*==\s*(ModEnchantments\.\w+)\.get\(\)", r"ScEnchants.is(\1, \2)", text)
        if n:
            stats["mod enchant =="] = n
        text, n = re.subn(r"(\w+)\s*!=\s*(ModEnchantments\.\w+)\.get\(\)", r"!ScEnchants.is(\1, \2)", text)
        if n:
            stats["mod enchant !="] = n
        if "ScEnchants." in text:
            text = ensure_import(text, "top.ribs.scguns.util.ScEnchants")
    if "BootstapContext" in text:
        stats["BootstapContext"] = text.count("BootstapContext")
        text = text.replace("BootstapContext", "BootstrapContext")
    if ".getPartialTick()" in text and "event.getPartialTick()" in text:
        stats["getPartialTick"] = text.count("event.getPartialTick()")
        text = text.replace("event.getPartialTick()", "event.getPartialTick().getGameTimeDeltaPartialTick(false)")
    imp = "import net.neoforged.neoforge.capabilities.Capability;\n"
    if imp in text:
        body = text.replace(imp, "")
        if not re.search(r"\bCapability<", body):
            text = body
            stats["drop Capability import"] = 1
    return text, stats


def main() -> None:
    files = [os.path.join(d, f) for d, _, fs in os.walk(SRC) for f in fs if f.endswith(".java")]
    total: dict = {}
    changed = 0
    for path in files:
        if os.path.relpath(path, SRC).replace("\\", "/") in AUTHORED:
            continue
        text = open(path, encoding="utf-8", errors="replace").read()
        new, stats = process(path, text)
        if new != text:
            with open(path, "w", encoding="utf-8", newline="") as fh:
                fh.write(new)
            changed += 1
            for k, v in stats.items():
                total[k] = total.get(k, 0) + v
    print(f"rewrote {changed} files")
    for k, v in sorted(total.items(), key=lambda kv: -kv[1]):
        print(f"{v:6d}  {k}")


if __name__ == "__main__":
    main()
