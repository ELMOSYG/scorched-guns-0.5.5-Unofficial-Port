#!/usr/bin/env python3
"""Second-pass NBT aliasing scan: re-entrancy through project helpers.

If NbtHelper.getOrCreateTag() starts returning a private copy of the tag (so an
in-place mutation becomes visible to ItemStack.matches/remoteSlots), a local
holding an earlier result goes stale as soon as ANY other code path calls
getOrCreateTag for the same stack -- including indirectly, through helpers such
as Gun.setAmmo(stack, n).

For every `CompoundTag v = NbtHelper.getOrCreateTag(expr);` this reports the
lines that (a) pass `expr` to some call while `v` is still live, and (b) use
`v` afterwards.
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, 'src', 'main', 'java', 'top', 'ribs', 'scguns')

ASSIGN = re.compile(r'CompoundTag\s+(\w+)\s*=\s*NbtHelper\.getOrCreateTag\((?P<arg>[^;]*?)\);')


def method_range(lines, idx):
    start = idx
    # walk back to a line that looks like a method signature and has no ';'
    while start > 0:
        prev = lines[start - 1]
        if re.search(r'\)\s*\{\s*$', prev) or re.search(r'\)\s*(throws [\w.,\s]+)?\{', prev):
            break
        if prev.strip() == '' or prev.strip().startswith('//') or prev.strip().startswith('*'):
            start -= 1
            continue
        start -= 1
    depth = 0
    for i in range(idx, len(lines)):
        depth += lines[i].count('{') - lines[i].count('}')
        if depth <= 0 and i > idx:
            return start, i
    return start, len(lines)


def main():
    hits = 0
    for base, _dirs, files in os.walk(SRC):
        for name in sorted(files):
            if not name.endswith('.java'):
                continue
            path = os.path.join(base, name)
            rel = os.path.relpath(path, ROOT).replace('\\', '/')
            with open(path, 'r', encoding='utf-8', errors='replace') as fh:
                lines = fh.read().splitlines()
            for i, line in enumerate(lines):
                m = ASSIGN.search(line)
                if not m:
                    continue
                var, arg = m.group(1), m.group('arg').strip()
                if not re.fullmatch(r'[A-Za-z_][\w.]*', arg):
                    continue
                _s, e = method_range(lines, i)
                uses = [j for j in range(i + 1, e + 1)
                        if re.search(r'\b%s\s*\.' % re.escape(var), lines[j])]
                if not uses:
                    continue
                last = uses[-1]
                danger = []
                for j in range(i + 1, last):
                    text = lines[j].strip()
                    if text.startswith('//') or text.startswith('*'):
                        continue
                    if re.search(r'\b%s\s*\.' % re.escape(var), lines[j]):
                        continue  # the local itself, not a re-entry
                    if re.search(r'\(\s*[^()]*\b%s\b[^()]*\)' % re.escape(arg), lines[j]):
                        danger.append((j + 1, text))
                if danger:
                    hits += 1
                    print('%s:%d  local %s = getOrCreateTag(%s)   live until line %d' % (rel, i + 1, var, arg, last + 1))
                    for ln, txt in danger:
                        print('    possible re-entry line %d: %s' % (ln, txt))
                    print()
    print('sites needing manual review: %d' % hits)
    return 0


if __name__ == '__main__':
    sys.exit(main())
