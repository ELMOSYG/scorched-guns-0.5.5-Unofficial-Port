"""Survey every datapack `conditions` block and list condition types.

NeoForge 1.21.1 resolves `neoforge:mod_loaded` (and the other `neoforge:*`
conditions) on its own, but a `scguns:*` condition needs a registered
ICondition serializer. The four `compat/*ModCondition` classes were removed
during the port, so any remaining `scguns:*` condition is a load-time failure.
`tools/fix_conditions.py` rewrites them.

NOTE  two different things are both spelled `conditions`:
      * datapack LOAD conditions  -> entries carry `"type"`
      * loot table/modifier PREDICATES -> entries carry `"condition"`
      Only the first kind can fail a datapack load, so only it is reported here.
"""
import collections
import json
import os

ROOT = os.path.join('src', 'main', 'resources')

types = collections.Counter()
files_by_type = collections.defaultdict(set)
predicates = collections.Counter()
broken = []


def walk(node, path):
    if isinstance(node, dict):
        for key, value in node.items():
            if key == 'conditions' and isinstance(value, list):
                for cond in value:
                    if isinstance(cond, dict):
                        if 'type' in cond:
                            ctype = cond['type']
                            types[ctype] += 1
                            files_by_type[ctype].add(path)
                        elif 'condition' in cond:
                            predicates[cond['condition']] += 1
            walk(value, path)
    elif isinstance(node, list):
        for item in node:
            walk(item, path)


scanned = 0
for dirpath, _, filenames in os.walk(ROOT):
    for filename in filenames:
        if not filename.endswith('.json'):
            continue
        full = os.path.join(dirpath, filename)
        try:
            with open(full, encoding='utf-8') as handle:
                data = json.load(handle)
        except Exception:
            broken.append(full)
            continue
        walk(data, full)
        scanned += 1

print(f'scanned {scanned} json files ({len(broken)} unparsable)')
print('== datapack load conditions (key "type") ==')
for ctype, count in types.most_common():
    print(f'{count:6d}  {ctype}')
print(f'== loot predicates (key "condition"), {sum(predicates.values())} total, top 5 ==')
for ctype, count in predicates.most_common(5):
    print(f'{count:6d}  {ctype}')

print('\n== condition types needing a mod-provided serializer (nothing outside vanilla/neoforge) ==')
missing = [t for t in types if not t.startswith(('neoforge:', 'minecraft:'))]
if not missing:
    print('(none - good)')
for ctype in missing:
    print(f'* {ctype}  ({types[ctype]} occurrence(s))')
    for path in sorted(files_by_type[ctype]):
        print('   ', path)

if broken:
    print('\n== unparsable json ==')
    for path in broken:
        print('   ', path)
