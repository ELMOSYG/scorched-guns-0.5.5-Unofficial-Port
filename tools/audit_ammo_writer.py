"""The `AmmoCount` key may only be read and written through `util/GunAmmo`.

Phase 1 of docs/reload-system.md. Before the helper, the key was spelled out at about forty call
sites, each choosing its own way to reach the tag (`getTag`, `getTagForWrite`, `getOrCreateTag`,
`NbtHelper.setInt`). Three things went wrong because of that, and all three are invisible to javac:

  * reads wrote to the stack - `Gun.getAmmoCount` and `ReloadTracker.isWeaponFull` used
    `getOrCreateTag`, which copies the tag and re-installs the component on every call, including the
    per-frame render paths and the per-tick reload check, and orphans any tag the caller held;
  * a write only landed while the tag was still the one inside the component, so a helper called in
    between silently swallowed it (this is how the reload reported the pre-reload magazine);
  * "the magazine" had four different clamp rules in four code paths.

Rules checked here:

  1. the quoted key literal `"AmmoCount"` appears only in `util/GunAmmo.java`;
  2. `Gun.getAmmoCount` and `GunAttackGoal.getAmmoCount` delegate to `GunAmmo.get` (a read must not
     fetch a writable tag again);
  3. `GunAmmo.set` keeps the 0.5.5 `tag != null` guard, so a guarded write cannot start filling
     magazines the old code left alone (and charging the player's ammo for them).

usage:
    python tools/audit_ammo_writer.py
    python tools/audit_ammo_writer.py --selftest   # must FAIL against the pre-refactor tag
"""
from __future__ import annotations

import os
import re
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "src", "main", "java", "top", "ribs", "scguns")
HELPER = os.path.join(SRC, "util", "GunAmmo.java")
GUN = os.path.join(SRC, "common", "Gun.java")
GOAL = os.path.join(SRC, "entity", "ai", "GunAttackGoal.java")

# The tag right before this refactor; stable, unlike HEAD, which stops being "before" once committed
# (the same trap HANDOFF 76.3 records for the raid audit's selftest).
PRE_FIX_REVISION = "backup-reloadstate-20261115"
KEY_PATTERN = re.compile(r'"AmmoCount"')


def strip_comments(text: str) -> str:
    text = re.sub(r"/\*.*?\*/", lambda m: re.sub(r"[^\n]", " ", m.group(0)), text, flags=re.S)
    return re.sub(r"//[^\n]*", "", text)


def method_body(text: str, signature: str) -> str | None:
    """The body of the first method whose header starts with `signature`, by bracket counting."""
    start = text.find(signature)
    if start < 0:
        return None
    brace = text.find("{", start)
    if brace < 0:
        return None
    depth = 0
    for index in range(brace, len(text)):
        if text[index] == "{":
            depth += 1
        elif text[index] == "}":
            depth -= 1
            if depth == 0:
                return text[brace + 1:index]
    return None


def key_literal_sites(sources: list[tuple[str, str]]) -> list[str]:
    """Rule 1: which files (other than the helper) still name the key."""
    problems = []
    for name, raw in sources:
        if os.path.basename(name) == "GunAmmo.java":
            continue
        text = strip_comments(raw)
        for number, line in enumerate(text.split("\n"), 1):
            if KEY_PATTERN.search(line):
                problems.append("%s:%d names the AmmoCount key outside GunAmmo" % (name, number))
    return problems


def delegation_problems(sources: dict[str, str]) -> list[str]:
    """Rule 2: the two read accessors must delegate, not reach for a writable tag."""
    problems = []
    for label, body in (
        ("Gun.getAmmoCount", method_body(sources.get("Gun.java", ""), "public static int getAmmoCount(ItemStack")),
        ("GunAttackGoal.getAmmoCount", method_body(sources.get("GunAttackGoal.java", ""), "public static int getAmmoCount(ItemStack")),
    ):
        if body is None:
            problems.append("%s is gone; has the ammo read path moved?" % label)
            continue
        if "GunAmmo.get" not in body:
            problems.append("%s does not delegate to GunAmmo.get, so a read can fetch a writable tag "
                            "again (one component copy per tick/frame, and it orphans the caller's tag)" % label)
        if "getOrCreateTag" in body or "getTagForWrite" in body:
            problems.append("%s passes through a tag-fetching helper; reads must stay read-only" % label)
    return problems


