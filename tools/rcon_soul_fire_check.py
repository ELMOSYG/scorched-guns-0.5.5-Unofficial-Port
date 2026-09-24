"""Prove the Soul Fire'd / Prometheus compatibility actually applies a fire type.

ScorchedGuns#setSoulFireOnEntity and FakeSoulFireBlock both call
it.crystalnest.prometheus.api.FireManager.setOnFire(entity, seconds, SOUL_FIRE_TYPE).
Prometheus keeps the fire type in synced data and mirrors it into NBT under
"FireType" (its EntityMixin saves and loads that key), which makes the call
observable over RCON.

Three environment facts this probe had to work around:
  * a 1.21 BlockState#entityInside only runs while the entity *moves*
    (Entity#move -> tryCheckInsideBlocks), so the entity is dropped through the
    fire block instead of standing in it;
  * with no player online, ordinary chunks are loaded but NOT entity-ticking, so
    the probe force-loads the chunk first (verified with tools/rcon_tick_probe.py);
  * BaseFireBlock-style fire needs a valid base under it, hence the soul soil.

Usage (dev server running with RCON enabled):
    python tools/rcon_soul_fire_check.py
"""
import os
import re
import sys
import time

sys.path.insert(0, __file__.rsplit('\\', 1)[0])
from rcon_cmd import Rcon  # noqa: E402

TAG = "scguns_fireprobe"
X, Z = 0, 0
BASE_Y = 99          # soul soil
FIRE_Y = BASE_Y + 1  # the fire block
DROP_Y = FIRE_Y + 2  # the entity falls through the fire block


def read(rcon, path):
    raw = rcon.command('data get entity @e[tag=%s,limit=1] %s' % (TAG, path))
    return raw.strip().replace('\n', ' ')


def probe(rcon, block, label):
    print('== %s' % label)
    for cmd in (
        'kill @e[tag=%s]' % TAG,
        'setblock %d %d %d minecraft:soul_soil' % (X, BASE_Y, Z),
        'setblock %d %d %d %s' % (X, FIRE_Y, Z, block),
        # An iron golem outlives the fire long enough to read the fire type back.
        'summon minecraft:iron_golem %d %d %d {PersistenceRequired:1b,Tags:["%s"]}'
        % (X, DROP_Y, Z, TAG),
    ):
        print('   $ %-72s -> %s' % (cmd[:72], rcon.command(cmd).strip().replace('\n', ' ')[:80]))
    time.sleep(3)
    health, fire, ftype, pos = (read(rcon, 'Health'), read(rcon, 'Fire'),
                                read(rcon, 'FireType'), read(rcon, 'Pos'))
    for name, raw in (('Pos', pos), ('Health', health), ('Fire', fire), ('FireType', ftype)):
        print('   %-9s %s' % (name, raw[:150]))
    rcon.command('kill @e[tag=%s]' % TAG)
    rcon.command('setblock %d %d %d air' % (X, FIRE_Y, Z))
    rcon.command('setblock %d %d %d air' % (X, BASE_Y, Z))
    hp = re.search(r'([\d.]+)f', health)
    ft = re.search(r'FireType:\s*"([^"]*)"', ftype)
    return (float(hp.group(1)) if hp else None), (ft.group(1) if ft else None)


def main():
    rcon = Rcon(os.environ.get("SCGUNS_RCON_PASSWORD", "scgunsverify"))
    rcon.command('forceload add %d %d' % (X >> 4, Z >> 4))
    try:
        vanilla_hp, _ = probe(rcon, 'minecraft:soul_fire', 'control: vanilla soul fire')
        mod_hp, mod_type = probe(rcon, 'scguns:fake_soul_fire', 'scguns:fake_soul_fire')
    finally:
        rcon.command('forceload remove %d %d' % (X >> 4, Z >> 4))

    print('')
    ok = True
    if vanilla_hp is None or vanilla_hp >= 20.0:
        print('NOTE: the vanilla soul fire control did not damage the golem, so the '
              'damage part of this probe is inconclusive')
        ok = False
    if mod_hp is None or mod_hp >= 20.0:
        print('FAIL: scguns:fake_soul_fire did not damage the entity -> entityInside did not run')
        ok = False
    elif mod_type in (None, '', 'minecraft:'):
        print('PARTIAL: the block damaged the entity, but Prometheus recorded no fire type '
              '(setOnFire threw and the plain-fire fallback ran?)')
        ok = False
    else:
        print('OK: block damage applied and Prometheus fire type = %s' % mod_type)
    return 0 if ok else 1


if __name__ == '__main__':
    sys.exit(main())
