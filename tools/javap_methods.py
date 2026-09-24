"""Dump the public shape of selected classes from a jar (methods only).

Usage:
    python tools/javap_methods.py libs/sable-neoforge-1.21.1-2.0.5.jar dev.a.B dev.a.C ...
"""
import os
import subprocess
import sys

JAVAP = r'D:\jdk-21.0.3\bin\javap.exe'


def main():
    args = sys.argv[1:]
    if len(args) < 2:
        raise SystemExit(__doc__)
    jar, classes = os.path.abspath(args[0]), args[1:]
    proc = subprocess.run([JAVAP, '-p', '-cp', jar] + classes, capture_output=True)
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
    for line in proc.stdout.decode('utf-8', 'replace').splitlines():
        line = line.rstrip()
        if not line or line.startswith('Compiled from') or line.strip() in ('}', '{'):
            continue
        # keep declarations only, drop private fields noise
        if '(' in line or line.startswith('public') or line.startswith('class') \
                or line.startswith('interface') or line.startswith('final'):
            print(line[:170])
    sys.stderr.write(proc.stderr.decode('utf-8', 'replace'))
    return 0


if __name__ == '__main__':
    sys.exit(main())
