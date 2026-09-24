"""Find methods that look like overrides but override nothing.

This port's most expensive bug class: 1.21.1 changed a method's parameters, the ported override
kept the 1.20.1 signature, and everything still compiled - the method simply became a private
helper nobody calls while the parent's default kept running. Seen so far:

  finalizeSpawn(..., SpawnGroupData)        -> the extra Forge parameter meant mobs spawned unarmed
  renderToBuffer(..., float r, g, b, a)     -> colour dropped, translucent layers rendered opaque
  getUseDuration(ItemStack)                 -> duration 0, so a grenade cooked off on right click
  Item.hurt / model layers / etc.           -> same shape, found by hand each time

The signature was not wrong; it was *stale*. So this walks every compiled mod class, and for each
declared method checks whether any ancestor declares a method with the same name but a different
descriptor. A same-name-different-args hit is exactly that shape: either a stale override, or a
genuine overload that should be reviewed once.

Usage:
    python tools/audit_stale_overrides.py [--limit N]
    SCGUNS_CLASSES=<dir> python tools/audit_stale_overrides.py
        (point it at an older build's classes to prove the audit can still fail)
"""

import os
import pathlib
import re
import struct
import sys
import zipfile

CLASSES = pathlib.Path(os.environ.get("SCGUNS_CLASSES", "build/classes/java/main"))
CLASSPATH_FILE = pathlib.Path("build-logs/compile-classpath.txt")
BASELINE = pathlib.Path("tools/stale_overrides_baseline.txt")
MOD_PREFIX = "top/ribs/scguns/"


def finding_key(finding):
    owner, name, descriptor, parent, parent_descriptor = finding
    return "%s#%s%s vs %s%s" % (owner, name, descriptor, parent, parent_descriptor)


def parameters(descriptor):
    """The parameter list of a method descriptor, split into single types."""
    end = descriptor.find(")")
    if not descriptor.startswith("(") or end < 0:
        return None
    body, out, current = descriptor[1:end], [], ""
    for ch in body:
        if ch == ";":
            out.append(current + ";")
            current = ""
        else:
            current += ch
    if current:
        out.append(current)
    return out


def looks_like_extra_parameter(finding):
    """True when one parameter list is a prefix of the other.

    That is the shape of an API change ("1.21.1 added a LivingEntity"), as opposed to a genuine
    overload that differs in the middle of its list. Filtering to this shape turns the audit from
    a review list into a short list of likely bugs - it is how the dead
    getDefaultAttributeModifiers(ItemStack) overrides were found.
    """
    left = parameters(finding[2])
    right = parameters(finding[4])
    if left is None or right is None or left == right:
        return False
    shorter, longer = sorted((left, right), key=len)
    return longer[:len(shorter)] == shorter


def classpath_entries():
    """-> list of jars/directories to resolve ancestors from."""
    entries = []
    if CLASSPATH_FILE.exists():
        text = CLASSPATH_FILE.read_text(encoding="utf-8", errors="replace")
        for chunk in re.split(r'[;\r\n]+', text):
            chunk = chunk.strip()
            if chunk and (chunk.endswith(".jar") or chunk.endswith(".zip") or pathlib.Path(chunk).is_dir()):
                entries.append(pathlib.Path(chunk))
    return entries


