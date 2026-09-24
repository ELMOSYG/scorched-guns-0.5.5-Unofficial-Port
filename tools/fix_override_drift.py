#!/usr/bin/env python3
"""Repair methods whose signature drifted and therefore stopped overriding.

`tools/audit_override_drift.py` finds these, but it reports only the *root* of
each drift family: a subclass that inherits the already-wrong declaration
compares equal to it and looks clean.  So this tool fixes the whole family and
adds the `@Override` that would have caught the drift at compile time.

Two families exist in this port:

1. ``appendHoverText`` -- 1.21.1 vanilla is
   ``(ItemStack, Item.TooltipContext, List<Component>, TooltipFlag)``
   (verified: ``javap net.minecraft.world.item.Item`` on
   build/moddev/artifacts/neoforge-21.1.249-merged.jar).  The port kept the
   1.20.1 order, ``(..., TooltipFlag, List<Component>)``, so the method became a
   plain overload and **no item tooltip ever rendered**.

2. ``getCloneItemStack`` -- 1.21.1 vanilla is
   ``(LevelReader, BlockPos, BlockState)``; the port used ``BlockGetter``.
   ``LevelReader extends BlockGetter``, so the override cannot happen in that
   direction and these blocks returned the vanilla clone item.

Usage:
    python tools/fix_override_drift.py --check
    python tools/fix_override_drift.py
    python tools/fix_override_drift.py --selftest
"""

import argparse
import pathlib
import re
import sys

SRC = pathlib.Path("src/main/java/top/ribs/scguns")

# `TooltipFlag x` and `List<Component> y`, each optionally preceded by an
# annotation such as `@NotNull`.  Captured whole so the pair can be swapped.
ANNOT = r"(?:@\w+(?:\([^)]*\))?\s+)*"
FLAG_PARAM = ANNOT + r"TooltipFlag\s+\w+"
LIST_PARAM = ANNOT + r"List<Component>\s+\w+"

HOVER_OK = re.compile(
    r"appendHoverText\s*\(\s*" + ANNOT + r"ItemStack[^,]*,\s*[^,]*,\s*" + LIST_PARAM + r"\s*,\s*" + FLAG_PARAM
)
HOVER_DRIFT = re.compile(
    r"(public\s+void\s+appendHoverText\s*\(\s*"
    + ANNOT
    + r"ItemStack[^,]*,\s*[^,]*,\s*)("
    + FLAG_PARAM
    + r")(\s*,\s*)("
    + LIST_PARAM
    + r")(\s*\))"
)

CLONE_DRIFT = re.compile(
    r"(public\s+ItemStack\s+getCloneItemStack\s*\(\s*)BlockGetter(\s+\w+\s*,\s*BlockPos\s+\w+\s*,\s*BlockState\s+\w+\s*\))"
)
# The five-argument form is NOT drift: it overrides NeoForge's
# `net.neoforged.neoforge.common.extensions.IBlockExtension#getCloneItemStack`
# (BlockState, HitResult, LevelReader, BlockPos, Player), which NeoForge
# injects into Block.  Verified by extracting Block.class from
# build/moddev/artifacts/neoforge-21.1.249-merged.jar: the 3-argument vanilla
# descriptor is present once and the 5-argument one is absent from Block.class
# and present in IBlockExtension.class, so the 5-argument override resolves
# through the injected interface.  It is therefore sound and is only counted,
# never rewritten -- an earlier revision wrongly reported it as dead code.
CLONE_NEofORGE_EXT = re.compile(r"public\s+ItemStack\s+getCloneItemStack\s*\(\s*BlockState\s+\w+\s*,\s*HitResult\b")

METHOD_LINE = re.compile(r"^\s*(?:public|protected)\s")


def ensure_override(lines, index):
    """Insert `@Override` before the declaration at *index* when missing."""
    indent = re.match(r"(\s*)", lines[index]).group(1)
    limit = max(0, index - 3)
    for probe in range(index - 1, limit - 1, -1):
        stripped = lines[probe].strip()
        if stripped == "@Override":
            return False
        if stripped and not stripped.startswith("@"):
            break
    lines.insert(index, indent + "@Override")
    return True


