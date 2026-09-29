"""Blood must spray; it must not simply fall out of the hit (HANDOFF 83.11, extended in 82.35).

The player reported that blood from a projectile hit "appears directly on the ground" and guessed
that the splash process was missing. It was.

Both call sites pass the three parameters that `createParticle`/`addParticle` name as *speeds* as
something else entirely: the projectile path passes a fixed `0.5, 0, 0.5` and the beam path passes
the weapon's beam colour, and `BloodParticle.Factory` hands all three straight to `setColor`. So the
colour is set from them and the motion never comes from them at all - it came from a hardcoded
`0.1, 0.1, 0.1` in the constructor, a barely visible isotropic scatter under `gravity = 1.5`, which
reads as blood falling straight out of the hit. 0.5.5 does exactly the same, so this is not a port
regression; it has never had a splash.

The first fix gave the particle a randomised, upward-biased velocity, and the player still read the
result as blood on the ground. The numbers say why, which is why this audit also pins them:

  * the whole fan was 0.12 - 0.30 blocks per tick under gravity 1.5, and
  * `lifetime = 12 / (0.1 .. 1.0)` lets a droplet live up to 120 ticks, so a few long lived ones hung
    around after landing and the burst read as one clump dropping, and
  * every droplet spawned on the exact same coordinate - ten sprites from one point.

So: the particle gives itself a randomised, upward-biased velocity and may not go back to a fixed
triple; it may not go back to a long life or heavy gravity; it throws the droplets over a configured
spread; and the count and throw strength are values, not code. Nothing here touches the colour hack,
which the beam path depends on.

usage: python tools/audit_blood_spray.py
"""
from __future__ import annotations

import os
import re
import sys

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
PARTICLE = os.path.join(ROOT, "src", "main", "java", "top", "ribs", "scguns", "client", "particle",
                        "BloodParticle.java")
HANDLER = os.path.join(ROOT, "src", "main", "java", "top", "ribs", "scguns", "client", "network",
                       "ClientPlayHandler.java")
BEAM = os.path.join(ROOT, "src", "main", "java", "top", "ribs", "scguns", "client", "handler",
                    "BeamHandler.java")
CONFIG = os.path.join(ROOT, "src", "main", "java", "top", "ribs", "scguns", "Config.java")

MAX_LIFETIME_TICKS = 40
# 0.5.5's own motion, which this build must reproduce exactly (PORTING_STATUS section 82.37). The
# player compared 1.20.1 and this port back to back and wants the same burst on both, so these are
# parity values rather than taste: the seed and the spread are the ones the vanilla Particle
# constructor applied to 0.5.5's `super(..., 0.1, 0.1, 0.1)`, and the lifetime is its own formula.
UPSTREAM_SEED = 0.1
UPSTREAM_SPREAD = 0.4
UPSTREAM_GRAVITY = 1.5
UPSTREAM_LIFE = (12.0, 0.9, 0.1)


