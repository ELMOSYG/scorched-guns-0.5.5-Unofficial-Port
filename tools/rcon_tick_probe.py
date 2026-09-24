"""Is the dedicated dev server actually ticking entities with no player online?

Every RCON-side functional test depends on this: if the world is not ticking,
summoned entities keep their spawn state and nothing that happens in
Entity#tick (entityInside, fire damage, ...) can be observed.

Usage:
    python tools/rcon_tick_probe.py
"""
import os
import sys
import time

sys.path.insert(0, __file__.rsplit('\\', 1)[0])
from rcon_cmd import Rcon  # noqa: E402

TAG = "scguns_tickprobe"


def main():
    rcon = Rcon(os.environ.get("SCGUNS_RCON_PASSWORD", "scgunsverify"))
    rcon.command('kill @e[tag=%s]' % TAG)
    rcon.command('summon minecraft:item 0 100 0 {Item:{id:"minecraft:stone",count:1},Tags:["%s"]}' % TAG)
    t0 = rcon.command('time query gametime').strip()
    time.sleep(5)
    t1 = rcon.command('time query gametime').strip()
    age = rcon.command('data get entity @e[tag=%s,limit=1] Age' % TAG).strip()
    pos = rcon.command('data get entity @e[tag=%s,limit=1] Pos' % TAG).strip()
    rcon.command('kill @e[tag=%s]' % TAG)
    print('gametime before: %s' % t0.splitlines()[-1][:90])
    print('gametime after : %s' % t1.splitlines()[-1][:90])
    print('item Age       : %s' % age.splitlines()[-1][:90])
    print('item Pos       : %s' % pos.splitlines()[-1][:90])
    return 0


if __name__ == '__main__':
    sys.exit(main())
