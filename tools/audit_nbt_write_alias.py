#!/usr/bin/env python3
"""Audit: every in-place write to an item's custom-data tag must go through a tag
that belongs to that stack alone.

Why this matters in 1.21: ItemStack.copy() (used by AbstractContainerMenu's
remoteSlots shadow, by ServerEntity's last-sent equipment, ...) copies the
PatchedDataComponentMap **shallowly** -- the copy shares the very same CustomData
instance, hence the very same CompoundTag. ItemStack.matches() then compares
components by value, so an in-place mutation of the shared tag is invisible on
both sides and no update packet is ever sent: the server has the change and the
client never learns about it. Players see this as "drop the item and pick it up
again to sync". 1.20.1 could not hit it because ItemStack.copy() deep-copied the
tag.

Therefore:
  * NbtHelper.getTag()        -> read-only, must never be mutated;
  * NbtHelper.getOrCreateTag()/ getTagForWrite() -> hand out a private copy and
    are the only accessors a write may go through;
  * a local bound from getOrCreateTag()/getTagForWrite() goes stale as soon as
    anything re-detaches the component for that stack (another NbtHelper call, or
    a helper such as Gun.getAmmoCount), so it must not be used afterwards.

Exit code 1 when findings exist.
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, 'src', 'main', 'java', 'top', 'ribs', 'scguns')

MUTATOR = r'(put|remove|merge)'
DIRECT_WRITE = re.compile(r'NbtHelper\.getTag\(([^()]*)\)\s*\.\s*' + MUTATOR)
BIND_TAG = re.compile(r'CompoundTag\s+(\w+)\s*=\s*NbtHelper\.(getTag|getTagForWrite|getOrCreateTag)\((?P<arg>[^;]*)\);')
ANY_NBT_CALL = re.compile(r'NbtHelper\.(getTag|getOrCreateTag|getTagForWrite|setTag)\(')
# Helpers that read the same stack's custom-data tag and therefore re-detach it.
# ScEnchants.level() is deliberately absent: it reads the ENCHANTMENTS component.
TAG_READING_HELPER = re.compile(r'\bGun\.getAmmoCount\s*\(')


def depths(lines):
    """Brace depth in effect for each line (0 = top level)."""
    out = []
    depth = 0
    for line in lines:
        out.append(depth)
        depth += line.count('{') - line.count('}')
        if depth < 0:
            depth = 0
    return out


def live_range(lines, dep, i, var):
    """Lines after i in which `var` is still in scope and not shadowed."""
    d = dep[i]
    j = i + 1
    while j < len(lines):
        if dep[j] < d:
            break
        if re.search(r'CompoundTag\s+%s\s*=' % re.escape(var), lines[j]) and dep[j] >= d:
            break
        j += 1
    return list(range(i + 1, j))


def main():
    problems = []
    for base, _dirs, files in os.walk(SRC):
        for name in sorted(files):
            if not name.endswith('.java'):
                continue
            path = os.path.join(base, name)
            rel = os.path.relpath(path, ROOT).replace('\\', '/')
            with open(path, 'r', encoding='utf-8', errors='replace') as fh:
                lines = fh.read().splitlines()
            dep = depths(lines)
            for i, line in enumerate(lines):
                if line.strip().startswith('//') or line.strip().startswith('*'):
                    continue
                m = DIRECT_WRITE.search(line)
                if m:
                    problems.append('%s:%d direct write through getTag: %s' % (rel, i + 1, line.strip()))
                    continue

                m = BIND_TAG.search(line)
                if not m:
                    continue
                var, kind, arg = m.group(1), m.group(2), m.group('arg').strip()
                rng = live_range(lines, dep, i, var)

                if kind == 'getTag':
                    for j in rng:
                        if re.search(r'\b%s\s*\.\s*%s' % (re.escape(var), MUTATOR), lines[j]):
                            problems.append('%s:%d local %s from getTag is mutated at line %d (%s) -- '
                                            'the write is invisible to ItemStack.matches'
                                            % (rel, i + 1, var, j + 1, lines[j].strip()))
                            break
                    continue

                uses = [j for j in rng if re.search(r'\b%s\s*\.' % re.escape(var), lines[j])]
                if not uses:
                    continue
                last = uses[-1]
                for j in rng:
                    if j >= last:
                        break
                    text = lines[j]
                    if re.search(r'\b%s\s*\.' % re.escape(var), text):
                        continue
                    if ANY_NBT_CALL.search(text) and arg and arg in text:
                        problems.append('%s:%d local %s = %s(%s) goes stale: NbtHelper call at line %d (%s), '
                                        'still used at line %d -- split the write instead'
                                        % (rel, i + 1, var, kind, arg, j + 1, text.strip(), last + 1))
                        break
                    if TAG_READING_HELPER.search(text) and arg and arg in text:
                        problems.append('%s:%d local %s = %s(%s) goes stale: tag-reading call at line %d (%s), '
                                        'still used at line %d -- split the write instead'
                                        % (rel, i + 1, var, kind, arg, j + 1, text.strip(), last + 1))
                        break

    print('NBT write-aliasing findings: %d' % len(problems))
    for p in problems:
        print('  ' + p)
    return 1 if problems else 0


if __name__ == '__main__':
    sys.exit(main())
