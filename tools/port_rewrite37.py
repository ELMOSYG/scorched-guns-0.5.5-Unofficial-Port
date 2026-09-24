"""Thirty-seventh-stage: widen modifier-id helper parameters to ResourceLocation."""
from __future__ import annotations

import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from port_rewrite import ensure_import  # noqa: E402

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")

HELPER = re.compile(r"(void\s+(?:add|remove)AttributeModifier\([^)]*?)UUID(\s+\w+)")


def main() -> None:
    total = 0
    for dirpath, _, filenames in os.walk(SRC):
        for fn in filenames:
            if not fn.endswith(".java"):
                continue
            path = os.path.join(dirpath, fn)
            text = open(path, encoding="utf-8", errors="replace").read()
            new, n = HELPER.subn(r"\1ResourceLocation\2", text)
            if not n:
                continue
            new = ensure_import(new, "net.minecraft.resources.ResourceLocation")
            with open(path, "w", encoding="utf-8", newline="") as fh:
                fh.write(new)
            total += n
            print(f"  {fn}: {n}")
    print(f"widened {total} helper parameters")


if __name__ == "__main__":
    main()
