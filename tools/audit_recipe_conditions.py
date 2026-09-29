"""A recipe that names another mod's items must be guarded by that mod being loaded.

Found by starting the dev server while checking the version bump: two recipes parse-failed.

    Parsing error loading recipe scguns:mech_press/depleted_diamond_steel
    ... Failed to parse either. First: Not a json array: {"item":"create:experience_nugget"};
        Second: No key tag in MapLike[...]; Unknown registry key in ... minecraft:item:
        create:experience_nugget

`scguns:mechanical_pressing/depleted_diamond_steel` and its powered twin list Create's experience
nugget, but unlike their `create/deploying` sibling they carried no `neoforge:conditions` guard. With
Create present they resolve; without it the item is unknown, the whole recipe fails to parse, and the
server logs an error - and since the port's own gate is "no recipe parse errors", a player running
without Create sees two of them. The guard says the intent out loud: the recipe only exists when the
mod whose item it wants is there.

The rule: every `data/<ns>/recipe/**` file whose contents name an item from a mod other than
minecraft, the mod itself, or a cross-mod tag namespace must carry a `neoforge:conditions` entry with a
`mod_loaded` condition for each such mod.

Note what this deliberately does NOT treat as an exemption: shipping *data* in a namespace. This mod
ships `data/create/...` recipes for Create's own machines, so "the mod has a create directory" says
nothing about whether Create is installed - using that as the test is how the first version of this
audit walked straight past the two files it exists for. The first version of it was checked against
those very files and reported them as fine.

usage: python tools/audit_recipe_conditions.py
"""
from __future__ import annotations

import json
import os
import re
import sys

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
RECIPES = os.path.join(ROOT, "src", "main", "resources", "data", "scguns", "recipe")
DATA = os.path.join(ROOT, "src", "main", "resources", "data")

# Namespaces a recipe may name without a guard: vanilla, the mod's own, and the NeoForge common tags
# (`c:`), which are item tags rather than a mod's registry.
FREE = {"minecraft", "scguns", "c", "neoforge", "forge"}

ID = re.compile(r'"([a-z0-9_.-]+):([a-z0-9_/.-]+)"')


def guarded_mods(text: str) -> set:
    """The mod ids the file declares it needs, from its neoforge:conditions block."""
    try:
        data = json.loads(text)
    except json.JSONDecodeError:
        return set()
    found = set()

    def walk(node):
        if isinstance(node, dict):
            if node.get("type") == "neoforge:mod_loaded" and "modid" in node:
                found.add(str(node["modid"]))
            for value in node.values():
                walk(value)
        elif isinstance(node, list):
            for value in node:
                walk(value)

    walk(data.get("neoforge:conditions", []))
    return found


def shipped_namespaces() -> set:
    """Namespaces this mod ships data in - deliberately unused by the rule above, kept out so nobody
    reaches for it again thinking it means the owning mod is present."""
    found = set()
    if os.path.isdir(DATA):
        for name in os.listdir(DATA):
            if os.path.isdir(os.path.join(DATA, name)):
                found.add(name)
    return found


def main() -> int:
    problems = []
    files = 0
    guarded = 0

    for folder, _, names in os.walk(RECIPES):
        for name in names:
            if not name.endswith(".json"):
                continue
            files += 1
            path = os.path.join(folder, name)
            text = open(path, encoding="utf-8", errors="replace").read()
            # Every namespace that is not vanilla or ours is another mod's, whatever directories this
            # mod happens to ship data in.
            wanted = {ns for ns, _ in ID.findall(text) if ns not in FREE}
            if not wanted:
                continue
            present = guarded_mods(text)
            missing = sorted(wanted - present)
            if missing:
                problems.append("%s names items from %s but has no neoforge:mod_loaded condition for "
                                "%s, so without that mod the recipe fails to parse and the server logs "
                                "an error"
                                % (os.path.relpath(path, ROOT), ", ".join(sorted(wanted)),
                                   ", ".join(missing)))
            else:
                guarded += 1

    if not files:
        problems.append("no recipes were found at all, so this audit is not measuring anything")

    for problem in problems:
        print("BROKEN %s" % problem)
    if problems:
        print("\n%d problem(s)" % len(problems))
        return 1
    print("0 problem(s): all %d recipes that name another mod's items are guarded (%d checked for it)"
          % (files, guarded))
    return 0


if __name__ == "__main__":
    sys.exit(main())
