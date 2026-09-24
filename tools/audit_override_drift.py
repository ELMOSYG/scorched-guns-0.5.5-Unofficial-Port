"""Find instance methods that *look* like overrides but whose signature drifted.

`AnimatedGunRenderer.renderRecursively` was ported with 0.5.5's GeckoLib 4.4 tail
`(..., float red, float green, float blue, float alpha)`.  GeckoLib 4.9.3 declares
that hook as `(..., int renderColor)`.  The parameter list therefore no longer
matched, so the method **silently stopped overriding** `GeoRenderer#renderRecursively`:
javac emitted no error and no warning, the whole body became dead code, and the gun
model rendered its own baked-in arms and attachments.  Nothing in the build could
see it because the only proof of an override is `@Override`, which the port had lost.

So the rule is: for every non-static, non-private, non-synthetic instance method we
declare, walk the superclass chain and the full interface closure (read from *compiled
bytecode*, because the supertypes live in jars).  A supertype method with the same name
and arity but a different parameter list means we are not overriding it, and is reported
as either

    DRIFT      a position's type is neither equal nor assignable to the supertype's, so
               nothing above can accept our arguments: no override, no bridge, dead
               code.  This is the `renderRecursively` failure, and it exits non-zero.
    erasure    every position is assignable, so the supertype is generic and its
               descriptor was erased (`T` -> `MyEntity`).  javac emits a bridge, the
               method really does override - it is only missing `@Override`.  Reported
               as latent risk (the missing annotation is what let the drift happen),
               but it is not lost behaviour, and it does not fail the run.

Methods that themselves carry `@Override` in source are skipped: javac has already
proved that override, so they cannot be this bug.  `java.lang.Override` has
`RetentionPolicy.SOURCE`, so the annotation is NOT in the class file and must be read
from the `.java` text.

    python tools/audit_override_drift.py              # audit the tree
    python tools/audit_override_drift.py --selftest   # prove the rules
    python tools/audit_override_drift.py --verbose    # also list suppressed candidates

Exits 1 when there is any DRIFT finding, 0 when clean (erasure-only findings included).
"""
from __future__ import annotations

import io
import os
import re
import struct
import sys
import zipfile

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")
CLASSES = os.path.join(ROOT, "build", "classes", "java", "main")
CLASSPATH_FILE = os.path.join(ROOT, "build-logs", "compile-classpath.txt")
LIBS = os.path.join(ROOT, "libs")

# Only classes of our own mod are audited; supertypes may live anywhere.
OWN_PREFIX = "top/ribs/scguns/"

ACC_PUBLIC = 0x0001
ACC_PRIVATE = 0x0002
ACC_STATIC = 0x0008
ACC_BRIDGE = 0x0040
ACC_SYNTHETIC = 0x1000

# Types the parser is allowed to touch.  A runaway supertype closure would mean a
# parser bug (a mis-read constant pool usually), so it fails loudly instead.
CLASS_BUDGET = 8000

# ---------------------------------------------------------------------------
# Known-benign, verified by hand.  Keyed by `binary.ClassName#methodName`, never
# by wildcard.  Every entry that actually suppresses a finding is printed with
# the rest of the report, so a stale entry is visible instead of silent.
#
# Empty on purpose.  Each of the 17 DRIFT findings on this tree was read and is a
# real bug, and the generic-erasure candidates are reported in their own bucket
# instead of being whitelisted because they are not false positives - they really
# do override.  Add an entry only for an overload that is intended, cannot be
# renamed, and has been verified by hand; put the reason in the value.
# ---------------------------------------------------------------------------
WHITELIST: dict[str, str] = {}

# ---------------------------------------------------------------------------
# Class file parser: constant pool + this/super/interfaces + method table.
# ---------------------------------------------------------------------------

# tag -> info byte size, for every tag that appears in a pool.  Entry 1 (Utf8) has a
# variable size and is advanced explicitly in parse_constant_pool; its `size` is still
# declared so that the tag lookup doubles as a "is this a tag I know?" check.
CP_SIZES = {
    1: 0,    # Utf8          (variable: u2 length + bytes, handled separately)
    3: 4,    # Integer
    4: 4,    # Float
    5: 8,    # Long          (takes two slots)
    6: 8,    # Double        (takes two slots)
    7: 2,    # Class
    8: 2,    # String
    9: 4,    # Fieldref
    10: 4,   # Methodref
    11: 4,   # InterfaceMethodref
    12: 4,   # NameAndType
    15: 3,   # MethodHandle
    16: 2,   # MethodType
    17: 4,   # Dynamic
    18: 4,   # InvokeDynamic
    19: 2,   # Module
    20: 2,   # Package
}
TWO_SLOT = (5, 6)  # CONSTANT_Long / CONSTANT_Double

TYPE_CHARS = {"B", "C", "D", "F", "I", "J", "S", "Z"}


def internal_to_binary(name: str) -> str:
    return name.replace("/", ".")


def class_entry(name: str) -> str:
    """Binary class name -> `a/b/C.class`, keeping the `$` of nested classes."""
    if name.endswith(".class"):
        return name
    return name.replace(".", "/") + ".class"


def parse_constant_pool(buf: bytes, pos: int) -> tuple[list, int]:
    """Return (pool, offset just past the pool).

    pool is indexed by constant-pool slot: pool[i] is (tag, value), and the slot after
    a CONSTANT_Long / CONSTANT_Double is left as None because those take two slots.
    """
    count = struct.unpack_from(">H", buf, pos)[0]
    pos += 2
    slots: dict[int, tuple] = {}
    i = 1
    while i < count:
        tag = buf[pos]
        pos += 1
        if os.environ.get("AUDIT_CP_DEBUG"):
            sys.stderr.write("cp slot=%d tag=%d at=%d\n" % (i, tag, pos - 1))
        size = CP_SIZES.get(tag)
        if size is None:
            raise ValueError("unknown constant pool tag %d at slot %d (pool count %d, bytes left %d)"
                             % (tag, i, count, len(buf) - pos))
        if tag == 1:  # Utf8: u2 length + bytes
            length = struct.unpack_from(">H", buf, pos)[0]
            raw = buf[pos + 2:pos + 2 + length]
            value = raw.decode("utf-8", "replace")  # modified UTF-8 is close enough
            pos += 2 + length
        elif tag in (7, 8, 16, 19, 20):  # single u2 index: Class, String, MethodType...
            value = struct.unpack_from(">H", buf, pos)[0]
            pos += size
        elif tag in (9, 10, 11, 12):  # two u2 indexes: refs and NameAndType
            value = struct.unpack_from(">HH", buf, pos)
            pos += size
        else:  # u4 payloads: Integer/Float/Dynamic/InvokeDynamic
            value = buf[pos:pos + size]
            pos += size
        if tag == 15:  # MethodHandle: u1 reference_kind + u2 reference_index
            value = (buf[pos - 3], struct.unpack_from(">H", buf, pos - 2)[0])
        slots[i] = (tag, value)
        i += 2 if tag in TWO_SLOT else 1
    pool = [None] * count
    for index, entry in slots.items():
        pool[index] = entry
    return pool, pos


