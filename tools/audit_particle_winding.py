"""Which way does each custom particle quad face? A quad facing away is culled, so it never appears.

This is the audit for the fault behind "blood at the hit point never shows, only the blood on the
ground" (PORTING_STATUS section 82.39). Both of the mod's hand-written particle quads were handed to
the buffer clockwise, which puts the front face on -Z (or -Y for the bullet hole's XZ quad). 0.5.5
got away with it, and so did this port until now, only because the particle pass happened not to cull
what it drew; the droplets on the ground were visible because landing turns the quad flat and that
same -Z face ends up pointing up, while the airborne ones were turned to face the camera and were
culled. Vanilla's own quad is counter-clockwise.

The facing is recomputed here from the corner list and the emission order in the source - the signed
area of the four corners in the plane (shoelace), whose sign gives the front face - and then checked
against what that orientation is for, which was measured with the game's own classes rather than
assumed:

  * BloodParticle, camera-facing (airborne, or rolled): the camera's rotation is the one vanilla uses
    for its own counter-clockwise quad, which is visible, so this one must be counter-clockwise too.
  * BloodParticle, flat (landed): rotated by Direction.NORTH.getRotation(). A counter-clockwise quad
    there points its front face at (0, -1, 0) - into the ground, which is why the fix keeps the
    clockwise order here; the clockwise one gives (0, 1, 0), straight up, and the player sees it.
  * BulletHoleParticle: rotated by the hit face's getRotation(); clockwise puts the front face inside
    the block for every face (up gives (0, -1, 0), north gives (0, 0, 1)), reversed it lands on the
    face that was hit.

usage: python tools/audit_particle_winding.py
"""
from __future__ import annotations

import os
import re
import sys

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
PARTICLE_DIR = os.path.join(ROOT, "src", "main", "java", "top", "ribs", "scguns", "client", "particle")
BLOOD = os.path.join(PARTICLE_DIR, "BloodParticle.java")
HOLE = os.path.join(PARTICLE_DIR, "BulletHoleParticle.java")


def strip_comments(src: str) -> str:
    """Remove comments, string-literal aware, so a `/*` inside a literal cannot hide code."""
    out, i, n = [], 0, len(src)
    while i < n:
        c = src[i]
        if c == '"':
            out.append(c)
            i += 1
            while i < n and src[i] != '"':
                if src[i] == "\\":
                    out.append(src[i])
                    i += 1
                if i < n:
                    out.append(src[i])
                    i += 1
            if i < n:
                out.append('"')
                i += 1
        elif src.startswith("//", i):
            while i < n and src[i] != "\n":
                i += 1
        elif src.startswith("/*", i):
            i += 2
            while i < n and not src.startswith("*/", i):
                i += 1
            i += 2
        else:
            out.append(c)
            i += 1
    return "".join(out)


def corners(text: str, name: str) -> list:
    """The corner list of the `name` array, as (x, y, z) tuples."""
    hit = re.search(name + r"\s*=\s*new\s+Vector3f\[\]\s*\{(.*?)\};", text, re.S)
    if not hit:
        return []
    return [tuple(float(v) for v in m) for m in
            re.findall(r"new\s+Vector3f\(\s*(-?[\d.]+)F\s*,\s*(-?[\d.]+)F\s*,\s*(-?[\d.]+)F\s*\)",
                       hit.group(1))]


def winding(corner_list: list, order: list, axes: tuple) -> float:
    """Signed area of the emitted quad in the given plane: positive is counter-clockwise."""
    i, j = axes
    area = 0.0
    for k in range(4):
        a = corner_list[order[k]]
        b = corner_list[order[(k + 1) % 4]]
        area += a[i] * b[j] - b[i] * a[j]
    return area


def emitted_order(text: str, array_name: str) -> list:
    """The order the corners are actually written to the buffer, from the addVertex calls."""
    order = []
    for hit in re.finditer(array_name + r"\[(\d+)\]\.x\(\)", text):
        order.append(int(hit.group(1)))
    return order


