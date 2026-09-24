"""Measure the physics-structure impulse: mass query at the force point and the spin limit.

Uses the temporary /scgunsphysdebug command, which calls the same helper a shot calls with an
arbitrary force point - standing in for "where the player is" on a server with no player.

For each sub-level it fires the same one-punch impulse twice:
  * force point ~5 blocks from the impact (punch range) -> the spin limit must not bind;
  * force point ~200 blocks away                     -> the spin limit must scale the impulse down.

Usage (dev server running with RCON and the debug command):
    python tools/rcon_sable_spin_check.py
"""
import os
import re
import sys
import time

sys.path.insert(0, __file__.rsplit('\\', 1)[0])
from rcon_cmd import Rcon  # noqa: E402


def sub_levels(rcon):
    text = rcon.command('sable storage find_all_sub_levels')
    out = {}
    for block in re.findall(
            r'([0-9a-f-]{36}):\s*\n\s*Position:\s*([-\d.eE]+)\s+([-\d.eE]+)\s+([-\d.eE]+)', text):
        out[block[0]] = (float(block[1]), float(block[2]), float(block[3]))
    return out


def main():
    rcon = Rcon(os.environ.get("SCGUNS_RCON_PASSWORD", "scgunsverify"))
    for cx in range(-1, 2):
        rcon.command('forceload add %d %d' % (cx, 0))
    rcon.command('execute positioned 0 100 0 run sable spawn platform 5')
    time.sleep(2.0)

    levels = sub_levels(rcon)
    if not levels:
        print('no sub-levels found - is Sable installed?')
        return 2
    uuid = max(levels, key=lambda u: levels[u][1])
    hx, hy, hz = levels[uuid]
    print('target sub-level %s at (%.2f, %.2f, %.2f)' % (uuid, hx, hy, hz))

    cases = [('punch range (5 blocks)', (hx + 5.0, hy + 1.0, hz)),
             ('distant shooter (200 blocks)', (hx + 200.0, hy + 1.0, hz))]
    for label, force in cases:
        before = rcon.command(
            'scgunsphysdebug %g %g %g %g %g %g' % (hx, hy, hz, force[0], force[1], force[2]))
        print('  %-30s -> %s' % (label, before.strip()[:80]))
        time.sleep(0.5)

    print('')
    print('server log lines ([SCGUNS-PHYS] lever/spinPerImpulse/allowedSpin/beforeSpin/afterSpin):')
    return 0


if __name__ == '__main__':
    sys.exit(main())
