"""Does the stun grenade's debuff reach a player, in survival and in creative?

The effect loop skips entities where ExplosionHelper.ignoresExplosion is true, which for a player
means creative (abilities.invulnerable) or spectator - the 1.20.1 behaviour. This checks that
directly with a real player: detonate a stun grenade next to them and read active_effects in both
game modes. deafened is used for the comparison because it is omnidirectional (360 degrees in the
default config), so the player's facing does not matter.

Usage (dev server running, a player connected):
    python tools/rcon_stun_player_check.py [playerName]
"""
import os
import re
import sys
import time

sys.path.insert(0, __file__.rsplit('\\', 1)[0])
from rcon_cmd import Rcon  # noqa: E402


def pos(rcon, player):
    answer = rcon.command('data get entity %s Pos' % player)
    numbers = re.findall(r'([-\d.]+)d', answer)
    return tuple(float(v) for v in numbers[:3]) if len(numbers) >= 3 else None


def effects(rcon, player):
    answer = rcon.command('data get entity %s active_effects' % player)
    return re.findall(r'id:\s*"([^"]+)"', answer), re.findall(r'duration:\s*(\d+)', answer)


def main():
    player = sys.argv[1] if len(sys.argv) > 1 else 'Dev'
    rcon = Rcon(os.environ.get("SCGUNS_RCON_PASSWORD", "scgunsverify"))
    for cx in range(-1, 2):
        rcon.command('forceload add %d %d' % (cx, 0))

    for mode in ('survival', 'creative'):
        rcon.command('effect clear %s' % player)
        print(rcon.command('gamemode %s %s' % (mode, player)).strip()[:70])
        time.sleep(1.0)
        where = pos(rcon, player)
        if where is None:
            print('   could not read the player position')
            return 2
        x, y, z = where
        print('   %s at (%.1f, %.1f, %.1f); before: %s'
              % (mode, x, y, z, effects(rcon, player)))
        print('   ' + rcon.command('summon scguns:throwable_stun_grenade %g %g %g'
                                   % (x + 1.5, y + 1.0, z)).strip()[:60])
        time.sleep(2.5)
        ids, durations = effects(rcon, player)
        print('   %s after: %s' % (mode, list(zip(ids, durations))))
        rcon.command('kill @e[type=scguns:throwable_stun_grenade]')

    rcon.command('gamemode survival %s' % player)
    return 0


if __name__ == '__main__':
    sys.exit(main())
