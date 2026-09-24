"""Can this source tree really run on the NeoForge floor it advertises?

`gradle.properties` declares `neo_version_range`, which is what NeoForge checks when a player starts
the game - and that range ends up verbatim in the shipped `neoforge.mods.toml`. Lowering it is only
honest if two things hold:

  * our own code compiles against that NeoForge (no API newer than the floor is used), and
  * the floor is not below what our REQUIRED dependencies demand - NeoForge checks their ranges too,
    so a lower number would be a promise we cannot keep. GeckoLib 4.9.3, for instance, declares
    `neoforge [21.1.150,)`, which makes 21.1.150 the lowest version this mod can possibly load on.

This tool checks both, and with `--run` also boots a dedicated server on the floor (integration mods
excluded, because Create/Sable/Mekanism demand newer NeoForge than the floor themselves).

    python tools/check_neoforge_api_floor.py          # compile against the declared floor
    python tools/check_neoforge_api_floor.py --run    # ... and boot a server on it
"""

import os
import pathlib
import re
import subprocess
import sys
import time

PROPERTIES = pathlib.Path("gradle.properties")
LIBS = pathlib.Path("libs")
REQUIRED = ("framework-", "geckolib-", "curios-")
LOGS = pathlib.Path("build-logs")
FLOOR_COMPILE_LOG = LOGS / "floor-compile.txt"
FLOOR_SERVER_LOG = LOGS / "floor-server.txt"
# `java` on PATH is Java 8 on this machine, and Gradle then fails with
# "Dependency requires at least JVM runtime version 17. This build uses a Java 8 JVM." - set it here
# instead of relying on the shell that happens to run this script (HANDOFF section 1).
DEFAULT_JAVA_HOME = r"D:\jdk-21.0.3"


def java_home():
    """A JDK 21 for Gradle: --java-home, then $JAVA_HOME, then the documented default."""
    candidates = []
    for i, arg in enumerate(sys.argv):
        if arg == "--java-home" and i + 1 < len(sys.argv):
            candidates.append(sys.argv[i + 1])
    candidates += [os.environ.get("JAVA_HOME"), DEFAULT_JAVA_HOME]
    for candidate in candidates:
        if candidate and (pathlib.Path(candidate) / "bin" / "java.exe").is_file():
            version = subprocess.run([str(pathlib.Path(candidate) / "bin" / "java.exe"), "-version"],
                                     capture_output=True, text=True).stderr
            first = version.splitlines()[0] if version else ""
            if re.search(r'version "(1[7-9]|2[0-9])', first):
                return candidate, first.strip()
    return None, "none found"


def declared_floor():
    """The lower bound of neo_version_range, e.g. '21.1.150'."""
    text = PROPERTIES.read_text(encoding="utf-8")
    version_range = re.search(r"^neo_version_range\s*=\s*(\S+)", text, re.M).group(1)
    lower = re.match(r"\[?\s*([0-9][^,)\]]*)", version_range)
    if not lower:
        raise SystemExit("cannot read a lower bound out of neo_version_range=%s" % version_range)
    return version_range, lower.group(1)


def required_dependency_floors():
    """-> {modName: floor} for the jars we require at runtime, read from their own metadata."""
    floors = {}
    for jar in sorted(LIBS.glob("*.jar")):
        if not jar.name.startswith(REQUIRED):
            continue
        import zipfile

        with zipfile.ZipFile(jar) as z:
            for entry in ("META-INF/neoforge.mods.toml", "META-INF/mods.toml"):
                if entry not in z.namelist():
                    continue
                text = z.read(entry).decode("utf-8", "replace")
                for block in text.split("[["):
                    if "neoforge" not in block or "versionRange" not in block:
                        continue
                    if 'type="required"' not in block and 'type = "required"' not in block:
                        continue
                    name = re.search(r"\[\[dependencies\.([^\]]+)\]\]", "[[" + block)
                    where = re.search(r'versionRange\s*=\s*"([^"]+)"', block)
                    if where:
                        floors[(name.group(1) if name else "?").strip('"')] = where.group(1)
    return floors


