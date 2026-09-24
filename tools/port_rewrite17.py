"""Seventeenth-stage: block constructors and food/effect holders.

  * Forge's StairBlock(Supplier<BlockState>, Properties) overload is gone; the
    base state is passed directly (the base block is declared before the stairs,
    so its deferred holder is already registered).
  * DropExperienceBlock's arguments are (IntProvider, Properties) in 1.21; the
    1.20.1 call sites pass them the other way round, and some pass no xp range.
  * FoodProperties.Builder.saturationMod became saturationModifier.
  * MobEffects constants are Holders, so effect keyed maps follow.

usage: python tools/port_rewrite17.py
"""
from __future__ import annotations

import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from port_rewrite import ensure_import, match_forward, split_args  # noqa: E402

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")

AUTHORED = {
    "util/NbtHelper.java", "util/Caps.java", "util/DistHelper.java", "util/MobType.java",
    "util/ScEnchants.java", "util/ScFuels.java", "util/ScEffects.java", "util/ScTrades.java",
    "network/FrameworkMessageBridge.java", "network/PacketHandler.java",
    "init/ModCapabilities.java", "init/ModEnchantments.java", "init/ModArmorMaterials.java",
    "init/ModJukeboxSongs.java", "enchantment/CorrodedEnchantment.java",
}

FOOD = [("\.saturationMod\(", ".saturationModifier(")]
TYPES = [("Map<MobEffect,", "Map<Holder<MobEffect>,"), ("Map<MobEffect ,", "Map<Holder<MobEffect> ,")]


def rewrite_ctor_args(text: str, ctor: str, transform, stats: dict, key: str) -> str:
    out = text
    pos = 0
    while True:
        m = re.search(r"new %s\(" % re.escape(ctor), out[pos:])
        if not m:
            break
        open_paren = pos + m.end() - 1
        close = match_forward(out, open_paren)
        if close < 0:
            break
        args = [a.strip() for a in split_args(out[open_paren + 1:close])]
        new_args = transform(args)
        if new_args is not None and new_args != args:
            out = out[:open_paren + 1] + ", ".join(new_args) + out[close:]
            stats[key] = stats.get(key, 0) + 1
            pos = open_paren + 1
        else:
            pos = close
    return out


def process(path: str, text: str) -> tuple[str, dict]:
    stats: dict = {}
    for pattern, repl in FOOD:
        text, n = re.subn(pattern, repl, text)
        if n:
            stats["saturationMod"] = n
    for old, new in TYPES:
        if old in text:
            text = text.replace(old, new)
            stats["effect map"] = stats.get("effect map", 0) + 1
            text = ensure_import(text, "net.minecraft.core.Holder")
    if "new StairBlock(" in text:
        def stairs(args: list[str]) -> list[str] | None:
            if len(args) == 2 and args[0].startswith("() -> "):
                return [args[0][len("() -> "):].strip(), args[1]]
            return None

        text = rewrite_ctor_args(text, "StairBlock", stairs, stats, "StairBlock base state")
    if "new DropExperienceBlock(" in text:
        def drop_xp(args: list[str]) -> list[str] | None:
            if len(args) == 1:
                return ["net.minecraft.util.valueproviders.ConstantInt.of(0)", args[0]]
            if len(args) == 2 and not args[0].startswith(("UniformInt", "ConstantInt", "net.minecraft.util")):
                return [args[1], args[0]]
            return None

        text = rewrite_ctor_args(text, "DropExperienceBlock", drop_xp, stats, "DropExperienceBlock args")
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