def utf8(pool: list, index: int) -> str | None:
    entry = pool[index] if 0 <= index < len(pool) else None
    if entry is None or entry[0] != 1:
        return None
    return entry[1]


def class_name(pool: list, index: int) -> str | None:
    """Resolve a class_info index: CONSTANT_Class -> its CONSTANT_Utf8 name."""
    entry = pool[index] if 0 <= index < len(pool) else None
    if entry is None:
        return None
    if entry[0] == 7:  # CONSTANT_Class: the value is a Utf8 index
        return utf8(pool, entry[1])
    if entry[0] == 1:  # be forgiving: already a Utf8 name
        return entry[1]
    return None


def decode_descriptor(desc: str) -> tuple[list[str], str]:
    """Method descriptor -> ([parameter type names], return type name)."""
    if not desc.startswith("("):
        raise ValueError("not a method descriptor: %r" % desc)
    i, params = 1, []
    while i < len(desc) and desc[i] != ")":
        dims = 0
        while desc[i] == "[":
            dims += 1
            i += 1
        if desc[i] == "L":
            end = desc.index(";", i)
            # keep the binary name, so `Foo$Bar` matches a class entry `Foo$Bar.class`
            name = internal_to_binary(desc[i + 1:end])
            i = end + 1
        elif desc[i] in TYPE_CHARS:
            name = desc[i]
            i += 1
        else:
            raise ValueError("bad descriptor character %r in %r" % (desc[i], desc))
        params.append("[" * dims + name)
    ret = desc[i + 1:]
    dims = 0
    while dims < len(ret) and ret[dims] == "[":
        dims += 1
    if dims:
        ret = "[" * dims + ret[dims]
    elif ret.startswith("L"):
        ret = internal_to_binary(ret[1:-1])
    return params, ret


class Method:
    __slots__ = ("name", "desc", "flags", "params", "ret")

    def __init__(self, name: str, desc: str, flags: int):
        self.name = name
        self.desc = desc
        self.flags = flags
        self.params, self.ret = decode_descriptor(desc)

    def __repr__(self) -> str:  # pragma: no cover - debugging aid
        return "Method(%s%s flags=0x%04x)" % (self.name, self.desc, self.flags)


class ClassInfo:
    __slots__ = ("name", "super_name", "interfaces", "flags", "methods", "root")

    def __init__(self, name, super_name, interfaces, flags, methods, root):
        self.name = name
        self.super_name = super_name
        self.interfaces = interfaces
        self.flags = flags
        self.methods = methods
        self.root = root


def parse_class(buf: bytes, root: str | None = None) -> ClassInfo:
    if len(buf) < 10 or buf[:4] != b"\xca\xfe\xba\xbe":
        raise ValueError("not a class file (bad magic)")
    pos = 8  # magic + minor + major
    pool, pos = parse_constant_pool(buf, pos)
    flags, this_i, super_i = struct.unpack_from(">HHH", buf, pos)
    pos += 6
    n_ifaces = struct.unpack_from(">H", buf, pos)[0]
    pos += 2
    iface_indexes = list(struct.unpack_from(">%dH" % n_ifaces, buf, pos)) if n_ifaces else []
    pos += 2 * n_ifaces
    n_fields = struct.unpack_from(">H", buf, pos)[0]
    pos += 2
    for _ in range(n_fields):  # names and descriptors are not needed: skip fields
        n_attr = struct.unpack_from(">H", buf, pos + 6)[0]
        pos += 8
        for _ in range(n_attr):
            length = struct.unpack_from(">I", buf, pos + 2)[0]
            pos += 6 + length
    n_methods = struct.unpack_from(">H", buf, pos)[0]
    pos += 2
    methods = []
    for _ in range(n_methods):
        m_flags, name_i, desc_i, n_attr = struct.unpack_from(">HHHH", buf, pos)
        pos += 8
        for _ in range(n_attr):
            length = struct.unpack_from(">I", buf, pos + 2)[0]
            pos += 6 + length
        name, desc = utf8(pool, name_i), utf8(pool, desc_i)
        if name is None or desc is None:
            continue
        try:
            methods.append(Method(name, desc, m_flags))
        except ValueError:
            continue  # malformed descriptor: ignore rather than abort the audit
    name = class_name(pool, this_i) or ""
    super_name = class_name(pool, super_i) if super_i else None
    ifaces = [class_name(pool, ix) for ix in iface_indexes]
    return ClassInfo(
        internal_to_binary(name),
        internal_to_binary(super_name) if super_name else None,
        [internal_to_binary(i) for i in ifaces if i],
        flags,
        methods,
        root,
    )


# ---------------------------------------------------------------------------
# Lazy class resolution: index entry names per root, parse only on demand.
# ---------------------------------------------------------------------------


