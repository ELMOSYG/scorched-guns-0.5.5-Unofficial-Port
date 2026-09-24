"""Eighth-stage rules: assorted 1.21 removals and renames.

usage: python tools/port_rewrite8.py
"""
from __future__ import annotations

import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from port_rewrite import ensure_import, match_forward, receiver_start  # noqa: E402

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")

AUTHORED = {
    "util/NbtHelper.java", "util/Caps.java", "util/DistHelper.java", "util/MobType.java",
    "util/ScEnchants.java", "network/FrameworkMessageBridge.java", "network/PacketHandler.java",
    "init/ModCapabilities.java", "init/ModEnchantments.java",
    "enchantment/CorrodedEnchantment.java",
}

UNUSED_IMPORTS = [
    "import net.neoforged.fml.DistExecutor;\n",
    "import net.neoforged.neoforge.capabilities.Capability;\n",
    "import net.neoforged.neoforge.common.capabilities.Capability;\n",
    "import net.neoforged.neoforge.common.util.LazyOptional;\n",
]

PACKAGE_FIXES = [
    ("net.neoforged.neoforge.common.crafting.conditions.", "net.neoforged.neoforge.common.conditions."),
    ("net.neoforged.neoforge.client.gui.overlay.VanillaGuiOverlay", "net.neoforged.neoforge.client.gui.VanillaGuiOverlay"),
    ("net.minecraft.world.entity.SpawnPlacements.Type", "net.minecraft.world.entity.SpawnPlacementType"),
    ("net.minecraft.world.entity.SpawnPlacements$Type", "net.minecraft.world.entity.SpawnPlacementType"),
    ("net.minecraft.world.item.enchantment.EnchantmentCategory", None),
]

VANILLA_LEVEL = re.compile(r"EnchantmentHelper\.getItemEnchantmentLevel\(\s*(Enchantments\.\w+)\s*,\s*")


def rewrite_arg_call(text: str, method_pattern: str, template: str) -> tuple[str, int]:
    from port_rewrite import split_args
    out = text
    pos = 0
    count = 0
    while True:
        m = re.search(method_pattern, out[pos:])
        if not m:
            break
        start = pos + m.start()
        open_paren = out.index("(", start + len(m.group(0)) - (len(m.group(0)) - m.group(0).rfind("(")) - 1)
        open_paren = pos + m.start() + m.group(0).rfind("(")
        close = match_forward(out, open_paren)
        if close < 0:
            break
        args = [a.strip() for a in split_args(out[open_paren + 1:close])]
        if len(args) != 2:
            pos = close
            continue
        new = template.replace("@1", args[0]).replace("@2", args[1])
        out = out[:start] + new + out[close + 1:]
        pos = start + len(new)
        count += 1
    return out, count


def process(path: str, text: str) -> tuple[str, dict]:
    stats: dict = {}
    if ".endVertex()" in text:
        stats["endVertex"] = text.count(".endVertex()")
        text = text.replace(".endVertex()", "")
    if "IMenuTypeExtension." in text and "import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;" not in text:
        text = ensure_import(text, "net.neoforged.neoforge.common.extensions.IMenuTypeExtension")
        stats["IMenuTypeExtension import"] = 1
    if "Ingredient.fromJson(" in text:
        text, n = rewrite_arg_call(text, r"Ingredient\.fromJson\(", "@1.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, @2).getOrThrow()")
        if n:
            stats["Ingredient.fromJson"] = n
    for old, new in PACKAGE_FIXES:
        if old in text:
            if new is None:
                text = text.replace("import %s;\n" % old, "")
                stats["drop import " + old.split(".")[-1]] = 1
            else:
                text = text.replace(old, new)
                stats["pkg " + old.split(".")[-1]] = 1
    # unused imports of removed Forge types
    for imp in UNUSED_IMPORTS:
        if imp in text:
            body = text.replace(imp, "")
            symbol = imp.split(".")[-1][:-2]
            if re.search(r"\b%s\b" % re.escape(symbol), body) is None:
                text = body
                stats["drop unused " + symbol] = 1
    # vanilla enchantment levels that the previous pass missed
    text, n = rewrite_arg_call(text, r"EnchantmentHelper\.getItemEnchantmentLevel\(", "ScEnchants.level(@2, @1)")
    if n:
        # only vanilla keys need the holder-free path; mod keys stay on the helper
        stats["enchant level"] = n
    if "ScEnchants." in text:
        text = ensure_import(text, "top.ribs.scguns.util.ScEnchants")
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
