"""Twenty-fourth-stage: the four highest-count categories left.

  * ModEffects constants are Holders, so `(MobEffect) ModEffects.X.get()` loses the
    cast and the .get().
  * item stack handlers deserialize with a registry lookup (their argument often
    contains parentheses, which the earlier regex refused).
  * MessageContext.getPlayer() yields a Player, so ServerPlayer locals cast.
  * BlockEntity.saveWithoutMetadata() needs the registry lookup.

usage: python tools/port_rewrite24.py <error-log>
"""
from __future__ import annotations

import collections
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from port_rewrite import match_forward, split_args  # noqa: E402

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")
ERR = re.compile(r"([A-Za-z]:\\[^:]+\.java):(\d+): error: (.*)$")


def rewrite_balanced(text: str, pattern: str, template: str) -> tuple[str, int]:
    """Rewrite `NAME(args)` where template may use @N (captured name) and @A (args)."""
    out = text
    pos = 0
    count = 0
    while True:
        m = re.search(pattern, out[pos:])
        if not m:
            break
        open_paren = pos + m.end() - 1
        close = match_forward(out, open_paren)
        if close < 0:
            break
        args = out[open_paren + 1:close]
        name = m.group(1) if m.groups() else ""
        new = template.replace("@N", name).replace("@A", args)
        out = out[:pos + m.start()] + new + out[close + 1:]
        pos = pos + m.start() + len(new)
        count += 1
    return out, count


def process(path: str, text: str) -> tuple[str, dict]:
    stats: dict = {}
    text, n = re.subn(r"\(MobEffect\)\s*(ModEffects\.\w+)\.get\(\)", r"\1", text)
    if n:
        stats["ModEffects holder"] = n
    text, n = rewrite_balanced(text, r"\b(\w*(?:[Hh]andler|[Ss]torage)\w*)\.deserializeNBT\(",
                               r"@N.deserializeNBT(registries, @A)")
    if n:
        stats["deserializeNBT"] = n
    text, n = rewrite_balanced(text, r"\bsaveWithoutMetadata\(", "saveWithoutMetadata(registries)")
    if n:
        stats["saveWithoutMetadata"] = n
    text, n = re.subn(r"ServerPlayer\s+(\w+)\s*=\s*context\.getPlayer\(\)\.orElse\(null\);",
                      r"ServerPlayer \1 = context.getPlayer() instanceof ServerPlayer serverPlayer ? serverPlayer : null;",
                      text)
    if n:
        stats["ServerPlayer from context"] = n
    if any(k in stats for k in ("deserializeNBT", "saveWithoutMetadata")):
        if "import net.minecraft.core.HolderLookup;" not in text:
            m = re.search(r"^package [\w.]+;\s*$", text, re.M)
            if m:
                text = text[:m.end()] + "\n\nimport net.minecraft.core.HolderLookup;" + text[m.end():]
    return text, stats


def fix_render_lines(log: str, stats: dict) -> None:
    raw = open(log, "rb").read()
    encoding = "utf-16" if raw[:2] in (b"\xff\xfe", b"\xfe\xff") else "utf-8"
    lines = raw.decode(encoding, "replace").splitlines()
    fixes: dict[str, set[int]] = collections.defaultdict(set)
    for line in lines:
        m = ERR.search(line)
        if not m:
            continue
        if "no suitable method found for render(" in m.group(3) or "ModelPart.render(PoseStack,VertexConsumer,int,int)" in line:
            fixes[os.path.normpath(m.group(1))].add(int(m.group(2)))
    total = 0
    for path, numbers in fixes.items():
        if not os.path.isfile(path):
            continue
        text = open(path, encoding="utf-8", errors="replace").read()
        file_lines = text.split("\n")
        for number in numbers:
            idx = number - 1
            if not (0 <= idx < len(file_lines)):
                continue
            line = file_lines[idx]
            m = re.search(r"\.render\(", line)
            if not m:
                continue
            open_paren = m.end() - 1
            close = match_forward(line, open_paren)
            if close < 0:
                continue
            args = split_args(line[open_paren + 1:close])
            if len(args) < 6:
                continue
            new_line = line[:open_paren + 1] + ", ".join(args[:-4]) + line[close:]
            if new_line != line:
                file_lines[idx] = new_line
                total += 1
        with open(path, "w", encoding="utf-8", newline="") as fh:
            fh.write("\n".join(file_lines))
    stats["render colour args"] = total


def main() -> None:
    files = [os.path.join(d, f) for d, _, fs in os.walk(SRC) for f in fs if f.endswith(".java")]
    total: dict = {}
    for path in files:
        text = open(path, encoding="utf-8", errors="replace").read()
        new, stats = process(path, text)
        if new != text:
            with open(path, "w", encoding="utf-8", newline="") as fh:
                fh.write(new)
            for k, v in stats.items():
                total[k] = total.get(k, 0) + v
    if len(sys.argv) > 1:
        fix_render_lines(sys.argv[1], total)
    for k, v in sorted(total.items(), key=lambda kv: -kv[1]):
        print(f"{v:6d}  {k}")


if __name__ == "__main__":
    main()
