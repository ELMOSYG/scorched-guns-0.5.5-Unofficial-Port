"""Twentieth-stage: Forge-only event and hook removals.

  * @Cancelable is gone; a custom event becomes cancellable by implementing
    ICancellableEvent (which also provides isCanceled/setCanceled).
  * LootingLevelEvent was removed in 1.21 (looting is data driven through
    enchantment effect components), so the hooks that only tweaked the level go.
  * ForgeHooksClient is gone: armor models come from IClientItemExtensions and
    camera transforms are handled by the item renderer.

usage: python tools/port_rewrite20.py
"""
from __future__ import annotations

import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from port_rewrite import ensure_import, match_forward  # noqa: E402

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")


def remove_method(text: str, signature: str) -> tuple[str, int]:
    out = text
    count = 0
    while True:
        m = re.search(signature, out)
        if not m:
            break
        brace = out.index("{", m.end() - 1)
        close = match_forward(out, brace)
        if close < 0:
            break
        line_start = out.rfind("\n", 0, m.start()) + 1
        prev = out.rfind("\n", 0, line_start - 1) + 1
        if "@SubscribeEvent" in out[prev:line_start] or "@Override" in out[prev:line_start]:
            line_start = prev
        prev2 = out.rfind("\n", 0, line_start - 1) + 1
        if "@SubscribeEvent" in out[prev2:line_start]:
            line_start = prev2
        out = out[:line_start] + out[close + 1:]
        count += 1
    return out, count


def process(path: str, text: str) -> tuple[str, dict]:
    stats: dict = {}
    if "import net.neoforged.bus.api.Cancelable;" in text:
        text = text.replace("import net.neoforged.bus.api.Cancelable;\n", "")
        text = ensure_import(text, "net.neoforged.bus.api.ICancellableEvent")
        text, n = re.subn(r"(class\s+\w+Event\s+extends\s+\w+Event)(\s*\{)", r"\1 implements ICancellableEvent\2", text)
        if n:
            stats["ICancellableEvent"] = n
    if "LootingLevelEvent" in text:
        text = text.replace("import net.neoforged.neoforge.event.entity.living.LootingLevelEvent;\n", "")
        text, n = remove_method(text, r"public\s+static\s+void\s+onLootingLevel\s*\(\s*LootingLevelEvent\s+\w+\s*\)\s*\{")
        if n:
            stats["drop onLootingLevel"] = n
    if "ForgeHooksClient.getArmorModel(" in text:
        text, n = re.subn(
            r"ForgeHooksClient\.getArmorModel\([^;]*?,\s*(\w+)\)",
            r"\1",
            text)
        if n:
            stats["getArmorModel"] = n
        text = text.replace("import net.neoforged.neoforge.client.ForgeHooksClient;\n", "")
    if "ForgeHooksClient.handleCameraTransforms(" in text:
        text, n = re.subn(
            r"\w+\s*=\s*ForgeHooksClient\.handleCameraTransforms\([^;]*\);",
            "// 1.21 lets the item renderer apply display transforms",
            text)
        if n:
            stats["handleCameraTransforms"] = n
        text = text.replace("import net.neoforged.neoforge.client.ForgeHooksClient;\n", "")
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
