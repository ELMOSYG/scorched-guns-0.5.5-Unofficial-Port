"""Find @SubscribeEvent listeners registered against an abstract event class.

NeoForge's event bus refuses them:

    Cannot register listeners for abstract class <event>.
    Register a listener to one of its subclasses instead!

Forge tolerated it, so 0.5.5 shipped several listeners declared against the base
class. Each one aborts mod loading (client-only ones abort the *client*, which a
dedicated-server smoke test cannot see), so they need to be found together.

Abstractness is read from the real NeoForge/vanilla sources in `.refs/nf-src`.

usage: python tools/audit_abstract_events.py
"""
from __future__ import annotations

import os
import re
import sys

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")
REFS = os.path.join(ROOT, ".refs", "nf-src")

IMPORT = re.compile(r"import\s+([\w.]+);")


def subscribe_handlers(body: str):
    """(method name, first parameter type) for each @SubscribeEvent method."""
    for m in re.finditer(r"@SubscribeEvent", body):
        seg = body[m.end():m.end() + 300]
        cut = seg.find("{")
        if cut >= 0:
            seg = seg[:cut]
        if ";" in seg:
            continue
        pm = re.search(r"\b(\w+)\s*\(([^)]*)\)", seg)
        if not pm:
            continue
        parts = [p.strip() for p in pm.group(2).split(",") if p.strip()]
        if not parts:
            continue
        pt = re.match(r"(?:final\s+)?([\w.]+)\s+\w+", parts[0])
        if pt:
            yield pm.group(1), pt.group(1)


def source_for(fqn: str) -> str | None:
    path = os.path.join(REFS, fqn.replace(".", os.sep) + ".java")
    return path if os.path.isfile(path) else None


def is_abstract(fqn: str) -> bool:
    """True if the class itself is declared abstract (nested types checked too)."""
    parts = fqn.split(".")
    for cut in range(len(parts), 2, -1):
        outer = ".".join(parts[:cut])
        path = source_for(outer)
        if not path:
            continue
        text = open(path, encoding="utf-8", errors="replace").read()
        simple = parts[-1] if cut == len(parts) else parts[cut - 1]
        if cut == len(parts):
            if re.search(r"\babstract\s+(?:final\s+)?class\s+" + re.escape(simple) + r"\b", text):
                return True
        else:
            # nested: `abstract static class Pre` / `public abstract static class Pre`
            if re.search(r"\babstract\s+(?:static\s+)?class\s+" + re.escape(parts[-1]) + r"\b", text):
                return True
        return False
    return False


def main() -> None:
    offenders = []
    for d, _, fs in os.walk(SRC):
        for f in fs:
            if not f.endswith(".java"):
                continue
            path = os.path.join(d, f)
            text = open(path, encoding="utf-8", errors="replace").read()
            if "@SubscribeEvent" not in text:
                continue
            body = re.sub(r"//[^\n]*", " ", text)
            imports = {fqn.split(".")[-1]: fqn for fqn in IMPORT.findall(body)}
            pkg = re.search(r"package\s+([\w.]+);", body)
            for name, ptype in subscribe_handlers(body):
                # keep the nested part: `PlayerTickEvent.Pre` must resolve to the
                # concrete subclass, not to the abstract outer class
                seg = ptype.split(".")
                head, nested = seg[0], seg[1:]
                fqn = imports.get(head)
                if fqn is None and pkg:
                    fqn = pkg.group(1) + "." + head
                if not fqn:
                    continue
                if nested:
                    fqn = fqn + "." + ".".join(nested)
                if is_abstract(fqn):
                    rel = os.path.relpath(path, SRC).replace("\\", "/")
                    offenders.append((rel, name, ptype, fqn))

    for rel, name, ptype, fqn in sorted(offenders):
        print("%-58s %-26s %s" % (rel, name, ptype))
    print("\n%d listener(s) registered against an abstract event class" % len(offenders))
    if offenders:
        sys.exit(1)


if __name__ == "__main__":
    main()
