"""Tenth-stage: 1.21 serialization now needs a registry lookup.

Block entities gained `loadAdditional(CompoundTag, HolderLookup.Provider)` and a
provider parameter on `saveAdditional`, and the item stack handlers need the same
provider so that components (which may hold registry entries) can round trip.

usage: python tools/port_rewrite10.py
"""
from __future__ import annotations

import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from port_rewrite import ensure_import  # noqa: E402

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")

AUTHORED = {
    "util/NbtHelper.java", "util/Caps.java", "util/DistHelper.java", "util/MobType.java",
    "util/ScEnchants.java", "network/FrameworkMessageBridge.java", "network/PacketHandler.java",
    "init/ModCapabilities.java", "init/ModEnchantments.java",
    "enchantment/CorrodedEnchantment.java",
}

PROVIDER = "HolderLookup.Provider"
PARAM = "registries"


def process(path: str, text: str) -> tuple[str, dict]:
    stats: dict = {}
    rel = os.path.relpath(path, SRC).replace("\\", "/")
    is_be = "extends BlockEntity" in text or "/blockentity/" in "/" + rel
    if not is_be:
        return text, stats

    # --- overrides ---------------------------------------------------------
    text, n = re.subn(
        r"(public|protected)\s+void\s+load\(CompoundTag\s+(\w+)\)",
        lambda m: "%s void loadAdditional(CompoundTag %s, %s %s)" % (m.group(1), m.group(2), PROVIDER, PARAM),
        text)
    if n:
        stats["loadAdditional override"] = n

    def save_repl(m: re.Match) -> str:
        return "%s void saveAdditional(CompoundTag %s, %s %s)" % (m.group(1), m.group(2), PROVIDER, PARAM)

    text, n = re.subn(r"(public|protected)\s+void\s+saveAdditional\(CompoundTag\s+(\w+)\)", save_repl, text)
    if n:
        stats["saveAdditional override"] = n

    # --- super calls -------------------------------------------------------
    text, n = re.subn(r"\bsuper\.load\((\w+)\)", r"super.loadAdditional(\1, %s)" % PARAM, text)
    if n:
        stats["super.load"] = n
    text, n = re.subn(r"\bsuper\.saveAdditional\((\w+)\)", r"super.saveAdditional(\1, %s)" % PARAM, text)
    if n:
        stats["super.saveAdditional"] = n

    # --- helpers that now take a provider ---------------------------------
    for helper in ("loadAllItems", "saveAllItems"):
        text, n = re.subn(
            r"(ContainerHelper\.%s\(\s*[^,()]+,\s*[^,()]+)\s*\)" % helper,
            r"\1, %s)" % PARAM,
            text)
        if n:
            stats["ContainerHelper." + helper] = n
    text, n = re.subn(r"(\b\w*[Hh]andler\w*)\.serializeNBT\(\)", r"\1.serializeNBT(%s)" % PARAM, text)
    if n:
        stats["handler.serializeNBT"] = n
    text, n = re.subn(
        r"(\b\w*[Hh]andler\w*)\.deserializeNBT\(([^()]+)\)",
        r"\1.deserializeNBT(%s, \2)" % PARAM,
        text)
    if n:
        stats["handler.deserializeNBT"] = n

    if any(k.startswith(("loadAdditional", "saveAdditional", "super.", "handler.")) for k in stats):
        text = ensure_import(text, "net.minecraft.core.HolderLookup")
    return text, stats


def main() -> None:
    files = [os.path.join(d, f) for d, _, fs in os.walk(SRC) for f in fs if f.endswith(".java")]
    total: dict = {}
    changed = 0
    for path in files:
        if os.path.relpath(path, SRC).replace("\\", "/") in AUTHORED:
            continue
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
