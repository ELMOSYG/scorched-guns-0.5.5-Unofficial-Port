"""Give the Create recipes that can have one a real processing_time, and take it off the ones that cannot.

Create 6 reads the field through one shared codec and then **validates** it:

    ProcessingRecipeParams.CODEC  ->  Codec.INT.optionalFieldOf("processing_time", 0)
    ProcessingRecipe#validate      ->  if (params.processingDuration > 0 && !canSpecifyDuration())
                                          problems.add("Recipe specified a duration. Durations have
                                                         no impact on this type of recipe.")

(both verified with javap on libs/create-1.21.1-6.0.10.jar). `canSpecifyDuration()` returns false
by default and is overridden in exactly three places - `AbstractCrushingRecipe`,
`processing/basin/BasinRecipe` and `kinetics/saw/CuttingRecipe` - so a recipe type that does not
inherit from one of those **rejects the key outright**, and the whole recipe is dropped at load
with a `Parsing error loading recipe` line.

So there are two lists, and getting this wrong in either direction is a real failure:

  * accepts: the key is read, and the default of 0 means 0 ticks. This is the actual bug -
    `create:mixing` had no key at all, so every blend in the mod (gunpowder, the two brass/steel
    blends, the iron blends) completed instantly. Also `create:crushing` (150 recipes, all of
    which already had a value) and `create:cutting` (2 sequenced-assembly steps, also set).
  * rejects: adding the key loses the recipe entirely. Measured on a dev server: 16
    `create:deploying` + 26 `create:sequenced_assembly` + 3 `create:splashing` = **45 recipes
    dropped** the first time this ran. `create:filling` and `create:pressing` are the same; the
    one filling recipe is gated on Create AEI so it only shows the error when that mod is present,
    which is exactly the kind of thing that is invisible until a player installs it.

`create:mechanical_crafting` (138 files, every gun) is in neither list on purpose: Create 6
**removed** the field from that type. `MechanicalCraftingRecipe` is a thin `ShapedRecipe` wrapper
whose codec only reads `accept_mirrored`, and the crafter's duration comes from the grid at run
time (`MechanicalCrafterBlockEntity#tick`: `grid.size() * 16 + 0.5` of rotation). There is
nothing to write and nothing to fix.

Values are Create 6's own most common ones, measured over every recipe in every Create-family jar
in libs/. The two that already had a value in our data agree with the table, which is the check
that it is the right table:

    type                value   evidence
    create:crushing     350     Create 6: 122 of 168 recipes use 350
    create:milling       50     Create 6: 205 of 241 use 50   (MillingRecipe extends AbstractCrushingRecipe)
    create:mixing       100     already in our data (nitro_powder_mixing.json); Create's basin default
    create:cutting       50     already in our data (2 sequenced-assembly steps); Create 6: 30 of 35

Edits are surgical: for the accepts, one line inserted after the object's own `"type"` line, at
that line's indentation; for the rejects, the offending line removed. Nothing is re-serialised,
because these files are not `sort_keys` dumps and re-serialising them would bury the change in
noise (the lesson from HANDOFF 16.1 and 40.3).

The table is re-checked against the Create jar by tools/audit_create_processing_time.py, so a
Create update that moves `canSpecifyDuration` fails the gate instead of quietly losing recipes.

Usage: python tools/fix_create_processing_time.py [--write] [--selftest]
"""
import json
import pathlib
import re
import sys

ROOT = pathlib.Path(r"E:\mod\scgun-0.5.5-1.21.1-neoforge")
RECIPES = ROOT / "src/main/resources/data/scguns/recipe/create"

# type -> (ticks, the Create class that decides it). The class is what
# tools/audit_create_processing_time.py re-checks in the jar, so this table cannot rot silently.
ACCEPTS = {
    "create:crushing": (350, "com.simibubi.create.content.kinetics.crusher.AbstractCrushingRecipe"),
    "create:milling": (50, "com.simibubi.create.content.kinetics.millstone.MillingRecipe"),
    "create:mixing": (100, "com.simibubi.create.content.kinetics.mixer.MixingRecipe"),
    "create:cutting": (50, "com.simibubi.create.content.kinetics.saw.CuttingRecipe"),
}

