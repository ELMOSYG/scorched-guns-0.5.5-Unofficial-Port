"""Install a jar into the user's instance - but never while their game is running.

Why this exists: on 2026-09-23 23:37 a jar was copied into mods/ while the player's client was
running (started 23:31). The JVM had the mod jar open and had cached entry offsets from the old
file, so 25 seconds later it threw ClassNotFoundException for a class the new file does contain,
and the game crashed. Replacing a mod jar under a running game is not safe; this refuses instead.

Usage:
    python tools/install_jar.py                      # install build/libs/scguns-0.5.5.jar
    python tools/install_jar.py --check              # only report whether it is safe
    python tools/install_jar.py --force              # install anyway (you accept the crash risk)
"""
import argparse
import datetime
import pathlib
import shutil
import subprocess
import sys

MODS = pathlib.Path(r"D:\MCJAVA\.minecraft\versions\1.21.1-NeoForge_21.1.250\mods")
TARGET = MODS / "scguns-0.5.5.jar"
SOURCE = pathlib.Path("build/libs/scguns-0.5.5.jar")
GAME_DIR_MARKER = r"1.21.1-NeoForge_21.1.250"


def game_running():
    """Client or dev server JVMs that have this instance (or the dev run) open."""
    try:
        out = subprocess.run(
            ["powershell", "-NoProfile", "-Command",
             "Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | "
             "Select-Object -ExpandProperty CommandLine"],
            capture_output=True, text=True, timeout=60).stdout
    except Exception as error:  # pragma: no cover - diagnostic only
        return [("could not inspect processes: %s" % error, "")]
    found = []
    for line in out.splitlines():
        if not line.strip():
            continue
        if GAME_DIR_MARKER in line:
            found.append(("Minecraft client", line.strip()[:160]))
        elif "devlaunch" in line:
            found.append(("dev run", line.strip()[:160]))
    return found


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--force", action="store_true")
    args = parser.parse_args()

    running = game_running()
    if running:
        print("REFUSING: a game process is running and holds the mod jar open;")
        print("replacing it now is what crashed the client on 2026-09-23 23:38.")
        for kind, cmd in running:
            print("   %s: %s" % (kind, cmd))
        print("\nClose the game first, then run this again.")
        if not args.force:
            return 1
        print("--force given: installing anyway.")

    if args.check:
        print("no game process found%s" % (" (forced)" if running else ""))
        return 0

    if not SOURCE.exists():
        print("MISSING: %s (run `gradlew build` first)" % SOURCE)
        return 2

    stamp = datetime.datetime.now().strftime("%H%M%S")
    backup = TARGET.with_name("%s.bak-%s" % (TARGET.name, stamp))
    shutil.copy2(TARGET, backup)
    shutil.copy2(SOURCE, TARGET)
    print("installed %s (%d bytes) <- %s" % (TARGET.name, TARGET.stat().st_size, SOURCE))
    print("previous jar kept as %s" % backup.name)
    return 0


if __name__ == "__main__":
    sys.exit(main())
