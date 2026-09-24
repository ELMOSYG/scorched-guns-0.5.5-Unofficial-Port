"""Scan the port's data tree for latent defects the server log cannot show.

The server only reports data files it actually parses.  Two classes of problem
hide behind that:

1. ``category`` values that are not valid ``CraftingBookCategory`` names.  In
   1.21.1 the only legal values are building/redstone/equipment/misc
   (.refs/nf-src/.../world/item/crafting/CraftingBookCategory.java:12-15).  A
   recipe gated behind a mod that is not installed is never parsed, so its bad
   category never surfaces -- until someone installs that mod.

2. Legacy ``neoforge:``/``forge:`` item-tag namespaces.  In 1.21 the shared
   convention namespace is ``c:``; a ``neoforge:dusts/...`` reference points at
   a tag nothing defines.

Usage:
    python tools/scan_data_latent.py
"""

import json
import pathlib
import re
from collections import Counter, defaultdict

DATA = pathlib.Path("src/main/resources/data")

VALID_CATEGORIES = {"building", "redstone", "equipment", "misc"}

# Tag references that use a legacy/common namespace and are worth reviewing.
TAG_KEY = re.compile(r"^(neoforge|forge|c):(.+)$")


def walk(node, path, out):
    if isinstance(node, dict):
        for key, value in node.items():
            if key == "category" and isinstance(value, str):
                out["categories"][value].append(path)
            if key == "tag" and isinstance(value, str):
                m = TAG_KEY.match(value)
                if m:
                    out["tags"][m.group(1)].append((path, value))
            walk(value, path, out)
    elif isinstance(node, list):
        for element in node:
            walk(element, path, out)


def main():
    walk_out = {"categories": defaultdict(list), "tags": defaultdict(list)}

    for path in sorted(DATA.rglob("*.json")):
        try:
            document = json.loads(path.read_text(encoding="utf-8"))
        except Exception:  # noqa: BLE001
            continue
        walk(document, str(path), walk_out)

    print("=== category values used ===")
    counts = Counter()
    for category, paths in walk_out["categories"].items():
        counts[category] = len(paths)
    for category, count in sorted(counts.items(), key=lambda kv: -kv[1]):
        mark = "" if category in VALID_CATEGORIES else "   <-- INVALID for 1.21.1"
        print("  %-14s %4d%s" % (category, count, mark))
        if category not in VALID_CATEGORIES:
            for path in sorted(set(walk_out["categories"][category]))[:5]:
                print("        %s" % path)

    print("")
    print("=== item tag namespaces referenced ===")
    ns_counts = Counter()
    for ns, entries in walk_out["tags"].items():
        ns_counts[ns] = len(entries)
    for ns, count in sorted(ns_counts.items(), key=lambda kv: -kv[1]):
        note = ""
        if ns in ("neoforge", "forge"):
            note = "   <-- legacy namespace; 1.21 convention is c:"
        print("  %-10s %4d%s" % (ns, count, note))

    legacy = walk_out["tags"].get("neoforge", []) + walk_out["tags"].get("forge", [])
    if legacy:
        print("")
        print("=== distinct legacy tag references ===")
        distinct = sorted({value for _path, value in legacy})
        for value in distinct:
            files = sorted({p for p, v in legacy if v == value})
            print("  %-40s %d file(s)" % (value, len(files)))


if __name__ == "__main__":
    main()
