"""Forty-third-stage: cross-package reconciliation after the parallel sweep.

Errors whose correct fix spans packages, so no single package worker settles them
consistently:

  * NeoForge's `DeferredHolder<R, T>` takes the REGISTRY type as its first type
    argument. The blanket `RegistryObject<X>` -> `DeferredHolder<X, X>` rewrite
    produced `DeferredHolder<MenuType<T>, MenuType<T>>`, which NeoForge's
    `DeferredRegister#register` cannot satisfy: it returns
    `DeferredHolder<MenuType<?>, MenuType<T>>`.
  * `Holder#get()` does not exist in 1.21 - the accessor is `value()`.
    `SoundEvents` constants are `Holder.Reference<SoundEvent>`.

Both rules are idempotent: re-running must report 0 changes.
`python tools/port_rewrite43.py --selftest` exercises the rules on samples.

usage: python tools/port_rewrite43.py <error-log>
"""
from __future__ import annotations

import collections
import os
import re
import sys

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")
ERR = re.compile(r"([A-Za-z]:\\[^:]+\.java):(\d+): error: (.*)$")
LOC_VAR = re.compile(r"location:\s+variable\s+(\w+)\s+of type\s+(.+?)\s*$")

# DeferredHolder<A<B>, A<B>>  ->  DeferredHolder<A<?>, A<B>>
NESTED_HOLDER = re.compile(
    r"\bDeferredHolder<\s*(\w+)\s*<\s*([^<>]+?)\s*>\s*,\s*\1\s*<\s*\2\s*>\s*>")
# <T extends X> ... DeferredHolder<T, T>  ->  DeferredHolder<X, T>
TYPEVAR_HOLDER = re.compile(
    r"<\s*(\w+)\s+extends\s+([\w.$]+)\s*>([^;{}]{0,240}?)\bDeferredHolder<\s*(\w+)\s*,\s*\4\s*>")


def fix_holder_generics(text: str, stats: dict) -> str:
    def nested(m: re.Match) -> str:
        raw = m.group(0)
        fixed = "DeferredHolder<%s<?>, %s<%s>>" % (m.group(1), m.group(1), m.group(2))
        if fixed == raw:
            return raw
        stats["DeferredHolder registry type"] = stats.get("DeferredHolder registry type", 0) + 1
        return fixed

    def typevar(m: re.Match) -> str:
        raw, tvar, bound, between, inner = m.group(0), m.group(1), m.group(2), m.group(3), m.group(4)
        if inner != tvar:
            return raw
        # `between` only spans the gap: the DeferredHolder<...> itself is the
        # pattern tail, so it has to be rebuilt rather than searched for.
        stats["DeferredHolder type var"] = stats.get("DeferredHolder type var", 0) + 1
        return "<%s extends %s>%sDeferredHolder<%s, %s>" % (tvar, bound, between, bound, tvar)

    text = NESTED_HOLDER.sub(nested, text)
    return TYPEVAR_HOLDER.sub(typevar, text)


# A file that unwraps its holder through an explicit `(DeferredHolder<X<T>, X<T>>) (Object)`
# cast is internally consistent: widening the declared type would make that cast's result
# no longer assignable, so such files must be left for a human. `init/ModContainers.java`
# and `init/ModEntities.java` use exactly that shape.
CAST_GUARD = re.compile(r"\(DeferredHolder<[^()]*>\)\s*\(Object\)")


def guard_skip(text: str) -> bool:
    return CAST_GUARD.search(text) is not None


def collect_holder_get(log: str) -> dict[str, dict[int, list[str]]]:
    """file -> line -> variables whose `.get()` is really a `Holder#value()`."""
    raw = open(log, "rb").read()
    encoding = "utf-16" if raw[:2] in (b"\xff\xfe", b"\xfe\xff") else "utf-8"
    lines = raw.decode(encoding, "replace").splitlines()
    out: dict[str, dict[int, list[str]]] = collections.defaultdict(lambda: collections.defaultdict(list))
    for i, line in enumerate(lines):
        m = ERR.search(line)
        if not m or "cannot find symbol" not in m.group(3):
            continue
        block = lines[i:i + 6]
        if not any("symbol:" in b and "method get()" in b for b in block):
            continue
        for b in block:
            lm = LOC_VAR.search(b)
            if not lm:
                continue
            if "Holder" in lm.group(2) or "Reference" in lm.group(2):
                out[os.path.normpath(m.group(1))][int(m.group(2))].append(lm.group(1))
            break
    return out


