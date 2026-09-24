"""Dump a javac log as grouped, greppable records.

Each record is `rel/path/File.java:line: message`, followed by the offending
source line and any `symbol:` / `location:` detail javac added.

usage: python tools/dump_errors.py <log> <out.txt> [--kind <substring>]
"""
from __future__ import annotations

import collections
import os
import re
import sys

ERR = re.compile(r"([A-Za-z]:\\[^:]+\.java):(\d+): error: (.*)$")
SRC = r"E:\mod\scgun-0.5.5-1.21.1-neoforge\src\main\java"
DETAIL = ("symbol:", "location:", "required:", "found:", "reason:")


def main() -> None:
    log, out = sys.argv[1], sys.argv[2]
    kind = None
    if "--kind" in sys.argv:
        kind = sys.argv[sys.argv.index("--kind") + 1]
    raw = open(log, "rb").read()
    encoding = "utf-16" if raw[:2] in (b"\xff\xfe", b"\xfe\xff") else "utf-8"
    lines = raw.decode(encoding, "replace").splitlines()
    rows = []
    for i, line in enumerate(lines):
        m = ERR.search(line)
        if not m:
            continue
        rel = os.path.relpath(os.path.normpath(m.group(1)), SRC).replace("\\", "/")
        src, details = "", []
        for extra in lines[i + 1:i + 8]:
            t = extra.strip()
            if not t or t == "^" or t.endswith(":"):
                continue
            if t.startswith(DETAIL):
                details.append(t)
                continue
            if not src:
                src = t
        rows.append((rel, int(m.group(2)), m.group(3), src, details))
    if kind:
        rows = [r for r in rows if kind in r[2]]
    rows.sort(key=lambda r: (r[0], r[1]))
    os.makedirs(os.path.dirname(os.path.abspath(out)), exist_ok=True)
    with open(out, "w", encoding="utf-8", newline="\n") as fh:
        for rel, line, msg, src, details in rows:
            fh.write("%s:%d: %s\n" % (rel, line, msg))
            if src:
                fh.write("    %s\n" % src[:190])
            for d in details:
                fh.write("      %s\n" % d[:170])
    print("wrote %d errors to %s" % (len(rows), out))
    kinds = collections.Counter(r[2].split(";")[0][:80] for r in rows)
    for k, v in kinds.most_common(40):
        print("%5d  %s" % (v, k))


if __name__ == "__main__":
    main()
