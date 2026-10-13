#!/usr/bin/env python3
"""Rank Scorched Guns weapons by damage per second, base or fully kitted.

`Projectile#damage` in a gun JSON is the damage of the whole shot: a gun with more than one
pellet splits it (`ProjectileEntity` divides by `Projectile#getProjectileAmount`), so DPS is
`damage * (20 / rate)` no matter how many pellets there are. Multiplying by the pellet count
double-counts and makes a shotgun look like it does 4000 DPS.

Base numbers come straight from the JSON. `--attach` additionally applies attachment modifiers
parsed out of `GunModifiers.java`, which is where the fire-rate modifiers live - including the
flat `rate - 1` step for slow guns that makes fully kitted fast weapons pull ahead.

Examples
--------
  python tools/gun_dps.py --top 15
  python tools/gun_dps.py --weapon lmg --top 10
  python tools/gun_dps.py --jar <addon>.jar --top 10
  python tools/gun_dps.py --id stigg,earths_corpse --attach bump_stock
  python tools/gun_dps.py --measured scguns:earths_corpse=1200,20.6
"""

import argparse
import glob
import json
import os
import re
import sys
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA = os.path.join(ROOT, "src", "main", "resources", "data", "scguns", "guns")
MODIFIERS_JAVA = os.path.join(ROOT, "src", "main", "java", "top", "ribs", "scguns", "common", "GunModifiers.java")
ITEMS_JAVA = os.path.join(ROOT, "src", "main", "java", "top", "ribs", "scguns", "init", "ModItems.java")


# --------------------------------------------------------------------------- gun loading

def gun_from_json(gid, blob):
    try:
        g = json.loads(blob)
    except Exception:
        return None
    gen, pr = g.get("general") or {}, g.get("projectile") or {}
    dmg = pr.get("damage")
    rate = gen.get("rate")
    if not dmg or not rate:
        return None
    return {
        "id": gid,
        "damage": float(dmg),
        "rate": int(rate),
        "pellets": int(gen.get("projectileAmount", pr.get("projectileAmount", 1)) or 1),
        "mode": gen.get("fireMode"),
        "weapon": gen.get("weaponType"),
        "falloff_start": pr.get("damageFalloffStart"),
        "falloff_end": pr.get("damageFalloffEnd"),
    }


def load_guns(data_dir, jars):
    guns = {}
    if data_dir and os.path.isdir(data_dir):
        for path in sorted(glob.glob(os.path.join(data_dir, "*.json"))):
            with open(path, encoding="utf-8-sig") as fh:
                gun = gun_from_json(os.path.basename(path)[:-5], fh.read())
            if gun:
                guns["scguns:" + gun["id"]] = gun
    for jar in jars:
        with zipfile.ZipFile(jar) as z:
            for name in z.namelist():
                m = re.match(r"data/([^/]+)/guns/([^/]+)\.json$", name)
                if not m:
                    continue
                gun = gun_from_json(m.group(2), z.read(name).decode("utf-8-sig"))
                if gun:
                    guns["%s:%s" % (m.group(1), gun["id"])] = gun
    return guns


# ----------------------------------------------------------------- modifiers from the Java source

