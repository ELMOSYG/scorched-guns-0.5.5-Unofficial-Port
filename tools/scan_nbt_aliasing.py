#!/usr/bin/env python3
"""Find NBT aliasing hazards before changing NbtHelper.getOrCreateTag.

If getOrCreateTag starts returning a *private copy* (needed so an in-place
mutation is visible to ItemStack.matches / remoteSlots), then any local that
holds an earlier result of the same helper for the same stack becomes a stale
reference: a later write through it would be silently dropped.

This lists, per method, every local bound to NbtHelper.getOrCreateTag(...)
together with the later NbtHelper calls on the same receiver expression.
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, 'src', 'main', 'java', 'top', 'ribs', 'scguns')

ASSIGN = re.compile(r'CompoundTag\s+(\w+)\s*=\s*NbtHelper\.getOrCreateTag\((?P<arg>[^;]*?)\);')
CALL = re.compile(r'NbtHelper\.(getOrCreateTag|getTag|setTag)\((?P<arg>[^;,)]*)[,)]')


def method_bounds(lines, idx):
    """Rough enclosing-method range: from the signature line above to its closing brace."""
    start = idx
    while start > 0 and not re.search(r'\b(private|public|protected)\b.*\(', lines[start - 1] or 'x'):
        start -= 1
    depth = 0
    for i in range(idx, len(lines)):
        depth += lines[i].count('{') - lines[i].count('}')
        if depth <= 0 and i > idx and '{' in ''.join(lines[idx:i + 1]):
            return start, i + 1
        if ';' in lines[i] and depth == 0 and i > idx:
            return start, i + 1
    return start, len(lines)


def main():
    findings = 0
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
                s, e = method_bounds(lines, i)
                later = []
                for j in range(i + 1, e):
                    for c in CALL.finditer(lines[j]):
                        if c.group('arg').strip() == arg and c.group(1) in ('getOrCreateTag', 'getTag'):
                            later.append((j + 1, lines[j].strip()))
                if later:
                    used = [j for j in range(later[0][0], e) if re.search(r'\b%s\s*\.' % re.escape(var), lines[j - 1])]
                    findings += 1
                    print('%s:%d  local %s = getOrCreateTag(%s)' % (rel, i + 1, var, arg))
                    for ln, txt in later:
                        print('    later call  line %d: %s' % (ln, txt))
                    for j in used:
                        print('    local used  line %d: %s' % (j, lines[j - 1].strip()))
                    print()
    print('candidate methods: %d' % findings)
    return 0


if __name__ == '__main__':
    sys.exit(main())
