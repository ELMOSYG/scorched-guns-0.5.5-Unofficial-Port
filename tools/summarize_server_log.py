"""Summarise a dev-server log: mixin application, data layer, errors."""
import re
import sys

path = sys.argv[1] if len(sys.argv) > 1 else r'build-logs\server-31.txt'
lines = open(path, encoding='utf-8', errors='replace').read().splitlines()

mix = [l for l in lines if 'scguns.mixins.json' in l and 'Mixing' in l]
fail = [l for l in lines if 'scguns.mixins.json' in l and re.search(r'fail|error|Unable|critical', l, re.I)]
print('scguns mixin applications: %d' % len(mix))
for l in mix[:8]:
    print('   ' + l[:170])
print('scguns mixin failures: %d' % len(fail))
for l in fail[:5]:
    print('   ' + l[:200])
print('recipe parse errors: %d' % len([l for l in lines if 'Parsing error loading recipe' in l]))
print('tag load failures: %d' % len([l for l in lines if "Couldn't load tag" in l]))
print('advancement orphan lines: %d' % len([l for l in lines if "Couldn't load advancements" in l]))
print('ERROR/FATAL lines: %d' % len([l for l in lines if re.search(r'\] \[(ERROR|FATAL)\]', l)]))
for l in [l for l in lines if re.search(r'\] \[(ERROR|FATAL)\]', l)][:8]:
    print('   ' + l[:200])
