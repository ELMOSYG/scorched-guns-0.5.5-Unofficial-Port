"""Beam damage must carry a source position, and a fast beam must still show its block crack.

Two separate 1.21.1-shaped defects, both reported by the player on 2026-09-29, both found by
reading the *vanilla* code the mod feeds rather than the mod's own.

1. Beam knockback went in a random direction. 1.21's `DamageSource#getSourcePosition` is

       return damageSourcePosition != null ? damageSourcePosition
            : (directEntity != null ? directEntity.position() : null);

   - it reads **only the direct entity** and never the causing entity. A hitscan beam has no
   projectile, and the mod built its damage source with a null direct entity, so the position was
   null. `LivingEntity#hurt` then has neither of its two branches to fill the direction in:

       if (source.getDirectEntity() instanceof Projectile p) { ... }
       else if (source.getSourcePosition() != null) { d0 = sourcePos.x - getX(); ... }
       this.knockback(0.4F, d0, d1);            // d0 = d1 = 0.0

   and vanilla's `knockback` replaces a near-zero vector with
   `(Math.random() - Math.random()) * 0.01` - hence a random shove. The same pair also orients the
   damage-tilt flash. This was the only damage source in the mod built that way; all twenty
   projectile sites pass `this`.

2. The mining laser showed no block-breaking texture. The increment is
   `miningSpeed / (hardness * 10)`, so a fast gun finishes a soft block inside one tick:
   `cr4k_mining_laser` (14.0) does dirt at 2.8 per tick, so the first update set stage 9, sent it,
   sent the -1 that clears it, and removed the block - all in the same tick, so the crack was never
   on screen. The two slow beam guns (1.0) take 15 ticks on stone and were always fine, which is
   why it only ever showed up on the fast one. The fix defers the break until stage 9 has been sent,
   which costs one tick and does not touch the mining rate.

Rules:

  1. a hitscan damage source (null direct entity) must pass a source position,
  2. the beam mining code must not clear the progress and break the block in the same tick it
     reaches the final stage,
  3. 1.21.1's `LevelRenderer#destroyBlockProgress` treats `progress >= 10` as a removal, so the
     stage must stay clamped below it.

usage: python tools/audit_beam_knockback_and_crack.py
"""
from __future__ import annotations

import os
import re
import sys

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java", "top", "ribs", "scguns")
BEAM_HANDLER = os.path.join(SRC, "common", "BeamWeaponHandler.java")
BEAM_COMMON = os.path.join(SRC, "common", "BeamHandlerCommon.java")
DAMAGE_TYPES = os.path.join(SRC, "init", "ModDamageTypes.java")


def strip_comments(text: str) -> str:
    text = re.sub(r"/\*.*?\*/", lambda m: re.sub(r"[^\n]", " ", m.group(0)), text, flags=re.S)
    return re.sub(r"//[^\n]*", "", text)


def read(path: str) -> str:
    return strip_comments(open(path, encoding="utf-8", errors="replace").read())


def split_top_level(text: str) -> list[str]:
    """Split an argument list on commas that are not inside parentheses or brackets."""
    parts: list[str] = []
    depth = 0
    current = ""
    for char in text:
        if char in "([":
            depth += 1
        elif char in ")]":
            depth -= 1
        if char == "," and depth == 0:
            parts.append(current)
            current = ""
        else:
            current += char
    if current.strip():
        parts.append(current)
    return [p.strip() for p in parts]


