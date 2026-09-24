"""Repair: the LevelLocation template closed the call before its remaining args."""
from __future__ import annotations

import os
import re

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge\src\main\java"
PATTERN = re.compile(r"(LevelLocation\.create\(\(ServerLevel\) [\w.]+\.level\(\))\)\s*,")


def main() -> None:
    total = 0
    for dirpath, _, filenames in os.walk(ROOT):
        for fn in filenames:
            if not fn.endswith(".java"):
                continue
            path = os.path.join(dirpath, fn)
            text = open(path, encoding="utf-8", errors="replace").read()
            new, n = PATTERN.subn(r"\1,", text)
            if n:
                with open(path, "w", encoding="utf-8", newline="") as fh:
                    fh.write(new)
                total += n
                print(f"  {fn}: {n}")
    print(f"repaired {total} call sites")


if __name__ == "__main__":
    main()
