"""Audit the mixins: target drift vs 0.5.5, and injection points that no longer exist.

A wrong `@Mixin` target or a renamed injection point crashes the *client* at startup,
which a dedicated-server smoke test cannot see. Two independent checks:

1. **Target drift** - compare `@Mixin(X.class)` against the 0.5.5 decompile. A target
   that changed during the port is a red flag on its own (e.g. `Item` -> `ItemStack`).
2. **Injection points** - resolve the target class in `.refs/nf-src` (the real 1.21.1
   sources) and confirm every `@Inject`/`@Redirect`/`@ModifyVariable`/`@ModifyArg`/
   `@WrapOperation`/`@Accessor`/`@Invoker` method name still exists there.

usage: python tools/audit_mixins.py
"""
from __future__ import annotations

import os
import re
import sys

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")
REFS = os.path.join(ROOT, ".refs", "nf-src")
ORIG = r"E:\mod\SG2-1.21\.sg055_deobf"

MIXIN = re.compile(r"@Mixin\s*\(\s*\{?\s*([\w.]+)\s*\.class")
INJECT = re.compile(r"@(Inject|Redirect|ModifyVariable|ModifyArg|ModifyArgs|WrapOperation|Accessor|Invoker)"
                    r"\s*\(([^)]*)\)", re.S)
METHOD_ATTR = re.compile(r'method\s*=\s*(\{[^}]*\}|"[^"]*")')
QUOTED = re.compile(r'"([^"]*)"')
IMPORT = re.compile(r"import\s+([\w.]+);")


def source_for(fqn: str) -> str | None:
    for base in (REFS,):
        path = os.path.join(base, fqn.replace(".", os.sep) + ".java")
        if os.path.isfile(path):
            return path
    return None


def declared_names(path: str) -> set[str]:
    text = open(path, encoding="utf-8", errors="replace").read()
    text = re.sub(r"//[^\n]*", " ", text)
    return set(re.findall(r"\b(\w+)\s*\(", text)) | set(re.findall(r"\b(\w+)\s*;", text))


def targets(text: str) -> list[str]:
    return MIXIN.findall(text)


# `Lowner/path/Class;member(descriptor)return` as used by @At(target = "...").
AT_TARGET = re.compile(r'target\s*=\s*"(L[\w/$]+;[^"]+)"')


def descriptor_param_names(descriptor: str) -> list[str] | None:
    """Simple type names of a JVM method descriptor's parameters."""
    if "(" not in descriptor or ")" not in descriptor:
        return None
    inner = descriptor[descriptor.index("(") + 1 : descriptor.index(")")]
    names = []
    index = 0
    while index < len(inner):
        char = inner[index]
        if char == "[":
            index += 1
            continue
        if char == "L":
            end = inner.index(";", index)
            names.append(inner[index + 1 : end].split("/")[-1])
            index = end + 1
        elif char in "ZBCSIJFD":
            names.append(
                {"Z": "boolean", "B": "byte", "C": "char", "S": "short",
                 "I": "int", "J": "long", "F": "float", "D": "double"}[char]
            )
            index += 1
        else:
            index += 1
    return names


def source_param_names(path: str, method: str) -> list[list[str]]:
    """For every declaration of *method* in *path*, the simple parameter type names."""
    text = open(path, encoding="utf-8", errors="replace").read()
    text = re.sub(r"//[^\n]*", " ", text)
    text = re.sub(r"/\*.*?\*/", " ", text, flags=re.S)
    found = []
    # A declaration, not a call site: `this.addLayer(...)` must not count, so the
    # character before the name may not be `.` (or part of a longer identifier).
    declaration = re.compile(r"(?<![\w.$])%s\s*\(" % re.escape(method))
    for match in declaration.finditer(text):
        depth = 0
        start = match.end() - 1
        index = start
        while index < len(text):
            if text[index] == "(":
                depth += 1
            elif text[index] == ")":
                depth -= 1
                if depth == 0:
                    break
            index += 1
        inner = text[start + 1 : index]
        # Split on top-level commas, then take the last identifier of each part.
        parts, depth, current = [], 0, ""
        for char in inner:
            if char in "<([":
                depth += 1
            elif char in ">)]":
                depth -= 1
            if char == "," and depth == 0:
                parts.append(current)
                current = ""
            else:
                current += char
        if current.strip():
            parts.append(current)
        names = []
        for part in parts:
            # Drop annotations, then generics, then modifiers, and the first
            # identifier left is the type. (Taking the second-to-last token would
            # mis-read `RenderLayer<T, M> layer` as `M`.)
            cleaned = re.sub(r"@\w+(?:\s*\([^)]*\))?", " ", part)
            cleaned = re.sub(r"<[^<>]*>", " ", cleaned)
            tokens = re.findall(r"[A-Za-z_][\w.]*", cleaned)
            tokens = [t for t in tokens if t not in
                      {"final", "public", "protected", "private", "static", "var"}]
            if tokens:
                names.append(tokens[0].split(".")[-1])
        found.append(names)
    return found


