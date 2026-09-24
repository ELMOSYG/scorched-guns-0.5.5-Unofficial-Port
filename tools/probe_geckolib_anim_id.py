"""Report how GeckoLib 4.6 stores the per-stack animatable id.

GeckoLib used to read the id from the stack's NBT ("GeckoLibID"); 4.6 moved it to a
data component (GeckoLibConstants.STACK_ANIMATABLE_ID_COMPONENT) and
GeoItem.getId(stack) now falls back to Long.MAX_VALUE when the component is absent.
This script lists every class in the GeckoLib jar that assigns ids, so we can tell
whether anything assigns one for us.

    python tools/probe_geckolib_anim_id.py [path\to\geckolib.jar]
"""
import sys
import zipfile
from pathlib import Path

DEFAULT = Path("libs/geckolib-neoforge-1.21.1-4.9.3.jar")

# Constant-pool UTF8 entries we care about.
NEEDLES = (
    b"getOrAssignId",
    b"STACK_ANIMATABLE_ID_COMPONENT",
    b"GeckoLibID",
)


def main() -> int:
    jar = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT
    if not jar.is_file():
        print(f"missing jar: {jar}")
        return 2
    z = zipfile.ZipFile(jar)
    print(f"jar: {jar}")
    for needle in NEEDLES:
        hits = [n for n in z.namelist() if n.endswith(".class") and needle in z.read(n)]
        print(f"\n{needle.decode()} referenced by {len(hits)} class(es):")
        for h in hits:
            print("   ", h)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
