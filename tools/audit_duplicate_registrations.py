"""Find event-handler registrations that happen more than once.

`ClientHandler.init` and `ScorchedGuns` register handlers on the NeoForge bus by
hand.  Registering the same handler class twice means every `@SubscribeEvent` method
runs twice per event, which silently doubles any transform, offset or sound it
applies.  Forge tolerated this; the duplicate lines came from 0.5.5, where they were
merely wasteful.

HANDOFF 82.12: this script only understood `new X()` and `X.class`, so it reported
"none" while `BulletTrailRenderingHandler` - registered as `X.get()` in two files -
had every trail ageing twice per client tick and rendering twice per frame, next to a
third copy from LevelRendererMixin.  The `.get()` form (the singleton pattern this mod
uses everywhere) is matched now, and any duplicate is a failure, not a note.

Usage:
    python tools/audit_duplicate_registrations.py
    python tools/audit_duplicate_registrations.py --selftest   # must FAIL against the pre-fix revision
"""

import os
import re
import subprocess
import sys
from collections import defaultdict

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ROOTS = [
    "src/main/java/top/ribs/scguns/client/ClientHandler.java",
    "src/main/java/top/ribs/scguns/ScorchedGuns.java",
]

PRE_FIX_REVISION = "5b53ff4"

REGISTER = re.compile(r"(\w+)\s*\.\s*register\(\s*new\s+([A-Z]\w*)\s*\(\)\s*\)")
REGISTER_CLASS = re.compile(r"(\w+)\s*\.\s*register\(\s*([A-Z]\w*)\.class\s*\)")
REGISTER_GET = re.compile(r"(\w+)\s*\.\s*register\(\s*([A-Z]\w*)\s*\.\s*get\(\)\s*\)")

PATTERNS = (REGISTER, REGISTER_CLASS, REGISTER_GET)


def scan(sources):
    """sources: [(name, text)] -> (per-file hits, overall hits)."""
    per_file = {}
    overall = defaultdict(list)
    for name, text in sources:
        hits = defaultdict(list)
        for number, line in enumerate(text.split("\n"), 1):
            for pattern in PATTERNS:
                for match in pattern.finditer(line):
                    hits[match.group(2)].append((number, match.group(1)))
                    overall[match.group(2)].append((name, number, match.group(1)))
        per_file[name] = hits
    return per_file, overall


def report(per_file, overall):
    """Prints both duplicate lists; returns the number of duplicate registrations."""
    print("=== registered more than once in the SAME file ===")
    found = 0
    for name, hits in per_file.items():
        for handler, places in sorted(hits.items()):
            if len(places) > 1:
                found += 1
                print("  %-24s %-34s %s" % (name, handler, places))
    if not found:
        print("  (none)")

    print("")
    print("=== registered more than once ACROSS files ===")
    for handler, places in sorted(overall.items()):
        files = {p[0] for p in places}
        if len(places) > 1 and len(files) > 1:
            found += 1
            print("  %-34s %s" % (handler, places))
    if found == 0:
        print("  (none)")

    print("")
    print("total distinct handlers registered: %d" % len(overall))
    return found


def current_sources():
    sources = []
    for relative in ROOTS:
        path = os.path.join(ROOT, relative)
        if os.path.isfile(path):
            sources.append((os.path.basename(path), open(path, encoding="utf-8", errors="replace").read()))
    return sources


def git_sources(revision):
    sources = []
    for relative in ROOTS:
        text = subprocess.run(["git", "show", "%s:%s" % (revision, relative)],
                              capture_output=True, text=True, encoding="utf-8", check=True).stdout
        sources.append((os.path.basename(relative), text))
    return sources


def selftest():
    try:
        sources = git_sources(PRE_FIX_REVISION)
    except (subprocess.CalledProcessError, FileNotFoundError) as error:
        print("selftest: cannot read revision %s (%s)" % (PRE_FIX_REVISION, error))
        return 1

    print("selftest: scanning revision %s" % PRE_FIX_REVISION)
    found = report(*scan(sources))
    if found == 0:
        print("selftest FAILED: the duplicate BulletTrailRenderingHandler registration at %s is not detected"
              % PRE_FIX_REVISION)
        return 1
    print("selftest OK: the duplicate .get() registration is detected")
    return 0


def main():
    if "--selftest" in sys.argv:
        return selftest()

    found = report(*scan(current_sources()))
    if found:
        print("\n%d duplicate registration(s): the handlers run once per registration" % found)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
