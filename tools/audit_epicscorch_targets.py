"""Check that the methods the Epic Fight compatibility layer injects into still exist.

`epicscorch` (Epic Fight x Scorched Guns) does not use a public API - it mixes into 25 of
this mod's classes and redirects specific *method calls* inside specific *method
signatures*. Renaming a parameter list or moving an event from `Post` to `Pre` is enough to
make one of its `@Inject`/`@Redirect` targets vanish, and the failure is fatal: the client
crashes on the loading screen with

    Mixin apply for mod epicscorch failed ... InvalidInjectionException:
    @Redirect annotation on epicscorch$... could not find any target

That happened once already: `ReloadHandler.onMouseInput` was moved to `MouseButton.Pre`
while the addon redirects a call inside the `Post` overload.

This audit pins the signatures the addon depends on. Run it after touching any of these
classes; a missing line means that addon will crash on load.

    python tools/audit_epicscorch_targets.py [--jar build/libs/scguns-<version>.jar]

The expected strings are `javap` output fragments, which is also what the check compares
against, so a change in parameter types shows up as a mismatch rather than a silent pass.
"""

from __future__ import annotations

import argparse
import glob
import pathlib
import re
import subprocess
import sys

# (class, javap fragment that must appear, why the addon needs it)
EXPECTED = [
    ("top.ribs.scguns.client.handler.AimingHandler",
     "onClientTick(net.neoforged.neoforge.client.event.ClientTickEvent$Pre)",
     "aim state is read before the tick"),
    ("top.ribs.scguns.client.handler.ReloadHandler",
     "onMouseInput(net.neoforged.neoforge.client.event.InputEvent$MouseButton$Post)",
     "@Redirect on KeyBinds.getAimMapping() inside onMouseInput"),
    ("top.ribs.scguns.client.handler.ReloadHandler",
     "onKeyPressed(net.neoforged.neoforge.client.event.InputEvent$Key)",
     "reload start is blocked while an Epic Fight skill runs"),
    ("top.ribs.scguns.client.handler.ReloadHandler",
     "onClientTick(net.neoforged.neoforge.client.event.ClientTickEvent$Post)",
     "critical-phase bookkeeping"),
    ("top.ribs.scguns.client.handler.ClientMeleeAttackHandler",
     "startMeleeAnimation",
     "bayonet melee is replaced by the Epic Fight animation"),
    ("top.ribs.scguns.client.handler.GunRenderingHandler",
     "onRenderOverlay",
     "HUD is suppressed while Epic Fight draws its own"),
    ("top.ribs.scguns.client.handler.MeleeAttackHandler",
     "startBanzai(net.minecraft.server.level.ServerPlayer",
     "banzai charge is redirected into an Epic Fight skill"),
    ("top.ribs.scguns.client.handler.MeleeAttackHandler",
     "performMeleeAttack(net.minecraft.server.level.ServerPlayer",
     "melee damage is routed through Epic Fight"),
    ("top.ribs.scguns.client.handler.ShootingHandler",
     "canFire",
     "firing is gated on the Epic Fight attack state"),
    ("top.ribs.scguns.client.handler.ShootingHandler",
     "fire",
     "firing is routed through Epic Fight"),
    ("top.ribs.scguns.common.ReloadTracker",
     "increaseAmmo",
     "ammo gain is mirrored into the Epic Fight HUD"),
    ("top.ribs.scguns.network.message.C2SMessageGunLoaded",
     "handle",
     "the reload acknowledgement is intercepted"),
]

DEFAULT_JAR = "build/libs/scguns-*.jar"


def newest_jar(pattern: str) -> pathlib.Path | None:
    matches = [pathlib.Path(p) for p in glob.glob(pattern)]
    return max(matches, key=lambda p: p.stat().st_mtime) if matches else None


def javap(jar: pathlib.Path, class_name: str, javap_bin: str) -> str:
    result = subprocess.run(
        [javap_bin, "-p", "-cp", str(jar), class_name],
        capture_output=True, text=True, check=False,
    )
    return result.stdout or ""


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jar", default=DEFAULT_JAR, help="built mod jar (glob allowed)")
    parser.add_argument("--javap", default="javap", help="javap executable")
    args = parser.parse_args(argv)

    jar = newest_jar(args.jar)
    if jar is None:
        print("no jar matched {}".format(args.jar))
        return 2

    cache: dict[str, str] = {}
    missing = 0
    for class_name, fragment, why in EXPECTED:
        if class_name not in cache:
            cache[class_name] = javap(jar, class_name, args.javap)
        dump = cache[class_name]
        short = class_name.rsplit(".", 1)[-1]

        if not dump.strip():
            print("MISSING CLASS  {}  ({})".format(short, why))
            missing += 1
            continue

        # javap prints one member per line; normalise whitespace before searching so a
        # reformat cannot hide a real signature change.
        flat = re.sub(r"\s+", " ", dump)
        if fragment in flat:
            print("ok             {}.{}".format(short, fragment.split("(")[0]))
        else:
            print("MISSING        {}.{}  ({})".format(short, fragment, why))
            missing += 1

    print("")
    if missing:
        print("{} signature(s) the Epic Fight compatibility layer injects into are gone; it will "
              "crash on load".format(missing))
    else:
        print("all {} signatures the Epic Fight compatibility layer needs are present".format(len(EXPECTED)))
    return 1 if missing else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
