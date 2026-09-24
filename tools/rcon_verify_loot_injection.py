"""Verify the mod's loot injection actually happens, on a running dev server, over RCON.

The injection is a global loot modifier list. NeoForge reads exactly one file for it -
data/neoforge/loot_modifiers/global_loot_modifiers.json (verified with javap on
LootModifierManager.prepare) - and this port had it under its own namespace instead, so nothing was
ever injected: no antique-tier guns in mineshafts or dungeons (HANDOFF section 51).

Reading back every dropped stack overflows a single RCON response, so the check asks the game the
narrow question instead: after N rolls, is a given item lying on the ground? Output stays tiny and
the answer is still about the real table.

Usage: python tools/rcon_verify_loot_injection.py
"""
import re
import subprocess
import sys

RCON = [sys.executable, "tools/rcon_cmd.py"]
ROLLS = 30
TABLE = "minecraft:chests/simple_dungeon"
# The antique-tier guns this table is supposed to add, plus two items from the ammo pool.
WANTED = ["scguns:flintlock_pistol", "scguns:longarm", "scguns:musket",
          "scguns:powder_and_ball", "scguns:grapeshot"]


def run(*commands):
    result = subprocess.run(RCON + list(commands), capture_output=True, text=True,
                            cwd=r"E:\mod\scgun-0.5.5-1.21.1-neoforge")
    return result.stdout


def main():
    roll = run("kill @e[type=minecraft:item]",
               *["loot spawn 0 100 0 loot %s" % TABLE] * ROLLS)
    dropped = sum(int(m) for m in re.findall(r"Dropped (\d+) items", roll))
    print("%d chests rolled into the world, %d stacks dropped" % (ROLLS, dropped))

    probes = run(*['execute if entity @e[type=minecraft:item,nbt={Item:{id:"%s"}}]' % item
                   for item in WANTED])
    run("kill @e[type=minecraft:item]")

    found = []
    lines = probes.splitlines()
    for index, line in enumerate(lines):
        if not line.strip().startswith(">"):
            continue
        # the reply to a command is the line right after the echoed command
        reply = lines[index + 1] if index + 1 < len(lines) else ""
        for item in WANTED:
            if item in line and "passed" in reply and item not in found:
                found.append(item)

    print("  injected items present: %s" % (found if found else "none"))
    ok = bool(found)
    print("\nRESULT: %s" % ("loot injection is live" if ok
                            else "NO injected items - the modifier list is not being read"))
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
