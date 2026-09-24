"""Reproduce "shooting the bottom of a structure does nothing", on a cleaned-up world.

Earlier runs left a column of test platforms that swallowed the shots before they could reach the
target's underside; the world is now cleaned with `/sable sub_level remove @all` (the selector
takes @all / @nearest / @latest / ..., not a uuid). One fresh platform is spawned, its centre and
size are read from /sable storage, and shots are aimed at its side (control) and at its underside,
slowly and fast.

The [SCGUNS-PHYS] lines then show whether the hit reached the mod at all, whether a structure was
found, and which check refused it.

Usage (dev server running with RCON, Sable installed):
    python tools/rcon_sable_bottom_check.py
"""
import os
import re
import sys
import time

sys.path.insert(0, __file__.rsplit('\\', 1)[0])
from rcon_cmd import Rcon  # noqa: E402

SHELL_TAG = "bottomprobe"
ENTRY = re.compile(
    r'([0-9a-f-]{36}):\s*\n\s*Position:\s*([-\d.eE]+)\s+([-\d.eE]+)\s+([-\d.eE]+)'
    r'\s*\n\s*World Bounds:\s*([-\d.eE]+)\s*x\s*([-\d.eE]+)\s*x\s*([-\d.eE]+)')


def sub_levels(rcon):
    """-> {uuid: (cx, cy, cz, sx, sy, sz)}"""
    text = rcon.command('sable storage find_all_sub_levels')
    return {m.group(1): tuple(float(v) for v in m.groups()[1:]) for m in ENTRY.finditer(text)}


def fire(rcon, label, x, y, z, mx, my, mz, damage=10.0):
    rcon.command('kill @e[tag=%s]' % SHELL_TAG)
    answer = rcon.command(
        'summon scguns:basic_turret %g %g %g {Tags:["%s"],TurretDamage:%gd,Motion:[%gd,%gd,%gd]}'
        % (x, y, z, SHELL_TAG, damage, mx, my, mz)).strip()
    print('  %-26s %s' % (label, answer[:50]))
    time.sleep(1.5)


def main():
    rcon = Rcon(os.environ.get("SCGUNS_RCON_PASSWORD", "scgunsverify"))
    for cx in range(-1, 2):
        rcon.command('forceload add %d %d' % (cx, 0))

    existing = sub_levels(rcon)
    print('removing %d leftover sub-level(s) with "@all"' % len(existing))
    print('   %s' % rcon.command('sable sub_level remove @all').strip()[:80])
    time.sleep(1.0)
    left = sub_levels(rcon)
    print('   %d left' % len(left))
    if left:
        for uuid in left:
            print('   still there: %s' % uuid[:8])

    rcon.command('execute positioned 0 100 0 run sable spawn platform 5')
    time.sleep(2.0)
    levels = sub_levels(rcon)
    if not levels:
        print('no sub-level found - is Sable installed?')
        return 2
    uuid = max(levels, key=lambda u: levels[u][1])
    cx, cy, cz, sx, sy, sz = levels[uuid]
    bottom = cy - sy / 2.0
    print('target %s centre (%.2f, %.2f, %.2f) size (%.2f, %.2f, %.2f) bottom y=%.2f'
          % (uuid, cx, cy, cz, sx, sy, sz, bottom))

    fire(rcon, 'control: side', cx + sx / 2.0 + 2.0, cy, cz, -6.0, 0.0, 0.0)
    # 0.6 blocks under the underside, so nothing else can be in the way.
    fire(rcon, 'bottom: slow up', cx, bottom - 0.6, cz, 0.0, 4.0, 0.0)
    fire(rcon, 'bottom: fast up', cx, bottom - 0.6, cz, 0.0, 20.0, 0.0)

    rcon.command('kill @e[tag=%s]' % SHELL_TAG)
    print('')
    print('read the [SCGUNS-PHYS] lines in run/logs/latest.log')
    return 0


if __name__ == '__main__':
    sys.exit(main())
