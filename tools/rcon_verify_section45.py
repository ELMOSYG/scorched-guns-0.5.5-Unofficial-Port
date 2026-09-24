"""Verify the two bugs fixed in HANDOFF section 45 on a running dev server, over RCON.

1. GunBenchBlockEntity.saveAdditional used to throw "Cannot encode empty ItemStack" on every chunk
   save, so the bench never persisted. A bench with one item and eleven empty slots is saved here
   with `save-all`, and the server log is checked for that message.
2. SulfurPoisoningEffect.getFireDamageMultiplier cast a Holder<MobEffect> to the effect class, which
   threw a ClassCastException inside the hurt event and crashed the game. A mob with the effect is
   summoned and then damaged, and the log is checked for that stack trace.

Both are evaluated from the server log, which is the only thing that can show them: they happen on
paths a headless server can drive but a screenshot cannot.
"""
import pathlib
import re
import subprocess
import sys

LOG = pathlib.Path(r"E:\mod\scgun-0.5.5-1.21.1-neoforge\build-logs\server-45.txt")
RCON = [sys.executable, r"tools\rcon_cmd.py"]


def rcon(*commands):
    result = subprocess.run(RCON + list(commands), capture_output=True, text=True, cwd=r"E:\mod\scgun-0.5.5-1.21.1-neoforge")
    print(result.stdout.strip())
    if result.returncode != 0:
        print(result.stderr.strip())
    return result.stdout


def main():
    before = LOG.read_text(encoding="utf-8", errors="replace")

    # 1. the bench must persist with empty slots in it
    rcon("setblock 0 100 0 scguns:gun_bench",
         'data merge block 0 100 0 {Item0:{id:"minecraft:stone",count:1}}',
         "save-all flush",
         "list")

    after = LOG.read_text(encoding="utf-8", errors="replace")
    new_text = after[len(before):]

    empty_encode = "Cannot encode empty ItemStack" in new_text
    cast = "cannot be cast to class top.ribs.scguns.effect.SulfurPoisoningEffect" in new_text
    print("\nbench save: %s" % ("THREW on empty stack" if empty_encode else "no empty-stack error"))

    # 2. the sulfur poisoning hurt path must not throw
    marker = len(after)
    rcon("summon minecraft:zombie 0 100 0",
         "effect give @e[type=minecraft:zombie,limit=1] scguns:sulfur_poisoning 200 0",
         "damage @e[type=minecraft:zombie,limit=1] 3 minecraft:in_fire",
         "list")
    after2 = LOG.read_text(encoding="utf-8", errors="replace")
    poison_text = after2[marker:]
    cast2 = "cannot be cast to class top.ribs.scguns.effect.SulfurPoisoningEffect" in poison_text
    tick_crash = "Ticking entity" in poison_text
    print("sulfur poisoning damage: %s" % ("THREW ClassCastException" if cast2 or cast else "no ClassCastException"))

    ok = not empty_encode and not cast2 and not tick_crash
    print("\nRESULT: %s" % ("both paths are clean" if ok else "FAILURES above"))
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