class ClassFile:
    """Just enough of the class file format: name, super, and declared methods."""

    def __init__(self, data):
        self.data = data
        self.methods = []
        self.interfaces = []
        self.name = None
        self.super_name = None
        self._parse()

    def _u1(self, i):
        return self.data[i]

    def _u2(self, i):
        return struct.unpack_from(">H", self.data, i)[0]

    def _u4(self, i):
        return struct.unpack_from(">I", self.data, i)[0]

    @staticmethod
    def _class_name(pool, index):
        """Resolves a Class constant: Class -> Utf8."""
        entry = pool.get(index)
        if isinstance(entry, tuple) and entry[0] == "class":
            return pool.get(entry[1])
        return entry if isinstance(entry, str) else None

    def _parse(self):
        if self._u4(0) != 0xCAFEBABE:
            raise ValueError("not a class file")
        count = self._u2(8)
        pool, index = {}, 10
        i = 1
        while i < count:
            tag = self._u1(index)
            index += 1
            if tag == 1:                                  # Utf8
                length = self._u2(index)
                pool[i] = self.data[index + 2:index + 2 + length].decode("utf-8", "replace")
                index += 2 + length
            elif tag == 7:                                # Class -> index of its Utf8 name
                pool[i] = ("class", self._u2(index))
                index += 2
            elif tag in (8, 16, 19, 20):                  # String, MethodType, Module, Package
                index += 2
            elif tag in (15,):                            # MethodHandle
                index += 3
            elif tag in (3, 4, 9, 10, 11, 12, 17, 18):    # Fieldref, Methodref, NameAndType, ...
                index += 4
            elif tag in (5, 6):                           # Long, Double take two slots
                index += 8
                i += 1
            else:
                raise ValueError("unknown constant pool tag %d" % tag)
            i += 1

        index += 2                                        # access flags
        self.name = self._class_name(pool, self._u2(index))
        index += 2
        self.super_name = self._class_name(pool, self._u2(index))
        index += 2

        interfaces = self._u2(index)
        index += 2
        for _ in range(interfaces):
            name = self._class_name(pool, self._u2(index))
            if name:
                self.interfaces.append(name)
            index += 2

        fields = self._u2(index)                          # fields
        index += 2
        for _ in range(fields):
            index += 6                                    # access, name, descriptor
            attributes = self._u2(index)
            index += 2
            for _ in range(attributes):
                length = self._u4(index + 2)
                index += 6 + length

        methods = self._u2(index)                         # methods
        index += 2
        for _ in range(methods):
            index += 2                                    # access flags
            name = pool.get(self._u2(index))
            index += 2
            descriptor = pool.get(self._u2(index))
            index += 2
            self.methods.append((name, descriptor))
            attributes = self._u2(index)
            index += 2
            for _ in range(attributes):
                length = self._u4(index + 2)
                index += 6 + length


class MergedClass:
    """What an ancestor looks like to the comparison: names and a method set."""

    def __init__(self, name, super_name, interfaces, methods):
        self.name = name
        self.super_name = super_name
        self.interfaces = interfaces
        self.methods = methods


def load(path):
    try:
        return load_from_bytes(path.read_bytes())
    except OSError:
        return None


def load_from_bytes(data):
    try:
        return ClassFile(data)
    except (ValueError, struct.error, IndexError, OSError):
        return None


