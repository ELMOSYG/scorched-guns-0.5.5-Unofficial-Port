"""Print a compact ours-vs-super table for the ARITY-MISMATCH findings.

Reads the audit's own output so the two stay in sync.

Usage:
    python tools/audit_override_drift.py > build-logs/drift.txt 2>&1
    python tools/audit_override_drift.py 2>&1 | python tools/drift_table.py
"""

import re
import sys
from collections import defaultdict

HEAD = re.compile(r"^(\S+\.java)\s*:(\d+)\s+(\S+#\w+)\s+ARITY-MISMATCH")
OURS = re.compile(r"^\s+ours: (\S+)$")
SUPER = re.compile(r"^\s+super: (\S+?)\.(\(.*)$")


def main():
    groups = defaultdict(list)
    cur = None
    for line in sys.stdin:
        line = line.rstrip("\n")
        m = HEAD.match(line)
        if m:
            cur = {"rel": m.group(1), "line": int(m.group(2)), "key": m.group(3),
                   "ours": "", "super": ""}
            continue
        if cur is None:
            continue
        m = OURS.match(line)
        if m:
            cur["ours"] = m.group(1)
            continue
        m = SUPER.match(line)
        if m:
            cur["super"] = m.group(2)
            groups[cur["key"].split("#")[-1]].append(cur)
            cur = None

    for name in sorted(groups, key=lambda k: (-len(groups[k]), k)):
        entries = groups[name]
        print("=" * 78)
        print("%s   (%d occurrence(s))" % (name, len(entries)))
        print("  ours : %s" % entries[0]["ours"])
        print("  super: %s" % entries[0]["super"])
        print("  files:")
        for e in entries:
            print("      %-58s:%d" % (e["rel"], e["line"]))
    print("=" * 78)
    print("total: %d method name(s), %d finding(s)"
          % (len(groups), sum(len(v) for v in groups.values())))


if __name__ == "__main__":
    main()
