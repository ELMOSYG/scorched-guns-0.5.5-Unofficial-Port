"""Optional-mod API references: the one failure mode a method guard cannot stop.

A JVM links a class - verifying every method in it - before the first of its methods runs, and
the type-checking verifier resolves the types it has to decide assignability between. So an
optional mod's class appearing anywhere in a *signature* of a class that gets linked
unconditionally is a latent `NoClassDefFoundError`, and no amount of `if (modLoaded)` inside
those methods helps: the guard has not run yet when the class dies.

That is not a hypothetical. It is why entering a save crashed without Sable:

    PhysicsStructureHelper.poseAt  ->  returns dev.ryanhcode.sable.companion.math.Pose3dc,
                                         computed from a dev.ryanhcode...Pose3d

so verifying the helper - triggered by the first turret tick, because a turret calls
`toWorld` unconditionally - had to load the Sable types. Reproduced standalone: a class whose
method returns an interface implemented by a class missing from the classpath throws at class
initialisation, while the same code behind a nested holder the guard never touches runs clean.

What is safe, and why (all three are in this tree today):

* naming an optional type only inside `instanceof` / `checkcast`, in a class that is linked
  unconditionally - `GuardFriendlyRules` and `Guard`. The verifier records the test and defers
  the load to the instruction, and the instruction only runs behind `isGuard(...)`.
* an optional type only as an intermediate of a call, never in a signature -
  `AirSourceHelper` and `BacktankUtil`, which returns primitives and `List<ItemStack>`. The
  declared locals are named types, so nothing has to be resolved.
* a whole class of optional references, reached only from behind the mod's flag -
  `SablePhysicsBridge`. A nested class is a class of its own, so it is verified separately,
  and nothing links it until the guard has already said yes.

The rules below therefore are: an optional package may appear in a signature only inside a class
listed in BRIDGES, and a class listed in BRIDGES must have no in-tree referrer other than a
guarded one. Anything else is reported.

Usage: python tools/audit_optional_api_refs.py [--selftest]
"""
import io
import os
import pathlib
import re
import sys

ROOT = pathlib.Path(r"E:\mod\scgun-0.5.5-1.21.1-neoforge")
SOURCE = ROOT / "src/main/java"
JAR = ROOT / "build/libs/scguns-0.5.5.1.jar"

# Optional mods whose classes this mod compiles against. com.mrcrayfish (Framework) and
# org.spongepowered (Mixin) are required dependencies, not optional, and are left out on purpose.
OPTIONAL_PACKAGES = {
    "dev.ryanhcode": "sable",
    "com.simibubi": "create",
    "tallestegg": "guardvillagers",
    "it.crystalnest": "prometheus / soul-fire-d",
    "org.antarcticgardens": "create_new_age",
    "me.shedaniel": "clothconfig",
}

# Classes allowed to name an optional mod's types in a signature. Each must be reached only
# from behind its mod's flag, which is checked below rather than assumed.
BRIDGES = {
    "src/main/java/top/ribs/scguns/compat/SablePhysicsBridge.java": "dev.ryanhcode",
}

IMPORT = re.compile(r"^\s*import\s+(?:static\s+)?([A-Za-z0-9_.*]+)\s*;")
SIGNATURE_USE = re.compile(r"\b([A-Z][A-Za-z0-9_]*)\b")

# `void f(Level level)` / `int g()` / `Vec3 h(Level, BlockPos)` - a parenthesised parameter list
# after a return type. Deliberately simple: it only has to recognise the shapes javac emits.
METHOD_HEAD = re.compile(r"^\s*(?:@\w+\s+)*(?:public|protected|private|static|final|\s)*"
                        r"(?P<ret>[A-Za-z0-9_.\[\]<>, ?]+?)\s+"
                        r"(?P<name>[A-Za-z0-9_$]+)\s*\((?P<params>[^)]*)\)")


def imported_types(lines):
    """Optional types this file imports, by simple name."""
    found = {}
    for line in lines:
        match = IMPORT.match(line)
        if not match or match.group(1).endswith(".*"):
            continue
        fqn = match.group(1)
        for package in OPTIONAL_PACKAGES:
            if fqn.startswith(package + "."):
                found[fqn.rsplit(".", 1)[-1]] = package
    return found


def methods(lines):
    """(line_number, ret_type, [param_types]) for every method or constructor declaration."""
    found = []
    for number, line in enumerate(lines, 1):
        head = METHOD_HEAD.match(line)
        if not head:
            continue
        params = [p.strip() for p in head.group("params").split(",") if p.strip()]
        found.append((number, head.group("ret").strip(), params))
    return found


