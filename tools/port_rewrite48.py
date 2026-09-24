"""Forty-eighth-stage: `EntityTickEvent` handlers that assume a living entity.

Forge's `LivingEvent.LivingTickEvent` only ever fired for `LivingEntity`. NeoForge
1.21 has no equivalent - the per-entity tick event is `EntityTickEvent.Pre/Post`,
which fires for **every** entity - so the port mapped all five handlers onto it and
kept the original's forced cast:

    public static void onPlayerTick(EntityTickEvent.Post event) {
       if (((LivingEntity) event.getEntity()) instanceof Player player) {

`ItemEntity` is not a `LivingEntity`, so the very first dropped item to tick throws

    java.lang.ClassCastException: ItemEntity cannot be cast to LivingEntity

Reachable in any world (items, projectiles, minecarts, boats, XP orbs).

Two fixes depending on what the handler actually wants:

  * player-only handlers -> `PlayerTickEvent.Post` (extends `PlayerEvent`, so
    `getEntity()` is already a `Player` and no cast is needed),
  * genuinely entity-wide handlers -> keep `EntityTickEvent.Post` and pattern-match
    the type directly instead of casting to `LivingEntity` first.

Idempotent; `--selftest` checks both rewrites and the import editing.

usage: python tools/port_rewrite48.py
"""
from __future__ import annotations

import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from port_rewrite import ensure_import  # noqa: E402

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")
PKG = "top/ribs/scguns/"

# handler -> (file, is the target a player only?)
PLAYER_HANDLERS = [
    "event/OceanWeaponEventHandler.java",
    "event/PiglinWeaponEventHandler.java",
    "event/ArmorBoostEventHandler.java",
]
ENTITY_HANDLERS = [
    "config/GunnerMobSpawner.java",
    "event/BatPoopEvent.java",
]

TICK_IMPORT = "import net.neoforged.neoforge.event.tick.EntityTickEvent;"
PLAYER_IMPORT = "import net.neoforged.neoforge.event.tick.PlayerTickEvent;"

NOTE = ("   // 0.5.5 listened on Forge's LivingTickEvent, which only fired for living\n"
        "   // entities. NeoForge's per-entity tick event fires for every entity, so this\n"
        "   // is pinned to the player tick event: no cast, and item entities no longer\n"
        "   // tick through this handler.\n")


def strip_unused_import(text: str, fqcn: str, symbol: str) -> str:
    line = "import %s;" % fqcn
    if line in text and not re.search(r"(?<![\w.])" + re.escape(symbol) + r"(?![\w])",
                                     text.replace(line, "")):
        return text.replace(line + "\n", "")
    return text


def process(path: str, player_only: bool) -> tuple[str, dict]:
    text = open(path, encoding="utf-8", errors="replace").read()
    stats: dict = {}
    original = text

    if player_only:
        text, n = re.subn(
            r"public static void (onPlayerTick)\(EntityTickEvent\.Post event\) \{",
            lambda m: NOTE + "   public static void %s(PlayerTickEvent.Post event) {" % m.group(1),
            text)
        if n:
            stats["PlayerTickEvent"] = n
        text, n = re.subn(r"\(\(LivingEntity\) event\.getEntity\(\)\) instanceof",
                          "event.getEntity() instanceof", text)
        if n:
            stats["dropped LivingEntity cast"] = n
        if "PlayerTickEvent" in text:
            text = ensure_import(text, "net.neoforged.neoforge.event.tick.PlayerTickEvent")
    else:
        text, n = re.subn(r"\(\(LivingEntity\) event\.getEntity\(\)\) instanceof",
                          "event.getEntity() instanceof", text)
        if n:
            stats["dropped LivingEntity cast"] = n

    # EntityTickEvent is unused once every handler moved to the player tick event
    text = strip_unused_import(text, "net.neoforged.neoforge.event.tick.EntityTickEvent",
                              "EntityTickEvent")
    # LivingEntity often stops being referenced once the cast is gone
    text = strip_unused_import(text, "net.minecraft.world.entity.LivingEntity", "LivingEntity")

    if text != original:
        with open(path, "w", encoding="utf-8", newline="") as fh:
            fh.write(text)
    return text, stats


def main() -> None:
    total: dict = {}
    changed = 0
    for rel, player_only in ([(r, True) for r in PLAYER_HANDLERS]
                             + [(r, False) for r in ENTITY_HANDLERS]):
        path = os.path.join(SRC, PKG.replace("/", os.sep), rel.replace("/", os.sep))
        before = open(path, encoding="utf-8", errors="replace").read()
        after, stats = process(path, player_only)
        if after != before:
            changed += 1
        for k, v in stats.items():
            total[k] = total.get(k, 0) + v
    print(f"rewrote {changed} files")
    for k, v in sorted(total.items(), key=lambda kv: -kv[1]):
        print(f"{v:6d}  {k}")


SELFTEST = [
    ("   public static void onPlayerTick(EntityTickEvent.Post event) {\n"
     "      if (((LivingEntity) event.getEntity()) instanceof Player player) {\n",
     True, ["PlayerTickEvent.Post event", "event.getEntity() instanceof Player", "(LivingEntity)"]),
    ("      if (((LivingEntity) event.getEntity()) instanceof PathfinderMob mob) {\n",
     False, ["event.getEntity() instanceof PathfinderMob mob", "(LivingEntity)"]),
]


def selftest() -> None:
    import tempfile
    for sample, player_only, expectations in SELFTEST:
        tmpdir = tempfile.mkdtemp()
        tmp = os.path.join(tmpdir, "Sample.java")
        with open(tmp, "w", encoding="utf-8") as fh:
            fh.write("package top.ribs.scguns.event;\n\n"
                     "import net.neoforged.neoforge.event.tick.EntityTickEvent;\n"
                     "import net.minecraft.world.entity.LivingEntity;\n\n"
                     "public class Sample {\n" + sample + "      }\n   }\n}\n")
        out, _ = process(tmp, player_only)
        os.unlink(tmp)
        os.rmdir(tmpdir)
        assert "((LivingEntity) event.getEntity())" not in out, out
        assert expectations[0] in out, out
        assert "import net.neoforged.neoforge.event.tick.EntityTickEvent;" not in out, out
        assert "import net.minecraft.world.entity.LivingEntity;" not in out, out
        print("ok   %s" % ("player handler" if player_only else "entity handler"))
    print("port_rewrite48 self-test passed")


if __name__ == "__main__":
    if "--selftest" in sys.argv:
        selftest()
    else:
        main()
