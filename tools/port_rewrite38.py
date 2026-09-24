"""Thirty-eighth-stage: modifier id locals and arrays become ResourceLocations."""
from __future__ import annotations

import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from port_rewrite import ensure_import  # noqa: E402

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")

LOCAL_UUID = re.compile(r"\bUUID(\s+\w+[Uu][Uu][Ii][Dd]\w*\s*=\s*)UUID\.randomUUID\(\)")
ARRAY = re.compile(r"new\s+UUID\[\]")


def main() -> None:
    total = {"local": 0, "array": 0}
    for dirpath, _, filenames in os.walk(SRC):
        for fn in filenames:
            if not fn.endswith(".java"):
                continue
            path = os.path.join(dirpath, fn)
            text = open(path, encoding="utf-8", errors="replace").read()
            new = text
            new, n1 = LOCAL_UUID.subn(
                r'ResourceLocation\1ResourceLocation.fromNamespaceAndPath("scguns", "modifier/" + java.util.UUID.randomUUID())',
                new)
            new, n2 = ARRAY.subn("new ResourceLocation[]", new)
            if new != text:
                new = ensure_import(new, "net.minecraft.resources.ResourceLocation")
                with open(path, "w", encoding="utf-8", newline="") as fh:
                    fh.write(new)
                total["local"] += n1
                total["array"] += n2
                print(f"  {fn}: {n1 + n2}")
    for k, v in total.items():
        print(f"{v:6d}  {k}")


if __name__ == "__main__":
    main()