def main() -> int:
    blood = strip_comments(open(BLOOD, encoding="utf-8", errors="replace").read())
    hole = strip_comments(open(HOLE, encoding="utf-8", errors="replace").read())
    problems: list[str] = []

    # --- blood: the choice has to be tied to whether the quad is turned to face the camera --------
    selection = re.search(r"int\[\]\s+order\s*=\s*(\w+)\s*\?\s*new\s+int\[\]\s*\{([\d,\s]+)\}\s*:\s*"
                          r"new\s+int\[\]\s*\{([\d,\s]+)\}", blood)
    if not selection:
        problems.append("BloodParticle no longer chooses its emission order from whether the quad is "
                        "camera-facing, so nothing here can tell which way it faces")
    else:
        flag = selection.group(1)
        facing_order = [int(v) for v in selection.group(2).replace(" ", "").split(",") if v]
        flat_order = [int(v) for v in selection.group(3).replace(" ", "").split(",") if v]
        if sorted(facing_order) != [0, 1, 2, 3] or sorted(flat_order) != [0, 1, 2, 3]:
            problems.append("the blood emission orders are not permutations of the four corners: %s / %s"
                            % (facing_order, flat_order))
        # The flag must be set where the camera's rotation is used, not somewhere unrelated.
        if not re.search(r"boolean\s+%s\s*=" % flag, blood):
            problems.append("the blood's %s flag is not declared where the rotation is chosen" % flag)
        if not re.search(r"%s\s*=\s*true;\s*\}\s*\}\s*else" % flag, blood):
            problems.append("the blood no longer marks the camera-rotation branch as camera-facing, so "
                            "the reversed order may be applied to the flat quad instead")
        elif "renderInfo.rotation()" not in blood:
            problems.append("the blood no longer uses the camera's rotation for airborne droplets")

        blood_corners = corners(blood, r"Vector3f\[\]\s+vertices")
        if len(blood_corners) != 4:
            problems.append("BloodParticle's four corners could not be read (%d found)" % len(blood_corners))
        else:
            facing_area = winding(blood_corners, facing_order, (0, 1))
            flat_area = winding(blood_corners, flat_order, (0, 1))
            if facing_area <= 0:
                problems.append("the camera-facing blood quad is clockwise, so its front face points "
                                "away from the viewer and the whole airborne burst is culled - which is "
                                "the fault this audit exists for")
            if flat_area >= 0:
                problems.append("the flat blood quad is counter-clockwise, so a landed droplet's front "
                                "face points into the ground (measured: (0, -1, 0)) and it vanishes")
        if not re.search(r"for\s*\(\s*int\s+\w+\s*:\s*order\s*\)", blood):
            problems.append("the blood no longer emits the corners through the chosen order")

    # --- bullet hole: its quad lives in the XZ plane and must face out of the block ---------------
    hole_corners = corners(hole, r"Vector3f\[\]\s+points")
    hole_order = emitted_order(hole, "points")
    if len(hole_corners) != 4:
        problems.append("BulletHoleParticle's four corners could not be read (%d found)" % len(hole_corners))
    elif sorted(hole_order) != [0, 1, 2, 3]:
        problems.append("BulletHoleParticle emits its corners as %s, not all four once" % hole_order)
    else:
        area = winding(hole_corners, hole_order, (0, 2))
        if area <= 0:
            problems.append("the bullet hole's quad is still clockwise in its own plane, so its front "
                            "face points into the block (measured: up gives (0, -1, 0), north gives "
                            "(0, 0, 1)) and the hole is culled from the side you are on")
        if "direction.getRotation()" not in hole:
            problems.append("the bullet hole no longer orients itself by the face it hit, so the "
                            "orientation this check reasons about is gone")

    for problem in problems:
        print("BROKEN %s" % problem)
    if problems:
        print("\n%d problem(s)" % len(problems))
        return 1
    print("0 problem(s): every hand-written particle quad faces the side it is meant to be seen from")
    return 0


if __name__ == "__main__":
    sys.exit(main())
