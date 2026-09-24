"""Audit `@EventBusSubscriber` classes against what NeoForge's scanner requires.

`AutomaticEventSubscriber` registers the annotated class itself, so the class that
carries the annotation must declare at least one `@SubscribeEvent` method, and every
one of those must be `static`. Two failures abort mod loading:

    class X has no @SubscribeEvent methods, but register was called anyway.
    Expected @SubscribeEvent method ... to NOT be static ... (mirror case)

`RangeFinderItem` is the instructive one: 0.5.5 put `@EventBusSubscriber` on **both**
the outer item class and a nested `ClientEventHandler` that actually holds the
handler. Forge ignored the outer registration; NeoForge aborts. Per-file greps miss
it because the file does contain `@SubscribeEvent` - just not on the annotated class.

Also reports the dist a subscriber is limited to, so client-only problems (invisible
to a dedicated-server smoke test) are easy to spot.

usage: python tools/audit_subscribers.py
"""
from __future__ import annotations

import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from port_rewrite import match_forward  # noqa: E402

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")

ANN = re.compile(r"@EventBusSubscriber\s*\(")
DECL = re.compile(r"\b(?:class|interface|enum|record)\s+(\w+)")
STRING = re.compile(r'"(?:\\.|[^"\\])*"')
CHARLIT = re.compile(r"'(?:\\.|[^'\\])*'")
LINE_COMMENT = re.compile(r"//[^\n]*")
BLOCK_COMMENT = re.compile(r"/\*.*?\*/", re.S)


def blank_out(text: str) -> str:
    """Replace comments and literals with spaces so brace counting is reliable."""
    def blank(m: re.Match) -> str:
        return re.sub(r"[^\n]", " ", m.group(0))
    text = BLOCK_COMMENT.sub(blank, text)
    text = LINE_COMMENT.sub(blank, text)
    text = STRING.sub(lambda m: " " * len(m.group(0)), text)
    text = CHARLIT.sub(lambda m: " " * len(m.group(0)), text)
    return text


def handlers_owned_by(body: str, brace: int, end: int) -> list[tuple[str, bool]]:
    """(method name, is_static) for @SubscribeEvent methods at the class's own level."""
    out = []
    i, depth = brace + 1, 1
    while i < end and depth >= 1:
        ch = body[i]
        if ch == "{":
            depth += 1
        elif ch == "}":
            depth -= 1
        elif depth == 1 and body.startswith("@SubscribeEvent", i):
            seg = body[i + len("@SubscribeEvent"):i + 400]
            cut = seg.find("{")
            head = seg[:cut] if cut >= 0 else seg
            name = re.search(r"\b(\w+)\s*\(", head)
            out.append((name.group(1) if name else "?", bool(re.search(r"\bstatic\b", head))))
            i += len("@SubscribeEvent")
            continue
        i += 1
    return out


def main() -> None:
    problems = 0
    client_only_problems = 0
    rows = []
    for d, _, fs in os.walk(SRC):
        for f in fs:
            if not f.endswith(".java"):
                continue
            path = os.path.join(d, f)
            raw = open(path, encoding="utf-8", errors="replace").read()
            if "@EventBusSubscriber" not in raw:
                continue
            rel = os.path.relpath(path, SRC).replace("\\", "/")
            body = blank_out(raw)
            for m in ANN.finditer(body):
                open_paren = body.find("(", m.start())
                close = match_forward(body, open_paren)
                if close < 0:
                    continue
                args = body[open_paren + 1:close]
                client_only = "Dist.CLIENT" in args
                dm = DECL.search(body, close)
                if not dm:
                    continue
                cls = dm.group(1)
                brace = body.find("{", dm.end())
                if brace < 0:
                    continue
                end = match_forward(body, brace)
                hs = handlers_owned_by(body, brace, end)
                notes = []
                if not hs:
                    notes.append("CRASH: no @SubscribeEvent methods on the annotated class")
                elif any(not s for _, s in hs):
                    notes.append("CRASH: non-static handler(s) %s" % [n for n, s in hs if not s])
                if notes:
                    problems += 1
                    if client_only:
                        client_only_problems += 1
                    rows.append((rel, cls, "client-only" if client_only else "both dists",
                                 "; ".join(notes)))

    for rel, cls, dist, note in sorted(rows):
        print("%-52s %-26s %-11s %s" % (rel, cls, dist, note))
    print("\n%d blocking subscriber problem(s) (%d of them client-only)"
          % (problems, client_only_problems))
    sys.exit(1 if problems else 0)


if __name__ == "__main__":
    main()
