"""Twenty-sixth-stage: two scope slips from earlier passes.

  * load/saveAdditional overrides annotated with @NotNull were missed, so call
    sites gained a `registries` argument that did not exist in the method.
  * the container screens name their partial tick `delta`, not `partialTick`.

usage: python tools/port_rewrite26.py
"""
from __future__ import annotations

import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from port_rewrite import ensure_import  # noqa: E402

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")

ANNOT = r"(?:@\w+(?:\([^)]*\))?\s+)*"


def process(path: str, text: str) -> tuple[str, dict]:
    stats: dict = {}
    if "extends BlockEntity" in text or "/blockentity/" in path.replace("\\", "/"):
        new, n1 = re.subn(
            r"(public|protected)\s+void\s+loadAdditional\(" + ANNOT + r"CompoundTag\s+(\w+)\s*\)",
            lambda m: "%s void loadAdditional(CompoundTag %s, HolderLookup.Provider registries)" % (m.group(1), m.group(2)),
            text)
        new, n2 = re.subn(
            r"(public|protected)\s+void\s+saveAdditional\(" + ANNOT + r"CompoundTag\s+(\w+)\s*\)",
            lambda m: "%s void saveAdditional(CompoundTag %s, HolderLookup.Provider registries)" % (m.group(1), m.group(2)),
            new)
        if n1 or n2:
            stats["be provider param"] = n1 + n2
            new = ensure_import(new, "net.minecraft.core.HolderLookup")
            text = new
    text, n = re.subn(r"(renderBackground\([^;]*?),\s*partialTick\)", r"\1, delta)", text)
    if n:
        stats["renderBackground delta"] = n
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
