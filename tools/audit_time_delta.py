"""Find port sites that used to read a per-frame TIME DELTA but now read a partial tick.

1.20.1 Forge exposed two different clocks on `Minecraft`:

    getFrameTime()       -> the partial tick, 0..1, how far we are between ticks
    getDeltaFrameTime()  -> the real frame delta in ticks, ~1.0 at 20 fps

1.21.1 has them under `Timer`:

    getTimer().getGameTimeDeltaPartialTick(false) -> partial tick
    getTimer().getRealtimeDeltaTicks()            -> real delta in ticks

Mapping `getDeltaFrameTime()` onto `getGameTimeDeltaPartialTick(false)` silently
changes the *meaning* of the number: any recovery/decay/accumulation driven by it
then advances at a rate unrelated to real time.  Compiles fine, no warning.

Usage:
    python tools/audit_time_delta.py
"""

import pathlib
import re
from collections import defaultdict

OLD = pathlib.Path(r"E:\mod\SG2-1.21\.sg055_deobf\top\ribs\scguns")
NEW = pathlib.Path("src/main/java/top/ribs/scguns")

DELTA_OLD = re.compile(r"getDeltaFrameTime\s*\(\s*\)")
PARTIAL_OLD = re.compile(r"getFrameTime\s*\(\s*\)")
PARTIAL_NEW = re.compile(r"getGameTimeDeltaPartialTick\s*\(")
# The faithful 1.21 replacement for Minecraft#getDeltaFrameTime(): the elapsed
# ticks this frame, NOT the 0..1 partial tick.
DELTA_NEW = re.compile(r"getGameTimeDeltaTicks\s*\(")


def suffix(path, root):
    return path.relative_to(root).as_posix()


LINE_COMMENT = re.compile(r"//[^\n]*")
BLOCK_COMMENT = re.compile(r"/\*.*?\*/", re.S)


def code_only(text):
    """Blank comments so merely *mentioning* a method name cannot satisfy a check."""
    text = BLOCK_COMMENT.sub(lambda m: re.sub(r"[^\n]", " ", m.group(0)), text)
    return LINE_COMMENT.sub(lambda m: " " * len(m.group(0)), text)


def main():
    # 1. Which 0.5.5 files used the delta, and how often.
    delta_files = defaultdict(list)
    partial_files = defaultdict(list)
    for path in OLD.rglob("*.java"):
        text = path.read_text(encoding="utf-8", errors="replace")
        for number, line in enumerate(text.split("\n"), 1):
            # getFrameTime() also matches getDeltaFrameTime() as a substring; the
            # delta pattern is checked first and the partial one excludes it.
            if DELTA_OLD.search(line):
                delta_files[suffix(path, OLD)].append((number, line.strip()))
            elif PARTIAL_OLD.search(line):
                partial_files[suffix(path, OLD)].append((number, line.strip()))

    print("=== 0.5.5 sites reading the FRAME DELTA (getDeltaFrameTime) ===")
    for name in sorted(delta_files):
        for number, line in delta_files[name]:
            print("  %-46s :%-5d %s" % (name, number, line[:110]))
    print("")
    print("total delta sites: %d across %d file(s)"
          % (sum(len(v) for v in delta_files.values()), len(delta_files)))
    print("")

    print("=== how the port renders those files today ===")
    # Each of these files, and how many former `getDeltaFrameTime()` sites it has.
    expected = {name: len(sites) for name, sites in delta_files.items()}
    problems = 0
    for name in sorted(expected):
        new_path = NEW / name
        if not new_path.is_file():
            print("  %-46s MISSING in port" % name)
            problems += 1
            continue
        text = code_only(new_path.read_text(encoding="utf-8", errors="replace"))
        partial = len(PARTIAL_NEW.findall(text))
        delta_ticks = len(DELTA_NEW.findall(text))
        ok = delta_ticks >= expected[name]
        if not ok:
            problems += 1
        print("  %-46s delta-sites=%d gameTimeDeltaTicks=%d partialTick-left=%d %s"
              % (name, expected[name], delta_ticks, partial, "OK" if ok else "  <-- STILL USING THE PARTIAL TICK"))

    # Guard the other direction: a file with no 0.5.5 delta site must not have
    # gained a gameTimeDeltaTicks read, which would mean an over-application.
    over = []
    for path in NEW.rglob("*.java"):
        rel = path.relative_to(NEW).as_posix()
        if rel in expected:
            continue
        text = code_only(path.read_text(encoding="utf-8", errors="replace"))
        if DELTA_NEW.search(text):
            over.append(rel)
    if over:
        print("")
        print("!!! gameTimeDeltaTicks used in file(s) with no 0.5.5 delta site:")
        for name in over:
            print("    %s" % name)
        problems += len(over)

    print("")
    print("=== 0.5.5 total partial-tick sites (for reference): %d ==="
          % sum(len(v) for v in partial_files.values()))
    print("")
    print("%d problem(s)" % problems)
    print("Rule: a former getDeltaFrameTime() site must read")
    print("      getTimer().getGameTimeDeltaTicks(); a partial tick (0..1) is the")
    print("      wrong quantity there and silently changes the rate of any recovery,")
    print("      decay or accumulation driven by it.")
    return 1 if problems else 0


if __name__ == "__main__":
    import sys
    sys.exit(main())
