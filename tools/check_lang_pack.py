"""Check a Chinese resource pack against the mod's own language file.

The unlock notice showed "敌人现在可生成携带:" with nothing after it, while the line above it correctly
said 铁. That is the signature of a translation whose format placeholder was dropped: the argument is
still passed, but the string has nowhere to put it. The pack in the player's instance turns out to hold
exactly that for two keys, so this compares the whole pack - every key, and every placeholder - against
what the mod ships.

usage: python tools/check_lang_pack.py <pack.zip> [<mod zh_cn.json>]
"""
from __future__ import annotations

import json
import re
import sys
import zipfile

DEFAULT_MOD = r"src\main\resources\assets\scguns\lang\zh_cn.json"
PLACEHOLDER = re.compile(r"%(?:\d+\$)?[sdf]")


def load_pack(path: str) -> dict:
    with zipfile.ZipFile(path) as z:
        for name in z.namelist():
            if name.endswith("lang/zh_cn.json"):
                return json.loads(z.read(name).decode("utf-8-sig"))
    raise SystemExit("no assets/<namespace>/lang/zh_cn.json inside %s" % path)


def main() -> int:
    if len(sys.argv) < 2:
        raise SystemExit(__doc__)
    pack_path = sys.argv[1]
    mod_path = sys.argv[2] if len(sys.argv) > 2 else DEFAULT_MOD

    pack = load_pack(pack_path)
    mod = json.load(open(mod_path, encoding="utf-8"))

    missing = sorted(k for k in mod if k not in pack)
    extra = sorted(k for k in pack if k not in mod)
    mismatched = []
    for key in sorted(set(pack) & set(mod)):
        want = sorted(PLACEHOLDER.findall(mod[key]))
        have = sorted(PLACEHOLDER.findall(pack[key]))
        if want != have:
            mismatched.append((key, mod[key], pack[key], want, have))

    print("pack: %s (%d keys)" % (pack_path, len(pack)))
    print("mod:  %s (%d keys)" % (mod_path, len(mod)))
    print("")
    print("keys in the mod but not in the pack: %d" % len(missing))
    for key in missing[:15]:
        print("   %s" % key)
    print("keys in the pack but not in the mod: %d" % len(extra))
    for key in extra[:15]:
        print("   %s" % key)
    print("")
    print("keys whose placeholders differ: %d" % len(mismatched))
    for key, mod_text, pack_text, want, have in mismatched:
        print("   %s" % key)
        print("      mod  %r  %s" % (mod_text, want))
        print("      pack %r  %s" % (pack_text, have))
    return 1 if (missing or extra or mismatched) else 0


if __name__ == "__main__":
    sys.exit(main())
