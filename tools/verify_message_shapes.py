"""Statically verify that FrameworkMessageBridge can resolve every message class.

`FrameworkMessageBridge` resolves `decode`/`encode`/`handle` by reflection exactly
once per class, from `PacketHandler.init()`. A class that does not expose the
expected shape therefore does not fail at compile time - it throws
`IllegalStateException` while the mod is starting up. This script re-implements
the bridge's own acceptance test over the Java sources so the whole registered
set can be checked without launching the game.

usage: python tools/verify_message_shapes.py [message-dir]
"""
from __future__ import annotations

import os
import re
import sys

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
PKG = os.path.join(ROOT, "src", "main", "java", "top", "ribs", "scguns", "network", "message")
PACKET_HANDLER = os.path.join(ROOT, "src", "main", "java", "top", "ribs", "scguns", "network", "PacketHandler.java")

# the two buffer types the bridge accepts; RegistryFriendlyByteBuf is a subclass
BUFFER = re.compile(r"^(?:Registry)?FriendlyByteBuf$")
ARG = re.compile(r"[\w.<>\[\]]+$")


def find_method(flat: str, name: str, type_name: str) -> list[tuple[str, list[str]]]:
    """All `returnType name(args)` declarations for `name` with `args` arity > 0."""
    out = []
    for m in re.finditer(r"([\w.<>\[\]]+)\s+" + name + r"\s*\(([^)]*)\)", flat):
        ret, raw = m.group(1), m.group(2)
        if ret in ("new", "return", "this", "super"):
            continue
        params = [p.strip() for p in raw.split(",") if p.strip()]
        out.append((ret, params))
    return out


def classify_type(param: str) -> str:
    """The declared type of a parameter written as `Type name` (or `Type<X> name`).

    The last whitespace-separated token is the parameter NAME, so the type is the
    token before it; anything shorter is returned unchanged.
    """
    tokens = [t for t in param.strip().split() if t]
    if len(tokens) < 2:
        return param.strip()
    return tokens[-2].split("<")[0].strip()


def check(path: str) -> list[str]:
    cls = os.path.basename(path)[:-5]
    src = open(path, encoding="utf-8", errors="replace").read()
    # strip comments BEFORE collapsing whitespace - otherwise `//[^\n]*` runs to EOF
    src = re.sub(r"/\*.*?\*/", " ", src, flags=re.S)
    src = re.sub(r"//[^\n]*", " ", src)
    flat = re.sub(r"\s+", " ", src)
    problems = []

    if not re.search(r"\b" + cls + r"\s*\(\s*\)\s*\{", flat):
        problems.append("no no-argument constructor (bridge needs it as the decode receiver)")

    ok_decode = False
    for ret, params in find_method(flat, "decode", cls):
        if len(params) == 1 and BUFFER.match(classify_type(params[0])) and ret.endswith(cls):
            ok_decode = True
    if not ok_decode:
        problems.append("no usable decode(...) : expected `%s decode(<buf>)`" % cls)

    ok_encode = False
    for ret, params in find_method(flat, "encode", cls):
        if len(params) == 2 and ret == "void":
            a, b = classify_type(params[0]), classify_type(params[1])
            if (a == cls or params[0].split()[-1].startswith(cls)) and BUFFER.match(b):
                ok_encode = True
    if not ok_encode:
        problems.append("no usable encode(...) : expected `void encode(%s, <buf>)`" % cls)

    ok_handle = False
    for ret, params in find_method(flat, "handle", cls):
        if len(params) == 2 and ret == "void" and classify_type(params[1]) == "MessageContext":
            ok_handle = True
    if not ok_handle:
        problems.append("no handle(X, MessageContext) - bridge substitutes a loud placeholder")

    return problems


def main() -> None:
    msg_dir = sys.argv[1] if len(sys.argv) > 1 else PKG
    handler = open(PACKET_HANDLER, encoding="utf-8", errors="replace").read()
    registered = set(re.findall(r'registerPlayMessage\("\w+",\s*(\w+)\.class', handler))

    files = sorted(f for f in os.listdir(msg_dir) if f.endswith(".java"))
    fatal = 0
    print("registered by PacketHandler: %d" % len(registered))
    for f in files:
        cls = f[:-5]
        problems = check(os.path.join(msg_dir, f))
        tag = "REGISTERED" if cls in registered else "not registered"
        if not problems:
            continue
        blocking = cls in registered
        if blocking:
            fatal += 1
        print("\n%s  %s  (%s)" % ("FAIL" if blocking else "note", cls, tag))
        for p in problems:
            print("      %s" % p)

    missing = registered - {f[:-5] for f in files}
    if missing:
        print("\nFAIL registered but no source file: %s" % ", ".join(sorted(missing)))
        fatal += len(missing)

    print("\n%d message classes scanned, %d blocking problems" % (len(files), fatal))
    if fatal == 0:
        print("every registered message satisfies the bridge contract (startup will not throw)")
    else:
        sys.exit(1)


if __name__ == "__main__":
    main()
