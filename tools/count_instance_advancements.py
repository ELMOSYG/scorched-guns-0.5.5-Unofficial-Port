"""Count advancements per namespace, splitting visible (has "display") from hidden.

Used to interpret the client's "Loaded N advancements" line: on the client the
tree only ends up holding the advancements the server sent, and the recipe-unlock
ones are hidden. Comparing the totals tells us whether the mod's own
advancements actually reached the client.

Usage:
    python tools/count_instance_advancements.py --display-only
"""
import glob
import json
import os
import re
import sys
import zipfile

VANILLA = r"D:\MCJAVA\.minecraft\libraries\net\minecraft\client\1.21.1\client-1.21.1-official.jar"
MODS = r"D:\MCJAVA\.minecraft\versions\1.21.1-NeoForge_21.1.250\mods"
ADV = re.compile(r'^data/([^/]+)/advancements?/.+\.json$')


def scan(path):
    """-> {namespace: [has_display, hidden]}, scanning dirs and jars alike."""
    out = {}
    if os.path.isdir(path):
        items = []
        for base, _d, files in os.walk(path):
            for f in files:
                if f.endswith('.json'):
                    full = os.path.join(base, f)
                    with open(full, 'rb') as fh:
                        items.append((full.replace('\\', '/'), fh.read()))
    else:
        items = []
        try:
            with zipfile.ZipFile(path) as zf:
                for name in zf.namelist():
                    if ADV.match(name):
                        items.append((name, zf.read(name)))
        except zipfile.BadZipFile:
            return out
    for name, raw in items:
        m = ADV.match(name) if not os.path.isabs(name) else re.search(r'/data/([^/]+)/advancements?/', name)
        if not m:
            continue
        ns = m.group(1)
        try:
            data = json.loads(raw.decode('utf-8-sig'))
        except Exception:
            out.setdefault(ns, [0, 0, 1])
            continue
        slot = out.setdefault(ns, [0, 0, 0])
        if 'display' in data:
            slot[0] += 1
        else:
            slot[1] += 1
    return out


def main():
    targets = [(os.path.basename(VANILLA), VANILLA)]
    targets += [(os.path.basename(j), j) for j in sorted(glob.glob(os.path.join(MODS, '*.jar')))]
    grand = [0, 0, 0]
    rows = []
    for label, path in targets:
        per_ns = scan(path)
        if not per_ns:
            continue
        for ns, (vis, hid, bad) in sorted(per_ns.items()):
            rows.append((label, ns, vis, hid, bad))
            grand[0] += vis
            grand[1] += hid
            grand[2] += bad
    for label, ns, vis, hid, bad in rows:
        print('%-50s %-22s display=%-5d hidden=%-5d unreadable=%d' % (label[:50], ns, vis, hid, bad))
    print('')
    print('TOTAL display=%d hidden=%d unreadable=%d' % tuple(grand))
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
