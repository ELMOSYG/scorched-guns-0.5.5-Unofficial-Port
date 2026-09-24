"""Group javac errors by source directory so the long tail can be split up.

usage: python tools/group_errors.py <log> [--depth N] [--group N]
"""
from __future__ import annotations

import collections
import os
import re
import sys

ERR = re.compile(r"([A-Za-z]:\\[^:]+\.java):(\d+): error: (.*)$")
SRC = r"E:\mod\scgun-0.5.5-1.21.1-neoforge\src\main\java"


def main() -> None:
    log = sys.argv[1]
    depth = 4
    if "--depth" in sys.argv:
        depth = int(sys.argv[sys.argv.index("--depth") + 1])
    raw = open(log, "rb").read()
    encoding = "utf-16" if raw[:2] in (b"\xff\xfe", b"\xfe\xff") else "utf-8"
    lines = raw.decode(encoding, "replace").splitlines()
    per_dir: collections.Counter = collections.Counter()
    per_file: collections.Counter = collections.Counter()
    for line in lines:
        m = ERR.search(line)
        if not m:
            continue
        rel = os.path.relpath(os.path.normpath(m.group(1)), SRC).replace("\\", "/")
        parts = rel.split("/")
        key = "/".join(parts[:depth]) if len(parts) > depth else "/".join(parts[:-1])
        per_dir[key] += 1
        per_file[rel] += 1
    total = sum(per_dir.values())
    print("total %d errors over %d files\n" % (total, len(per_file)))
    print("=== by directory (depth %d) ===" % depth)
    for k, v in per_dir.most_common():
        print("%5d  %s" % (v, k))
    print("\n=== worst files ===")
    for k, v in per_file.most_common(30):
        print("%5d  %s" % (v, k))


if __name__ == "__main__":
    main()
