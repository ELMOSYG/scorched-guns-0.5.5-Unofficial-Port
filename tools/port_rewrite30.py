"""Thirtieth-stage: event bus post semantics, scope slips and level casts.

  * Forge's IEventBus#post returned "was cancelled"; NeoForge returns the event,
    so the call sites ask the event instead.
  * the menu screen listener landed in a method without an event bus.
  * Minecraft#getPartialTick is the timer's delta tracker.
  * 1.21 arrows and level locations want a ServerLevel.

usage: python tools/port_rewrite30.py
"""
from __future__ import annotations

import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from port_rewrite import ensure_import, match_forward  # noqa: E402

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")


def fix_event_posts(text: str, stats: dict) -> str:
    out = text
    pos = 0
    count = 0
    pattern = re.compile(r"(?:NeoForge|MinecraftForge)\.EVENT_BUS\.post\(")
    while True:
        m = pattern.search(out, pos)
        if not m:
            break
        open_paren = m.end() - 1
        close = match_forward(out, open_paren)
        if close < 0:
            break
        if out[close + 1:close + 12] != ".isCanceled":
            out = out[:close + 1] + ".isCanceled()" + out[close + 1:]
            count += 1
        pos = close + 12
    if count:
        stats["event post isCanceled"] = count
    return out


def process(path: str, text: str) -> tuple[str, dict]:
    stats: dict = {}
    if "EVENT_BUS.post(" in text:
        text = fix_event_posts(text, stats)
    text, n = re.subn(r"Minecraft\.getInstance\(\)\.getPartialTick\(\)",
                      "Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false)", text)
    if n:
        stats["Minecraft.getInstance().getPartialTick"] = n
    if "new Arrow(world, player)" in text:
        text = text.replace("new Arrow(world, player)", "new Arrow((ServerLevel) world, player)")
        if "import net.minecraft.server.level.ServerLevel;" not in text:
            m = re.search(r"^package [\w.]+;\s*$", text, re.M)
            text = text[:m.end()] + "\n\nimport net.minecraft.server.level.ServerLevel;" + text[m.end():]
        stats["Arrow ServerLevel"] = 1
    if "net.minecraft.client.gui.screens.controls.MouseSettingsScreen" in text:
        text = text.replace("net.minecraft.client.gui.screens.controls.MouseSettingsScreen",
                            "net.minecraft.client.gui.screens.options.MouseSettingsScreen")
        stats["MouseSettingsScreen package"] = 1
    if "net.minecraft.client.gui.screens.MouseSettingsScreen" in text:
        text = text.replace("net.minecraft.client.gui.screens.MouseSettingsScreen",
                            "net.minecraft.client.gui.screens.options.MouseSettingsScreen")
        stats["MouseSettingsScreen package"] = 1
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
