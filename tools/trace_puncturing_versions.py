"""Trace the Sharpshooter (puncturing) enchantment across every Scorched Guns version on disk.

The player suspects the damage penalty used to exist and was removed later, leaving a stale
description. That is checkable: for each jar, disassemble getPuncturingDamageReduction and read the
description string out of the same jar. Reporting both together shows whether the text and the code
ever agreed.

Usage: python tools/trace_puncturing_versions.py
"""
import json
import pathlib
import re
import subprocess
import sys
import zipfile

JAVAP = r"D:\jdk-21.0.3\bin\javap.exe"
JARS = [
    (r"E:\mod\scgun-0.5.5-1.21.1-neoforge\需要移植的mod\ScorchedGuns-0.5.5-1.20.1.jar", "0.5.5 (baseline, 1.20.1)"),
    (r"C:\Users\len\Desktop\ScorchedGuns-1.2.5.jar", "1.2.5"),
    (r"E:\mod\SCG2_TLM\MDK-1.21.1-ModDevGradle-main\libs\ScorchedGuns-1.5.jar", "1.5"),
    (r"E:\mod\GFL2MC\libs\ScorchedGuns-1.5.2.jar", "1.5.2"),
]
HELPER = "top.ribs.scguns.util.GunEnchantmentHelper"
OUT = pathlib.Path("build-logs/puncturing-history.txt")


def describe_code(jar):
    """Classify the method body: a bare `return damage;` or something that actually computes."""
    result = subprocess.run([JAVAP, "-p", "-c", "-classpath", jar, HELPER],
                            capture_output=True, text=True)
    text = result.stdout
    if "getPuncturingDamageReduction" not in text:
        return "method not present"
    body = text.split("getPuncturingDamageReduction", 1)[1]
    body = body.split("\n\n", 1)[0]
    ops = [l.strip() for l in body.splitlines() if re.match(r"\s+\d+:", l)]
    computes = any(op.split("//")[0].split(":")[1].strip().split()[0] in
                   ("fmul", "fsub", "fadd", "fdiv", "imul", "i2f", "ldc", "fload")
                   for op in ops if len(op.split(":")[1].strip().split()) > 1)
    return ("computes something (%d ops)" % len(ops)) if computes else "returns the argument unchanged (%d ops)" % len(ops)


def describe_lang(jar, lang="en_us"):
    with zipfile.ZipFile(jar) as z:
        name = "assets/scguns/lang/%s.json" % lang
        if name not in z.namelist():
            return "(no %s)" % lang
        data = json.loads(z.read(name).decode("utf-8"))
    for key in ("enchantment.scguns.puncturing.desc", "enchantment.scguns.puncturing.tooltip",
                "enchantment.scguns.puncturing"):
        if key.endswith(".desc") and key in data:
            return data[key]
    return "(no description key)"


def main():
    lines = []
    for path, label in JARS:
        if not pathlib.Path(path).exists():
            lines.append("%-26s MISSING" % label)
            continue
        lines.append("=" * 96)
        lines.append(label)
        lines.append("  code:      %s" % describe_code(path))
        for lang in ("en_us", "zh_cn"):
            lines.append("  desc[%s]: %s" % (lang, describe_lang(path, lang)))

    OUT.write_text("\n".join(lines), encoding="utf-8")
    print("\n".join(lines))
    return 0


if __name__ == "__main__":
    sys.exit(main())
