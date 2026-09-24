"""Probe how 1.21.1 actually decodes an advancement `display.icon`.

For each candidate icon payload the script rewrites the *built* resource
(build/resources/main/data/scguns/advancement/main/root.json), asks the running
dev server to `/reload`, and reports:

  * the resulting "Loaded N advancements" count (3529 = the 115 scguns
    advancements were dropped, 3644 = they all loaded), and
  * any "Couldn't load advancements" line (lists the orphans),

so we can tell a tolerated icon from a rejected one instead of trusting a
codec reading.

Usage (dev server already running with RCON enabled):
    python tools/probe_advancement_icon.py
"""
import json
import os
import re
import subprocess
import sys
import time

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BUILT = os.path.join(ROOT, 'build', 'resources', 'main', 'data', 'scguns', 'advancement', 'main', 'root.json')
BACKUP = BUILT + '.probe-backup'
LOG = os.path.join(ROOT, 'run', 'logs', 'latest.log')

CASES = [
    ('item = valid scguns item (0.5.5 form)', {'item': 'scguns:m3_carabine'}),
    ('item = bogus item', {'item': 'scguns:no_such_item_at_all'}),
    ('id = valid scguns item', {'id': 'scguns:m3_carabine', 'count': 1}),
    ('id = bogus item', {'id': 'scguns:no_such_item_at_all'}),
    ('empty object', {}),
    ('not an object', 5),
]

# Whole-field probes: do these failures drop the advancement at all?
FIELD_CASES = [
    ('display = 5 (type error)', lambda d: d.__setitem__('display', 5)),
    ('display.title missing', lambda d: d['display'].pop('title')),
    ('criteria removed', lambda d: d.pop('criteria')),
    ('criteria empty', lambda d: d.__setitem__('criteria', {})),
    ('parent = bogus', lambda d: d.__setitem__('parent', 'scguns:no/such/parent')),
]


def tail_log():
    with open(LOG, encoding='utf-8', errors='replace') as fh:
        return fh.read().splitlines()


def reload_server():
    out = subprocess.run([sys.executable, os.path.join(ROOT, 'tools', 'rcon_cmd.py'), 'reload'],
                         capture_output=True, text=True, cwd=ROOT)
    return (out.stdout or '') + (out.stderr or '')


def report(before):
    lines = tail_log()[before:]
    count = [l for l in lines if 'advancements' in l and 'Loaded' in l]
    orphan = [l for l in lines if 'Couldn' in l and 'advancement' in l]
    n = re.search(r'Loaded (\d+) advancements', count[-1]).group(1) if count else '?'
    print('    loaded: %s%s' % (n, '   ORPHANED/DROPPED' if orphan else ''))
    if orphan:
        ids = re.findall(r'scguns:[^,\]]+', orphan[0])
        print('    dropped ids: %d (e.g. %s)' % (len(ids), ', '.join(ids[:3])))
    return n, bool(orphan)


def main():
    if not os.path.exists(BUILT):
        raise SystemExit('built root.json not found: %s' % BUILT)
    with open(BUILT, encoding='utf-8') as fh:
        original = fh.read()
    with open(BACKUP, 'w', encoding='utf-8') as fh:
        fh.write(original)
    try:
        for label, icon in CASES:
            data = json.loads(original)
            data['display']['icon'] = icon
            with open(BUILT, 'w', encoding='utf-8') as fh:
                fh.write(json.dumps(data, indent=2))
            before = len(tail_log())
            print('== %s -> %s' % (label, json.dumps(icon)))
            print('   reload: %s' % reload_server().strip().replace('\n', ' | '))
            time.sleep(5)
            report(before)

        for label, mutate in FIELD_CASES:
            data = json.loads(original)
            mutate(data)
            with open(BUILT, 'w', encoding='utf-8') as fh:
                fh.write(json.dumps(data, indent=2))
            before = len(tail_log())
            print('== %s' % label)
            print('   reload: %s' % reload_server().strip().replace('\n', ' | '))
            time.sleep(5)
            report(before)
    finally:
        with open(BUILT, 'w', encoding='utf-8') as fh:
            fh.write(original)
        os.remove(BACKUP)
        print('== restored original built root.json')
        reload_server()
    return 0


if __name__ == '__main__':
    sys.exit(main())