BLOCK = re.compile(r"public static final IGunModifier ([A-Z0-9_]+) = new IGunModifier\(\) \{(.*?)\n\s*\};", re.S)
SIG = re.compile(r"public\s+[\w<>\[\], .]+\s+(\w+)\s*\(([^)]*)\)\s*\{")
SAFE = {"max": max, "min": min, "int": int, "abs": abs, "ceil": lambda v: int(-(-v // 1)), "floor": lambda v: int(v // 1)}
UNPARSED = []


def _methods(block):
    """(name, params, body) for every method in an IGunModifier body, matched by braces.

    A regex cannot do this: the bodies contain nested braces, and a non-greedy match stops at the
    first closing brace it meets, which silently truncated `modifyFireRate` into something that
    would not even compile - it was reported as "applied" while changing nothing.
    """
    out = []
    pos = 0
    while True:
        m = SIG.search(block, pos)
        if not m:
            return out
        depth, i = 1, m.end()
        while i < len(block) and depth:
            if block[i] == "{":
                depth += 1
            elif block[i] == "}":
                depth -= 1
            i += 1
        out.append((m.group(1), m.group(2), block[m.end():i - 1]))
        pos = i


def _ternary(expr):
    """Java `cond ? a : b` -> Python `(a if cond else b)`, including nested ternaries.

    Without this the bodies do not parse, exec raises, and the modifier is silently recorded as
    None - which is how `bump_stock` first appeared to be applied while changing nothing at all.
    """
    depth = 0
    q = None
    for i, ch in enumerate(expr):
        if ch in "([":
            depth += 1
        elif ch in ")]":
            depth -= 1
        elif ch == "?" and depth == 0:
            q = i
            break
    if q is None:
        return expr
    depth = 0
    for j in range(q + 1, len(expr)):
        ch = expr[j]
        if ch in "([":
            depth += 1
        elif ch in ")]":
            depth -= 1
        elif ch == ":" and depth == 0:
            return "(%s if %s else %s)" % (_ternary(expr[q + 1:j]), _ternary(expr[:q]), _ternary(expr[j + 1:]))
    return expr


def _to_python(body):
    lines = []
    for line in body.splitlines():
        line = _ternary(line) if "?" in line else line
        line = re.sub(r"\bMath\.max\b", "max", line)
        line = re.sub(r"\bMath\.min\b", "min", line)
        line = re.sub(r"\bMath\.ceil\b", "ceil", line)
        line = re.sub(r"\bMath\.floor\b", "floor", line)
        line = re.sub(r"\(int\)|\(float\)|\(double\)|\(long\)", "", line)
        line = re.sub(r"(\d+(?:\.\d+)?)[FfDdLl]\b", r"\1", line)
        lines.append(line)
    return "\n".join(lines)


def parse_modifiers(path=MODIFIERS_JAVA):
    """Turn the simple IGunModifier bodies into callables.

    Anything that does not reduce to plain arithmetic is recorded as None instead of guessed at.
    """
    mods = {}
    if not os.path.exists(path):
        return mods
    text = open(path, encoding="utf-8").read()
    for name, block in BLOCK.findall(text):
        funcs = {}
        for mname, params, body in _methods(block):
            args = [p.strip().split()[-1] for p in params.split(",") if p.strip()]
            src = "def _f(%s):\n" % ", ".join(args)
            src += "".join("    " + line.strip() + "\n" for line in _to_python(body).strip().splitlines())
            env = dict(SAFE)
            env["__builtins__"] = {}
            try:
                exec(src, env)
                funcs[mname] = env["_f"]
            except Exception:
                funcs[mname] = None
                UNPARSED.append("%s.%s" % (name, mname))
        mods[name] = funcs
    return mods


def parse_attachment_map(path=ITEMS_JAVA):
    """item id -> [modifier names] taken from the register(...) calls."""
    items = {}
    if not os.path.exists(path):
        return items
    text = open(path, encoding="utf-8").read()
    for m in re.finditer(r'REGISTER\.register\(\s*"([a-z0-9_]+)"\s*,\s*\(\)\s*->(.*?)\n\s*\);', text, re.S):
        items[m.group(1)] = re.findall(r"GunModifiers\.([A-Z0-9_]+)", m.group(2))
    return items


def apply_attachments(gun, names, mods, item_map):
    dmg, rate = gun["damage"], gun["rate"]
    applied, unknown = [], []
    for name in names:
        for mod_name in (item_map.get(name) or [name.upper() + "_MODIFIER", name.upper()]):
            fns = mods.get(mod_name)
            if not fns:
                continue
            applied.append(mod_name)
            if fns.get("modifyFireRate"):
                rate = fns["modifyFireRate"](rate)
            if fns.get("modifyProjectileDamage"):
                dmg = fns["modifyProjectileDamage"](dmg)
            break
        else:
            unknown.append(name)
    return dmg, rate, applied, unknown


# --------------------------------------------------------------------------- reporting

def dps(damage, rate):
    return damage * (20.0 / rate) if rate > 0 else 0.0


def row(gun, kitted=None):
    damage, rate = kitted if kitted else (gun["damage"], gun["rate"])
    falloff = "none" if not gun["falloff_start"] and not gun["falloff_end"] else "%g~%g" % (
        gun["falloff_start"] or 0, gun["falloff_end"] or 0)
    return "   %-34s dmg=%-6.1f rate=%-3d rpm=%-5.0f pellets=%-3d falloff=%-9s dps=%6.1f" % (
        gun["id"], damage, rate, 1200.0 / rate, gun["pellets"], falloff, dps(damage, rate))


def main():
    ap = argparse.ArgumentParser(description="Scorched Guns DPS report")
    ap.add_argument("--data", default=DATA, help="directory of gun JSONs (default: the mod's own)")
    ap.add_argument("--jar", action="append", default=[], help="jar to read guns from (repeatable)")
    ap.add_argument("--top", type=int, default=0, help="print only the N strongest")
    ap.add_argument("--id", help="comma separated gun ids to show")
    ap.add_argument("--weapon", help="only guns whose weaponType contains this")
    ap.add_argument("--attach", help="comma separated attachment item ids to apply")
    ap.add_argument("--measured", action="append", default=[],
                    help="id=rpm,perShot as measured in game; reports the implied multipliers")
    ap.add_argument("--dump-attachments", action="store_true", help="list the parsed attachment -> modifier map")
    args = ap.parse_args()

    mods = parse_modifiers()
    item_map = parse_attachment_map()
    if args.dump_attachments:
        print("attachment id -> modifiers (%d parsed from ModItems, %d modifier bodies from GunModifiers)\n"
              % (len(item_map), len(mods)))
        for key in sorted(item_map):
            print("   %-28s %s" % (key, ", ".join(item_map[key]) or "(none)"))
        if UNPARSED:
            print("\n   WARNING: %d modifier methods could not be translated to Python and are treated as"
                  " no-ops, so any attachment using them will be reported unchanged:\n      %s"
                  % (len(UNPARSED), "\n      ".join(sorted(UNPARSED))))
        return 0

    guns = load_guns(args.data, args.jar)
    if not guns:
        print("no guns found", file=sys.stderr)
        return 1

    chosen = list(guns.values())
    if args.id:
        wanted = {"scguns:" + i if ":" not in i else i for i in args.id.split(",")}
        chosen = [g for k, g in guns.items() if k in wanted or g["id"] in args.id.split(",")]
    if args.weapon:
        chosen = [g for g in chosen if (g["weapon"] or "").endswith(args.weapon)]

    attach = [a.strip() for a in args.attach.split(",")] if args.attach else []
    kitted = {}
    unknown = set()
    for gun in chosen:
        kitted[gun["id"]] = apply_attachments(gun, attach, mods, item_map)
        unknown |= set(kitted[gun["id"]][3])

    chosen.sort(key=lambda g: dps(*kitted[g["id"]][:2]), reverse=True)
    if args.top:
        chosen = chosen[: args.top]

    print("%d guns from %s%s%s\n" % (len(guns), args.data, "" if not args.jar else " + %d jar(s)" % len(args.jar),
                                     "" if not attach else ", kitted with " + ", ".join(attach)))
    for gun in chosen:
        print(row(gun))
        if attach:
            dmg, rate, applied, _ = kitted[gun["id"]]
            if applied:
                print("      -> %s  =>  dmg=%.1f (x%.2f)  rate=%d (x%.2f)  rpm=%.0f" % (
                    ", ".join(applied), dmg, dmg / gun["damage"], rate, gun["rate"] / rate, 1200.0 / rate))
    if unknown:
        print("\n   could not resolve these attachment ids: %s" % ", ".join(sorted(unknown)))

    if args.measured:
        print("\nmeasured reports:")
        for spec in args.measured:
            try:
                gid, rest = spec.split("=")
                rpm, per = (float(x) for x in rest.split(","))
            except Exception:
                print("   bad --measured value: %s" % spec)
                continue
            gun = guns.get(gid) or guns.get("scguns:" + gid)
            print("   %-30s rpm=%-6.0f perShot=%-6.2f  =>  dps=%.1f" % (gid, rpm, per, rpm / 60.0 * per))
            if gun:
                print("   %-30s base dmg=%.1f rate=%d => rpm=%.0f dps=%.1f   implied: dmg x%.2f, rpm x%.2f, dps x%.2f"
                      % ("", gun["damage"], gun["rate"], 1200.0 / gun["rate"], dps(gun["damage"], gun["rate"]),
                         per / gun["damage"], rpm / (1200.0 / gun["rate"]),
                         (rpm / 60.0 * per) / dps(gun["damage"], gun["rate"])))
    return 0


if __name__ == "__main__":
    sys.exit(main())