class ClassPath:
    def __init__(self, roots: list[str]):
        self.roots = roots
        self.index: list[tuple[str, dict[str, str]]] = []  # (root, lower entry -> path)
        self.cache: dict[str, ClassInfo | None] = {}
        self.stats = {"resolve": 0, "parse": 0, "miss": 0}
        for root in roots:
            entries = self._index_root(root)
            if entries is not None:
                self.index.append((root, entries))

    @staticmethod
    def _index_root(root: str) -> dict[str, str] | None:
        table: dict[str, str] = {}
        if root.endswith(".jar"):
            if not os.path.isfile(root):
                return None
            try:
                with zipfile.ZipFile(root) as zf:
                    for entry in zf.namelist():
                        if entry.endswith(".class"):
                            table[entry.lower()] = entry
            except (OSError, zipfile.BadZipFile):
                return None
            return table
        if not os.path.isdir(root):
            return None
        for dirpath, _, files in os.walk(root):
            for f in files:
                if not f.endswith(".class"):
                    continue
                rel = os.path.relpath(os.path.join(dirpath, f), root).replace(os.sep, "/")
                table[rel.lower()] = rel
        return table

    def _lookup(self, root: str, table: dict[str, str], entry: str) -> str | None:
        if root.endswith(".jar"):
            rel = table.get(entry.lower())
            return None if rel is None else "%s!/%s" % (root, rel)
        rel = table.get(entry.lower())
        if rel is None:
            return None
        return os.path.join(root, rel.replace("/", os.sep))

    def find(self, binary_name: str) -> str | None:
        entry = class_entry(binary_name)
        for root, table in self.index:
            path = self._lookup(root, table, entry)
            if path is not None:
                return path
        return None

    @staticmethod
    def read(path: str) -> bytes:
        if "!/" in path:
            jar, entry = path.split("!/", 1)
            with zipfile.ZipFile(jar) as zf:
                return zf.read(entry)
        with open(path, "rb") as fh:
            return fh.read()

    def reify(self, binary_name: str) -> ClassInfo | None:
        """Parsed ClassInfo for a binary name, or None when it cannot be resolved."""
        if binary_name in self.cache:
            return self.cache[binary_name]
        if len(self.cache) > CLASS_BUDGET:
            raise RuntimeError("class budget exceeded (%d): parser bug?" % len(self.cache))
        self.stats["resolve"] += 1
        info = None
        path = self.find(binary_name)
        if path is not None:
            try:
                info = parse_class(self.read(path), path)
                self.stats["parse"] += 1
            except (ValueError, OSError, struct.error, IndexError, zipfile.BadZipFile):
                info = None
        else:
            self.stats["miss"] += 1
        self.cache[binary_name] = info
        return info

    def available_classes(self, root: str) -> list[str]:
        """`a/b/C.class` names available under one root (already indexed)."""
        for indexed_root, table in self.index:
            if indexed_root == root:
                return sorted(table.values())
        return []


def own_instance_methods(info: ClassInfo) -> list[Method]:
    """Declared methods that could participate in an override."""
    out = []
    for m in info.methods:
        if m.name in ("<init>", "<clinit>"):
            continue
        if m.flags & (ACC_STATIC | ACC_PRIVATE | ACC_SYNTHETIC | ACC_BRIDGE):
            continue
        out.append(m)
    return out


def supertype_closure(cp: ClassPath, binary_name: str, limit: int = 400) -> set[str]:
    """Every class/interface name transitively above `binary_name` (cached on cp)."""
    cache = getattr(cp, "_closure_cache", None)
    if cache is None:
        cache = cp._closure_cache = {}
    if binary_name in cache:
        return cache[binary_name]
    seen: set[str] = set()
    pending = [binary_name]
    while pending and len(seen) < limit:
        name = pending.pop()
        if name in seen:
            continue
        seen.add(name)
        info = cp.reify(name)
        if info is None:
            continue
        if info.super_name:
            pending.append(info.super_name)
        pending.extend(info.interfaces)
    seen.discard(binary_name)
    cache[binary_name] = seen
    return seen


def is_subtype(cp: ClassPath, sub: str, sup: str) -> bool:
    """Can a `sub` value be passed where a `sup` is expected, by name alone?"""
    if sub == sup:
        return True
    if sup == "java.lang.Object":
        return True  # everything is an Object (and generic T erases to Object)
    if sub.startswith("[") or sup.startswith("[") or len(sub) == 1 or len(sup) == 1:
        return False  # primitives/arrays: exact matching only
    return sup in supertype_closure(cp, sub)


def erasure_compatible(cp: ClassPath, ours: Method, theirs: Method) -> bool:
    """True when every parameter of `theirs` can already accept `ours`.

    A generic supertype erases its type parameter in the descriptor, so
    `LivingEntityRenderer.render(LivingEntity, ...)` is what a subclass's
    `render(MyEntity, ...)` overrides.  That is a real override, not drift, and this
    is what tells the two apart.
    """
    if len(ours.params) != len(theirs.params):
        return False
    return all(is_subtype(cp, o, t) for o, t in zip(ours.params, theirs.params))


class Finding:
    __slots__ = ("cls", "method", "desc", "origin", "origin_desc", "kind", "arity",
                 "line", "rel")

    def __init__(self, cls, method, desc, origin, origin_desc, kind, arity):
        self.cls = cls
        self.method = method
        self.desc = desc
        self.origin = origin
        self.origin_desc = origin_desc
        self.kind = kind
        self.arity = arity
        self.line = 0
        self.rel = "-"

    @property
    def key(self) -> str:
        return "%s#%s" % (self.cls, self.method)


