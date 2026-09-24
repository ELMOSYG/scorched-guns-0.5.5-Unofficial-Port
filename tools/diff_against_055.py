"""Show the real differences between a 0.5.5 source file and its ported 1.21.1 copy.

The port kept the decompiled formatting, so a line-by-line comparison is readable and
exactly what is needed for "this feature behaves differently" reports: the interesting lines
are the ones that were changed by hand during the port, not the API renames.

Usage:
    python tools/diff_against_055.py <relative/path/Under/top/ribs/scguns> [...]
"""

import difflib
import pathlib
import sys

ORIGINAL = pathlib.Path(r"E:\mod\SG2-1.21\.sg055_deobf\top\ribs\scguns")
PORTED = pathlib.Path("src/main/java/top/ribs/scguns")


def show(relative):
    original = (ORIGINAL / relative).read_text(encoding="utf-8", errors="replace").splitlines()
    ported = (PORTED / relative).read_text(encoding="utf-8", errors="replace").splitlines()
    diff = [line for line in difflib.unified_diff(original, ported, "0.5.5", "port", n=1,
                                                  lineterm="")
            if line[:1] in "+-" and line[:3] not in ("+++", "---")]
    print("=== %s  (%d changed lines)" % (relative, len(diff)))
    for line in diff:
        print("   %s" % line)
    print("")


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        return 2
    for relative in sys.argv[1:]:
        show(relative)
    return 0


if __name__ == "__main__":
    sys.exit(main())
