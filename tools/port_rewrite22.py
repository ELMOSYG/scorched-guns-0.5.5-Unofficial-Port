"""Twenty-second-stage: revert two over-eager textual rules at their exact sites.

  * `event.getPlayer()` was rewritten to `event.getEntity()` everywhere, but only
    PlayerEvent subclasses gained getEntity() - OnDatapackSyncEvent still has
    getPlayer().
  * `event.getPartialTick()` returns a DeltaTracker on frame/level render events
    but a plain float on player/hand render events, so the added
    getGameTimeDeltaPartialTick(false) is wrong for the latter.

Both are located from the compiler log instead of by guessing.

usage: python tools/port_rewrite22.py <error-log>
"""
from __future__ import annotations

import collections
import os
import re
import sys

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"

ERR = re.compile(r"([A-Za-z]:\\[^:]+\.java):(\d+): error: (.*)$")


def main() -> None:
    log = sys.argv[1]
    raw = open(log, "rb").read()
    encoding = "utf-16" if raw[:2] in (b"\xff\xfe", b"\xfe\xff") else "utf-8"
    lines = raw.decode(encoding, "replace").splitlines()

    fixes: dict[str, set[int]] = collections.defaultdict(set)
    for i, line in enumerate(lines):
        m = ERR.search(line)
        if not m:
            continue
        message = m.group(3)
        if "float cannot be dereferenced" in message:
            fixes[os.path.normpath(m.group(1))].add(int(m.group(2)))
        elif "cannot find symbol" in message and any("method getEntity()" in nxt for nxt in lines[i:i + 5]):
            fixes[os.path.normpath(m.group(1))].add(int(m.group(2)))

    total = 0
    for path, numbers in fixes.items():
        if not os.path.isfile(path):
            continue
        text = open(path, encoding="utf-8", errors="replace").read()
        file_lines = text.split("\n")
        changed = 0
        for number in numbers:
            idx = number - 1
            if idx < 0 or idx >= len(file_lines):
                continue
            original = file_lines[idx]
            new = original.replace(
                "event.getPartialTick().getGameTimeDeltaPartialTick(false)",
                "event.getPartialTick()")
            if "getEntity()" in new:
                new = new.replace("event.getEntity()", "event.getPlayer()")
            if new != original:
                file_lines[idx] = new
                changed += 1
        if changed:
            with open(path, "w", encoding="utf-8", newline="") as fh:
                fh.write("\n".join(file_lines))
            total += changed
            print(f"  {os.path.relpath(path, ROOT)}: {changed} site(s)")
    print(f"reverted {total} sites")


if __name__ == "__main__":
    main()
