"""Ninth-stage: 1.21 rendering API.

  * ModelPart.render(PoseStack, VertexConsumer, light, overlay, r, g, b, a) lost
    the colour arguments in 1.21 (colour lives on the VertexConsumer).
  * BufferBuilder is no longer reusable: begin(mode, format) moved to
    Tesselator.begin(mode, format) and end() became buildOrThrow().
  * A local vertex(...) helper that the earlier pass renamed at call sites only.

usage: python tools/port_rewrite9.py
"""
from __future__ import annotations

import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from port_rewrite import match_forward, split_args  # noqa: E402

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")

AUTHORED = {
    "util/NbtHelper.java", "util/Caps.java", "util/DistHelper.java", "util/MobType.java",
    "util/ScEnchants.java", "network/FrameworkMessageBridge.java", "network/PacketHandler.java",
    "init/ModCapabilities.java", "init/ModEnchantments.java",
    "enchantment/CorrodedEnchantment.java",
}

BEGIN = re.compile(r"(\w+)\.begin\(\s*(VertexFormat\.Mode\.\w+|Mode\.\w+)\s*,\s*([\w.]+)\s*\);")
GET_BUILDER = re.compile(r"(\w[\w.<>]*)\s+(\w+)\s*=\s*([\w.()]+)\.getBuilder\(\);")


def drop_trailing_white(text: str, method: str) -> tuple[str, int]:
    """Remove the four 1.0F colour arguments from `<x>.method(...)` calls."""
    out = text
    count = 0
    pos = 0
    while True:
        m = re.search(r"\." + method + r"\(", out[pos:])
        if not m:
            break
        start = pos + m.start()
        open_paren = pos + m.end() - 1
        close = match_forward(out, open_paren)
        if close < 0:
            break
        args = [a.strip() for a in split_args(out[open_paren + 1:close])]
        if len(args) >= 4 and args[-4:] == ["1.0F", "1.0F", "1.0F", "1.0F"]:
            new_args = ", ".join(args[:-4])
            out = out[:open_paren + 1] + new_args + out[close:]
            count += 1
        pos = open_paren + 1
    return out, count


def fold_buffer_begin(text: str, stats: dict) -> str:
    """`BufferBuilder b = t.getBuilder(); ... b.begin(m, f);` -> `... = t.begin(m, f);`"""
    out = text
    for m in list(GET_BUILDER.finditer(text)):
        var = m.group(2)
        begin = re.search(re.escape(var) + r"\.begin\(\s*([\w.]+)\s*,\s*([\w.]+)\s*\);", text[m.end():])
        if not begin:
            continue
        mode, fmt = begin.group(1), begin.group(2)
        new_decl = "%s %s = %s.begin(%s, %s);" % (m.group(1), var, m.group(3), mode, fmt)
        # remove the separate begin statement (and the whitespace in front of it)
        stmt_start = m.end() + begin.start()
        line_start = text.rfind("\n", 0, stmt_start) + 1
        stmt_end = m.end() + begin.end()
        out = out.replace(text[m.start():m.end()], new_decl, 1)
        out = out[:line_start] + out[stmt_end:]
        stats["buffer begin"] = stats.get("buffer begin", 0) + 1
        text = out
    return out


def process(path: str, text: str) -> tuple[str, dict]:
    stats: dict = {}
    text, n = drop_trailing_white(text, "render")
    if n:
        stats["render colour args"] = n
    text, n = drop_trailing_white(text, "renderToBuffer")
    if n:
        stats["renderToBuffer colour args"] = n
    if ".end()" in text and "BufferBuilder" in text:
        n = text.count(".end()")
        text = text.replace(".end()", ".buildOrThrow()")
        stats["buffer end"] = n
    if "getBuilder()" in text:
        text = fold_buffer_begin(text, stats)
    if "public void vertex(" in text:
        statics = re.findall(r"\bvoid vertex\(", text)
        text = text.replace("public void vertex(", "public void addVertex(")
        text = text.replace("private void vertex(", "private void addVertex(")
        stats["vertex definition rename"] = 1
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
