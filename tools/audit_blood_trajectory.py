"""Does the blood burst actually read as a spray? Measured, by replaying the vanilla tick.

The player reported that the droplets "appear directly on the ground" with "no falling process", and
this is the audit that decides that question with numbers instead of opinion. It reads the motion
constants out of BloodParticle.java, replays 1.21.1's own particle tick, and reports what a player
can see: how far the droplets rise above the wound, how long they are airborne, and how far they
travel.

The tick being replayed, taken from the merged 1.21.1 jar rather than from memory:

  Particle.<init>(level, x, y, z, xs, ys, zs):
      xd = xs + (Math.random() * 2 - 1) * 0.4          // vanilla spreads every axis by +-0.4
  Particle.tick():
      if (age++ >= lifetime) remove();
      else { yd -= 0.04 * gravity; move(xd, yd, zd);
             xd *= friction; yd *= friction; zd *= friction;      // friction is 0.98
             if (onGround) { xd *= 0.7; zd *= 0.7; } }
  BloodParticle.tick():
      super.tick(); if (onGround) { xd = 0; zd = 0; quadSize *= 0.95F; }

The last line matters most: a droplet that lands is frozen and shrinks, so everything the player
sees after that is a stain. "Falling" is therefore only visible if the droplet is still in the air
long enough - which is exactly what upstream fails at.

Upstream (0.5.5, the official 1.21.1 1.5.2, and every other port - their particle is identical):
    rises 0.10 blocks, airborne 5.5 ticks, travels 1.5 blocks.
    A horizontal squirt that is on the ground in a quarter of a second.

So the rule is: the burst must rise at least RISE blocks above the wound and stay airborne at least
AIRBORNE ticks, and the upstream numbers must FAIL both thresholds - if they ever stop failing, this
audit has stopped measuring anything.

usage: python tools/audit_blood_trajectory.py [--detail] [--height 1.2]
"""
from __future__ import annotations

import argparse
import math
import os
import random
import re
import sys

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
PARTICLE = os.path.join(ROOT, "src", "main", "java", "top", "ribs", "scguns", "client", "particle",
                        "BloodParticle.java")
CONFIG = os.path.join(ROOT, "src", "main", "java", "top", "ribs", "scguns", "Config.java")

# What the burst has to do to be worth calling a spray, at the shipped default speed.
MIN_RISE = 0.6           # blocks above the wound
MIN_AIRBORNE = 14        # ticks in the air, out of 20 per second
MIN_TRAVEL = 0.5         # blocks outwards; below this it is a puff, not a splatter
MAX_TRAVEL = 5.0         # above this the blood flies across the room

FRICTION = 0.98
GROUND_FRICTION = 0.7
HALF_SIZE = 0.1
GRAVITY_PER_TICK = 0.04
TICKS = 80


def strip_comments(src: str) -> str:
    """Remove comments, string-literal aware.

    A `/*` inside a string literal otherwise swallows real code and the audit goes quiet.
    """
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


def read_constants(particle: str, config: str) -> dict:
    """The motion numbers as written in the source, so the audit cannot drift away from them."""
    body = strip_comments(open(particle, encoding="utf-8", errors="replace").read())
    cfg = strip_comments(open(config, encoding="utf-8", errors="replace").read())
    found = {}
    patterns = {
        "gravity": r"this\.gravity\s*=\s*([\d.]+)F",
        "life_min": r"this\.lifetime\s*=\s*(\d+)\s*\+\s*this\.random\.nextInt\(",
        "life_span": r"this\.lifetime\s*=\s*\d+\s*\+\s*this\.random\.nextInt\((\d+)\)",
        "horizontal_lo": r"double\s+spread\s*=\s*\(([\d.]+)\s*\+\s*this\.random\.nextDouble\(\)",
        "horizontal_span": r"double\s+spread\s*=\s*\(\s*[\d.]+\s*\+\s*this\.random\.nextDouble\(\)"
                           r"\s*\*\s*([\d.]+)\s*\)",
        "upward_lo": r"this\.yd\s*=\s*\(([\d.]+)\s*\+\s*this\.random\.nextDouble\(\)",
        "upward_span": r"this\.yd\s*=\s*\(\s*[\d.]+\s*\+\s*this\.random\.nextDouble\(\)"
                       r"\s*\*\s*([\d.]+)\s*\)",
        "speed_default": r'defineInRange\(\s*"bloodParticleSpeed"\s*,\s*([\d.]+)',
        "count_default": r'defineInRange\(\s*"bloodParticleCount"\s*,\s*(\d+)',
    }
    for key, pattern in patterns.items():
        hit = re.search(pattern, body if key not in ("speed_default", "count_default") else cfg)
        found[key] = float(hit.group(1)) if hit else None
    return found