# Types that inherit the default canSpecifyDuration() == false and drop the recipe if it is there.
REJECTS = (
    "create:pressing",
    "create:filling",
    "create:splashing",
    "create:deploying",
)

TYPE_LINE = re.compile(r'^(?P<indent>[ ]*)"type":\s*"(?P<type>[^"]+)",\s*$')
KEY_LINE = re.compile(r'^[ ]*"(?P<key>[^"]+)"\s*:')
PT_LINE = re.compile(r'^[ ]*"processing_time":\s*(?P<ticks>\d+),\s*$')


class RecipeObject:
    """One open JSON object in a recipe file. `type` stays None until a "type" key names it."""

    __slots__ = ("type", "type_index", "indent", "keys", "pt_index")

    def __init__(self):
        self.type = None
        self.type_index = -1
        self.indent = ""
        self.keys = set()
        self.pt_index = -1

    def __repr__(self):
        return "RecipeObject(%r, line %d)" % (self.type, self.type_index)


def scan_recipe_objects(lines):
    """Every object in the file that declares a "type" of its own, in file order.

    A line-based scan rather than a json round trip, because the output has to be a one-line
    change to the original text. A key belongs to the innermost open object, and only when it is
    written at that object's own nesting level - which is what keeps a step's "processing_time"
    from being read as its parent sequenced assembly's.

    Braces and brackets both count towards depth, but only braces open an object, and strings are
    tracked so a brace inside a value cannot shift anything.
    """
    typed_objects = []
    stack = []
    depth = 0
    in_string = False
    escaped = False
    for index, line in enumerate(lines):
        line_depth = depth
        matched_key = None if in_string else KEY_LINE.match(line)
        if matched_key is not None and line_depth == len(stack) and stack:
            key = matched_key.group("key")
            current = stack[-1]
            current.keys.add(key)
            if key == "type" and current.type is None:
                typed = TYPE_LINE.match(line)
                if typed is not None:
                    current.type = typed.group("type")
                    current.type_index = index
                    current.indent = typed.group("indent")
                    typed_objects.append(current)
            elif key == "processing_time" and current.pt_index < 0 and PT_LINE.match(line):
                current.pt_index = index
        for char in line:
            if in_string:
                if escaped:
                    escaped = False
                elif char == "\\":
                    escaped = True
                elif char == '"':
                    in_string = False
                continue
            if char == '"':
                in_string = True
            elif char == "{":
                depth += 1
                stack.append(RecipeObject())
            elif char == "}":
                depth -= 1
                if stack:
                    stack.pop()
        in_string = False
    return typed_objects


def plan(lines):
    """The edits this file needs: (line_index, "insert"|"delete", indent, ticks)."""
    edits = []
    for obj in scan_recipe_objects(lines):
        if obj.type in ACCEPTS:
            ticks = ACCEPTS[obj.type][0]
            if obj.pt_index < 0:
                edits.append((obj.type_index, "insert", obj.indent, ticks))
        elif obj.type in REJECTS and obj.pt_index >= 0:
            edits.append((obj.pt_index, "delete", obj.indent, 0))
    return edits


def apply_edits(lines, edits):
    out = list(lines)
    for line_index, kind, indent, ticks in sorted(edits, key=lambda e: e[0], reverse=True):
        if kind == "insert":
            out.insert(line_index + 1, '%s"processing_time": %d,' % (indent, ticks))
        else:
            del out[line_index]
    return out


