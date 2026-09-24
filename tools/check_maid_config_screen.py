"""Report config options that the maid compat's Cloth Config screen does not expose.

A config value the player cannot reach from the screen is not a bug by itself (the TOML file is
always there), but the screen is the discoverable half - and the port grew several options after the
screen was written, so this keeps the two from drifting silently.

Usage:
    python tools/check_maid_config_screen.py
"""
import pathlib
import re
import sys

ROOT = pathlib.Path("maid-compat/src/main/java/com/scg2tlm/elmomod")
CONFIG = ROOT / "SCG2TLMConfig.java"
SCREEN = ROOT / "client/SCG2TLMClothConfig.java"


def main():
    config_text = CONFIG.read_text(encoding="utf-8")
    screen_text = SCREEN.read_text(encoding="utf-8") if SCREEN.exists() else ""

    # Generic types included: ConfigValue<String> etc. The spec field itself (ModConfigSpec SPEC) is
    # not an option, so only declarations whose type name contains "Value" count.
    declared = set()
    for type_name, name in re.findall(
            r"public static final (ModConfigSpec\.\w+(?:<[^>]*>)?)\s+(\w+)\s*;", config_text):
        if "Value" in type_name:
            declared.add(name)
    shown = set(re.findall(r"SCG2TLMConfig\.(\w+)", screen_text))

    missing = sorted(declared - shown)
    print("config options declared : %d" % len(declared))
    print("referenced by the screen: %d" % len(shown & declared))
    print("not exposed in the screen: %d" % len(missing))
    for name in missing:
        print("   %s" % name)

    if not SCREEN.exists():
        print("FAIL: the config screen is missing entirely")
        return 1

    # The screen must not reference options that no longer exist; javac catches that, but saying it
    # here makes the failure legible before the compiler is reached. SPEC is the spec itself (the
    # screen saves it), not an option.
    unknown = sorted(shown - declared - {"SPEC"})
    if unknown:
        print("FAIL: the screen references undeclared options: %s" % unknown)
        return 1

    return 0


if __name__ == "__main__":
    sys.exit(main())
