"""Common (c:) tags must be the ones NeoForge actually defines.

The player reported that scguns:buckshot could not be crafted with vanilla gunpowder, and that vanilla
gunpowder is not in the gunpowder tag. Both halves were right, and the cause was a rename slip: 1.20.1's
common tags were `forge:x`, NeoForge's are `c:x`, and this port turned `forge:gunpowder` into
`c:gunpowder` when NeoForge's tag is `c:gunpowders` - plural. Read out of the jars:

    NeoForge 21.1.249  data/c/tags/item/gunpowders.json      = [minecraft:gunpowder, #forge:gunpowder?]
    Forge 1.20.1       data/forge/tags/items/gunpowder.json  = [minecraft:gunpowder]
    ScorchedGuns 0.5.5 data/forge/tags/items/gunpowder.json  = [scguns:sheol, scguns:peal]

On 1.20.1 those two merged, so the tag held vanilla gunpowder plus the mod's powders. On 1.21.1 the mod's
file became `c:gunpowder`, a tag nothing else defines, so `#c:gunpowder` was just the two mod items and
vanilla gunpowder silently stopped working - a tag that does not exist is empty, not an error. The same
slip had hit glass: 0.5.5's `#forge:glass` resolved to Forge's glass tag (vanilla, stained and tinted)
merged with the mod's niter glass, while the port's invented `c:glass` held only niter glass, so
`long_scope` stopped accepting vanilla glass.

A `c:` tag is not a name this mod gets to choose; it is NeoForge's vocabulary. Two rules:

  A. a `c:`/`neoforge:` tag the mod references must be defined by NeoForge or shipped by the mod, because
     a name nobody defines can only ever resolve to nothing. A file that declares a `neoforge:mod_loaded`
     condition is exempt: it is guarded on that mod, and that mod may well be the one defining the tag
     (Create defines c:ingots/brass, for instance).
  B. a tag NeoForge does not define, where NeoForge defines one differing only by a trailing "s", is a
     rename slip whatever else is true of it.

Only real tag references count - `"tag": "..."` and any `"#c:..."` / `"#neoforge:..."` string. Matching
every `"c:..."` string instead drags in codec names like neoforge:conditions and biome modifier types,
which are not tags at all and produced 18 false positives on the first version of this check.

usage: python tools/audit_common_tags.py
"""
from __future__ import annotations

import collections
import glob
import json
import os
import re
import sys
import zipfile

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SOURCES = [
    os.path.join(ROOT, "src", "main", "resources"),
    os.path.join(ROOT, "src", "main", "java"),
    os.path.join(ROOT, "maid-compat", "src", "main"),
]
SKIP_DIRS = {"build", ".git", "run", ".gradle", ".refs", "build-logs"}

# Where a tag can be named: an ingredient's "tag" field, or a #-prefixed entry anywhere (tag lists,
# biome modifier targets, and so on). A bare "c:..." string is far too common as a codec id to use.
REFERENCE = re.compile(r'"(?:tag"\s*:\s*)?"#?(c|neoforge):([a-z0-9_/]+)"')
# "tag": "c:..." has no leading #, so it needs its own pattern.
TAG_FIELD = re.compile(r'"tag"\s*:\s*"#?(c|neoforge):([a-z0-9_/]+)"')

# Registry directories under data/<ns>/tags/.  Everything after them is the tag's own path.
SINGLE_REGISTRIES = {
    "item", "block", "fluid", "entity_type", "game_event", "enchantment", "painting_variant",
    "structure", "point_of_interest_type", "banner_pattern", "instrument", "jukebox_song",
    "cat_variant", "wolf_variant", "frog_variant", "painting_variant", "damage_type", "trim_material",
    "trim_pattern", "menu", "recipe_serializer", "mob_effect", "attribute", "potion", "worldgen",
}
TAG_PATH = re.compile(r"data[\\/]([a-z0-9_]+)[\\/]tags[\\/](.+)\.json$")


def tag_id(namespace: str, under_tags: str) -> str:
    """Turn a path under tags/ into the id the game uses.

    item/dusts/nickel -> c:dusts/nickel, item/ingots/iron -> c:ingots/iron,
    worldgen/biome/is_plains -> c:is_plains. Getting this wrong is how the first draft of this check
    reported c:ingots/iron - a tag NeoForge certainly does define - as missing.
    """
    parts = under_tags.replace("\\", "/").split("/")
    if parts and parts[-1].endswith(".json"):
        parts[-1] = parts[-1][:-5]
    if parts and parts[0] == "worldgen":
        parts = parts[2:]
    elif parts and parts[0] in SINGLE_REGISTRIES:
        parts = parts[1:]
    return "%s:%s" % (namespace, "/".join(parts))