def main() -> int:
    problems: list[str] = []
    handler = read(BEAM_HANDLER)
    common = read(BEAM_COMMON)
    types = read(DAMAGE_TYPES)

    # 1. every damage source built with a null direct entity must carry a position.
    # The argument list is found by counting parentheses: `player.server.registryAccess()` has one
    # of its own, so a non-greedy pattern up to the first ")" silently truncates the call and the
    # check then passes on the very code it exists to reject.
    hitscan = []
    for match in re.finditer(r"Sources\.projectile\(", handler):
        # The scan starts just after the call's own "(", so that one is already open.
        depth = 1
        start = match.end()
        for index in range(start, len(handler)):
            if handler[index] == "(":
                depth += 1
            elif handler[index] == ")":
                depth -= 1
                if depth == 0:
                    hitscan.append(handler[start:index])
                    break
    if not hitscan:
        problems.append("no beam damage source found in BeamWeaponHandler - has it moved?")
    for call in hitscan:
        args = split_top_level(call)
        # The signature is (access, directEntity, causingEntity[, position]): the *second* argument
        # is the direct entity, which is the one getSourcePosition() reads.
        direct_is_null = len(args) > 1 and args[1] == "null"
        if direct_is_null and len(args) < 4:
            problems.append("a beam damage source passes a null direct entity and no source "
                            "position: LivingEntity#hurt will call knockback(0.4, 0, 0) and vanilla "
                            "turns that into a random direction (83.10)")
    if "getLocation()" not in handler:
        problems.append("the beam damage source does not appear to be built from the hit point")

    # 2. the position-carrying factory and constructor must exist
    if "Vec3 sourcePosition" not in types and "Vec3 pSourcePosition" not in types:
        problems.append("ModDamageTypes has no way to build a damage source with an explicit "
                        "position, so the caller cannot fix 83.10")
    if "damageSourcePosition" not in types and "pSourcePosition" not in types:
        problems.append("the position-carrying constructor does not forward to DamageSource")

    # 3. the crack must survive at least one tick, and the stage must stay below 10
    # 3. the crack must survive at least one tick, and the stage must stay below 10.
    #    The test has to be on the stage *captured before* the update. A first version of the fix
    #    tested `progress.lastStage`, which the lines just above had already set to 9 - so it held on
    #    the first tick, nothing was deferred, and the player reported it still broken. Checking that
    #    the right identifier is used, and that it is captured before the assignment, is the only way
    #    a text check can catch that; a check on the mere presence of "lastStage >= 9" did not.
    stage_stmt = re.search(r"int\s+newStage\s*=\s*([^;]+);", common)
    if not stage_stmt:
        problems.append("the crack stage assignment has gone; how is the stage computed now?")
    else:
        stmt = stage_stmt.group(1)
        if "Math.min" not in stmt or not re.search(r",\s*9\s*\)\s*$", stmt):
            problems.append("the crack stage is no longer clamped to 9; 1.21.1's "
                            "LevelRenderer#destroyBlockProgress treats >= 10 as a removal: %r" % stmt)

    capture = re.search(r"int\s+(\w+)\s*=\s*progress\.lastStage\s*;", common)
    if not capture:
        problems.append("the stage is not captured before the update, so the break test reads a "
                        "lastStage that was just assigned 9 and the one-tick deferral never happens")
    else:
        name = capture.group(1)
        assign = common.find("progress.lastStage = newStage")
        if assign < 0:
            problems.append("lastStage is no longer assigned from newStage; the break test cannot "
                            "tell whether the final stage was ever sent")
        elif common.find(capture.group(0)) > assign:
            problems.append("the stage is captured *after* lastStage is assigned, which is the "
                            "exact bug of the first fix: the deferral silently does nothing")
        if not re.search(r"progress\.progress\s*>=\s*1\.0F\s*&&\s*%s\s*>=\s*9" % re.escape(name),
                         common):
            problems.append("the break test does not require the captured stage to be 9, so a fast "
                            "beam clears the crack and breaks in the same tick (83.10)")

    # 4. sanity: the real mining-speed data, so a change to it is noticed here rather than in game
    guns = os.path.join(ROOT, "src", "main", "resources", "data", "scguns", "guns")
    import json
    speeds = {}
    for name in sorted(os.listdir(guns)):
        if not name.endswith(".json"):
            continue
        general = json.load(open(os.path.join(guns, name), encoding="utf-8")).get("general", {})
        if general.get("miningSpeed"):
            speeds[name[:-5]] = general["miningSpeed"]
    print("mining guns: %s" % speeds)
    fast = [n for n, s in speeds.items() if s >= 2.0]
    if fast and not re.search(r"lastStage\s*>=\s*9", common):
        print("note: %s can finish a soft block in one tick, so the one-tick deferral matters"
              % ", ".join(fast))

    for problem in problems:
        print("BROKEN %s" % problem)
    if problems:
        print("\n%d problem(s)" % len(problems))
        return 1
    print("0 problem(s): beam damage carries a position (no random knockback) and a fast beam shows "
          "its block crack for at least a tick")
    return 0


if __name__ == "__main__":
    sys.exit(main())
