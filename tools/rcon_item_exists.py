"""Ask a running dev server whether an item id is actually registered.

An unknown item id does not necessarily fail datapack loading (the item holder
codec tolerates unbound names), so "no error in the log" is not proof that a
conditionally registered item exists. This spawns an item entity carrying the id
and reads the stack back: an unregistered id comes back as minecraft:air.

Usage (dev server running with RCON enabled):
    python tools/rcon_item_exists.py scguns:anthralite_knife farmersdelight:iron_knife
"""
import os
import re
import sys

sys.path.insert(0, __file__.rsplit('\\', 1)[0])
from rcon_cmd import Rcon  # noqa: E402

TAG = "scguns_probe"


def exists(rcon, item_id):
    rcon.command('kill @e[tag=%s]' % TAG)
    rcon.command('summon minecraft:item 0 100 0 {Item:{id:"%s",count:1},Tags:["%s"],NoGravity:1b}' % (item_id, TAG))
    raw = rcon.command('data get entity @e[tag=%s,limit=1] Item' % TAG)
    rcon.command('kill @e[tag=%s]' % TAG)
    m = re.search(r'id:\s*"([^"]+)"', raw)
    got = m.group(1) if m else None
    return got, raw.strip().splitlines()[-1][:150] if raw.strip() else ''


def main():
    ids = sys.argv[1:]
    if not ids:
        raise SystemExit(__doc__)
    rcon = Rcon(os.environ.get("SCGUNS_RCON_PASSWORD", "scgunsverify"))
    bad = 0
    for item_id in ids:
        got, line = exists(rcon, item_id)
        ok = got == item_id
        bad += 0 if ok else 1
        print('%-42s %s   (server said: %s)' % (item_id, 'REGISTERED' if ok else 'MISSING -> %s' % got, line))
    return 1 if bad else 0


if __name__ == '__main__':
    sys.exit(main())