def main():
    limit = 40
    if "--limit" in sys.argv:
        limit = int(sys.argv[sys.argv.index("--limit") + 1])

    roots = classpath_entries()
    if not roots:
        print("no compile classpath found - run `gradlew printCompileClasspath` first")
        return 2

    # Mod classes are parsed up front; every other class (Minecraft, NeoForge, libraries) is
    # found by name on demand and parsed once, so the whole classpath never has to be read.
    lookup = {}
    for path in CLASSES.rglob("*.class"):
        klass = load(path)
        if klass and klass.name:
            lookup[klass.name] = klass

    index, handles = {}, []
    for root in roots:
        if root.is_dir():
            for path in root.rglob("*.class"):
                name = path.relative_to(root).as_posix()[:-len(".class")]
                index.setdefault(name, []).append(("dir", root, path))
        elif root.suffix in (".jar", ".zip"):
            try:
                handle = zipfile.ZipFile(root)
            except (zipfile.BadZipFile, OSError):
                continue
            handles.append(handle)
            for name in handle.namelist():
                if name.endswith(".class"):
                    index.setdefault(name[:-len(".class")], []).append(("zip", handle, name))

    def resolve(name):
        """Merged view of a class: every jar that declares it contributes its methods.

        Merging matters because NeoForge adds overloads to vanilla classes; resolving from a
        single jar would report those legitimately-overridden methods as stale.
        """
        if name in lookup:
            return lookup[name]
        sources = index.get(name)
        if not sources:
            return None
        methods, super_name, interfaces = set(), None, []
        for kind, source, location in sources:
            try:
                data = source.read_bytes() if kind == "dir" else source.read(location)
            except OSError:
                continue
            klass = load_from_bytes(data)
            if klass is None:
                continue
            methods.update(klass.methods)
            super_name = super_name or klass.super_name
            interfaces.extend(klass.interfaces)
        klass = MergedClass(name, super_name, interfaces, sorted(methods))
        lookup[name] = klass
        return klass

    findings, checked, unresolved = [], 0, 0
    for path in sorted(CLASSES.rglob("*.class")):
        klass = load(path)
        if klass is None or not klass.name or not klass.name.startswith(MOD_PREFIX):
            continue
        if "$" in klass.name.rsplit("/", 1)[-1]:
            continue

        # Walk the whole ancestor graph (superclasses and interfaces, transitively): NeoForge
        # adds its overloads through interfaces such as IBlockExtension, so a superclass-only
        # walk reports every legitimate NeoForge override as stale.
        ancestors, queue, seen, guard = [], [klass.super_name] + list(klass.interfaces), set(), 0
        while queue and guard < 64:
            current = queue.pop(0)
            guard += 1
            if not current or current in seen:
                continue
            seen.add(current)
            parent = resolve(current)
            if parent is None:
                unresolved += 1
                continue
            ancestors.append(parent)
            queue.extend([parent.super_name] + list(getattr(parent, "interfaces", [])))

        for name, descriptor in klass.methods:
            if name in ("<init>", "<clinit>") or name.startswith("lambda$") or name.startswith("access$"):
                continue                                # compiler-generated
            checked += 1
            # A method is suspicious only when it overrides NOTHING: some ancestor declares the
            # same name, but no ancestor anywhere declares this exact descriptor. Comparing
            # against a single ancestor would flag every legitimate NeoForge overload (the
            # added-parameter forms live on interfaces such as IBlockExtension).
            if any(descriptor in [d for _, d in parent.methods] for parent in ancestors):
                continue
            for parent in ancestors:
                same_name = [d for n, d in parent.methods if n == name]
                if same_name:
                    findings.append((klass.name[len(MOD_PREFIX):], name, descriptor,
                                     parent.name[len(MOD_PREFIX):] if parent.name.startswith(MOD_PREFIX)
                                     else parent.name, same_name[0]))
                    break

    if "--prefix-only" in sys.argv:
        # The likely-bug subset: an API that gained or lost a parameter at the end.
        filtered = [f for f in findings if looks_like_extra_parameter(f)]
        for owner, name, descriptor, parent, parent_descriptor in filtered:
            print("  %s#%s%s" % (owner.replace("/", "."), name, descriptor))
            print("      %s declares %s%s" % (parent.replace("/", "."), name, parent_descriptor))
        print("%d extra-parameter finding(s) out of %d; %d checked method(s)"
              % (len(filtered), len(findings), checked))
        return 0

    for owner, name, descriptor, parent, parent_descriptor in findings[:limit]:
        print("  %s#%s%s" % (owner.replace("/", "."), name, descriptor))
        print("      %s declares %s%s" % (parent.replace("/", "."), name, parent_descriptor))
    if len(findings) > limit:
        print("  ... %d more" % (len(findings) - limit))

    keys = {finding_key(f) for f in findings}
    print("%d method(s) that override nothing but share a name with an ancestor, out of %d "
          "checked method(s)%s"
          % (len(findings), checked,
             "" if not unresolved else " (%d ancestor chain(s) unresolvable)" % unresolved))
    print("This is a review aid, not a gate: most hits are legitimate (NeoForge interface "
          "defaults,\nmod helpers that share a name, delegating overloads). The real gate is "
          "@Override,\nwhich makes javac reject a drifted signature outright.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
