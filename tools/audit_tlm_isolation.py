"""The host mod must never reference Touhou Little Maid.

The maid compat ships inside the host jar as a nested mod, which is what keeps "no TLM installed"
safe: the nested mod is loaded by its own class loader and carries all the TLM types, while the
host's own classes must stay completely free of them. If a host class ever gained a TLM reference,
loading it would throw NoClassDefFoundError for anyone without TLM - the exact failure this design
is meant to prevent.

Two independent checks:
  1. host sources under src/main/java/top/ribs/scguns - no TLM package import, no TLM type name
  2. the built host jar, excluding the nested compat - no TLM class reference in any entry

usage: python tools/audit_tlm_isolation.py
"""
import os
import re
import sys
import zipfile

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(REPO, 'src', 'main', 'java')
JAR = os.path.join(REPO, 'build', 'libs', 'scguns-0.5.5.jar')

# Any of these in the host's own code is a defect.
NEEDLES = [
    'com.github.tartaricacid',
    'touhoulittlemaid',
    'touhou_little_maid',
    'scg2tlm',
]


def main():
    problems = 0

    source_hits = 0
    for root, _dirs, names in os.walk(SRC):
        for name in names:
            if not name.endswith('.java'):
                continue
            path = os.path.join(root, name)
            text = open(path, encoding='utf-8', errors='replace').read()
            for needle in NEEDLES:
                if needle in text:
                    line = next((i + 1 for i, l in enumerate(text.split('\n')) if needle in l), 0)
                    print('  SOURCE %s:%d references %r' % (os.path.relpath(path, REPO), line, needle))
                    source_hits += 1
    print('host sources: %d TLM reference(s)' % source_hits)
    problems += source_hits

    if not os.path.isfile(JAR):
        print('  (host jar not built yet - run: gradlew build)')
        return 1 if problems else 0

    jar_hits = 0
    with zipfile.ZipFile(JAR) as jar:
        for entry in jar.namelist():
            if not entry.endswith('.class') or entry.startswith('META-INF/jarjar/'):
                continue
            data = jar.read(entry)
            for needle in NEEDLES:
                if needle.encode() in data:
                    print('  JAR    %s references %r' % (entry, needle))
                    jar_hits += 1
    print('host jar entries (excluding the nested compat): %d TLM reference(s)' % jar_hits)
    problems += jar_hits

    if problems:
        print('')
        print('FAIL: the host must stay TLM-free; move the reference into maid-compat/ instead.')
        return 1

    print('')
    print('OK: the host is TLM-free, so it loads with or without Touhou Little Maid.')
    return 0


if __name__ == '__main__':
    sys.exit(main())