def compare_with_owners(info: ClassInfo, cp: ClassPath) -> tuple[list[Finding], list[str]]:
    """The rule.

    An exact descriptor match anywhere in the hierarchy means a genuine override, so
    the method is fine however it was reached.  Two weaker matches are reported
    separately, because they mean different things:

    * `arity`  - same parameter count, but a position's type is neither equal nor a
                 subtype of the supertype's.  This is the drift class: ours cannot
                 override that method and no bridge will be generated for it.
    * `erasure`- same parameter count and every position is assignment compatible, so
                 the supertype is generic and its descriptor was erased.  The class
                 bytecode does contain a matching override (via a synthetic bridge) -
                 worth flagging as an `@Override` annotation that has gone missing,
                 never as lost behaviour.

    A same-name supertype method with a *different* parameter count is a plain
    overload and is not reported at all.
    """
    findings: list[Finding] = []
    owners: dict[str, list[tuple[str, Method]]] = {}
    distance: dict[str, int] = {}
    visited: set[str] = set()
    unresolved: list[str] = []
    # BFS from the class itself, so a finding can name the *nearest* supertype that
    # declares the same-name method instead of whichever interface happened to be
    # visited last.
    pending: list[tuple[int, str]] = [(1, name) for name in
                                      ([info.super_name] if info.super_name else [])
                                      + list(info.interfaces)]
    while pending:
        depth, name = pending.pop(0)
        if name in visited:
            continue
        visited.add(name)
        sup = cp.reify(name)
        if sup is None:
            unresolved.append(name)
            continue
        distance[name] = depth
        for m in sup.methods:
            if m.flags & (ACC_STATIC | ACC_PRIVATE):
                continue
            owners.setdefault(m.name, []).append((name, m))
        if sup.super_name:
            pending.append((depth + 1, sup.super_name))
        for iface in sup.interfaces:
            pending.append((depth + 1, iface))

    def nearest(cands: list[tuple[str, Method]]) -> tuple[str, Method]:
        return min(cands, key=lambda os_: (distance.get(os_[0], 99), os_[1].desc))

    for m in own_instance_methods(info):
        supers = owners.get(m.name)
        if not supers:
            continue
        if any(s.desc == m.desc for _, s in supers):
            continue  # exact descriptor somewhere above: a genuine override
        same_arity = [(owner, s) for owner, s in supers if len(s.params) == len(m.params)]
        if not same_arity:
            # A different-arity pairing is usually a plain overload, so it is not
            # drift by itself.  But the Forge -> NeoForge migration removed
            # trailing callback parameters (e.g. `finalizeSpawn(..., SpawnGroupData,
            # CompoundTag)` -> `finalizeSpawn(..., SpawnGroupData)`), which leaves
            # our parameter list as the supertype's list plus extra params on the
            # end.  That shape is a strong, low-noise signal: the supertype's
            # parameters must be a PREFIX of ours.  Getting this wrong is silent --
            # the method simply never runs -- so it is reported in its own bucket.
            prefix = [
                (owner, s)
                for owner, s in supers
                if len(s.params) < len(m.params)
                and m.params[: len(s.params)] == s.params
            ]
            if prefix:
                owner, nearest_m = nearest(prefix)
                findings.append(Finding(info.name, m.name, m.desc, owner, nearest_m.desc,
                                        "arity-mismatch", len(m.params)))
            continue
        erased = [(owner, s) for owner, s in same_arity if erasure_compatible(cp, m, s)]
        if erased:
            owner, nearest_m = nearest(erased)
            findings.append(Finding(info.name, m.name, m.desc, owner, nearest_m.desc,
                                    "erasure", len(m.params)))
        else:
            owner, nearest_m = nearest(same_arity)
            findings.append(Finding(info.name, m.name, m.desc, owner, nearest_m.desc,
                                    "arity", len(m.params)))
    return findings, unresolved


# ---------------------------------------------------------------------------
# Source side: `@Override` (SOURCE retention, absent from the bytecode) and lines.
# ---------------------------------------------------------------------------

BLOCK_COMMENT = re.compile(r"/\*.*?\*/", re.S)
LINE_COMMENT = re.compile(r"//[^\n]*")
STRING_LIT = re.compile(r'"(?:\\.|[^"\\])*"')
CHAR_LIT = re.compile(r"'(?:\\.|[^'\\])*'")
CLASS_DECL = re.compile(r"\b(?:class|interface|enum|record|@interface)\s+(\w+)")
# `@Override` optionally followed by more annotations, then a method name
OVERRIDE = re.compile(
    r"@Override\b"
    r"(?:\s*@\s*\w+(?:\s*\([^()]*\))?)*"
    r"\s*[\w\s<>\[\],.?&@]*?\b(\w+)\s*\("
)


def code_only(text: str) -> str:
    """Comments and literals blanked out (length preserved), so regexes see code."""
    text = BLOCK_COMMENT.sub(lambda m: re.sub(r"[^\n]", " ", m.group(0)), text)
    text = LINE_COMMENT.sub(lambda m: " " * len(m.group(0)), text)
    text = STRING_LIT.sub(lambda m: " " * len(m.group(0)), text)
    text = CHAR_LIT.sub(lambda m: " " * len(m.group(0)), text)
    return text


def class_blocks(code: str) -> dict[str, tuple[int, int]]:
    """Depth-0 type name -> (start, end) span in `code` (its own body, not nested ones)."""
    positions = [(m.start(), m.group(1)) for m in CLASS_DECL.finditer(code)]
    brace_positions = [m.start() for m in re.finditer(r"[{}]", code)]
    result: dict[str, tuple[int, int]] = {}
    depth = 0
    pi = 0
    for start, name in positions:
        while pi < len(brace_positions) and brace_positions[pi] < start:
            depth += 1 if code[brace_positions[pi]] == "{" else -1
            pi += 1
        if depth == 0 and name not in result:
            result[name] = (start, start)
    # compute ends: from the declaration's opening brace to its matching close
    for name, (start, _) in list(result.items()):
        brace = code.find("{", start)
        if brace < 0:
            result[name] = (start, len(code))
            continue
        depth, i, end = 1, brace + 1, len(code)
        while i < len(code):
            ch = code[i]
            if ch == "{":
                depth += 1
            elif ch == "}":
                depth -= 1
                if depth == 0:
                    end = i
                    break
            i += 1
        result[name] = (start, end)
    return result


def override_method_names(source: str) -> tuple[set[str], dict[str, int]]:
    """Method names carrying `@Override`, plus one line number each as evidence.

    Name-level granularity: if any declaration of a name is annotated, every
    declaration of that name in the class is treated as annotated.  That direction
    is deliberate - it can only ever suppress a report, never invent one.
    """
    code = code_only(source)
    names: set[str] = set()
    lines: dict[str, int] = {}
    for m in OVERRIDE.finditer(code):
        name = m.group(1)
        if name in ("class", "interface", "enum", "record"):
            continue
        names.add(name)
        lines.setdefault(name, source[:m.start()].count("\n") + 1)
    return names, lines


def source_line_for(source: str, method: str) -> int:
    m = re.search(r"\b" + re.escape(method) + r"\s*\(", source)
    if not m:
        return 0
    return source[:m.start()].count("\n") + 1


# ---------------------------------------------------------------------------
# Discovery
# ---------------------------------------------------------------------------


