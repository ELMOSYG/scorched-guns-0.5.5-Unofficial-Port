"""Forty-fifth-stage: the `Capabilities.ItemHandler` constant mix-up.

1.20.1 Forge had a single `ForgeCapabilities.ITEM_HANDLER`, which served both
item stacks and block entities. NeoForge split it: `ItemHandler.ITEM` is an
`ItemCapability` for stacks, `ItemHandler.BLOCK` is a `BlockCapability` for
blocks. The mechanical rename picked `.ITEM`, so every block-entity lookup

  * fails to compile (no `getCapability(ItemCapability, Direction)` on
    `BlockEntity`), and then
  * would have returned `null` at runtime anyway, because this port's block
    entity shim compares `cap == Capabilities.ItemHandler.BLOCK`
    (`init/ModCapabilities.java` registers them under `.BLOCK` too).

The menus therefore looked up their own inventory with the wrong capability: the
machine screens would have come up with no item slots.

`Caps#itemHandler(BlockEntity, Direction)` is the port's intended accessor for
this (registered capability first, `Container` wrapper as the 1.20.1 fallback),
and it pins the lambda parameter type, which also fixes the nested-inference
failure that made `handler` an `Object`.

Idempotent; `--selftest` checks both rules.

usage: python tools/port_rewrite45.py
"""
from __future__ import annotations

import os
import re
import sys

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")

# Caps.ifPresent(<recv>.getCapability(Capabilities.ItemHandler.ITEM, null), ...)
BAD_IF_PRESENT = re.compile(
    r"Caps\.ifPresent\(\s*([\w.]+)\.getCapability\(\s*Capabilities\.ItemHandler\.ITEM\s*,\s*null\s*\)\s*,")
# any other block-entity use of the wrong constant
PLAIN_ITEM_CAP = re.compile(r"\.getCapability\(\s*Capabilities\.ItemHandler\.ITEM\s*,")


def process(text: str, stats: dict) -> str:
    text, n = BAD_IF_PRESENT.subn(lambda m: "Caps.ifPresent(Caps.itemHandler(%s, null)," % m.group(1), text)
    if n:
        stats["Caps.itemHandler via ifPresent"] = stats.get("Caps.itemHandler via ifPresent", 0) + n
    left = PLAIN_ITEM_CAP.findall(text)
    if left:
        stats["REMAINING .ITEM block lookups (fix by hand)"] = \
            stats.get("REMAINING .ITEM block lookups (fix by hand)", 0) + len(left)
    return text


def main() -> None:
    total: dict = {}
    changed = 0
    files = [os.path.join(d, f) for d, _, fs in os.walk(SRC) for f in fs if f.endswith(".java")]
    for path in files:
        text = open(path, encoding="utf-8", errors="replace").read()
        new = process(text, total)
        if new != text:
            with open(path, "w", encoding="utf-8", newline="") as fh:
                fh.write(new)
            changed += 1
    print(f"rewrote {changed} files")
    for k, v in sorted(total.items(), key=lambda kv: -kv[1]):
        print(f"{v:6d}  {k}")


SELFTEST = [
    ("Caps.ifPresent(this.blockEntity.getCapability(Capabilities.ItemHandler.ITEM, null), handler -> {",
     "Caps.ifPresent(Caps.itemHandler(this.blockEntity, null), handler -> {"),
    ("Caps.ifPresent(this.blockEntity.getCapability(Capabilities.ItemHandler.ITEM, null), handler -> this.addSlot(new SlotItemHandler(handler, 0, 80, 45)));",
     "Caps.ifPresent(Caps.itemHandler(this.blockEntity, null), handler -> this.addSlot(new SlotItemHandler(handler, 0, 80, 45)));"),
]


def selftest() -> None:
    for text, expected in SELFTEST:
        got = process(text, {})
        print("%s %s" % ("ok  " if got == expected else "FAIL", got))
        assert got == expected, "expected %r got %r" % (expected, got)
        assert process(got, {}) == got, "not idempotent: %r" % got
    print("port_rewrite45 self-test passed (and is idempotent)")


if __name__ == "__main__":
    if "--selftest" in sys.argv:
        selftest()
    else:
        main()
