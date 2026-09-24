"""Run one or more commands against the dev server over RCON.

Usage:
    python tools/rcon_cmd.py "reload" "list"
    python tools/rcon_cmd.py --password scgunsverify "advancement grant @a only scguns:main/root"

The server must already be running with enable-rcon=true (run/server.properties).
"""
import os
import socket
import struct
import sys
import time

HOST = "127.0.0.1"
PORT = 25575
LOGIN, COMMAND = 3, 2


class Rcon:
    def __init__(self, password, timeout=15.0):
        self.sock = socket.create_connection((HOST, PORT), timeout=timeout)
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
        chunks, remaining = [], count
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
        deadline = time.time() + 30.0
        collected = []
        while time.time() < deadline:
            try:
                request_id, _kind, body = self._recv()
            except (socket.timeout, EOFError):
                break
            if request_id == sent:
                collected.append(body)
                if body:
                    break
        return "".join(collected)


def main():
    args = sys.argv[1:]
    password = os.environ.get("SCGUNS_RCON_PASSWORD", "scgunsverify")
    if args and args[0] == "--password":
        password = args[1]
        args = args[2:]
    if args and args[0] == "--file":
        # Windows PowerShell 5.1 strips the double quotes inside an argument it passes to a native
        # executable, which makes SNBT with quoted keys ("minecraft:mending") impossible to pass
        # inline. Reading the commands from a file avoids the shell entirely.
        with open(args[1], "r", encoding="utf-8") as handle:
            args = [line.strip() for line in handle if line.strip() and not line.startswith("#")]
    if not args:
        raise SystemExit(__doc__)
    rcon = Rcon(password)
    for text in args:
        print("> %s" % text)
        print(rcon.command(text))
    return 0


if __name__ == "__main__":
    sys.exit(main())
