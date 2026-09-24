"""Eighteenth-stage: item/ingredient (de)serialisation call sites.

  * ItemStack.of(CompoundTag) is gone (components need a registry lookup), so
    NbtHelper gains itemFromTag and all call sites follow.
  * Ingredient.fromNetwork/toNetwork and FriendlyByteBuf.readItem/writeItem were
    replaced by the stream codecs the 1.21 recipe serialisers use.

usage: python tools/port_rewrite18.py
"""
from __future__ import annotations

import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from port_rewrite import ensure_import, match_forward, receiver_start  # noqa: E402

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")

AUTHORED = {
    "util/NbtHelper.java", "util/Caps.java", "util/DistHelper.java", "util/MobType.java",
    "util/ScEnchants.java", "util/ScFuels.java", "util/ScEffects.java", "util/ScTrades.java",
    "network/FrameworkMessageBridge.java", "network/PacketHandler.java",
    "init/ModCapabilities.java", "init/ModEnchantments.java", "init/ModArmorMaterials.java",
    "init/ModJukeboxSongs.java", "enchantment/CorrodedEnchantment.java",
}


def rewrite_encode_call(text: str, pattern: str, template: str) -> tuple[str, int]:
    """Rewrite `RECV.method(args)` into `template` with @R/@A placeholders."""
    out = text
    pos = 0
    count = 0
    while True:
        m = re.search(pattern, out[pos:])
        if not m:
            break
        start = pos + m.start()
        open_paren = pos + m.end() - 1
        close = match_forward(out, open_paren)
        if close < 0:
            break
        args = out[open_paren + 1:close].strip()
        recv_start = receiver_start(out, start)
        while recv_start < start and out[recv_start].isspace():
            recv_start += 1
        recv = out[recv_start:start].strip()
        if not recv:
            pos = close
            continue
        new = template.replace("@R", recv).replace("@A", args)
        out = out[:recv_start] + new + out[close + 1:]
        pos = recv_start + len(new)
        count += 1
    return out, count


def process(path: str, text: str) -> tuple[str, dict]:
    stats: dict = {}
    if "ItemStack.of(" in text:
        text, n = rewrite_encode_call(text, r"ItemStack\.of\(", "NbtHelper.itemFromTag(@A)")
        if n:
            stats["ItemStack.of"] = n
            text = ensure_import(text, "top.ribs.scguns.util.NbtHelper")
    if "Ingredient.fromNetwork(" in text:
        text, n = rewrite_encode_call(text, r"Ingredient\.fromNetwork\(", "Ingredient.CONTENTS_STREAM_CODEC.decode(@A)")
        if n:
            stats["Ingredient.fromNetwork"] = n
    text, n = rewrite_encode_call(text, r"\.toNetwork\(", "Ingredient.CONTENTS_STREAM_CODEC.encode(@A, @R)")
    if n:
        stats["Ingredient.toNetwork"] = n
    text, n = rewrite_encode_call(text, r"\.readItem\(", "ItemStack.STREAM_CODEC.decode(@R)")
    if n:
        stats["readItem"] = n
    text, n = rewrite_encode_call(text, r"\.writeItem\(", "ItemStack.STREAM_CODEC.encode(@R, @A)")
    if n:
        stats["writeItem"] = n
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