def body_depths(lines):
    """Brace depth at the start of each line, ignoring braces inside strings and comments."""
    depths = []
    depth = 0
    in_string = False
    in_block_comment = False
    for line in lines:
        depths.append(depth)
        in_comment = False
        for char in line:
            if in_block_comment:
                if char == "*" and line.endswith("*/"):
                    in_block_comment = False
                continue
            if in_string:
                if char == '"':
                    in_string = False
                continue
            if char == '"' and not in_comment:
                in_string = True
            elif char == "/" and not in_comment and line.lstrip().startswith("//"):
                in_comment = True
            elif char == "{":
                depth += 1
            elif char == "}":
                depth -= 1
    return depths


def signature_uses(lines, types):
    """Lines where an optional type appears as a return type, a parameter type or a field type.

    A local variable declaration is deliberately *not* included: the verifier types a local from
    whatever the producing expression already is, so `List<ItemStack> x = BacktankUtil.f()` and
    `Pose3dc p = poseAt(...)` never ask it to resolve anything. A *field*, though, is a real
    descriptor, so those are included - and only at class-body depth, since a declaration inside a
    method body is a local.
    """
    hits = []
    for line_number, ret, params in methods(lines):
        tokens = ret.replace(",", " ").split()
        if any(name in tokens for name in types):
            hits.append((line_number, "return type: " + ret.strip()))
        for param in params:
            simple = param.split()[0] if param.split() else ""
            if simple in types:
                hits.append((line_number, "parameter: " + param.strip()))
    depths = body_depths(lines)
    for number, line in enumerate(lines, 1):
        if depths[number - 1] != 1:
            continue
        stripped = line.strip()
        for name in types:
            if re.match(r"^(?:public|protected|private|static|final|transient|volatile|\s)*%s\s+\w+\s*(?:=|;)"
                        % re.escape(name), stripped):
                hits.append((number, "field: " + stripped))
    return hits


def referrers(target_simple, source_root):
    """Source files that mention the bridge class by name."""
    out = []
    for base, _dirs, files in os.walk(source_root):
        for name in files:
            if not name.endswith(".java"):
                continue
            path = pathlib.Path(base) / name
            if path.name == target_simple:
                continue
            text = path.read_text(encoding="utf-8", errors="replace")
            if target_simple in text:
                out.append((path, [i for i, l in enumerate(text.splitlines(), 1)
                                   if target_simple in l]))
    return out


def guard_on_same_method(lines, method_line):
    """The guard flag a call on `method_line`'s statement must sit behind."""
    body_start = method_line
    for index in range(method_line, len(lines)):
        if "ScorchedGuns." in lines[index] or "return" in lines[index] or "{" in lines[index]:
            body_start = index
            break
    window = lines[max(0, body_start - 4):body_start + 1]
    return any("ScorchedGuns." in line for line in window)


def check_source():
    problems = []
    reported = []

    for base, _dirs, files in os.walk(SOURCE):
        for name in sorted(files):
            if not name.endswith(".java"):
                continue
            path = pathlib.Path(base) / name
            key = path.relative_to(ROOT).as_posix()
            lines = path.read_text(encoding="utf-8", errors="replace").splitlines()
            types = imported_types(lines)
            if not types:
                continue
            hits = signature_uses(lines, types)
            if not hits:
                continue
            reported.append((key, sorted({p for p, _f in types.items()})))
            if key in BRIDGES:
                # A bridge may name its own optional mod freely; naming a *second* one would mean
                # one flag is guarding two mods that can be installed independently.
                expected = BRIDGES[key]
                wrong = sorted({package for package in types.values()} - {expected})
                if wrong:
                    problems.append("%s is the bridge for %s but also names %s, which is installed "
                                    "independently and needs its own guard"
                                    % (key, expected, ", ".join(wrong)))
                continue
            for line_number, what in hits:
                problems.append("%s:%d uses optional type(s) %s in a %s - a class guard cannot "
                                "stop this, put them in a bridge class reached from one"
                                % (key, line_number, ", ".join(sorted(types)), what))

    for key, names in reported:
        print("optional types in signatures: %s -> %s" % (key, ", ".join(names)))

    # Every bridge must be reached only from behind its flag.
    for key in BRIDGES:
        simple = pathlib.Path(key).name
        for path, lines_hit in referrers(simple, SOURCE):
            text = path.read_text(encoding="utf-8", errors="replace").splitlines()
            for line_number in lines_hit:
                if IMPORT.match(text[line_number - 1]):
                    continue
                if not guard_on_same_method(text, line_number - 1):
                    problems.append("%s:%d reaches %s with no ScorchedGuns flag guard in sight"
                                    % (path.relative_to(ROOT).as_posix(), line_number, simple))
                break

    return problems


