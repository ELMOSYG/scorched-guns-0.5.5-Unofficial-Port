"""A raid must not be declared defeated just because its boss is out of view.

`ActiveRaid` finds the boss with `level.getEntity(uuid)`, which returns **null for an entity in an
unloaded chunk** - not only for a dead one. 0.5.5 (and this port until HANDOFF section 74) ran

    } else {
       this.endRaid(this.bossConfirmed);
    }

in `tick()`, so the moment the boss's chunk unloaded - the player died and respawned away from it, was
teleported, or simply walked off - the raid was ended as **defeated**: the boss bar was hidden for every
player, `RaidManager` dropped the raid from its map and from the save, and because the raid's special
loot table is only rolled from `RaidManager.onEntityDeath` while the raid is tracked, killing the boss
afterwards dropped nothing. That is the reported bug: "the bar disappears when I die and there is no
loot".

This audit fails when:
  1. the unresolvable case is treated as a defeat again (`endRaid(this.bossConfirmed)`);
  2. the boss being unresolvable does not pull its chunk back in (no `keepBossChunkLoaded`);
  3. the "boss not found" wait is not bounded by `BOSS_LOST_GRACE_TICKS`;
  4. the chunk the raid forced open is never released (0.5.5 leaked the one it took while waiting for
     the boss to appear, which keeps a chunk loaded forever after the raid is gone).

Client-only, unrelated to this file's logic: see HANDOFF section 74 for the verification probe output.

Usage:
    python tools/audit_raid_unload_safety.py
    python tools/audit_raid_unload_safety.py --selftest   # must FAIL against the pre-fix source
"""

import pathlib
import re
import subprocess
import sys

SOURCE = pathlib.Path("src/main/java/top/ribs/scguns/entity/raid/ActiveRaid.java")
RELATIVE = "src/main/java/top/ribs/scguns/entity/raid/ActiveRaid.java"


def method_body(text, name):
    match = re.search(r"\b%s\s*\([^)]*\)\s*\{" % re.escape(name), text, re.S)
    if not match:
        return ""
    depth, i = 0, match.end() - 1
    while i < len(text):
        if text[i] == "{":
            depth += 1
        elif text[i] == "}":
            depth -= 1
            if depth == 0:
                return text[match.end():i]
        i += 1
    return ""


def check(text):
    problems = []

    if "endRaid(this.bossConfirmed)" in text:
        problems.append("tick() still ends the raid with endRaid(this.bossConfirmed): an unloaded boss "
                        "is treated as a defeated one, which hides the bar and makes the special loot "
                        "impossible to get")

    tick = method_body(text, "tick")
    if "keepBossChunkLoaded" not in tick:
        problems.append("tick() does not pull an unresolvable boss's chunk back in")
    if "BOSS_LOST_GRACE_TICKS" not in tick:
        problems.append("tick() gives up on an unresolvable boss without the bounded grace period")
    if not re.search(r"else if \(boss != null\)", tick):
        problems.append("tick() does not tell 'boss resolved and dead' apart from 'boss not resolvable'")

    validate = method_body(text, "validateBoss")
    if "keepBossChunkLoaded" not in validate:
        problems.append("validateBoss() does not keep the boss's chunk loaded while waiting for it")
    if "BOSS_LOST_GRACE_TICKS" not in validate:
        problems.append("validateBoss() still uses the short pre-confirmation timeout for an unloaded "
                        "boss, so a restored raid is thrown away after 30 seconds")

    end = method_body(text, "endRaid")
    if "releaseForcedChunk" not in end:
        problems.append("endRaid() does not release the chunk this raid forced open")

    resolved = method_body(text, "onBossResolved")
    if "releaseForcedChunk" not in resolved or "ticksSinceLoad = 0" not in resolved:
        problems.append("onBossResolved() does not clear the lost-boss state")

    return problems


def selftest():
    try:
        before = subprocess.run(["git", "show", "HEAD:%s" % RELATIVE],
                                capture_output=True, text=True, check=True).stdout
    except (subprocess.CalledProcessError, FileNotFoundError) as error:
        print("selftest: cannot read HEAD:%s (%s)" % (RELATIVE, error))
        return 1

    expected = [
        "endRaid(this.bossConfirmed)",
        "tick() does not pull an unresolvable boss's chunk back in",
        "validateBoss() does not keep the boss's chunk loaded while waiting for it",
        "endRaid() does not release the chunk this raid forced open",
    ]
    found = check(before)
    print("selftest: pre-fix source reports %d problem(s)" % len(found))
    for problem in found:
        print("   %s" % problem)
    missing = [e for e in expected if not any(e in f for f in found)]
    if missing:
        for entry in missing:
            print("selftest MISSING: %s" % entry)
        print("selftest FAILED: the audit does not catch the bug it exists for")
        return 1
    print("selftest OK: the pre-fix raid-unload bug is detected")
    return 0


def main():
    if "--selftest" in sys.argv:
        return selftest()
    if not SOURCE.exists():
        print("FAIL: %s is missing" % SOURCE)
        return 1
    problems = check(SOURCE.read_text(encoding="utf-8"))
    for problem in problems:
        print("  %s" % problem)
    if problems:
        print("%d problem(s): a raid can be thrown away while its boss is merely unloaded" % len(problems))
        return 1
    print("0 problem(s): an unloaded boss keeps the raid (and its bar and its loot) alive")
    return 0


if __name__ == "__main__":
    sys.exit(main())
