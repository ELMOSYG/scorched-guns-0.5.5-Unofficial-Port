"""Seventh-stage rules: 1.21 enchantment API.

Enchantments became datapack entries addressed by ResourceKey, so every Forge
helper that took an `Enchantment` instance disappears and the mod's own
enchantments are plain JSON now. This pass rewrites the call sites onto
EnchantmentHelper (for mod enchantments, whose DeferredHolder is already a
Holder) and onto ScEnchants (vanilla enchantments + the removed Forge helpers).

usage: python tools/port_rewrite7.py
"""
from __future__ import annotations

import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from port_rewrite import ensure_import, receiver_start, rewrite_dot_calls  # noqa: E402

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")

AUTHORED = {
    "util/NbtHelper.java", "util/Caps.java", "util/DistHelper.java", "util/MobType.java",
    "util/ScEnchants.java", "network/FrameworkMessageBridge.java", "network/PacketHandler.java",
    "init/ModCapabilities.java", "init/ModEnchantments.java",
    "enchantment/CorrodedEnchantment.java",
}

MOD_LEVEL = re.compile(
    r"EnchantmentHelper\.getItemEnchantmentLevel\(\s*\(Enchantment\)\s*(ModEnchantments\.\w+)\.get\(\)\s*,\s*"
)
ITEM_STACK_LEVEL = re.compile(r"\.getEnchantmentLevel\(\(Enchantment\)\s*(ModEnchantments\.\w+)\.get\(\)\)")
VANILLA_ENTITY_LEVEL = re.compile(r"EnchantmentHelper\.getEnchantmentLevel\(\s*(Enchantments\.\w+)\s*,\s*")
VANILLA_STACK_LEVEL = re.compile(r"\.getEnchantmentLevel\((Enchantments\.\w+)\)")
SIMPLE_HELPERS = [
    (r"EnchantmentHelper\.getDamageBonus\(", "ScEnchants.getDamageBonus("),
    (r"EnchantmentHelper\.getKnockbackBonus\(", "ScEnchants.getKnockbackBonus("),
    (r"EnchantmentHelper\.getFireAspect\(", "ScEnchants.getFireAspect("),
    (r"EnchantmentHelper\.getSweepingDamageRatio\(", "ScEnchants.getSweepingDamageRatio("),
    (r"EnchantmentHelper\.hasBindingCurse\(", "ScEnchants.hasBindingCurse("),
    (r"EnchantmentHelper\.hasVanishingCurse\(", "ScEnchants.hasVanishingCurse("),
    (r"EnchantmentHelper\.getEnchantments\(", "ScEnchants.getEnchantments("),
]


def rewrite_argument_calls(text: str, method: str, template: str) -> tuple[str, int]:
    """Turn `Method(<recv>, ...)` into template using the first argument."""
    pattern = re.compile(r"\b" + method + r"\(")
    out = text
    count = 0
    pos = 0
    while True:
        m = pattern.search(out, pos)
        if not m:
            break
        open_paren = m.end() - 1
        from port_rewrite import match_forward, split_args
        close = match_forward(out, open_paren)
        if close < 0:
            break
        args = [a.strip() for a in split_args(out[open_paren + 1:close])]
        if len(args) != 2:
            pos = close
            continue
        new = template.replace("@1", args[0]).replace("@2", args[1])
        out = out[:m.start()] + new + out[close + 1:]
        pos = m.start() + len(new)
        count += 1
    return out, count


def process(path: str, text: str) -> tuple[str, dict]:
    stats: dict = {}
    text, n = MOD_LEVEL.subn(r"EnchantmentHelper.getItemEnchantmentLevel(\1, ", text)
    if n:
        stats["mod level"] = n
    if ITEM_STACK_LEVEL.search(text):
        out = text
        pos = 0
        count = 0
        while True:
            m = ITEM_STACK_LEVEL.search(out, pos)
            if not m:
                break
            start = receiver_start(out, m.start())
            while start < m.start() and out[start].isspace():
                start += 1
            recv = out[start:m.start()].strip()
            new = "EnchantmentHelper.getItemEnchantmentLevel(%s, %s)" % (m.group(1), recv)
            out = out[:start] + new + out[m.end():]
            pos = start + len(new)
            count += 1
        text = out
        stats["stack level"] = count
    text, n = rewrite_argument_calls(text, "EnchantmentHelper.getEnchantmentLevel", "ScEnchants.level(@2, @1)")
    if n:
        stats["entity level"] = n
    text, n = rewrite_argument_calls(text, "EnchantmentHelper.setEnchantments", "ScEnchants.setEnchantments(@2, @1)")
    if n:
        stats["setEnchantments"] = n
    if VANILLA_STACK_LEVEL.search(text):
        out = text
        pos = 0
        count = 0
        while True:
            m = VANILLA_STACK_LEVEL.search(out, pos)
            if not m:
                break
            start = receiver_start(out, m.start())
            while start < m.start() and out[start].isspace():
                start += 1
            recv = out[start:m.start()].strip()
            new = "ScEnchants.level(%s, %s)" % (recv, m.group(1))
            out = out[:start] + new + out[m.end():]
            pos = start + len(new)
            count += 1
        text = out
        stats["vanilla stack level"] = count
    for pattern, repl in SIMPLE_HELPERS:
        text, n = re.subn(pattern, repl, text)
        if n:
            stats["helper"] = stats.get("helper", 0) + n
    if "Map<Enchantment, Integer>" in text:
        text = text.replace("Map<Enchantment, Integer>", "Map<Holder<Enchantment>, Integer>")
        text = ensure_import(text, "net.minecraft.core.Holder")
        stats["map type"] = 1
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
