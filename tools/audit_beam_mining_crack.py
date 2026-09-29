"""The beam's mining feedback: the crack packets must reach the miner, and digging must be audible.

This is the reason the mining guns showed no crack texture while vanilla mining showed one, even though
the server was computing it correctly (the SCGUNS-MINE probe prints stages 1, 3, 5, 6, 8, 9 going out
for a slow gun).

Vanilla, from the 1.21.1 merged jar, ServerLevel#destroyBlockProgress:

    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
        if (player != null && player.level() == this && player.getId() != breakerId
            && player.blockPosition().distSqr(pos) < 1024.0D) {
            player.connection.send(new ClientboundBlockDestructionPacket(breakerId, pos, progress));
        }
    }

The `player.getId() != breakerId` test means the player whose entity id equals the breaker id is
skipped - vanilla does not need to send a miner their own crack, because the client tracks its own
mining locally through MultiPlayerGameMode. This mod's beam mining is not the client's own mining, so
it depends entirely on that packet. Entity ids are only ever >= 0, so a breaker id counter that counts
down cannot collide with one; the old counter started at 1, which handed the first mining player the id
1 and silently muted their crack in any world where their entity id was 1 too.

The id also has to stay away from the player's real id even though it is "their" crack: the client keys
its own mining state by id, so using the player's id would let the client's own mining clear the beam's
crack the moment they mined anything by hand.

The second half of the same story is the sound. Vanilla plays the block's hit sound every four ticks
while digging, but it is the client that plays it for its own mining
(MultiPlayerGameMode.continueDestroyBlock: getSoundType(...).getHitSound(), volume (v + 1) / 8, pitch
p * 0.5, every 4 ticks), so a server-driven beam chews through blocks in silence unless the server sends
it. The break itself needs nothing here: it goes out as vanilla's level event 2001, which the client
turns into the break particles and the break sound.

usage: python tools/audit_beam_mining_crack.py
"""
from __future__ import annotations

import os
import re
import sys

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SOURCE = os.path.join(ROOT, "src", "main", "java", "top", "ribs", "scguns", "common",
                      "BeamHandlerCommon.java")


def strip_comments(src: str) -> str:
    out, i, n = [], 0, len(src)
    while i < n:
        c = src[i]
        if c == '"':
            out.append(c)
            i += 1
            while i < n and src[i] != '"':
                if src[i] == "\\":
                    out.append(src[i])
                    i += 1
                if i < n:
                    out.append(src[i])
                    i += 1
            if i < n:
                out.append('"')
                i += 1
        elif src.startswith("//", i):
            while i < n and src[i] != "\n":
                i += 1
        elif src.startswith("/*", i):
            i += 2
            while i < n and not src.startswith("*/", i):
                i += 1
            i += 2
        else:
            out.append(c)
            i += 1
    return "".join(out)


def main() -> int:
    text = strip_comments(open(SOURCE, encoding="utf-8", errors="replace").read())
    problems: list[str] = []

    start = re.search(r"int\s+nextBreakerId\s*=\s*(-?\d+)\s*;", text)
    if not start:
        problems.append("there is no breaker id counter to inspect any more")
    else:
        first = int(start.group(1))
        if first >= 0:
            problems.append("the breaker id counter starts at %d; ids are handed to players in order, so "
                            "the first mining player can be given an id that equals their own entity id, "
                            "and ServerLevel#destroyBlockProgress skips exactly that player - their crack "
                            "packets go nowhere" % first)
        if not re.search(r"computeIfAbsent\(\s*\w+\s*,\s*\w+\s*->\s*[\w.]*nextBreakerId\s*--", text):
            if re.search(r"nextBreakerId\s*\+\+", text):
                problems.append("the breaker id counter counts upwards from %d, so it can collide with an "
                                "entity id" % first)
            else:
                problems.append("the breaker id is no longer taken from the counting-down counter, so "
                                "nothing here guarantees it cannot equal an entity id")

    if not re.search(r"destroyBlockProgress\(\s*\w+\.breakerId\s*,", text):
        problems.append("the stage packets are no longer sent under the stored breaker id, so the id this "
                        "audit reasons about is not the one being used")
    if not re.search(r"progress\.lastStage\s*=\s*\w+;\s*\n\s*(?:if\s*\([^)]*\)\s*\{\s*)?[^\n]*"
                     r"destroyBlockProgress", text, re.S):
        problems.append("a stage change no longer sends a packet, so the crack can never deepen")

    # The digging sound, which vanilla plays client-side and a beam therefore has to play itself.
    if "getHitSound" not in text:
        problems.append("nothing plays the block's hit sound while a beam mines, so the beam chews "
                        "through blocks in silence - vanilla plays that sound from the client, which "
                        "never runs for a beam")
    else:
        if not re.search(r"getGameTime\(\)\s*%\s*4L\s*==\s*0L", text):
            problems.append("the hit sound is not on vanilla's four-tick interval")
        if not re.search(r"getVolume\(\)\s*\+\s*1\.0F\s*\)\s*/\s*8\.0F", text):
            problems.append("the hit sound does not use vanilla's volume, (volume + 1) / 8")
        if not re.search(r"getPitch\(\)\s*\*\s*0\.5F", text):
            problems.append("the hit sound does not use vanilla's pitch, pitch * 0.5")
        if "playNotifySound" not in text:
            problems.append("the hit sound is not sent to the miner alone, which is who hears it in "
                            "vanilla")
    if "levelEvent(2001" not in text and "levelEvent(2001," not in text:
        problems.append("the break no longer emits vanilla's level event 2001, which is what gives the "
                        "break its particles and its break sound on the client")

    for problem in problems:
        print("BROKEN %s" % problem)
    if problems:
        print("\n%d problem(s)" % len(problems))
        return 1
    print("0 problem(s): the beam's crack packets reach the miner, and digging it is audible")
    return 0


if __name__ == "__main__":
    sys.exit(main())
