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
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    return re.sub(r"//[^\n]*", "", text)


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


class WorkTree:
    """The checkout as it is on disk."""

    def __init__(self, root):
        self.root = root

    def readers(self, option):
        """Files (relative to src/main/java) that read a Config.Raids option.

        `Config.java` declares the options and `ModCommands.java` only *prints* them in the raid
        diagnostic, so neither counts as acting on one: at the revision this audit was written against,
        minDaysBetweenRaids was read by exactly the diagnostic that reported it as unenforced.
        """
        found = []
        pattern = re.compile(r"raids\.%s\b" % re.escape(option))
        for path in self.root.rglob("*.java"):
            if path.name in ("Config.java", "ModCommands.java"):
                continue
            if pattern.search(path.read_text(encoding="utf-8", errors="replace")):
                found.append(path.relative_to(self.root).as_posix())
        return found


class GitTree:
    """A revision's sources, through `git grep` - so the selftest can ask the same dead-config question
    about the pre-fix tree without checking out ~1000 files."""

    def __init__(self, revision):
        self.revision = revision

    def readers(self, option):
        result = subprocess.run(
            ["git", "grep", "-l", "-E", r"raids\.%s\b" % option, self.revision, "--", "src/main/java"],
            capture_output=True, text=True, encoding="utf-8")
        if result.returncode not in (0, 1):
            raise RuntimeError(result.stderr.strip() or "git grep failed")
        paths = [line.split(":", 1)[1] for line in result.stdout.splitlines() if ":" in line]
        return [p for p in paths if not p.endswith(("/Config.java", "/ModCommands.java"))]


def check(manager, savedata, commands, tree):
    manager = strip_comments(manager)
    savedata = strip_comments(savedata)
    commands = strip_comments(commands)
    problems = []

    nightly = method_body(manager, "checkForNightlyRaidSpawn")
    if "canScheduleRaid(" not in nightly:
        problems.append("the dusk path never consults the cooldown, so minDaysBetweenRaids is decoration "
                        "again and a raid can come every night")
    if "minDaysBetweenRaids" not in nightly:
        problems.append("the cooldown is not driven by the minDaysBetweenRaids option")
    if "setLastRaidDay(" not in nightly:
        problems.append("no day is recorded when a natural raid starts, so the cooldown can never lift "
                        "or never engage")
    elif "hasActiveRaid()" not in nightly:
        problems.append("the day is recorded without checking that a raid really started, so a night the "
                        "gate or the placement refused would count against the next raid")

    # The manual path must stay free of it: a raid flare or /scguns raid start is the player asking.
    manual = method_body(manager, "startRaid")
    for needle in ("canScheduleRaid", "setLastRaidDay", "minDaysBetweenRaids"):
        if needle in manual:
            problems.append("the manual path (startRaid) consults %s, so a raid the player asked for "
                            "would be refused or would start a cooldown" % needle)

    cooldown = method_body(savedata, "canScheduleRaid")
    if not cooldown:
        problems.append("RaidSaveData.canScheduleRaid is missing")
    elif re.search(r"currentDay\s*-\s*lastRaidDay\s*>=", cooldown):
        problems.append("the cooldown uses >=, which makes 0 and 1 behave identically (every night) and "
                        "contradicts the option's own comment: 1 means every other night")
    elif not re.search(r"currentDay\s*-\s*lastRaidDay\s*>", cooldown):
        problems.append("the cooldown arithmetic is unrecognisable: expected `currentDay - lastRaidDay > ...`")

    # The report has to ask the same helper, or it will disagree with the scheduler.
    report = method_body(commands, "executeRaidCheck")
    if not report:
        problems.append("executeRaidCheck is missing, so the cooldown cannot be inspected in game")
    elif "canScheduleRaid" not in report:
        problems.append("the raid report does not use canScheduleRaid, so it can claim a raid is due "
                        "while the scheduler is still cooling down")

    for option in RAID_OPTIONS:
        if not tree.readers(option):
            problems.append("Config.Raids.%s is read by nothing that acts on it - only Config.java "
                            "declares it, and a diagnostic printing a value does not implement it" % option)

    return problems


def selftest():
    try:
        def show(path):
            return subprocess.run(["git", "show", "%s:%s" % (PRE_FIX_REVISION, path)],
                                  capture_output=True, text=True, encoding="utf-8", check=True).stdout

        manager = show(RELATIVE_MANAGER)
        savedata = show(RELATIVE_SAVEDATA)
        commands = show(RELATIVE_COMMANDS)
        found = check(manager, savedata, commands, GitTree(PRE_FIX_REVISION))
    except (subprocess.CalledProcessError, FileNotFoundError, RuntimeError) as error:
        print("selftest: cannot read revision %s (%s)" % (PRE_FIX_REVISION, error))
        return 1

    expected = [
        "the dusk path never consults the cooldown",
        "no day is recorded when a natural raid starts",
        "the cooldown uses >=",
        "the raid report does not use canScheduleRaid",
        "Config.Raids.minDaysBetweenRaids is read by nothing that acts on it",
    ]
    print("selftest: revision %s reports %d problem(s)" % (PRE_FIX_REVISION, len(found)))
    for problem in found:
        print("   %s" % problem)
    missing = [e for e in expected if not any(e in f for f in found)]
    if missing:
        for entry in missing:
            print("selftest MISSING: %s" % entry)
        print("selftest FAILED: the audit does not notice a cooldown nobody consults")
        return 1
    print("selftest OK: the unwired cooldown, its wrong arithmetic and the dead option are all detected")
    return 0


def main():
    if "--selftest" in sys.argv:
        return selftest()
    for path in (MANAGER, SAVEDATA, COMMANDS):
        if not path.exists():
            print("FAIL: %s is missing" % path)
            return 1
    problems = check(MANAGER.read_text(encoding="utf-8"),
                     SAVEDATA.read_text(encoding="utf-8"),
                     COMMANDS.read_text(encoding="utf-8"),
                     WorkTree(SOURCE_ROOT))
    for problem in problems:
        print("  %s" % problem)
    if problems:
        print("%d problem(s): minDaysBetweenRaids does not decide when a raid may happen" % len(problems))
        return 1
    print("0 problem(s): the cooldown gates the dusk roll, records only real raids, and is reported")
    return 0


if __name__ == "__main__":
    sys.exit(main())
