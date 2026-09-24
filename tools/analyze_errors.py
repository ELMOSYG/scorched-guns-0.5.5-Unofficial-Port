"""Summarise a javac error log by category so the port can be worked in batches.

usage: python tools/analyze_errors.py build-logs/compile-05.txt [--top N]
"""
from __future__ import annotations

import collections
import re
import sys

ERR = re.compile(r"([A-Za-z]:\\[^:]+\.java):(\d+): (error|warning): (.*)$")
SYM = re.compile(r"symbol:\s+(?:method|variable|class|constructor)\s+(\S+)")


def normalise(msg: str) -> str:
    msg = re.sub(r"'[^']*'", "'X'", msg)
    msg = re.sub(r"\s+", " ", msg).strip()
    return msg[:110]


def main() -> None:
    path = sys.argv[1]
    top = 40
    if "--top" in sys.argv:
        top = int(sys.argv[sys.argv.index("--top") + 1])
    symbols_only = "--symbols" in sys.argv
    if symbols_only:
        top = int(sys.argv[sys.argv.index("--symbols") + 1])
    raw = open(path, "rb").read()
    encoding = "utf-16" if raw[:2] in (b"\xff\xfe", b"\xfe\xff") else "utf-8"
    errors = []
    symbols = collections.Counter()
    files = collections.Counter()
    lines = raw.decode(encoding, "replace").splitlines()
    for i, line in enumerate(lines):
        m = ERR.search(line)
        if not m or m.group(3) != "error":
            continue
        errors.append((m.group(1), int(m.group(2)), m.group(4)))
        files[m.group(1)] += 1
        if "symbol:" in line or any("symbol:" in nxt for nxt in lines[i:i + 5]):
            for nxt in lines[i:i + 5]:
                s = SYM.search(nxt)
                if s:
                    symbols[s.group(1)] += 1
                    break
    print(f"total errors parsed: {len(errors)}")
    if symbols_only:
        for s, n in symbols.most_common(top):
            print(f"{n:5d}  {s}")
        return
    for line in lines:
        if "error" in line and "errors" in line and re.search(r"\d+ errors", line):
            print("javac summary line:", line.strip())
    print(f"\n==== distinct files: {len(files)} ====")
    for f, n in files.most_common(top):
        print(f"{n:5d}  {f.split(chr(92))[-1]}")

    print("\n==== error kinds ====")
    kinds = collections.Counter(normalise(e[2]) for e in errors)
    for k, n in kinds.most_common(top):
        print(f"{n:5d}  {k}")

    print("\n==== missing symbols ====")
    for s, n in symbols.most_common(top):
        print(f"{n:5d}  {s}")


if __name__ == "__main__":
    main()