def helper_problems(sources: dict[str, str]) -> list[str]:
    """Rule 3: the helper itself must keep the guard and write back through one routine."""
    problems = []
    text = strip_comments(sources.get("GunAmmo.java", ""))
    if not text:
        return ["util/GunAmmo.java is missing - the ammo key has no single owner"]

    guarded = method_body(text, "public static boolean set(ItemStack")
    if guarded is None:
        problems.append("GunAmmo.set is gone")
    else:
        if "hasCustomData" not in guarded:
            problems.append("GunAmmo.set no longer refuses to create the tag: every 0.5.5 write site "
                            "was guarded on `tag != null`, and an unguarded write would fill (and "
                            "charge ammo for) magazines the old code left alone")
        if "NbtHelper.setInt" not in guarded:
            problems.append("GunAmmo.set does not write the copy back through NbtHelper.setInt, so the "
                            "write can land in an orphaned tag again")

    creating = method_body(text, "public static void setOrCreate(ItemStack")
    if creating is None:
        problems.append("GunAmmo.setOrCreate is gone")
    elif "NbtHelper.setInt" not in creating:
        problems.append("GunAmmo.setOrCreate does not go through NbtHelper.setInt")

    if "public static final String KEY" not in text:
        problems.append("GunAmmo no longer declares the KEY constant")
    return problems


def scan(sources: list[tuple[str, str]]) -> list[str]:
    # Comments are stripped for every rule: this repo documents the old behaviour in place, so a
    # check that reads comments fails on the very explanation of the bug it guards against (the
    # `audit_time_delta.py` lesson - "strip comments first, or the check passes on its own javadoc").
    by_name = {os.path.basename(name): strip_comments(raw) for name, raw in sources}
    return key_literal_sites(sources) + delegation_problems(by_name) + helper_problems(by_name)


def read_tree() -> list[tuple[str, str]]:
    sources = []
    for base, _dirs, files in os.walk(SRC):
        for name in sorted(files):
            if name.endswith(".java"):
                path = os.path.join(base, name)
                sources.append((os.path.relpath(path, SRC), open(path, encoding="utf-8", errors="replace").read()))
    return sources


def selftest() -> int:
    """The pre-refactor tag must be reported: the literal was everywhere and the read created tags."""
    wanted = {
        "ReloadTracker.java": "src/main/java/top/ribs/scguns/common/ReloadTracker.java",
        "Gun.java": "src/main/java/top/ribs/scguns/common/Gun.java",
        "GunAttackGoal.java": "src/main/java/top/ribs/scguns/entity/ai/GunAttackGoal.java",
    }
    sources = []
    for name, relative in wanted.items():
        try:
            text = subprocess.run(
                ["git", "show", "%s:%s" % (PRE_FIX_REVISION, relative)],
                capture_output=True, text=True, encoding="utf-8", check=True,
            ).stdout
        except (subprocess.CalledProcessError, FileNotFoundError) as error:
            print("selftest: cannot read %s at %s (%s)" % (relative, PRE_FIX_REVISION, error))
            return 1
        sources.append((name, text))

    problems = scan(sources)
    found_literal = [p for p in problems if "names the AmmoCount key" in p]
    found_delegation = [p for p in problems if "does not delegate" in p]
    if not found_literal:
        print("selftest FAILED: the AmmoCount literal in the pre-refactor sources is not detected")
        return 1
    if not found_delegation:
        print("selftest FAILED: the read-through-getOrCreateTag in the pre-refactor sources is not detected")
        return 1

    for problem in found_literal[:5]:
        print("  " + problem)
    print("  " + found_delegation[0])
    print("selftest OK: %d literal site(s) and %d non-delegating read(s) detected at %s"
          % (len(found_literal), len(found_delegation), PRE_FIX_REVISION))
    return 0


def main() -> int:
    if "--selftest" in sys.argv:
        return selftest()

    sources = read_tree()
    problems = scan(sources)
    users = sorted(name for name, raw in sources if "GunAmmo." in strip_comments(raw) and name != "util\\GunAmmo.java")
    print("AmmoCount key owners: util/GunAmmo.java only; %d file(s) call it" % len(users))
    for problem in problems:
        print("BROKEN %s" % problem)
    if problems:
        print("\n%d problem(s)" % len(problems))
        return 1
    print("0 problem(s): the magazine has one owner, reads never fetch a writable tag, and writes keep "
          "the 0.5.5 no-tag guard")
    return 0


if __name__ == "__main__":
    sys.exit(main())
