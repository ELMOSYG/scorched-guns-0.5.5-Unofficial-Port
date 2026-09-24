"""Verify the gun enchantment rules on a running dev server, over RCON.

Both rules come from 0.5.5's Java classes and are data in 1.21, so the only honest check is to ask
the game: give a mob a gun and try to apply enchantments to it.

  1. CorrodedEnchantment used EnchantmentCategory.WEAPON (swords/axes) - a gun must refuse it, and a
     sword must accept it.
  2. GunEnchantment.checkCompatibility made enchantments of the same Type mutually exclusive:
     heavy_shot (PROJECTILE) must block accelerator (PROJECTILE) but not quick_hands (RELOAD).
  3. TriggerFingerEnchantment excluded #scguns:single_shot, so a musket must refuse it.

Usage: python tools/rcon_verify_enchant_conflicts.py   (server must be running with rcon on)
"""
import re
import subprocess
import sys

RCON = [sys.executable, "tools/rcon_cmd.py"]


def run(*commands):
    result = subprocess.run(RCON + list(commands), capture_output=True, text=True,
                            cwd=r"E:\mod\scgun-0.5.5-1.21.1-neoforge")
    return result.stdout


def held(item):
    """Clear the field, then summon one zombie and put *item* in its main hand.

    HandItems in the summon NBT did not stick here, and leaving old zombies around makes
    @e[type=zombie,limit=1] ambiguous, so the item goes in with /item replace and there is only
    ever one zombie.
    """
    return ("kill @e[type=minecraft:zombie]",
            "summon minecraft:zombie 0 100 0",
            "item replace entity @e[type=minecraft:zombie,limit=1] weapon.mainhand with %s" % item)


def enchant(name):
    return 'enchant @e[type=minecraft:zombie,limit=1] %s' % name


def outcome(output, name):
    """The reply that follows the last mention of *name*, and whether the enchantment applied."""
    idx = output.rfind(name)
    if idx == -1:
        return "(no reply found)", False
    rest = output[idx + len(name):].strip()
    reply = rest.splitlines()[0].strip() if rest else "(empty reply)"
    return reply, reply.startswith("Applied enchantment")


def main():
    results = []

    # --- a normal gun versus corroded (melee only) and the two conflict groups
    out = run(*held("scguns:gale"), enchant("scguns:corroded"))
    reply, ok = outcome(out, "scguns:corroded")
    results.append(("gun refuses corroded (melee only)", not ok, reply))

    out = run(*held("scguns:gale"), enchant("scguns:heavy_shot"), enchant("scguns:accelerator"))
    reply, ok = outcome(out, "scguns:heavy_shot")
    results.append(("gun accepts heavy_shot (PROJECTILE)", ok, reply))
    reply, ok = outcome(out, "scguns:accelerator")
    results.append(("accelerator refused with heavy_shot (same group)", not ok, reply))

    out = run(*held("scguns:gale"), enchant("scguns:heavy_shot"), enchant("scguns:quick_hands"))
    reply, ok = outcome(out, "scguns:quick_hands")
    results.append(("quick_hands accepted with heavy_shot (other group)", ok, reply))

    # --- a single-shot gun versus trigger_finger
    out = run(*held("scguns:musket"), enchant("scguns:trigger_finger"))
    reply, ok = outcome(out, "scguns:trigger_finger")
    results.append(("musket refuses trigger_finger (#single_shot)", not ok, reply))

    out = run(*held("scguns:gale"), enchant("scguns:trigger_finger"))
    reply, ok = outcome(out, "scguns:trigger_finger")
    results.append(("a normal gun accepts trigger_finger", ok, reply))

    # --- a sword accepts corroded (it is the melee enchantment)
    out = run(*held("minecraft:iron_sword"), enchant("scguns:corroded"))
    reply, ok = outcome(out, "scguns:corroded")
    results.append(("sword accepts corroded", ok, reply))

    print()
    failed = 0
    for label, ok, reply in results:
        print("  %-52s %s   %s" % (label, "OK" if ok else "FAIL", reply))
        failed += 0 if ok else 1
    print("\n%d/%d checks passed" % (len(results) - failed, len(results)))
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
