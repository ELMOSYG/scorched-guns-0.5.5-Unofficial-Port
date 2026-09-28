"""A bayonet charge must not be ended by losing the sprint flag (HANDOFF 82.23).

Sprinting starts a charge and scales its damage, so it looks natural to *sustain* the charge on the sprint flag
too - and 0.5.5 did exactly that, in two places:

  * a 50 ms scheduled task that called `stopBanzai()` on `!player.isSprinting()` before it ever called
    `handleBanzaiMode`, and
  * `handleBanzaiMode` itself, with a knockback grace period meant to cover the charge's own wall impact.

The flag is dropped by vanilla in situations a charge walks straight into: every block collision
(`LocalPlayer.aiStep`: `horizontalCollision && !minorHorizontalCollision`), water, and an empty food bar
(`!hasEnoughFoodToStartSprinting()`), and for players who sprint by double-tapping W it only returns on a fresh
double tap (`sprintTriggerTime = 7`). So a charge ended at the first wall, and because the scheduled task ran
first, `handleBanzaiMode`'s grace period could never be reached at all - it was dead code.

The rules below keep the fix honest:

  1. The scheduled ticker must not cancel on sprinting; that decision belongs to `handleBanzaiMode`.
  2. `handleBanzaiMode` must tolerate a lost flag (tolerance constant + helper), not stop on it.
  3. The wall impact must be evaluated before the sustain check, since that impact is what kills the flag.
  4. The knockback grace must outlast vanilla's own 7-tick sprint re-acquire delay.
  5. Tags and distances go through their constants, the way 0.5.5 wrote them.
  6. One ticker per charge: cancel the previous one instead of stacking a new task per charge.

usage: python tools/audit_banzai_charge.py
"""
from __future__ import annotations

import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "src", "main", "java", "top", "ribs", "scguns")
PACKET = os.path.join(SRC, "network", "message", "C2SMessageMeleeAttack.java")
HANDLER = os.path.join(SRC, "client", "handler", "MeleeAttackHandler.java")
VANILLA_SPRINT_RETRIGGER_TICKS = 7


def strip_comments(text: str) -> str:
    """Remove // and /* */ comments with a state machine. A lazy block-comment regex is not safe here: these
    files contain comments mentioning paths like `.../*.json`, and such a pattern swallows real code between
    two of them."""
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
    """The braces-balanced body of the first method whose declaration contains `signature`."""
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
    problems = []

    packet = strip_comments(open(PACKET, encoding="utf-8", errors="replace").read())
    handler = strip_comments(open(HANDLER, encoding="utf-8", errors="replace").read())

    # 1. The ticker may not cancel on the sprint flag. The entry check ("a charge starts from a sprint") stays:
    #    it is outside the scheduled lambda, so look only at what follows the scheduling call.
    at = packet.find("scheduleAtFixedRate")
    ticker = packet[at:packet.find("\n   }", at)] if at >= 0 else ""
    if "isSprinting" in ticker:
        problems.append("the banzai ticker checks the sprint flag again, so it cancels the charge before "
                        "handleBanzaiMode - and its knockback grace period - can ever run")
    if "handleBanzaiMode" not in ticker:
        problems.append("the banzai ticker no longer drives handleBanzaiMode")
    if "isSprinting" not in packet:
        problems.append("a charge no longer requires a sprint to start")

    # 6. One ticker per charge.
    if "banzaiTask" not in packet or ticker.count(".cancel(") == 0:
        problems.append("the ticker is not cancelled, so every charge leaves another repeating task behind")
    if packet.count("cancel(") < 2:
        problems.append("a new charge does not cancel the previous ticker first")

    # 2. handleBanzaiMode tolerates a lost flag.
    body = method_body(handler, "handleBanzaiMode")
    if not body:
        problems.append("MeleeAttackHandler has no handleBanzaiMode")
    else:
        if "keepCharging(" not in body:
            problems.append("handleBanzaiMode does not use keepCharging, so a lost sprint flag still ends the "
                            "charge")
        if re.search(r"!\s*player\.isSprinting\(\)", body):
            problems.append("handleBanzaiMode stops on a bare !isSprinting() again")
    if "BANZAI_SPRINT_LOST_TOLERANCE_TICKS" not in handler or "BANZAI_SPRINT_LOST_TAG" not in handler:
        problems.append("the sprint-loss tolerance is gone")
    if "BANZAI_MIN_FORWARD_SPEED" not in handler or "horizontalForwardSpeed(" not in handler:
        problems.append("there is no forward-motion test, so a charge cannot tell 'sprint flag lost' from "
                        "'player stopped running'")

    # 3. The wall impact must be evaluated before the sustain check.
    if body:
        wall = body.find("checkForWallCollision(")
        sustain = body.find("keepCharging(")
        if wall < 0:
            problems.append("handleBanzaiMode no longer reacts to a wall impact")
        elif sustain < 0 or wall > sustain:
            problems.append("the sustain check runs before the wall impact, so the charge's own knockback - "
                            "which is what drops the sprint flag - ends it")

    # 4. The grace period must outlast vanilla's own sprint re-acquire delay.
    match = re.search(r"KNOCKBACK_GRACE_PERIOD_TICKS\s*=\s*(\d+)", handler)
    if not match:
        problems.append("no named knockback grace period")
    elif int(match.group(1)) <= VANILLA_SPRINT_RETRIGGER_TICKS:
        problems.append("KNOCKBACK_GRACE_PERIOD_TICKS is %s, which is not longer than vanilla's %d-tick "
                        "sprintTriggerTime, so a wall bump still ends the charge"
                        % (match.group(1), VANILLA_SPRINT_RETRIGGER_TICKS))

    # 5. Tags and distances through their constants.
    for literal in ("KnockbackGracePeriod", "WallCollisionCooldown", "BanzaiDamageCooldown", "BanzaiSprintLostAt"):
        if handler.count('"%s"' % literal) != 1:
            problems.append('the tag "%s" is used as a literal outside its constant' % literal)
    if "scale(1.0)" in handler and "WALL_CHECK_DISTANCE" in handler:
        problems.append("the wall check distance is inlined instead of using WALL_CHECK_DISTANCE")

    print("=== charge lifecycle ===")
    print("  entry requires a sprint            %s" % ("yes" if "isSprinting" in packet else "NO"))
    print("  ticker re-checks the sprint flag   %s" % ("YES (wrong)" if "isSprinting" in ticker else "no"))
    print("  sustain decision                   %s" % ("handleBanzaiMode: wall -> keepCharging -> damage"
                                                        if body else "MISSING"))
    print("  knockback grace                    %s ticks" % (match.group(1) if match else "MISSING"))
    print("")
    if problems:
        for problem in problems:
            print("BROKEN %s" % problem)
        print("\n%d problem(s)" % len(problems))
        return 1
    print("0 problem(s): a charge starts from a sprint and ends when the player stops running")
    return 0


if __name__ == "__main__":
    sys.exit(main())
