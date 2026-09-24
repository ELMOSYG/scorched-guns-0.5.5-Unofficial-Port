"""Forty-first-stage: the last scattered categories.

usage: python tools/port_rewrite41.py
"""
from __future__ import annotations

import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from port_rewrite import ensure_import, match_forward, split_args  # noqa: E402

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")


def drop_last_arg(text: str, call: str, keep: int, stats: dict, key: str) -> str:
    out = text
    pos = 0
    count = 0
    while True:
        m = re.search(re.escape(call) + r"\(", out[pos:])
        if not m:
            break
        open_paren = pos + m.end() - 1
        close = match_forward(out, open_paren)
        if close < 0:
            break
        args = split_args(out[open_paren + 1:close])
        if len(args) > keep:
            out = out[:open_paren + 1] + ", ".join(a.strip() for a in args[:keep]) + out[close:]
            count += 1
        pos = open_paren + 1
    if count:
        stats[key] = stats.get(key, 0) + count
    return out


def process(path: str, text: str) -> tuple[str, dict]:
    stats: dict = {}
    if ".finalizeSpawn(" in text:
        text = drop_last_arg(text, ".finalizeSpawn", 4, stats, "finalizeSpawn")
    if "RenderUtils." in text:
        text = text.replace("RenderUtils.", "RenderUtil.")
        text = text.replace("import software.bernie.geckolib.util.RenderUtils;",
                            "import software.bernie.geckolib.util.RenderUtil;")
        stats["RenderUtil rename"] = 1
    # recipe holder streams
    if "getAllRecipesFor(" in text and "RecipeHolder" in text:
        text, n = re.subn(r"\.filter\((\w+) -> \1\.matches\(", r".filter(\1 -> \1.value().matches(", text)
        if n:
            stats["holder stream matches"] = n
        if ".findFirst()" in text and "RecipeHolder" in text:
            text, n = re.subn(r"(getAllRecipesFor\([^;]*?\.findFirst\(\))",
                              r"\1.map(net.minecraft.world.item.crafting.RecipeHolder::value)", text)
            if n:
                stats["holder stream unwrap"] = n
    # entities have no provider parameter: take it from their level
    if ".save(registries," in text and "extends BlockEntity" not in text:
        text = text.replace(".save(registries,", ".save(this.level().registryAccess(),")
        stats["entity save provider"] = 1
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
