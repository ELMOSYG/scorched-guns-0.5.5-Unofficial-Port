"""The arms a gun draws must be put into their own pose, not into whatever a player animation left on them.

Player Animator (the library behind Better Combat's and Iron's Spells' player animations) animates the *shared*
player model. Its `AnimationApplier.updatePart` writes `x/y/z`, `xRot/yRot/zRot`, `xScale/yScale/zScale`, and -
through bendylib - a per-cuboid bend (`IBendHelper.bend(part, side, amount)`). This mod draws its arms by
calling `ModelPart.render` on that same model, and in first person nothing re-runs `setupAnim` for the local
player, so after an attack animation the arm stayed exactly as the animation left it: attack, switch to a gun,
and the arms render stretched or bent. Zeroing position and rotation is not enough - the scale and the bend are
not in those fields.

That is why the fix has three parts, and why this audit checks all three:

  1. every part drawn (both arms *and* both sleeves) goes through the reset,
  2. the reset is complete: baked pose (scale included) + the gun's pivot + the animated bend,
  3. the bend clears through the optional compat helper, which checks the mod id first and caches its
     reflective handles - the renderer itself must never name a `dev.kosmx` type, and nothing may look a class
     up per frame.

usage: python tools/audit_arm_render_reset.py
"""
from __future__ import annotations

import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "src", "main", "java", "top", "ribs", "scguns")
RENDERER = os.path.join(SRC, "client", "render", "gun", "animated", "AnimatedGunRenderer.java")
COMPAT = os.path.join(SRC, "client", "compat", "PlayerAnimatorCompat.java")

ARM_PARTS = ("rightArm", "rightSleeve", "leftArm", "leftSleeve")
BEND_API = "dev.kosmx.playerAnim.impl.animation.IBendHelper"


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
            problems.append("%s is rendered without a resetArmPose, so a leftover animation pose shows up in "
                            "the gun's arms" % part)
        elif render and reset and reset.start() > render.start():
            problems.append("%s is reset after it is rendered" % part)
    if not re.search(r"playerEntityModel\.rightArm\.render\(", text):
        problems.append("the right arm is no longer drawn at all - did this file change shape?")

    # 2. the reset is complete
    body = method_body(text, "private void resetArmPose(")
    if not body:
        problems.append("AnimatedGunRenderer has no resetArmPose helper")
    else:
        if "resetPose()" not in body:
            problems.append("resetArmPose does not call resetPose(), so an animation's leftover scale "
                            "(xScale/yScale/zScale) survives and the arm renders stretched")
        if "setPos(" not in body:
            problems.append("resetArmPose does not apply the gun's pivot")
        if "PlayerAnimatorCompat.resetBend(" not in body:
            problems.append("resetArmPose does not clear the animated bend, which does not live in the model "
                            "part's fields")
    if "dev.kosmx" in text:
        problems.append("the renderer names a Player Animator type directly; that class would fail to load "
                        "for players without the mod")

    # 3. the optional compat helper
    if not os.path.isfile(COMPAT):
        problems.append("client/compat/PlayerAnimatorCompat.java is missing")
    else:
        compat = strip_comments(open(COMPAT, encoding="utf-8", errors="replace").read())
        if "playeranimator" not in compat:
            problems.append("the compat helper does not name the playeranimator mod id")
        if BEND_API not in compat:
            problems.append("the compat helper does not reach the bend API")
        if "Class.forName" not in compat:
            problems.append("the compat helper does not resolve the bend API reflectively")
        if not re.search(r"private static Method\s+\w+;", compat):
            problems.append("the compat helper does not cache its reflective Method, so it would look it up "
                            "on every arm, every frame")
        reset = method_body(compat, "public static void resetBend(")
        if not reset:
            problems.append("the compat helper has no resetBend")
        elif "isLoaded()" not in reset:
            # `unusable` alone is not enough: it is false until a failure happens, so the first call would
            # still reflect on a client that has no Player Animator at all.
            problems.append("resetBend does not gate on the mod being loaded, so it would reflect on a client "
                            "that does not have Player Animator")

    print("=== gun arm pose ===")
    print("  parts reset before drawing            %s"
          % ", ".join("%s:%s" % (p, "yes" if re.search(r"resetArmPose\(playerEntityModel\.%s," % p, text) else "NO")
                      for p in ARM_PARTS))
    print("  reset covers pose / pivot / bend      %s"
          % ("yes" if body and "resetPose()" in body and "setPos(" in body
             and "PlayerAnimatorCompat.resetBend(" in body else "NO"))
    print("")
    if problems:
        for problem in problems:
            print("BROKEN %s" % problem)
        print("\n%d problem(s)" % len(problems))
        return 1
    print("0 problem(s): the gun's arms are drawn in this mod's own pose, whatever a player animation left behind")
    return 0


if __name__ == "__main__":
    sys.exit(main())
