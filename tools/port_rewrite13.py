"""Thirteenth-stage: MobEffect/Attribute holders and remaining provider slots.

  * 1.21 wraps effects and attributes in Holder; `MobEffectInstance#getEffect`
    returns a Holder, and mod helpers typed on the raw value need widening.
  * INBTSerializable now takes a registry lookup on both sides, and energy
    storages gained the same provider parameter as item handlers.

usage: python tools/port_rewrite13.py
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
    "util/ScEnchants.java", "util/ScFuels.java", "util/ScEffects.java",
    "network/FrameworkMessageBridge.java", "network/PacketHandler.java",
    "init/ModCapabilities.java", "init/ModEnchantments.java",
    "enchantment/CorrodedEnchantment.java",
}


def process(path: str, text: str) -> tuple[str, dict]:
    stats: dict = {}
    # MobEffectInstance(...) arguments that are raw effects
    text, n = re.subn(r"new MobEffectInstance\(\s*([\w.]+)\.getEffect\(\)\s*,",
                      r"new MobEffectInstance(ScEffects.holder(\1.getEffect()),", text)
    if n:
        stats["MobEffectInstance wrap"] = n
        text = ensure_import(text, "top.ribs.scguns.util.ScEffects")
    # local variables holding a raw effect where the holder is what 1.21 returns
    text, n = re.subn(r"\bMobEffect(\s+\w+\s*=\s*[\w.]+\.getEffect\(\))", r"Holder<MobEffect>\1", text)
    if n:
        stats["Holder<MobEffect> local"] = n
        text = ensure_import(text, "net.minecraft.core.Holder")
    if "ScEffects." in text:
        text = ensure_import(text, "top.ribs.scguns.util.ScEffects")
    # mod helpers that take the raw type but are handed holders now
    for raw, holder in (("MobEffect", "Holder<MobEffect>"), ("Attribute", "Holder<Attribute>")):
        text, n = re.subn(
            r"((?:private|public|protected|static|final|\s)+void\s+remove\w+\(\s*[\w<>.]+\s+\w+\s*,\s*)%s(\s+\w+\s*[,)])" % raw,
            r"\1%s\2" % holder, text)
        if n:
            stats["widen helper " + raw] = n
    # INBTSerializable outside the block entity tree
    if "INBTSerializable<CompoundTag>" in text:
        text, n = re.subn(r"(public|protected)\s+void\s+deserializeNBT\(CompoundTag\s+(\w+)\)",
                          lambda m: "%s void deserializeNBT(HolderLookup.Provider provider, CompoundTag %s)" % (m.group(1), m.group(2)),
                          text)
        if n:
            stats["deserializeNBT override"] = n
        text, n = re.subn(r"(public|protected)\s+CompoundTag\s+serializeNBT\(\)",
                          r"\1 CompoundTag serializeNBT(HolderLookup.Provider provider)", text)
        if n:
            stats["serializeNBT override"] = n
        text = ensure_import(text, "net.minecraft.core.HolderLookup")
    # storages / handlers that need the provider on serialization
    text, n = re.subn(r"(\b\w*(?:[Ss]torage|[Hh]andler)\w*)\.serializeNBT\(\)",
                      r"\1.serializeNBT(registries)", text)
    if n:
        stats["storage.serializeNBT"] = n
    text, n = re.subn(r"(\b\w*(?:[Ss]torage|[Hh]andler)\w*)\.deserializeNBT\(([^()]+)\)",
                      r"\1.deserializeNBT(registries, \2)", text)
    if n:
        stats["storage.deserializeNBT"] = n
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