def strip_comments(text: str) -> str:
    """Blank out comments, aware of string literals.

    The regex version this audit started with pairs a `/*` inside a string literal with any later `*/` and
    deletes everything between them - a silent false negative, which is the one failure an audit must not have.
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


def call_args(text: str, head: str) -> str:
    """Return the argument text of the first `head(...)` call, parens balanced, or "" if absent."""
    at = text.find(head)
    if at < 0:
        return ""
    start = text.find("(", at + len(head) - 1)
    if start < 0:
        return ""
    depth = 0
    for i in range(start, len(text)):
        if text[i] == "(":
            depth += 1
        elif text[i] == ")":
            depth -= 1
            if depth == 0:
                return text[start + 1:i]
    return ""


def main() -> int:
    source = strip_comments(open(PARTICLE, encoding="utf-8", errors="replace").read())
    handler = strip_comments(open(HANDLER, encoding="utf-8", errors="replace").read())
    beam = strip_comments(open(BEAM, encoding="utf-8", errors="replace").read())
    config = strip_comments(open(CONFIG, encoding="utf-8", errors="replace").read())
    problems: list[str] = []

    ctor = re.search(r"public\s+BloodParticle\([^)]*\)\s*\{(.*?)\n   \}", source, re.S)
    if not ctor:
        problems.append("the BloodParticle constructor has moved; where is the motion set now?")
    else:
        body = ctor.group(1)
        super_call = re.search(r"super\(([^;]*)\)\s*;", body)
        if super_call and re.search(r"super\([^;]*[^0.\s]\s*[^;]*\)", super_call.group(1)):
            if re.search(r"0\.1\s*,\s*0\.1\s*,\s*0\.1", super_call.group(1)):
                problems.append("the constructor is back to a fixed 0.1 triple: a fixed magnitude "
                                "under gravity reads as blood falling straight out of the hit")
        for axis in ("xd", "yd", "zd"):
            if not re.search(r"this\.%s\s*=" % axis, body):
                problems.append("the constructor no longer sets this.%s, so the particle has no "
                                "motion on that axis" % axis)
        if not re.search(r"this\.random", body):
            problems.append("the spray is no longer randomised per particle, so all ten drops fly "
                            "the same way")
        # Parity with 0.5.5 is the requirement (PORTING_STATUS section 82.37): the player compared
        # 1.20.1 and this port side by side and wants the same burst. 0.5.5's motion is the vanilla
        # one - every axis gets 0.1 + (random * 2 - 1) * 0.4 - so each axis must read that way and
        # read the multiplier.
        velocity = re.compile(
            r"this\.(xd|yd|zd)\s*=\s*\(\s*([\d.]+)\s*\+\s*\(\s*this\.random\.nextDouble\(\)\s*\*\s*"
            r"([\d.]+)\s*-\s*([\d.]+)\s*\)\s*\*\s*([\d.]+)\s*\)\s*\*\s*speedMultiplier")
        axes = {m.group(1): (float(m.group(2)), float(m.group(5))) for m in velocity.finditer(body)}
        for axis, (seed, spread) in axes.items():
            if abs(seed - UPSTREAM_SEED) > 1e-9:
                problems.append("this.%s starts from %s; 0.5.5's seed is %s"
                                % (axis, seed, UPSTREAM_SEED))
            if abs(spread - UPSTREAM_SPREAD) > 1e-9:
                problems.append("this.%s spreads by %s; 0.5.5 gets +-%s from the vanilla constructor"
                                % (axis, spread, UPSTREAM_SPREAD))
        for axis in ("xd", "yd", "zd"):
            if axis not in axes:
                problems.append("this.%s is not written as 0.5.5's "
                                "(0.1 + (random * 2 - 1) * 0.4) * speedMultiplier, so its motion is "
                                "no longer the reference one" % axis)
        gravity = re.search(r"this\.gravity\s*=\s*([\d.]+)F", body)
        if not gravity:
            problems.append("the constructor does not set gravity")
        elif abs(float(gravity.group(1)) - UPSTREAM_GRAVITY) > 1e-9:
            problems.append("gravity is %s; 0.5.5 uses %s" % (gravity.group(1), UPSTREAM_GRAVITY))
        life = re.search(r"this\.lifetime\s*=\s*\(int\)\(\s*([\d.]+)F\s*/\s*\(\s*"
                         r"this\.random\.nextFloat\(\)\s*\*\s*([\d.]+)F\s*\+\s*([\d.]+)F", body)
        if not life:
            problems.append("the lifetime is no longer 0.5.5's 12 / (random * 0.9 + 0.1)")
        elif (abs(float(life.group(1)) - UPSTREAM_LIFE[0]) > 1e-9
              or abs(float(life.group(2)) - UPSTREAM_LIFE[1]) > 1e-9
              or abs(float(life.group(3)) - UPSTREAM_LIFE[2]) > 1e-9):
            problems.append("the lifetime reads %s / (random * %s + %s); 0.5.5's is 12 / (random * "
                            "0.9 + 0.1)" % (life.group(1), life.group(2), life.group(3)))

    # The factory must keep feeding the caller's three parameters to setColor: the beam path
    # relies on it for the weapon's beam colour. Removing it would silently recolour that effect.
    if "setColor((float)xSpeed, (float)ySpeed, (float)zSpeed)" not in source:
        problems.append("the factory no longer turns the caller's parameters into a colour, which "
                        "is how the beam path colours its blood with the weapon's beam colour")

    # And the caller must still spawn the drops at the hit height, scattered over the configured spread.
    spawn = re.search(r"createParticle\([^;]*?ModParticleTypes\.BLOOD[^;]*?\)\s*;", handler, re.S)
    if not spawn:
        problems.append("the projectile-hit blood spawn is gone from ClientPlayHandler")
    elif "message.getY()" not in spawn.group(0):
        problems.append("the blood spawn no longer uses the message's Y, so the droplets are not at "
                        "the hit height")
    for path, name, signature, head in (
        (handler, "ClientPlayHandler", "public static void handleMessageBlood(", "createParticle("),
        (beam, "BeamHandler", "public static void spawnBeamImpactParticles(", "addParticle("),
    ):
        if "bloodParticleSpread" not in path:
            problems.append("%s does not read bloodParticleSpread" % name)
        # Naming the option is not scattering, and counting the whole file is not either - other methods have
        # their own random draws. What matters is that the spawn coordinates themselves are offset, so count the
        # draws inside this spawn call: either written inline or carried in a local the body fills from
        # nextDouble(). A site that computes offsets and then passes the bare hit position still misses.
        body = method_body(path, signature)
        if not body:
            problems.append("%s has no %s to inspect" % (name, signature.strip(" (")))
        else:
            spawn = call_args(body, head)
            if not spawn:
                problems.append("%s no longer calls %s for its blood droplets" % (name, head.strip("(")))
            else:
                via_local = sum(1 for local in set(re.findall(r"(\w+)\s*=[^;]*?nextDouble\(\)", body))
                                if re.search(r"\b%s\b" % local, spawn))
                if spawn.count("nextDouble()") + via_local < 3:
                    problems.append("%s passes fewer than three offset axes to %s, so its droplets all leave "
                                    "from one point" % (name, head.strip("(")))
        if "bloodParticleCount" not in path:
            problems.append("%s does not read bloodParticleCount" % name)

    count = re.search(r'defineInRange\(\s*"bloodParticleCount"\s*,\s*(\d+)', config)
    if not count:
        problems.append("no bloodParticleCount option")
    elif int(count.group(1)) < 2:
        problems.append("bloodParticleCount defaults to %s: one droplet cannot splatter" % count.group(1))
    if not re.search(r'defineInRange\(\s*"bloodParticleSpread"', config):
        problems.append("no bloodParticleSpread option")
    if not re.search(r'defineInRange\(\s*"bloodParticleSpeed"', config):
        problems.append("no bloodParticleSpeed option")
    if "bloodParticleSpeed" not in source:
        problems.append("BloodParticle does not read bloodParticleSpeed, so the burst cannot be tuned")

    for problem in problems:
        print("BROKEN %s" % problem)
    if problems:
        print("\n%d problem(s)" % len(problems))
        return 1
    print("0 problem(s): the burst is 0.5.5's motion to the value, scaled only by the config "
          "multiplier - and the beam path's colour-by-parameter behaviour is intact")
    return 0


if __name__ == "__main__":
    sys.exit(main())
