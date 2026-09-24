"""Compare the 0.5.5 decompile against the port so lost/missing classes show up.

A port that silently drops a class loses the feature it implemented, and nothing
in the build complains.  This lists, per subpackage, the files present on only
one side plus the files whose byte size differs a lot (a rough proxy for "was
ported" vs "was rewritten" vs "was gutted").

Usage:
    python tools/compare_trees.py [subpackage ...]
"""

import pathlib
import sys

OLD = pathlib.Path(r"E:\mod\SG2-1.21\.sg055_deobf\top\ribs\scguns")
NEW = pathlib.Path(r"E:\mod\scgun-0.5.5-1.21.1-neoforge\src\main\java\top\ribs\scguns")

DEFAULT_SUBS = ["client/render", "mixin", "client/handler", "entity/ai", "client"]


def files(root, sub):
    base = root / sub
    if not base.is_dir():
        return {}
    return {p.relative_to(base).as_posix(): p.stat().st_size
            for p in base.rglob("*.java")}


def main():
    subs = sys.argv[1:] or DEFAULT_SUBS
    for sub in subs:
        old, new = files(OLD, sub), files(NEW, sub)
        print("=" * 78)
        print("%s   (0.5.5: %d files, port: %d files)" % (sub, len(old), len(new)))
        missing = sorted(set(old) - set(new))
        added = sorted(set(new) - set(old))
        if missing:
            print("  --- in 0.5.5 but MISSING from the port ---")
            for name in missing:
                print("      %-58s %6d bytes" % (name, old[name]))
        if added:
            print("  --- only in the port ---")
            for name in added:
                print("      %-58s %6d bytes" % (name, new[name]))
        # Big size deltas on common files, worth a look.
        drift = []
        for name in sorted(set(old) & set(new)):
            o, n = old[name], new[name]
            if o and (n < o * 0.6 or n > o * 1.8):
                drift.append((name, o, n))
        if drift:
            print("  --- large size change (possible gutting / rewrite) ---")
            for name, o, n in drift[:25]:
                print("      %-58s %6d -> %6d" % (name, o, n))
        if not (missing or added or drift):
            print("  (identical file sets, no large size changes)")
        print("")


if __name__ == "__main__":
    main()
