"""Tripwire for "which gun is in the player's hands?" in AnimatedGunItem.

GeckoLib 4.6 keeps the per-stack animatable id in a data component
(`GeckoLibConstants.STACK_ANIMATABLE_ID_COMPONENT`) and `GeoItem.getId(stack)` falls back to
`Long.MAX_VALUE` when it is missing. The port wrote the legacy `"GeckoLibID"` NBT key instead,
which GeckoLib 4.9.3 does not read at all (0 references in the jar), so every gun in the game
reported the same id.

Two things followed from that, both reported by the player as "the other guns in my backpack
really reload while I reload one":

  * `handlePlayerSpecificLogic` picked its branch with `GeoItem.getId(mainHand) != id`, which was
    therefore false for EVERY gun, so the entire held-gun state machine (which keys off the
    player-level `ModSyncedDataKeys.RELOADING`) ran for every gun in the inventory and triggered
    `reload`/`carbine_reload` on all of them;
  * all stacks of one gun type shared a single AnimationController, and a triggered animation
    bypasses the controller's `predicate` (verified in `AnimationController.handleAnimationState`),
    so those guns then played the reload animation wherever they were drawn.

The held-vs-stored decision must come from the slot the stack was ticked from, and the id must be
assigned through GeckoLib's own API. This audit fails when either regresses.

Usage:
    python tools/audit_gun_animation_identity.py
    python tools/audit_gun_animation_identity.py --selftest   # must FAIL against the pre-fix file
"""

import pathlib
import re
import subprocess
import sys

SOURCE = pathlib.Path("src/main/java/top/ribs/scguns/item/animated/AnimatedGunItem.java")
RELATIVE = "src/main/java/top/ribs/scguns/item/animated/AnimatedGunItem.java"
PREFIX = "top/ribs/scguns/"

# GeckoLib 4.6's own accessor, which assigns the component when it is missing.
ASSIGN = "GeoItem.getOrAssignId("
# The 0.5.5-era key, unread by GeckoLib 4.9.3.
DEAD_KEY = re.compile(r'putLong\(\s*"GeckoLibID"|AnimatableIdCache\.getFreeId')
# Any id based "is this the held stack?" test.
ID_IDENTITY = re.compile(r'GeoItem\.getId\([^)]*\)\s*!=|!=\s*id\b')
# Anything outside AnimatedGunItem that still keys off the dead NBT name.
DEAD_KEY_ANYWHERE = re.compile(r'"GeckoLibID"')


def method_body(text, name):
    """Body of the (first) method called `name`, or "" when it is not there."""
    match = re.search(r'\b' + re.escape(name) + r'\s*\([^)]*\)\s*\{', text, re.S)
    if not match:
        return ""
    depth, i = 0, match.end() - 1
    while i < len(text):
        if text[i] == '{':
            depth += 1
        elif text[i] == '}':
            depth -= 1
            if depth == 0:
                return text[match.end():i]
        i += 1
    return ""


def check(text):
    """-> list of problem descriptions (empty when the identity handling looks right)."""
    problems = []

    if ASSIGN not in text:
        problems.append("no GeoItem.getOrAssignId: guns never get a GeckoLib 4.6 animatable id, "
                        "so every stack reports Long.MAX_VALUE and shares one AnimationController")
    if DEAD_KEY.search(text):
        problems.append('still writes the "GeckoLibID" NBT key / uses AnimatableIdCache.getFreeId, '
                        "which GeckoLib 4.9.3 does not read")

    inventory_tick = method_body(text, "inventoryTick")
    if "boolean inHands" not in inventory_tick:
        problems.append("inventoryTick does not compute the held state from the ticked slot")
    elif "selected ||" not in inventory_tick:
        problems.append("inventoryTick does not use the slot/selected flag for the held state")

    specific = method_body(text, "handlePlayerSpecificLogic")
    if "!inHands" not in specific:
        problems.append("handlePlayerSpecificLogic does not branch on the in-hands flag, so the "
                        "held-gun state machine runs for every gun in the inventory")
    if ID_IDENTITY.search(specific):
        problems.append("handlePlayerSpecificLogic still decides held-ness with GeoItem ids")

    sync = method_body(text, "handleReloadStateSynchronization")
    if "!inHands" not in sync:
        problems.append("handleReloadStateSynchronization is not scoped to the held gun, so a gun "
                        "in the inventory can mirror the player-level RELOADING flag")

    not_held = method_body(text, "handleItemNotHeld")
    if "justLeftHands" not in not_held:
        problems.append("handleItemNotHeld resets the controller on every tick, which cancels the "
                        "animation of a gun of the same type that is still in hand")

    return problems


def scan_tree(root=pathlib.Path("src/main/java")):
    """Other files that still lean on the dead GeckoLib id key."""
    problems = []
    for path in sorted(root.rglob("*.java")):
        if path.name == SOURCE.name:
            continue
        if DEAD_KEY_ANYWHERE.search(path.read_text(encoding="utf-8", errors="ignore")):
            problems.append("%s still references the dead GeckoLib id key" % path)
    return problems


def selftest():
    """The pre-fix file (git HEAD) must fail every check that guards this bug."""
    try:
        before = subprocess.run(["git", "show", "HEAD:%s" % RELATIVE],
                                capture_output=True, text=True, check=True).stdout
    except (subprocess.CalledProcessError, FileNotFoundError) as error:
        print("selftest: cannot read HEAD:%s (%s)" % (RELATIVE, error))
        return 1

    expected = [
        "no GeoItem.getOrAssignId",
        'still writes the "GeckoLibID"',
        "handlePlayerSpecificLogic does not branch on the in-hands flag",
        "handlePlayerSpecificLogic still decides held-ness with GeoItem ids",
        "handleReloadStateSynchronization is not scoped to the held gun",
        "handleItemNotHeld resets the controller on every tick",
    ]
    found = check(before)
    missing = [e for e in expected if not any(e in f for f in found)]
    print("selftest: pre-fix source reports %d problem(s)" % len(found))
    for problem in found:
        print("   %s" % problem)
    if missing:
        for m in missing:
            print("   MISSING expected problem: %s" % m)
        print("selftest FAILED: the audit does not catch the bug it exists for")
        return 1
    print("selftest OK: every check fires on the pre-fix source")
    return 0


def main():
    if "--selftest" in sys.argv:
        return selftest()

    if not SOURCE.exists():
        print("FAIL: %s is missing" % SOURCE)
        return 1

    problems = check(SOURCE.read_text(encoding="utf-8"))
    problems += scan_tree()
    for problem in problems:
        print("  %s" % problem)
    if problems:
        print("%d problem(s) with the gun animation identity handling "
              "(guns in the inventory will animate with the held one)" % len(problems))
        return 1
    print("0 problem(s): held state comes from the slot, ids are assigned through GeckoLib")
    return 0


if __name__ == "__main__":
    sys.exit(main())