def run(write):
    files = 0
    inserts = 0
    deletes = 0
    for path in sorted(RECIPES.rglob("*.json")):
        lines = path.read_text(encoding="utf-8").splitlines()
        edits = plan(lines)
        if not edits:
            continue
        files += 1
        inserts += sum(1 for e in edits if e[1] == "insert")
        deletes += sum(1 for e in edits if e[1] == "delete")
        rewritten = apply_edits(lines, edits)
        # Every file must still parse, and must end up in a state the fixer would leave alone.
        json.loads("\n".join(rewritten))
        if plan(rewritten):
            raise AssertionError("%s still needs an edit after applying them" % path)
        if write:
            path.write_text("\n".join(rewritten) + "\n", encoding="utf-8")
    print("%d recipe file(s): +%d / -%d processing_time %s"
          % (files, inserts, deletes, "" if write else "(dry run)"))
    return files, inserts, deletes


def selftest():
    """Assert the things a rewrite script is most likely to get wrong: that only the intended lines
    changed, that a rejected type loses the key, and that a second run is a no-op."""
    global RECIPES
    failures = []

    def check(name, before, after, edits, wanted_insert, wanted_delete):
        if len(after) != len(before) + wanted_insert - wanted_delete:
            failures.append("%s: line count %d, expected %d"
                            % (name, len(after), len(before) + wanted_insert - wanted_delete))
        if plan(after):
            failures.append("%s: still needs an edit afterwards" % name)
        json.loads("\n".join(after))
        for line_index, kind, indent, ticks in edits:
            if kind == "insert":
                want = '%s"processing_time": %d,' % (indent, ticks)
                if want not in after:
                    failures.append("%s: did not insert %r" % (name, want))
        return

    # 1. an accepting type gains the key, with its own indentation, and nothing else moves
    mixing = ['{', '  "type": "create:mixing",', '  "results": []', '}']
    edits = plan(mixing)
    check("mixing", mixing, apply_edits(mixing, edits), edits, 1, 0)
    if apply_edits(mixing, edits)[2] != '  "processing_time": 100,':
        failures.append("mixing: wrong inserted line %r" % apply_edits(mixing, edits)[2])

    # 2. a rejecting type loses the key instead - this is the one that loses recipes
    deploying = ['{', '  "type": "create:deploying",', '  "processing_time": 10,', '  "results": []', '}']
    edits = plan(deploying)
    if [e[1] for e in edits] != ["delete"]:
        failures.append("deploying: expected one deletion, got %r" % edits)
    after = apply_edits(deploying, edits)
    if "processing_time" in "\n".join(after):
        failures.append("deploying: the rejected key survived")

    # 3. a rejecting type nested as a sequenced-assembly step also loses it, and the parent is
    #    untouched (it has no duration of its own to lose)
    assembly = ['{', '  "type": "create:sequenced_assembly",', '  "sequence": [', '    {',
                '      "type": "create:deploying",', '      "processing_time": 10,',
                '      "results": []', '    }', '  ]', '}']
    edits = plan(assembly)
    if [e[1] for e in edits] != ["delete"]:
        failures.append("assembly: expected one deletion on the step, got %r" % edits)
    after = apply_edits(assembly, edits)
    if "processing_time" in "\n".join(after):
        failures.append("assembly: the step kept its rejected key")
    if after[1] != '  "type": "create:sequenced_assembly",':
        failures.append("assembly: the parent line moved")

    # 4. a type with no duration at all is left alone
    mech = ['{', '  "type": "create:mechanical_crafting",', '  "key": {}', '}']
    if plan(mech):
        failures.append("mechanical_crafting must not be touched")

    # 5. and the real tree must already be settled, or the above is measuring nothing
    pending = [p for p in sorted(RECIPES.rglob("*.json")) if plan(p.read_text(
        encoding="utf-8").splitlines())]
    if pending:
        failures.append("%d real file(s) still need an edit, first: %s"
                        % (len(pending), pending[0].name))

    for failure in failures:
        print("SELFTEST FAIL:", failure)
    print("selftest: %d failure(s)" % len(failures))
    return 1 if failures else 0


def main():
    if "--selftest" in sys.argv:
        return selftest()
    run(write="--write" in sys.argv)
    return 0


if __name__ == "__main__":
    sys.exit(main())
