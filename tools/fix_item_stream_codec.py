#!/usr/bin/env python3
"""Replace the strict ItemStack stream codec with the optional one.

1.20.1's FriendlyByteBuf#writeItem happily wrote ItemStack.EMPTY (count 0). In
1.21 the replacement, ItemStack.STREAM_CODEC, throws
"Empty ItemStack not allowed" -- and that exception happens inside the netty
encoder, so it does not surface as a normal error: it kills the connection
("打开界面会崩溃" / disconnect).

ItemStack.OPTIONAL_STREAM_CODEC has the same wire layout and additionally
encodes empty stacks. Every encode/decode pair in this mod is ours on both
sides, so switching both halves together is safe.

Usage:
    python tools/fix_item_stream_codec.py            # dry run
    python tools/fix_item_stream_codec.py --apply
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, 'src', 'main', 'java')

STRICT = re.compile(r'ItemStack\.(STREAM_CODEC|LIST_STREAM_CODEC)\b')


def main():
    apply = '--apply' in sys.argv
    files = 0
    sites = 0
    for base, _dirs, names in os.walk(SRC):
        for name in sorted(names):
            if not name.endswith('.java'):
                continue
            path = os.path.join(base, name)
            with open(path, 'r', encoding='utf-8', errors='replace') as fh:
                text = fh.read()
            hits = STRICT.findall(text)
            if not hits:
                continue
            files += 1
            sites += len(hits)
            rel = os.path.relpath(path, ROOT).replace('\\', '/')
            print('%-70s %d site(s)' % (rel, len(hits)))
            if apply:
                new = STRICT.sub(lambda m: 'ItemStack.OPTIONAL_' + m.group(1), text)
                with open(path, 'w', encoding='utf-8', newline='') as fh:
                    fh.write(new)
    print('')
    print('files: %d, sites: %d%s' % (files, sites, ' (applied)' if apply else ' (dry run)'))
    return 0


if __name__ == '__main__':
    sys.exit(main())
