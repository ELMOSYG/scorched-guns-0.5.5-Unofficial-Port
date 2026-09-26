"""Tripwire for the two ways the shot path breaks on a server.

Both were found by driving `ServerPlayHandler.handleShoot` with a FakePlayer on a dedicated server
(HANDOFF section 72), and both are invisible in the client-side smoke tests the port used before:

1. **An arrow-firing gun could not fire at all.** 1.21's `AbstractArrow` validates the weapon it was
   fired from and throws `IllegalArgumentException("Invalid weapon firing an arrow")` when that stack is
   empty *on the server side*. The port passed `ItemStack.EMPTY` for `scguns:niami` (0.5.5 called the
   removed `Arrow(Level, LivingEntity)` constructor, which passed no weapon at all), so the exception
   aborted the whole shot - the player's report was simply "niami cannot shoot".

2. **Animation controllers are null on a dedicated server.** `AnimatedGunItem.registerControllers` only
   runs on the client, so `getAnimationControllers().get("controller")` returns null there. Three
   server-reachable call sites called into it unguarded:
   `GunFireEvent$Post` (its constructor runs before the event is even posted, so the NPE escaped
   `handleShoot` and killed every post-shot effect), `GunEventBus.postShoot` and `ReloadTracker`
   (a completed reload, in a tick handler).

Client-only call sites are exempt: the client always has its controllers.

Usage:
    python tools/audit_server_fire_paths.py
    python tools/audit_server_fire_paths.py --selftest   # must FAIL against the pre-fix sources
"""

import pathlib
import re
import subprocess
import sys

SRC = pathlib.Path("src/main/java")
CLIENT_DIR = "client"
# The revision these checks were written against - it still had both bugs. A fixed id, not HEAD:
# comparing against HEAD makes the selftest pass as soon as the fix is committed.
PRE_FIX_REVISION = "a70bcb3"
ARROW = re.compile(r"new Arrow\s*\(([^;]*)\)")
CONTROLLER = re.compile(r'\.get\(\s*"controller"\s*\)')
GUARD = re.compile(r"!=\s*null|null\s*!=")
# A method declaration line, so we can tell whether the enclosing method is client-only.
METHOD = re.compile(r"^[ \t]*(?:@\w+[^\n]*\n[ \t]*)*(?:public|private|protected|static)[^\n{;]*\([^\n;]*\)"
                    r"[^\n;{]*\{", re.M)


def top_level_args(call):
    out, depth, current = [], 0, ""
    for ch in call:
        if ch in "([{":
            depth += 1
        elif ch in ")]}":
            depth -= 1
        if ch == "," and depth == 0:
            out.append(current.strip())
            current = ""
        else:
            current += ch
    if current.strip():
        out.append(current.strip())
    return out


def client_only_method_starts(text):
    """Start offsets of methods annotated @OnlyIn(Dist.CLIENT) (the client always has controllers)."""
    starts = []
    for match in METHOD.finditer(text):
        head = text[max(0, match.start() - 220):match.end()]
        if "@OnlyIn(Dist.CLIENT)" in head:
            starts.append(match.start())
    return starts


def check(text, rel):
    """-> list of problems for one file."""
    problems = []

    for match in ARROW.finditer(text):
        args = top_level_args(match.group(1))
        if len(args) < 4:
            problems.append("%s: new Arrow(...) with %d argument(s) - 1.21 has no such constructor "
                            "(the weapon stack is required)" % (rel, len(args)))
        elif args[3] in ("ItemStack.EMPTY", "ItemStack.EMPTY.copy()"):
            problems.append("%s: new Arrow(...) is given an EMPTY weapon - on the server 1.21 throws "
                            "\"Invalid weapon firing an arrow\" and the shot is lost" % rel)

    posix = rel.replace("\\", "/")
    if posix.startswith(CLIENT_DIR) or "/client/" in posix:
        return problems  # the client always has its controllers

    client_methods = client_only_method_starts(text)
    for match in CONTROLLER.finditer(text):
        if any(start < match.start() and match.start() - start < 4000 for start in client_methods):
            continue
        # Which variable holds it? (`Type name = ...get("controller");`)
        statement_start = max(text.rfind(";", 0, match.start()), text.rfind("{", 0, match.start())) + 1
        assignment = text[statement_start:match.end()]
        name = re.search(r"(\w+)\s*=\s*[^;]*$", assignment)
        if not name:
            continue
        var = name.group(1)
        # The guard may sit anywhere between the lookup and the first use, comments included - so strip the
        # comments and give it a generous window, but require the guard to name *this* variable.
        look = re.sub(r"//[^\n]*", "", text[match.end():match.end() + 2000])
        guard = re.compile(r"\b%s\s*!=\s*null|null\s*!=\s*%s\b|\b%s\s*==\s*null\s*\)\s*(?:\{|return)"
                           % (var, var, var))
        if not guard.search(look):
            line = text.count("\n", 0, match.start()) + 1
            problems.append("%s:%d: uses the animation controller '%s' without a null check - it is null "
                            "on a dedicated server" % (rel, line, var))
    return problems


def scan(root=SRC):
    problems = []
    for path in sorted(root.rglob("*.java")):
        rel = path.relative_to(root).as_posix()
        problems += check(path.read_text(encoding="utf-8"), rel)
    return problems


def selftest():
    """The three unguarded sites and the empty arrow weapon must all be found in the pre-fix tree."""
    files = {
        "top/ribs/scguns/common/network/ServerPlayHandler.java": "EMPTY weapon",
        "top/ribs/scguns/event/GunFireEvent.java": "without a null check",
        "top/ribs/scguns/event/GunEventBus.java": "without a null check",
        "top/ribs/scguns/common/ReloadTracker.java": "without a null check",
    }
    missing = []
    for name, needle in files.items():
        try:
            before = subprocess.run(["git", "show", "%s:src/main/java/%s" % (PRE_FIX_REVISION, name)],
                                    capture_output=True, text=True, check=True).stdout
        except (subprocess.CalledProcessError, FileNotFoundError) as error:
            print("selftest: cannot read %s:%s (%s)" % (PRE_FIX_REVISION, name, error))
            return 1
        found = check(before, name)
        if not any(needle in problem for problem in found):
            missing.append("%s (%s)" % (name, needle))
    if missing:
        for entry in missing:
            print("selftest MISSING: %s" % entry)
        print("selftest FAILED: the audit does not catch every site it was written for")
        return 1
    print("selftest OK: all 4 pre-fix problems are detected (1 empty arrow weapon, 3 unguarded "
          "controllers)")
    return 0


def main():
    if "--selftest" in sys.argv:
        return selftest()
    problems = scan()
    for problem in problems:
        print("  %s" % problem)
    if problems:
        print("%d problem(s) on the server-side shot path (shots will fail or the log will fill with"
              " exceptions)" % len(problems))
        return 1
    print("0 problem(s): arrow guns name a weapon and every non-client controller use is guarded")
    return 0


if __name__ == "__main__":
    sys.exit(main())
