"""The server must not take the client's word for it when loading rounds or handing out casings.

Both packets are animation keyframes: the client plays a reload animation and, on the keyframes it
contains, asks the server to seat a round ("loaded") or to drop a spent case ("eject_casing"). Until
this round the server performed both unconditionally, which the player found: unloading the magazine
in the middle of a reload ends the reload, but the client's loop keeps running and keeps sending both
keyframes, so the gun refilled itself from the returned shells and every loop produced a case - on a
full magazine, for ever. Reproduced on NeoForge 21.1.264 (10 loops after an unload: ammo 0 -> 1 -> 2,
3 casings, out of a magazine that was never fired).

The rules:

  1. a "loaded" keyframe may only seat a round while a reload is actually running;
  2. an "eject_casing" keyframe needs a running reload AND a fired round whose case is still owed
     (`SpentRounds`), so casings can never outnumber the rounds actually fired;
  3. firing has to record the owed case, or rule 2 never pays out;
  4. the client has to release a reload animation that outlived its reload - the unload case above
     leaves the loop playing, because the server's answer clears the state instead of moving it to
     STOPPING, so nothing ever triggers the stop animation.

usage:
    python tools/audit_reload_trust.py
    python tools/audit_reload_trust.py --selftest   # must FAIL against the pre-fix tag
"""
from __future__ import annotations

import os
import re
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "src", "main", "java", "top", "ribs", "scguns")
GUN_LOADED = "network/message/C2SMessageGunLoaded.java"
EJECT = "network/message/C2SMessageEjectCasing.java"
SERVER_PLAY = "common/network/ServerPlayHandler.java"
ANIMATED = "item/animated/AnimatedGunItem.java"
CLIENT_PLAY = "client/network/ClientPlayHandler.java"

# The tag before this round: both packets trusted the client and nothing counted spent rounds.
PRE_FIX_REVISION = "backup-reloadstate-20261115"


def strip_comments(text: str) -> str:
    text = re.sub(r"/\*.*?\*/", lambda m: re.sub(r"[^\n]", " ", m.group(0)), text, flags=re.S)
    return re.sub(r"//[^\n]*", "", text)


def method_body(text: str, signature: str) -> str | None:
    start = text.find(signature)
    if start < 0:
        return None
    brace = text.find("{", start)
    if brace < 0:
        return None
    depth = 0
    for index in range(brace, len(text)):
        if text[index] == "{":
            depth += 1
        elif text[index] == "}":
            depth -= 1
            if depth == 0:
                return text[brace + 1:index]
    return None


def early_return_on_reloading(text: str) -> int:
    """Position of an `if (!...RELOADING...) { ... return; }` guard, or -1.

    The obvious text check ("is RELOADING mentioned before the round is seated?") is not enough: the
    pre-fix code did read the flag, in `if (current >= maxAmmo && !RELOADING) return;` - which still
    seated a round whenever the magazine was not full, and that is exactly the unload case. The rule
    has to be "a bare negation of the flag, followed by a return".
    """
    for match in re.finditer(r"if\s*\(", text):
        open_paren = match.end() - 1
        depth = 0
        for index in range(open_paren, len(text)):
            if text[index] == "(":
                depth += 1
            elif text[index] == ")":
                depth -= 1
                if depth == 0:
                    condition = text[open_paren + 1:index]
                    body_start = text.find("{", index)
                    if body_start < 0:
                        break
                    body = text[body_start:body_start + 240]
                    # The whole condition has to be the negation of the reload flag. Anything else -
                    # `current >= maxAmmo && !RELOADING` above all - lets the packet through when it
                    # matters, so it does not count as a guard.
                    compact = re.sub(r"\s+", "", condition)
                    bare = (
                        re.fullmatch(
                            r"!Boolean\.TRUE\.equals\((?:top\.ribs\.scguns\.init\.)?ModSyncedDataKeys"
                            r"\.RELOADING\.getValue\(player\)\)", compact)
                        or re.fullmatch(
                            r"!\(Boolean\)(?:top\.ribs\.scguns\.init\.)?ModSyncedDataKeys"
                            r"\.RELOADING\.getValue\(player\)", compact)
                    )
                    if bare and re.search(r"return\b", body):
                        return match.start()
                    break
    return -1


