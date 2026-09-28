"""Client-side gameplay state must be synced, not read out of a server-side static (HANDOFF 82.22).

A static field in a class that exists on both sides is per-JVM, not per-side-of-the-wire. In single player
the client and the integrated server share one JVM, so a client read of a server-written static silently
"works"; on a dedicated server it never does, and the symptom is a feature that simply does not animate or
react for the player - no error, nothing in the log.

That is what happened to the bayonet charge: `MeleeAttackHandler.startBanzai` (reached from the melee
packet, so server side) set `isBanzai`, and the client's `GunRenderingHandler` and `AnimatedGunItem` read
`isBanzaiActive()` straight off that field, so `banzaiProgress` stayed at zero on a server and the charge
animation never played. The mod already has the right mechanism for this - Framework synced data keys, as
`ModSyncedDataKeys.MELEE` shows - so the charge is carried by `ModSyncedDataKeys.BANZAI` now.

Rules:

  1. The client-visible half of the charge must be read through the synced key helper.
  2. The server-side static may only be read where the server is the caller.
  3. The key must exist and must actually be registered, or the value is never synced and the helper
     silently returns the default.

usage: python tools/audit_synced_state.py
"""
from __future__ import annotations

import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "src", "main", "java", "top", "ribs", "scguns")

# Reading the server's own static is correct in these files, and only these: the packet handler runs on the
# server and decides whether a charge starts or stops, and GunEventBus' use stops the charge when the item
# changes (server side). Everything that draws or animates must use the synced helper instead.
SERVER_SIDE_CALLERS = {
    "network/message/C2SMessageMeleeAttack.java",
    "event/GunEventBus.java",
    "client/handler/MeleeAttackHandler.java",
}

CLIENT_READERS = {
    "client/handler/GunRenderingHandler.java",
    "item/animated/AnimatedGunItem.java",
}


def strip_comments(text: str) -> str:
    """Blank out comments, aware of string literals.

    A `/*` inside a string is not a comment: this codebase has config comments naming file globs such as
    `data/scguns/entity/equipment/*.json`, and a stripper that ignores string literals treats that `/*` as a
    block comment start and deletes the code after it. That reads as "the thing does not exist" - a false
    negative, which is the one failure an audit must not have.
    """
    out = []
    i = 0
    n = len(text)
    while i < n:
        c = text[i]
        if c == '"':
            out.append(c)
            i += 1
            while i < n:
                if text[i] == "\\":
                    out.append(text[i:i + 2])
                    i += 2
                    continue
                out.append(text[i])
                if text[i] == '"':
                    i += 1
                    break
                i += 1
        elif c == "/" and i + 1 < n and text[i + 1] == "/":
            while i < n and text[i] != "\n":
                i += 1
        elif c == "/" and i + 1 < n and text[i + 1] == "*":
            i += 2
            while i + 1 < n and not (text[i] == "*" and text[i + 1] == "/"):
                if text[i] == "\n":
                    out.append("\n")
                i += 1
            i += 2
        else:
            out.append(c)
            i += 1
    return "".join(out)

def main() -> int:
    problems = []

    read_sites = {}
    for dirpath, _, names in os.walk(SRC):
        for name in names:
            if not name.endswith(".java"):
                continue
            path = os.path.join(dirpath, name)
            rel = os.path.relpath(path, SRC).replace("\\", "/")
            text = strip_comments(open(path, encoding="utf-8", errors="replace").read())
            if re.search(r"MeleeAttackHandler\.isBanzaiActive\(\)", text):
                read_sites[rel] = text

    for rel in sorted(read_sites):
        if rel not in SERVER_SIDE_CALLERS:
            problems.append("%s reads MeleeAttackHandler.isBanzaiActive(), which only the server ever "
                            "writes; use MeleeAttackHandler.isBanzaiCharging(player)" % rel)

    for rel in sorted(CLIENT_READERS):
        path = os.path.join(SRC, rel)
        if not os.path.isfile(path):
            problems.append("%s is missing" % rel)
            continue
        text = open(path, encoding="utf-8", errors="replace").read()
        if "isBanzaiCharging(" not in text:
            problems.append("%s no longer reads the synced charge state, so the charge animation only works "
                            "in single player" % rel)

    keys = os.path.join(SRC, "init", "ModSyncedDataKeys.java")
    key_text = open(keys, encoding="utf-8", errors="replace").read()
    if "BANZAI" not in key_text or '"banzai"' not in key_text:
        problems.append("ModSyncedDataKeys has no BANZAI key, so the charge state cannot be synced")

    registration = open(os.path.join(SRC, "ScorchedGuns.java"), encoding="utf-8", errors="replace").read()
    if "registerSyncedDataKey(ModSyncedDataKeys.BANZAI)" not in registration:
        problems.append("ScorchedGuns does not register ModSyncedDataKeys.BANZAI, so its value is never sent "
                        "to the client and the helper always returns the default")

    handler = os.path.join(SRC, "client/handler/MeleeAttackHandler.java")
    if "isBanzaiCharging" not in open(handler, encoding="utf-8", errors="replace").read():
        problems.append("MeleeAttackHandler has no isBanzaiCharging helper, so client code has no synced "
                        "way to read the charge state")

    print("=== files reading the server-side charge static ===")
    for rel in sorted(read_sites):
        print("  %-58s %s" % (rel, "server side (ok)" if rel in SERVER_SIDE_CALLERS else "CLIENT - wrong"))
    print("")
    if problems:
        for problem in problems:
            print("BROKEN %s" % problem)
        print("\n%d problem(s)" % len(problems))
        return 1
    print("0 problem(s): the charge state reaches the client through the synced key")
    return 0


if __name__ == "__main__":
    sys.exit(main())
