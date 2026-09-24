#!/usr/bin/env python3
"""Point our data's item-tag references at the namespace that actually exists.

NeoForge 21.1.249 ships 243 ``data/c/tags/item/...`` convention tags and almost
no ``data/neoforge/tags``: the shared namespace moved from ``neoforge:`` (and
``forge:``) to ``c:``.  Mekanism ships ``c:`` tags and zero ``neoforge:`` ones.
The port's data still referenced ``neoforge:`` tags, which nothing defines, so
those ingredients silently resolved to nothing -- the recipe loads and can
never be crafted, which never shows up in the server log.

Only tags whose ``c:`` equivalent was *verified present* are remapped; the table
below is deliberate, not a blanket prefix swap, because a few paths were also
renamed (``cobblestone/normal`` -> ``cobblestones/normal``).  Any ``neoforge:``
or ``forge:`` tag reference left over is reported and fails the run, so nothing
is missed silently.

Usage:
    python tools/fix_tag_refs.py --check
    python tools/fix_tag_refs.py
    python tools/fix_tag_refs.py --selftest
"""

import argparse
import json
import pathlib
import re
import sys

DATA = pathlib.Path("src/main/resources/data")

# Legacy reference -> verified modern reference.  Every entry was checked to
# exist in NeoForge 21.1.249 / ImmersiveEngineering 12.4.2 / Mekanism 10.7.19.85
# / Create 6.0.10 (see tools/scan_tag_namespaces.py and tools/c_tags.py).
TAG_MAP = {
    "neoforge:dusts/saltpeter": "c:dusts/saltpeter",
    "neoforge:dusts/sulfur": "c:dusts/sulfur",
    "neoforge:dusts/phosphorus": "c:dusts/phosphorus",
    "neoforge:dusts/nickel": "c:dusts/nickel",
    "neoforge:ingots/brass": "c:ingots/brass",
    "neoforge:ingots/iron": "c:ingots/iron",
    "neoforge:ingots/steel": "c:ingots/steel",
    "neoforge:ingots/copper": "c:ingots/copper",
    "neoforge:ingots/netherite": "c:ingots/netherite",
    "neoforge:nuggets/iron": "c:nuggets/iron",
    "neoforge:glass": "c:glass",
    "neoforge:glass_panes": "c:glass_panes",
    "neoforge:gunpowder": "c:gunpowder",
    "neoforge:chests/wooden": "c:chests/wooden",
    # these two were also renamed, not just re-namespaced
    "neoforge:cobblestone/normal": "c:cobblestones/normal",
    "neoforge:cobblestone/deepslate": "c:cobblestones/deepslate",
}

LEGACY_NS = ("neoforge:", "forge:")


def remap_tag(value):
    return TAG_MAP.get(value)


def rewrite_text(text):
    """Return (new_text, applied_count, leftovers)."""
    applied = 0
    leftovers = []

    def repl(match):
        nonlocal applied
        whole, tag = match.group(0), match.group(1)
        new = remap_tag(tag)
        if new is None:
            if tag.startswith(LEGACY_NS):
                leftovers.append(tag)
            return whole
        applied += 1
        return '"tag": "%s"' % new

    new_text = re.sub(r'"tag"\s*:\s*"([^"]+)"', repl, text)
    return new_text, applied, leftovers


def run(check_only):
    if not DATA.exists():
        print("ERROR: %s not found; run from the repo root" % DATA)
        return 2

    files = sorted(DATA.rglob("*.json"))
    touched = 0
    total = 0
    leftover_files = {}

    for path in files:
        try:
            text = path.read_text(encoding="utf-8")
        except Exception as exc:  # noqa: BLE001
            print("SKIP unreadable %s: %s" % (path, exc))
            continue

        new_text, applied, leftovers = rewrite_text(text)
        for tag in leftovers:
            leftover_files.setdefault(tag, set()).add(str(path))
        if not applied:
            continue

        total += applied
        touched += 1
        if not check_only:
            path.write_text(new_text, encoding="utf-8")

    print("scanned %d json files" % len(files))
    print("tag references remapped : %d" % total)
    print("files %s : %d" % ("needing changes" if check_only else "rewritten", touched))

    if leftover_files:
        print("")
        print("UNMAPPED legacy tag references still present (%d):" % len(leftover_files))
        for tag, where in sorted(leftover_files.items()):
            print("  %-40s %d file(s)" % (tag, len(where)))
        print("")
        print("Each of these points at a tag nothing defines.  Either add it to")
        print("TAG_MAP after verifying the modern equivalent exists, or delete the")
        print("reference.  Failing the run so it cannot be missed.")
        return 1

    return 0


def selftest():
    failures = []

    def check(label, got, want):
        if got != want:
            failures.append("%s: got %r want %r" % (label, got, want))

    text = '{\n  "key": {"Q": {"tag": "neoforge:dusts/saltpeter"}}\n}\n'
    out, applied, leftovers = rewrite_text(text)
    check("remap applied", applied, 1)
    check("remap value", "c:dusts/saltpeter" in out, True)
    check("remap old gone", "neoforge:" in out, False)
    check("no leftovers", leftovers, [])

    # A renamed (not merely re-namespaced) path must use the table, not a swap.
    out, applied, _ = rewrite_text('{"tag": "neoforge:cobblestone/normal"}')
    check("renamed path", "c:cobblestones/normal" in out, True)

    # Non-tag occurrences of the same string must not be touched.
    text = '{"item": "neoforge:dusts/saltpeter"}'
    out, applied, _ = rewrite_text(text)
    check("item field untouched", out, text)
    check("item field not counted", applied, 0)

    # An unmapped legacy reference must be surfaced, not silently kept.
    out, applied, leftovers = rewrite_text('{"tag": "neoforge:does/not/exist"}')
    check("leftover reported", leftovers, ["neoforge:does/not/exist"])
    check("leftover not counted", applied, 0)
    check("leftover preserved", "neoforge:does/not/exist" in out, True)

    # A modern reference must be left alone and must not be counted as a leftover.
    out, applied, leftovers = rewrite_text('{"tag": "c:dusts/saltpeter"}')
    check("modern untouched", out, '{"tag": "c:dusts/saltpeter"}')
    check("modern not counted", applied, 0)
    check("modern no leftover", leftovers, [])

    # minecraft: tags are vanilla and must never be flagged or renamed.
    out, applied, leftovers = rewrite_text('{"tag": "minecraft:wool"}')
    check("vanilla untouched", applied, 0)
    check("vanilla no leftover", leftovers, [])

    # Idempotence: a second pass must do nothing.
    once, _, _ = rewrite_text('{"tag": "neoforge:glass"}')
    twice, applied, _ = rewrite_text(once)
    check("idempotent", twice, once)
    check("idempotent count", applied, 0)

    # The table itself must be internally consistent.
    for legacy, modern in TAG_MAP.items():
        if not legacy.startswith(LEGACY_NS):
            failures.append("table key not legacy: %s" % legacy)
        if not modern.startswith("c:"):
            failures.append("table value not c:: %s" % modern)

    if failures:
        print("SELFTEST FAILED (%d)" % len(failures))
        for failure in failures:
            print("  - %s" % failure)
        return 1

    print("SELFTEST OK (%d mapping entries, rules behave as specified)" % len(TAG_MAP))
    return 0


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="report without writing")
    parser.add_argument("--selftest", action="store_true")
    args = parser.parse_args()

    if args.selftest:
        return selftest()
    return run(args.check)


if __name__ == "__main__":
    sys.exit(main())
