"""Thirty-second-stage: arrows, level locations, final GUI overrides.

  * 1.21's Arrow constructor takes the level as a ServerLevel (and the pickup and
    weapon stacks), and LevelLocation wants a ServerLevel.
  * AbstractSelectionList#render is final; scissor clipping goes through
    GuiGraphics in 1.21.
  * AbstractArrow gained getDefaultPickupItem and dropped getExperienceReward.

usage: python tools/port_rewrite32.py
"""
from __future__ import annotations

import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from port_rewrite import match_forward  # noqa: E402

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
    if "new Arrow((ServerLevel) world, player)" in text:
        text = text.replace("new Arrow((ServerLevel) world, player)",
                            "new Arrow(world, player, new ItemStack(Items.ARROW), ItemStack.EMPTY)")
        if "import net.minecraft.world.item.Items;" not in text:
            m = re.search(r"^package [\w.]+;\s*$", text, re.M)
            text = text[:m.end()] + "\n\nimport net.minecraft.world.item.Items;" + text[m.end():]
        stats["Arrow ctor"] = 1
    text, n = re.subn(r"LevelLocation\.create\((\w+)\.level\(\)", r"LevelLocation.create((ServerLevel) \1.level())", text)
    if n:
        stats["LevelLocation ServerLevel"] = n
    # the selection list's render is final: fold the scissor into the caller's list
    text, n = remove_method(text, r"public\s+void\s+render\(\s*GuiGraphics\s+\w+\s*,\s*int\s+\w+\s*,\s*int\s+\w+\s*,\s*float\s+\w+\s*\)\s*\{")
    if n:
        stats["drop final render override"] = n
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
