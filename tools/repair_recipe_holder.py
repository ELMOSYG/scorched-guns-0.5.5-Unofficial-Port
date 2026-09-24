"""Repair the escaped recipe-holder unwrap that stage 35 wrote literally."""
from __future__ import annotations

import os

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge\src\main\java"
BROKEN = r"\.map(net\.minecraft\.world\.item\.crafting\.RecipeHolder::value)"
FIXED = ".map(net.minecraft.world.item.crafting.RecipeHolder::value)"


def main() -> None:
    total = 0
    for dirpath, _, filenames in os.walk(ROOT):
        for fn in filenames:
            if not fn.endswith(".java"):
                continue
            path = os.path.join(dirpath, fn)
            text = open(path, encoding="utf-8", errors="replace").read()
            if BROKEN not in text:
                continue
            count = text.count(BROKEN)
            with open(path, "w", encoding="utf-8", newline="") as fh:
                fh.write(text.replace(BROKEN, FIXED))
            total += count
            print(f"  {fn}: {count}")
    print(f"repaired {total} sites")


if __name__ == "__main__":
    main()
