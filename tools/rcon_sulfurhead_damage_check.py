"""Check that the sulfurhead takes damage from sources other than a player.

0.5.5 overrode `hurt` so that a server-side damage source whose causing entity was not a Player
was refused outright - explosions, fire, other mobs and its own blast all did nothing. The port
kept that rule, and the user then asked for it to be removed, so the mob now uses the ordinary
`Mob.hurt` path.

This is the acceptance test for that change: the mob must lose health from non-player sources,
and a vanilla zombie is hit with the same sources as the control (if the control loses nothing,
the test itself is broken rather than the mob being immune).

A player cannot be summoned on a dedicated server, so the original positive half (a player's hit,
including a player's bullet, whose DamageSource causing entity is the shooter) is verified by
construction: that path always called super.hurt.

Usage (dev server running with RCON):
    python tools/rcon_sulfurhead_damage_check.py
"""
import os
import re
import sys
import time

sys.path.insert(0, __file__.rsplit('\\', 1)[0])
from rcon_cmd import Rcon  # noqa: E402

SH = '@e[type=scguns:sulfurhead,limit=1]'
ZOMBIE = '@e[type=minecraft:zombie,limit=1]'
SPOT = (0.5, 100.0, 0.5)


def health(rcon, selector):
    answer = rcon.command('data get entity %s Health' % selector)
    match = re.search(r'Health:\s*([-\d.]+)f?', answer)
    if match:
        return float(match.group(1))
    match = re.search(r'has the following entity data:\s*([-\d.]+)f?', answer)
    return float(match.group(1)) if match else None


def main():
    rcon = Rcon(os.environ.get("SCGUNS_RCON_PASSWORD", "scgunsverify"))
    for cx in range(-1, 2):
        rcon.command('forceload add %d %d' % (cx, 0))

    rcon.command('kill @e[type=scguns:sulfurhead]')
    rcon.command('kill @e[type=minecraft:zombie]')
    time.sleep(0.5)
    print(rcon.command('summon scguns:sulfurhead %g %g %g {NoAI:1b,PersistenceRequired:1b}'
                       % SPOT).strip())
    print(rcon.command('summon minecraft:zombie %g %g %g {NoAI:1b,PersistenceRequired:1b}'
                       % (SPOT[0] + 3.0, SPOT[1], SPOT[2])).strip())
    time.sleep(1.0)

    before_sh, before_zombie = health(rcon, SH), health(rcon, ZOMBIE)
    if before_sh is None or before_zombie is None:
        print('RESULT: could not read health - test invalid')
        return 2
    print('start: sulfurhead %.1f hp, zombie %.1f hp' % (before_sh, before_zombie))

    sources = [('minecraft:generic', 4.0), ('minecraft:explosion', 6.0),
               ('minecraft:on_fire', 3.0)]
    for kind, amount in sources:
        rcon.command('damage %s %g %s' % (SH, amount, kind))
        rcon.command('damage %s %g %s' % (ZOMBIE, amount, kind))
        time.sleep(0.3)

    after_sh, after_zombie = health(rcon, SH), health(rcon, ZOMBIE)
    lost_sh = before_sh - after_sh
    lost_zombie = before_zombie - after_zombie
    print('after 3 non-player damage sources: sulfurhead %.1f hp (-%.1f), zombie %.1f hp (-%.1f)'
          % (after_sh, lost_sh, after_zombie, lost_zombie))

    rcon.command('kill @e[type=scguns:sulfurhead]')
    rcon.command('kill @e[type=minecraft:zombie]')

    if lost_zombie <= 0.0:
        print('RESULT: the control zombie took no damage - test invalid')
        return 2
    if lost_sh <= 0.0:
        print('RESULT: the sulfurhead is still ignoring non-player damage')
        return 1
    if abs(lost_sh - lost_zombie) > 2.0:
        print('RESULT: the sulfurhead took %.1f hp but the control took %.1f - check manually'
              % (lost_sh, lost_zombie))
        return 1
    print('RESULT: the sulfurhead now takes non-player damage like any other monster')
    return 0


if __name__ == '__main__':
    sys.exit(main())
