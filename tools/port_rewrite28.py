"""Twenty-eighth-stage: assorted core fixes found in the four largest files.

usage: python tools/port_rewrite28.py
"""
from __future__ import annotations

import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from port_rewrite import ensure_import, match_forward  # noqa: E402

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")


def remove_method(text: str, signature: str) -> tuple[str, int]:
    out = text
    count = 0
    while True:
        m = re.search(signature, out)
        if not m:
            break
        brace = out.index("{", m.end() - 1)
        close = match_forward(out, brace)
        if close < 0:
            break
        line_start = out.rfind("\n", 0, m.start()) + 1
        prev = out.rfind("\n", 0, line_start - 1) + 1
        while "@Override" in out[prev:line_start] or "@NotNull" in out[prev:line_start]:
            line_start = prev
            prev = out.rfind("\n", 0, line_start - 1) + 1
        out = out[:line_start] + out[close + 1:]
        count += 1
    return out, count


def process(path: str, text: str) -> tuple[str, dict]:
    stats: dict = {}
    # Minecraft#getPartialTick() became the timer's delta tracker
    text, n = re.subn(r"\b(client|mc|minecraft)\.getPartialTick\(\)",
                      r"\1.getTimer().getGameTimeDeltaPartialTick(false)", text)
    if n:
        stats["Minecraft.getPartialTick"] = n
    # EntityDimensions gained an eye height: use the factory
    text, n = re.subn(r"new EntityDimensions\(([^,]+),\s*([^,]+),\s*(?:true|false)\)",
                      r"EntityDimensions.scalable(\1, \2)", text)
    if n:
        stats["EntityDimensions"] = n
    # annotation prefixed InteractionHand arguments
    text, n = re.subn(r"\.hurtAndBreak\((\d+),\s*(\w+),\s*(?:@\w+\s+)?hand\)",
                      r".hurtAndBreak(\1, \2, \2.getUsedItemHand())", text)
    if n:
        stats["hurtAndBreak hand"] = n
    # equip sounds that are holders
    text, n = re.subn(r"playSound\(SoundEvents\.(ARMOR_EQUIP_\w+),", r"playSound(SoundEvents.\1.value(),", text)
    if n:
        stats["equip sound holder"] = n
    # enchantment map entries are holders now
    text, n = re.subn(r"Entry<Enchantment, Integer>", "Entry<Holder<Enchantment>, Integer>", text)
    if n:
        stats["Entry holder"] = n
        text = ensure_import(text, "net.minecraft.core.Holder")
    # final in 1.21
    text, n = remove_method(text, r"public\s+boolean\s+canBreatheUnderwater\(\s*\)\s*\{")
    if n:
        stats["drop canBreatheUnderwater"] = n
    return text, stats


def main() -> None:
    files = [os.path.join(d, f) for d, _, fs in os.walk(SRC) for f in fs if f.endswith(".java")]
    total: dict = {}
    changed = 0
    for path in files:
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
