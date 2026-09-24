"""Dump two regions of Sable's punch packet: the local setup and punchCurve.

Used to calibrate our structure impulse against Sable's own player-punch force.

Usage:
    python tools/dump_punch_math.py
"""
import os
import subprocess
import sys

JAVAP = r'D:\jdk-21.0.3\bin\javap.exe'
JAR = os.path.abspath(r'libs/sable-neoforge-1.21.1-2.0.5.jar')
CLS = 'dev.ryanhcode.sable.network.packets.tcp.ServerboundPunchSubLevelPacket'


def main():
    proc = subprocess.run([JAVAP, '-p', '-c', '-cp', JAR, CLS], capture_output=True)
    lines = proc.stdout.decode('utf-8', 'replace').splitlines()
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')

    # 1. the dstore 10 initialization inside handle()
    start = next(i for i, l in enumerate(lines) if 'void handle' in l)
    print('=== handle() locals, first 150 lines ===')
    for line in lines[start:start + 150]:
        text = line.strip()
        if text and ('ldc' in text or 'dstore' in text or 'applyImpulse' in text
                     or 'getInverseNormalMass' in text or 'Strength' in text
                     or 'ifge' in text or 'dload' in text or 'invoke' in text):
            print(text[:150])

    # 2. punchCurve in full
    idx = [i for i, l in enumerate(lines) if 'punchCurve' in l and '(' in l and 'double' in l]
    if idx:
        start = idx[-1]
        print('')
        print('=== punchCurve ===')
        for line in lines[start:start + 60]:
            print(line.rstrip()[:150])
    return 0


if __name__ == '__main__':
    sys.exit(main())
