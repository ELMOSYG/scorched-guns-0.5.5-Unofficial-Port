"""Audit every explicit `NeoForge.EVENT_BUS.register(...)` call.

NeoForge's event bus is far stricter than Forge's and each violation aborts mod
loading, so they have to be found together rather than one crash at a time:

  * "class X has no @SubscribeEvent methods, but register was called anyway"
    - Forge tolerated registering a class/object with nothing to subscribe.
  * "Expected @SubscribeEvent method ... to NOT be static because register() was
    called with an instance type" (and the mirror case for a class registration).
  * registering the same target twice - not an error, but the handlers then run
    twice per event.

An earlier version of this script only understood `new X()` / `X.get()` /
`X.class` / `this` and **silently skipped everything else**, which let
`ModelOverrides.register`'s `EVENT_BUS.register(model)` through and cost a client
launch to find. It now resolves the argument's declared type and reports anything
it cannot resolve instead of skipping it.

usage: python tools/audit_bus_registrations.py
"""
from __future__ import annotations

import collections
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from port_rewrite import match_forward  # noqa: E402

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")

CALL = re.compile(r"EVENT_BUS\.register\(")
COMMENT = re.compile(r"//[^\n]*")


def registrations(text: str):
    """(index, raw argument) for every register(...) call, paren-balanced."""
    for m in CALL.finditer(text):
        open_paren = m.end() - 1
        close = match_forward(text, open_paren)
        if close < 0:
            continue
        yield m.start(), text[open_paren + 1:close].strip()


def resolve(arg: str, text: str) -> tuple[str | None, str]:
    """(class simple name or None, 'instance' | 'class')."""
    if re.match(r"new\s+([\w.]+)\s*\(", arg):
        return re.match(r"new\s+([\w.]+)\s*\(", arg).group(1).split(".")[-1], "instance"
    m = re.match(r"([\w.]+)\.get\(\)$", arg)
    if m:
        return m.group(1).split(".")[-1], "instance"
    m = re.match(r"([\w.]+)\.class$", arg)
    if m:
        return m.group(1).split(".")[-1], "class"
    if arg == "this":
        return None, "instance"
    if re.fullmatch(r"[\w$]+", arg):
        # a local or field: look up its declared type in the same file
        m = re.search(r"\b([\w.]+)\s+" + re.escape(arg) + r"\s*(?:=|;|\))", text)
        if m:
            return m.group(1).split(".")[-1], "instance"
        return None, "instance"
    return None, "?"


def index_sources() -> dict[str, str]:
    out: dict[str, str] = {}
    for d, _, fs in os.walk(SRC):
        for f in fs:
            if f.endswith(".java"):
                out[f[:-5]] = os.path.join(d, f)
    return out


def handlers(path: str) -> tuple[list[str], list[str]]:
    """(static handler names, instance handler names) declared at the top level."""
    raw = open(path, encoding="utf-8", errors="replace").read()
    text = COMMENT.sub(lambda m: " " * len(m.group(0)), raw)
    statics, instances = [], []
    for m in re.finditer(r"@SubscribeEvent", text):
        seg = text[m.end():m.end() + 300]
        cut = seg.find("{")
        if cut >= 0:
            seg = seg[:cut]
        if ";" in seg:
            continue
        pm = re.search(r"\b(\w+)\s*\(", seg)
        if not pm:
            continue
        (statics if re.search(r"\bstatic\b", seg) else instances).append(pm.group(1))
    return statics, instances


def main() -> None:
    srcs = index_sources()
    problems = 0
    unresolved = 0
    seen: dict[str, list[str]] = collections.defaultdict(list)

    for d, _, fs in os.walk(SRC):
        for f in fs:
            if not f.endswith(".java"):
                continue
            path = os.path.join(d, f)
            raw = open(path, encoding="utf-8", errors="replace").read()
            if "EVENT_BUS.register(" not in raw:
                continue
            text = COMMENT.sub(lambda m: " " * len(m.group(0)), raw)
            rel = os.path.relpath(path, SRC).replace("\\", "/")
            for _, arg in registrations(text):
                cls, kind = resolve(arg, text)
                if cls is None:
                    if arg == "this":
                        continue  # the enclosing class; audited by audit_subscribers
                    problems += 1
                    unresolved += 1
                    print("%-58s register(%s)  <-- CANNOT RESOLVE, inspect by hand"
                          % (rel, arg))
                    continue
                if cls not in srcs:
                    problems += 1
                    unresolved += 1
                    print("%-58s register(%s)  <-- %s is not a mod class, inspect by hand"
                          % (rel, arg, cls))
                    continue
                statics, instances = handlers(srcs[cls])
                seen[cls].append("%s (%s)" % (rel, kind))
                notes = []
                if not statics and not instances:
                    notes.append("CRASH: no @SubscribeEvent methods")
                elif kind == "instance" and statics:
                    notes.append("CRASH: instance registration with static handler(s) %s" % statics)
                elif kind == "class" and instances:
                    notes.append("CRASH: class registration with instance handler(s) %s" % instances)
                if notes:
                    problems += 1
                    print("%-58s %-34s %s" % (rel, cls, "; ".join(notes)))

    dupes = {c: w for c, w in seen.items() if len(w) > 1}
    if dupes:
        print("\n=== registered more than once (handlers run once per registration) ===")
        for cls, where in sorted(dupes.items()):
            print("  %-34s %s" % (cls, ", ".join(where)))
    print("\n%d blocking registration problem(s) (%d unresolvable)" % (problems, unresolved))
    sys.exit(1 if problems else 0)


if __name__ == "__main__":
    main()
