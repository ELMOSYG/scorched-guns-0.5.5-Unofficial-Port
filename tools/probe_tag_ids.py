"""Probe the environment for tag / item ids the port's own tags reference.

Answers three questions the server log raises:

1. Does ``minecraft:grass`` still exist as an item in 1.21.1, or was it renamed
   to ``minecraft:short_grass``?
2. Which glass tags exist as BLOCK tags (``scguns:tags/block/fragile.json``
   references ``#c:glass``, which is an item tag in NeoForge)?
3. Is ``scguns:anthralite_knife`` really absent from the built jar (it is
   registered reflectively and only when FarmersDelight is loaded)?

Usage:
    python tools/probe_tag_ids.py
"""

import pathlib
import re
import zipfile

JARS = [
    r"D:\MCJAVA\.minecraft\versions\1.21.1-NeoForge_21.1.250\mods\ImmersiveEngineering-1.21.1-12.4.2-194.jar",
    r"D:\MCJAVA\.minecraft\versions\1.21.1-NeoForge_21.1.250\mods\Mekanism-1.21.1-10.7.19.85.jar",
    r"D:\MCJAVA\.minecraft\versions\1.21.1-NeoForge_21.1.250\mods\create-1.21.1-6.0.10.jar",
]

GLOBS = [
    r"C:\Users\len\.gradle\caches\modules-2\files-2.1\net.neoforged\neoforge\21.1.249\*\neoforge-21.1.249-universal.jar",
    r"C:\Users\len\.gradle\caches\modules-2\files-2.1\net.neoforged\neoforge\21.1.249\*\neoforge-21.1.249-merged.jar",
]


def all_jars():
    found = []
    for pattern in GLOBS:
        found.extend(str(p) for p in pathlib.Path(pattern.split("*")[0]).glob("*/*.jar"))
    found.extend(JARS)
    # the merged vanilla+neoforge jar the build uses
    found.extend(str(p) for p in pathlib.Path("build/moddev/artifacts").glob("*.jar"))
    return sorted(set(found))


GLASS_BLOCK = re.compile(r"^data/([^/]+)/tags/block/(.*glass.*)\.json$")
GLASS_ITEM = re.compile(r"^data/([^/]+)/tags/item/(.*glass.*)\.json$")


def main():
    jars = all_jars()
    print("scanning %d jar(s)" % len(jars))

    grass = []
    glass_block = []
    glass_item = []

    for jar in jars:
        try:
            with zipfile.ZipFile(jar) as zf:
                names = zf.namelist()
        except Exception:  # noqa: BLE001
            continue
        for name in names:
            if name.endswith("data/minecraft/tags/item/grass.json"):
                grass.append((jar, name))
            if name.endswith("data/minecraft/tags/item/short_grass.json"):
                grass.append((jar, name))
            m = GLASS_BLOCK.match(name)
            if m:
                glass_block.append((jar, "block %s:%s" % (m.group(1), m.group(2))))
            m = GLASS_ITEM.match(name)
            if m:
                glass_item.append((jar, "item  %s:%s" % (m.group(1), m.group(2))))

    print("")
    print("=== grass item tags found ===")
    for jar, name in grass:
        print("  %-60s %s" % (name, pathlib.Path(jar).name))
    if not grass:
        print("  (none found in the scanned jars -> vanilla data not on this scan list)")

    print("")
    print("=== glass BLOCK tags available ===")
    for _jar, tag in sorted(set(glass_block)):
        print("  %s" % tag)

    print("")
    print("=== glass ITEM tags available (top 25) ===")
    for _jar, tag in sorted(set(glass_item))[:25]:
        print("  %s" % tag)

    print("")
    print("=== is scguns:anthralite_knife in the built jar? ===")
    jars = sorted((p for p in pathlib.Path("build/libs").glob("scguns-*.jar")
                   if not p.name.endswith(("-sources.jar", "-javadoc.jar"))),
                  key=lambda p: p.stat().st_mtime, reverse=True)
    built = jars[0] if jars else pathlib.Path("build/libs/scguns-none.jar")
    print("  jar: %s" % built.name)
    if built.exists():
        with zipfile.ZipFile(built) as zf:
            hits = [n for n in zf.namelist() if "anthralite_knife" in n]
        print("  entries mentioning anthralite_knife: %d" % len(hits))
        for hit in hits[:10]:
            print("    %s" % hit)


if __name__ == "__main__":
    main()