def problems_for(sources: dict[str, str]) -> list[str]:
    problems: list[str] = []

    loaded = strip_comments(sources.get(GUN_LOADED, ""))
    handle = method_body(loaded, "public void handle(C2SMessageGunLoaded")
    if handle is None:
        problems.append("C2SMessageGunLoaded.handle is gone; has the packet moved?")
    else:
        guard = early_return_on_reloading(handle)
        # Any of these seats a round; the guard has to come before the first one.
        seating = [position for position in
                   (handle.find("increaseMagAmmo("), handle.find("reloadItem("), handle.find("increaseAmmo("))
                   if position >= 0]
        if not seating:
            problems.append("C2SMessageGunLoaded no longer seats a round anywhere - has the flow changed?")
        elif guard < 0 or guard > min(seating):
            problems.append("C2SMessageGunLoaded seats a round without first returning when no reload is "
                            "running: a stuck client animation refills the magazine for free")

    eject = strip_comments(sources.get(EJECT, ""))
    eject_handle = method_body(eject, "public void handle(C2SMessageEjectCasing")
    if eject_handle is None:
        problems.append("C2SMessageEjectCasing.handle is gone")
    else:
        if "ModSyncedDataKeys.RELOADING" not in eject_handle:
            problems.append("C2SMessageEjectCasing hands out a casing without a reload running")
        if "SpentRounds.take" not in eject_handle:
            problems.append("C2SMessageEjectCasing hands out a casing without claiming a fired round")

    server = strip_comments(sources.get(SERVER_PLAY, ""))
    consume = method_body(server, "private static void consumeAmmo(ServerPlayer")
    if consume is None:
        problems.append("ServerPlayHandler.consumeAmmo is gone")
    elif "SpentRounds.add" not in consume:
        problems.append("firing does not record the spent case, so the reload-eject rule never pays out")

    animated = strip_comments(sources.get(ANIMATED, ""))
    main_logic = method_body(animated, "private void handleMainAnimationLogic(")
    if main_logic is None:
        problems.append("AnimatedGunItem.handleMainAnimationLogic is gone")
    elif "cleanupAllReloadTags" not in main_logic:
        problems.append("a reload animation that outlived its reload is never released: the loop keeps "
                        "playing (and keeps firing its keyframes) until the player switches items")

    # The firing path interrupts a reload smoothly, and the whole difference is that it does NOT stop
    # the controller first: "reload_stop" is registered as a chain that plays the clip and then loops
    # idle, so triggering it is already the transition. The stop packet used to stop() first, which
    # threw away the pose it blends from - the player described that as "no smooth transition".
    client_play = strip_comments(sources.get(CLIENT_PLAY, ""))
    stop_reload = method_body(client_play, "public static void handleStopReload(")
    if stop_reload is None:
        problems.append("ClientPlayHandler.handleStopReload is gone")
    else:
        if "tryTriggerAnimation" not in stop_reload:
            problems.append("the stop packet does not trigger the stop clip")
        if ".stop()" in stop_reload:
            problems.append("the stop packet stops the controller before triggering reload_stop: that "
                            "throws away the pose the clip blends from and is what looked abrupt")

    return problems


def scan(sources: dict[str, str]) -> list[str]:
    return problems_for(sources)


def read_tree() -> dict[str, str]:
    files = [GUN_LOADED, EJECT, SERVER_PLAY, ANIMATED, CLIENT_PLAY]
    return {
        relative: open(os.path.join(SRC, relative), encoding="utf-8", errors="replace").read()
        for relative in files
    }


def selftest() -> int:
    files = [GUN_LOADED, EJECT, SERVER_PLAY, ANIMATED, CLIENT_PLAY]
    sources = {}
    for relative in files:
        path = "src/main/java/top/ribs/scguns/" + relative
        try:
            sources[relative] = subprocess.run(
                ["git", "show", "%s:%s" % (PRE_FIX_REVISION, path)],
                capture_output=True, text=True, encoding="utf-8", check=True,
            ).stdout
        except (subprocess.CalledProcessError, FileNotFoundError) as error:
            print("selftest: cannot read %s at %s (%s)" % (path, PRE_FIX_REVISION, error))
            return 1

    found = scan(sources)
    if len(found) < 3:
        print("selftest FAILED: only %d problem(s) found in the pre-fix sources, expected at least 3"
              % len(found))
        for problem in found:
            print("  " + problem)
        return 1

    for problem in found:
        print("  " + problem)
    print("selftest OK: %d problem(s) detected at %s" % (len(found), PRE_FIX_REVISION))
    return 0


def main() -> int:
    if "--selftest" in sys.argv:
        return selftest()

    problems = scan(read_tree())
    for problem in problems:
        print("BROKEN %s" % problem)
    if problems:
        print("\n%d problem(s)" % len(problems))
        return 1
    print("0 problem(s): rounds are only seated during a reload, casings only for rounds actually "
          "fired, and a stale reload animation is released")
    return 0


if __name__ == "__main__":
    sys.exit(main())
