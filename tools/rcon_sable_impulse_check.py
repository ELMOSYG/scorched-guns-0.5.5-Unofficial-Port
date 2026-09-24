"""Prove that Scorched Guns projectiles actually shove a Sable physics structure.

Screenplay (no player required, everything through RCON):
  1. spawn a fresh /sable spawn platform, then snapshot every sub-level position with
     `/sable storage find_all_sub_levels` - which keeps answering for a structure that has
     gone dormant, unlike `/sable info <uuid>` ("Cannot find sub-level" there only means
     "not currently simulated", NOT "deleted");
  2. CONTROL: fire 6 vanilla arrows (they have no impulse code) and measure the drift;
  3. TEST: fire shells that actually reach the platform and measure the drift again.

The shells use TurretDamage 10: the impulse config is measured in PLAYER PUNCHES per 10
damage, so 10 damage is exactly one punch - the reference force the reaction is calibrated
against. A bigger number only proves that a nonsense impulse moves things (TurretDamage
500 = 50 punches launched the test platform out of sight).

Usage (dev server running with RCON, Sable installed):
    python tools/rcon_sable_impulse_check.py
"""
import os
import re
import sys
import time

sys.path.insert(0, __file__.rsplit('\\', 1)[0])
from rcon_cmd import Rcon  # noqa: E402

ARROW_TAG = "impulsearrow"
SHELL_TAG = "impulseshell"
SHELL_DAMAGE = 10.0     # 10 damage = exactly one punch


def positions(rcon):
    """-> {uuid: (x, y, z)} from /sable storage find_all_sub_levels."""
    text = rcon.command('sable storage find_all_sub_levels')
    out = {}
    for block in re.findall(
            r'([0-9a-f-]{36}):\s*\n\s*Position:\s*([-\d.eE]+)\s+([-\d.eE]+)\s+([-\d.eE]+)', text):
        out[block[0]] = (float(block[1]), float(block[2]), float(block[3]))
    return out


def moved(before, after):
    """-> {uuid: distance moved}, for every structure present in both snapshots."""
    out = {}
    for uuid, pos in before.items():
        if uuid in after:
            out[uuid] = sum((a - b) ** 2 for a, b in zip(after[uuid], pos)) ** 0.5
    return out


def report(before, after):
    """Prints the drift of every structure, newest (highest y) first."""
    deltas = moved(before, after)
    for uuid, dist in sorted(deltas.items(), key=lambda kv: -before[kv[0]][1]):
        print('   %s y=%.2f  moved %.3f blocks' % (uuid[:8], before[uuid][1], dist))
    return deltas


def fire(rcon, tag, entity, nbt, px, py, pz, count, delay):
    """Summons `count` projectiles beside the platform; returns how many were created."""
    hit = 0
    for _ in range(count):
        rcon.command('kill @e[tag=%s]' % tag)
        # Start just outside the ~11 block wide platform and fly fast, so gravity (0.05
        # blocks/tick^2) cannot drop the projectile below the platform before it arrives.
        answer = rcon.command(
            'summon %s %g %g %g {Tags:["%s"],%s,Motion:[-20.0d,0.0d,0.0d]}'
            % (entity, px + 7.0, py + 0.4, pz, tag, nbt)).strip()
        if 'Summoned' not in answer:
            return hit, answer
        hit += 1
        time.sleep(delay)
    return hit, ''


def main():
    rcon = Rcon(os.environ.get("SCGUNS_RCON_PASSWORD", "scgunsverify"))
    for cx in range(-1, 2):
        rcon.command('forceload add %d %d' % (cx, 0))

    print('spawning a fresh platform:')
    print('   %s' % rcon.command('execute positioned 0 100 0 run sable spawn platform 5').strip()[:110])
    time.sleep(2.0)

    snapshot = positions(rcon)
    if not snapshot:
        print('no sub-levels found - is Sable installed?')
        return 2
    # The freshest platform is the highest one; the other entries are leftovers from earlier
    # runs sitting in the same column.
    uuid = max(snapshot, key=lambda u: snapshot[u][1])
    px, py, pz = snapshot[uuid]
    print('target sub-level %s at (%.2f, %.2f, %.2f)' % (uuid, px, py, pz))
    print('   %d sub-levels in storage' % len(snapshot))

    print('')
    print('CONTROL: 6 vanilla arrows (no impulse code)')
    made, error = fire(rcon, ARROW_TAG, 'minecraft:arrow', 'pickup:0b', px, py, pz, 6, 0.4)
    if made == 0:
        print('RESULT: could not summon arrows - test invalid (%s)' % error[:80])
        return 2
    time.sleep(3.0)
    control_before = snapshot
    control_after = positions(rcon)
    control = report(control_before, control_after)
    rcon.command('kill @e[tag=%s]' % ARROW_TAG)

    print('')
    print('TEST: 8 scguns turret shells (TurretDamage %g = 1 punch each)' % SHELL_DAMAGE)
    test_before = positions(rcon)
    made, error = fire(rcon, SHELL_TAG, 'scguns:basic_turret',
                       'TurretDamage:%sd' % SHELL_DAMAGE, px, py, pz, 8, 0.5)
    print('   %d shells summoned' % made)
    if made == 0:
        print('RESULT: could not summon shells - test invalid (%s)' % error[:80])
        return 2
    time.sleep(4.0)
    test_after = positions(rcon)
    test = report(test_before, test_after)
    rcon.command('kill @e[tag=%s]' % SHELL_TAG)

    print('')
    ctrl = control.get(uuid, 0.0)
    test_drift = test.get(uuid, 0.0)
    print('target platform: arrows moved it %.3f blocks, shells moved it %.3f blocks'
          % (ctrl, test_drift))
    if test_drift > ctrl + 0.25:
        print('RESULT: projectiles DO push the structure')
        return 0
    print('RESULT: no measurable push - check the [SCGUNS-PHYS] server log lines')
    return 1


if __name__ == '__main__':
    sys.exit(main())