def lower_bound(range_text):
    match = re.match(r"\[?\s*([0-9][^,)\]]*)", range_text)
    return match.group(1) if match else "0"


def gradle_env():
    """Environment for Gradle with a JDK 21 pinned - `java` on PATH is Java 8 here."""
    home, _ = java_home()
    env = os.environ.copy()
    if home:
        env["JAVA_HOME"] = home
    return env


def run_gradle(args, log_path):
    LOGS.mkdir(exist_ok=True)
    quoted = ["\"%s\"" % a if any(c in a for c in "[](),") else a for a in args]
    command = "gradlew.bat %s --console=plain > %s 2>&1" % (" ".join(quoted), log_path)
    print("   $ %s" % command)
    subprocess.run(["cmd", "/c", command], check=False, env=gradle_env())
    return log_path.read_text(encoding="utf-8", errors="replace")


def stop_dev_runs():
    """Stop the dev run this tool started (`runServer` never exits on its own).

    Matched on the command line like the project's other tooling does - never by process name, because
    the DSH backend itself is started through cmd.exe (HANDOFF section 38).
    """
    script = ("Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | "
              "Where-Object { $_.CommandLine -match 'devlaunch' } | "
              "ForEach-Object { Stop-Process -Id $_.ProcessId -Force }")
    subprocess.run(["powershell", "-NoProfile", "-Command", script], check=False)


def run_gradle_until_done(args, log_path, marker="Done (", timeout=420):
    """Run gradle, watch the log, and stop the dev run as soon as the server reports it started."""
    LOGS.mkdir(exist_ok=True)
    quoted = ["\"%s\"" % a if any(c in a for c in "[](),") else a for a in args]
    command = "gradlew.bat %s --console=plain > %s 2>&1" % (" ".join(quoted), log_path)
    print("   $ %s" % command)
    watchdog = ["powershell", "-NoProfile", "-Command",
                "Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | "
                "Where-Object { $_.CommandLine -match 'devlaunch' } | "
                "ForEach-Object { Stop-Process -Id $_.ProcessId -Force }"]
    with open(log_path, "w", encoding="utf-8", errors="replace"):
        pass  # truncate so we never read a previous run's "Done"
    process = subprocess.Popen(["cmd", "/c", command], env=gradle_env())
    try:
        deadline = time.time() + timeout
        while time.time() < deadline:
            if process.poll() is not None:
                break
            text = log_path.read_text(encoding="utf-8", errors="replace")
            if marker in text:
                print("   server reported startup; stopping the dev run")
                subprocess.run(watchdog, check=False)
                break
            time.sleep(3)
        else:
            print("   timed out after %ds - stopping the dev run" % timeout)
            subprocess.run(watchdog, check=False)
        try:
            process.wait(timeout=120)
        except subprocess.TimeoutExpired:
            subprocess.run(["taskkill", "/PID", str(process.pid), "/T", "/F"], check=False)
    finally:
        pass
    return log_path.read_text(encoding="utf-8", errors="replace")


def refresh_run_mods():
    """Copy the built maid compat into run/mods.

    A dev run loads the nested compat from `run/mods/<jar>` (it is not a source-set mod), and
    `gradlew build` does not refresh that copy: a stale one still demanded the old NeoForge range and
    blocked every floor run until it was replaced (HANDOFF section 70).
    """
    import shutil
    import zipfile

    built = pathlib.Path("maid-compat/build/libs/scg2_maid_compat-neoforge-1.21.1-1.0.8.jar")
    target = pathlib.Path("run/mods") / built.name
    if not built.is_file():
        return
    if target.is_file():
        with zipfile.ZipFile(target) as z:
            before = re.search(r'versionRange="([^"]+)"', z.read("META-INF/neoforge.mods.toml").decode())
        if before:
            print("   run/mods copy was %s" % before.group(1))
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(built, target)
    with zipfile.ZipFile(target) as z:
        after = re.search(r'versionRange="([^"]+)"', z.read("META-INF/neoforge.mods.toml").decode())
    print("   run/mods copy now %s" % (after.group(1) if after else "?"))


