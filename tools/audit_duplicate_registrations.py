"""Find event-handler registrations that happen more than once.

`ClientHandler.init` and `ScorchedGuns` register handlers on the NeoForge bus by
hand.  Registering the same handler class twice means every `@SubscribeEvent`
method runs twice per event, which silently doubles any transform, offset or
sound it applies.  Forge tolerated this; the duplicate lines came from 0.5.5.

Usage:
    python tools/audit_duplicate_registrations.py
"""

import pathlib
import re
from collections import defaultdict

ROOTS = [
    pathlib.Path("src/main/java/top/ribs/scguns/client/ClientHandler.java"),
    pathlib.Path("src/main/java/top/ribs/scguns/ScorchedGuns.java"),
]

REGISTER = re.compile(r"(\w+)\s*\.\s*register\(\s*new\s+([A-Z]\w*)\s*\(\)\s*\)")
REGISTER_CLASS = re.compile(r"(\w+)\s*\.\s*register\(\s*([A-Z]\w*)\.class\s*\)")


def main():
    per_file = {}
    overall = defaultdict(list)
    for path in ROOTS:
        if not path.is_file():
            continue
        hits = defaultdict(list)
        for number, line in enumerate(path.read_text(encoding="utf-8").split("\n"), 1):
            for match in REGISTER.finditer(line):
                hits[match.group(2)].append((number, match.group(1)))
                overall[match.group(2)].append((path.name, number, match.group(1)))
            for match in REGISTER_CLASS.finditer(line):
                hits[match.group(2)].append((number, match.group(1)))
                overall[match.group(2)].append((path.name, number, match.group(1)))
        per_file[path] = hits

    print("=== registered more than once in the SAME file ===")
    found = 0
    for path, hits in per_file.items():
        for handler, places in sorted(hits.items()):
            if len(places) > 1:
                found += 1
                print("  %-24s %-34s %s" % (path.name, handler, places))
    if not found:
        print("  (none)")

    print("")
    print("=== registered more than once ACROSS files ===")
    found = 0
    for handler, places in sorted(overall.items()):
        files = {p[0] for p in places}
        if len(places) > 1 and len(files) > 1:
            found += 1
            print("  %-34s %s" % (handler, places))
    if not found:
        print("  (none)")

    print("")
    print("total distinct handlers registered: %d" % len(overall))


if __name__ == "__main__":
    main()
