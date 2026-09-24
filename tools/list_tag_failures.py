"""Extract every unresolved tag reference from a server log.

`TagLoader` logs one block per broken tag:

    Couldn't load tag <tag> as it is missing following references:
        <reference> (from mod/<mod>)

An entry here means the tag did not load at all, so anything keyed off it
(recipe ingredients, enchantable lists, IE toolbox tools, ...) silently
misbehaves.  Deduplicated and grouped so the real causes are visible.

Usage:
    python tools/list_tag_failures.py build-logs/server-final2.txt
"""

import re
import sys
from collections import defaultdict

HEAD = re.compile(r"Couldn't load tag (\S+) as it is missing following references:")
REF = re.compile(r"^\s+(\S+)\s+\(from (?:mod|tag)/([^)]+)\)\s*$")


def main():
    path = sys.argv[1] if len(sys.argv) > 1 else "build-logs/server-final2.txt"
    with open(path, encoding="utf-8", errors="replace") as fh:
        lines = fh.read().splitlines()

    broken = defaultdict(set)   # tag -> {missing reference}
    cause = defaultdict(set)    # missing reference -> {tag}

    current = None
    for line in lines:
        m = HEAD.search(line)
        if m:
            current = m.group(1)
            continue
        if current:
            m = REF.match(line)
            if m:
                broken[current].add(m.group(1))
                cause[m.group(1)].add(current)
                continue
            if line.strip():
                current = None

    print("broken tags: %d" % len(broken))
    print("")
    print("=== grouped by MISSING REFERENCE (the actual causes) ===")
    for ref, tags in sorted(cause.items(), key=lambda kv: (-len(kv[1]), kv[0])):
        print("  %-44s breaks %d tag(s)" % (ref, len(tags)))
        for tag in sorted(tags):
            print("        %s" % tag)

    print("")
    print("=== grouped by BROKEN TAG ===")
    for tag, refs in sorted(broken.items()):
        print("  %-44s missing: %s" % (tag, ", ".join(sorted(refs))))


if __name__ == "__main__":
    main()
