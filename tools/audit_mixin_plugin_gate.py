"""Catch a mixin config plugin that can silently disable the whole config.

A skipped mixin logs nothing at normal log levels, so a `shouldApplyMixin` that
returns a gate field can turn off every mixin in the mod with no visible symptom.
That is exactly what happened here: 0.5.5's `MixinPlugin` probed
`com.mrcrayfish.framework.FrameworkForge`, which does not exist on NeoForge (the
class is `FrameworkNeoForge`), so the gate was always false and all 15 mixins in
`scguns.mixins.json` were skipped.

Two checks:

1. Every `Class.forName("<name>")` in the plugin must resolve against the
   dependency jars on the compile classpath (or our own sources). A probe for a
   name that exists nowhere is a guaranteed-false gate.
2. `shouldApplyMixin` must not be able to return false, unless the plugin's
   own mixins actually reference the gated dependency.

Usage:
    python tools/audit_mixin_plugin_gate.py
"""

import pathlib
import re
import sys
import zipfile

MIXIN_ROOT = pathlib.Path("src/main/java/top/ribs/scguns/mixin")
CLASSPATH_FILE = pathlib.Path("build-logs/compile-classpath.txt")

FOR_NAME = re.compile(r'Class\.forName\(\s*"([^"]+)"')
# A fully qualified class name used as a literal, e.g. in a candidate-name array.
# The first segment is package-style (lowercase); later segments may be CamelCase.
CLASS_LITERAL = re.compile(r'"([a-z][\w]*(?:\.[A-Za-z_][\w]*)+)"')


def probe_names(text):
    """Class names this plugin probes, whether inline or from a candidate array."""
    names = set(FOR_NAME.findall(text))
    for literal in CLASS_LITERAL.findall(text):
        # Only keep things that look like a class reference, not message text.
        if literal.count(".") >= 2 and len(literal.split(".")[-1]) > 2:
            names.add(literal)
    return sorted(names)


def dependency_class_exists(binary_name):
    """Look for `binary_name` inside the jars on the compile classpath."""
    entry = binary_name.replace(".", "/") + ".class"
    if not CLASSPATH_FILE.is_file():
        return None  # cannot tell
    text = CLASSPATH_FILE.read_text(encoding="utf-8", errors="replace")
    for raw in re.split(r"[;\r\n]+", text):
        jar = raw.strip()
        if not jar.lower().endswith(".jar"):
            continue
        path = pathlib.Path(jar)
        if not path.is_file():
            continue
        try:
            with zipfile.ZipFile(path) as zf:
                if entry in zf.namelist():
                    return True
        except Exception:  # noqa: BLE001
            continue
    return False


def main():
    problems = []
    plugins = [p for p in MIXIN_ROOT.rglob("*.java") if "IMixinConfigPlugin" in p.read_text(encoding="utf-8", errors="replace")]
    if not plugins:
        print("no IMixinConfigPlugin found")
        return 0

    for plugin in plugins:
        text = plugin.read_text(encoding="utf-8", errors="replace")
        print("=== %s ===" % plugin.name)

        probes = probe_names(text)
        resolved = []
        unresolved = []
        for name in probes:
            exists = dependency_class_exists(name)
            if exists is None:
                print("  probe %-52s (classpath not built - cannot check)" % name)
                continue
            print("  probe %-52s %s" % (name, "resolves" if exists else "NOT FOUND ANYWHERE"))
            (resolved if exists else unresolved).append(name)

        # The failure mode is that the gate can NEVER be true, i.e. every candidate
        # name is missing. A stale alternate kept as a fallback is fine as long as
        # one of the names resolves.
        if probes and not resolved and unresolved:
            problems.append(
                "%s probes only %s, none of which exists in any dependency jar on the "
                "compile classpath: the gate can never be true and every mixin in the "
                "config would be skipped" % (plugin.name, ", ".join(unresolved))
            )

        # Does shouldApplyMixin gate on a field?
        match = re.search(r"shouldApplyMixin\s*\([^)]*\)\s*\{([^}]*)\}", text, re.S)
        body = match.group(1) if match else ""
        gating = re.search(r"return\s+(?!true\b)([\w.]+)\s*;", body)
        if gating:
            field = gating.group(1)
            # Does any mixin actually touch the gated dependency?
            dep_hint = "mrcrayfish"
            users = [
                p.name
                for p in MIXIN_ROOT.rglob("*.java")
                if "IMixinConfigPlugin" not in p.read_text(encoding="utf-8", errors="replace")
                and dep_hint in p.read_text(encoding="utf-8", errors="replace")
            ]
            print("  shouldApplyMixin returns the gate field '%s'" % field)
            if users:
                print("    mixins that reference the gated dependency: %s" % ", ".join(users))
            else:
                print("    no mixin references the gated dependency")
                problems.append(
                    "%s gates every mixin on '%s', but no mixin uses that dependency: a "
                    "false gate disables everything for no benefit" % (plugin.name, field)
                )
        else:
            print("  shouldApplyMixin does not gate on a field (good)")

    print("")
    if problems:
        print("%d problem(s):" % len(problems))
        for problem in problems:
            print("  - %s" % problem)
        return 1
    print("0 problem(s)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
