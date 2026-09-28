"""A shot has to tell you it landed (HANDOFF 82.27).

0.5.5's hit feedback had three holes. A plain body hit on a mob played **no sound at all** - only a hit marker,
which is the hit that happens most often. The headshot sound was `entity.player.attack.knockback`, a soft
swing whoosh that does not read as a hit. And nothing was rate limited: a shotgun sends one hit packet per
pellet, so one trigger pull produced up to 26 simultaneous UI sounds, which clip into noise instead of landing
as a hit.

There is also a trap worth pinning: `scguns:item.ping.ping` looks exactly like a hit confirmation and is
registered, but it is a reload sound taken from another mod (the player identified it), so wiring it into the
headshot feedback would be both wrong-sounding and a borrowed asset used more prominently than before.

Rules:

  1. every hit category has a sound: an ordinary mob hit must not be silent again,
  2. a headshot is layered - an impact plus a short confirmation - and the confirmation must not be the mod's
     borrowed ping asset,
  3. hits are rate limited per category per tick, one sound per category per tick,
  4. every hit sound plays at the listener (`SimpleSoundInstance.forUI`) with a configurable volume, so the
     distance to the target does not change how loud the shooter's own feedback is.

usage: python tools/audit_hit_feedback.py
"""
from __future__ import annotations

import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "src", "main", "java", "top", "ribs", "scguns")
HANDLER = os.path.join(SRC, "client", "network", "ClientPlayHandler.java")
CONFIG = os.path.join(SRC, "Config.java")

CATEGORIES = ("Impact", "Critical", "Headshot")
VOLUME_KEYS = ("impactSoundVolume", "criticalSoundVolume", "headshotSoundVolume", "headshotConfirmVolume")
BORROWED_ASSET = "scguns:item.ping.ping"


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

    handler = strip_comments(open(HANDLER, encoding="utf-8", errors="replace").read())
    config = open(CONFIG, encoding="utf-8", errors="replace").read()
    # Config is checked on its code, not its prose: this audit's own subject is explained in a comment there
    # (the borrowed asset is named precisely because it must NOT be used), and a raw substring search would
    # read that explanation as a violation.
    config_code = strip_comments(config)
    feedback = method_body(handler, "private static void playHitFeedback(")

    # 1. no silent category
    if not feedback:
        problems.append("ClientPlayHandler has no playHitFeedback")
    else:
        impact = re.search(r"playSoundWhenImpact", feedback)
        if not impact:
            problems.append("an ordinary mob hit has no gate of its own, so it can go silent again - it was the "
                            "only silent category in 0.5.5 and the most common hit")
        if "impactSound" not in feedback:
            problems.append("the impact category does not use a configurable sound")

    # 2. headshot is layered, and not with the borrowed asset
    if feedback:
        if "headshotSound" not in feedback or "headshotConfirmSound" not in feedback:
            problems.append("a headshot is no longer a layered sound (impact + confirmation)")
        if "playConfirmWhenHeadshot" not in feedback:
            # Naming the confirm sound is not enough: the layer has to be behind its own switch, or disabling
            # it (or a stray edit to the branch) removes the headshot's second half silently.
            problems.append("the headshot confirmation is not gated by its own config switch, so it can be "
                            "turned off (or edited away) without the gate noticing")
        if BORROWED_ASSET in handler:
            problems.append("the borrowed %s asset is wired into the hit feedback; it is a reload sound from "
                            "another mod, not a hit confirmation" % BORROWED_ASSET)
    # The asset may be *named* in config prose (that is how a maintainer learns why it is unused), but it must
    # never be a value: check the define call that would carry it, not a bare substring.
    confirm_default = re.search(r'\.define\(\s*"headshotConfirmSound"\s*,\s*"([^"]*)"\s*\)', config_code)
    if confirm_default and confirm_default.group(1) == BORROWED_ASSET:
        problems.append("the borrowed %s asset is the headshot confirmation default" % BORROWED_ASSET)

    # 3. per-category, per-tick rate limiting
    for category in CATEGORIES:
        field = "last%sSoundTick" % category
        if not re.search(r"private static long %s\s*=" % field, handler):
            problems.append("no %s field: a shotgun burst would play one sound per pellet and clip" % field)
        elif feedback and re.search(r"tick == %s" % field, feedback) is None:
            problems.append("%s is never compared against the current tick, so it does not rate limit" % field)

    # 4. listener-side playback with configurable volumes
    helper = method_body(handler, "private static void playHitSound(")
    if not helper:
        problems.append("no playHitSound helper")
    elif "SimpleSoundInstance.forUI(" not in helper:
        problems.append("hit sounds do not go through SimpleSoundInstance.forUI, so they would be attenuated by "
                        "the distance to whatever was hit")
    if feedback and re.search(r"forUI\(", feedback):
        problems.append("playHitFeedback plays a sound directly instead of through the shared helper")
    for stray in ("playLocalSound(", "world.playSound(", "level.playSound("):
        if stray in feedback:
            problems.append("playHitFeedback uses %s, which is positional and therefore distance dependent"
                            % stray)
    for key in VOLUME_KEYS:
        if key not in feedback:
            problems.append("the volume of %s is not configurable at the call site" % key)
        # The field name surviving in Config is not proof the option exists: a rename takes the *define key*
        # with it and leaves the player without a knob, so require the quoted key itself.
        if '"%s"' % key not in config_code:
            problems.append('Config does not define the option "%s" as written here' % key)
    # a volume of 0 must be honoured as "off" rather than played silently
    if helper and "volume <= 0.0F" not in helper:
        problems.append("playHitSound does not skip a zero volume")

    print("=== hit feedback ===")
    print("  categories                            %s"
          % ", ".join("%s:%s" % (c, "yes" if re.search("last%sSoundTick" % c, handler) else "NO")
                      for c in CATEGORIES))
    print("  playback                              %s"
          % ("listener-side forUI" if helper and "forUI(" in helper else "WRONG"))
    print("  borrowed ping asset                   %s"
          % ("not used as a value" if not (confirm_default and confirm_default.group(1) == BORROWED_ASSET)
             and BORROWED_ASSET not in handler else "USED"))
    print("")
    if problems:
        for problem in problems:
            print("BROKEN %s" % problem)
        print("\n%d problem(s)" % len(problems))
        return 1
    print("0 problem(s): every hit category lands, once per tick, at the shooter's ear")
    return 0


if __name__ == "__main__":
    sys.exit(main())
