"""Dump the convention (``c:``) item tags a set of jars provides, filtered by
substring.  Used to decide the correct modern name for a legacy ``neoforge:``
tag reference.  Read-only.

Usage:
    python tools/c_tags.py cobblestone chests wool glass_panes
"""

import pathlib
import re
import sys
import zipfile

JARS = [
    r"C:\Users\len\.gradle\caches\modules-2\files-2.1\net.neoforged\neoforge\21.1.249"
    r"\6bb029ee4e7c4893495d75909208bbc3f6d97b55\neoforge-21.1.249-universal.jar",
]

TAG_ENTRY = re.compile(r"^data/(c)/tags/item/(.+)\.json$")


def main():
    needles = [n.lower() for n in sys.argv[1:]] or [""]
    names = set()
    for jar in JARS:
        path = pathlib.Path(jar)
        if not path.exists():
            print("MISSING jar: %s" % jar)
            continue
        with zipfile.ZipFile(path) as zf:
            for name in zf.namelist():
                m = TAG_ENTRY.match(name)
                if m:
                    names.add(m.group(2))

    print("total c: item tags in NeoForge: %d" % len(names))
    for needle in needles:
        hits = sorted(n for n in names if needle in n.lower())
        print("")
        print("=== matching %r (%d) ===" % (needle, len(hits)))
        for hit in hits:
            print("  c:%s" % hit)


if __name__ == "__main__":
    main()
