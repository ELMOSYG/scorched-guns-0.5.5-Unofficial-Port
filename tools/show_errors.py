"""Print javac errors matching a pattern, with the offending source line.

usage: python tools/show_errors.py <log> <regex> [max]
"""
from __future__ import annotations

import re
import sys

ERR = re.compile(r"([A-Za-z]:\\[^:]+\.java):(\d+): error: (.*)$")


def main() -> None:
    log, pattern = sys.argv[1], sys.argv[2]
    limit = 12
    file_filter = None
    rest = sys.argv[3:]
    while rest:
        arg = rest.pop(0)
        if arg == "--file":
            file_filter = rest.pop(0)
        else:
            limit = int(arg)
    raw = open(log, "rb").read()
    encoding = "utf-16" if raw[:2] in (b"\xff\xfe", b"\xfe\xff") else "utf-8"
    lines = raw.decode(encoding, "replace").splitlines()
    rx = re.compile(pattern)
    shown = 0
    i = 0
    while i < len(lines) and shown < limit:
        m = ERR.search(lines[i])
        context = "\n".join(lines[i:i + 5])
        if m and rx.search(context) and (file_filter is None or file_filter in m.group(1)):
            print(f"--- {m.group(1).split(chr(92))[-1]}:{m.group(2)}  {m.group(3)[:100]}")
            for extra in lines[i + 1:i + 5]:
                if extra.strip() and not extra.rstrip().endswith(":"):
                    print("   ", extra.strip()[:150])
            shown += 1
            i += 4
            continue
        i += 1
    if not shown:
        print("no match for", pattern)


if __name__ == "__main__":
    main()
