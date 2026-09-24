"""Print the LocalVariableTable of Minecraft.startUseItem so a LocalCapture
injection can be checked against the real locals instead of guessed at.

Usage:
    python tools/probe_startuseitem_locals.py
"""

import pathlib
import re
import subprocess

JAR = "build/moddev/artifacts/neoforge-21.1.249-merged.jar"
JAVAP = r"D:\jdk-21.0.3\bin\javap.exe"


def main():
    result = subprocess.run(
        [JAVAP, "-p", "-c", "-l", "-classpath", JAR, "net.minecraft.client.Minecraft"],
        capture_output=True, text=True, errors="replace",
    )
    lines = result.stdout.splitlines()

    start = next((i for i, l in enumerate(lines) if "startUseItem()" in l and "private" in l), None)
    if start is None:
        print("startUseItem not found")
        return
    # Method body runs until the next method declaration.
    end = next((i for i in range(start + 1, len(lines))
                if re.match(r"^  \S.*\(.*\);?$", lines[i]) and "LocalVariableTable" not in lines[i]),
               len(lines))
    body = lines[start:end]

    print("=== itemUsed call sites ===")
    for line in body:
        if "itemUsed" in line:
            print("   " + line.strip())

    print("")
    print("=== LocalVariableTable ===")
    in_table = False
    for line in body:
        if "LocalVariableTable" in line:
            in_table = True
            continue
        if in_table:
            if line.strip().startswith("Start") or line.strip().startswith("Slot"):
                print("   " + line.strip())
            elif re.match(r"^\s+\d+\s+\d+\s+\d+\s+\S", line):
                print("   " + line.strip())


if __name__ == "__main__":
    main()
