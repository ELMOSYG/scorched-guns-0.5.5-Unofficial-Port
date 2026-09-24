"""Print the bytecode of the punch handler between two offsets (for attribute discovery)."""
import os
import re
import subprocess
import sys

JAVAP = r'D:\jdk-21.0.3\bin\javap.exe'
JAR = os.path.abspath(r'libs/sable-neoforge-1.21.1-2.0.5.jar')
CLS = 'dev.ryanhcode.sable.network.packets.tcp.ServerboundPunchSubLevelPacket'
LO, HI = 180, 215


def main():
    proc = subprocess.run([JAVAP, '-p', '-c', '-cp', JAR, CLS], capture_output=True)
    lines = proc.stdout.decode('utf-8', 'replace').splitlines()
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
    start = next(i for i, l in enumerate(lines) if 'void handle' in l)
    for line in lines[start:start + 400]:
        m = re.match(r'\s*(\d+):', line)
        if m and LO <= int(m.group(1)) <= HI:
            print(line.rstrip()[:170])
    return 0


if __name__ == '__main__':
    sys.exit(main())
