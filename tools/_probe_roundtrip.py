"""Measure how many data JSON files survive a json round-trip byte-identically.

If a file round-trips unchanged, a structural (parse -> transform -> dump)
migration produces a minimal git diff and is safe.  If it does not, the
migration must edit text surgically instead.  This is a throwaway probe.
"""

import json
import pathlib
import sys

roots = [
    "src/main/resources/data/scguns/recipe/create",
    "src/main/resources/data/scguns/recipe/mekanism",
    "src/main/resources/data/scguns/recipe/createoreexcavation",
]

same = diff = unparseable = 0
diff_files = []
bad = []
for root in roots:
    p = pathlib.Path(root)
    if not p.exists():
        continue
    for f in sorted(p.rglob("*.json")):
        raw = f.read_text(encoding="utf-8")
        try:
            obj = json.loads(raw)
        except Exception as e:  # noqa: BLE001
            unparseable += 1
            bad.append((str(f), repr(e)[:120]))
            continue
        out = json.dumps(obj, indent=2, ensure_ascii=False) + "\n"
        if out == raw:
            same += 1
        else:
            diff += 1
            if len(diff_files) < 5:
                diff_files.append(str(f))

print("round-trip identical : %d" % same)
print("round-trip differs   : %d" % diff)
print("strict-parse FAILED  : %d" % unparseable)
for f in diff_files:
    print("  differs: %s" % f)
for f, e in bad[:8]:
    print("  unparseable: %s -> %s" % (f, e))
sys.exit(0)
