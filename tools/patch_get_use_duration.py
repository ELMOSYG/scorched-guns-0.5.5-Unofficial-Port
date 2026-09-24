"""Move the getUseDuration overrides onto the 1.21.1 signature.

1.20.1:  public int getUseDuration(ItemStack stack)
1.21.1:  public int getUseDuration(ItemStack stack, LivingEntity entity)

A method whose signature no longer exists in the parent still compiles - it just becomes a
brand new helper nobody calls, and the real method keeps its default. For an item that means a
use duration of 0 ticks, so `startUsingItem` completes on the next tick and `finishUsingItem`
runs immediately.

That is what the reported grenade bug was: right-clicking a grenade ran finishUsingItem at once,
which calls ThrowableGrenadeEntity#onDeath - the grenade cooked off in the player's hand instead
of being thrown. The reference 1.21.1 port uses the two argument form in all of these files.

This patch rewrites each one-argument override, adds @Override (so javac now *fails* if the
signature ever drifts again instead of silently doing nothing) and adds the LivingEntity import
when it is missing. Run tools/audit_stale_overrides.py afterwards - that is the general check.

Usage:
    python tools/patch_get_use_duration.py [--dry-run]
"""

import pathlib
import re
import sys

SOURCE = pathlib.Path("src/main/java/top/ribs/scguns")
OLD = re.compile(r'(?P<indent>[ \t]*)public int getUseDuration\(ItemStack stack\) \{')
NEW_SIGNATURE = ("public int getUseDuration(ItemStack stack, LivingEntity entity) {")
IMPORT = "import net.minecraft.world.entity.LivingEntity;"
CALL = re.compile(r'(?<![\w$])getUseDuration\(stack\)')
METHOD = re.compile(r'(?:public|protected|private)\s+[A-Za-z0-9_<>\[\], .]+\s+\w+\s*\((?P<params>[^)]*)\)\s*\{')
ENTITY_PARAM = re.compile(r'\b(?:LivingEntity|Player)\s+(\w+)\s*(?:,|$)')


def body_range(text, open_brace):
    depth, i = 0, open_brace
    while i < len(text):
        if text[i] == '{':
            depth += 1
        elif text[i] == '}':
            depth -= 1
            if depth == 0:
                return open_brace, i
        i += 1
    return open_brace, len(text)


def fix_calls(text):
    """Pass the entity to the internal getUseDuration calls.

    The old one argument helper was also called from onUseTick and releaseUsing; now that the
    method really overrides the 1.21.1 one, those calls have to name the entity. Returns
    (new text, fixed count, list of call sites that had no entity in scope).
    """
    methods = []
    for match in METHOD.finditer(text):
        start, end = body_range(text, match.end() - 1)
        entity = ENTITY_PARAM.search(match.group('params'))
        methods.append((start, end, entity.group(1) if entity else None))

    fixed, unresolved = 0, []
    out, cursor = [], 0
    for call in CALL.finditer(text):
        holder = next((m for m in methods if m[0] < call.start() < m[1]), None)
        name = holder[2] if holder else None
        if name is None:
            unresolved.append(text.count("\n", 0, call.start()) + 1)
            continue
        out.append(text[cursor:call.start()])
        out.append("getUseDuration(stack, %s)" % name)
        cursor = call.end()
        fixed += 1
    out.append(text[cursor:])
    return "".join(out), fixed, unresolved


def patch(text):
    """-> (new text, number of conversions)"""
    if not OLD.search(text):
        return text, 0

    def repl(match):
        return "%s@Override\n%s%s" % (match.group('indent'), match.group('indent'), NEW_SIGNATURE)

    text, count = OLD.subn(repl, text)
    if IMPORT not in text and "LivingEntity" in text:
        # Insert the import in the import block, keeping the alphabetical-ish grouping the
        # decompiler produced by placing it before the first net.minecraft.world.item import.
        lines = text.splitlines(keepends=True)
        target = next((i for i, line in enumerate(lines)
                       if line.startswith("import net.minecraft.world.item.")), None)
        if target is not None:
            lines.insert(target, IMPORT + "\n")
            text = "".join(lines)
    return text, count


def main():
    dry_run = "--dry-run" in sys.argv
    total, files, calls, unresolved = 0, 0, 0, []
    for path in sorted(SOURCE.rglob("*.java")):
        original = path.read_text(encoding="utf-8")
        patched, count = patch(original)
        patched, call_count, missed = fix_calls(patched)
        if not count and not call_count:
            continue
        files += 1
        total += count
        calls += call_count
        unresolved += ["%s:%d" % (path.name, line) for line in missed]
        print("  %-32s %d method(s), %d call site(s)" % (path.name, count, call_count))
        if not dry_run:
            path.write_text(patched, encoding="utf-8")
    print("%d method(s) and %d call site(s) in %d file(s)%s"
          % (total, calls, files, " (dry run, nothing written)" if dry_run else ""))
    for entry in unresolved:
        print("  UNRESOLVED (no LivingEntity in scope): %s" % entry)
    return 0


if __name__ == "__main__":
    sys.exit(main())
