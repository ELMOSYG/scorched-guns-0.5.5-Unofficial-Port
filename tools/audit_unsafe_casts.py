"""Flag forced casts of an event's entity without a guard.

`OceanWeaponEventHandler` crashed with

    ClassCastException: ItemEntity cannot be cast to LivingEntity

because a `@SubscribeEvent` handler did `((LivingEntity) event.getEntity())` on an
event that fires for every entity. Decompiled code is full of casts like this, and
whether one is safe depends entirely on the event: `PlayerEvent#getEntity()` really
is a `Player`, while `EntityTickEvent#getEntity()` is whatever entity ticked.

The check is deliberately simple and conservative: a cast of `event.getX()` is
reported unless an `instanceof` guard appears in the same statement (the usual
`if (event.getEntity() instanceof Foo foo)` idiom, or an explicit `&&` chain).

usage: python tools/audit_unsafe_casts.py
"""
from __future__ import annotations

import os
import re
import sys

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")

CAST = re.compile(r"\((\w+)\)\s*event\.(get\w+)\(\)")
COMMENT = re.compile(r"//[^\n]*")
# a statement boundary: start at the nearest of these before the cast
BOUNDARY = re.compile(r"[;{}]")

# Verified by hand, with the reason, instead of being silently ignored.
#   RenderPlayerEvent extends PlayerEvent, but NeoForge only fires it from
#   PlayerRenderer#render, whose entity parameter is an AbstractClientPlayer, so
#   this downcast cannot fail.
SAFE = {
    ("client/handler/PlayerModelHandler.java", "AbstractClientPlayer", "getEntity"),
}


def statement_before(text: str, index: int) -> str:
    """The text of the statement containing `index` (from the last } ; or {)."""
    cut = 0
    for m in BOUNDARY.finditer(text, 0, index):
        cut = m.end()
    return text[cut:index]


def main() -> None:
    findings = []
    for d, _, fs in os.walk(SRC):
        for f in fs:
            if not f.endswith(".java"):
                continue
            path = os.path.join(d, f)
            raw = open(path, encoding="utf-8", errors="replace").read()
            if "event.get" not in raw:
                continue
            text = COMMENT.sub(lambda m: " " * len(m.group(0)), raw)
            rel = os.path.relpath(path, SRC).replace("\\", "/")
            for m in CAST.finditer(text):
                cast_type, accessor = m.group(1), m.group(2)
                # the enclosing condition: from the nearest `if (`/`&&` before it
                head = text[max(0, m.start() - 200):m.start()]
                stmt = statement_before(text, m.start())
                guarded = "instanceof" in stmt or "instanceof" in head.split(";")[-1]
                if guarded:
                    continue
                if any(rel.endswith(f) and ct == cast_type and ac == accessor
                       for f, ct, ac in SAFE):
                    print("%-58s      (%s) event.%s()  known safe (see SAFE list)"
                          % (rel, cast_type, accessor))
                    continue
                line = raw[:m.start()].count("\n") + 1
                findings.append((rel, line, cast_type, accessor))

    for rel, line, cast_type, accessor in findings:
        print("%-58s:%-5d (%s) event.%s()  <-- unguarded cast" % (rel, line, cast_type, accessor))
    print("\n%d unguarded cast(s) of an event accessor" % len(findings))
    sys.exit(1 if findings else 0)


if __name__ == "__main__":
    main()
