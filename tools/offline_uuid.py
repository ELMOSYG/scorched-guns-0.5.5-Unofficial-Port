#!/usr/bin/env python
"""Turn a player name into the UUID an offline-mode server used for them.

An offline server derives the UUID from the name alone:

    uuid = MD5("OfflinePlayer:" + name)   with version 3 / variant 1 bits patched in

so a UUID cannot be reversed into a name, but a candidate name can be verified. That is what this
does: given names, print their UUIDs; given a UUID and a name, say whether they match. Use it with the
player table in a world archive to work out who an old UUID belonged to.

usage:
    python tools/offline_uuid.py Notch Steve          # print the offline UUID for each name
    python tools/offline_uuid.py --check 063632aa-3f98-49f0-9bcf-0b8bcc3baf3d SomeName
    python tools/offline_uuid.py --check-file names.txt <uuid>

Exit code 0 when every requested operation succeeded, 1 when a check failed.
"""

from __future__ import annotations

import argparse
import hashlib
import sys


def offline_uuid(name: str) -> str:
    """The UUID an offline-mode server assigns to ``name``."""
    digest = bytearray(hashlib.md5(("OfflinePlayer:" + name).encode("utf-8")).digest())
    digest[6] = (digest[6] & 0x0F) | 0x30      # version 3
    digest[8] = (digest[8] & 0x3F) | 0x80      # variant 1
    text = digest.hex()
    return "%s-%s-%s-%s-%s" % (text[0:8], text[8:12], text[12:16], text[16:20], text[20:32])


def main() -> int:
    parser = argparse.ArgumentParser(description="Offline-mode UUID helper.")
    parser.add_argument("names", nargs="*", help="player names to convert")
    parser.add_argument("--check", nargs=2, metavar=("UUID", "NAME"), help="verify a UUID against a name")
    parser.add_argument("--check-file", nargs=2, metavar=("FILE", "UUID"), help="find which name in FILE has this UUID")
    args = parser.parse_args()

    if args.check:
        uuid, name = args.check
        expected = offline_uuid(name)
        match = uuid.lower() == expected.lower()
        print("%s  <- %s" % (expected, name))
        print("%s" % ("MATCH" if match else "NO MATCH"))
        return 0 if match else 1

    if args.check_file:
        path, uuid = args.check_file
        try:
            with open(path, encoding="utf-8", errors="replace") as handle:
                candidates = [line.strip() for line in handle if line.strip()]
        except OSError as exc:
            print("cannot read %s: %s" % (path, exc))
            return 1
        for name in candidates:
            if offline_uuid(name).lower() == uuid.lower():
                print("%s  <- %s" % (uuid, name))
                return 0
        print("no name in %s matches %s" % (path, uuid))
        return 1

    if not args.names:
        parser.print_help()
        return 0

    for name in args.names:
        print("%s  %s" % (offline_uuid(name), name))
    return 0


if __name__ == "__main__":
    sys.exit(main())
