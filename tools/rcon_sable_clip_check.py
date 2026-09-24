"""Prove that block raycasts in this environment do see Sable sub-levels.

The mod's gun projectiles used a hand-written per-block traversal, which only ever
reads the main level and so flew straight through Sable's moving structures. The fix
makes them call `Level#clip` when a physics-structure mod is loaded, because Sable
replaces `BlockGetter#clip` with a sub-level aware version
(dev.ryanhcode.sable.mixin.clip_overwrite.BlockGetterMixin).

Method: spawn a Sable platform sub-level in empty sky, then fire plain vanilla arrows
horizontally through the air where it is. An arrow that gets stuck in mid-air (with no
block in the main level there) can only have hit the sub-level. Two controls keep this
honest:
  * an arrow fired with no structure in the way must keep flying;
  * `/sable spawn` runs at the command source, which over RCON is (0,0,0) - deep
    underground - so the spawn is wrapped in `/execute positioned`.

Usage (dev server running with RCON; Sable installed):
    python tools/rcon_sable_clip_check.py
"""
import os
import re
import sys
import time

sys.path.insert(0, __file__.rsplit('\\', 1)[0])
from rcon_cmd import Rcon  # noqa: E402

ARROW_TAG = "sablearrow"
PLATFORM_X, PLATFORM_Y, PLATFORM_Z = 0, 100, 0
FIRE_FROM_X = 15


def fire(rcon, x, y, z, seconds=2.0):
    """Fire one arrow towards -X and report (pos, inGround)."""
    rcon.command('kill @e[tag=%s]' % ARROW_TAG)
    rcon.command('summon minecraft:arrow %g %g %g {Tags:["%s"],pickup:0b,Motion:[-0.75d,0.0d,0.0d]}'
                 % (x, y, z, ARROW_TAG))
    time.sleep(seconds)
    raw = rcon.command('data get entity @e[tag=%s,limit=1] Pos' % ARROW_TAG)
    ground = rcon.command('data get entity @e[tag=%s,limit=1] inGround' % ARROW_TAG)
    nums = re.findall(r'(-?[\d.]+)d', raw)
    stuck = '1b' in ground
    rcon.command('kill @e[tag=%s]' % ARROW_TAG)
    if len(nums) < 3:
        return None, False
    return (float(nums[0]), float(nums[1]), float(nums[2])), stuck


def main():
    rcon = Rcon(os.environ.get("SCGUNS_RCON_PASSWORD", "scgunsverify"))
    for cx in range(0, 3):
        rcon.command('forceload add %d %d' % (cx, 0))

    print('control: arrow with nothing in the way (must keep flying)')
    pos, stuck = fire(rcon, FIRE_FROM_X, PLATFORM_Y + 25, PLATFORM_Z)
    print('   arrow ended at %s  inGround=%s' % (pos, stuck))
    control_ok = pos is not None and (pos[0] < FIRE_FROM_X - 3 or pos[1] < PLATFORM_Y + 24)
    print('   -> %s' % ('flew on (good)' if control_ok else 'DID NOT MOVE (probe is broken)'))

    print('')
    print('spawning the platform sub-level at (%d, %d, %d)' % (PLATFORM_X, PLATFORM_Y, PLATFORM_Z))
    print('   %s' % rcon.command('execute positioned %d %d %d run sable spawn platform 5'
                                 % (PLATFORM_X, PLATFORM_Y, PLATFORM_Z)).strip()[:110])
    print('   %s' % rcon.command('execute positioned %d %d %d run sable spawn platform 5'
                                 % (PLATFORM_X, PLATFORM_Y + 6, PLATFORM_Z)).strip()[:110])

    print('')
    print('probing through the air where the platform should be:')
    hits = []
    for dy in (-4, -2, 0, 2, 4, 6, 8):
        for dz in (0, -3, 3):
            pos, stuck = fire(rcon, FIRE_FROM_X, PLATFORM_Y + dy, PLATFORM_Z + dz)
            if pos is None:
                continue
            mid_air = stuck and pos[0] > PLATFORM_X - 2 and abs(pos[1] - (PLATFORM_Y + dy)) < 1.5
            print('   dy=%+3d dz=%+d -> (%6.1f, %6.1f, %6.1f) inGround=%-5s %s'
                  % (dy, dz, pos[0], pos[1], pos[2], stuck,
                     '<== STOPPED IN MID-AIR' if mid_air else ''))
            if mid_air:
                hits.append(pos)

    print('')
    if not control_ok:
        print('RESULT: inconclusive - the control arrow did not move')
        return 2
    if hits:
        print('RESULT: %d arrow(s) stopped in mid-air -> Level#clip sees Sable sub-levels, '
              'which is exactly the call the projectiles now delegate to' % len(hits))
        return 0
    print('RESULT: no arrow stopped in mid-air - the platform may not be where we probed')
    return 1


if __name__ == '__main__':
    sys.exit(main())
