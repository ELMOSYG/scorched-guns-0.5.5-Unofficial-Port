"""Thirty-fifth-stage: recipe holder unwrapping and the last holder/slot casts.

usage: python tools/port_rewrite35.py <error-log>
"""
from __future__ import annotations

import collections
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from port_rewrite import ensure_import, match_forward  # noqa: E402

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")
ERR = re.compile(r"([A-Za-z:\\\\/][^:]*\.java):(\d+): error: (.*)$")

HOLDER_CAST = r"\.map(net\.minecraft\.world\.item\.crafting\.RecipeHolder::value)"


def unwrap_holders(text: str, stats: dict) -> str:
    """`...getRecipeFor(x, y, z)` assigned to Optional<T> -> unwrap the holder."""
    out = text
    pos = 0
    count = 0
    while True:
        m = re.search(r"getRecipeFor\(", out[pos:])
        if not m:
            break
        start = pos + m.start()
        open_paren = pos + m.end() - 1
        close = match_forward(out, open_paren)
        if close < 0:
            break
        if not out[close + 1:close + 20].startswith(".map("):
            out = out[:close + 1] + HOLDER_CAST + out[close + 1:]
            count += 1
        pos = close + 1
    if count:
        stats["recipe holder unwrap"] = stats.get("recipe holder unwrap", 0) + count
    return out


def main() -> None:
    log = sys.argv[1]
    raw = open(log, "rb").read()
    encoding = "utf-16" if raw[:2] in (b"\xff\xfe", b"\xfe\xff") else "utf-8"
    lines = raw.decode(encoding, "replace").splitlines()

    # 1) every getRecipeFor result in the mod is used as a plain recipe
    files = [os.path.join(d, f) for d, _, fs in os.walk(SRC) for f in fs if f.endswith(".java")]
    total: dict = {}
    for path in files:
        text = open(path, encoding="utf-8", errors="replace").read()
        if "getRecipeFor(" not in text:
            continue
        new = unwrap_holders(text, total)
        if new != text:
            with open(path, "w", encoding="utf-8", newline="") as fh:
                fh.write(new)

    # 2) remaining hurtAndBreak slot arguments, located from the log
    hits: dict[str, list[int]] = collections.defaultdict(list)
    for line in lines:
        m = re.search(r"([A-Za-z]:\\[^:]+\.java):(\d+): error: (.*)$", line)
        if not m:
            continue
        if "InteractionHand cannot be converted to EquipmentSlot" in m.group(3) \
                or "Optional<Player> cannot be converted to ServerPlayer" in m.group(3):
            hits[os.path.normpath(m.group(1))].append(int(m.group(2)))
    fixed = 0
    for path, numbers in hits.items():
        if not os.path.isfile(path):
            continue
        text = open(path, encoding="utf-8", errors="replace").read()
        file_lines = text.split("\n")
        touched = False
        for number in numbers:
            idx = number - 1
            if not (0 <= idx < len(file_lines)):
                continue
            line = file_lines[idx]
            new = re.sub(r"\.hurtAndBreak\((\d+),\s*([\w.]+),\s*(\w+)\)",
                         r".hurtAndBreak(\1, \2, \2.getUsedItemHand())", line)
            if new == line:
                new = re.sub(r"\(ServerPlayer\)\s*([\w.]+)\.getPlayer\(\)\.orElse\(null\)",
                             r"\1.getPlayer().map(p -> (ServerPlayer) p).orElse(null)", line)
            if new != line:
                file_lines[idx] = new
                touched = True
                fixed += 1
        if touched:
            with open(path, "w", encoding="utf-8", newline="") as fh:
                fh.write("\n".join(file_lines))
    total["log driven"] = fixed
    for k, v in sorted(total.items(), key=lambda kv: -kv[1]):
        print(f"{v:6d}  {k}")


if __name__ == "__main__":
    main()
