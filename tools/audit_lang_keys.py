"""Compare every packaged language file against the English one.

The mod ships en_us plus several translations, and zh_cn was added by embedding the community
translation pack the user had enabled (it covered all 1784 keys, so the pack is no longer needed).
This reports, per file, which English keys have no translation and which keys the file carries that
English does not - the latter are usually keys added by a translator ahead of the mod, which are
harmless but worth seeing.

It is a report, not a gate: a partial translation is a legitimate state, and the port's fidelity
target is the English file, which is verified separately to match 0.5.5 key for key.

Usage:
    python tools/audit_lang_keys.py
"""

import json
import pathlib
import sys

LANG_DIR = pathlib.Path("src/main/resources/assets/scguns/lang")
REFERENCE = "en_us.json"


def subtitle_report(reference):
    """Report the subtitle keys the client actually asks for.

    Vanilla builds a sound's subtitle key as "subtitles.<namespace>.<sound path>" - with an s -
    and takes it from the language file; 1.21.1's SoundEvent no longer even carries a subtitle
    component. This mod's 46 keys use the legacy "subtitle.scguns.*" spelling instead, so they can
    never be reached. The check below derives the correct keys from sounds.json and reports the gap,
    which is what a player would otherwise see as a raw key in the subtitle line.
    """
    sounds_path = LANG_DIR.parent / "sounds.json"
    if not sounds_path.exists():
        return
    sounds = json.loads(sounds_path.read_text(encoding="utf-8"))
    needed = ["subtitles.scguns." + name for name in sounds]
    have = set(reference)
    missing = [k for k in needed if k not in have]
    print("")
    print("subtitles the client asks for (%d sound events):" % len(needed))
    print("  present: %d   missing: %d" % (len(needed) - len(missing), len(missing)))
    for key in missing[:5]:
        print("   missing: %s" % key)
    if len(missing) > 5:
        print("   ... %d more" % (len(missing) - 5))
    legacy = [k for k in reference if k.startswith("subtitle.")]
    print("  legacy 'subtitle.*' keys in the reference (%d) - never looked up by the client" % len(legacy))


def main():
    reference = json.loads((LANG_DIR / REFERENCE).read_text(encoding="utf-8"))
    print("%-14s %6s %8s %10s %8s" % ("file", "keys", "missing", "extra", "coverage"))
    problems = 0
    for path in sorted(LANG_DIR.glob("*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        missing = sorted(set(reference) - set(data))
        extra = sorted(set(data) - set(reference))
        covered = 100.0 * len(set(reference) & set(data)) / max(1, len(reference))
        print("%-14s %6d %8d %10d %7.1f%%" % (path.name, len(data), len(missing), len(extra), covered))
        if missing and path.name == REFERENCE:
            problems += 1
            print("   the reference file itself is missing keys - that is a bug")
        if missing and path.name not in (REFERENCE,):
            for key in missing[:3]:
                print("   untranslated: %s" % key)
            if len(missing) > 3:
                print("   ... %d more" % (len(missing) - 3))
    subtitle_report(reference)
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
