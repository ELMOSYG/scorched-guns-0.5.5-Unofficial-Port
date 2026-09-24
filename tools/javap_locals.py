"""Print a method's LocalVariableTable (parameter names) from a jar.

Windows shells mangle nested class names and long classpaths, so this runs javap with an
argument list. Useful for checking the ORDER of parameters of an API that takes two
same-typed arguments (e.g. Sable's applyImpulseAtPoint(point, impulse)).

Usage:
    python tools/javap_locals.py libs/x.jar dev.a.B applyImpulseAtPoint
"""
import os
import subprocess
import sys

JAVAP = r'D:\jdk-21.0.3\bin\javap.exe'


def main():
    if len(sys.argv) < 4:
        raise SystemExit(__doc__)
    jar, cls, needle = os.path.abspath(sys.argv[1]), sys.argv[2], sys.argv[3]
    proc = subprocess.run([JAVAP, '-p', '-c', '-l', '-cp', jar, cls], capture_output=True)
    out = proc.stdout.decode('utf-8', 'replace').splitlines()
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
    printing = False
    for line in out:
        if needle in line and ('(' in line or 'Method' in line):
            printing = True
        if printing:
            print(line.rstrip()[:170])
            if 'LocalVariableTable' in line:
                printed = 0
                continue
        if printing and line.strip().startswith('public ') and needle not in line:
            printing = False
    print(proc.stderr.decode('utf-8', 'replace')[:400])
    return 0


if __name__ == '__main__':
    sys.exit(main())
