"""Check whether the item tags our data references actually exist.

In 1.21 the shared convention tag namespace is ``c:`` -- NeoForge 21.1.249
ships 277 ``data/c/tags/item/...`` files and almost no ``data/neoforge/tags``,
and Mekanism ships ``c:`` tags with zero ``neoforge:`` ones.  A ``neoforge:``
reference therefore points at a tag nothing defines, which silently yields an
empty ingredient (the recipe loads but can never be crafted).  That is
invisible in the server log, so it needs a tool.

Usage:
    python tools/scan_tag_namespaces.py
"""

import json
import pathlib
import re
import zipfile
from collections import defaultdict

DATA = pathlib.Path("src/main/resources/data")

JARS = [
    r"C:\Users\len\.gradle\caches\modules-2\files-2.1\net.neoforged\neoforge\21.1.249"
    r"\6bb029ee4e7c4893495d75909208bbc3f6d97b55\neoforge-21.1.249-universal.jar",
    r"D:\MCJAVA\.minecraft\versions\1.21.1-NeoForge_21.1.250\mods\Mekanism-1.21.1-10.7.19.85.jar",
    r"D:\MCJAVA\.minecraft\versions\1.21.1-NeoForge_21.1.250\mods\create-1.21.1-6.0.10.jar",
    r"D:\MCJAVA\.minecraft\versions\1.21.1-NeoForge_21.1.250\mods\ImmersiveEngineering-1.21.1-12.4.2-194.jar",
    r"D:\MCJAVA\.minecraft\versions\1.21.1-NeoForge_21.1.250\mods\createoreexcavation-1.21-1.6.8.jar",
]

TAG_ENTRY = re.compile(r"^data/([^/]+)/tags/item/(.+)\.json$")


def provided_tags():
    """{tag id -> set of jars providing it} for item tags only."""
    found = defaultdict(set)
    for jar in JARS:
        path = pathlib.Path(jar)
        if not path.exists():
            continue
        with zipfile.ZipFile(path) as zf:
            for name in zf.namelist():
                m = TAG_ENTRY.match(name)
                if m:
                    found["%s:%s" % (m.group(1), m.group(2))].add(path.name)
    # our own tree, in both the 1.21 ('item') and legacy 1.20 ('items') layouts
    for subdir, note in (("item", "SELF"), ("items", "SELF(legacy items/ layout)")):
        for path in DATA.glob("*/tags/%s/**/*.json" % subdir):
            parts = path.relative_to(DATA).parts
            tag = "%s:%s" % (parts[0], "/".join(parts[3:])[: -len(".json")])
            found[tag].add(note)
    return found


def referenced_tags():
    """{tag id -> set of data files referencing it}."""
    refs = defaultdict(set)
    for path in DATA.rglob("*.json"):
        try:
            text = path.read_text(encoding="utf-8")
        except Exception:  # noqa: BLE001
            continue
        for m in re.finditer(r'"tag"\s*:\s*"([^"]+)"', text):
            refs[m.group(1)].add(str(path))
    return refs


def main():
    provided = provided_tags()
    refs = referenced_tags()

    print("tags provided by NeoForge / Mekanism / Create / IE / COE / SELF: %d" % len(provided))
    print("distinct tags referenced by our data: %d" % len(refs))
    print("")

    missing = {}
    for tag, files in sorted(refs.items()):
        if tag in provided:
            continue
        missing[tag] = files

    print("=== referenced tags that NOTHING provides (%d) ===" % len(missing))
    for tag, files in sorted(missing.items(), key=lambda kv: -len(kv[1])):
        alt = tag.split(":", 1)[1]
        c_form = "c:" + alt
        note = "  (c:%s EXISTS -> rename)" % alt if c_form in provided else "  (no c: equivalent either)"
        print("  %-38s %3d file(s)%s" % (tag, len(files), note))

    print("")
    print("=== self-defined tags and where they were found ===")
    for tag, sources in sorted(provided.items()):
        if any(s.startswith("SELF") for s in sources):
            print("  %-38s %s" % (tag, sorted(sources)))


if __name__ == "__main__":
    main()
