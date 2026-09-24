"""Report, for every gun item, whether it can be drawn in third person.

`GunRenderingHandler.renderGun` deliberately draws NOTHING for an
`AnimatedGunItem` in third person, because GeckoLib's own item renderer is
supposed to draw it.  But `ItemInHandLayerMixin` *cancels* vanilla's item layer
for a player holding a gun and routes the draw through `renderWeapon`, so for a
player the gun is only drawn when `ModelOverrides` has an entry for it (that
branch calls the override model and returns).

Any animated gun without a `ModelOverrides` entry is therefore invisible when
held by a player in third person.  This lists those guns.

Usage:
    python tools/audit_gun_render_paths.py
"""

import pathlib
import re
from collections import Counter

ITEMS = pathlib.Path("src/main/java/top/ribs/scguns/init/ModItems.java")
CLIENT = pathlib.Path("src/main/java/top/ribs/scguns/client/ClientHandler.java")
SRC_ROOT = pathlib.Path("src/main/java")

# One `CONST = REGISTER.register("id", () -> ...);` statement, lazily matched.
REGISTER = re.compile(
    r"public static final Deferred\w*Holder<[^;]*?>\s+(\w+)\s*=\s*REGISTER\.register\((.*?)\n\s*\);",
    re.S,
)
CONSTRUCTED = re.compile(r"new\s+([A-Z][A-Za-z0-9_]*)\s*\(")
EXTENDS = re.compile(r"\bclass\s+\w+[^{]*?\bextends\s+([A-Za-z0-9_.]+)")


def ancestry(simple):
    """Walk `extends` clauses through our own sources, starting at *simple*.

    Substring matching on the registration body is not enough: the port has
    `AnimatedAirGunItem`, `AnimatedSculkGunItem`, `AnimatedDualWieldGunItem` and
    friends, none of which contain the literal text "AnimatedGunItem" even
    though they all descend from it.
    """
    chain = []
    seen = set()
    current = simple
    while current and current not in seen:
        seen.add(current)
        chain.append(current)
        found = None
        for path in SRC_ROOT.rglob(current + ".java"):
            match = EXTENDS.search(path.read_text(encoding="utf-8", errors="replace"))
            if match:
                found = match.group(1).split(".")[-1]
            break
        current = found
    return chain


def main():
    items_src = ITEMS.read_text(encoding="utf-8")
    client_src = CLIENT.read_text(encoding="utf-8")

    overrides = set(re.findall(r"ModelOverrides\.register\(\(Item\)\s*ModItems\.(\w+)", client_src))

    guns = {}
    for const, body in REGISTER.findall(items_src):
        match = CONSTRUCTED.search(body)
        if not match:
            continue
        cls = match.group(1)
        chain = ancestry(cls)
        if "AnimatedGunItem" in chain:
            guns[const] = ("animated", cls)
        elif "GunItem" in chain:
            guns[const] = ("plain", cls)

    counts = Counter(kind for kind, _cls in guns.values())
    print("gun items registered: %d (%s)" % (len(guns), dict(counts)))
    print("ModelOverrides entries targeting gun constants: %d"
          % len([c for c in overrides if c in guns]))
    print("")

    invisible = sorted(c for c, (kind, _cls) in guns.items() if kind == "animated" and c not in overrides)
    plain_no_override = sorted(c for c, (kind, _cls) in guns.items() if kind == "plain" and c not in overrides)

    print("=== animated guns WITHOUT a ModelOverride (%d) ===" % len(invisible))
    print("    -> GeckoLib's renderer is their BEWLR, so third person is fine; but")
    print("       for a PLAYER the mixin cancels vanilla and routes through")
    print("       renderWeapon, where renderGun returns early for animated guns in")
    print("       third person without drawing anything.")
    for c in invisible:
        print("       %s" % c)

    print("")
    print("=== non-animated GunItems (%d) ===" % len(plain_no_override) if not plain_no_override
          else "=== non-animated GunItems WITHOUT a ModelOverride (%d) ===" % len(plain_no_override))
    print("    -> these use GunItemStackRenderer as their BEWLR, whose")
    print("       renderByItem(NONE) re-enters renderWeapon(NONE) -> renderGun(NONE)")
    print("       -> ItemRenderer.render(NONE): first person would recurse forever.")
    for c in plain_no_override:
        print("       %s" % c)
    if not plain_no_override:
        print("       (none: every registered gun is an Animated*GunItem, so the")
        print("        BEWLR is GeckoLib's and the NONE fallback terminates)")

    print("")
    print("animated guns with an override: %d"
          % len([c for c, (k, _x) in guns.items() if k == "animated" and c in overrides]))


if __name__ == "__main__":
    main()
