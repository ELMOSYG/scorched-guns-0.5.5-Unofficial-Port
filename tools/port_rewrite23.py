"""Twenty-third-stage: keep SoundEvents holders only where the compiler needs them.

1.21 makes *some* SoundEvents constants Holders (the jukebox/music ones) while the
rest stay plain SoundEvent. A blanket `.value()` therefore breaks more than it
fixes, so the previous pass is reverted and re-applied from the compiler log.

usage: python tools/port_rewrite23.py <error-log>
"""
from __future__ import annotations

import collections
import os
import re
import sys

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")
ERR = re.compile(r"([A-Za-z]:\\[^:]+\.java):(\d+): error: (.*)$")


def main() -> None:
    # 1) revert the blanket change
    reverted = 0
    for dirpath, _, filenames in os.walk(SRC):
        for fn in filenames:
            if not fn.endswith(".java"):
                continue
            path = os.path.join(dirpath, fn)
            text = open(path, encoding="utf-8", errors="replace").read()
            if "SoundEvents." not in text or ".value()" not in text:
                continue
            new = re.sub(r"SoundEvents\.(\w+)\.value\(\)", r"SoundEvents.\1", text)
            if new != text:
                with open(path, "w", encoding="utf-8", newline="") as fh:
                    fh.write(new)
                reverted += 1
    print(f"reverted in {reverted} files")

    # 2) re-apply only at the sites the compiler flagged
    log = sys.argv[1] if len(sys.argv) > 1 else None
    if not log:
        return
    raw = open(log, "rb").read()
    encoding = "utf-16" if raw[:2] in (b"\xff\xfe", b"\xfe\xff") else "utf-8"
    lines = raw.decode(encoding, "replace").splitlines()
    fixes: dict[str, set[int]] = collections.defaultdict(set)
    for i, line in enumerate(lines):
        m = ERR.search(line)
        if not m:
            continue
        if "Holder<SoundEvent> cannot be converted to SoundEvent" in m.group(3):
            fixes[os.path.normpath(m.group(1))].add(int(m.group(2)))
    applied = 0
    for path, numbers in fixes.items():
        if not os.path.isfile(path):
            continue
        text = open(path, encoding="utf-8", errors="replace").read()
        file_lines = text.split("\n")
        for number in numbers:
            idx = number - 1
            if 0 <= idx < len(file_lines):
                new = re.sub(r"SoundEvents\.(\w+)(?!\.value)", r"SoundEvents.\1.value()", file_lines[idx])
                if new != file_lines[idx]:
                    file_lines[idx] = new
                    applied += 1
        with open(path, "w", encoding="utf-8", newline="") as fh:
            fh.write("\n".join(file_lines))
    print(f"re-applied at {applied} flagged sites")


if __name__ == "__main__":
    main()
