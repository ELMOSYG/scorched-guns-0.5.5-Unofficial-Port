"""Tag keys the mod asks for must exist on 1.21.1 - `forge:` ones do not.

NeoForge 21.1 renamed the cross-mod common tags from `forge:` to `c:`. This port's data files were
migrated (its own `scguns:fragile` tag now lists `#c:glass_blocks` and `#c:glass_panes`), but two tag
lookups in Java still asked for the old namespace, and a `TagKey` for a tag nobody defines resolves
to nothing: `state.is(...)` is simply false for every block, so the branch silently never runs. Found
in BeamHandlerCommon:

  * `#forge:glass` in `isGlassBlock` - so tagged glass (stained glass, glass from other mods) stopped
    counting as glass and the beam would not pass through it, and
  * `#forge:ores` in the fortune path - harmless there because 0.5.5 ran the same loop in both
    branches, but it was still a dead lookup.

The rule: a namespace used for a tag lookup must be one that exists on 1.21.1 - `minecraft`, `c`,
`neoforge`, the mod's own `scguns`, or a namespace the mod ships a `data/<ns>/tags/` tree for.
`forge` is not in that list on 1.21.1, and neither is anything the mod does not ship.

usage: python tools/audit_tag_namespaces.py
"""
from __future__ import annotations

import os
import re
import sys

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")
DATA = os.path.join(ROOT, "src", "main", "resources", "data")

# Tag namespaces that exist on 1.21.1 regardless of the mod: vanilla, the NeoForge common `c` tags, and
# NeoForge's own namespace.
BUILT_IN = {"minecraft", "c", "neoforge"}

# `BlockTags.create(...)`, `ItemTags.create(...)`, `TagKey.create(...)` and friends, with either
# `fromNamespaceAndPath("ns", "path")` or `new ResourceLocation("ns:path")` inside.
LOOKUP = re.compile(r"(?:BlockTags|ItemTags|EntityTypeTags|FluidTags|BiomeTags|StructureTags|TagKey)"
                    r"\s*\.\s*create\s*\(([^;]*?)\)\s*;", re.S)
NS_CALL = re.compile(r'fromNamespaceAndPath\(\s*"([a-z0-9_.-]+)"\s*,')
NS_LITERAL = re.compile(r'"(?:new\s+)?ResourceLocation\(\s*"([a-z0-9_.-]+):')


def shipped_namespaces() -> set:
    """Namespaces the mod itself ships tag data for, so its own keys always resolve."""
    found = set()
    if os.path.isdir(DATA):
        for name in os.listdir(DATA):
            if os.path.isdir(os.path.join(DATA, name, "tags")):
                found.add(name)
    return found


def main() -> int:
    shipped = shipped_namespaces()
    allowed = BUILT_IN | shipped | {"scguns"}
    problems = []
    checked = 0

    for folder, _, files in os.walk(SRC):
        for name in files:
            if not name.endswith(".java"):
                continue
            path = os.path.join(folder, name)
            text = open(path, encoding="utf-8", errors="replace").read()
            for hit in LOOKUP.finditer(text):
                inner = hit.group(1)
                for pattern in (NS_CALL, NS_LITERAL):
                    for ns in pattern.findall(inner):
                        checked += 1
                        if ns in allowed:
                            continue
                        line = text[:hit.start()].count("\n") + 1
                        problems.append("%s:%d asks for a tag in the `%s` namespace, which does not "
                                        "exist on 1.21.1, so the lookup is false for every block"
                                        % (os.path.relpath(path, ROOT), line, ns))
            # A bare "forge:" string is how the two dead lookups looked before they were fixed.
            for hit in re.finditer(r'"(forge:[a-z0-9_/]+)"', text):
                line = text[:hit.start()].count("\n") + 1
                problems.append("%s:%d still names the tag %s; 1.21.1 calls it c:..."
                                % (os.path.relpath(path, ROOT), line, hit.group(1)))

    if not checked:
        problems.append("no tag lookups were found at all, so this audit is not measuring anything")

    for problem in problems:
        print("BROKEN %s" % problem)
    if problems:
        print("\n%d problem(s)" % len(problems))
        return 1
    print("0 problem(s): every tag the code asks for lives in a namespace that exists on 1.21.1 "
          "(%d lookups checked)" % checked)
    return 0


if __name__ == "__main__":
    sys.exit(main())
