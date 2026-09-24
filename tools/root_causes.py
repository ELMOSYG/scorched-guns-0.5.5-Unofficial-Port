"""Find missing symbols that appear in several files, i.e. shared root causes.

usage: python tools/root_causes.py <log> [--min N] [--packages]
"""
from __future__ import annotations

import collections
import os
import re
import sys

ERR = re.compile(r"([A-Za-z]:\\[^:]+\.java):(\d+): error: (.*)$")
SYM = re.compile(r"symbol:\s+(?:method|variable|class|constructor|interface)\s+(\S+)")
SRC = r"E:\mod\scgun-0.5.5-1.21.1-neoforge\src\main\java"


def main() -> None:
    log = sys.argv[1]
    minimum = 2
    if "--min" in sys.argv:
        minimum = int(sys.argv[sys.argv.index("--min") + 1])
    raw = open(log, "rb").read()
    encoding = "utf-16" if raw[:2] in (b"\xff\xfe", b"\xfe\xff") else "utf-8"
    lines = raw.decode(encoding, "replace").splitlines()
    where: dict[str, set[str]] = collections.defaultdict(set)
    for i, line in enumerate(lines):
        m = ERR.search(line)
        if not m or "cannot find symbol" not in m.group(3):
            continue
        rel = os.path.relpath(os.path.normpath(m.group(1)), SRC).replace("\\", "/")
        for nxt in lines[i:i + 6]:
            s = SYM.search(nxt)
            if s:
                where[s.group(1)].add(rel)
                break
    rows = sorted(where.items(), key=lambda kv: (-len(kv[1]), kv[0]))
    print("=== missing symbols spanning several files (>= %d) ===" % minimum)
    for sym, files in rows:
        if len(files) < minimum:
            continue
        pkgs = sorted({"/".join(f.split("/")[:-1]) for f in files})
        print("\n%-46s %d files / %d packages" % (sym, len(files), len(pkgs)))
        for f in sorted(files):
            print("      %s" % f)


if __name__ == "__main__":
    main()
