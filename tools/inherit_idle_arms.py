"""Copy the idle arm tracks into every melee animation that has none.

GeckoLib blends animations per file, not per bone, so a melee animation that only
keys `gun_body` leaves both arms frozen in their bind pose for its whole length -
which reads as the character's arms locking up mid-stab. There is no "inherit this
bone from that animation" in the format, so the tracks are copied in as data.

Which track is copied, and from where:

  * from `idle` - it is the pose the arms already hold while the gun is out, so a
    stab that keeps them in it looks like the gun is being driven by the arms
    rather than by a floating model;
  * into every melee animation present in the file (`bayonet`, `carbine_bayonet`,
    `melee`, `carbine_melee`), and only for bones the animation does not already
    key - a hand-authored arm track is never overwritten.

Idempotent: running it twice changes nothing. `--dry-run` reports without writing.

    python tools/inherit_idle_arms.py [--dry-run] [--path <dir>]
"""

from __future__ import annotations

import argparse
import copy
import json
import pathlib
import sys

ARMS = ("right_arm", "left_arm")
MELEE_ANIMATIONS = ("bayonet", "carbine_bayonet", "melee", "carbine_melee")

DEFAULT_PATH = pathlib.Path("src/main/resources/assets/scguns/animations/item")


def inherit(document: dict) -> list[str]:
    """Copies the missing arm tracks in place; returns what was copied, as "file: anim: bone"."""
    animations = document.get("animations")
    if not isinstance(animations, dict):
        return []

    idle_bones = animations.get("idle", {}).get("bones", {})
    if not isinstance(idle_bones, dict):
        return []

    copied: list[str] = []
    for name in MELEE_ANIMATIONS:
        animation = animations.get(name)
        if not isinstance(animation, dict):
            continue

        bones = animation.setdefault("bones", {})
        for arm in ARMS:
            if arm in bones or arm not in idle_bones:
                continue
            bones[arm] = copy.deepcopy(idle_bones[arm])
            copied.append("{}: {}".format(name, arm))
    return copied


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dry-run", action="store_true", help="report without writing")
    parser.add_argument("--path", default=str(DEFAULT_PATH), help="animation directory")
    args = parser.parse_args(argv)

    root = pathlib.Path(args.path)
    if not root.is_dir():
        print("no such directory: {}".format(root))
        return 2

    files = changed = 0
    for path in sorted(root.glob("*.animation.json")):
        files += 1
        try:
            document = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, ValueError) as error:
            print("{}: unreadable ({})".format(path.name, error))
            continue

        copied = inherit(document)
        if not copied:
            continue

        changed += 1
        print("{}: {}".format(path.name, ", ".join(copied)))
        if not args.dry_run:
            path.write_text(json.dumps(document, ensure_ascii=False, indent="\t") + "\n", encoding="utf-8")

    print("{} file(s) scanned, {} updated{}".format(files, changed, " (dry run)" if args.dry_run else ""))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
