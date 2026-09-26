"""`/scguns raid check`: the on-demand report of the nightly raid decision (HANDOFF section 79).

A natural raid is nearly untestable by hand: it rolls at dusk (13000) and only starts at 18000, four
minutes of real time later, and then does nothing at all - silently - when the player is below sea
level, when the ground has no open surface column, when the dusk roll failed, when the chosen player has
raid level 0, or when a raid is already running. Every one of those looks the same from the outside:
no raid.

`/scguns raid check` prints the same decisions on demand. The value of that command is entirely in two
properties, and this audit exists to keep both of them true:

  1. **It reports, it does not act.** The moment it starts, schedules or ends a raid it stops being a
     diagnostic and becomes a second, divergent way to trigger a raid; `/scguns raid start` and
     `startnext` already exist for that.
  2. **It asks the scheduler's own code.** `RaidManager.canGetNaturalRaid`, `findRaidSpawnLocation`
     and `RaidSaveData.getScheduledRaid` are called, never reimplemented - a copy of the gate would
     keep printing ok after the gate itself changed, which is exactly the failure mode that made the
     underground-raid bug (HANDOFF section 75) take three rounds to find.

It also checks the 14 `commands.scguns.raid.check.*` lang keys: present in both `en_us.json` and
`zh_cn.json`, and with the same number of `%s` placeholders in each - a translator editing one file
only, or a missing key, turns the report into a raw key or a crash instead of an answer.

Usage:
    python tools/audit_raid_check_command.py
    python tools/audit_raid_check_command.py --selftest   # must FAIL against the pre-fix revision
"""

import json
import pathlib
import re
import subprocess
import sys

COMMANDS = pathlib.Path("src/main/java/top/ribs/scguns/init/ModCommands.java")
MANAGER = pathlib.Path("src/main/java/top/ribs/scguns/entity/raid/RaidManager.java")
EN = pathlib.Path("src/main/resources/assets/scguns/lang/en_us.json")
ZH = pathlib.Path("src/main/resources/assets/scguns/lang/zh_cn.json")
RELATIVE = "src/main/java/top/ribs/scguns/init/ModCommands.java"

# The revision this was written against: HANDOFF section 78, before the diagnostic existed. A fixed id,
# not HEAD - comparing against HEAD makes a selftest pass as soon as the change is committed.
PRE_FIX_REVISION = "1bd9f60"

PREFIX = "commands.scguns.raid.check."

# The calls the report has to make, and what each one is for.
REQUIRED_CALLS = {
    "RaidManager.canGetNaturalRaid": "the sea-level gate the scheduler applies at spawn time",
    "findRaidSpawnLocation": "the surface placement search the scheduler uses",
    "getScheduledRaid": "tonight's schedule, the record that decides whether a raid is coming",
    "getCurrentRaidLevel": "the progression level the scheduler picks a raid from",
    "canScheduleRaid": "the minDaysBetweenRaids cooldown the scheduler applies before it rolls (section 80)",
}

# Starting, scheduling or ending a raid from a report would make it a second trigger.
FORBIDDEN_CALLS = ["startRaid", "scheduleRaid", "endRaid", "surrenderRaid", "startRaidFromPlayer"]


def strip_comments(text):
    """Comments quote the old behaviour and the forbidden calls on purpose - never count them."""
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


def call_arguments(text, start):
    """Split the top-level arguments of the call whose opening paren is the last one before `start`
    (the key string is the first argument, so the call's paren is *behind* it - searching forward lands
    on the first nested call instead, which is how the first version of this audit counted
    `dimension.toString()` as the whole argument list). Honours nested parentheses and string literals."""
    i = text.rindex("(", 0, start)
    depth, current, args = 0, "", []
    in_string = False
    while i < len(text):
        char = text[i]
        if in_string:
            if char == "\\":
                current += text[i:i + 2]
                i += 2
                continue
            if char == '"':
                in_string = False
        elif char == '"':
            in_string = True
        elif char == "(":
            depth += 1
            if depth == 1:
                i += 1
                continue
        elif char == ")":
            depth -= 1
            if depth == 0:
                args.append(current.strip())
                return args
        elif char == "," and depth == 1:
            args.append(current.strip())
            current = ""
            i += 1
            continue
        current += char
        i += 1
    return args


def placeholders(pattern):
    """Count real format specifiers: `%%` is a literal percent sign, and a specifier may carry flags
    (`%1$s`). The naive `%(?!%)` regex counts the second half of `%%` as a placeholder, which is how the
    first version of this audit demanded an argument for "chance 20% per night"."""
    count, i = 0, 0
    while i < len(pattern):
        if pattern[i] != "%":
            i += 1
            continue
        if i + 1 < len(pattern) and pattern[i + 1] == "%":
            i += 2
            continue
        count += 1
        i += 1
        while i < len(pattern) and pattern[i] in "0123456789$.-+ #<":
            i += 1
        i += 1
    return count


