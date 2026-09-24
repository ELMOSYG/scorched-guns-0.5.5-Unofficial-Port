"""Do gunner mobs spawn with the Gun Rust curse on their guns?

85% of mob guns are supposed to be cursed (GunCurseUtil.applyCurseIfRoll), applied while the mob's
equipment is generated. This summons a batch of gunner mobs and reads the enchantment component of
what they are holding, which is the only way to tell whether that path really runs.

Gunners also roll melee weapons and armour out of the same equipment config, and only GunItem
stacks can be cursed, so a stack is only treated as a gun when it carries the mod's gun data. A
cursed non-gun, or no cursed gun at all over a batch, is a failure.

Usage (dev server running with RCON):
    python tools/rcon_mob_gun_curse_check.py [rounds-per-mob]
"""
import os
import re
import sys
import time

sys.path.insert(0, __file__.rsplit('\\', 1)[0])
from rcon_cmd import Rcon  # noqa: E402

MOBS = ["scguns:cog_knight", "scguns:cog_minion", "scguns:dissident", "scguns:blunderer",
        "scguns:adjudicator", "scguns:subjugator"]
DEFAULT_ROUNDS = 3
# A gun stack is identified by the mod's own gun data (an ammo count), not by its namespace: the
# same config also hands out scguns melee weapons and armour, which must never be cursed.
GUN_MARKER = 'AmmoCount'
CURSE = 'scguns:gun_rust'


def main():
    rounds = int(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_ROUNDS
    rcon = Rcon(os.environ.get("SCGUNS_RCON_PASSWORD", "scgunsverify"))
    for cx in range(-1, 2):
        rcon.command('forceload add %d %d' % (cx, 0))

    guns = cursed = wrong_cursed = summons = 0
    seen = {}
    for mob in MOBS:
        for _ in range(rounds):
            rcon.command('kill @e[type=%s]' % mob)
            rcon.command('summon %s 0.5 72 0.5' % mob)
            time.sleep(1.0)
            raw = rcon.command('data get entity @e[type=%s,limit=1] HandItems' % mob)
            summons += 1
            match = re.search(r'id: "([a-z_]+:[a-z_0-9]+)"', raw)
            if not match:
                seen['<empty>'] = seen.get('<empty>', 0) + 1
                continue
            name, is_gun, has_curse = match.group(1), GUN_MARKER in raw, CURSE in raw
            seen[name] = seen.get(name, 0) + 1
            guns += 1 if is_gun else 0
            if is_gun:
                cursed += 1 if has_curse else 0
            elif has_curse:
                wrong_cursed += 1
                print('  %-24s cursed but not a gun' % name)
    rcon.command('kill @e[type=scguns:cog_knight]')

    print('held items over %d summons: %s' % (summons, ', '.join(
        '%s x%d' % (k, v) for k, v in sorted(seen.items(), key=lambda kv: -kv[1]))))
    print('%d summons held a gun, %d of those were cursed (configured rate 85%%)' % (guns, cursed))
    if wrong_cursed:
        print('FAIL: %d non-gun stacks carried the curse' % wrong_cursed)
        return 1
    if guns and not cursed:
        print('FAIL: no gunner gun carried %s -- the curse is not being applied' % CURSE)
        return 1
    if not guns:
        print('FAIL: no summon held a gun, nothing was verified')
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main())
