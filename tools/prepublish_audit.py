"""Pre-publish audit for the public GitHub/CurseForge release.

Reports what the current git tree would publish: third-party binaries, the upstream jar, oversized
files, local paths and possible credentials - the things that must not end up in a public repo.
"""

import pathlib
import re
import subprocess
import sys

KEY_PATTERNS = [
    ("curseforge api key", re.compile(r"cf-api|curseforge.{0,20}(key|token)|X-Api-Key", re.I)),
    ("github token", re.compile(r"gh[pousr]_[A-Za-z0-9]{20,}|github.{0,20}token", re.I)),
    ("generic secret", re.compile(r"(secret|password|passwd|api[_-]?key)\s*[:=]\s*[\"'][^\"']{12,}", re.I)),
    ("private key block", re.compile(r"BEGIN [A-Z ]*PRIVATE KEY")),
]

BINARY_DIRS = ("libs/", "maid-compat/libs/", "libs-compile/", "准备的前置/", "需要移植的mod/")
TEXT_EXT = {".java", ".gradle", ".properties", ".md", ".json", ".toml", ".py", ".txt", ".json5",
            ".cfg", ".yml", ".yaml", ".ps1", ".cmd", ".bat", ".gitignore"}


def main():
    files = subprocess.run(["git", "ls-files"], capture_output=True, text=True,
                           check=True).stdout.splitlines()
    print("tracked files: %d" % len(files))

    sizes = {}
    total = 0
    for name in files:
        p = pathlib.Path(name)
        if not p.is_file():
            continue
        size = p.stat().st_size
        sizes[name] = size
        total += size
    print("tracked size : %.1f MB" % (total / 1048576))

    print("\n-- third-party / upstream binaries (must NOT be published) --")
    blocked = 0
    for prefix in BINARY_DIRS:
        group = [n for n in files if n.replace("\\", "/").startswith(prefix)]
        group_size = sum(sizes.get(n, 0) for n in group)
        if group:
            print("  %-22s %3d file(s)  %6.1f MB" % (prefix, len(group), group_size / 1048576))
            blocked += group_size
    jars = [n for n in files if n.endswith(".jar")]
    print("  total jars tracked: %d  (%.1f MB)" % (len(jars), sum(sizes.get(n, 0) for n in jars) / 1048576))
    print("  size we would need to drop: %.1f MB" % (blocked / 1048576))

    print("\n-- files over 5 MB (GitHub warns above 50 MB, hard limit 100 MB) --")
    for name, size in sorted(sizes.items(), key=lambda kv: -kv[1])[:6]:
        if size > 5 * 1048576:
            print("  %8.2f MB  %s" % (size / 1048576, name))

    print("\n-- build logs --")
    logs = [n for n in files if n.replace("\\", "/").startswith("build-logs/")]
    print("  %d file(s), %.1f MB" % (len(logs), sum(sizes.get(n, 0) for n in logs) / 1048576))

    print("\n-- licence / readme / gitignore --")
    for name in ("LICENSE", "LICENSE.md", "LICENSE.txt", "README.md", ".gitignore", "NOTICE"):
        print("  %-12s %s" % (name, "present" if pathlib.Path(name).exists() else "MISSING"))

    print("\n-- possible credentials in tracked text files --")
    hits = 0
    for name in files:
        p = pathlib.Path(name)
        if p.suffix not in TEXT_EXT or not p.is_file() or sizes.get(name, 0) > 2_000_000:
            continue
        try:
            text = p.read_text(encoding="utf-8", errors="ignore")
        except OSError:
            continue
        for label, pattern in KEY_PATTERNS:
            for match in pattern.finditer(text):
                hits += 1
                print("  %s: %s -> %r" % (label, name, match.group(0)[:60]))
    if not hits:
        print("  none found")

    print("\n-- local absolute paths in tracked files (informational) --")
    sample = {}
    for name in files:
        p = pathlib.Path(name)
        if p.suffix not in TEXT_EXT or not p.is_file() or sizes.get(name, 0) > 2_000_000:
            continue
        try:
            text = p.read_text(encoding="utf-8", errors="ignore")
        except OSError:
            continue
        for match in re.finditer(r"[A-Za-z]:\\[^\s\"'`]{3,60}", text):
            sample.setdefault(match.group(0)[:40], 0)
            sample[match.group(0)[:40]] += 1
    for path, count in sorted(sample.items(), key=lambda kv: -kv[1])[:10]:
        print("  %4d x %s" % (count, path))
    return 0


if __name__ == "__main__":
    sys.exit(main())
