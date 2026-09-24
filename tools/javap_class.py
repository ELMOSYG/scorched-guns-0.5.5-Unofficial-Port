"""javap a class out of a jar without shell quoting problems.

Windows shells mangle nested class names (the '$') and long -cp arguments, which
makes `javap 'a.b.Outer$Inner'` fail with "class not found". This runs javap with an
argument list instead (no shell), so nested names survive.

Usage:
    python tools/javap_class.py libs/sable-neoforge-1.21.1-2.0.5.jar 'dev.a.B$C' [-c]
"""
import os
import subprocess
import sys

JAVAP = r'D:\jdk-21.0.3\bin\javap.exe'


def main():
    args = [a for a in sys.argv[1:] if a != '-c']
    if len(args) < 2:
        raise SystemExit(__doc__)
    jar, classes = args[0], args[1:]
    cmd = [JAVAP, '-p'] + (['-c'] if '-c' in sys.argv else []) + ['-cp', os.path.abspath(jar)] + classes
    proc = subprocess.run(cmd, capture_output=True)
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
    text = proc.stdout.decode('utf-8', 'replace') + proc.stderr.decode('utf-8', 'replace')
    print(text)
    return 0


if __name__ == '__main__':
    sys.exit(main())
