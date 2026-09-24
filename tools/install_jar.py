"""Install a jar into the user's instance - but never while their game is running.

Why this exists: on 2026-09-23 23:37 a jar was copied into mods/ while the player's client was
running (started 23:31). The JVM had the mod jar open and had cached entry offsets from the old
file, so 25 seconds later it threw ClassNotFoundException for a class the new file does contain,
and the game crashed. Replacing a mod jar under a running game is not safe; this refuses instead.

The jar name follows `mod_version`, so a version bump renames it. Anything else the instance already
has under the same mod name is backed up and **removed**: two copies of one mod id in mods/ is a
loading error, not a harmless leftover.

Usage:
    python tools/install_jar.py                      # install the newest build/libs/scguns-*.jar
    python tools/install_jar.py --check              # only report whether it is safe
    python tools/install_jar.py --force              # install anyway (you accept the crash risk)
"""
import argparse
import datetime
import pathlib
import re
import shutil
import subprocess
import sys
import zipfile

MODS = pathlib.Path(r"D:\MCJAVA\.minecraft\versions\1.21.1-NeoForge_21.1.250\mods")
GAME_DIR_MARKER = r"1.21.1-NeoForge_21.1.250"
PROPERTIES = pathlib.Path("gradle.properties")


def built_jars():
    """Every scguns jar in build/libs, newest first (sources/javadoc excluded)."""
    return sorted((p for p in pathlib.Path("build/libs").glob("scguns-*.jar")
                   if not p.name.endswith(("-sources.jar", "-javadoc.jar"))),
                  key=lambda p: p.stat().st_mtime, reverse=True)


def installed_jars():
    """Every scguns jar in the instance's mods folder, backups excluded."""
    if not MODS.is_dir():
        return []
    return sorted(p for p in MODS.glob("scguns-*.jar") if ".bak-" not in p.name)


def declared_version(jar):
    """The `version="..."` of the first [[mods]] block inside the jar."""
    with zipfile.ZipFile(jar) as z:
        text = z.read("META-INF/neoforge.mods.toml").decode("utf-8", "replace")
    match = re.search(r'^\s*version\s*=\s*"([^"]+)"', text, re.M)
    return match.group(1) if match else None


def expected_version():
    match = re.search(r"^mod_version\s*=\s*(\S+)", PROPERTIES.read_text(encoding="utf-8"), re.M)
    return match.group(1) if match else None


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

    jars = built_jars()
    if not jars:
        print("MISSING: no build/libs/scguns-*.jar (run `gradlew build` first)")
        return 2
    source = jars[0]
    for older in jars[1:]:
        print("note: %s is older than %s" % (older.name, source.name))

    version = declared_version(source)
    expected = expected_version()
    if expected and version != expected:
        print("REFUSING: %s declares version %s but gradle.properties says mod_version=%s"
              % (source, version, expected))
        return 3
    print("installing %s (declared version %s)" % (source.name, version))

    stamp = datetime.datetime.now().strftime("%H%M%S")
    target = MODS / source.name
    for existing in installed_jars():
        backup = existing.with_name("%s.bak-%s" % (existing.name, stamp))
        shutil.copy2(existing, backup)
        existing.unlink()
        if existing.name == target.name:
            print("replaced %s (kept as %s)" % (existing.name, backup.name))
        else:
            print("removed the old copy %s (kept as %s) - two mod jars with one mod id would not load"
                  % (existing.name, backup.name))

    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source, target)
    print("installed %s (%d bytes)" % (target.name, target.stat().st_size))
    return 0


if __name__ == "__main__":
    sys.exit(main())