# The superclass of a class declaration. Regexes cannot do this reliably -- the
# type-parameter list may contain its own `extends` clauses and nested generics
# (`class MobRenderer<T extends Mob, M extends EntityModel<T>> extends ...`), so the
# header is scanned for its closing brace and the LAST `extends` in it is taken.
CLASS_HEAD = re.compile(r"\bclass\s+\w+")
EXTENDS_AT = re.compile(r"\bextends\s+([\w.]+)")


def superclass_of(text: str) -> str | None:
    match = CLASS_HEAD.search(text)
    if not match:
        return None
    index = match.end()
    depth = 0
    while index < len(text):
        char = text[index]
        if char in "<(":
            depth += 1
        elif char in ">)":
            depth -= 1
        elif char == "{" and depth <= 0:
            break
        index += 1
    header = text[match.end():index]
    found = EXTENDS_AT.findall(header)
    return found[-1].split(".")[-1] if found else None
IMPORT_LINE = re.compile(r"^import\s+([\w.]+);", re.M)


def resolve_type(fqn: str, simple: str) -> str | None:
    """Turn a simple class name into an FQN using the file's imports if possible."""
    path = source_for(fqn)
    if path is None:
        return None
    package = fqn.rsplit(".", 1)[0]
    for imported in IMPORT_LINE.findall(open(path, encoding="utf-8", errors="replace").read()):
        if imported.rsplit(".", 1)[-1] == simple:
            return imported
    candidate = package + "." + simple
    return candidate if source_for(candidate) else None


def declared_with_hierarchy(fqn: str, method: str, depth: int = 0):
    """Parameter lists for *method*, searching superclasses when not declared here.

    An `@At` INVOKE target may legitimately name an inherited method: javac emits
    the static receiver type as the owner, and Mixin resolves it up the hierarchy.
    """
    if depth > 8:
        return None
    path = source_for(fqn)
    if path is None:
        return None
    found = source_param_names(path, method)
    if found:
        return found
    text = open(path, encoding="utf-8", errors="replace").read()
    simple = superclass_of(text)
    if simple is None:
        return []
    parent = resolve_type(fqn, simple)
    if parent is None:
        return []
    return declared_with_hierarchy(parent, method, depth + 1)


def check_at_target(literal: str) -> str | None:
    """None when unresolvable, '' when fine, or the member name when stale."""
    owner, _, member = literal[1:].partition(";")
    if not member or "(" not in member:
        return None
    fqn = owner.replace("/", ".")
    if source_for(fqn) is None:
        return None
    name = member.split("(")[0]
    wanted = descriptor_param_names(member[len(name) :])
    if wanted is None:
        return None
    candidates = declared_with_hierarchy(fqn, name)
    if candidates is None:
        return None
    if not candidates:
        return name
    for params in candidates:
        if params == wanted:
            return ""
    return name


def check_callbacks(rel: str, text: str, target_fqn: str) -> int:
    """Verify each @Inject callback's parameters match its target method's.

    Mixin appends a CallbackInfo / CallbackInfoReturnable to the target's
    parameter list; anything else means the injection cannot be applied and, with
    defaultRequire = 1, the game fails to start.

    Only @Inject is checked. `@Redirect` / `@ModifyArg` / `@ModifyVariable`
    callbacks take the *redirected member's* arguments (or just the modified
    value), not the target method's, and a callback using
    `locals = LocalCapture...` additionally declares captured locals -- applying
    the @Inject rule to either produces false positives.
    """
    broken = 0
    for kind, args in INJECT.findall(text):
        if kind != "Inject":
            continue
        attr = METHOD_ATTR.search(args)
        if not attr:
            continue
        # LocalCapture callbacks declare CallbackInfo FIRST and then the captured
        # locals; a plain @Inject callback always has it LAST. Only the latter has
        # to match the target's parameter list.
        callback = re.search(
            r"\b(?:private|protected|public)\s+[\w<>\[\],.\s]+?\s(\w+)\s*\(([^;{]*?)\)\s*\{",
            text[text.index(args) + len(args):],
            re.S,
        )
        if not callback:
            continue
        declared = source_param_names_from_text(callback.group(2))
        if not declared or not declared[-1].startswith("CallbackInfo"):
            continue
        literals = QUOTED.findall(attr.group(1))
        if not literals:
            continue
        wanted_name = literals[0].split("(")[0].strip().rstrip("*").strip()
        wanted_desc = literals[0][len(literals[0].split("(")[0]):] if "(" in literals[0] else ""
        wanted = None
        if wanted_desc:
            wanted = descriptor_param_names(wanted_desc)
        params = declared
        if wanted is None:
            candidates = declared_with_hierarchy(target_fqn, wanted_name) or []
            matches = [p for p in candidates if p == params[:-1]]
            if candidates and not matches:
                broken += 1
                print("BROKEN %-57s @%s callback %s params %s do not match %s's %s"
                      % (rel, kind, callback.group(1), params, wanted_name, candidates))
            continue
        if params[:-1] != wanted:
            broken += 1
            print("BROKEN %-57s @%s callback %s params %s != target %s %s"
                  % (rel, kind, callback.group(1), params, wanted_name, wanted))
    return broken


