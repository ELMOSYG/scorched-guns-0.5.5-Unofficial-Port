#!/usr/bin/env python
"""Say who a crash belongs to, in one line, so long test runs do not need manual reading.

The failures in this pack have a habit of looking like one mod and being another: a mod that fails to
initialise aborts the load, and whichever mod runs next reports a confusing error of its own (Sodium
blaming Iris for missing metadata, for instance). This script reads the newest crash report, pulls out
the parts that identify the origin, and decides who is responsible.

usage:
    python tools/diagnose_crash.py                     # newest crash report, all known instances
    python tools/diagnose_crash.py --all               # every crash report, one line each
    python tools/diagnose_crash.py --file <path>       # a specific report
    python tools/diagnose_crash.py --watch 3600        # poll for a new report for up to an hour

Exit code 0 when a report was parsed, 1 when none was found.
"""

from __future__ import annotations

import argparse
import datetime
import glob
import os
import re
import sys
import time

# Instances we look at, newest report first.
INSTANCE_DIRS = (
    r"D:\MCJAVA\.minecraft\versions\1.21.1-NeoForge_21.1.250\crash-reports",
    r"D:\MCJAVA\.minecraft\versions\航空学SG2-test\crash-reports",
    r"E:\mod\scgun-0.5.5-1.21.1-neoforge\run\crash-reports",
)

# Mods whose failure we can fix ourselves. Everything else is reported as "not ours".
OUR_MOD_IDS = ("scguns", "scg2_maid_compat")

# Turns a noisy javadoc-style header comment into a hint that the report is a follow-on, not the origin.
FOLLOW_ON_HINTS = (
    "Please make sure this issue is not caused by",
    "before reporting it to other mod authors",
)


def find_reports() -> list[str]:
    found: list[str] = []
    for directory in INSTANCE_DIRS:
        found.extend(glob.glob(os.path.join(directory, "*.txt")))
    return sorted(found, key=os.path.getmtime, reverse=True)


def first(lines: list[str], pattern: str, group: int = 1) -> str | None:
    for line in lines:
        match = re.search(pattern, line)
        if match:
            return match.group(group).strip()
    return None


def diagnose(path: str) -> dict[str, object]:
    with open(path, encoding="utf-8", errors="replace") as handle:
        lines = handle.read().split("\n")

    description = first(lines, r"Description:\s*(.+)$")
    crash_report = path.replace("\\", "/").rsplit("/", 1)[1]

    # "Mod loading issue for: X" identifies the mod FML held responsible at the first failure.
    issue_mods: list[str] = []
    for line in lines:
        match = re.search(r"-- Mod loading issue for:\s*([^\s]+)\s*--", line)
        if match and match.group(1) not in issue_mods:
            issue_mods.append(match.group(1))

    exception_message = first(lines, r"Exception message:\s*(.+)$")
    failure_message = first(lines, r"Failure message:\s*(.+)$")

    # The top of the first stack trace is usually the true origin.
    top_frames: list[str] = []
    for line in lines:
        if re.match(r"\s+at ", line):
            top_frames.append(line.strip())
            if len(top_frames) >= 6:
                break

    # Which mod packages appear in the top frames? TRANSPORTER/<modid>@<version>/ is the FML marker.
    frame_mods: list[str] = []
    for frame in top_frames:
        match = re.search(r"TRANSFORMER/([a-z0-9_]+)@", frame)
        if match and match.group(1) not in frame_mods:
            frame_mods.append(match.group(1))

    follow_on = any(hint in "\n".join(lines[:12]) for hint in FOLLOW_ON_HINTS)

    # A report whose first failure is "Mod X not found in ModList" or a missing dependency is almost
    # always a consequence of an earlier abort, not the cause.
    consequence = bool(
        re.search(r"Mod with id .* not found in ModList", "\n".join(lines[:40]))
        or re.search(r"Missing or unsupported mandatory dependencies", "\n".join(lines[:60]))
        or re.search(r"Access transformer file", "\n".join(lines[:60]))
    )

    blamed = list(issue_mods)
    if not blamed and frame_mods:
        blamed = frame_mods[:2]

    ours = [m for m in blamed if m in OUR_MOD_IDS]
    verdict = "OURS" if ours else ("UNKNOWN" if not blamed else "NOT OURS")

    return {
        "report": crash_report,
        "time": datetime.datetime.fromtimestamp(os.path.getmtime(path)).strftime("%Y-%m-%d %H:%M"),
        "description": description,
        "issue_mods": issue_mods,
        "frame_mods": frame_mods,
        "exception": exception_message,
        "failure": failure_message,
        "top_frames": top_frames,
        "follow_on": follow_on,
        "consequence": consequence,
        "verdict": verdict,
    }


def print_one(info: dict[str, object], verbose: bool) -> None:
    print("%s  %s" % (info["time"], info["report"]))
    print("  description : %s" % info["description"])
    if info["issue_mods"]:
        print("  issue for   : %s" % ", ".join(info["issue_mods"]))  # type: ignore[arg-type]
    if info["frame_mods"]:
        print("  top frames  : %s" % ", ".join(info["frame_mods"]))  # type: ignore[arg-type]
    if info["exception"]:
        print("  exception   : %s" % str(info["exception"])[:150])
    if info["failure"]:
        print("  failure     : %s" % str(info["failure"])[:150])
    if info["consequence"]:
        print("  note        : this looks like a CONSEQUENCE of an earlier failure, not the origin")
    if info["follow_on"]:
        print("  note        : the header disclaimer belongs to a mod that injects itself into reports")
    print("  verdict     : %s" % info["verdict"])
    if verbose and info["top_frames"]:
        print("  stack:")
        for frame in info["top_frames"]:  # type: ignore[union-attr]
            print("    " + frame[:150])
    print()


def main() -> int:
    parser = argparse.ArgumentParser(description="Identify which mod a crash report belongs to.")
    parser.add_argument("--file", help="a specific crash report")
    parser.add_argument("--all", action="store_true", help="summarise every report")
    parser.add_argument("--verbose", action="store_true", help="also print the top stack frames")
    parser.add_argument("--watch", type=int, metavar="SECONDS", help="wait for a newer report, up to this long")
    args = parser.parse_args()

    if args.file:
        if not os.path.isfile(args.file):
            print("no such report: %s" % args.file)
            return 1
        print_one(diagnose(args.file), True)
        return 0

    reports = find_reports()
    if not reports:
        print("no crash reports found in:")
        for directory in INSTANCE_DIRS:
            print("  %s" % directory)
        return 1

    if args.watch:
        newest = os.path.getmtime(reports[0])
        print("watching for a new crash report (up to %d s)..." % args.watch)
        deadline = time.time() + args.watch
        while time.time() < deadline:
            time.sleep(5)
            current = find_reports()
            if current and os.path.getmtime(current[0]) > newest:
                print("\nnew crash report detected:\n")
                print_one(diagnose(current[0]), True)
                return 0
        print("no new report appeared")
        return 0

    if args.all:
        for path in reports:
            info = diagnose(path)
            print("%s  %-40s %-10s %s" % (info["time"], info["report"][:40], info["verdict"], info["description"]))
        return 0

    print_one(diagnose(reports[0]), args.verbose)
    return 0


if __name__ == "__main__":
    sys.exit(main())