def simulate(gravity: float, lifetime: int, vx: float, vy: float, vz: float, height: float) -> dict:
    """Replay one droplet; report what a player can see of it."""
    x, y, z = 0.0, height, 0.0
    xd, yd, zd = vx, vy, vz
    peak, travelled, airborne, landed_at = y, 0.0, 0, None
    path = []
    for age in range(TICKS):
        if age >= lifetime:
            break
        xo, zo = x, z
        yd -= GRAVITY_PER_TICK * gravity
        nx, ny, nz = x + xd, y + yd, z + zd
        on_ground = False
        if ny - HALF_SIZE < 0.0:
            ny = HALF_SIZE
            on_ground = yd < 0.0
        x, y, z = nx, ny, nz
        xd, yd, zd = xd * FRICTION, yd * FRICTION, zd * FRICTION
        if on_ground:
            xd *= GROUND_FRICTION
            zd *= GROUND_FRICTION
            if landed_at is None:
                landed_at = age
        else:
            airborne += 1
        travelled += math.hypot(x - xo, z - zo)
        peak = max(peak, y)
        path.append((age, y, math.hypot(x, z), on_ground))
    return {"path": path, "peak": peak, "airborne": airborne, "landed_at": landed_at,
            "travelled": travelled, "lifetime": lifetime}


def burst(gravity, life_min, life_span, h_lo, h_span, u_lo, u_span, speed, count, height,
          seed: int = 7) -> list:
    rng = random.Random(seed)
    rows = []
    for _ in range(count):
        angle = rng.random() * math.tau
        radius = (h_lo + rng.random() * h_span) * speed
        rows.append(simulate(gravity, int(life_min) + rng.randrange(int(life_span)),
                             math.cos(angle) * radius,
                             (u_lo + rng.random() * u_span) * speed, math.sin(angle) * radius,
                             height))
    return rows


def burst_upstream(count: int, height: float, seed: int = 7) -> list:
    """0.5.5's own burst: super(..., 0.1, 0.1, 0.1) plus the vanilla per-axis +-0.4 spread.

    Every axis gets its own independent draw, so this is not a fan, and the life is the real
    12 / (random * 0.9 + 0.1) rather than a flat range. The official 1.21.1 1.5.2 and every other
    port ship this exact constructor.
    """
    rng = random.Random(seed)
    rows = []
    for _ in range(count):
        rows.append(simulate(1.5, int(12.0 / (rng.random() * 0.9 + 0.1)),
                             0.1 + (rng.random() * 2 - 1) * 0.4,
                             0.1 + (rng.random() * 2 - 1) * 0.4,
                             0.1 + (rng.random() * 2 - 1) * 0.4, height))
    return rows


def measure(rows: list, height: float, count: int) -> dict:
    n = len(rows)
    return {
        "rise": sum(max(0.0, r["peak"] - height) for r in rows) / n,
        "airborne": sum(r["airborne"] for r in rows) / n,
        "travel": sum(r["travelled"] for r in rows) / n,
        "still_airborne": sum(1 for r in rows if r["landed_at"] is None),
        "count": count,
    }


