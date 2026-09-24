"""Thirty-first-stage: GeckoLib render args, skin texture, capability leftovers.

usage: python tools/port_rewrite31.py
"""
from __future__ import annotations

import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from port_rewrite import match_forward, split_args  # noqa: E402

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")


def drop_render_colour(text: str, stats: dict) -> str:
    """GeckoLib renderRecursively lost the trailing r,g,b,a like vanilla did."""
    out = text
    pos = 0
    count = 0
    while True:
        m = re.search(r"\.renderRecursively\(", out[pos:])
        if not m:
            break
        open_paren = pos + m.end() - 1
        close = match_forward(out, open_paren)
        if close < 0:
            break
        args = split_args(out[open_paren + 1:close])
        if len(args) == 12:
            out = out[:open_paren + 1] + ", ".join(args[:11]) + out[close:]
            count += 1
        pos = open_paren + 1
    if count:
        stats["renderRecursively colour args"] = count
    return out


def process(path: str, text: str) -> tuple[str, dict]:
    stats: dict = {}
    if ".renderRecursively(" in text:
        text = drop_render_colour(text, stats)
    text, n = re.subn(r"(\w+)\.getSkinTextureLocation\(\)", r"\1.getSkin().texture()", text)
    if n:
        stats["getSkinTextureLocation"] = n
    text, n = re.subn(r"(\w+)\.getModelName\(\)\.equals\(\"slim\"\)",
                      r"\1.getSkin().model() == net.minecraft.client.resources.PlayerSkin.Model.SLIM", text)
    if n:
        stats["getModelName"] = n
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
