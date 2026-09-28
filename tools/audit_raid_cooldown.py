"""The raid cooldown: `minDaysBetweenRaids` must actually decide something (HANDOFF section 80).

The player found this by asking why the option was never read. It never was: `RaidSaveData.canScheduleRaid`,
`setLastRaidDay` and `getLastRaidDay` shipped from 0.5.5 as dead code with no caller anywhere, so a raid
came every night the dusk roll succeeded and the config screen offered a knob that did nothing.

Wiring it up is easy to get half-right, so this audit pins the four properties that make the option mean
what its own config comment says:

  1. the dusk path consults it (`checkForNightlyRaidSpawn` calls `canScheduleRaid`);
  2. the day is recorded only for a raid that **really started** - recorded in the 18000 branch behind
     `hasActiveRaid()`, so a night the sea-level gate or the placement search refused does not push the
     next raid back a day;
  3. the arithmetic matches the documented wording - "0 = every night, 1 = every other night, 2 = further
     apart" is a strict `>`, not the `>=` the dead helper shipped with (which made 0 and 1 identical);
  4. the manual path (`startRaid`, i.e. `/scguns raid start` and the raid flare) ignores the cooldown, and
     the diagnostic reports the cooldown through the same helper - a report with its own copy of the
     arithmetic would keep saying "allowed" after the gate changed.

It also guards the wider failure this found: every `Config.Raids` option must be read from somewhere
outside `Config.java`, so a future option cannot ship as decoration.

Usage:
    python tools/audit_raid_cooldown.py
    python tools/audit_raid_cooldown.py --selftest   # must FAIL against the pre-fix revision
"""

import pathlib
import re
import subprocess
import sys

MANAGER = pathlib.Path("src/main/java/top/ribs/scguns/entity/raid/RaidManager.java")
SAVEDATA = pathlib.Path("src/main/java/top/ribs/scguns/entity/raid/RaidSaveData.java")
COMMANDS = pathlib.Path("src/main/java/top/ribs/scguns/init/ModCommands.java")
CONFIG = pathlib.Path("src/main/java/top/ribs/scguns/Config.java")
SOURCE_ROOT = pathlib.Path("src/main/java")
RELATIVE_MANAGER = "src/main/java/top/ribs/scguns/entity/raid/RaidManager.java"
RELATIVE_SAVEDATA = "src/main/java/top/ribs/scguns/entity/raid/RaidSaveData.java"
RELATIVE_COMMANDS = "src/main/java/top/ribs/scguns/init/ModCommands.java"
RELATIVE_CONFIG = "src/main/java/top/ribs/scguns/Config.java"

# The revision this was written against: HANDOFF section 79, where the diagnostic reported the option as
# unenforced because nothing read it. A fixed id, not HEAD.
PRE_FIX_REVISION = "c6519dc"

# Every option Config.Raids offers. A knob nobody reads is the bug this audit exists for.
RAID_OPTIONS = ["raidsEnabled", "nightlyRaidChance", "minDaysBetweenRaids", "raidTimeoutMinutes"]


def strip_comments(text):
    """Comments name the dead helpers and the old operator on purpose - never count them as code."""
    out = []
    i = 0
    n = len(text)
    while i < n:
        c = text[i]
        if c == '"':
            out.append(c)
            i += 1
            while i < n:
                if text[i] == "\\":
                    out.append(text[i:i + 2])
                    i += 2
                    continue
                out.append(text[i])
                if text[i] == '"':
                    i += 1
                    break
                i += 1
        elif c == "/" and i + 1 < n and text[i + 1] == "/":
            while i < n and text[i] != "\n":
                i += 1
        elif c == "/" and i + 1 < n and text[i + 1] == "*":
            i += 2
            while i + 1 < n and not (text[i] == "*" and text[i + 1] == "/"):
                if text[i] == "\n":
                    out.append("\n")
                i += 1
            i += 2
        else:
            out.append(c)
            i += 1
    return "".join(out)
