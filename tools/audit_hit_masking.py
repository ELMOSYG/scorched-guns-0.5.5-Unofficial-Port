"""Hit feedback has to be audible over the gunshot (HANDOFF 82.28).

The player's own diagnosis: the headshot sound was there, but the gunshot drowned it. That is temporal and
spectral masking, and the numbers explain why "just add more sounds" was never going to help:

  * playing a hit sound in the same tick as the shot is the whole problem, and
  * the mod's fire sounds are long - measured over 45 of them, the median is about 1.0 s and the longest
    (a carbine) is 3.77 s - and they are broadband.

The gunshot's volume is not the issue: `GunModifierHelper.getFireSoundVolume` starts at 1.0 and only
attachments change it, so it is exactly as loud as the hit sound used to be. A short, quiet click at the same
level as a long bang loses. The fix raises three things at once - a short delay, a pitch above 1.0 (so the
click's energy sits above the blast's band) and a volume above 1.0 - and it uses the mod's own 0.106 s click.

Rules:

  1. hit sounds are scheduled, never played in the packet handler itself (which runs in the shot's tick),
  2. the queue is drained every client tick and is bounded, so it cannot grow without limit,
  3. the default delay, pitch and volume are set so the click is not merely equal to what drowns it,
  4. the configured hit sound really exists - a shipped asset needs both a sounds.json entry and the .ogg.

usage: python tools/audit_hit_feedback.py
"""
from __future__ import annotations

import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "src", "main", "java", "top", "ribs", "scguns")
HANDLER = os.path.join(SRC, "client", "network", "ClientPlayHandler.java")
CLIENT = os.path.join(SRC, "client", "ClientHandler.java")
CONFIG = os.path.join(SRC, "Config.java")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "scguns")
SOUNDS_DIR = os.path.join(ASSETS, "sounds")

MIN_PITCH = 1.5
MIN_VOLUME = 1.5
MIN_DELAY = 1


def strip_comments(text: str) -> str:
    """Blank out comments, aware of string literals.

    A `/*` inside a string is not a comment: this codebase has config comments naming file globs such as
    `data/scguns/entity/equipment/*.json`, and a stripper that ignores string literals treats that `/*` as a
    block comment start and deletes the code after it. That reads as "the thing does not exist" - a false
    negative, which is the one failure an audit must not have.
    """
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
    client = strip_comments(open(CLIENT, encoding="utf-8", errors="replace").read())
    config = strip_comments(open(CONFIG, encoding="utf-8", errors="replace").read())

    # 1. the packet handler must not play the sound itself - that runs in the shot's tick
    receive = method_body(handler, "public static void handleProjectileHitEntity(")
    if not receive:
        problems.append("ClientPlayHandler has no handleProjectileHitEntity")
    else:
        if "scheduleHitSound(" not in receive:
            problems.append("the hit sound is not scheduled, so it plays in the same tick as the gunshot - "
                            "which is exactly what made it inaudible")
        if re.search(r"getSoundManager\(\)\.play\(", receive):
            problems.append("handleProjectileHitEntity plays the sound directly instead of queueing it")

    schedule = method_body(handler, "private static void scheduleHitSound(")
    if not schedule:
        problems.append("no scheduleHitSound helper")
    elif "hitSoundDelayTicks" not in schedule:
        problems.append("the delay is not read from config, so the masking fix cannot be tuned")

    # 2. drained every client tick, and bounded
    tick = method_body(handler, "public static void tickHitSounds(")
    if not tick:
        problems.append("no tickHitSounds drain")
    else:
        if "pendingHitSounds" not in tick:
            problems.append("tickHitSounds does not drain the queue")
        if "dueTick" not in tick:
            problems.append("tickHitSounds does not compare the due tick")
        if "level == null" not in tick:
            problems.append("tickHitSounds does not clear the queue when there is no level")
    bound = re.search(r"private static final int MAX_PENDING_HIT_SOUNDS\s*=\s*(\d+)", handler)
    if not bound:
        # A field that exists but holds Integer.MAX_VALUE is not a bound: check the number, not the name.
        problems.append("the pending-hit-sound queue has no small numeric bound")
    elif not 0 < int(bound.group(1)) <= 64:
        problems.append("MAX_PENDING_HIT_SOUNDS is %s: that does not bound anything" % bound.group(1))
    if "tickHitSounds()" not in client:
        problems.append("nothing calls tickHitSounds, so queued hit sounds would never play")

    # 3. the defaults have to beat the masking, not match it
    delay = re.search(r'defineInRange\(\s*"hitSoundDelayTicks"\s*,\s*(\d+)', config)
    volume = re.search(r'defineInRange\(\s*"hitSoundVolume"\s*,\s*([\d.]+)', config)
    pitch = re.search(r'defineInRange\(\s*"hitSoundPitch"\s*,\s*([\d.]+)', config)
    if not delay or int(delay.group(1)) < MIN_DELAY:
        problems.append("hitSoundDelayTicks defaults to 0 or is missing: the click would land inside the "
                        "muzzle blast's onset")
    if not volume or float(volume.group(1)) < MIN_VOLUME:
        problems.append("hitSoundVolume defaults to at most 1.0, which is exactly as loud as the gunshot it "
                        "has to be heard over")
    if not pitch or float(pitch.group(1)) < MIN_PITCH:
        problems.append("hitSoundPitch defaults to at most 1.0, so the click stays inside the gunshot's "
                        "frequency band")

    # 4. the configured hit sound must exist
    headshot = re.search(r'\.define\(\s*"headshotSound"\s*,\s*"([^"]*)"\s*\)', config)
    if not headshot:
        problems.append("Config has no headshotSound default")
    else:
        value = headshot.group(1)
        if not value.startswith("minecraft:"):
            namespace, _, path = value.partition(":")
            if namespace != "scguns":
                problems.append("the headshot sound default %s belongs to another namespace" % value)
            else:
                sounds_json = open(os.path.join(ASSETS, "sounds.json"), encoding="utf-8", errors="replace").read()
                entry = re.compile(r'"%s"\s*:\s*\{(.*?)\n  \}' % re.escape(path), re.S).search(sounds_json)
                if not entry:
                    problems.append('sounds.json has no "%s", so the configured sound cannot play' % path)
                else:
                    name = re.search(r'"name"\s*:\s*"scguns:([^"]+)"', entry.group(1))
                    if not name:
                        problems.append('the sounds.json entry for "%s" names no file' % path)
                    elif not os.path.isfile(os.path.join(SOUNDS_DIR, name.group(1) + ".ogg")):
                        problems.append("the .ogg behind %s is not in the tree" % path)

    print("=== hit feedback vs the gunshot ===")
    print("  scheduled, not played in the shot tick  %s"
          % ("yes" if receive and "scheduleHitSound(" in receive else "NO"))
    print("  drained per client tick                 %s"
          % ("yes" if tick and "tickHitSounds()" in client else "NO"))
    print("  delay / volume / pitch defaults         %s / %s / %s"
          % (delay.group(1) if delay else "-", volume.group(1) if volume else "-", pitch.group(1) if pitch else "-"))
    print("  headshot sound                          %s" % (headshot.group(1) if headshot else "MISSING"))
    print("")
    if problems:
        for problem in problems:
            print("BROKEN %s" % problem)
        print("\n%d problem(s)" % len(problems))
        return 1
    print("0 problem(s): the hit click is offset from the shot, louder than it, and above its frequency band")
    return 0


if __name__ == "__main__":
    sys.exit(main())
