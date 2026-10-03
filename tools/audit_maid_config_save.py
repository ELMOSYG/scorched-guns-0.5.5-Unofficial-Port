"""The maid compat's config screens must actually write their file.

The player changed options in the screen TLM's own Cloth Config UI shows, pressed save, and the values
came back as the old ones after a restart. The reason is that adding entries is only half of a working
screen:

  * Cloth calls a `setSaveConsumer` when the screen saves, and every consumer here is
    `SCG2TLMConfig.X.set(value)`.
  * `ModConfigSpec$ConfigValue#set` only writes the in-process NightConfig value and the cache
    (`loadedConfig.config().set(path, value)`, then return) - it never touches the file.
  * The file is written only by `ModConfigSpec#save`, which the standalone screen wired up through
    `setSavingRunnable(SPEC::save)` - and the copy of the entries injected into TLM's screen never did.

So the injected options worked for the session and were overwritten by the file's old contents on the
next launch. The listener now chains TLM's runnable (read back with `getSavingRunnable`, because
`setSavingRunnable` replaces) and saves this compat's spec after it; replacing it outright would have
fixed one screen by breaking TLM's own config.

The same round restored the compat's own screen: `SCG2TLMClothConfig.createScreen` existed but was never
registered, because the port dropped Forge's `ConfigScreenHandler.ConfigScreenFactory` and never
replaced it with NeoForge's `IConfigScreenFactory`.

usage: python tools/audit_maid_config_save.py
"""
from __future__ import annotations

import os
import re
import sys

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
COMPAT = os.path.join(ROOT, "maid-compat", "src", "main", "java")
LISTENER = os.path.join(COMPAT, "com", "scg2tlm", "elmomod", "client", "SCG2TLMClothConfigListener.java")
SCREEN = os.path.join(COMPAT, "com", "scg2tlm", "elmomod", "client", "SCG2TLMClothConfig.java")
ENTRY = os.path.join(COMPAT, "com", "scg2tlm", "elmomod", "ExampleMod.java")


def strip_comments(src: str) -> str:
    """Drop // and /* */ comments, keeping string literals intact.

    The checks below have to look at code. The first version searched the raw text, and the class
    docstring - which explains that the runnable is read back with getSavingRunnable - was enough to
    satisfy the rule: removing the actual call went unnoticed, and the control caught it.
    """
    out, i, n = [], 0, len(src)
    while i < n:
        c = src[i]
        if c == '"':
            out.append(c)
            i += 1
            while i < n and src[i] != '"':
                if src[i] == "\\":
                    out.append(src[i])
                    i += 1
                if i < n:
                    out.append(src[i])
                    i += 1
            if i < n:
                out.append('"')
                i += 1
        elif src.startswith("//", i):
            while i < n and src[i] != "\n":
                i += 1
        elif src.startswith("/*", i):
            i += 2
            while i < n and not src.startswith("*/", i):
                i += 1
            i += 2
        else:
            out.append(c)
            i += 1
    return "".join(out)


def read(path: str) -> str:
    try:
        return strip_comments(open(path, encoding="utf-8", errors="replace").read())
    except OSError:
        return ""


def main() -> int:
    listener = read(LISTENER)
    screen = read(SCREEN)
    entry = read(ENTRY)
    problems = []

    if not listener:
        problems.append("the TLM config listener is gone, so this audit is not measuring anything")
    else:
        if "SCG2TLMConfig.SPEC.save()" not in listener:
            problems.append("the listener that injects entries into TLM's screen never saves this "
                            "compat's spec, so options changed there are lost on restart (this is the "
                            "bug the player reported)")
        if "getSavingRunnable" not in listener:
            problems.append("the listener does not read TLM's saving runnable back, so setting ours "
                            "would replace it and TLM's own config would stop saving instead")

    if not screen:
        problems.append("the compat's own config screen class is missing")
    else:
        if not re.search(r"setSavingRunnable\(\s*SCG2TLMConfig\.SPEC::save\s*\)", screen):
            problems.append("the compat's own screen does not save the spec when it is saved")

    if "registerExtensionPoint(IConfigScreenFactory.class" not in entry:
        problems.append("the compat's own screen is never registered - NeoForge needs "
                        "modContainer.registerExtensionPoint(IConfigScreenFactory.class, ...), which "
                        "replaced Forge's ConfigScreenHandler.ConfigScreenFactory")
    elif "SCG2TLMClothConfig.createScreen" not in entry:
        problems.append("a config screen is registered but it is not the compat's screen")

    for problem in problems:
        print("BROKEN %s" % problem)
    if problems:
        print("\n%d problem(s)" % len(problems))
        return 1
    print("0 problem(s): the maid compat's options save from TLM's screen and from its own screen")
    return 0


if __name__ == "__main__":
    sys.exit(main())