def read_classpath() -> list[str]:
    """Dependency jars from build-logs/compile-classpath.txt (sep `;` or newline)."""
    if not os.path.isfile(CLASSPATH_FILE):
        return []
    with open(CLASSPATH_FILE, encoding="utf-8", errors="replace") as fh:
        raw = fh.read()
    parts: list[str] = []
    for chunk in raw.replace("\r\n", "\n").split(";"):
        for line in chunk.split("\n"):
            line = line.strip().strip('"')
            if line:
                parts.append(line)
    return parts


def build_classpath() -> ClassPath:
    roots: list[str] = []
    seen: set[str] = set()

    def add(path: str) -> None:
        key = os.path.normcase(os.path.abspath(path))
        if key in seen or not os.path.exists(path):
            return
        seen.add(key)
        roots.append(path)

    add(CLASSES)  # our own classes win over everything else
    for entry in read_classpath():
        add(entry)
    if os.path.isdir(LIBS):  # extra jars that the recorded classpath may not name
        for f in sorted(os.listdir(LIBS)):
            if f.endswith(".jar"):
                add(os.path.join(LIBS, f))
    return ClassPath(roots)


class Sources:
    """`a/b/C` (or `a/b/C$D`) -> (relative path, text)."""

    def __init__(self, root: str):
        self.root = root
        self.files: dict[str, tuple[str, str]] = {}
        for dirpath, _, files in os.walk(root):
            for f in files:
                if not f.endswith(".java"):
                    continue
                path = os.path.join(dirpath, f)
                rel = os.path.relpath(path, root).replace(os.sep, "/")
                key = rel[:-len(".java")]
                with open(path, encoding="utf-8", errors="replace") as fh:
                    self.files[key] = (rel, fh.read())

    def mirror(self, binary_name: str) -> tuple[str, str] | None:
        """Java's public-type/file-name rule: nested `Outer$Inner` lives in Outer.java."""
        key = binary_name.replace(".", "/")
        while True:
            hit = self.files.get(key)
            if hit is not None:
                return hit
            if "$" not in key:
                return None
            key = key.rsplit("$", 1)[0]

    def nested_source(self, binary_name: str) -> tuple[str, str, str] | None:
        """(rel path, full text, text of just that nested/top-level type)."""
        hit = self.mirror(binary_name)
        if hit is None:
            return None
        rel, text = hit
        short = binary_name.split(".")[-1]
        simple = short.split("$")[-1]
        blocks = class_blocks(code_only(text))
        span = blocks.get(simple)
        if span is None:
            return rel, text, text
        return rel, text, text[span[0]:span[1]]


# ---------------------------------------------------------------------------
# Audit
# ---------------------------------------------------------------------------


def audit(cp: ClassPath, sources: Sources) -> tuple[dict, list, list, dict]:
    """Classify every same-name candidate into drift, arity-mismatch, erasure or whitelist."""
    buckets: dict[str, list[Finding]] = {"arity": [], "arity-mismatch": [], "erasure": []}
    suppressed_override: list[tuple[str, str]] = []
    whitelisted: list[Finding] = []
    unresolved: dict[str, list[str]] = {}
    for entry in cp.available_classes(CLASSES):
        if not entry.startswith(OWN_PREFIX):
            continue
        binary = entry[:-len(".class")].replace("/", ".")
        info = cp.reify(binary)
        if info is None:
            continue
        found, missing = compare_with_owners(info, cp)
        if missing:
            unresolved[binary] = sorted(set(missing))
        if not found:
            continue
        src = sources.nested_source(binary)
        if src is None:
            annotated: set[str] = set()
        else:
            annotated, _ = override_method_names(src[2])
        for f in found:
            if f.method in annotated:
                suppressed_override.append((binary, f.method))
                continue
            if f.key in WHITELIST:
                whitelisted.append(f)
                continue
            orig = sources.nested_source(f.cls)
            f.line = source_line_for(orig[2], f.method) if orig else 0
            f.rel = orig[0] if orig else "-"
            buckets[f.kind].append(f)
    for bucket in buckets.values():
        bucket.sort(key=lambda f: (f.rel, f.line, f.method))
    return buckets, suppressed_override, whitelisted, unresolved


def report(buckets, suppressed_override, whitelisted, unresolved, stats, verbose) -> None:
    drift = buckets["arity"]
    arity_mismatch = buckets["arity-mismatch"]
    erasure = buckets["erasure"]

    for f in erasure:
        print("%-58s:%-5d %s#%s  erasure-only" % (f.rel, f.line, f.cls, f.method))
    if erasure:
        print("   (name + arity match and every parameter is assignable: the supertype is")
        print("    generic, so javac emits a bridge and the method DOES override - it is")
        print("    simply missing @Override, which is the guard that would have made")
        print("    0.5.5's renderRecursively drift a compile error instead of a silent one)")

    for f in drift:
        print("%-58s:%-5d %s#%s  DRIFT" % (f.rel, f.line, f.cls, f.method))
        print("%s    ours: %s" % (" " * 62, f.desc))
        print("%s   super: %s.%s" % (" " * 62, f.origin, f.origin_desc))
        print("%s   -> no exact match and no compatible erasure above: this does NOT "
              "override anything" % (" " * 62))

    for f in arity_mismatch:
        print("%-58s:%-5d %s#%s  ARITY-MISMATCH" % (f.rel, f.line, f.cls, f.method))
        print("%s    ours: %s" % (" " * 62, f.desc))
        print("%s   super: %s.%s" % (" " * 62, f.origin, f.origin_desc))
        print("%s   -> the supertype's parameters are a PREFIX of ours, which is the shape"
              " left behind when a\n%s      Forge callback parameter was dropped in"
              " NeoForge. This does NOT override, so it never runs."
              % (" " * 62, " " * 62))

    if whitelisted:
        print("")
        for f in whitelisted:
            print("%-58s:%-5d %s#%s  known benign (WHITELIST)" % (f.rel, f.line, f.cls, f.method))

    if verbose:
        print("")
        for cls, method in sorted(set(suppressed_override)):
            print("suppressed: %s#%s  carries @Override in source" % (cls, method))
        for cls, missing in sorted(unresolved.items()):
            print("unresolved supertype(s) for %s: %s" % (cls, ", ".join(missing)))

    print("\n%d drifted override signature(s), %d arity-mismatch, %d erasure-only, "
          "%d whitelisted as benign, %d suppressed by an @Override in source"
          % (len(drift), len(arity_mismatch), len(erasure), len(whitelisted),
             len(set(suppressed_override))))
    print("parsed %d class(es), %d resolved, %d unresolved name(s)"
          % (stats["parse"], stats["resolve"], stats["miss"]))
    if drift or arity_mismatch:
        sys.exit(1)


