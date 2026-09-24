"""Thirty-third-stage: log driven fixes for the remaining bulk categories.

For each compiler-flagged line:
  * `new MobEffectInstance(rawEffect, ...)`  -> wrap the effect in a holder
  * `stack.save(tag)`                        -> `stack.save(registries, tag)`
  * `stack.hurtAndBreak(n, entity, hand)`    -> use the entity's used hand as slot

usage: python tools/port_rewrite33.py <error-log>
"""
from __future__ import annotations

import collections
import os
import re
import sys

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")
ERR = re.compile(r"([A-Za-z]:\\[^:]+\.java):(\d+): error: (.*)$")

MOB_EFFECT = re.compile(r"new MobEffectInstance\(\s*([\w.]+)\s*,")
SAVE = re.compile(r"(\.save\(\s*)([\w.]+)\s*\)")
HURT = re.compile(r"\.hurtAndBreak\((\d+),\s*([\w.]+),\s*(\w+)\)")


def main() -> None:
    log = sys.argv[1]
    raw = open(log, "rb").read()
    encoding = "utf-16" if raw[:2] in (b"\xff\xfe", b"\xfe\xff") else "utf-8"
    lines = raw.decode(encoding, "replace").splitlines()
    hits: dict[str, list[int]] = collections.defaultdict(list)
    for line in lines:
        m = ERR.search(line)
        if not m:
            continue
        message = m.group(3)
        if "MobEffect cannot be converted to Holder" in message \
                or "CompoundTag cannot be converted to Provider" in message \
                or "InteractionHand cannot be converted to EquipmentSlot" in message:
            hits[os.path.normpath(m.group(1))].append(int(m.group(2)))

    total = {"mob effect": 0, "save": 0, "hurtAndBreak": 0}
    for path, numbers in hits.items():
        if not os.path.isfile(path):
            continue
        text = open(path, encoding="utf-8", errors="replace").read()
        file_lines = text.split("\n")
        touched = False
        for number in numbers:
            idx = number - 1
            if not (0 <= idx < len(file_lines)):
                continue
            original = file_lines[idx]
            new = MOB_EFFECT.sub(lambda m: "new MobEffectInstance(top.ribs.scguns.util.ScEffects.holder(%s)," % m.group(1), original)
            if new != original:
                total["mob effect"] += 1
            new = SAVE.sub(lambda m: "%sregistries, %s)" % (m.group(1), m.group(2)), new)
            if new != original and "registries," in new:
                total["save"] += 1
            new = HURT.sub(lambda m: ".hurtAndBreak(%s, %s, %s.getUsedItemHand())" % (m.group(1), m.group(2), m.group(2)), new)
            if new != original and "getUsedItemHand" in new:
                total["hurtAndBreak"] += 1
            if new != original:
                file_lines[idx] = new
                touched = True
        if touched:
            with open(path, "w", encoding="utf-8", newline="") as fh:
                fh.write("\n".join(file_lines))
    for k, v in total.items():
        print(f"{v:6d}  {k}")


if __name__ == "__main__":
    main()
