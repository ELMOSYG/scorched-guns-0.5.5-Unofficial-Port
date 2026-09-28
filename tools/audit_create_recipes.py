"""Keep the Create-family integration recipes valid for the versions we ship against (HANDOFF 82.21).

Create 6.0 (1.21.1) and Create Addition 1.7.1 do **not** spell their parameters the way Create 0.5.x did, and
none of this fails loudly:

  * an unrecognised key is simply ignored. Our milling recipe asked for `processingTime` where Create reads
    `processing_time`, so the recipe loaded with a processing time of 0 - and Create's millstone only rolls
    its results inside the branch that runs while `timer > 0` (MillstoneBlockEntity: `ifle` past the
    decrement, with `MillingRecipe.rollResults` inside it), so a zero-length recipe spins and **never
    produces anything**. That is "the sulfur chunk cannot be milled into sulfur dust".
  * the same drift had turned `heat_requirement` into `heatRequirement` (a superheated mixing recipe then
    ran at any temperature) and `max_charge_rate`/`results` into `maxChargeRate`/`result`, which made every
    createaddition:charging recipe fail to decode outright.

Checks:

  1. Every recipe under a `create*` integration folder must gate itself on the mod that owns its type, and
     that id must match the type's namespace. A recipe whose type mod is absent is then dropped quietly
     instead of logging a parse error, and a mistyped id is caught here.
  2. No camelCase parameter names. Every schema in this family is snake_case, so a camelCase key is either a
     1.20.1-era leftover or a typo, and either way it is ignored.
  3. When the relevant jar is available in `libs/`, every key we use is compared against that mod's own
     recipes of the same type. Keys it never uses are reported for review (they are not always wrong: a
     processing recipe schema accepts a count for some types and never shows one in its own data).

usage: python tools/audit_create_recipes.py
"""
from __future__ import annotations

import json
import os
import re
import sys
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RECIPES = os.path.join(ROOT, "src", "main", "resources", "data", "scguns", "recipe")
LIBS = os.path.join(ROOT, "libs")

# Integration folders whose recipes belong to another mod, and the jar that documents that mod's schemas.
VENDOR_JARS = {
    "create": "create-",
    "createaddition": "createaddition-",
    "create_new_age": "create-new-age-",
    "createoreexcavation": "createoreexcavation-",
}

CAMEL = re.compile(r"[a-z]+[A-Z]")


def integration_files():
    for dirpath, _, names in os.walk(RECIPES):
        base = os.path.basename(dirpath)
        if not any(base == key or base.startswith(key) for key in VENDOR_JARS):
            continue
        for name in sorted(names):
            if name.endswith(".json"):
                yield os.path.join(dirpath, name)


def vendor_keys():
    """{recipe type: set of keys} from the vendored jars, so our files can be compared with real data."""
    out = {}
    for mod_id, prefix in VENDOR_JARS.items():
        if not os.path.isdir(LIBS):
            continue
        for jar_name in sorted(os.listdir(LIBS)):
            if not (jar_name.startswith(prefix) and jar_name.endswith(".jar")):
                continue
            try:
                with zipfile.ZipFile(os.path.join(LIBS, jar_name)) as zf:
                    for entry in zf.namelist():
                        if not (entry.endswith(".json") and "/recipe/" in entry):
                            continue
                        try:
                            data = json.loads(zf.read(entry).decode("utf-8"))
                        except ValueError:
                            continue
                        kind = data.get("type")
                        if not kind:
                            continue
                        keys = flatten_keys(data)
                        out.setdefault(kind, set()).update(keys)
                        # Every key the mod uses anywhere, so a key it never uses can be spotted even when
                        # no recipe of our type happens to carry it (processing_time/processingTime).
                        out.setdefault(kind.split(":")[0] + ":*", set()).update(keys)
            except zipfile.BadZipFile:
                continue
    return out


def flatten_keys(value, prefix=""):
    keys = set()
    if isinstance(value, dict):
        for key, item in value.items():
            keys.add(prefix + key)
            # `key` in a mechanical crafting grid is a map of single letters, not a schema.
            if key != "key":
                keys |= flatten_keys(item, prefix + key + ".")
    elif isinstance(value, list):
        for item in value:
            keys |= flatten_keys(item, prefix)
    return keys


def conditions_of(data):
    for key in ("neoforge:conditions", "conditions"):
        entries = data.get(key)
        if isinstance(entries, list):
            return [e.get("modid") for e in entries if isinstance(e, dict)]
    return []


def main():
    known = vendor_keys()
    problems = 0
    divergences = {}

    for path in integration_files():
        rel = os.path.relpath(path, RECIPES).replace("\\", "/")
        try:
            data = json.loads(open(path, encoding="utf-8").read())
        except ValueError as error:
            print("BROKEN %-58s not valid JSON (%s)" % (rel, error))
            problems += 1
            continue

        kind = str(data.get("type", ""))
        namespace = kind.split(":")[0] if ":" in kind else ""
        vanilla = namespace == "minecraft"

        # 1. gated on the mod that owns the type. Vanilla types need no gate and must not have one: a
        # minecraft:smelting recipe that is gated on Create simply disappears on a Create-less install.
        mod_ids = conditions_of(data)
        if vanilla:
            if mod_ids:
                print("BROKEN %-58s vanilla type %s is gated on %s" % (rel, kind, "/".join(mod_ids)))
                problems += 1
        elif not mod_ids:
            print("BROKEN %-58s has no neoforge:conditions gate" % rel)
            problems += 1
        elif namespace not in mod_ids:
            print("BROKEN %-58s type %s is gated on %s instead of %s"
                  % (rel, kind, "/".join(mod_ids), namespace))
            problems += 1

        # 2. no camelCase parameter names.
        for key in sorted(flatten_keys(data)):
            leaf = key.split(".")[-1]
            if CAMEL.search(leaf) and not key.startswith("neoforge:"):
                print("BROKEN %-58s key %r looks like a 1.20.1 parameter name; this family is snake_case"
                      % (rel, key))
                problems += 1

        # 3. a key the owning mod's recipes never use anywhere is one it will silently ignore - that is the
        # processing_time/processingTime failure, and the only way to see it without running the game.
        every_key = known.get(namespace + ":*")
        if every_key:
            ours = {k for k in flatten_keys(data) if not k.startswith("neoforge:") and k != "type"}
            ignored = sorted(k for k in ours if k not in every_key and
                             not any(k in keys for k in known.values() if k.split(".")[0] in ("ingredients", "results", "input", "result")))
            if ignored:
                print("BROKEN %-58s key(s) no %s recipe uses: %s" % (rel, namespace, ", ".join(ignored)))
                problems += 1

        # 4. compared with the mod's own recipes of this exact type, for review. This family shares one
        # schema across types, so a difference here is usually legitimate (a mixing recipe rolling a chance,
        # for instance: BasinRecipe does call rollResults).
        if kind in known:
            ours = {k for k in flatten_keys(data) if not k.startswith("neoforge:") and k != "type"}
            for key in sorted(k for k in ours if k not in known[kind]):
                divergences[key] = divergences.get(key, 0) + 1

    print("")
    if divergences:
        print("Keys our recipes use that the same type's own recipes never use (review, usually fine):")
        for key, count in sorted(divergences.items(), key=lambda kv: -kv[1]):
            print("  %-28s %d file(s)" % (key, count))
        print("")
    print("%d blocking problem(s)" % problems)
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
