"""Sixth-stage: finish the capability port in the files that used
LazyOptional#ifPresent / inline anonymous storages.

usage: python tools/port_rewrite6.py
"""
from __future__ import annotations

import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from port_rewrite import ensure_import  # noqa: E402

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")

# hand-authored port files must never be rewritten by the bulk rules
AUTHORED = {
    "util/NbtHelper.java", "util/Caps.java", "util/DistHelper.java", "util/MobType.java",
    "network/FrameworkMessageBridge.java", "network/PacketHandler.java",
    "init/ModCapabilities.java",
}

BE_IF_PRESENT = re.compile(
    r"(\b[\w.]+)\.getCapability\(\s*(Capabilities\.[\w.]+\.BLOCK)\s*,\s*([^()]*?)\s*\)\s*\.ifPresent\("
)
ITEM_IF_PRESENT = re.compile(
    r"(\b[\w.]+)\.getCapability\(\s*(Capabilities\.[\w.]+\.ITEM)\s*\)\s*\.ifPresent\("
)
VAR_IF_PRESENT = re.compile(r"\b(\w*[Cc]ap\w*|\w*[Ss]torage\w*|\w*[Hh]andler)\.ifPresent\(")

FIXES = [
    # LightningBattery anonymous EnergyStorage: paren eaten by the field rewrite
    (r"receiveEnergy\(maxReceive, simulate;", "receiveEnergy(maxReceive, simulate);"),
    (r"LazyOptional<IEnergyStorage> (\w+) = ([\w.]+)\.getCapability\(\s*Capabilities\.EnergyStorage\.ITEM\s*\);",
     r"IEnergyStorage \1 = \2.getCapability(Capabilities.EnergyStorage.ITEM);"),
    (r"private final LazyOptional<IEnergyStorage> externalEnergy = LazyOptional\.of\(\(\) -> new EnergyStorage\(",
     "private final IEnergyStorage externalEnergy = new EnergyStorage("),
]


def fix_anonymous_end(text: str, stats: dict) -> str:
    """`});` that closes a `= new EnergyStorage(...) {` field becomes `};`."""
    out = text
    lines = out.split("\n")
    for i, line in enumerate(lines):
        if line.strip() != "});":
            continue
        # walk back to the field declaration that owns this block
        depth = 0
        j = i - 1
        owner = None
        while j >= 0:
            stripped = lines[j].strip()
            depth += stripped.count("}") - stripped.count("{")
            if depth < 0:
                owner = stripped
                break
            j -= 1
        if owner and re.search(r"=\s*new\s+EnergyStorage\(|=\s*new\s+IEnergyStorage\(|=\s*new\s+\w*Storage\(", owner):
            indent = line[:len(line) - len(line.lstrip())]
            lines[i] = indent + "};"
            stats["anonymous storage end"] = stats.get("anonymous storage end", 0) + 1
    return "\n".join(lines)


def process(path: str, text: str) -> tuple[str, dict]:
    stats: dict = {}
    for pattern, repl in FIXES:
        text, n = re.subn(pattern, repl, text)
        if n:
            stats["fix"] = stats.get("fix", 0) + n
    if "ifPresent(" in text and "getCapability" in text:
        text, n = BE_IF_PRESENT.subn(r"Caps.ifPresent(Caps.of(\1, \2, \3), ", text)
        if n:
            stats["be ifPresent"] = n
        text, n = ITEM_IF_PRESENT.subn(r"Caps.ifPresent(\1.getCapability(\2), ", text)
        if n:
            stats["item ifPresent"] = n
        text, n = VAR_IF_PRESENT.subn(r"Caps.ifPresent(\1, ", text)
        if n:
            stats["local ifPresent"] = n
        text = ensure_import(text, "top.ribs.scguns.util.Caps")
    if "new EnergyStorage(" in text:
        text = fix_anonymous_end(text, stats)
    return text, stats


def main() -> None:
    files = [os.path.join(d, f) for d, _, fs in os.walk(SRC) for f in fs if f.endswith(".java")]
    total: dict = {}
    changed = 0
    for path in files:
        rel = os.path.relpath(path, SRC).replace("\\", "/")
        if rel in AUTHORED:
            continue
        text = open(path, encoding="utf-8", errors="replace").read()
        new, stats = process(path, text)
        if new != text:
            with open(path, "w", encoding="utf-8", newline="") as fh:
                fh.write(new)
            changed += 1
            for k, v in stats.items():
                total[k] = total.get(k, 0) + v
    print(f"rewrote {changed} files")
    for k, v in sorted(total.items(), key=lambda kv: -kv[1]):
        print(f"{v:6d}  {k}")


if __name__ == "__main__":
    main()
