"""Compare every PNG in the original 0.5.5 jar against the ported resources.

The port was assembled from the decompiled tree, so a texture can go missing, arrive empty,
or be replaced by a placeholder without anything failing at build time - the game just draws
nothing (or the purple/black missing texture). This lists size and hash differences so the
cause of "the translucent material does not render" can be seen instead of guessed.

Usage:
    python tools/compare_textures.py [substring-filter]
"""

import hashlib
import pathlib
import sys
import zipfile

ORIGINAL = pathlib.Path(r"需要移植的mod\ScorchedGuns-0.5.5-1.20.1.jar")
PORTED = pathlib.Path("src/main/resources")


def digest(data):
    return hashlib.sha256(data).hexdigest()[:12]


def main():
    needle = sys.argv[1].lower() if len(sys.argv) > 1 else ""
    with zipfile.ZipFile(ORIGINAL) as jar:
        original = {n: jar.read(n) for n in jar.namelist()
                    if n.endswith(".png") and needle in n.lower()}

    missing, empty, different, same = [], [], [], 0
    for name, data in sorted(original.items()):
        path = PORTED / name
        if not path.exists():
            missing.append(name)
        elif path.stat().st_size == 0:
            empty.append(name)
        elif digest(path.read_bytes()) != digest(data):
            different.append((name, len(data), path.stat().st_size))
        else:
            same += 1

    print("original jar PNGs matching %r: %d" % (needle or "*", len(original)))
    print("  identical: %d" % same)
    for name in missing:
        print("  MISSING   %s" % name)
    for name in empty:
        print("  EMPTY     %s" % name)
    for name, a, b in different:
        print("  DIFFERENT %s (jar %d bytes, port %d bytes)" % (name, a, b))

    # Extra PNGs in the port that the jar does not have are fine (new content), so only the
    # original side is reported.
    return 1 if (missing or empty or different) else 0


if __name__ == "__main__":
    sys.exit(main())
