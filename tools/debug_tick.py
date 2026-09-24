"""Debug helper for the tick handler rewrite."""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from port_rewrite2 import RETURN_TYPE, phase_of, strip_phase_term  # noqa: E402

path = sys.argv[1]
text = open(path, encoding="utf-8").read()
for m in RETURN_TYPE.finditer(text):
    print("MATCH", m.group("type"), m.group("param"), "at", m.start())
    brace = text.index("{", m.end() - 1)
    ifm = re.compile(r"if\s*\(").search(text, brace)
    print("   first if:", repr(text[ifm.start():ifm.start() + 60]) if ifm else None)
    if ifm:
        op = text.index("(", ifm.start())
        from port_rewrite import match_forward
        cl = match_forward(text, op)
        cond = text[op + 1:cl]
        print("   cond:", repr(cond), "phase:", phase_of(cond, m.group("param")),
              "rest:", repr(strip_phase_term(cond, m.group("param"))))