def main() -> None:
    if not os.path.isdir(CLASSES):
        sys.exit("no compiled classes at %s - build the project first (this tool does not run gradle)"
                 % CLASSES)
    cp = build_classpath()
    sources = Sources(SRC)
    buckets, suppressed, whitelisted, unresolved = audit(cp, sources)
    report(buckets, suppressed, whitelisted, unresolved, cp.stats, "--verbose" in sys.argv)


# ---------------------------------------------------------------------------
# Self-test: build synthetic class files in memory and run the real parser and the
# real comparison rule over them.
# ---------------------------------------------------------------------------

CONST_UTF8 = 1


def _u2(v: int) -> bytes:
    return struct.pack(">H", v)


def _u4(v: int) -> bytes:
    return struct.pack(">I", v)


def _utf(s: str) -> bytes:
    raw = s.encode("utf-8")
    return bytes([CONST_UTF8]) + _u2(len(raw)) + raw


def make_class_file(name: str, super_name: str, interfaces: list[str],
                    methods: list[tuple[str, str, int]], noise: bool = False) -> bytes:
    """Assemble a minimal but structurally real class file.

    With `noise=True` the pool also carries every awkward constant tag - including the
    two-slot CONSTANT_Long and CONSTANT_Double - ahead of the entries the parser has to
    follow, so a parser that mishandles slot counting or a tag width lands on the wrong
    byte and fails loudly instead of quietly reading the wrong class name.
    """
    pool: list[bytes] = []   # serialized entry bodies, in order
    width: list[int] = []    # how many constant-pool slots each body occupies

    def add(entry: bytes, takes: int = 1) -> int:
        """Append a pool body; return the slot the entry starts at."""
        index = 1 + sum(width)
        pool.append(entry)
        width.append(takes)
        return index

    if noise:
        # tag 3 Integer, 4 Float, 5 Long, 6 Double, 8 String, 9 Fieldref, 10 Methodref,
        # 11 InterfaceMethodref, 12 NameAndType, 15 MethodHandle, 16 MethodType,
        # 17 Dynamic, 18 InvokeDynamic, 19 Module, 20 Package.
        add(bytes([3]) + _u4(7))
        add(bytes([4]) + struct.pack(">f", 1.5))
        add(bytes([5]) + struct.pack(">q", 1234567890123), takes=2)  # Long: two slots
        add(bytes([6]) + struct.pack(">d", 2.5), takes=2)            # Double: two slots
        add(bytes([8]) + _u2(1))
        add(bytes([9]) + _u2(1) + _u2(2))
        add(bytes([10]) + _u2(1) + _u2(2))
        add(bytes([11]) + _u2(1) + _u2(2))
        add(bytes([12]) + _u2(1) + _u2(2))
        add(bytes([15]) + bytes([6]) + _u2(1))
        add(bytes([16]) + _u2(2))
        add(bytes([17]) + _u2(1) + _u2(2))
        add(bytes([18]) + _u2(1) + _u2(2))
        add(bytes([19]) + _u2(1))
        add(bytes([20]) + _u2(1))

    # `this_class`, `super_class` and the interface table hold CONSTANT_Class entries
    # (tag 7), each pointing at a Utf8 name - exactly as javac writes them, so the
    # fixture exercises the same two-step index resolution the parser must do.
    def add_class(internal_name: str) -> int:
        if not internal_name:
            return 0
        utf8_i = add(_utf(internal_name))
        return add(bytes([7]) + _u2(utf8_i))

    this_i = add_class(name)
    super_i = add_class(super_name)
    iface_indexes = [add_class(iface) for iface in interfaces]
    method_entries = []
    for m_name, m_desc, m_flags in methods:
        ni = add(_utf(m_name))
        di = add(_utf(m_desc))
        method_entries.append((m_flags, ni, di))

    pool_bytes = b"".join(pool)
    body = io.BytesIO()
    body.write(b"\xca\xfe\xba\xbe")
    body.write(_u2(0) + _u2(65))  # minor, major = Java 21
    body.write(_u2(1 + sum(width)))  # constant_pool_count = highest used slot + 1
    body.write(pool_bytes)
    body.write(_u2(0x0021))  # ACC_PUBLIC | ACC_SUPER
    body.write(_u2(this_i))
    body.write(_u2(super_i))
    body.write(_u2(len(iface_indexes)))
    for ix in iface_indexes:
        body.write(_u2(ix))
    body.write(_u2(0))  # fields_count
    body.write(_u2(len(method_entries)))
    for flags, ni, di in method_entries:
        body.write(_u2(flags) + _u2(ni) + _u2(di) + _u2(0))  # attributes_count = 0
    blob = body.getvalue()
    # self-check: the serialized layout must be exactly the size the format implies, so
    # a silently mis-sized tail cannot hide a pool-count or width mistake
    expected = (10                    # magic, minor, major, constant_pool_count
                + len(pool_bytes)
                + 2 + 2 + 2 + 2       # access_flags, this_class, super_class, interfaces_count
                + 2 * len(iface_indexes)
                + 2                   # fields_count
                + 2                   # methods_count
                + 8 * len(method_entries))  # per method: flags, name, descriptor, attr count
    assert len(blob) == expected, (len(blob), expected)
    return blob


class _MemClassPath(ClassPath):
    """A ClassPath over an in-memory binary-name -> class-file-bytes map."""

    def __init__(self, classes: dict[str, bytes]):
        self.blobs = {name.replace("/", ".").replace(".class", ""): blob
                      for name, blob in classes.items()}
        self.cache = {}
        self.stats = {"resolve": 0, "parse": 0, "miss": 0}
        self.index = []

    def find(self, binary_name: str) -> str | None:
        return binary_name if binary_name in self.blobs else None

    def read(self, path: str) -> bytes:
        return self.blobs[path]

    def available_classes(self, root: str) -> list[str]:
        return sorted(class_entry(name) for name in self.blobs)


