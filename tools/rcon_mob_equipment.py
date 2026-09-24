"""Prove in a running game that the mod's mobs spawn holding a gun.

The dedicated server cannot show client-side rendering, but it *can* show
whether `finalizeSpawn` actually ran, because that is where
`EntityEquipmentConfig.equipEntity` is called.  Before the arity fix the method
had the wrong parameter count, never overrode `Mob.finalizeSpawn`, and so never
executed -- every gunner spawned empty-handed.

This drives a real server over RCON: summon the mob, then read its `HandItems`
back out of the entity data.

Usage:
    1. run/server.properties must have enable-rcon=true and an rcon.password
    2. cmd /c "gradlew.bat runServer ..."   (in another shell)
    3. python tools/rcon_mob_equipment.py [password]
"""

import os
import re
import socket
import struct
import sys
import time

HOST = "127.0.0.1"
PORT = 25575
PASSWORD = sys.argv[1] if len(sys.argv) > 1 else os.environ.get("SCGUNS_RCON_PASSWORD", "scgunsverify")

LOGIN, COMMAND, RESPONSE = 3, 2, 0


class Rcon:
    def __init__(self, host, port, password, timeout=10.0):
        self.sock = socket.create_connection((host, port), timeout=timeout)
        self.request_id = 0
        self._send(LOGIN, password)
        response_id, _kind, _body = self._recv()
        if response_id == -1:
            raise SystemExit("RCON authentication failed (wrong password?)")

    def _send(self, kind, body):
        self.request_id += 1
        payload = struct.pack("<ii", self.request_id, kind) + body.encode("utf-8") + b"\x00\x00"
        self.sock.sendall(struct.pack("<i", len(payload)) + payload)
        return self.request_id

    def _read_exact(self, count):
        chunks = []
        remaining = count
        while remaining:
            chunk = self.sock.recv(remaining)
            if not chunk:
                raise EOFError("connection closed")
            chunks.append(chunk)
            remaining -= len(chunk)
        return b"".join(chunks)

    def _recv(self):
        (length,) = struct.unpack("<i", self._read_exact(4))
        body = self._read_exact(length)
        request_id, kind = struct.unpack("<ii", body[:8])
        return request_id, kind, body[8:-2].decode("utf-8", "replace")

    def command(self, text):
        sent = self._send(COMMAND, text)
        deadline = time.time() + 15.0
        collected = []
        while time.time() < deadline:
            try:
                request_id, _kind, body = self._recv()
            except (socket.timeout, EOFError):
                break
            if request_id == sent:
                collected.append(body)
                # Keep draining briefly: long replies arrive split over packets.
                self.sock.settimeout(0.35)
                continue
            break
        self.sock.settimeout(10.0)
        return "\n".join(collected)

    def close(self):
        self.sock.close()


# Mobs that have an equipment config (id in data/scguns/entity/equipment/*.json).
TARGETS = [
    ("scguns:adjudicator", "scguns:adjudicator"),
    ("scguns:blunderer", "scguns:blunderer"),
    ("scguns:cog_knight", "scguns:cog_knight"),
    ("scguns:finforcer", "scguns:finforcer"),
    ("scguns:subjugator", "scguns:subjugator"),
    ("scguns:cog_minion", "scguns:cog_minion"),
]


ID = re.compile(r'id:\s*"([^"]+)"')
AMMO = re.compile(r"AmmoCount:\s*(\d+)")


def main():
    print("connecting to rcon %s:%d ..." % (HOST, PORT))
    rcon = Rcon(HOST, PORT, PASSWORD)
    print("authenticated")
    print("")

    rcon.command("gamerule doMobSpawning false")
    rcon.command("kill @e[type=!player]")

    results = []
    for entity, _config_id in TARGETS:
        rcon.command("kill @e[type=%s]" % entity)
        # Summon in the air near spawn so it cannot suffocate, then read it back.
        rcon.command("execute positioned 0 100 0 run summon %s ~ ~ ~" % entity)
        time.sleep(0.35)
        data = rcon.command(
            "data get entity @e[type=%s,limit=1,sort=nearest] HandItems" % entity
        )
        # HandItems is [mainhand, offhand]; the first id is what we care about.
        ids = ID.findall(data)
        main_hand = ids[0] if ids else "(empty)"
        ammo = AMMO.search(data)
        results.append((entity, main_hand, ammo.group(1) if ammo else "-"))

    print("=" * 74)
    print("%-22s %-34s %s" % ("entity", "main hand", "AmmoCount"))
    print("-" * 74)
    for entity, main_hand, ammo in results:
        print("%-22s %-34s %s" % (entity, main_hand, ammo))

    print("")
    rcon.command("kill @e[type=!player]")
    rcon.close()

    equipped = sum(1 for _e, h, _a in results if h != "(empty)")
    gunners = sum(1 for _e, h, a in results if a != "-")
    print("%d/%d mobs spawned with a main-hand item" % (equipped, len(results)))
    print("%d/%d mobs spawned with a loaded gun (AmmoCount present)" % (gunners, len(results)))
    # The regression this guards: equipment must be applied at all.
    return 0 if equipped == len(results) else 1


if __name__ == "__main__":
    raise SystemExit(main())
