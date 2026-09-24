"""Thirty-sixth-stage: two mechanical repairs.

  * `context.getPlayer() instanceof ServerPlayer` is not legal - the getter hands
    back an Optional<Player>, so the cast belongs inside the Optional mapping.
  * AttributeModifier is a record now: (ResourceLocation, double, Operation), so
    the UUID constants become ResourceLocations and the name argument goes away.

usage: python tools/port_rewrite36.py
"""
from __future__ import annotations

import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from port_rewrite import ensure_import, match_forward, split_args  # noqa: E402

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")

BAD_CAST = re.compile(
    r"((?:final\s+)?ServerPlayer\s+\w+\s*=\s*)(\w+)\.getPlayer\(\)\s*instanceof\s+ServerPlayer\s+(\w+)\s*\?\s*\3\s*:\s*null;"
)
UUID_CONST = re.compile(r"UUID\s+(\w+)\s*=\s*UUID\.fromString\(\s*\"([^\"]+)\"\s*\)")


def fix_attr_modifiers(text: str, stats: dict) -> str:
    out = text
    pos = 0
    count = 0
    while True:
        m = re.search(r"new AttributeModifier\(", out[pos:])
        if not m:
            break
        open_paren = pos + m.end() - 1
        close = match_forward(out, open_paren)
        if close < 0:
            break
        args = [a.strip() for a in split_args(out[open_paren + 1:close])]
        if len(args) == 4:
            # (uuid, name, amount, operation) -> (location, amount, operation)
            new = "new AttributeModifier(%s, %s, %s)" % (args[0], args[2], args[3])
            out = out[:pos + m.start()] + new + out[close + 1:]
            pos = pos + m.start() + len(new)
            count += 1
        else:
            pos = close
    if count:
        stats["AttributeModifier args"] = count
    return out


def process(path: str, text: str) -> tuple[str, dict]:
    stats: dict = {}
    text, n = BAD_CAST.subn(r"\1\2.getPlayer().map(player -> (ServerPlayer) player).orElse(null);", text)
    if n:
        stats["ServerPlayer optional cast"] = n
    if "new AttributeModifier(" in text:
        text = fix_attr_modifiers(text, stats)
    if "getModifier(" in text and "UUID" in text:
        pass
    # UUID modifier ids became resource locations
    if UUID_CONST.search(text):
        def repl(m: re.Match) -> str:
            return 'ResourceLocation %s = ResourceLocation.fromNamespaceAndPath("scguns", "%s")' % (
                m.group(1), m.group(2))
        text, n = UUID_CONST.subn(repl, text)
        if n:
            stats["modifier id location"] = n
            text = ensure_import(text, "net.minecraft.resources.ResourceLocation")
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
