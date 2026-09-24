"""Fortieth-stage: tooltips, hurt slots and multiline finalizeSpawn.

usage: python tools/port_rewrite40.py <error-log>
"""
from __future__ import annotations

import collections
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from port_rewrite import ensure_import, match_forward, split_args  # noqa: E402

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")

HOVER = re.compile(
    r"public\s+void\s+appendHoverText\(\s*ItemStack\s+(\w+)\s*,\s*(?:@?\w+\s+)*Level\s+(\w+)\s*,\s*"
    r"(?:@?\w+\s+)*List<Component>\s+(\w+)\s*,\s*(?:@?\w+\s+)*TooltipFlag\s+(\w+)\s*\)")


def process(path: str, text: str) -> tuple[str, dict]:
    stats: dict = {}
    # 1.21 tooltips take a context and a consumer
    m = HOVER.search(text)
    if m:
        stack, level, tooltip, flag = m.groups()
        signature = ("public void appendHoverText(ItemStack %s, Item.TooltipContext %s, TooltipFlag %s, "
                     "Consumer<Component> %s)" % (stack, level, flag, tooltip))
        text = text[:m.start()] + signature + text[m.end():]
        # within that method the list becomes a consumer
        brace = text.index("{", m.start())
        close = match_forward(text, brace)
        if close > 0:
            body = text[brace:close]
            body = re.sub(r"\b%s\.add\(" % re.escape(tooltip), "%s.accept(" % tooltip, body)
            text = text[:brace] + body + text[close:]
        text = ensure_import(text, "java.util.function.Consumer")
        stats["appendHoverText"] = 1
    return text, stats


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

    # 2. per line fixes located from the compiler log
    if len(sys.argv) > 1:
        raw = open(sys.argv[1], "rb").read()
        encoding = "utf-16" if raw[:2] in (b"\xff\xfe", b"\xfe\xff") else "utf-8"
        lines = raw.decode(encoding, "replace").splitlines()
        hits: dict[str, list[int]] = collections.defaultdict(list)
        for line in lines:
            m = re.search(r"([A-Za-z]:\\[^:]+\.java):(\d+): error: (.*)$", line)
            if not m:
                continue
            if "InteractionHand cannot be converted to EquipmentSlot" in m.group(3) \
                    or "finalizeSpawn in class Mob cannot be applied" in m.group(3):
                hits[os.path.normpath(m.group(1))].append(int(m.group(2)))
        fixed = 0
        for path, numbers in hits.items():
            if not os.path.isfile(path):
                continue
            text = open(path, encoding="utf-8", errors="replace").read()
            file_lines = text.split("\n")
            for number in numbers:
                idx = number - 1
                if not (0 <= idx < len(file_lines)):
                    continue
                original = file_lines[idx]
                new = re.sub(r"\.hurtAndBreak\((\d+),\s*([\w.]+),\s*(\w+)\)",
                             r".hurtAndBreak(\1, \2, \2.getUsedItemHand())", original)
                if new == original:
                    # finalizeSpawn(x, y, z, a, b) -> four arguments
                    mm = re.search(r"finalizeSpawn\(", original)
                    if mm:
                        op = mm.end() - 1
                        cl = match_forward(original, op)
                        if cl > 0:
                            args = split_args(original[op + 1:cl])
                            if len(args) > 4:
                                new = original[:op + 1] + ", ".join(args[:4]) + original[cl:]
                if new != original:
                    file_lines[idx] = new
                    fixed += 1
            with open(path, "w", encoding="utf-8", newline="") as fh:
                fh.write("\n".join(file_lines))
        total["log driven lines"] = fixed

    for k, v in sorted(total.items(), key=lambda kv: -kv[1]):
        print(f"{v:6d}  {k}")


if __name__ == "__main__":
    main()
