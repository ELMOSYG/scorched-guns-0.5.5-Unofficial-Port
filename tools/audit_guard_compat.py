"""The Guard Villagers integration must not be able to break a server that does not have that mod.

Guard Villagers is optional: most players do not have it, and a compat that names one of its classes from
host code fails at *link* time on their server (`NoClassDefFoundError` when the instruction is first
executed), which is a crash at spawn time, not a missing feature. HANDOFF section 82 wires the
integration the other way round, and this audit pins that shape:

  1. `tallestegg.guardvillagers` appears in exactly **one** file, `GuardFriendlyRules`, which is reached
     only behind `GuardVillagersCompat.isGuard(...)` - a registry-id comparison that cannot be true
     without the mod. Everything else - the equip hook, the gun goal, the projectile filter, the damage
     handler - is guard-class-free and therefore safe to load and call on any server.
  2. Guards are recognised by the same id that the data file uses, so
     `data/scguns/entity/gunner_mobs.json` really does hand them a gun: the constant and the JSON key must
     agree, or the compat silently does nothing.
  3. The equip hook is reached: `GunnerMobSpawner.onEntityJoinWorld` calls it, the guard's own goal counts
     as "has gun AI" (otherwise `reassessWeaponGoal` would also give a guard the hostile raider AI), and a
     guard's shots are filtered out of both projectile hit searches.
  4. The optional dependency is declared where players can see it, and the guard accuracy is configurable.

Usage:
    python tools/audit_guard_compat.py
    python tools/audit_guard_compat.py --selftest   # must FAIL against the pre-compat revision
"""

import json
import pathlib
import re
import subprocess
import sys

SOURCE = pathlib.Path("src/main/java")
PACKAGE = SOURCE / "top/ribs/scguns"
COMPAT = PACKAGE / "compat/guardvillagers"
SPAWNER = PACKAGE / "config/GunnerMobSpawner.java"
PROJECTILE = PACKAGE / "entity/projectile/ProjectileEntity.java"
CONFIG = PACKAGE / "Config.java"
GUNNER_JSON = pathlib.Path("src/main/resources/data/scguns/entity/gunner_mobs.json")
MODS_TOML = pathlib.Path("src/main/templates/META-INF/neoforge.mods.toml")
MIXIN_CONFIG = pathlib.Path("src/main/resources/scguns.mixins.json")

RELATIVE_PACKAGE = "src/main/java/top/ribs/scguns"
GUARD_PACKAGE = "tallestegg.guardvillagers"
GUARD_ID = "guardvillagers:guard"

# The revision this was written against: HANDOFF section 81, before the guard compat existed.
PRE_FIX_REVISION = "8863bb4"


def strip_comments(text):
    out = []
    i = 0
    n = len(text)
    while i < n:
        c = text[i]
        if c == '"':
            out.append(c)
            i += 1
            while i < n:
                if text[i] == "\\":
                    out.append(text[i:i + 2])
                    i += 2
                    continue
                out.append(text[i])
                if text[i] == '"':
                    i += 1
                    break
                i += 1
        elif c == "/" and i + 1 < n and text[i + 1] == "/":
            while i < n and text[i] != "\n":
                i += 1
        elif c == "/" and i + 1 < n and text[i + 1] == "*":
            i += 2
            while i + 1 < n and not (text[i] == "*" and text[i + 1] == "/"):
                if text[i] == "\n":
                    out.append("\n")
                i += 1
            i += 2
        else:
            out.append(c)
            i += 1
    return "".join(out)
