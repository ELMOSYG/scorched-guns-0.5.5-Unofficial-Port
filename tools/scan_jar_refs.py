"""Scan jar classes for references to a given constant-pool string.

    python tools/scan_jar_refs.py <jar> <needle> [<needle> ...]

Written for GeckoLib 4.6 archaeology: it tells us which classes mention e.g.
ITEM_RENDER_PERSPECTIVE (who sets the item display context that AnimationControllers
are keyed by).
"""
import sys
import zipfile
from pathlib import Path


def main() -> int:
    if len(sys.argv) < 3:
        print(__doc__)
        return 2
    jar, needles = Path(sys.argv[1]), [n.encode() for n in sys.argv[2:]]
    z = zipfile.ZipFile(jar)
    classes = [n for n in z.namelist() if n.endswith(".class")]
    for needle in needles:
        hits = [n for n in classes if needle in z.read(n)]
        print(f"{needle.decode()}: {len(hits)} class(es)")
        for h in hits:
            print("   ", h)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
