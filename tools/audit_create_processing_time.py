"""Every Create recipe must carry a processing_time only if its type can accept one.

Create 6 reads `processing_time` through one shared codec with a default of **zero**
(`ProcessingRecipeParams`: `Codec.INT.optionalFieldOf("processing_time", 0)`), and then validates
it: `ProcessingRecipe#validate` drops any recipe that has the key while `canSpecifyDuration()` is
false, with `Recipe specified a duration. Durations have no impact on this type of recipe.`

`canSpecifyDuration()` is false by default and overridden in exactly three places -
`AbstractCrushingRecipe`, `processing/basin/BasinRecipe`, `kinetics/saw/CuttingRecipe` - so the
set of types that accept the key is a property of the class hierarchy, and this audit reads that
hierarchy out of the Create jar rather than trusting a table. That is the whole point: a hardcoded
list is exactly what produced 45 dropped recipes the first time this was written by hand.

Two failures, both of which have actually happened here:

  * a type that accepts the key with no key present => 0 ticks, silently. That was the 14
    `create:mixing` recipes: every blend in the mod completed instantly. 0.5.5 shipped the same
    data, so it was never a port regression - it just finally got noticed.
  * a type that rejects the key carrying one => the recipe is gone, with a `Parsing error loading
    recipe` line per entry. Measured: 16 `create:deploying` + 26 `create:sequenced_assembly` +
    3 `create:splashing` = 45, plus one `create:filling` that only shows the error when Create AEI
    is installed, since it is gated on that mod.

Usage: python tools/audit_create_processing_time.py [--selftest]
"""
from __future__ import annotations

import json
import pathlib
import sys
import zipfile

ROOT = pathlib.Path(r"E:\mod\scgun-0.5.5-1.21.1-neoforge")
RECIPES = ROOT / "src/main/resources/data/scguns/recipe/create"
CREATE_JAR = ROOT / "libs/create-1.21.1-6.0.10.jar"

# Where Create overrides the default, i.e. which hierarchy roots make a duration meaningful.
DURATION_ROOTS = (
    "com/simibubi/create/content/kinetics/crusher/AbstractCrushingRecipe",
    "com/simibubi/create/content/processing/basin/BasinRecipe",
    "com/simibubi/create/content/kinetics/saw/CuttingRecipe",
)

# Type -> the Create class that implements it. Resolved in the jar and walked up to its superclasses
# to decide acceptance, so a Create update that moves the override fails this audit.
TYPE_CLASSES = {
    "create:crushing": "com/simibubi/create/content/kinetics/crusher/CrushingRecipe",
    "create:milling": "com/simibubi/create/content/kinetics/millstone/MillingRecipe",
    "create:mixing": "com/simibubi/create/content/kinetics/mixer/MixingRecipe",
    "create:compacting": "com/simibubi/create/content/kinetics/mixer/CompactingRecipe",
    "create:cutting": "com/simibubi/create/content/kinetics/saw/CuttingRecipe",
    "create:pressing": "com/simibubi/create/content/kinetics/press/PressingRecipe",
    "create:filling": "com/simibubi/create/content/fluids/transfer/FillingRecipe",
    "create:splashing": "com/simibubi/create/content/kinetics/fan/processing/SplashingRecipe",
    "create:deploying": "com/simibubi/create/content/kinetics/deployer/ItemApplicationRecipe",
    "create:mechanical_crafting":
        "com/simibubi/create/content/kinetics/crafter/MechanicalCraftingRecipe",
}

# Types with no duration concept at all: their codec never reads the key, so there is nothing to
# check and nothing to write. `mechanical_crafting` is here because Create 6 removed the field.
NO_DURATION_FIELD = frozenset({"create:mechanical_crafting"})

# The values the fixer writes, and the evidence for each. The audit checks the *acceptance*
# against the jar; the values are only reported, so a balance change needs no edit here.
EXPECTED_TICKS = {
    "create:crushing": 350,
    "create:milling": 50,
    "create:mixing": 100,
    "create:cutting": 50,
}

def jar_classes():
    """internal class name -> class file bytes, for the Create jar."""
    if not CREATE_JAR.exists():
        return None
    out = {}
    with zipfile.ZipFile(CREATE_JAR) as archive:
        for name in archive.namelist():
            if name.endswith(".class"):
                out[name[:-len(".class")]] = archive.read(name)
    return out


def constant_pool(data: bytes):
    """[(tag, payload)] for a class file's constant pool, plus the offset just past it.

    Needed to reach the `super_class` field, which sits immediately after the pool: the class file
    header is magic(4) minor(2) major(2) count(2), then the pool, then access_flags(2),
    this_class(2), super_class(2). Walking the pool by hand is the only way to find that offset
    without a bytecode library, and the entry sizes are fixed per tag.
    """
    count = int.from_bytes(data[8:10], "big")
    offset = 10
    entries = []
    index = 1
    while index < count:
        tag = data[offset]
        offset += 1
        if tag == 1:  # Utf8
            length = int.from_bytes(data[offset:offset + 2], "big")
            payload = data[offset + 2:offset + 2 + length]
            offset += 2 + length
        elif tag in (7, 8, 16, 19, 20):  # Class, String, MethodType, Module, Package
            payload = data[offset:offset + 2]
            offset += 2
        elif tag == 15:  # MethodHandle
            payload = data[offset:offset + 3]
            offset += 3
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18):  # Integer, Float, *ref, NameAndType, Dynamic
            payload = data[offset:offset + 4]
            offset += 4
        elif tag in (5, 6):  # Long, Double - each takes two pool slots
            payload = data[offset:offset + 8]
            offset += 8
            entries.append((tag, payload))
            entries.append(None)
            index += 2
            continue
        else:
            raise AssertionError("unknown constant pool tag %d" % tag)
        entries.append((tag, payload))
        index += 1
    return entries, offset