def source_param_names_from_text(param_text: str):
    """Simple type names of a parameter list written as source text."""
    parts, depth, current = [], 0, ""
    for char in param_text:
        if char in "<([":
            depth += 1
        elif char in ">)]":
            depth -= 1
        if char == "," and depth == 0:
            parts.append(current)
            current = ""
        else:
            current += char
    if current.strip():
        parts.append(current)
    names = []
    for part in parts:
        cleaned = re.sub(r"@\w+(?:\s*\([^)]*\))?", " ", part)
        cleaned = re.sub(r"<[^<>]*>", " ", cleaned)
        tokens = [t for t in re.findall(r"[A-Za-z_][\w.]*", cleaned)
                  if t not in {"final", "var"}]
        if tokens:
            names.append(tokens[0].split(".")[-1])
    return names


def main() -> None:
    mixin_dir = os.path.join(SRC, "top", "ribs", "scguns", "mixin")
    drift = 0
    broken = 0
    unresolved = 0

    for d, _, fs in os.walk(mixin_dir):
        for f in sorted(fs):
            if not f.endswith(".java") or f == "MixinPlugin.java":
                continue
            path = os.path.join(d, f)
            text = open(path, encoding="utf-8", errors="replace").read()
            rel = os.path.relpath(path, SRC).replace("\\", "/")
            now = targets(text)

            orig_path = os.path.join(ORIG, rel.replace("/", os.sep))
            before = targets(open(orig_path, encoding="utf-8", errors="replace").read()) \
                if os.path.isfile(orig_path) else None
            if before is not None and sorted(before) != sorted(now):
                drift += 1
                print("DRIFT  %-58s 0.5.5=%s now=%s" % (rel, before, now))

            imports = {fqn.split(".")[-1]: fqn for fqn in IMPORT.findall(text)}
            for target in now:
                fqn = imports.get(target)
                if fqn is None:
                    continue
                src = source_for(fqn)
                if src is None:
                    unresolved += 1
                    print("UNRES %-58s target %s not in 1.21.1 vanilla sources"
                          % (rel, fqn))
                    continue
                names = declared_names(src)
                for kind, args in INJECT.findall(text):
                    attr = METHOD_ATTR.search(args)
                    if not attr:
                        continue
                    # only the `method =` attribute - `at = @At("HEAD")` is a point, not a name
                    for literal in QUOTED.findall(attr.group(1)):
                        # Mixin accepts a trailing `*` to match any descriptor
                        # (`setupAnim*`); check the bare name in that case.
                        raw = literal.split("(")[0].strip().rstrip("*").strip()
                        if not raw:
                            continue
                        if raw not in names:
                            broken += 1
                            print("BROKEN %-57s @%s method %r not in %s"
                                  % (rel, kind, raw, target))
                            continue
                        # When a descriptor is given it must match a real overload:
                        # `turnPlayer()V` matched nothing once 1.21 changed the method
                        # to `turnPlayer(double)`, which crashes at startup.
                        if "(" in literal:
                            wanted = descriptor_param_names(literal[len(literal.split("(")[0]):])
                            candidates = declared_with_hierarchy(fqn, raw) or []
                            if wanted is not None and candidates and wanted not in candidates:
                                broken += 1
                                print("BROKEN %-57s @%s selector %r: no overload with "
                                      "parameters %s (candidates: %s)"
                                      % (rel, kind, literal, wanted, candidates))

            # `@At(value = "INVOKE", target = "Lowner;name(desc)ret")` points at a
            # specific bytecode member. A renamed *or re-typed* member makes the
            # injection point vanish, and with defaultRequire = 1 that is a hard
            # "Critical injection failure" at startup -- which is exactly how
            # EndPortalBlockMixin broke: Entity#changeDimension still exists in
            # 1.21, but takes a DimensionTransition instead of a ServerLevel.
            for literal in AT_TARGET.findall(text):
                stale = check_at_target(literal)
                if stale is None:
                    unresolved += 1
                    print("UNRES %-58s @At target %s not resolvable" % (rel, literal[:60]))
                elif stale:
                    broken += 1
                    print("BROKEN %-57s @At target %s\n        no method %s with those "
                          "parameter types" % (rel, literal, stale))

            # An @Inject callback's parameters must match the target method's, plus a
            # trailing CallbackInfo / CallbackInfoReturnable. A mismatch is also a hard
            # startup failure, so it is checked rather than left to the game.
            if imports:
                target_fqn = next((imports[t] for t in now if t in imports), None)
                if target_fqn is not None:
                    broken += check_callbacks(rel, text, target_fqn)

    print("\n%d target drift(s), %d missing injection point(s), %d unresolved target(s)"
          % (drift, broken, unresolved))
    sys.exit(1 if (drift or broken) else 0)


if __name__ == "__main__":
    main()
