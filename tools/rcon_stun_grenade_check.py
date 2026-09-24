"""Does a stun grenade actually give the surrounding living entities its debuff?

Server-side question, so it can be answered on a dedicated server: summon a zombie, detonate a
stun grenade next to it, and read the zombie's active effects. The deafened effect has a 360
degree cone in the default config, so the zombie does not need to be facing the grenade.

The throwables are registered with `.noSummon()` (as in 0.5.5), so /summon refuses them; to run
this, temporarily comment that out in ModEntities.registerBasic, rebuild, and restore it
afterwards. Result at the time of writing:

    start: zombie effects before: [(), []]
    Summoned new Thrown Stun Grenade
    zombie effects after:  [('scguns:deafened',), ['185']]

Usage (dev server running with RCON):
    python tools/rcon_stun_grenade_check.py
"""
import os
import re
import sys
import time

sys.path.insert(0, __file__.rsplit('\\', 1)[0])
from rcon_cmd import Rcon  # noqa: E402

ZOMBIE = '@e[type=minecraft:zombie,limit=1,sort=nearest]'
SPOT = (0.5, 72.0, 0.5)


def effects(rcon, selector):
    answer = rcon.command('data get entity %s active_effects' % selector)
    if 'No entity was found' in answer or 'not found' in answer:
        return None
    return [tuple(re.findall(r'id:\s*"([^"]+)"', answer)),
            re.findall(r'duration:\s*(\d+)', answer)]


def main():
    rcon = Rcon(os.environ.get("SCGUNS_RCON_PASSWORD", "scgunsverify"))
    for cx in range(-1, 2):
        rcon.command('forceload add %d %d' % (cx, 0))

    rcon.command('kill @e[type=minecraft:zombie]')
    rcon.command('kill @e[type=scguns:throwable_stun_grenade]')
    time.sleep(0.5)
    print(rcon.command('summon minecraft:zombie %g %g %g {NoAI:1b,PersistenceRequired:1b}'
                       % SPOT).strip())
    time.sleep(0.5)
    before = effects(rcon, ZOMBIE)
    print('   zombie effects before: %s' % (before,))

    print(rcon.command('summon scguns:throwable_stun_grenade %g %g %g'
                       % (SPOT[0], SPOT[1] + 3.0, SPOT[2])).strip())
    time.sleep(1.0)
    print('   grenade still present: %s'
          % ('No entity was found' not in rcon.command(
              'data get entity @e[type=scguns:throwable_stun_grenade,limit=1] Pos')))
    time.sleep(2.0)

    after = effects(rcon, ZOMBIE)
    print('   zombie effects (not facing): %s' % (after,))
    rcon.command('kill @e[type=minecraft:zombie]')

    # Second scenario: the zombie faces the flash, which is what BLINDED requires (85 degree cone
    # plus an unobstructed line of sight) - the overlay a player sees needs this effect.
    time.sleep(0.5)
    print('')
    print('--- facing the flash (yaw 90 = looking towards -X, grenade 3 blocks west) ---')
    print(rcon.command('summon minecraft:zombie %g %g %g {NoAI:1b,PersistenceRequired:1b,'
                       'Rotation:[0f,90f]}' % SPOT).strip())
    time.sleep(0.5)
    print(rcon.command('summon scguns:throwable_stun_grenade %g %g %g'
                       % (SPOT[0] - 3.0, SPOT[1] + 0.5, SPOT[2])).strip())
    time.sleep(3.0)
    facing = effects(rcon, ZOMBIE)
    print('   zombie effects (facing):     %s' % (facing,))
    rcon.command('kill @e[type=minecraft:zombie]')
    rcon.command('kill @e[type=scguns:throwable_stun_grenade]')

    ids = (after or [[], []])[0] + (facing or [[], []])[0]
    if 'scguns:blinded' in (facing or [[], []])[0]:
        print('RESULT: deafened and blinded both apply server-side '
              '(the overlay is a client-side matter)')
        return 0
    if ids:
        print('RESULT: only %s landed; scguns:blinded did NOT - see HANDOFF 28.5' % (set(ids),))
        return 1
    print('RESULT: no effect landed at all')
    return 1


if __name__ == '__main__':
    sys.exit(main())
