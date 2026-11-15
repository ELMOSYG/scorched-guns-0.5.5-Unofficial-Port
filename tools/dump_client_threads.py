"""Dump the running client's threads so a frozen startup leaves evidence instead of a guess.

A client that "hangs at startup" is nearly always one of: a network probe with no useful
timeout, a deadlock between transformers, or a mod waiting on something that never finishes.
None of that is visible in `latest.log` if the main thread simply never gets to its next log
line - which is exactly what happened on 2026-11-15 (the log stopped 17 seconds in, while the
language-pack updater was probing mirrors that time out).

This runs `jcmd <pid> Thread.print` against the client JVM and writes the dump into the
instance's `logs/` folder, then prints a digest: what the main thread / render thread are
doing, and the most common top frames across all threads.

Usage:
    python tools/dump_client_threads.py                 # find the client, dump, print digest
    python tools/dump_client_threads.py --pid 12345     # dump a specific process
    python tools/dump_client_threads.py --digest-only PATH   # digest an existing dump
"""
from __future__ import annotations

import argparse
import datetime
import os
import re
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
INSTANCE = r"D:\MCJAVA\.minecraft\versions\1.21.1-NeoForge_21.1.250"
JDK_BIN = r"D:\jdk-21.0.3\bin"

NAME = re.compile(r'^"(?P<name>[^"]+)".*$')
STATE = re.compile(r"java\.lang\.Thread\.State: (?P<state>\w+)")
FRAME = re.compile(r"^\s+at (?P<frame>[\w.$]+)\((?P<where>[^)]*)\)")


def client_pids() -> list[int]:
    """PIDs whose command line points at the client instance (the launcher passes --gameDir)."""
    try:
        out = subprocess.run(
            ["powershell", "-NoProfile", "-Command",
             "Get-CimInstance Win32_Process -Filter \"Name='javaw.exe' OR Name='java.exe'\" | "
             "Select-Object ProcessId,CommandLine | ConvertTo-Json -Compress"],
            capture_output=True, text=True, encoding="utf-8", errors="replace", check=False,
        ).stdout
    except OSError as error:
        print("cannot list processes: %s" % error)
        return []

    import json
    try:
        entries = json.loads(out) if out.strip() else []
    except json.JSONDecodeError:
        return []
    if isinstance(entries, dict):
        entries = [entries]

    pids = []
    for entry in entries:
        command = entry.get("CommandLine") or ""
        if INSTANCE.lower() in command.lower() and "forgeclient" in command.lower():
            pids.append(int(entry["ProcessId"]))
    return pids


def dump(pid: int) -> str:
    os.makedirs(os.path.join(INSTANCE, "logs"), exist_ok=True)
    stamp = datetime.datetime.now().strftime("%Y%m%d-%H%M%S")
    path = os.path.join(INSTANCE, "logs", "thread-dump-%s.txt" % stamp)

    for tool in (os.path.join(JDK_BIN, "jcmd.exe"), "jcmd"):
        try:
            result = subprocess.run([tool, str(pid), "Thread.print"],
                                    capture_output=True, text=True, encoding="utf-8", errors="replace",
                                    timeout=120, check=False)
        except (OSError, subprocess.TimeoutExpired):
            continue
        if result.returncode == 0 and result.stdout.strip():
            with open(path, "w", encoding="utf-8", newline="\n") as fh:
                fh.write(result.stdout)
            return path
        print("jcmd failed via %s: %s" % (tool, (result.stderr or "").strip()[:200]))

    raise SystemExit("could not dump pid %d - is it a JDK process and are you the same user?" % pid)


def digest(path: str) -> None:
    text = open(path, encoding="utf-8", errors="replace").read()
    blocks = re.split(r"\n(?=\")", text)

    print("dump: %s (%d thread blocks)\n" % (path, len(blocks)))
    print("=== main / render / reload threads ===")
    for block in blocks:
        header = NAME.match(block.splitlines()[0]) if block.strip() else None
        if not header:
            continue
        name = header.group("name")
        if not any(key in name for key in ("main", "Render thread", "reload", "Worker-Main")):
            continue
        state = STATE.search(block)
        frames = [FRAME.match(line) for line in block.splitlines()]
        frames = [match.group("frame") for match in frames if match][:6]
        print("  %-28s %-14s %s" % (name[:28], state.group("state") if state else "?", " <- ".join(frames)))

    print("\n=== most common top frames across all threads ===")
    counts: dict[str, int] = {}
    for block in blocks:
        for line in block.splitlines():
            match = FRAME.match(line)
            if match:
                counts[match.group("frame")] = counts.get(match.group("frame"), 0) + 1
                break
    for frame, count in sorted(counts.items(), key=lambda item: -item[1])[:12]:
        print("  %3d  %s" % (count, frame))

    print("\n=== sockets / http / known startup blockers ===")
    for needle in ("SocketInputStream", "HttpClient", "I18nAutoUpdate", "Connect timed out",
                   "ServiceLoader", "TransformingClassLoader"):
        hits = text.count(needle)
        if hits:
            print("  %-24s %d occurrence(s)" % (needle, hits))


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--pid", type=int)
    parser.add_argument("--digest-only", metavar="PATH")
    args = parser.parse_args()

    if args.digest_only:
        digest(args.digest_only)
        return 0

    pids = [args.pid] if args.pid else client_pids()
    if not pids:
        print("no running client found for %s" % INSTANCE)
        print("start the game first, and run this while the window looks stuck (before closing it)")
        return 1

    for pid in pids:
        path = dump(pid)
        digest(path)
    return 0


if __name__ == "__main__":
    sys.exit(main())
