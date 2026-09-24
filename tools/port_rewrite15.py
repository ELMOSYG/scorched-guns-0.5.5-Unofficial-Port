"""Fifteenth-stage: armor holders and the renamed spawn placement event.

  * ArmorItem now takes Holder<ArmorMaterial> (ModArmorMaterials is a deferred
    holder bound from the datapack registry), so the mod's armor item
    constructors and any getMaterial() locals follow.
  * Forge's SpawnPlacementRegisterEvent became RegisterSpawnPlacementsEvent and
    SpawnPlacements.Type became SpawnPlacementType (constants in
    SpawnPlacementTypes).

usage: python tools/port_rewrite15.py
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
    "util/ScEnchants.java", "util/ScFuels.java", "util/ScEffects.java",
    "network/FrameworkMessageBridge.java", "network/PacketHandler.java",
    "init/ModCapabilities.java", "init/ModEnchantments.java", "init/ModArmorMaterials.java",
    "enchantment/CorrodedEnchantment.java",
}

SPAWN_TYPES = ("ON_GROUND", "IN_WATER", "NO_RESTRICTIONS", "IN_LAVA")


def process(path: str, text: str) -> tuple[str, dict]:
    stats: dict = {}
    # ---- armor ----------------------------------------------------------
    if "extends ArmorItem" in text:
        text, n = re.subn(r"(?<![\w<>])ArmorMaterial(\s+\w+\s*[,)])", r"Holder<ArmorMaterial>\1", text)
        if n:
            stats["armor param"] = n
        text, n = re.subn(r"\bArmorMaterial(\s+\w+\s*=\s*[\w.]+\.getMaterial\(\))", r"Holder<ArmorMaterial>\1", text)
        if n:
            stats["armor local"] = n
        if "Holder<ArmorMaterial>" in text:
            text = ensure_import(text, "net.minecraft.core.Holder")
            text = ensure_import(text, "net.minecraft.world.item.ArmorMaterial")
    if ".getMaterial()" in text and "ArmorMaterial" in text:
        text, n = re.subn(r"\bArmorMaterial(\s+\w+\s*=\s*[\w.]+\.getMaterial\(\))", r"Holder<ArmorMaterial>\1", text)
        if n:
            stats["armor local"] = stats.get("armor local", 0) + n
            text = ensure_import(text, "net.minecraft.core.Holder")

    # ---- spawn placements ------------------------------------------------
    if "SpawnPlacementRegisterEvent" in text:
        text = text.replace("SpawnPlacementRegisterEvent", "RegisterSpawnPlacementsEvent")
        text = text.replace("import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation;",
                            "import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation;")
        stats["spawn event"] = 1
    if re.search(r"\bType\.(%s)" % "|".join(SPAWN_TYPES), text):
        text, n = re.subn(r"\bType\.(%s)" % "|".join(SPAWN_TYPES), r"SpawnPlacementTypes.\1", text)
        if n:
            stats["SpawnPlacementTypes"] = n
            text = ensure_import(text, "net.minecraft.world.entity.SpawnPlacementTypes")
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
