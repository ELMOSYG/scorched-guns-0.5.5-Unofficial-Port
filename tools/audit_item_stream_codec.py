#!/usr/bin/env python3
"""Audit: never use the strict ItemStack stream codec in this mod.

1.20.1's FriendlyByteBuf#writeItem wrote ItemStack.EMPTY without complaint (count
0). Its 1.21 replacement, ItemStack.STREAM_CODEC, throws
io.netty.handler.codec.EncoderException: "Empty ItemStack not allowed" -- and
because that happens inside the netty encoder it does not look like a normal
error: the client is dropped ("opening the exosuit GUI crashes").

ItemStack.OPTIONAL_STREAM_CODEC has the same wire layout and also handles empty
stacks, so every encode/decode pair in this mod uses it. Such a pair is always
ours on both ends, which is why switching both halves together is safe.

Exit code 1 when a strict site is found.
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, 'src', 'main', 'java')

STRICT = re.compile(r'ItemStack\.(STREAM_CODEC|LIST_STREAM_CODEC)\b')
OPTIONAL = re.compile(r'ItemStack\.OPTIONAL_(STREAM_CODEC|LIST_STREAM_CODEC)\b')


def main():
    findings = []
    optional = 0
    for base, _dirs, names in os.walk(SRC):
        for name in sorted(names):
            if not name.endswith('.java'):
                continue
            path = os.path.join(base, name)
            rel = os.path.relpath(path, ROOT).replace('\\', '/')
            with open(path, 'r', encoding='utf-8', errors='replace') as fh:
                lines = fh.read().splitlines()
            for i, line in enumerate(lines):
                if STRICT.search(line):
                    findings.append('%s:%d %s' % (rel, i + 1, line.strip()))
                optional += len(OPTIONAL.findall(line))

    print('optional ItemStack codec sites: %d' % optional)
    print('strict ItemStack codec sites: %d' % len(findings))
    for f in findings:
        print('  ' + f + '   <-- empty stacks throw inside the encoder; use OPTIONAL_')
    return 1 if findings else 0


if __name__ == '__main__':
    sys.exit(main())
