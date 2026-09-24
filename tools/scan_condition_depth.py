"""Report the nesting depth of every ``neoforge:conditions`` key in our data.

NeoForge's conditional wrapper (``ConditionalOps``) only ever wraps the
*top-level* datapack object: it overrides no ``getMap``/``getMapValues``, so
nested maps are never scanned.  A ``neoforge:conditions`` key nested inside
anything is therefore inert -- and in some places (ImmersiveEngineering's
``secondaries[]``) the mod reads its own ``conditions`` key instead.

So every nested ``neoforge:conditions`` is either dead weight or a rename that
should be reverted.  This tool lists them so the distinction can be made.

Usage:
    python tools/scan_condition_depth.py
"""

import json
import pathlib
from collections import Counter

DATA = pathlib.Path("src/main/resources/data")
KEY = "neoforge:conditions"


def walk(node, depth, path, breadcrumb, hits):
    if isinstance(node, dict):
        for key, value in node.items():
            here = breadcrumb + [key]
            if key == KEY:
                hits.append((depth, path, "/".join(here)))
            walk(value, depth + 1, path, here, hits)
    elif isinstance(node, list):
        for index, element in enumerate(node):
            walk(element, depth, path, breadcrumb + ["[%d]" % index], hits)


def main():
    hits = []
    for path in sorted(DATA.rglob("*.json")):
        try:
            document = json.loads(path.read_text(encoding="utf-8"))
        except Exception:  # noqa: BLE001
            continue
        walk(document, 1, str(path), [], hits)

    by_depth = Counter(depth for depth, _p, _b in hits)
    print("total neoforge:conditions keys: %d" % len(hits))
    for depth, count in sorted(by_depth.items()):
        note = "   <- top level (correct)" if depth == 1 else "   <- NESTED (inert / suspicious)"
        print("  depth %-3d %4d%s" % (depth, count, note))

    nested = [(d, p, b) for d, p, b in hits if d > 1]
    if nested:
        print("")
        print("=== nested occurrences grouped by parent path (%d) ===" % len(nested))
        groups = Counter()
        for _depth, path, breadcrumb in nested:
            parent = breadcrumb.rsplit("/", 1)[0] if "/" in breadcrumb else breadcrumb
            groups["%s  @  %s" % (parent, path.split("data\\")[-1])] += 1
        for entry, count in sorted(groups.items()):
            print("  %-100s %d" % (entry, count))


if __name__ == "__main__":
    main()
