"""Twenty-fifth-stage: 1.21 block contracts.

  * BaseEntityBlock gained an abstract `codec()`; concrete blocks provide it with
    `simpleCodec(Block::new)` when their constructor takes only Properties.
  * SCAttributes constants are Holders, so `(Attribute) SCAttributes.X.get()`
    loses both the cast and the .get().
  * ItemStack.hasCustomHoverName() became a component lookup.

usage: python tools/port_rewrite25.py
"""
from __future__ import annotations

import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from port_rewrite import ensure_import, match_forward  # noqa: E402

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")

CLASS_DECL = re.compile(r"(public|abstract public|abstract)\s+(?:abstract\s+)?class\s+(\w+)\s+extends\s+(\w+)")
PROPS_CTOR = re.compile(r"public\s+(\w+)\s*\(\s*Properties\s+\w+\s*\)")


def add_block_codec(path: str, text: str, stats: dict) -> str:
    if "extends BaseEntityBlock" not in text:
        return text
    m = CLASS_DECL.search(text)
    if not m or "abstract" in m.group(1):
        return text
    cls = m.group(2)
    if not PROPS_CTOR.search(text):
        return text  # extra constructor arguments need a hand written codec
    if "MapCodec<" in text:
        return text
    body = ("\n   public static final MapCodec<%s> CODEC = simpleCodec(%s::new);\n\n"
            "   @Override\n"
            "   protected MapCodec<? extends BaseEntityBlock> codec() {\n"
            "      return CODEC;\n"
            "   }\n") % (cls, cls)
    # insert right after the class declaration line
    decl_end = text.index("{", m.end() - 1) + 1
    out = text[:decl_end] + body + text[decl_end:]
    out = ensure_import(out, "com.mojang.serialization.MapCodec")
    stats["block codec"] = stats.get("block codec", 0) + 1
    return out


def process(path: str, text: str) -> tuple[str, dict]:
    stats: dict = {}
    text = add_block_codec(path, text, stats)
    text, n = re.subn(r"\(Attribute\)\s*(SCAttributes\.\w+)\.get\(\)", r"\1", text)
    if n:
        stats["SCAttributes holder"] = n
    text, n = re.subn(r"\(MobEffect\)\s*(ModEffects\.\w+)\.get\(\)", r"\1", text)
    if n:
        stats["ModEffects holder"] = n
    text, n = re.subn(r"(\w+)\.hasCustomHoverName\(\)",
                      r"\1.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)", text)
    if n:
        stats["hasCustomHoverName"] = n
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