def neoforge_tags() -> set:
    jars = glob.glob(os.path.join(ROOT, "build", "moddev", "artifacts", "neoforge-*-merged.jar"))
    if not jars:
        raise SystemExit("no merged NeoForge jar under build/moddev/artifacts - run a build first")
    found = set()
    for jar in jars:
        with zipfile.ZipFile(jar) as z:
            for name in z.namelist():
                if not name.startswith("data/") or "/tags/" not in name or not name.endswith(".json"):
                    continue
                parts = name.split("/")
                if len(parts) >= 5 and parts[1] in ("c", "neoforge"):
                    found.add(tag_id(parts[1], "/".join(parts[3:])))
    return found


def is_guarded(text: str) -> bool:
    """True when the file says it depends on another mod being loaded."""
    return "neoforge:mod_loaded" in text


def scan(sources) -> tuple:
    refs = collections.defaultdict(set)
    shipped = set()
    for root in sources:
        for base, dirs, names in os.walk(root):
            dirs[:] = [d for d in dirs if d not in SKIP_DIRS]
            for name in names:
                if not name.endswith((".json", ".java")):
                    continue
                path = os.path.join(base, name)
                rel = os.path.relpath(path, ROOT)
                if name.endswith(".json"):
                    m = TAG_PATH.search(rel)
                    if m and m.group(1) in ("c", "neoforge"):
                        shipped.add(tag_id(m.group(1), m.group(2)))
                try:
                    text = open(path, encoding="utf-8", errors="replace").read()
                except OSError:
                    continue
                if is_guarded(text):
                    continue
                for ns, tag in REFERENCE.findall(text) + TAG_FIELD.findall(text):
                    refs["%s:%s" % (ns, tag)].add(rel)
    return refs, shipped


def main() -> int:
    neo = neoforge_tags()
    refs, shipped = scan(SOURCES)

    problems = []
    notes = []
    print("NeoForge defines %d common tags; the mod ships %d; the mod references %d"
          % (len(neo), len(shipped), len(refs)))
    # Shipped tags matter even when nothing in the mod references them: shipping a `c:` tag is a claim
    # about shared vocabulary, and a private tag wearing a common name misleads every other mod. The
    # first version of this loop only walked the references it found, so a stray c:gunpowder with no
    # recipe pointing at it slipped straight through - which is what the controls caught.
    for tag in sorted(set(refs) | shipped):
        if tag in neo:
            continue
        where = sorted(refs[tag])[0] if tag in refs else "the tag file itself"
        if tag in shipped:
            # The mod is defining this common tag itself, which is right for its own items - unless
            # NeoForge already spells the same idea differently, which is the gunpowder and glass
            # mistake.
            siblings = sorted(t for t in neo if t.startswith(tag + "s") or t.startswith(tag + "_")
                              or t.startswith(tag + "/"))
            if siblings:
                problems.append("%s is shipped by the mod, but NeoForge does not define that tag - it "
                                "defines %s. Renaming forge: to c: is not a prefix swap: the tag has to "
                                "be the one NeoForge spells, or the mod's file is a private tag sharing "
                                "nothing with anyone else's. This is what left buckshot unable to use "
                                "vanilla gunpowder and long_scope unable to use vanilla glass "
                                "(referenced by %s)" % (tag, siblings[0], where))
            continue
        # Not ours and not NeoForge's: another mod may well define it (Create defines c:ingots/brass),
        # so this is a note rather than a failure.
        notes.append("%s is referenced by %s but neither NeoForge nor the mod defines it - it only "
                     "resolves if another installed mod ships it" % (tag, where))

    for note in notes:
        print("NOTE   %s" % note)
    for problem in problems:
        print("BROKEN %s" % problem)
    if problems:
        print("\n%d problem(s)" % len(problems))
        return 1
    print("0 problem(s): every common tag the mod ships is NeoForge's own spelling (%d noted as another "
          "mod's)" % len(notes))
    return 0


if __name__ == "__main__":
    sys.exit(main())
