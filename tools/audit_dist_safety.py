"""Audit for client-only types leaking into server-side me​thod signatures.

NeoForge's `AutomaticEventSubscriber` asks the JVM for a class's declared methods
while the dedicated server is starting. Resolving a method signature forces its
parameter and return types to be loaded, so a single un-annotated method that
mentions a client-only class aborts mod loading with

    Attempted to load class net/minecraft/client/Minecraft for invalid dist DEDICATED_SERVER

`@OnlyIn(Dist.CLIENT)` members are stripped on the server and are therefore safe;
anything else is not. This finds signature-level leaks, so they can be fixed in one
pass instead of one crash at a time.

usage: python tools/audit_dist_safety.py [--all]
       (default: only classes the automatic subscriber scans, i.e. @EventBusSubscriber)
"""
from __future__ import annotations

import os
import re
import sys

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")

CLASS_DECL = re.compile(r"(?:^|\n)\s*(?:public\s+|final\s+|abstract\s+)*class\s+(\w+)")
IMPORT = re.compile(r"import\s+([\w.]+);")
METHOD = re.compile(
    r"(?:^|\n)((?:[ \t]*(?:@\w+(?:\([^)]*\))?|public|protected|private|static|final|synchronized|"
    r"strictfp|native|abstract|default)\s*)*)"
    r"([\w.<>\[\],\s]+?)\s+(\w+)\s*\(([^)]*)\)\s*(?:throws [\w.,\s]+)?\{", re.M)


def client_types(imports: dict[str, str]) -> dict[str, str]:
    return {name: fqn for name, fqn in imports.items()
            if ".client." in fqn or fqn.startswith("net.minecraft.client.")}


def main() -> None:
    scan_all = "--all" in sys.argv
    hits = []
    for d, _, fs in os.walk(SRC):
        for f in fs:
            if not f.endswith(".java"):
                continue
            path = os.path.join(d, f)
            text = open(path, encoding="utf-8", errors="replace").read()
            if not scan_all and "@EventBusSubscriber" not in text:
                continue
            body = re.sub(r"//[^\n]*", " ", text)
            imports = {fqn.split(".")[-1]: fqn for fqn in IMPORT.findall(body)}
            clients = client_types(imports)
            if not clients:
                continue
            # The automatic subscriber only scans a class registered for the running
            # dist. Every @EventBusSubscriber in the file carrying `value = {Dist.CLIENT}`
            # (including one on an inner class) means a dedicated server never touches
            # it, so its signatures cannot leak there.
            anns_found = re.findall(r"@EventBusSubscriber\s*\(([^)]*)\)", body)
            if anns_found and all("Dist.CLIENT" in a for a in anns_found):
                continue
            if "@EventBusSubscriber" not in body and not scan_all:
                continue
            if "Dist.CLIENT" in body[:body.find("class ")] if "class " in body else False:
                continue
            scanned = "@EventBusSubscriber" in body
            for m in METHOD.finditer(body):
                anns, ret, name, params = m.group(1), m.group(2).strip(), m.group(3), m.group(4)
                if name in ("if", "for", "while", "switch", "catch", "return", "new"):
                    continue
                if "class" in anns or ret in ("class", "new"):
                    continue
                if "@OnlyIn" in anns:
                    continue
                referenced = set()
                for token in re.findall(r"\b[A-Z]\w*\b", ret + " " + params):
                    if token in clients:
                        referenced.add(token)
                if referenced:
                    rel = os.path.relpath(path, SRC).replace("\\", "/")
                    hits.append((rel, name, sorted(referenced), scanned))

    for rel, name, types, scanned in sorted(hits):
        tag = "SCANNED-BY-SUBSCRIBER" if scanned else "other"
        print("%-64s %-28s %-14s %s" % (rel, name, ",".join(types), tag))
    print("\n%d method signature(s) leak client-only types" % len(hits))


if __name__ == "__main__":
    main()
