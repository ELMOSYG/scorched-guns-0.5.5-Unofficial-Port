"""Remap third-party imports onto the packages that actually exist in the
1.21.1 dependency jars.

GeckoLib dropped its `core` package level in 4.6 (software.bernie.geckolib.core.*
-> software.bernie.geckolib.*), so every import of a moved class becomes a
"package does not exist" error. Rather than hand-editing 55 files, the real
class list of the shipped jar decides the new package: a unique simple name
wins, ties are broken by matching the tail of the old package path.

usage: python tools/fix_thirdparty_imports.py [--apply]
"""
from __future__ import annotations

import collections
import os
import re
import sys
import zipfile

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")
JARS = [
    os.path.join(ROOT, "libs", "geckolib-neoforge-1.21.1-4.9.3.jar"),
    os.path.join(ROOT, "libs", "framework-neoforge-1.21.1-0.13.11.jar"),
    os.path.join(ROOT, "libs", "curios-neoforge-9.5.1+1.21.1.jar"),
]
PREFIXES = ("software.bernie.geckolib.", "com.mrcrayfish.framework.", "top.theillusivec4.curios.")

# Nested types and renames the unique-simple-name rule cannot infer.
MANUAL = {
    "software.bernie.geckolib.util.RenderUtils": "software.bernie.geckolib.util.RenderUtil",
    "software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar":
        "software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar",
    "software.bernie.geckolib.core.animation.Animation.LoopType":
        "software.bernie.geckolib.animation.Animation.LoopType",
    "software.bernie.geckolib.core.animation.AnimationController.State":
        "software.bernie.geckolib.animation.AnimationController.State",
    "software.bernie.geckolib.core.animation.AnimatableManager$ControllerRegistrar":
        "software.bernie.geckolib.animation.AnimatableManager$ControllerRegistrar",
}

IMPORT = re.compile(r"^import\s+(static\s+)?([\w.$]+);\s*$", re.M)


def jar_classes() -> dict[str, list[str]]:
    by_simple = collections.defaultdict(list)
    for jar in JARS:
        if not os.path.isfile(jar):
            continue
        with zipfile.ZipFile(jar) as z:
            for name in z.namelist():
                if not name.endswith(".class"):
                    continue
                fq = name[:-6].replace("/", ".")
                if "$" in fq:
                    continue
                simple = fq.rsplit(".", 1)[1]
                by_simple[simple].append(fq)
    return by_simple


def pick(simple: str, old_pkg: str, by_simple: dict[str, list[str]]) -> str | None:
    cands = by_simple.get(simple)
    if not cands:
        return None
    if len(cands) == 1:
        return cands[0]
    old_tail = old_pkg.split(".")
    best, best_score = None, -1
    for cand in cands:
        parts = cand.split(".")[:-1]
        score = 0
        for a, b in zip(reversed(old_tail), reversed(parts)):
            if a == b:
                score += 1
            else:
                break
        if score > best_score:
            best, best_score = cand, score
    return best


def main() -> None:
    apply = "--apply" in sys.argv
    by_simple = jar_classes()
    all_classes = {c for cands in by_simple.values() for c in cands}
    changes = []
    unknown = []
    for dirpath, _, files in os.walk(SRC):
        for fn in files:
            if not fn.endswith(".java"):
                continue
            path = os.path.join(dirpath, fn)
            text = open(path, encoding="utf-8", errors="replace").read()
            out = text
            for match in IMPORT.finditer(text):
                fq = match.group(2)
                if not fq.startswith(PREFIXES):
                    continue
                pkg, _, simple = fq.rpartition(".")
                if fq in MANUAL:
                    new = MANUAL[fq]
                    changes.append((os.path.relpath(path, ROOT), fq, new))
                    out = out.replace("import %s;" % fq, "import %s;" % new)
                    continue
                if simple and simple[0].isupper() and fq in all_classes:
                    continue  # already valid
                new = pick(simple, pkg, by_simple)
                if new is None:
                    unknown.append((os.path.relpath(path, ROOT), fq))
                    continue
                if new != fq:
                    changes.append((os.path.relpath(path, ROOT), fq, new))
                    out = out.replace("import %s;" % fq, "import %s;" % new)
            if apply and out != text:
                with open(path, "w", encoding="utf-8", newline="") as fh:
                    fh.write(out)
    print(f"{len(changes)} import remaps" + ("" if apply else " (dry run)"))
    for rel, old, new in changes[:60]:
        print(f"  {old}\n    -> {new}")
    if len(changes) > 60:
        print(f"  ... and {len(changes) - 60} more")
    if unknown:
        print(f"\n{len(set(f for _, f in unknown))} imports with no candidate class:")
        for f in sorted(set(f for _, f in unknown)):
            print("  ", f)


if __name__ == "__main__":
    main()
