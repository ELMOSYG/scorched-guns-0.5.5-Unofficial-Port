"""Run one compile round and summarise it in a single step.

Picks the next free `build-logs/compile-NNN.txt`, runs `gradlew compileJava`,
then prints the totals, the error kinds, a syntax-error check and the error dump
path. Writing the log directly from Python avoids the console-width wrapping that
PowerShell pipelines introduce (see HANDOFF.md section 3).

usage: python tools/round.py [--no-dump]
"""
from __future__ import annotations

import collections
import glob
import os
import re
import subprocess
import sys

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
LOGS = os.path.join(ROOT, "build-logs")
JAVA_HOME = r"D:\jdk-21.0.3"
ERR = re.compile(r"([A-Za-z]:\\[^:]+\.java):(\d+): error: (.*)$")
SYNTAX = ("expected", "illegal character", "reached end of file", "not a statement",
          "unclosed", "class, interface, enum, or record expected")


def next_log() -> str:
    numbers = []
    for path in glob.glob(os.path.join(LOGS, "compile-*.txt")):
        m = re.search(r"compile-(\d+)\.txt$", path)
        if m:
            numbers.append(int(m.group(1)))
    return os.path.join(LOGS, "compile-%d.txt" % (max(numbers) + 1 if numbers else 1))


def main() -> None:
    out = next_log()
    env = dict(os.environ, JAVA_HOME=JAVA_HOME)
    with open(out, "w", encoding="utf-8", newline="\n") as log:
        proc = subprocess.run(
            [os.path.join(ROOT, "gradlew.bat"), "compileJava", "--console=plain"],
            cwd=ROOT, env=env, stdout=log, stderr=subprocess.STDOUT,
        )
    text = open(out, encoding="utf-8", errors="replace").read()
    rows = [(m.group(1), int(m.group(2)), m.group(3))
            for m in (ERR.search(line) for line in text.splitlines()) if m]
    files = collections.Counter(os.path.basename(r[0]) for r in rows)
    kinds = collections.Counter(r[2].split(";")[0][:90] for r in rows)
    print("log:      %s" % out)
    print("gradle:   exit %d" % proc.returncode)
    print("errors:   %d over %d files" % (len(rows), len(files)))
    syntax = [k for k in kinds if any(s in k for s in SYNTAX)]
    print("syntax:   %s" % ("!!! " + "; ".join(syntax) if syntax else "clean (no syntax-class errors)"))
    print("\n-- error kinds --")
    for k, v in kinds.most_common(25):
        print("%5d  %s" % (v, k))
    print("\n-- worst files --")
    for k, v in files.most_common(12):
        print("%5d  %s" % (v, k))
    if "--no-dump" not in sys.argv:
        dump = os.path.join(LOGS, "errors-%s.txt" % re.search(r"compile-(\d+)", out).group(1))
        subprocess.run([sys.executable, os.path.join(ROOT, "tools", "dump_errors.py"), out, dump],
                       cwd=ROOT, stdout=subprocess.DEVNULL)
        print("\nerror dump: %s" % dump)


if __name__ == "__main__":
    main()
