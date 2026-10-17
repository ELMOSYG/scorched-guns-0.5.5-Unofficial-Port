#!/usr/bin/env python
"""Back up the per-instance files that a failed mixin injection resets.

When a mixin fails to apply the game aborts the load, and several of the files below are written back
with defaults as a result. `options.txt` is the painful one: keybinds, FOV, GUI scale, particle level,
render distance and language all go back to stock, and there is no undo inside the game.

Two files that show up only after Sodium / Iris have been configured are included so a later failure
cannot silently lose them either.

usage:
    python tools/backup_game_options.py                       # newest instance under D:\\MCJAVA\\.minecraft\\versions
    python tools/backup_game_options.py --instance <path>     # a specific instance directory
    python tools/backup_game_options.py --list                # show the backups that exist
    python tools/backup_game_options.py --keep 40             # how many backups to retain (default 20)
    python tools/backup_game_options.py --restore <backup>    # copy a backup back into the instance

Exit code 0 on success, 1 when nothing could be backed up.
"""

from __future__ import annotations

import argparse
import datetime
import os
import shutil
import sys
from pathlib import Path

DEFAULT_VERSIONS = Path(r"D:\MCJAVA\.minecraft\versions")

# Files and directories worth keeping. Anything missing is skipped silently: not every instance has
# Sodium or Iris installed.
TARGETS = (
    "options.txt",
    "optionsof.txt",
    "iris.properties",
    "servers.dat",
    "config",
)


def find_default_instance() -> Path | None:
    if not DEFAULT_VERSIONS.is_dir():
        return None

    # Newest instance that already has an options.txt: that is the one being played.
    candidates = [
        p for p in DEFAULT_VERSIONS.iterdir() if p.is_dir() and (p / "options.txt").is_file()
    ]
    if not candidates:
        return None
    return max(candidates, key=lambda p: (p / "options.txt").stat().st_mtime)


def backup(instance: Path, keep: int) -> int:
    root = instance / "options-backups"
    stamp = datetime.datetime.now().strftime("%Y%m%d-%H%M%S")
    target = root / stamp
    target.mkdir(parents=True, exist_ok=True)

    copied: list[str] = []
    for name in TARGETS:
        source = instance / name
        if not source.exists():
            continue
        destination = target / name
        if source.is_dir():
            shutil.copytree(source, destination)
        else:
            shutil.copy2(source, destination)
        copied.append(name)

    if not copied:
        print("nothing to back up in %s" % instance)
        shutil.rmtree(target, ignore_errors=True)
        return 1

    print("instance : %s" % instance)
    print("backup   : %s" % target)
    for name in copied:
        full = target / name
        if full.is_dir():
            count = sum(1 for _ in full.rglob("*") if _.is_file())
            print("  %-16s directory, %d files" % (name, count))
        else:
            print("  %-16s %d bytes" % (name, full.stat().st_size))

    # keep only the newest `keep` backups
    existing = sorted((p for p in root.iterdir() if p.is_dir()), key=lambda p: p.name, reverse=True)
    for old in existing[keep:]:
        shutil.rmtree(old, ignore_errors=True)
        print("  pruned old backup %s" % old.name)
    return 0


def list_backups(instance: Path) -> int:
    root = instance / "options-backups"
    if not root.is_dir():
        print("no backups yet under %s" % root)
        return 0

    for entry in sorted((p for p in root.iterdir() if p.is_dir()), key=lambda p: p.name, reverse=True):
        files = [p.name for p in entry.iterdir()]
        print("%s   %s" % (entry.name, ", ".join(sorted(files))))
    return 0


def restore(instance: Path, name: str) -> int:
    root = instance / "options-backups"
    source = Path(name)
    if not source.is_absolute():
        source = root / name
    if not source.is_dir():
        print("backup not found: %s" % source)
        return 1

    for entry in source.iterdir():
        destination = instance / entry.name
        if entry.is_dir():
            if destination.exists():
                shutil.rmtree(destination)
            shutil.copytree(entry, destination)
        else:
            shutil.copy2(entry, destination)
        print("restored %s" % entry.name)
    print("restored from %s" % source)
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="Back up options.txt and friends before testing mixins.")
    parser.add_argument("--instance", help="instance directory (default: newest one that has options.txt)")
    parser.add_argument("--keep", type=int, default=20, help="how many backups to retain (default 20)")
    parser.add_argument("--list", action="store_true", help="list existing backups")
    parser.add_argument("--restore", metavar="BACKUP", help="restore a backup by folder name")
    args = parser.parse_args()

    instance = Path(args.instance) if args.instance else find_default_instance()
    if instance is None or not instance.is_dir():
        print("could not find an instance; pass --instance <path>")
        return 1

    if args.list:
        return list_backups(instance)
    if args.restore:
        return restore(instance, args.restore)
    return backup(instance, max(1, args.keep))


if __name__ == "__main__":
    sys.exit(main())