def fix_text(text):
    """Return (new_text, applied_dict, notes)."""
    applied = {
        "hover_swapped": 0,
        "hover_override": 0,
        "clone_fixed": 0,
        "clone_import": 0,
        "imports_cleaned": 0,
    }
    notes = []

    lines = text.split("\n")
    out = []
    for line in lines:
        if CLONE_NEofORGE_EXT.search(line):
            notes.append(line.strip())

        match = HOVER_DRIFT.search(line)
        if match:
            # Rebuild only the matched span.  Splicing the groups back together
            # and assigning that to the whole line would silently drop the
            # method body opener (" {") and the leading indentation, which the
            # regex does not cover because it starts at "public".
            swapped = (
                match.group(1)
                + match.group(4)
                + match.group(3)
                + match.group(2)
                + match.group(5)
            )
            line = line[: match.start()] + swapped + line[match.end():]
            applied["hover_swapped"] += 1
            out.append(line)
            continue

        match = CLONE_DRIFT.search(line)
        if match:
            line = CLONE_DRIFT.sub(r"\1LevelReader\2", line)
            applied["clone_fixed"] += 1
            out.append(line)
            continue

        out.append(line)

    lines = out

    # Add @Override to every declaration we repaired that lacks one.
    index = 0
    while index < len(lines):
        line = lines[index]
        is_repaired = HOVER_OK.search(line) or re.search(
            r"public\s+ItemStack\s+getCloneItemStack\s*\(\s*LevelReader\b", line
        )
        if is_repaired and METHOD_LINE.match(line):
            if ensure_override(lines, index):
                applied["hover_override"] += 1
                index += 1
        index += 1

    text = "\n".join(lines)

    # The signature now says LevelReader.  Merely renaming the BlockGetter
    # import would break a file that still uses BlockGetter elsewhere, so make
    # sure LevelReader is imported and let normalize_imports() drop whatever
    # became unused or duplicated.
    if applied["clone_fixed"]:
        text, added = ensure_import(text, "net.minecraft.world.level.LevelReader")
        applied["clone_import"] += added

    text, applied["imports_cleaned"] = normalize_imports(text, DROPPABLE_IMPORTS)
    return text, applied, notes


def ensure_import(text, fqcn):
    """Add `import <fqcn>;` when absent.  Returns (text, added_count)."""
    line = "import %s;" % fqcn
    if re.search(r"^\s*%s\s*$" % re.escape(line), text, flags=re.M):
        return text, 0

    lines = text.split("\n")
    last_import = -1
    for index, candidate in enumerate(lines):
        if IMPORT_LINE.match(candidate):
            last_import = index
    if last_import < 0:
        # No import block: put it after the package declaration, if any.
        insert_at = 0
        for index, candidate in enumerate(lines):
            if candidate.startswith("package "):
                insert_at = index + 1
                break
        lines.insert(insert_at, line)
        return "\n".join(lines), 1

    lines.insert(last_import + 1, line)
    return "\n".join(lines), 1


IMPORT_LINE = re.compile(r"^\s*import\s+(static\s+)?[\w.$*]+;\s*$")


def normalize_imports(text, drop_dead=()):
    """Drop duplicate imports, plus any import named in *drop_dead* that is
    no longer referenced outside the import block.

    `drop_dead` is an explicit allowlist on purpose.  Removing every unused
    import would sweep up ~226 unrelated lines across the tree (verified: the
    repo is decompile-derived and carries plenty of dead imports), which turns a
    targeted correctness fix into an unreviewable diff.  Only imports this tool
    itself invalidated are eligible.
    """
    lines = text.split("\n")
    seen = set()
    kept = []
    for line in lines:
        stripped = line.strip()
        if IMPORT_LINE.match(line) and stripped in seen:
            continue
        if IMPORT_LINE.match(line):
            seen.add(stripped)
        kept.append(line)
    removed = len(lines) - len(kept)

    body = "\n".join(l for l in kept if not IMPORT_LINE.match(l))
    result = []
    for line in kept:
        stripped = line.strip()
        if IMPORT_LINE.match(line) and not stripped.startswith("import static"):
            fqcn = stripped[len("import ") : -1]
            simple = fqcn.rsplit(".", 1)[-1]
            if fqcn in drop_dead and simple != "*" and not re.search(r"\b%s\b" % re.escape(simple), body):
                removed += 1
                continue
        result.append(line)

    return "\n".join(result), removed