def lang_check(problems):
    for path in (EN, ZH):
        if not path.exists():
            problems.append("%s is missing" % path)
            continue
        data = json.loads(path.read_text(encoding="utf-8"))
        keys = [k for k in data if k.startswith(PREFIX)]
        if len(keys) < 17:
            problems.append("%s has only %d %s* key(s)" % (path.name, len(keys), PREFIX))
    if not (EN.exists() and ZH.exists()):
        return {}
    english = json.loads(EN.read_text(encoding="utf-8"))
    chinese = json.loads(ZH.read_text(encoding="utf-8"))
    for key in sorted(i for i in english if i.startswith(PREFIX)):
        if key not in chinese:
            problems.append("%s is missing from zh_cn.json" % key)
            continue
        if placeholders(english[key]) != placeholders(chinese[key]):
            problems.append("%s has %d placeholder(s) in en_us.json but %d in zh_cn.json"
                            % (key, placeholders(english[key]), placeholders(chinese[key])))
    return english


def check(commands_text, manager_text, english):
    commands_text = strip_comments(commands_text)
    manager_text = strip_comments(manager_text)
    problems = []

    register = method_body(commands_text, "register")
    # `/scguns progression check` is also a `check`, so the registration has to be tied to the raid
    # node: the sequence `literal("raid")` ... `literal("check").executes(... executeRaidCheck ...)`.
    raid_at = register.find('literal("raid")')
    registered = re.search(r'literal\("check"\)\s*\.executes\([\s\S]{0,200}?executeRaidCheck', register)
    if raid_at < 0 or registered is None or registered.start() < raid_at:
        problems.append("the raid command has no `check` subcommand, so the nightly decision cannot be "
                        "inspected without waiting for dusk")

    report = method_body(commands_text, "executeRaidCheck")
    if not report:
        problems.append("executeRaidCheck is missing")
    else:
        for needle, purpose in REQUIRED_CALLS.items():
            if needle not in report:
                problems.append("the report does not call %s (%s), so it can disagree with the real gate"
                                % (needle, purpose))
        for needle in FORBIDDEN_CALLS:
            if needle in report:
                problems.append("the report calls %s - a diagnostic must not start, schedule or end a raid"
                                % needle)

    # The gate must stay reachable: the command can only call what the raid classes expose.
    if "boolean canGetNaturalRaid" not in manager_text:
        problems.append("RaidManager.canGetNaturalRaid is gone, so the report cannot ask the real gate")
    if "public Vec3 findRaidSpawnLocation" not in manager_text:
        problems.append("RaidManager.findRaidSpawnLocation is not public, so the report cannot ask the "
                        "real placement search")

    # Every key the command prints must exist, and its argument count must match the placeholders.
    if english:
        for key, args in reported_keys(commands_text).items():
            if key not in english:
                problems.append("%s is printed but not defined in en_us.json" % key)
                continue
            text_args = [a for a in args if a]
            want = placeholders(english[key])
            if len(text_args) != want:
                problems.append("%s is called with %d argument(s) but its text has %d placeholder(s)"
                                % (key, len(text_args), want))
    return problems


def reported_keys(commands_text):
    """Every `Component.translatable("commands.scguns.raid.check...")` call in the command, mapped to
    its arguments *after* the key (`call_arguments` includes the key itself as the first argument)."""
    found = {}
    for match in re.finditer(r'"(%s[a-z_.]+)"' % re.escape(PREFIX), commands_text):
        # A key inside a ternary is still one key, and both branches share the call's argument list.
        found.setdefault(match.group(1), call_arguments(commands_text, match.start())[1:])
    return found


def selftest():
    try:
        before = subprocess.run(["git", "show", "%s:%s" % (PRE_FIX_REVISION, RELATIVE)],
                                capture_output=True, text=True, encoding="utf-8", check=True).stdout
        manager = subprocess.run(["git", "show", "%s:%s" % (PRE_FIX_REVISION,
                                "src/main/java/top/ribs/scguns/entity/raid/RaidManager.java")],
                                capture_output=True, text=True, encoding="utf-8", check=True).stdout
        english = json.loads(subprocess.run(["git", "show", "%s:%s" % (PRE_FIX_REVISION,
                                 "src/main/resources/assets/scguns/lang/en_us.json")],
                                 capture_output=True, text=True, encoding="utf-8", check=True).stdout)
    except (subprocess.CalledProcessError, FileNotFoundError, json.JSONDecodeError) as error:
        print("selftest: cannot read revision %s (%s)" % (PRE_FIX_REVISION, error))
        return 1

    expected = [
        "no `check` subcommand",
        "executeRaidCheck is missing",
        "not public",
    ]
    found = check(before, manager, english)
    print("selftest: revision %s reports %d problem(s)" % (PRE_FIX_REVISION, len(found)))
    for problem in found:
        print("   %s" % problem)
    missing = [e for e in expected if not any(e in f for f in found)]
    if missing:
        for entry in missing:
            print("selftest MISSING: %s" % entry)
        print("selftest FAILED: the audit does not notice a missing raid-check command")
        return 1
    print("selftest OK: the missing command and the still-private placement search are detected")
    return 0


def main():
    if "--selftest" in sys.argv:
        return selftest()
    for path in (COMMANDS, MANAGER):
        if not path.exists():
            print("FAIL: %s is missing" % path)
            return 1
    problems = []
    english = lang_check(problems)
    problems += check(COMMANDS.read_text(encoding="utf-8"), MANAGER.read_text(encoding="utf-8"), english)
    for problem in problems:
        print("  %s" % problem)
    if problems:
        print("%d problem(s): /scguns raid check would not answer why no raid came" % len(problems))
        return 1
    print("0 problem(s): /scguns raid check reports the scheduler's own decisions, in both languages")
    return 0


if __name__ == "__main__":
    sys.exit(main())
