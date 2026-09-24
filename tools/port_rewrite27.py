"""Twenty-seventh-stage: recipe contracts and the last tick phase guards.

  * 1.21 recipes take a registry lookup in getResultItem/assemble, and the Gson
    item helpers (ShapedRecipe.itemStackFromJson, CraftingHelper.getItemStack)
    were replaced by the ItemStack codec.
  * two client tick handlers still carry the 1.20.1 phase check.

usage: python tools/port_rewrite27.py
"""
from __future__ import annotations

import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from port_rewrite import ensure_import, match_forward  # noqa: E402

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")

JSON_PARSE = [
    (r"(?:\w+\.)?ShapedRecipe\.itemStackFromJson\(", "ItemStack.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, "),
    (r"(?:\w+\.)?CraftingHelper\.getItemStack\(", "ItemStack.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, "),
]


def close_codec_call(text: str, pattern: str) -> tuple[str, int]:
    """Append .getOrThrow() after the argument list of the rewritten call."""
    out = text
    pos = 0
    count = 0
    while True:
        m = re.search(pattern.replace("\\(", "\\("), out[pos:])
        if not m:
            break
        open_paren = pos + m.end() - 1
        close = match_forward(out, open_paren)
        if close < 0:
            break
        # CraftingHelper.getItemStack(json, boolean) -> drop the flag argument
        args = out[open_paren + 1:close]
        if "," in args:
            from port_rewrite import split_args
            parts = split_args(args)
            if len(parts) == 2 and parts[1].strip() in ("true", "false"):
                args = parts[0]
                out = out[:open_paren + 1] + args + out[close:]
                close = open_paren + 1 + len(args)
        if not out[close + 1:close + 14].startswith(".getOrThrow()"):
            out = out[:close + 1] + ".getOrThrow()" + out[close + 1:]
        pos = close + 14
        count += 1
    return out, count


def process(path: str, text: str) -> tuple[str, dict]:
    stats: dict = {}
    if "implements Recipe<" in text:
        text, n = re.subn(r"public\s+ItemStack\s+getResultItem\(\s*\)",
                          "public ItemStack getResultItem(HolderLookup.Provider registries)", text)
        if n:
            stats["getResultItem"] = n
        text, n = re.subn(r"public\s+ItemStack\s+assemble\(\s*(\w+)\s+(\w+)\s*\)",
                          r"public ItemStack assemble(\1 \2, HolderLookup.Provider registries)", text)
        if n:
            stats["assemble"] = n
        if "HolderLookup.Provider" in text:
            text = ensure_import(text, "net.minecraft.core.HolderLookup")
    for pattern, replacement in JSON_PARSE:
        if re.search(pattern, text):
            text, n = re.subn(pattern, replacement, text)
            text, m = close_codec_call(text, replacement.replace("ItemStack.CODEC.parse(", r"ItemStack\.CODEC\.parse\("))
            if n or m:
                stats["item json codec"] = n
    return text, stats


def fix_tick_handlers(path: str, text: str, stats: dict) -> str:
    """`ClientTickEvent event` + `event.phase == Phase.END` -> ClientTickEvent.Post."""
    if "Phase.END" not in text and "Phase.START" not in text:
        return text
    out = text
    guard = re.compile(r"if\s*\(\s*(\w+)\.phase\s*(==|!=)\s*Phase\.(START|END)\s*\)\s*\{")
    pos = 0
    while True:
        m = guard.search(out, pos)
        if not m:
            break
        brace = out.index("{", m.end() - 1)
        close = match_forward(out, brace)
        if close < 0:
            break
        # unwrap the guard and switch the handler parameter to the matching phase class
        out = out[:m.start()] + "{" + out[brace + 1:close] + "}" + out[close + 1:]
        param = m.group(1)
        out = re.sub(r"ClientTickEvent\s+%s\b" % param, "ClientTickEvent.Post %s" % param, out, count=1)
        stats["tick phase"] = stats.get("tick phase", 0) + 1
        pos = m.start() + 1
    return out


def main() -> None:
    files = [os.path.join(d, f) for d, _, fs in os.walk(SRC) for f in fs if f.endswith(".java")]
    total: dict = {}
    changed = 0
    for path in files:
        text = open(path, encoding="utf-8", errors="replace").read()
        new, stats = process(path, text)
        new = fix_tick_handlers(path, new, stats)
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
