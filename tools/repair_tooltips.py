"""Repair: 1.21.1 keeps List<Component> in appendHoverText, it only swaps Level
for Item.TooltipContext.

usage: python tools/repair_tooltips.py
"""
from __future__ import annotations

import os
import re

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge\src\main\java"


def main() -> None:
    total = {"signature": 0, "accept": 0, "super": 0}
    for dirpath, _, filenames in os.walk(ROOT):
        for fn in filenames:
            if not fn.endswith(".java"):
                continue
            path = os.path.join(dirpath, fn)
            text = open(path, encoding="utf-8", errors="replace").read()
            original = text
            text, n = re.subn(r"Consumer<Component>(\s+\w+)\)", r"List<Component>\1)", text)
            total["signature"] += n
            if n:
                text = text.replace("import java.util.function.Consumer;\n", "")
            # the tooltip list is a list again
            m = re.search(r"public\s+void\s+appendHoverText\(\s*ItemStack\s+\w+\s*,\s*Item\.TooltipContext\s+(\w+)", text)
            if m:
                context = m.group(1)
                text, n = re.subn(r"\bsuper\.appendHoverText\((\s*[\w.]+)\s*,\s*[\w.]+\s*,",
                                  r"super.appendHoverText(\1, %s," % context, text)
                total["super"] += n
                # the collector is a list in this method
                text, n = re.subn(r"(\w+)\.accept\(", r"\1.add(", text)
                total["accept"] += n
            if text != original:
                with open(path, "w", encoding="utf-8", newline="") as fh:
                    fh.write(text)
    for k, v in total.items():
        print(f"{v:6d}  {k}")


if __name__ == "__main__":
    main()