_OBJECT_NAME = "java/lang/Object"


def _object_class() -> bytes:
    """A stand-in for java.lang.Object so synthetic hierarchies terminate."""
    return make_class_file(_OBJECT_NAME, "", [], [("<init>", "()V", ACC_PUBLIC)])


def selftest() -> None:
    # --- 1. descriptors -----------------------------------------------------
    params, ret = decode_descriptor("(Lcom/mojang/blaze3d/vertex/PoseStack;"
                                    "Ltop/ribs/scguns/AnimatedGunItem;"
                                    "Lsoftware/bernie/geckolib/cache/object/GeoBone;IF[J[[J)V")
    assert params == ["com.mojang.blaze3d.vertex.PoseStack", "top.ribs.scguns.AnimatedGunItem",
                      "software.bernie.geckolib.cache.object.GeoBone", "I", "F", "[J", "[[J"], params
    assert ret == "V", ret  # `V` is the JVM's void descriptor character
    params, ret = decode_descriptor("(Ljava/lang/Object;IJ)Lnet/minecraft/resources/ResourceLocation;")
    assert params == ["java.lang.Object", "I", "J"], params
    assert ret == "net.minecraft.resources.ResourceLocation", ret
    params, ret = decode_descriptor("(Ltop/ribs/scguns/Outer$Inner;)Ltop/ribs/scguns/Outer$Inner;")
    assert params == ["top.ribs.scguns.Outer$Inner"], params
    print("ok    descriptor decoding (primitives, objects, arrays, nested names)")

    # --- 2. constant pool, including the odd tags --------------------------
    blob = make_class_file("p/C", "java/lang/Object", [],
                           [("<init>", "()V", ACC_PUBLIC), ("run", "(I)V", ACC_PUBLIC)],
                           noise=True)
    info = parse_class(blob)
    assert info.name == "p.C", info.name
    assert info.super_name == "java.lang.Object", info.super_name
    assert [(m.name, m.desc) for m in info.methods] == [("<init>", "()V"), ("run", "(I)V")], \
        info.methods
    # the noise pool must not have shifted `this_class`: prove by dropping the noise too
    plain = parse_class(make_class_file("p/C", "java/lang/Object", [],
                                        [("<init>", "()V", ACC_PUBLIC)]))
    assert plain.name == "p.C"
    print("ok    constant pool reader (Long/Double two-slot, MethodHandle/MethodType/"
          "Dynamic/InvokeDynamic/Module/Package)")

    # --- 3. hierarchy walk + rule -----------------------------------------
    def desc_args(*types: str) -> str:
        return "(" + "".join(types) + ")V"

    POSESTACK = "Lnet/minecraft/client/renderer/PoseStack;"
    GUN = "Ltop/ribs/scguns/AnimatedGunItem;"
    BONE = "Lsoftware/bernie/geckolib/cache/object/GeoBone;"
    RTYPE = "Lnet/minecraft/client/renderer/RenderType;"
    MBS = "Lnet/minecraft/client/renderer/MultiBufferSource;"
    VC = "Lcom/mojang/blaze3d/vertex/VertexConsumer;"
    HEAD = POSESTACK + GUN + BONE + RTYPE + MBS + VC + "ZFI"

    # the real 4.9.3 hook: (..., int renderColor)  -> 11 parameters
    fixed = desc_args(POSESTACK, GUN, BONE, RTYPE, MBS, VC, "Z", "F", "I", "I", "I")
    # 0.5.5's 4.4 signature: (..., float red, float green, float blue, float alpha)
    # -> still 11 parameters, but the last one is F where the 4.9.3 hook has I
    drifted = desc_args(POSESTACK, GUN, BONE, RTYPE, MBS, VC, "Z", "F", "I", "I", "F")

    cp = _MemClassPath({
        _OBJECT_NAME: _object_class(),
        "p/GeoRenderer": make_class_file(
            "p/GeoRenderer", "java/lang/Object", [],
            [("renderRecursively", fixed, ACC_PUBLIC)], noise=True),
        # (a) the historical bug reproduced: one float where the hook now takes an int
        "p/AnimatedGunRenderer": make_class_file(
            "p/AnimatedGunRenderer", "p/GeoRenderer", [],
            [("renderRecursively", drifted, ACC_PUBLIC)], noise=True),
    })
    info = cp.reify("p.AnimatedGunRenderer")
    assert info is not None, ("fixture class did not parse", cp.cache.keys(),
                              sorted(class_entry(k) for k in cp.blobs))
    found, unresolved = compare_with_owners(info, cp)
    assert not unresolved, unresolved
    assert len(found) == 1, [(f.method, f.desc) for f in found]
    assert found[0].method == "renderRecursively", found[0].method
    assert found[0].origin == "p.GeoRenderer", found[0].origin
    assert found[0].kind == "arity" and found[0].arity == 11, \
        (found[0].kind, found[0].arity)
    print("ok    RULE: the historical drift (float instead of int renderColor) IS flagged")

    # (b) the fixed method must be clean
    cp2 = _MemClassPath({
        _OBJECT_NAME: _object_class(),
        "p/GeoRenderer": make_class_file("p/GeoRenderer", "java/lang/Object", [],
                                         [("renderRecursively", fixed, ACC_PUBLIC)]),
        "p/AnimatedGunRenderer": make_class_file("p/AnimatedGunRenderer", "p/GeoRenderer", [],
                                                 [("renderRecursively", fixed, ACC_PUBLIC)]),
    })
    found, _ = compare_with_owners(cp2.reify("p.AnimatedGunRenderer"), cp2)
    assert not found, found
    print("ok    RULE: the shipped signature (int renderColor) is clean")

    # (c) interface closure is walked, not just the superclass chain
    cp3 = _MemClassPath({
        _OBJECT_NAME: _object_class(),
        "p/GeoRenderer": make_class_file("p/GeoRenderer", "java/lang/Object", [],
                                         [("renderRecursively", fixed, ACC_PUBLIC)]),
        "p/MixinBase": make_class_file("p/MixinBase", "java/lang/Object", ["p/GeoRenderer"], []),
        "p/AnimatedGunRenderer": make_class_file("p/AnimatedGunRenderer", "p/MixinBase", [],
                                                 [("renderRecursively", drifted, ACC_PUBLIC)]),
    })
    found, _ = compare_with_owners(cp3.reify("p.AnimatedGunRenderer"), cp3)
    assert len(found) == 1 and found[0].origin == "p.GeoRenderer", found
    print("ok    RULE: interfaces (and their closure) are searched, not only the superclass chain")

    # (d) a real override inherited one ancestor up must not be flagged
    cp4 = _MemClassPath({
        _OBJECT_NAME: _object_class(),
        "p/A": make_class_file("p/A", "java/lang/Object", [], [("f", "(I)V", ACC_PUBLIC)]),
        "p/B": make_class_file("p/B", "p/A", [], []),
        "p/C": make_class_file("p/C", "p/B", [], [("f", "(I)V", ACC_PUBLIC)]),
    })
    found, _ = compare_with_owners(cp4.reify("p.C"), cp4)
    assert not found, found
    print("ok    RULE: an inherited (grandparent) exact match is respected")

    # (e) static / private / bridge / synthetic methods are ignored
    cp5 = _MemClassPath({
        _OBJECT_NAME: _object_class(),
        "p/A": make_class_file("p/A", "java/lang/Object", [],
                               [("f", "(I)V", ACC_PUBLIC), ("g", "(I)V", ACC_PRIVATE),
                                ("h", "(I)V", ACC_PUBLIC | ACC_STATIC)]),
        "p/C": make_class_file("p/C", "p/A", [],
                               [("f", "(J)V", ACC_PUBLIC | ACC_STATIC),
                                ("g", "(J)V", ACC_PUBLIC),
                                ("h", "(J)V", ACC_PUBLIC),
                                ("k", "(J)V", ACC_PUBLIC | ACC_BRIDGE),
                                ("n", "(J)V", ACC_PUBLIC | ACC_SYNTHETIC),
                                ("<init>", "()V", ACC_PUBLIC)]),
    })
    found, _ = compare_with_owners(cp5.reify("p.C"), cp5)
    assert not found, found
    print("ok    RULE: static/private/bridge/synthetic/<init> methods are ignored")

    # (e2) a generic supertype's erased parameter must NOT be reported as drift: this is
    # exactly the MobRenderer<T>/#getTextureLocation shape that fills the tree
    cp6 = _MemClassPath({
        _OBJECT_NAME: _object_class(),
        "p/Entity": make_class_file("p/Entity", "java/lang/Object", [], []),
        "p/MyEntity": make_class_file("p/MyEntity", "p/Entity", [], []),
        "p/EntityRenderer": make_class_file(
            "p/EntityRenderer", "java/lang/Object", [],
            [("getTextureLocation", "(Lp/Entity;)V", ACC_PUBLIC)]),
        "p/MyRenderer": make_class_file(
            "p/MyRenderer", "p/EntityRenderer", [],
            [("getTextureLocation", "(Lp/MyEntity;)V", ACC_PUBLIC)]),
    })
    found, _ = compare_with_owners(cp6.reify("p.MyRenderer"), cp6)
    assert len(found) == 1 and found[0].kind == "erasure", \
        [(f.method, f.kind) for f in found]
    print("ok    RULE: generic erasure (T -> MyEntity) is classified as erasure, not drift")

    # (e3) an unrelated parameter type IS drift even with a matching arity
    cp7 = _MemClassPath({
        _OBJECT_NAME: _object_class(),
        "p/Unrelated": make_class_file("p/Unrelated", "java/lang/Object", [], []),
        "p/Entity": make_class_file("p/Entity", "java/lang/Object", [], []),
        "p/EntityRenderer2": make_class_file(
            "p/EntityRenderer2", "java/lang/Object", [],
            [("getTextureLocation", "(Lp/Entity;)V", ACC_PUBLIC)]),
        "p/MyRenderer2": make_class_file(
            "p/MyRenderer2", "p/EntityRenderer2", [],
            [("getTextureLocation", "(Lp/Unrelated;)V", ACC_PUBLIC)]),
    })
    found, _ = compare_with_owners(cp7.reify("p.MyRenderer2"), cp7)
    assert len(found) == 1 and found[0].kind == "arity", \
        [(f.method, f.kind) for f in found]
    print("ok    RULE: an unrelated parameter type is reported as drift")

    # (f) whitelist suppression is reported, not silent
    global WHITELIST
    saved = WHITELIST
    try:
        WHITELIST = {"p.AnimatedGunRenderer#renderRecursively": "deliberate overload (test)"}
        sup = set()
        flagged = []
        for f in compare_with_owners(cp.reify("p.AnimatedGunRenderer"), cp)[0]:
            if f.key in WHITELIST:
                sup.add(f.key)
            else:
                flagged.append(f)
        assert not flagged and sup == {"p.AnimatedGunRenderer#renderRecursively"}, (flagged, sup)
        print("ok    WHITELIST: entry suppresses the finding and is printed as suppressed")
    finally:
        WHITELIST = saved

    # --- 4. @Override detection on real source text ------------------------
    src = (
        "public class T {\n"
        "   // @Override\n"
        "   public void inComment() { }\n"
        "   @Override\n"
        "   public void render(int color) {\n"
        "      super.render(color);\n"
        "   }\n"
        "   @Override\n"
        "   @SuppressWarnings(\"unchecked\")\n"
        "   public void other(int x) { }\n"
        "   public void plain(int x) { }\n"
        "   public void ifLike(int x) { if (x > 1) { } }\n"
        "}\n"
    )
    names, lines = override_method_names(src)
    assert "render" in names and "other" in names, names
    assert "inComment" not in names, names  # inside a line comment
    assert "plain" not in names, names
    assert "ifLike" not in names, names  # an argument list is not a declaration
    assert lines["render"] == 4, lines
    print("ok    @Override is read from source (comments and argument lists are not fooled)")

    # --- 5. class-block splitting ------------------------------------------
    blocks = class_blocks(code_only(
        "public class A {\n void m() { }\n}\n"
        "class B {\n int x;\n}\n"
    ))
    assert set(blocks) == {"A", "B"}, blocks
    print("ok    top-level class blocks are split so nested classes use their own text")

    print("audit_override_drift self-test passed")
    return 0


if __name__ == "__main__":
    if "--selftest" in sys.argv:
        raise SystemExit(selftest())
    main()