def check_jar():
    """Bytecode truth: an optional type may appear in a descriptor only inside a bridge class.

    A type *descriptor* in the constant pool is written `Ldev/ryanhcode/...;` - the `L` prefix
    and the terminating `;` are what a field descriptor, a method descriptor or a generic
    signature has and a plain class reference does not. A method body that merely calls into the
    optional mod stores the internal name (`dev/ryanhcode/...`), which is safe on its own: those
    instructions only resolve when they execute. So the two are told apart by that prefix, which
    is the same distinction the JVM's verifier makes.
    """
    import zipfile

    if not JAR.exists():
        print("jar not built yet, skipping the bytecode check (%s)" % JAR.name)
        return []
    problems = []
    allowed_files = {pathlib.Path(key).name for key in BRIDGES}
    allowed_folders = {pathlib.Path(key).parent.name for key in BRIDGES}
    counts = {}
    with zipfile.ZipFile(JAR) as archive:
        for name in archive.namelist():
            if not name.endswith(".class"):
                continue
            data = archive.read(name)
            for package in OPTIONAL_PACKAGES:
                descriptor = ("L" + package.replace(".", "/")).encode("ascii")
                body = package.replace(".", "/").encode("ascii")
                in_descriptor = descriptor in data
                in_body = body in data
                if not (in_descriptor or in_body):
                    continue
                key = (package, name)
                counts[package] = counts.get(package, 0) + 1
                if not in_descriptor:
                    continue
                if name.split("/")[-1] in allowed_files or any(
                        "/%s/" % folder in name for folder in allowed_folders):
                    continue
                problems.append("%s has %s in a field or method descriptor - the class is linked "
                                "and verified with it, so it needs the bridge treatment"
                                % (name, package))
    for package in sorted(counts):
        print("%-20s named by %d class file(s)" % (package, counts[package]))
    return problems


def selftest():
    """Prove each rule fires: hand it the shape of the bug it exists to catch."""
    failures = []

    # The original shape: an optional type as a return type of an unguarded class.
    bug = ("package q;\n"
           "import dev.ryanhcode.sable.companion.math.Pose3dc;\n"
           "public class Bug {\n"
           "   private static Pose3dc poseAt(Object level, Object pos) { return null; }\n"
           "}\n")
    scratch = ROOT / "build" / "audit-optional-api-selftest"
    scratch.mkdir(parents=True, exist_ok=True)
    path = scratch / "Bug.java"
    path.write_text(bug, encoding="utf-8")
    if not signature_uses(path.read_text(encoding="utf-8").splitlines(), {"Pose3dc": "dev.ryanhcode"}):
        failures.append("a return type from an optional package was not detected")

    # The shape that is safe: the same type only inside instanceof.
    safe = ("package q;\n"
            "import tallestegg.guardvillagers.common.entities.Guard;\n"
            "public class Safe {\n"
            "   static boolean isAlly(Object a) { return a instanceof Guard; }\n"
            "}\n")
    path.write_text(safe, encoding="utf-8")
    if signature_uses(path.read_text(encoding="utf-8").splitlines(), {"Guard": "tallestegg"}):
        failures.append("an instanceof-only use was wrongly reported as a signature use")

    # A call whose result is stored in a vanilla local must not be reported either.
    call = ("package q;\n"
            "import com.simibubi.create.content.equipment.armor.BacktankUtil;\n"
            "import java.util.List;\n"
            "public class Call {\n"
            "   static void f(Object player) { List x = BacktankUtil.getAllWithAir(player); }\n"
            "}\n")
    path.write_text(call, encoding="utf-8")
    if signature_uses(path.read_text(encoding="utf-8").splitlines(), {"BacktankUtil": "com.simibubi"}):
        failures.append("a vanilla-typed local was wrongly reported as a signature use")

    # And the real tree must be clean, or the rules above are measuring nothing.
    real = check_source()
    if real:
        failures.append("the tree itself reports %d problem(s)" % len(real))

    for failure in failures:
        print("SELFTEST FAIL:", failure)
    print("selftest: %d failure(s)" % len(failures))
    return 1 if failures else 0


def main():
    if "--selftest" in sys.argv:
        return selftest()
    problems = check_source()
    problems += check_jar()
    for problem in problems:
        print("PROBLEM:", problem)
    print("%d problem(s)" % len(problems))
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
