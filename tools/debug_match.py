"""Debug: locate why brace matching fails on a specific source file."""
import inspect
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import port_rewrite  # noqa: E402

print("module file:", port_rewrite.__file__)
src = inspect.getsource(port_rewrite.match_forward)
print("has comment skipping:", "text[j + 1] == \"/\"" in src)

path = sys.argv[1]
index = int(sys.argv[2])
text = open(path, encoding="utf-8").read()
print("char at index:", repr(text[index]))
print("match_forward:", port_rewrite.match_forward(text, index))

# instrumented copy
pairs = {"(": ")", "[": "]", "{": "}"}
close = pairs[text[index]]
depth = 0
j = index
n = len(text)
trace = 0
while j < n:
    c = text[j]
    if c == '"':
        j += 1
        while j < n:
            if text[j] == "\\":
                j += 2
                continue
            if text[j] == '"':
                break
            j += 1
    elif c == "'" and port_rewrite.looks_like_char_literal(text, j):
        j += 1
        while j < n:
            if text[j] == "\\":
                j += 2
                continue
            if text[j] == "'":
                break
            j += 1
    elif c in pairs:
        depth += 1
    elif c == close:
        depth -= 1
        if depth == 0:
            print("instrumented close at", j)
            break
    j += 1
else:
    print("instrumented: no close found")