# Imports this tool may invalidate.
DROPPABLE_IMPORTS = ("net.minecraft.world.level.BlockGetter",)


def run(check_only):
    if not SRC.exists():
        print("ERROR: %s not found; run from the repo root" % SRC)
        return 2

    totals = {
        "hover_swapped": 0,
        "hover_override": 0,
        "clone_fixed": 0,
        "clone_import": 0,
        "imports_cleaned": 0,
    }
    touched = []
    neo_ext = []

    for path in sorted(SRC.rglob("*.java")):
        text = path.read_text(encoding="utf-8")
        new_text, applied, notes = fix_text(text)
        for note in notes:
            neo_ext.append((str(path), note))
        if not any(applied.values()):
            continue
        touched.append((str(path), {k: v for k, v in applied.items() if v}))
        for key in totals:
            totals[key] += applied[key]
        if not check_only:
            path.write_text(new_text, encoding="utf-8")

    print("appendHoverText parameters swapped    : %d" % totals["hover_swapped"])
    print("@Override added                       : %d" % totals["hover_override"])
    print("getCloneItemStack BlockGetter->LevelReader: %d" % totals["clone_fixed"])
    print("imports rewritten                     : %d" % totals["clone_import"])
    print("duplicate/dead imports removed        : %d" % totals["imports_cleaned"])
    print("files %s: %d" % ("needing changes" if check_only else "rewritten", len(touched)))
    for name, applied in touched:
        print("    %-78s %s" % (name, applied))

    if neo_ext:
        print("")
        print(
            "INFO: %d five-argument getCloneItemStack declaration(s) found. These are"
            % len(neo_ext)
        )
        print("VALID: they override NeoForge's")
        print("net.neoforged.neoforge.common.extensions.IBlockExtension#getCloneItemStack")
        print("(BlockState, HitResult, LevelReader, BlockPos, Player), which NeoForge")
        print("injects into Block. Left untouched on purpose:")
        for name, note in neo_ext:
            print("    %s\n        %s" % (name, note))

    return 0