def main():
    version_range, floor = declared_floor()
    print("declared range : neo_version_range=%s  (floor %s)" % (version_range, floor))
    home, version = java_home()
    if not home:
        print("FAIL: no JDK 17+ found for Gradle - pass --java-home <dir> (java on PATH is Java 8 here)")
        return 1
    print("java for gradle: %s  (%s)" % (home, version))

    floors = required_dependency_floors()
    if floors:
        highest = max(floors.items(), key=lambda kv: tuple(int(p) for p in re.split(r"[.\-+]", lower_bound(kv[1])) if p.isdigit()))
        for mod, declared in sorted(floors.items()):
            print("required dep   : %-12s needs neoforge %s" % (mod, declared))
        if tuple(int(p) for p in re.split(r"[.\-+]", floor) if p.isdigit()) < \
           tuple(int(p) for p in re.split(r"[.\-+]", lower_bound(highest[1])) if p.isdigit()):
            print("FAIL: the declared floor %s is below what %s demands (%s) - NeoForge would refuse to"
                  " load it and this mod could never run there" % (floor, highest[0], highest[1]))
            return 1
        print("floor is consistent with the required dependencies")

    print("compiling the whole tree against NeoForge %s ..." % floor)
    # Only neo_version is overridden: neo_version_range already holds the floor we parsed it from, and
    # passing "[21.1.150,)" through `cmd /c` needs quoting that Gradle then reads as a task name.
    log = run_gradle(["compileJava", "-Pneo_version=%s" % floor], FLOOR_COMPILE_LOG)
    if "BUILD SUCCESSFUL" not in log:
        print("FAIL: the sources do not compile against NeoForge %s - the declared floor is too low"
              % floor)
        for line in log.splitlines():
            if "error:" in line:
                print("   %s" % line.strip())
        return 1
    print("OK: the sources compile against NeoForge %s" % floor)

    if "--run" in sys.argv:
        print("booting a dedicated server on NeoForge %s (integration mods excluded) ..." % floor)
        refresh_run_mods()
        log = run_gradle_until_done(["runServer", "-Pneo_version=%s" % floor,
                                     "-PnoIntegrationRuntime=true"], FLOOR_SERVER_LOG)
        done = re.search(r"Done \([\d.]+s\)!", log)
        mixins = len(re.findall(r"Mixing .* from scguns\.mixins\.json", log))

        # The dev world was saved with the integration mods, so loading its chunks without them logs
        # recoverable errors; that is the test setup, not the mod. Only the remainder counts.
        noise_markers = ("ChunkSerializer]", "AttachmentHolder]", "Parsing error loading recipe")
        errors = [line for line in log.splitlines() if re.search(r"/(ERROR|FATAL)\]", line)]
        noise = [line for line in errors if any(marker in line for marker in noise_markers)]
        relevant = [line for line in errors if line not in noise]
        print("   server start : %s" % (done.group(0) if done else "NO 'Done (...)'  -> FAIL"))
        print("   mixins applied: %d" % mixins)
        print("   ERROR/FATAL lines: %d (%d of them environment noise: missing integration mods /"
              " integration-mod recipes)" % (len(errors), len(noise)))
        for line in relevant[:10]:
            print("     %s" % line.strip()[:160])
        if not done:
            print("FAIL: see %s" % FLOOR_SERVER_LOG)
            return 1
        if relevant:
            print("FAIL: %d mod-related ERROR/FATAL line(s) on the floor - see %s"
                  % (len(relevant), FLOOR_SERVER_LOG))
            return 1
        print("OK: the mod loads and starts on NeoForge %s" % floor)

    return 0


if __name__ == "__main__":
    sys.exit(main())