def apply_holder_get(log: str, stats: dict) -> int:
    changed_files = 0
    for path, per_line in collect_holder_get(log).items():
        if not os.path.isfile(path):
            continue
        text = open(path, encoding="utf-8", errors="replace").read()
        lines = text.split("\n")
        dirty = False
        for number, names in per_line.items():
            idx = number - 1
            if not (0 <= idx < len(lines)):
                continue
            original = lines[idx]
            new = original
            for name in names:
                new = re.sub(r"\b%s\.get\(\)" % re.escape(name), "%s.value()" % name, new)
            if new != original:
                lines[idx] = new
                dirty = True
                stats["Holder.get() -> value()"] = stats.get("Holder.get() -> value()", 0) + 1
        if dirty:
            with open(path, "w", encoding="utf-8", newline="") as fh:
                fh.write("\n".join(lines))
            changed_files += 1
    return changed_files


def main() -> None:
    # `--only <substring>` (repeatable) restricts the sweep to matching paths, so
    # the rules can be applied to a subtree while other workers still own the rest.
    file_filter = [sys.argv[i + 1] for i, a in enumerate(sys.argv) if a == "--only"]
    files = [os.path.join(d, f) for d, _, fs in os.walk(SRC) for f in fs if f.endswith(".java")]
    if file_filter:
        files = [p for p in files if any(sub in p.replace("\\", "/") for sub in file_filter)]
    total: dict = {}
    changed = 0
    skipped: list[str] = []
    for path in files:
        text = open(path, encoding="utf-8", errors="replace").read()
        if guard_skip(text):
            skipped.append(os.path.relpath(path, SRC).replace("\\", "/"))
            continue
        new = fix_holder_generics(text, total)
        if new != text:
            with open(path, "w", encoding="utf-8", newline="") as fh:
                fh.write(new)
            changed += 1
    log = [a for a in sys.argv[1:] if not a.startswith("--")]
    if log and os.path.isfile(log[0]):
        changed += apply_holder_get(log[0], total)
    print(f"rewrote {changed} files")
    for k, v in sorted(total.items(), key=lambda kv: -kv[1]):
        print(f"{v:6d}  {k}")
    for rel in skipped:
        print(f"  SKIPPED (explicit (Object) cast unwrap, fix by hand): {rel}")


SELFTEST = [
    # (input, expected)
    ("public static final DeferredHolder<MenuType<AttachmentContainer>, MenuType<AttachmentContainer>> X;",
     "public static final DeferredHolder<MenuType<?>, MenuType<AttachmentContainer>> X;"),
    ("private static <T extends AbstractContainerMenu> DeferredHolder<MenuType<T>, MenuType<T>> register(String id, MenuSupplier<T> f) {",
     "private static <T extends AbstractContainerMenu> DeferredHolder<MenuType<?>, MenuType<T>> register(String id, MenuSupplier<T> f) {"),
    ("private static <T extends Entity> DeferredHolder<EntityType<T>, EntityType<T>> registerBasic(String id, BiFunction<EntityType<T>, Level, T> f) {",
     "private static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> registerBasic(String id, BiFunction<EntityType<T>, Level, T> f) {"),
    ("private static <T extends Block> DeferredHolder<T, T> registerBurnable(String id, Supplier<T> s, int t) {",
     "private static <T extends Block> DeferredHolder<Block, T> registerBurnable(String id, Supplier<T> s, int t) {"),
    # already-correct forms must be left alone
    ("public static final DeferredHolder<Block, Block> EXAMPLE = REGISTER.register(\"example\", Block::new);", None),
    ("private static <T extends Block> DeferredHolder<Block, T> register(String id, Supplier<T> s) {", None),
]


def selftest() -> None:
    for text, expected in SELFTEST:
        fixed = fix_holder_generics(text, {})
        want = text if expected is None else expected
        status = "ok  " if fixed == want else "FAIL"
        print(f"{status} {fixed}")
        assert fixed == want, "expected %r got %r" % (want, fixed)
        # idempotency
        assert fix_holder_generics(fixed, {}) == fixed, "not idempotent: %r" % fixed

    # the cast-unwrap guard must trip on the init/ModContainers shape
    guarded = ("      DeferredHolder<MenuType<?>, MenuType<T>> holder = REGISTER.register(\n"
               "      return (DeferredHolder<MenuType<T>, MenuType<T>>) (Object) holder;\n")
    assert guard_skip(guarded), "cast-unwrap guard did not trip"
    assert not guard_skip(SELFTEST[0][0]), "guard tripped on a file without the cast"
    print("ok   cast-unwrap guard trips (and stays clear on ordinary files)")
    print("port_rewrite43 self-test passed (and is idempotent)")


if __name__ == "__main__":
    if "--selftest" in sys.argv:
        selftest()
    else:
        main()
