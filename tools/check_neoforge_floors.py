"""What NeoForge range do the jars we depend on demand?

Our own declared range is only half the story: NeoForge checks every mod's `neoforge` dependency
separately, so the lowest NeoForge a player can actually run this mod on is the maximum of our own
range and those of framework / geckolib / curios - if any of them refuses to load, the game stops
before our code runs.

    python tools/check_neoforge_floors.py
    python tools/check_neoforge_floors.py <jar> [<jar> ...]
"""

import pathlib
import re
import sys
import zipfile

DEFAULT_JARS = sorted(pathlib.Path("libs").glob("*.jar"))
RANGE = re.compile(r'modId\s*=\s*"neoforge".*?versionRange\s*=\s*"([^"]+)"', re.S)


def floors(jar):
    """-> list of (modId, neoforge range) declared inside the jar."""
    out = []
    with zipfile.ZipFile(jar) as z:
        for name in ("META-INF/neoforge.mods.toml", "META-INF/mods.toml"):
            if name not in z.namelist():
                continue
            text = z.read(name).decode("utf-8", "replace")
            for block in re.split(r"\[\[", text):
                if 'modId="neoforge"' not in block and 'modId = "neoforge"' not in block:
                    continue
                range_match = RANGE.search(block)
                if range_match:
                    mod = re.search(r'\[\[dependencies\.([^\]]+)\]\]', "[[" + block)
                    out.append((mod.group(1) if mod else "?", range_match.group(1)))
    return out


def main():
    jars = [pathlib.Path(a) for a in sys.argv[1:]] or DEFAULT_JARS
    if not jars:
        print("no jars to inspect")
        return 2
    lowest = {}
    for jar in jars:
        try:
            declared = floors(jar)
        except zipfile.BadZipFile:
            continue
        for mod, version_range in declared:
            print("%-46s %-22s neoforge %s" % (jar.name[:46], mod, version_range))
            lowest[mod] = version_range
    print("")
    print("%d dependency declaration(s) with a NeoForge range" % len(lowest))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
