"""Compile the whole mod source tree with the JDK 21 javac, bypassing gradle.

Gradle rounds take minutes; this takes seconds and uses the same compiler and the
same classpath (`build-logs/compile-classpath.txt`, produced by
`gradlew printCompileClasspath`). It mirrors the `build.gradle` source-set
excludes so the JEI 19 classes stay out until they are migrated.

usage: python tools/javac_check.py [--keep-going]
"""
from __future__ import annotations

import collections
import os
import re
import subprocess
import sys

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")
CP_FILE = os.path.join(ROOT, "build-logs", "compile-classpath.txt")
SCRATCH = os.path.join(ROOT, "build", "javac-scratch")
LOGS = os.path.join(ROOT, "build-logs")
JAVAC = r"D:\jdk-21.0.3\bin\javac.exe"

# `build.gradle` no longer excludes anything: the JEI 19 block is part of the build
# again. Kept as a knob so a temporarily excluded file can be mirrored here.
PKG_PREFIX = "top/ribs/scguns/"
EXCLUDED: list[str] = []
ERR = re.compile(r"([A-Za-z]:\\[^:]+\.java):(\d+): error: (.*)$")


def main() -> None:
    if not os.path.isfile(CP_FILE):
        raise SystemExit("run `gradlew printCompileClasspath` first")
    classpath = open(CP_FILE, encoding="utf-8").read().strip()
    # `--with-jei` compiles the JEI 19 block too, which build.gradle excludes:
    # that previews exactly what removing the excludes will surface.
    with_jei = "--with-jei" in sys.argv
    skipped = set() if with_jei else {os.path.join(SRC, PKG_PREFIX.replace("/", os.sep),
                                                  e.replace("/", os.sep)) for e in EXCLUDED}

    files = [os.path.join(d, f)
             for d, _, fs in os.walk(SRC) for f in fs
             if f.endswith(".java") and os.path.join(d, f) not in skipped]
    os.makedirs(SCRATCH, exist_ok=True)

    numbers = [int(m.group(1)) for m in
               (re.search(r"javac-(\d+)\.txt$", p) for p in os.listdir(LOGS)) if m]
    log_path = os.path.join(LOGS, "javac-%d.txt" % (max(numbers) + 1 if numbers else 1))
    argfile = os.path.join(LOGS, ".javac-files.txt")
    with open(argfile, "w", encoding="utf-8") as fh:
        # javac's @argfile treats `\` as an escape character, so paths must be
        # written with forward slashes (accepted on Windows) or they get mangled.
        fh.write("\n".join('"%s"' % f.replace("\\", "/") for f in files))

    cmd = [JAVAC, "-J-Duser.language=en", "-J-Duser.country=US",
           "-cp", classpath.replace("\\", "/"), "-d", SCRATCH.replace("\\", "/"),
           "-encoding", "UTF-8", "-Xmaxerrs", "20000", "-nowarn", "-proc:none",
           "@" + argfile.replace("\\", "/")]
    with open(log_path, "w", encoding="utf-8", newline="\n") as log:
        proc = subprocess.run(cmd, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT)

    text = open(log_path, encoding="utf-8", errors="replace").read()
    rows = [m for m in (ERR.search(l) for l in text.splitlines()) if m]
    kinds = collections.Counter(r.group(3).split(";")[0][:80] for r in rows)
    per_dir = collections.Counter(
        "/".join(os.path.relpath(os.path.normpath(r.group(1)), SRC).replace("\\", "/").split("/")[3:4])
        for r in rows)
    print("log:    %s" % log_path)
    print("javac:  exit %d" % proc.returncode)
    print("files:  %d" % len(files))
    print("errors: %d over %d files" % (len(rows), len({r.group(1) for r in rows})))
    syntax = [k for k in kinds if any(s in k for s in
              ("expected", "illegal character", "reached end of file", "not a statement"))]
    print("syntax: %s" % ("!!! " + "; ".join(syntax) if syntax else "clean"))
    print("\n-- kinds --")
    for k, v in kinds.most_common(15):
        print("%5d  %s" % (v, k))
    print("\n-- by package --")
    for k, v in per_dir.most_common(15):
        print("%5d  %s" % (v, k))


if __name__ == "__main__":
    main()
