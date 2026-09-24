"""Eleventh-stage: enchantment holder comparisons, recipe buffers, Forge-only hooks.

  * 1.21 addresses enchantments by ResourceKey, so `holder == Enchantments.X`
    becomes a holder/key comparison.
  * `IForgeItem#canApplyAtEnchantingTable` is gone: applicability is data driven
    through each enchantment's supported_items tag.
  * Recipe serializers moved to RegistryFriendlyByteBuf (readItem/writeItem).
  * AbstractClientPlayer#getModelName is gone; the skin model is queried instead.

usage: python tools/port_rewrite11.py
"""
from __future__ import annotations

import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from port_rewrite import ensure_import, match_forward  # noqa: E402

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")

AUTHORED = {
    "util/NbtHelper.java", "util/Caps.java", "util/DistHelper.java", "util/MobType.java",
    "util/ScEnchants.java", "network/FrameworkMessageBridge.java", "network/PacketHandler.java",
    "init/ModCapabilities.java", "init/ModEnchantments.java",
    "enchantment/CorrodedEnchantment.java",
}


def remove_methods(text: str, signature: str, stats: dict, key: str) -> str:
    """Delete every method whose declaration matches `signature`."""
    out = text
    while True:
        m = re.search(signature, out)
        if not m:
            break
        open_brace = out.index("{", m.end() - 1)
        close = match_forward(out, open_brace)
        if close < 0:
            break
        line_start = out.rfind("\n", 0, m.start()) + 1
        # drop a preceding @Override line as well
        prev_line_start = out.rfind("\n", 0, line_start - 1) + 1
        if "@Override" in out[prev_line_start:line_start]:
            line_start = prev_line_start
        out = out[:line_start] + out[close + 1:]
        stats[key] = stats.get(key, 0) + 1
    return out


def process(path: str, text: str) -> tuple[str, dict]:
    stats: dict = {}
    if "canApplyAtEnchantingTable" in text:
        text = remove_methods(
            text,
            r"(?:public|protected)\s+boolean\s+canApplyAtEnchantingTable\s*\([^)]*\)\s*\{",
            stats, "drop canApplyAtEnchantingTable")
    if "Enchantments." in text:
        text, n = re.subn(r"(\w+)\s*==\s*(Enchantments\.\w+)", r"ScEnchants.is(\1, \2)", text)
        if n:
            stats["holder =="] = n
        text, n = re.subn(r"(\w+)\s*!=\s*(Enchantments\.\w+)", r"!ScEnchants.is(\1, \2)", text)
        if n:
            stats["holder !="] = n
        text, n = re.subn(r"(\w+)\.containsKey\(\s*(ModEnchantments\.\w+)\.get\(\)\s*\)",
                          r"ScEnchants.contains(\1, \2)", text)
        if n:
            stats["containsKey"] = n
        text, n = re.subn(r"(\w+)\.get\(\s*(ModEnchantments\.\w+)\.get\(\)\s*\)",
                          r"ScEnchants.get(\1, \2)", text)
        if n:
            stats["map get"] = n
        if "ScEnchants." in text:
            text = ensure_import(text, "top.ribs.scguns.util.ScEnchants")
    if "canApplyAtEnchantingTable" in text:
        # the surrounding helper that existed only to serve the Forge hook
        text = remove_methods(text, r"(?:public|protected)\s+boolean\s+canApplyAtEnchantingTable\s*\([^)]*\)\s*\{",
                              stats, "drop canApplyAtEnchantingTable")
    if "readItem()" in text or "writeItem(" in text:
        text = text.replace("FriendlyByteBuf buffer", "RegistryFriendlyByteBuf buffer")
        text = text.replace("FriendlyByteBuf buf", "RegistryFriendlyByteBuf buf")
        text = ensure_import(text, "net.minecraft.network.RegistryFriendlyByteBuf")
        stats["registry buffer"] = 1
    if "getModelName()" in text:
        text = text.replace("((AbstractClientPlayer)event.getEntity()).getModelName().equals(\"slim\")",
                            "((AbstractClientPlayer)event.getEntity()).getSkin().model() == PlayerSkin.Model.SLIM")
        text = text.replace(".getModelName().equals(\"slim\")", ".getSkin().model() == PlayerSkin.Model.SLIM")
        text = ensure_import(text, "net.minecraft.client.resources.PlayerSkin")
        stats["getModelName"] = 1
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
