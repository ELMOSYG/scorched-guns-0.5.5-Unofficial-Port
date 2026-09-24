"""Print every rejected data entry from a server log, one compact line each."""

import re
import sys

HEAD = re.compile(r"Parsing error loading recipe (\S+)$")


def main():
    path = sys.argv[1]
    with open(path, encoding="utf-8", errors="replace") as fh:
        lines = fh.read().splitlines()

    rows = []
    for i, line in enumerate(lines):
        m = HEAD.search(line.rstrip())
        if not m:
            continue
        msg = ""
        for j in range(i + 1, min(i + 5, len(lines))):
            if "Exception" in lines[j]:
                msg = lines[j].strip()
                break
        rows.append((m.group(1), msg))

    seen = set()
    for rid, msg in rows:
        if rid in seen:
            continue
        seen.add(rid)
        short = msg.replace("com.google.gson.JsonParseException: ", "")
        print("%-50s %s" % (rid, short[:170]))
    print("\ntotal distinct: %d" % len(seen))


if __name__ == "__main__":
    main()
