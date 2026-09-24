"""Fourteenth-stage: undo the over-broad INBTSerializable widening.

NeoForge's INBTSerializable now takes a registry lookup, but the Scorched Guns
holders that implement it (Vent, VentCollectorConfig.Processing, ...) are plain
mod-internal data holders that are only ever read/written by the mod itself. So
instead of threading a provider through every call site, they stop implementing
the interface and keep their provider-free methods.
"""
from __future__ import annotations

import os
import re
import sys

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")

SKIP_DIR = ("blockentity",)


def main() -> None:
    changed = 0
    for dirpath, _, filenames in os.walk(SRC):
        if any(part in dirpath for part in SKIP_DIR):
            continue
        for fn in filenames:
            if not fn.endswith(".java"):
                continue
            path = os.path.join(dirpath, fn)
            text = open(path, encoding="utf-8", errors="replace").read()
            if "INBTSerializable<CompoundTag>" not in text:
                continue
            new = text.replace(", INBTSerializable<CompoundTag>", "")
            new = new.replace("implements INBTSerializable<CompoundTag>", "")
            new = new.replace("import net.neoforged.neoforge.common.util.INBTSerializable;\n", "")
            new = re.sub(r"public\s+void\s+deserializeNBT\(HolderLookup\.Provider \w+, CompoundTag (\w+)\)",
                         r"public void deserializeNBT(CompoundTag \1)", new)
            new = re.sub(r"public\s+CompoundTag\s+serializeNBT\(HolderLookup\.Provider \w+\)",
                         r"public CompoundTag serializeNBT()", new)
            new = new.replace(".serializeNBT(registries)", ".serializeNBT()")
            new = re.sub(r"\.deserializeNBT\(registries,\s*([^()]+)\)", r".deserializeNBT(\1)", new)
            new = new.replace("import net.minecraft.core.HolderLookup;\n", "")
            if new != text:
                with open(path, "w", encoding="utf-8", newline="") as fh:
                    fh.write(new)
                changed += 1
                print("  shimmed", os.path.relpath(path, ROOT))
    print(f"rewrote {changed} files")


if __name__ == "__main__":
    main()
