"""Install a jar into the user's instance - but never while their game is running.

Why this exists: on 2026-09-23 23:37 a jar was copied into mods/ while the player's client was
running (started 23:31). The JVM had the mod jar open and had cached entry offsets from the old
file, so 25 seconds later it threw ClassNotFoundException for a class the new file does contain,
and the game crashed. Replacing a mod jar under a running game is not safe; this refuses instead.

The jar name follows `mod_version`, so a version bump renames it. Anything else the instance already
has under the same mod name is backed up and **removed**: two copies of one mod id in mods/ is a
loading error, not a harmless leftover.

Backups go to `mods/备份文件/`, not next to the live jar. Two reasons, one of which is about the
game and one about the launcher:

  * NeoForge would not have loaded them anyway. `ModDirTransformerDiscoverer#scan` calls
    `Files.walk(modsDir, 1)` - depth 1, so a subfolder's contents are never visited - and its
    filter is `endsWith(".jar")`, which `scguns-0.5.5.1.jar.bak-165917` does not satisfy. So the
    34 backups that had accumulated beside the live jar were harmless to the game.
  * They are not harmless to the player. Every launcher lists the whole mods folder, so 34 near
    identical 19 MB jars buried the one that was actually in use. That is what the player reported,
    and the subfolder is the fix.

Usage:
    python tools/install_jar.py                      # install the newest build/libs/scguns-*.jar
    python tools/install_jar.py --check              # only report whether it is safe
    python tools/install_jar.py --force              # install anyway (you accept the crash risk)
    python tools/install_jar.py --tidy               # move loose backups into the subfolder only
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
BACKUPS = MODS / "备份文件"
GAME_DIR_MARKER = r"1.21.1-NeoForge_21.1.250"
PROPERTIES = pathlib.Path("gradle.properties")


def built_jars():
    """Every scguns jar in build/libs, newest first (sources/javadoc excluded)."""
    return sorted((p for p in pathlib.Path("build/libs").glob("scguns-*.jar")
                   if not p.name.endswith(("-sources.jar", "-javadoc.jar"))),
                  key=lambda p: p.stat().st_mtime, reverse=True)


def installed_jars():
    """Every live scguns jar in the instance's mods folder, backups excluded."""
    if not MODS.is_dir():
        return []
    return sorted(p for p in MODS.iterdir()
                  if p.is_file() and p.name.startswith("scguns-") and p.name.endswith(".jar"))


def loose_backups():
    """Backup files still sitting beside the live jar, from before BACKUPS existed."""
    if not MODS.is_dir():
        return []
    return sorted(p for p in MODS.iterdir()
                  if p.is_file() and ".bak-" in p.name)


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


def tidy_backups():
    """Move backup files out of mods/ root and into mods/备份文件/.

    Self-healing rather than a one-off migration: backups written before the subfolder existed
    are still in the root, and the next install must not leave them there either.
    """
    moved = 0
    for backup in loose_backups():
        BACKUPS.mkdir(parents=True, exist_ok=True)
        destination = BACKUPS / backup.name
        if destination.exists():
            # Same name, already in the subfolder: keep the older copy out of the way rather than
            # overwriting it, so nothing is ever lost.
            destination = BACKUPS / ("%s.dup-%s" % (backup.stem, datetime.datetime.now().strftime("%H%M%S")))
        shutil.move(str(backup), str(destination))
        print("moved %s -> %s" % (backup.name, destination.relative_to(MODS)))
        moved += 1
    if not moved:
        print("no loose backups in mods/ (%s holds %d)"
              % (BACKUPS.name, len(list(BACKUPS.glob("*"))) if BACKUPS.is_dir() else 0))
    return moved


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--force", action="store_true")
    parser.add_argument("--tidy", action="store_true",
                        help="only move loose backups into the subfolder, install nothing")
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

    if args.tidy:
        tidy_backups()
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

    # Tidy first, so a backup written by an earlier version of this tool never sits beside the
    # live jar even for the length of this install.
    tidy_backups()

    stamp = datetime.datetime.now().strftime("%H%M%S")
    target = MODS / source.name
    for existing in installed_jars():
        BACKUPS.mkdir(parents=True, exist_ok=True)
        backup = BACKUPS / ("%s.bak-%s" % (existing.name, stamp))
        shutil.copy2(existing, backup)
        existing.unlink()
        if existing.name == target.name:
            print("replaced %s (kept as %s/%s)" % (existing.name, BACKUPS.name, backup.name))
        else:
            print("removed the old copy %s (kept as %s/%s) - two mod jars with one mod id would not load"
                  % (existing.name, BACKUPS.name, backup.name))

    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source, target)
    print("installed %s (%d bytes)" % (target.name, target.stat().st_size))
    return 0


if __name__ == "__main__":
    sys.exit(main())