def line(label: str, stats: dict) -> str:
    return ("%-30s rise %5.2f blocks   airborne %5.1f ticks   travel %5.2f blocks   "
            "in the air when it expired: %d/%d"
            % (label, stats["rise"], stats["airborne"], stats["travel"],
               stats["still_airborne"], stats["count"]))


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--detail", action="store_true", help="print one droplet tick by tick")
    parser.add_argument("--height", type=float, default=1.2,
                        help="how far above the ground the wound is (a mob's torso)")
    args = parser.parse_args()
    height = args.height

    values = read_constants(PARTICLE, CONFIG)
    missing = [k for k, v in values.items() if v is None]
    if missing:
        print("BROKEN the particle no longer has a readable value for: %s" % ", ".join(missing))
        return 1

    count = int(values["count_default"])
    speed = values["speed_default"]
    now = measure(burst(values["gravity"], values["life_min"], values["life_span"],
                        values["horizontal_lo"], values["horizontal_span"],
                        values["upward_lo"], values["upward_span"], speed, count, height),
                  height, count)
    # Upstream: 0.5.5's constructor, which the official 1.21.1 1.5.2 and every other port also ship.
    upstream = measure(burst_upstream(10, height), height, 10)

    print("the wound is %.2f blocks above the ground; a landed droplet is frozen and shrinks\n" % height)
    print(line("upstream (0.5.5, 1.5.2, ...)", upstream))
    print(line("this build, speed %.1f (default)" % speed, now))
    for other in (0.5, 2.0):
        print(line("this build, speed %.1f" % other,
                   measure(burst(values["gravity"], values["life_min"], values["life_span"],
                                 values["horizontal_lo"], values["horizontal_span"],
                                 values["upward_lo"], values["upward_span"], other, count, height),
                           height, count)))
    print("")

    problems = []
    if now["rise"] < MIN_RISE:
        problems.append("the droplets rise only %.2f blocks above the wound (want %.2f): there is "
                        "nothing to watch fall" % (now["rise"], MIN_RISE))
    if now["airborne"] < MIN_AIRBORNE:
        problems.append("a droplet is airborne for only %.1f ticks (want %d): it is on the ground "
                        "before the eye can follow it" % (now["airborne"], MIN_AIRBORNE))
    if not MIN_TRAVEL <= now["travel"] <= MAX_TRAVEL:
        problems.append("the droplets travel %.2f blocks (want %.2f - %.2f)"
                        % (now["travel"], MIN_TRAVEL, MAX_TRAVEL))
    if now["still_airborne"] >= count:
        problems.append("no droplet ever lands while it is still visible, so the burst never shows "
                        "blood coming down onto the ground")
    # The thresholds have to be able to fail: upstream must miss both of them.
    if upstream["rise"] >= MIN_RISE or upstream["airborne"] >= MIN_AIRBORNE:
        problems.append("upstream now passes this audit's own thresholds, so it has stopped "
                        "measuring the difference")

    if args.detail:
        print("one droplet of this build, at the default speed (age, height, distance, state):")
        row = simulate(values["gravity"], int(values["life_min"]) + 6,
                       (values["horizontal_lo"] + values["horizontal_span"] * 0.5),
                       (values["upward_lo"] + values["upward_span"] * 0.5),
                       (values["horizontal_lo"] + values["horizontal_span"] * 0.5), height)
        for age, y, dist, ground in row["path"]:
            print("   tick %2d   height %.3f   distance %.3f %s"
                  % (age, y, dist, "LANDED" if ground else ""))
        print("")

    for problem in problems:
        print("BROKEN %s" % problem)
    if problems:
        print("\n%d problem(s)" % len(problems))
        return 1
    print("0 problem(s): the burst rises, hangs long enough to be seen falling, and still lands "
          "near the target")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