def superclass(classes, name):
    """The superclass of `name` as a dotted class name, or None (for Object and interfaces)."""
    data = classes.get(name)
    if data is None:
        return None
    entries, pool_end = constant_pool(data)
    index = int.from_bytes(data[pool_end + 4:pool_end + 6], "big")
    if index == 0:
        return None
    entry = entries[index - 1]
    if entry is None or entry[0] != 7:
        return None
    name_index = int.from_bytes(entry[1], "big")
    name_entry = entries[name_index - 1]
    if name_entry is None or name_entry[0] != 1:
        return None
    return name_entry[1].decode("utf-8", "replace").replace("/", ".")


def accepts_duration(classes, class_name):
    """True when `class_name` inherits an override of canSpecifyDuration()."""
    seen = set()
    current = class_name
    while current and current not in seen:
        seen.add(current)
        internal = current.replace(".", "/")
        if internal in DURATION_ROOTS:
            return True
        if internal not in classes:
            return False
        current = superclass(classes, internal)
    return False



def ours_recipes():
    """Every recipe object in our Create data, as (path, type, ticks or None, is_step)."""
    out = []
    for path in sorted(RECIPES.rglob("*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        out.append((path, data.get("type", ""), data.get("processing_time"), False))
        for step in data.get("sequence", []) or []:
            if isinstance(step, dict):
                out.append((path, step.get("type", ""), step.get("processing_time"), True))
    return out


def main() -> int:
    classes = jar_classes()
    if classes is None:
        print("FAIL: %s is missing, so the acceptance table cannot be checked" % CREATE_JAR.name)
        return 1

    problems: list[str] = []
    accepts = {}
    for recipe_type, class_name in sorted(TYPE_CLASSES.items()):
        accepts[recipe_type] = accepts_duration(classes, class_name.replace(".", "/"))
        print("  %-28s %-6s (%s)"
              % (recipe_type, "accepts" if accepts[recipe_type] else "rejects",
                 class_name.rsplit(".", 1)[-1]))

    counts: dict[str, int] = {}
    for path, recipe_type, ticks, is_step in ours_recipes():
        if recipe_type in NO_DURATION_FIELD or recipe_type not in accepts:
            if ticks is not None and recipe_type not in NO_DURATION_FIELD:
                problems.append("%s has processing_time but %r is not a known Create recipe type"
                                % (path.name, recipe_type))
            continue
        counts[recipe_type] = counts.get(recipe_type, 0) + 1
        if accepts[recipe_type] and ticks is None:
            problems.append("%s (%s%s) has no processing_time => 0 ticks; Create's default is 0"
                            % (path.name, recipe_type, " step" if is_step else ""))
        elif not accepts[recipe_type] and ticks is not None:
            problems.append("%s (%s%s) has processing_time %d, which Create rejects: the recipe is "
                            "dropped at load with 'Durations have no impact on this type of recipe'"
                            % (path.name, recipe_type, " step" if is_step else "", ticks))

    print("")
    for recipe_type in sorted(counts):
        want = EXPECTED_TICKS.get(recipe_type)
        print("  %-28s %d recipe(s), value %s" % (recipe_type, counts[recipe_type], want or "n/a"))

    print("")
    for problem in problems:
        print("BROKEN %s" % problem)
    if problems:
        print("\n%d problem(s)" % len(problems))
        return 1
    print("0 problem(s): every duration-capable Create recipe has a real processing_time, and no "
          "recipe carries one its type would reject")
    return 0


def selftest() -> int:
    """The classification is the whole audit, so check it against the three roots and a default."""
    classes = jar_classes()
    failures = []
    if classes is None:
        print("SELFTEST FAIL: %s missing" % CREATE_JAR.name)
        return 1
    expectations = {
        "com/simibubi/create/content/kinetics/crusher/CrushingRecipe": True,
        "com/simibubi/create/content/kinetics/millstone/MillingRecipe": True,
        "com/simibubi/create/content/kinetics/mixer/MixingRecipe": True,
        "com/simibubi/create/content/kinetics/saw/CuttingRecipe": True,
        "com/simibubi/create/content/kinetics/press/PressingRecipe": False,
        "com/simibubi/create/content/fluids/transfer/FillingRecipe": False,
        "com/simibubi/create/content/kinetics/fan/processing/SplashingRecipe": False,
        "com/simibubi/create/content/kinetics/deployer/ItemApplicationRecipe": False,
    }
    for class_name, want in sorted(expectations.items()):
        got = accepts_duration(classes, class_name)
        if got != want:
            failures.append("%s: classified %s, expected %s"
                            % (class_name.rsplit("/", 1)[-1],
                               "accepts" if got else "rejects",
                               "accepts" if want else "rejects"))
    problems = main()
    if problems != 0:
        failures.append("the tree itself is not clean")
    for failure in failures:
        print("SELFTEST FAIL:", failure)
    print("selftest: %d failure(s)" % len(failures))
    return 1 if failures else 0


if __name__ == "__main__":
    if "--selftest" in sys.argv:
        sys.exit(selftest())
    sys.exit(main())
