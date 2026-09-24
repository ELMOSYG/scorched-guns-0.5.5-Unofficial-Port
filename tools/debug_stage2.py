"""Run one staged rewrite rule on one file and diff the result."""
import difflib
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from port_rewrite2 import process, registry_type_map, SRC  # noqa: E402

path = sys.argv[1]
text = open(path, encoding="utf-8").read()
mapping = registry_type_map(
    [os.path.join(d, f) for d, _, fs in os.walk(SRC) for f in fs if f.endswith(".java")]
)
new, stats = process(path, text, mapping)
print("stats:", stats)
for line in difflib.unified_diff(text.splitlines(), new.splitlines(), lineterm="", n=1):
    print(line)
