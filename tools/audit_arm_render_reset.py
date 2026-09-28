"""The arms a gun draws must come from a model this mod owns, in this mod's own pose.

This is the third time the same symptom was reported - Better Combat, Iron's Spells, and a resource pack that
changes the player's animations (the Entity Model Features kind). All three are the same bug: the gun drew its
first-person arms from `EntityRenderDispatcher.getRenderer(player).getModel()`, which is precisely the model
instance that every player-animation feature animates. In first person nothing re-runs `setupAnim` for the
local player, so whatever the last attack or locomotion animation left on the arms was still there the next
time a gun drew them - stretched, bent or twisted arms.

Zeroing fields is a patch per animation system: Player Animator writes position, rotation, scale and a
per-cuboid bend (bendylib), a pack may keep its own state, and anything applied from a `ModelPart.render` hook
cannot be cleared by touching fields at all. So the arms are drawn from a `PlayerModel` this mod bakes and
caches itself - one per skin variant, chosen exactly as vanilla chooses it. Nobody else holds a reference, so
nothing can animate it, and the whole class of bug is gone rather than one instance of it.

Rules:

  1. every part drawn (both arms *and* both sleeves) is reset to the gun's pose before it is rendered,
  2. the reset is the baked pose plus the gun's pivot,
  3. the arm model is baked by this mod (both skin variants) and never taken from the player renderer,
  4. no player-animation library is named here - the point is not to be coupled to one.

usage: python tools/audit_arm_render_reset.py
"""
from __future__ import annotations

import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "src", "main", "java", "top", "ribs", "scguns")
RENDERER = os.path.join(SRC, "client", "render", "gun", "animated", "AnimatedGunRenderer.java")

ARM_PARTS = ("rightArm", "rightSleeve", "leftArm", "leftSleeve")
# Borrowing the player renderer's model is the bug; naming one of these back is how it comes back.
BORROWED_MODEL = ("getEntityRenderDispatcher(", ".getModel()")


def strip_comments(text: str) -> str:
    out = []
    i = 0
    n = len(text)
    while i < n:
        c = text[i]
        if c == "/" and i + 1 < n and text[i + 1] == "/":
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


def method_body(text: str, signature: str) -> str:
    at = text.find(signature)
    if at < 0:
        return ""
    start = text.find("{", at)
    if start < 0:
        return ""
    depth = 0
    for i in range(start, len(text)):
        if text[i] == "{":
            depth += 1
        elif text[i] == "}":
            depth -= 1
            if depth == 0:
                return text[start:i + 1]
    return ""


def main() -> int:
    problems: list[str] = []

    text = strip_comments(open(RENDERER, encoding="utf-8", errors="replace").read())

    # 1. every drawn part is reset first
    for part in ARM_PARTS:
        render = re.search(r"playerEntityModel\.%s\.render\(" % part, text)
        reset = re.search(r"resetArmPose\(playerEntityModel\.%s," % part, text)
        if render and not reset:
            problems.append("%s is rendered without a resetArmPose, so a leftover pose shows up in the gun's "
                            "arms" % part)
        elif render and reset and reset.start() > render.start():
            problems.append("%s is reset after it is rendered" % part)
    if not re.search(r"playerEntityModel\.rightArm\.render\(", text):
        problems.append("the right arm is no longer drawn at all - did this file change shape?")

    # 2. the reset itself
    body = method_body(text, "private void resetArmPose(")
    if not body:
        problems.append("AnimatedGunRenderer has no resetArmPose helper")
    else:
        if "resetPose()" not in body:
            problems.append("resetArmPose does not call resetPose(), so a part can be left with scale or "
                            "rotation from an earlier draw")
        if "setPos(" not in body:
            problems.append("resetArmPose does not apply the gun's pivot")

    # 3. own the model, never borrow the animated one
    for needle, why in (
        ("bakeLayer(", "the arm model is not baked by this mod"),
        ("ModelLayers.PLAYER", "the wide player layer is not used"),
        ("ModelLayers.PLAYER_SLIM", "the slim player layer is missing, so slim players would get wide arms"),
        ("PlayerSkin.Model.SLIM", "the skin variant is no longer read, so the wrong mesh can be picked"),
        ("armModel(", "there is no accessor for this mod's own arm model"),
    ):
        if needle not in text:
            problems.append("%s (%s)" % (why, needle))
    for needle in BORROWED_MODEL:
        if needle in text:
            problems.append("the renderer takes %s again: that is the player renderer's own model, the very "
                            "instance every player animation animates" % needle)
    arms = method_body(text, "private void renderPlayerArms(")
    if arms and "armModel(" not in arms:
        problems.append("renderPlayerArms does not draw from this mod's own model")

    # 4. no coupling to one animation library
    if "dev.kosmx" in text:
        problems.append("the renderer names a player animation library type; owning the model is what keeps "
                        "this independent of any of them")

    print("=== gun arm pose ===")
    print("  parts reset before drawing            %s"
          % ", ".join("%s:%s" % (p, "yes" if re.search(r"resetArmPose\(playerEntityModel\.%s," % p, text) else "NO")
                      for p in ARM_PARTS))
    print("  arm model                             %s"
          % ("this mod's own (wide + slim)" if "bakeLayer(" in text else "BORROWED"))
    print("")
    if problems:
        for problem in problems:
            print("BROKEN %s" % problem)
        print("\n%d problem(s)" % len(problems))
        return 1
    print("0 problem(s): the gun's arms come from a model only this mod can touch")
    return 0


if __name__ == "__main__":
    sys.exit(main())
