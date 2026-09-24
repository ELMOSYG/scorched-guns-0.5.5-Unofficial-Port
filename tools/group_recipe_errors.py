"""Group the recipe/data parse errors out of a dedicated-server log.

The server log is the ground truth for "which data files the loaders actually
rejected": every rejected file produces exactly one

    [..] [minecraft/RecipeManager]: Parsing error loading recipe <id>

line, and the JSON parse failure follows on the next line(s).  This script
turns that into a compact, grouped report so a migration can be driven by fact
instead of by guessing which files are broken.

Usage:
    python tools/group_recipe_errors.py build-logs/server-renderfix.txt
"""

import re
import sys
rsplit = None

HEAD = re.compile(r"Parsing error loading recipe (\S+)$")


def parse(path):
    """Return {recipe_id: first_error_line} for every rejected entry."""
    with open(path, "r", encoding="utf-8", errors="replace") as fh:
        lines = fh.read().splitlines()

    out = {}
    for i, line in enumerate(lines):
        m = HEAD.search(line.rstrip())
        if not m:
            continue
        rid = m.group(1)
        # The exception message is the following line.
        msg = ""
        for j in range(i + 1, min(i + 4, len(lines))):
            if "Exception" in lines[j] or "Error" in lines[j]:
                msg = lines[j]
                break
        out[rid] = msg
    return out


# Each rule maps a rejected file to the *reason category* we care about, and to
# the mod whose version renamed the field.  Order matters: first match wins.
RULES = [
    ("create:accept_mirrored", "acceptMirrored", "create"),
    ("create:transitional_item", "transitionalItem", "create"),
    ("create:transitional_id", "No key transitional_item", "create"),
    ("mekanism:snake_case", "chemical_input", "mekanism"),
    ("mekanism:output_id", "No key id in MapLike[{\"count\"", "mekanism"),
    ("createoreexcavation:vanilla_bound", "amountMultiplierMax", "createoreexcavation"),
    ("farmersdelight:missing", "farmersdelight:", "farmersdelight"),
    ("ingredient:vanilla_or_tag", "No key tag in MapLike[{\"ingredient\"", "vanilla"),
]


def category(msg):
    for name, needle, _mod in RULES:
        if needle in msg:
            return name
    return "unclassified"


def main():
    path = sys.argv[1] if len(sys.argv) > 1 else "build-logs/server-renderfix.txt"
    found = parse(path)

    groups = {}
    for rid, msg in found.items():
        groups.setdefault(category(msg), []).append(rid)

    print("rejected entries: %d" % len(found))
    print("")
    for name in sorted(groups, key=lambda k: (-len(groups[k]), k)):
        ids = sorted(groups[name])
        print("%-34s %4d" % (name, len(ids)))
        for rid in ids[:4]:
            print("      - %s" % rid)
        if len(ids) > 4:
            print("      ... and %d more" % (len(ids) - 4))
        print("")

    if "unclassified" in groups:
        print("=== unclassified raw messages (first 12) ===")
        for rid in sorted(groups["unclassified"])[:12]:
            print("%s\n    %s\n" % (rid, found[rid][:400]))


if __name__ == "__main__":
    main()
