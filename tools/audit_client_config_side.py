"""Client-only config options must not be read raw from code that a dedicated server runs.

This is the bug behind the player's report that gunner mobs never reload (HANDOFF section 81), and it was
reproduced on a dedicated server: a mob fired, `AIGunEvent.performGunAttack` read
`Config.CLIENT.display.fireLights`, NeoForge threw

    IllegalStateException: Cannot get config value before config is loaded.

and because that read sits **before** `GunAttackGoal.consumeAmmo`, the magazine never went down - so the AI
never reached its reload branch - and the server died with "Ticking entity". Forge 1.20.1 returned the
default for an unloaded spec, so 0.5.5 could read a client option from server code harmlessly; NeoForge
21.1 throws, and that difference is what turned a latent "wrong config" into a crash.

Three sites had already been patched **individually** (`TemporaryLightManager` and `ProjectileEntity`
caught the IllegalStateException, `ProjectileEntity.onWaterImpact` defaulted to true) which is exactly why
the other six were missed. The rule here is therefore the general one:

  * a file under `client/` may read `Config.CLIENT` directly - it only ever runs on a client;
  * anywhere else, the read must go through `Config.clientOr(...)`, which returns the option's default
    when the client spec is not loaded (what Forge used to do);
  * and no file outside `client/` may catch `IllegalStateException` around such a read: that ad-hoc
    workaround is what hid the remaining sites, and catching it also hides every other state error in the
    same block.

Usage:
    python tools/audit_client_config_side.py
    python tools/audit_client_config_side.py --selftest   # must FAIL against the pre-fix revision
"""

import pathlib
import re
import subprocess
import sys

ROOT = pathlib.Path("src/main/java")
PACKAGE = ROOT / "top/ribs/scguns"
RELATIVE_PACKAGE = "src/main/java/top/ribs/scguns"
CONFIG = PACKAGE / "Config.java"

# The revision this was written against: HANDOFF section 80, where six server-reachable sites still read
# the client config raw. A fixed id, not HEAD.
PRE_FIX_REVISION = "d950b34"


def is_client_only(relative):
    """Files that only ever run on a client: the client package, and client mixins."""
    return "/client/" in relative or relative.startswith("client/")


def strip_comments(text):
    """Blank out comments while keeping their newlines, so line numbers stay usable.

    The comments in this codebase quote the broken pattern on purpose - the first version of this audit
    flagged a comment that explained the removed `catch (IllegalStateException)` as if it were the catch
    itself.
    """
    text = re.sub(r"/\*.*?\*/", lambda m: re.sub(r"[^\n]", " ", m.group(0)), text, flags=re.S)
    return re.sub(r"//[^\n]*", "", text)


def scan(files):
    """`files` maps a package-relative path to its text. Returns the problems found."""
    problems = []
    for relative, raw in sorted(files.items()):
        if is_client_only(relative) or relative == "Config.java":
            continue
        text = strip_comments(raw)
        for number, line in enumerate(text.splitlines(), 1):
            if "Config.CLIENT" not in line:
                continue
            if "clientOr(" in line:
                continue
            problems.append("%s:%d reads a client-only option on the server: %s"
                            % (relative, number, line.strip()))
        if "Config.CLIENT" in text and "catch (IllegalStateException" in text:
            problems.append("%s catches IllegalStateException around a client-config read; that ad-hoc "
                            "workaround is what left the other sites broken - use Config.clientOr"
                            % relative)
    config = strip_comments(files.get("Config.java") or "")
    if "public static <T> T clientOr(ConfigValue<T> value)" not in config:
        problems.append("Config.clientOr is missing, so no server-side read of a client option is safe")
    return problems


def current_files():
    files = {}
    for path in PACKAGE.rglob("*.java"):
        files[path.relative_to(PACKAGE).as_posix()] = path.read_text(encoding="utf-8", errors="replace")
    return files


def git_files(revision):
    listing = subprocess.run(["git", "ls-tree", "-r", "--name-only", revision, RELATIVE_PACKAGE],
                             capture_output=True, text=True, encoding="utf-8", check=True).stdout.split()
    files = {}
    for full in listing:
        if not full.endswith(".java"):
            continue
        text = subprocess.run(["git", "show", "%s:%s" % (revision, full)],
                              capture_output=True, text=True, encoding="utf-8", check=True).stdout
        files[full[len(RELATIVE_PACKAGE) + 1:]] = text
    return files


def selftest():
    try:
        files = git_files(PRE_FIX_REVISION)
    except (subprocess.CalledProcessError, FileNotFoundError) as error:
        print("selftest: cannot read revision %s (%s)" % (PRE_FIX_REVISION, error))
        return 1

    found = scan(files)
    expected = [
        "AIGunEvent.java",               # the crash the player's report led to
        "ProjectileEntity.java",         # the lava impact read, left broken next to the patched water one
        "SulfurGasCloud.java",           # three server-side particle paths
        "GunProgressionEventHandler.java",
        "LightningProjectileEntity.java",
        "TemporaryLightManager.java",    # the CATCH, kept by the earlier ad-hoc patch
        "Config.clientOr is missing",
    ]
    print("selftest: revision %s reports %d problem(s)" % (PRE_FIX_REVISION, len(found)))
    for problem in found:
        print("   %s" % problem)
    missing = [e for e in expected if not any(e in f for f in found)]
    if missing:
        for entry in missing:
            print("selftest MISSING: %s" % entry)
        print("selftest FAILED: the audit does not catch server-side client-config reads")
        return 1
    print("selftest OK: every unguarded server-side client-config read is detected")
    return 0


def main():
    if "--selftest" in sys.argv:
        return selftest()
    if not PACKAGE.exists():
        print("FAIL: %s is missing" % PACKAGE)
        return 1
    problems = scan(current_files())
    for problem in problems:
        print("  %s" % problem)
    if problems:
        print("%d problem(s): a dedicated server can throw on a client-only option" % len(problems))
        return 1
    print("0 problem(s): client options are read through Config.clientOr on the server")
    return 0


if __name__ == "__main__":
    sys.exit(main())
