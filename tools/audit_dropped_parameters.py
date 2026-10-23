#!/usr/bin/env python3
"""Find methods that drop trailing parameters a supertype method expects.

Why this exists
---------------
`audit_override_drift.py` compares our methods against every supertype method with the same
name. It reports a genuine signature drift when the parameter *types* differ at the same
position, and it reports the Forge-to-NeoForge shape where **our** parameter list is the
supertype's plus extra trailing parameters, because that is what happened when a callback
lost its last argument.

It deliberately ignores the other direction - ours has *fewer* parameters than a supertype
method of the same name - on the grounds that "a different-arity pairing is usually a plain
overload". That is true most of the time and it is also how a real bug hid for months:

    1.20.1  Screen#mouseScrolled(double mouseX, double mouseY, double scroll)
    1.21    Screen#mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)

The port kept the three-argument version. It is a valid overload, it compiles, `@Override` is
absent so nothing complains - and it is never called, so the wheel did nothing in the gun
modification screen. The same shape cost us `Entity#readAdditionalSaveData` and, in the
container save path, an API whose return value carried the whole payload.

So: ours is a *prefix* of a supertype method of the same name, and there is no supertype
method with our exact arity. That is reported here.

Usage
-----
    python tools/audit_dropped_parameters.py             # audit
    python tools/audit_dropped_parameters.py --verbose   # plus every pairing considered

Exits 1 when anything is reported, 0 when clean.

Requires compiled classes: run the build first. This tool does not invoke gradle.
"""

from __future__ import annotations

import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import audit_override_drift as drift  # noqa: E402  (same directory, reused parser/classpath)

# Names that are never overrides in the sense we care about.
SKIP_METHODS = {"<init>", "<clinit>"}

# Class#method pairs where the name collides with a supertype method that has nothing to do with it.
# Each one was read: they are plain getters that happen to share a name with an extension hook.
WHITELIST = {
    "top/ribs/scguns/blockentity/VentCollectorBlockEntity#getData":
        "returns this machine's ContainerData; the supertype method is attachment lookup, unrelated",
    "top/ribs/scguns/item/BayonetItem#getDamage":
        "the bayonet's own melee damage getter; the supertype hook is item-stack damage, unrelated",
}


def own_classes() -> list[str]:
    """Binary names of our own compiled classes."""
    out = []
    classes_dir = drift.CLASSES

    for dirpath, _, files in os.walk(classes_dir):
        for name in files:
            if not name.endswith(".class"):
                continue
            path = os.path.join(dirpath, name)
            rel = os.path.relpath(path, classes_dir).replace(os.sep, "/")

            if rel.startswith(drift.OWN_PREFIX):
                out.append(rel[: -len(".class")])

    return sorted(out)


def supertype_methods(cp, binary_name: str) -> dict[str, list[tuple[str, object]]]:
    """Method name -> [(owner binary name, Method)] for the whole supertype closure."""
    owners: dict[str, list[tuple[str, object]]] = {}

    for sup in drift.supertype_closure(cp, binary_name):
        info = cp.reify(sup)

        if info is None:
            continue

        for method in info.methods:
            if method.name in SKIP_METHODS:
                continue
            owners.setdefault(method.name, []).append((sup, method))

    return owners


def supers_for(owners, name):
    return owners.get(name) or []


def audit(verbose: bool) -> list[tuple[str, str, str, str, str]]:
    """Return (class, method, our desc, owner, their desc) for every dropped-parameter case."""
    cp = drift.build_classpath()
    findings = []
    whitelisted: list[str] = []
    considered = 0

    for binary in own_classes():
        info = cp.reify(binary)

        if info is None:
            continue

        owners = supertype_methods(cp, binary)

        if not owners:
            continue

        own_methods = drift.own_instance_methods(info)
        own_by_name: dict[str, list[object]] = {}

        for method in own_methods:
            own_by_name.setdefault(method.name, []).append(method)

        for method in own_methods:
            if method.name in SKIP_METHODS:
                continue

            # A class may legitimately declare a short convenience overload next to the real
            # override - TurretBlockEntity forwards its one-argument handleUpdateTag into the
            # two-argument one, for example.  Only report when nothing in this class matches the
            # supertype exactly, because then the callback really is missing.
            if any(s.desc in {o.desc for o in own_by_name.get(method.name, [])}
                   for _, s in supers_for(owners, method.name)):
                continue

            supers = owners.get(method.name)

            if not supers:
                continue

            considered += 1

            if any(s.desc == method.desc for _, s in supers):
                continue  # exact override somewhere above

            if any(len(s.params) == len(method.params) for _, s in supers):
                continue  # same arity: that is the drift audit's business

            # ours must be a prefix of theirs, and strictly shorter
            longer = [
                (owner, s)
                for owner, s in supers
                if len(s.params) > len(method.params)
                and s.params[: len(method.params)] == method.params
            ]

            if not longer:
                continue

            key = "%s#%s" % (binary, method.name)

            if key in WHITELIST:
                whitelisted.append(key)
                continue

            owner, theirs = min(longer, key=lambda pair: len(pair[1].params))
            findings.append((binary, method.name, method.desc, owner, theirs.desc))

            if verbose:
                print("  candidate %s#%s  ours=%s  theirs=%s" % (binary, method.name, method.desc, theirs.desc))

    print("considered %d same-name method(s) across %d class(es)" % (considered, len(own_classes())))

    if whitelisted:
        print("whitelisted as unrelated name collisions: %d" % len(whitelisted))

        if verbose:
            for key in whitelisted:
                print("  %s - %s" % (key, WHITELIST[key]))

    return findings


def main() -> None:
    if not os.path.isdir(drift.CLASSES):
        sys.exit(
            "no compiled classes at %s - build the project first (this tool does not run gradle)"
            % drift.CLASSES
        )

    findings = audit("--verbose" in sys.argv)

    if not findings:
        print("no method drops a trailing parameter that its supertype expects")
        return

    print()
    print("OURS HAS FEWER PARAMETERS THAN THE SUPERTYPE METHOD OF THE SAME NAME")
    print("(it compiles as an overload, is never called, and the feature silently does nothing)")
    print()

    for binary, method, desc, owner, owner_desc in findings:
        print("  %s#%s" % (binary.replace("/", "."), method))
        print("      ours  : %s" % desc)
        print("      super : %s.%s" % (owner.replace("/", "."), method))
        print("              %s" % owner_desc)
        print("      -> add the missing parameter, and put @Override on it so the next drift is a compile error")
        print()

    print("%d finding(s)" % len(findings))
    sys.exit(1)


if __name__ == "__main__":
    main()