def selftest():
    failures = []

    def check(label, got, want):
        if got != want:
            failures.append("%s: got %r want %r" % (label, got, want))

    # The real drifted shape must be swapped.
    src = (
        "public class A {\n"
        "   public void appendHoverText(ItemStack stack, Item.TooltipContext w, TooltipFlag flag, List<Component> tooltip) {\n"
        "      tooltip.add(flag.isAdvanced() ? X : Y);\n"
        "   }\n"
        "}\n"
    )
    out, applied, _ = fix_text(src)
    check("swap applied", applied["hover_swapped"], 1)
    check(
        "order corrected",
        "Item.TooltipContext w, List<Component> tooltip, TooltipFlag flag" in out,
        True,
    )
    check("@Override added", "@Override" in out, True)
    # The body must be untouched: the parameter NAMES moved with their types.
    check("body untouched", "tooltip.add(flag.isAdvanced() ? X : Y);" in out, True)
    # Regression guards: an earlier revision rebuilt the whole line from the
    # regex groups and silently ate both of these.
    check("body opener preserved", "TooltipFlag flag) {" in out, True)
    check("indentation preserved", "\n   public void appendHoverText(" in out, True)
    check("@Override indented", "\n   @Override\n   public void appendHoverText(" in out, True)
    check("no line was dropped", out.count("\n"), src.count("\n") + 1)

    # An already-correct declaration must be left alone except for @Override.
    ok = (
        "public class B {\n"
        "   public void appendHoverText(ItemStack s, Item.TooltipContext w, List<Component> t, TooltipFlag f) {\n"
        "   }\n"
        "}\n"
    )
    out, applied, _ = fix_text(ok)
    check("correct not swapped", applied["hover_swapped"], 0)
    check("correct gets @Override", "@Override" in out, True)

    # An existing @Override must not be duplicated.
    with_override = (
        "public class C {\n"
        "   @Override\n"
        "   public void appendHoverText(ItemStack s, Item.TooltipContext w, TooltipFlag f, List<Component> t) {\n"
        "   }\n"
        "}\n"
    )
    out, _applied, _ = fix_text(with_override)
    check("no duplicate @Override", out.count("@Override"), 1)

    # Annotated parameters must swap as a unit with their annotations.
    annotated = (
        "   public void appendHoverText(@NotNull ItemStack stack, Item.TooltipContext level, "
        "@NotNull TooltipFlag isAdvanced, @NotNull List<Component> tooltipComponents) {\n"
    )
    out, applied, _ = fix_text(annotated)
    check("annotated swapped", applied["hover_swapped"], 1)
    check(
        "annotations stay with their parameter",
        "@NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced" in out,
        True,
    )

    # getCloneItemStack: type change and import rewrite.
    clone = (
        "import net.minecraft.world.level.BlockGetter;\n"
        "\n"
        "public class D {\n"
        "   public ItemStack getCloneItemStack(BlockGetter world, BlockPos pos, BlockState state) {\n"
        "      return ItemStack.EMPTY;\n"
        "   }\n"
        "}\n"
    )
    out, applied, _ = fix_text(clone)
    check("clone type fixed", applied["clone_fixed"], 1)
    check("LevelReader used", "getCloneItemStack(LevelReader world, BlockPos pos, BlockState state)" in out, True)
    check("import rewritten", "import net.minecraft.world.level.LevelReader;" in out, True)
    check("old import gone", "import net.minecraft.world.level.BlockGetter;" not in out, True)

    # A different use of BlockGetter must keep its import, and LevelReader must
    # be imported in addition -- renaming would leave BlockGetter unresolved.
    keeps_import = (
        "import net.minecraft.world.level.BlockGetter;\n"
        "public class E {\n"
        "   public ItemStack getCloneItemStack(BlockGetter w, BlockPos p, BlockState s) { return e(w); }\n"
        "   BlockGetter other;\n"
        "}\n"
    )
    out, _applied, _ = fix_text(keeps_import)
    check("import kept when still used", "import net.minecraft.world.level.BlockGetter;" in out, True)
    check("LevelReader import added", "import net.minecraft.world.level.LevelReader;" in out, True)

    # When LevelReader is ALREADY imported, the unused BlockGetter import must be
    # dropped, and no duplicate LevelReader import may remain.
    already = (
        "import net.minecraft.world.level.BlockGetter;\n"
        "import net.minecraft.world.level.ItemLike;\n"
        "import net.minecraft.world.level.LevelReader;\n"
        "public class F {\n"
        "   ItemLike used;\n"
        "   public ItemStack getCloneItemStack(BlockGetter w, BlockPos p, BlockState s) { return r(w); }\n"
        "}\n"
    )
    out, _applied, _ = fix_text(already)
    check("no duplicate LevelReader import", out.count("import net.minecraft.world.level.LevelReader;"), 1)
    check("dead BlockGetter import removed", "import net.minecraft.world.level.BlockGetter;" not in out, True)
    check("unrelated import kept", "import net.minecraft.world.level.ItemLike;" in out, True)

    # normalize_imports must remove a pre-existing duplicate import.
    dupe = "import java.util.List;\nimport java.util.List;\npublic class G { List<?> l; }\n"
    out, removed = normalize_imports(dupe)
    check("duplicate import removed", out.count("import java.util.List;"), 1)
    check("duplicate removal counted", removed >= 1, True)

    # The NeoForge 5-arg form is a VALID override and must never be rewritten.
    neo_ext_src = (
        "   public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader w, BlockPos p, Player pl) {\n"
    )
    out, applied, notes = fix_text(neo_ext_src)
    check("neoform ext form noted", len(notes), 1)
    check("neoform ext form untouched", applied["clone_fixed"], 0)
    check("neoform ext text preserved", out.rstrip("\n") == neo_ext_src.rstrip("\n"), True)

    # Idempotence.
    once, _a, _l = fix_text(src)
    twice, applied, _l = fix_text(once)
    check("idempotent", twice, once)
    check("idempotent no-op", sum(applied.values()), 0)

    if failures:
        print("SELFTEST FAILED (%d)" % len(failures))
        for failure in failures:
            print("  - %s" % failure)
        return 1

    print("SELFTEST OK (swap, @Override insertion, import rewrite and legacy detection)")
    return 0


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--selftest", action="store_true")
    args = parser.parse_args()

    if args.selftest:
        return selftest()
    return run(args.check)


if __name__ == "__main__":
    sys.exit(main())
