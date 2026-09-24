"""Twenty-ninth-stage: tool constructors, records and the last GUI leftovers.

  * 1.21 tool items lost the attack damage/speed constructor arguments (they live
    in the Tool component / default attribute modifiers now).
  * several decompiled records carry an explicit super() call, which a canonical
    record constructor may not have.
  * AbstractSelectionList#updateNarration is final; VertexConsumer wants floats.

usage: python tools/port_rewrite29.py
"""
from __future__ import annotations

import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from port_rewrite import ensure_import, match_forward, split_args  # noqa: E402

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")

TOOLS = ("PickaxeItem", "SwordItem", "AxeItem", "ShovelItem", "HoeItem")


def fix_tool_ctors(text: str, stats: dict) -> str:
    for tool in TOOLS:
        pattern = re.compile(r"new\s+" + tool + r"\(")
        out = text
        pos = 0
        while True:
            m = pattern.search(out, pos)
            if not m:
                break
            open_paren = m.end() - 1
            close = match_forward(out, open_paren)
            if close < 0:
                break
            args = [a.strip() for a in split_args(out[open_paren + 1:close])]
            if len(args) == 4:
                new = "new %s(%s, %s)" % (tool, args[0], args[3])
                out = out[:m.start()] + new + out[close + 1:]
                pos = m.start() + len(new)
                stats["tool ctor"] = stats.get("tool ctor", 0) + 1
            else:
                pos = close
        text = out
    return text


def fix_vertex_floats(text: str, stats: dict) -> str:
    out = text
    pos = 0
    count = 0
    while True:
        m = re.search(r"\.addVertex\(", out[pos:])
        if not m:
            break
        open_paren = pos + m.end() - 1
        close = match_forward(out, open_paren)
        if close < 0:
            break
        args = out[open_paren + 1:close]
        new_args = args.replace("(double)", "(float)")
        if new_args != args:
            out = out[:open_paren + 1] + new_args + out[close:]
            count += 1
        pos = open_paren + 1
    if count:
        stats["vertex double->float"] = count
    return out


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
    if "new PickaxeItem(" in text or "new SwordItem(" in text or "new AxeItem(" in text \
            or "new ShovelItem(" in text or "new HoeItem(" in text:
        text = fix_tool_ctors(text, stats)
    if ".addVertex(" in text and "(double)" in text:
        text = fix_vertex_floats(text, stats)
    if "record " in text and "super();" in text:
        # canonical record constructors may not delegate
        text, n = re.subn(r"(\n\s*)super\(\);\n", r"\n", text)
        if n:
            stats["record super call"] = n
    text, n = remove_method(text, r"public\s+void\s+updateNarration\(\s*NarrationElementOutput\s+\w+\s*\)\s*\{")
    if n:
        stats["drop updateNarration"] = n
    # enchantment map entries are holders
    text, n = re.subn(r"Enchantment(\s+\w+\s*=\s*\w+\.getKey\(\))", r"Holder<Enchantment>\1", text)
    if n:
        stats["holder local"] = n
        text = ensure_import(text, "net.minecraft.core.Holder")
    if "DamageEnchantment" in text:
        text = text.replace("import net.minecraft.world.item.enchantment.DamageEnchantment;\n", "")
        stats["drop DamageEnchantment import"] = 1
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
