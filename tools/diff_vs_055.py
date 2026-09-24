"""Content-diff the 0.5.5 decompile against the port for a subpackage.

Byte-size comparison only catches big losses.  This normalises the usual
decompile/port noise (the 1.20.1 -> 1.21 renames this project applied) and then
shows what is actually different, so a silent behaviour change inside a mixin
shows up.

Usage:
    python tools/diff_vs_055.py mixin
    python tools/diff_vs_055.py client/render/pose
"""

import difflib
import pathlib
import re
import sys

OLD = pathlib.Path(r"E:\mod\SG2-1.21\.sg055_deobf\top\ribs\scguns")
NEW = pathlib.Path(r"E:\mod\scgun-0.5.5-1.21.1-neoforge\src\main\java\top\ribs\scguns")

# Mechanical 1.20.1 -> 1.21.1 differences this port applied on purpose.
NOISE = [
    (r"getFrameTime\(\)", "getTimer().getGameTimeDeltaPartialTick(false)"),
    (r"getDeltaFrameTime\(\)", "getTimer().getGameTimeDeltaPartialTick(false)"),
    (r"MinecraftForge\.EVENT_BUS", "NeoForge.EVENT_BUS"),
    (r"net\.minecraftforge", "net.neoforged"),
    (r"\.getTag\(\)", ".NbtHelper$TAG()"),
    (r"stack\.getOrCreateTag\(\)", "THING"),
]


def normalise(text):
    text = re.sub(r"//[^\n]*", "", text)
    text = re.sub(r"[ \t]+", " ", text)
    text = re.sub(r"\n\s*\n+", "\n", text)
    return text


def main():
    subs = sys.argv[1:] or ["mixin"]
    for sub in subs:
        old_root, new_root = OLD / sub, NEW / sub
        if not old_root.is_dir() or not new_root.is_dir():
            print("skip %s (missing on one side)" % sub)
            continue
        print("#" * 78)
        print("# %s" % sub)
        print("#" * 78)
        for old_file in sorted(old_root.rglob("*.java")):
            rel = old_file.relative_to(old_root)
            new_file = new_root / rel
            if not new_file.is_file():
                print("MISSING in port: %s" % rel)
                continue
            a = normalise(old_file.read_text(encoding="utf-8", errors="replace")).splitlines()
            b = normalise(new_file.read_text(encoding="utf-8", errors="replace")).splitlines()
            if a == b:
                continue
            diff = [l for l in difflib.unified_diff(a, b, "0.5.5", "port", lineterm="", n=1)]
            print("")
            print("=== %s (%d differing line(s)) ===" % (rel, len([l for l in diff if l[:1] in "+-"])))
            for line in diff[:400]:
                print("   " + line)
            if len(diff) > 400:
                print("   ... (%d more diff lines)" % (len(diff) - 400))


if __name__ == "__main__":
    main()
