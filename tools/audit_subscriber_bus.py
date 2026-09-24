"""Does every `@EventBusSubscriber` class declare the bus its listeners actually belong to?

NeoForge has two buses and events declare which one they belong to (`IModBusEvent`) - registering a
mod-bus event on the game bus throws:

    java.lang.IllegalArgumentException: IModBusEvent events are not allowed on the common NeoForge bus!
    Use a mod bus instead.

Old and new FML disagree about who is responsible for getting that right:

  * FML 4.0.38 (shipped with NeoForge 21.1.150): `AutomaticEventSubscriber` reads the annotation's
    `bus` (default GAME) and registers the WHOLE class there. A class that listens to a mod-bus event
    without saying `bus = Bus.MOD` therefore aborts mod loading.
  * FML 4.0.44 (NeoForge 21.1.249): it inspects the listeners and routes them per event type -
    "Found mix of game bus and mod bus listeners in @EventBusSubscriber class {}, registering them
    separately" - so the same class works there and the mistake stays invisible.

`ModCapabilities` was exactly that: `@EventBusSubscriber(modid = "scguns")` (GAME) with a
`RegisterCapabilitiesEvent` (MOD) listener. It ran fine on 21.1.249+ and crashed on 21.1.150.

Usage:
    python tools/audit_subscriber_bus.py
"""

import glob
import os
import pathlib
import re
import struct
import sys
import zipfile

SRC = pathlib.Path("src/main/java")
CLASSES = pathlib.Path("build/classes/java/main")
MERGED_GLOBS = ("build/moddev/artifacts/neoforge-*-merged.jar",)
LIB_GLOBS = ("libs/*.jar",)

ANN = re.compile(r"@EventBusSubscriber\s*\(")
BARE_ANN = re.compile(r"@EventBusSubscriber\b(?!\s*\()")
DECL = re.compile(r"\b(?:class|interface|enum|record)\s+(\w+)")
LINE_COMMENT = re.compile(r"//[^\n]*")
BLOCK_COMMENT = re.compile(r"/\*.*?\*/", re.S)
STRING = re.compile(r'"(?:\\.|[^"\\])*"')
CHARLIT = re.compile(r"'(?:\\.|[^'\\])*'")
PARAM = re.compile(r"\(\s*(?:final\s+)?([\w.]+)\s*(?:<[^>]*>)?\s+\w+\s*[,)]")
IMODBUS = "net/neoforged/fml/event/IModBusEvent"


def blank_out(text):
    def blank(m):
        return re.sub(r"[^\n]", " ", m.group(0))
    text = BLOCK_COMMENT.sub(blank, text)
    text = LINE_COMMENT.sub(blank, text)
    text = STRING.sub(lambda m: " " * len(m.group(0)), text)
    return CHARLIT.sub(lambda m: " " * len(m.group(0)), text)


def match_forward(text, index):
    depth = 0
    for i in range(index, len(text)):
        if text[i] in "([{":
            depth += 1
        elif text[i] in ")]}":
            depth -= 1
            if depth == 0:
                return i
    return -1


# ---------------------------------------------------------------- class file reading


def _parse_class(data):
    """-> (this_name, super_name, [interface names]) without a full constant pool decode."""
    if data[:4] != b"\xca\xfe\xba\xbe":
        return None
    count = struct.unpack_from(">H", data, 8)[0]
    off = 10
    pool = {}
    i = 1
    while i < count:
        tag = data[off]
        off += 1
        if tag == 1:
            length = struct.unpack_from(">H", data, off)[0]
            pool[i] = data[off + 2:off + 2 + length].decode("utf-8", "replace")
            off += 2 + length
        elif tag in (7, 8, 16, 19, 20):
            pool[i] = struct.unpack_from(">H", data, off)[0]
            off += 2
        elif tag in (15,):
            off += 3
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18):
            off += 4
        elif tag in (5, 6):
            off += 8
            i += 1
        else:
            return None
        i += 1
    off += 2  # access flags
    this_i = struct.unpack_from(">H", data, off)[0]
    off += 2
    super_i = struct.unpack_from(">H", data, off)[0]
    off += 2
    n_if = struct.unpack_from(">H", data, off)[0]
    off += 2
    interfaces = []
    for _ in range(n_if):
        interfaces.append(pool.get(pool.get(struct.unpack_from(">H", data, off)[0], -1), None))
        off += 2

    def name(index):
        return pool.get(pool.get(index, -1), None)

    return name(this_i), name(super_i), interfaces


