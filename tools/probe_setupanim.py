"""Print the exact descriptors of every `setupAnim` in the model classes the
arm-pose mixins inject into.

A mixin inject whose descriptor does not match silently fails to apply, which
is exactly how "mobs have no gun-holding pose" can happen without any error the
port would notice.  javap prints the generic form, so read the raw descriptors.

Usage:
    python tools/probe_setupanim.py
"""

import pathlib
import re
import zipfile

JAR = pathlib.Path("build/moddev/artifacts/neoforge-21.1.249-merged.jar")

CLASSES = [
    "net/minecraft/client/model/HumanoidModel",
    "net/minecraft/client/model/IllagerModel",
    "net/minecraft/client/model/AbstractZombieModel",
    "net/minecraft/client/model/PlayerModel",
    "net/minecraft/client/model/EntityModel",
]

# Our mixin targets, as written in source.
TARGETS = {
    "net/minecraft/client/model/HumanoidModel":
        "(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",
    "net/minecraft/client/model/IllagerModel":
        "(Lnet/minecraft/world/entity/monster/AbstractIllager;FFFFF)V",
    "net/minecraft/client/model/AbstractZombieModel":
        "(Lnet/minecraft/world/entity/monster/Monster;FFFFF)V",
}

SETUP = re.compile(rb"\(([^()]*)\)V")
# A JVM descriptor made only of the characters legal in one.  Searching for the
# shape is necessary because constant-pool UTF8 entries sit back to back, so
# splitting the file into printable runs merges neighbouring entries and the
# "ends with )V" check then never matches.
DESCRIPTOR = re.compile(rb"\([A-Za-z0-9/;\[\]$()]*\)[A-Za-z0-9/;\[\]]+")


def candidate_descriptors(data: bytes):
    """All descriptor-shaped strings in the class file."""
    return {m.group(0).decode("latin-1") for m in DESCRIPTOR.finditer(data)}


def main():
    with zipfile.ZipFile(JAR) as zf:
        for name in CLASSES:
            entry = name + ".class"
            try:
                data = zf.read(entry)
            except KeyError:
                print("%s: not in jar" % entry)
                continue
            descs = candidate_descriptors(data)
            # Keep only entity-taking setups with five floats.
            setups = [d for d in descs
                      if d.endswith(")V") and d.count("F") == 5
                      and "Lnet/minecraft/world/entity" in d]
            print("=== %s ===" % name)
            for d in sorted(set(setups)):
                marker = ""
                if TARGETS.get(name) == d:
                    marker = "   <-- our mixin target: MATCHES"
                print("   %s%s" % (d, marker))
            if name in TARGETS and TARGETS[name] not in setups:
                print("   !!! our mixin target %s is NOT among them -> inject would FAIL"
                      % TARGETS[name])
            print("")


if __name__ == "__main__":
    main()