class Index:
    """Everything we may need to resolve an event type, keyed by simple name."""

    def __init__(self):
        self.by_simple = {}
        self.cache = {}
        self.containers = []

    def add_dir(self, root):
        self.containers.append(("dir", pathlib.Path(root)))

    def add_jar(self, jar):
        self.containers.append(("jar", pathlib.Path(jar)))

    def _entries(self):
        for kind, path in self.containers:
            if kind == "dir":
                for f in path.rglob("*.class"):
                    yield f.relative_to(path).as_posix(), f.read_bytes()
            else:
                # Read eagerly: the zip is closed when this generator is exhausted, and the index is
                # used afterwards (keeping a lambda over a closed ZipFile silently yielded None for
                # every NeoForge event type).
                with zipfile.ZipFile(path) as z:
                    for name in z.namelist():
                        if name.endswith(".class"):
                            yield name, z.read(name)

    def build(self):
        for name, data in self._entries():
            base = name.rsplit("/", 1)[-1].split(".")[0]
            # Nested events are used by their inner name in source (`ModelEvent.ModifyBakingResult`
            # is stored as `ModelEvent$ModifyBakingResult.class`).
            keys = {base, base.rsplit("$", 1)[-1]}
            for simple in keys:
                if simple in self.by_simple and not name.startswith("net/neoforged/"):
                    continue
                self.by_simple[simple] = data

    def _info(self, data):
        key = id(data)
        if key not in self.cache:
            try:
                self.cache[key] = _parse_class(data)
            except Exception:
                self.cache[key] = None
        return self.cache[key]

    def is_mod_bus(self, simple_name, seen=None):
        """True when the event type (or an ancestor) implements IModBusEvent."""
        seen = seen or set()
        if simple_name in seen:
            return False
        seen.add(simple_name)
        data = self.by_simple.get(simple_name)
        if data is None:
            return None  # unknown type
        parsed = self._info(data)
        if not parsed:
            return None
        _, parent, interfaces = parsed
        names = [n for n in ([parent] + list(interfaces)) if n]
        if IMODBUS in names:
            return True
        for n in names:
            result = self.is_mod_bus(n.rsplit("/", 1)[-1], seen)
            if result:
                return True
        return False


def handlers(body, brace, end):
    """[(method name, first parameter type simple name)] for @SubscribeEvent methods of this class."""
    out = []
    i, depth = brace + 1, 1
    while i < end and depth >= 1:
        ch = body[i]
        if ch == "{":
            depth += 1
        elif ch == "}":
            depth -= 1
        elif depth == 1 and body.startswith("@SubscribeEvent", i):
            head_end = body.find("{", i)
            head = body[i:head_end if head_end > 0 else i + 400]
            name = re.search(r"\b(\w+)\s*\(", head)
            param = PARAM.search(head)
            out.append((name.group(1) if name else "?",
                        param.group(1).rsplit(".", 1)[-1] if param else "?"))
            i = head_end if head_end > 0 else i + len("@SubscribeEvent")
            continue
        i += 1
    return out


def main():
    merged = sorted(p for pattern in MERGED_GLOBS for p in glob.glob(pattern))
    if not merged:
        print("no %s - run 'gradlew compileJava' once so the dev artifacts exist" % MERGED_GLOBS[0])
        return 2
    index = Index()
    index.add_jar(merged[-1])
    index.add_dir(CLASSES)
    for pattern in LIB_GLOBS:
        for jar in glob.glob(pattern):
            index.add_jar(jar)
    # The compile classpath also carries the FancyModLoader and bus jars, which hold the lifecycle /
    # mod-bus events themselves (FMLClientSetupEvent, ...) - without them those listeners look
    # unresolvable and the class would be skipped silently.
    classpath = pathlib.Path("build-logs/compile-classpath.txt")
    if classpath.is_file():
        for entry in classpath.read_text(encoding="utf-8", errors="replace").split(";"):
            entry = entry.strip()
            if entry.endswith(".jar") and pathlib.Path(entry).is_file():
                index.add_jar(entry)
    index.build()

    problems = []
    unknown_types = set()
    checked = 0
    for path in sorted(SRC.rglob("*.java")):
        raw = path.read_text(encoding="utf-8", errors="replace")
        if "@EventBusSubscriber" not in raw:
            continue
        body = blank_out(raw)
        matchers = [(m.start(), m.end()) for m in ANN.finditer(body)]
        matchers += [(m.start(), m.end()) for m in BARE_ANN.finditer(body)]
        for start, _ in matchers:
            open_paren = body.find("(", start)
            if open_paren >= 0:
                close = match_forward(body, open_paren)
                if close < 0:
                    continue
                args = body[open_paren + 1:close]
            else:
                args = ""
            declared = "MOD" if re.search(r"\bMOD\b", args) else "GAME"
            decl = DECL.search(body, start)
            if not decl:
                continue
            brace = body.find("{", decl.end())
            if brace < 0:
                continue
            end = match_forward(body, brace)
            hs = handlers(body, brace, end)
            if not hs:
                continue
            checked += 1
            kinds = {}
            for name, param in hs:
                mod = index.is_mod_bus(param)
                if mod is None:
                    unknown_types.add(param)
                else:
                    kinds.setdefault("MOD" if mod else "GAME", []).append(name)
            for bus, names in sorted(kinds.items()):
                if bus != declared:
                    problems.append(
                        "%s: @EventBusSubscriber bus=%s (default is GAME) but these listeners belong to"
                        " the %s bus: %s" % (path.as_posix()[len("src/main/java/"):].replace("/", "\\"),
                                             declared, bus, ", ".join(sorted(set(names)))))

    for problem in sorted(problems):
        print("  %s" % problem)
    if unknown_types:
        print("\n(unresolved event type(s), not classified: %s)" % ", ".join(sorted(unknown_types)))
    if problems:
        print("\n%d subscriber class(es) declare the wrong bus - FML 4.0.38 (NeoForge 21.1.150) registers"
              " the whole class on the declared bus and aborts mod loading" % len(problems))
        return 1
    print("0 problem(s): %d annotated subscriber class(es) declare the bus their listeners need"
          % checked)
    return 0


if __name__ == "__main__":
    sys.exit(main())
