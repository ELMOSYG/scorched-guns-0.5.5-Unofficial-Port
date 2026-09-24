#!/usr/bin/env python3
"""Migrate the port's data files to 1.21.1 NeoForge / Create 6 conventions.

Three independent defects are repaired.  Each has a *stated* discriminator so
the rules can be re-derived rather than trusted, and the script is idempotent:
running it twice changes nothing the second time.

R1  ``"conditions"`` -> ``"neoforge:conditions"``
    NeoForge 21.1 reads the condition list from the literal key
    ``neoforge:conditions`` (``ConditionalOps.DEFAULT_CONDITIONS_KEY``,
    .refs/nf-src/.../common/conditions/ConditionalOps.java:54), and
    ``RecipeManager`` parses ``Recipe.CONDITIONAL_CODEC`` *before* the recipe
    body (..world/item/crafting/RecipeManager.java:60), so a top-level
    ``"conditions"`` list is simply ignored.  The port used ``"conditions"``
    everywhere, so every mod-gating condition in the whole data tree was inert.

    Discriminator: only arrays whose elements ALL carry a ``"type"`` key are
    NeoForge conditions.  Vanilla loot conditions carry ``"condition"`` (e.g.
    ``{"condition": "neoforge:loot_table_id"}``) and MUST keep the plain
    ``"conditions"`` key -- rewriting them would break every loot table.  The
    tree contains 432 of the former and 198 of the latter, with zero arrays
    that mix the two styles.

R2  Create 6 renamed two mechanical-crafting / sequenced-assembly fields
    ``acceptMirrored``  -> ``accept_mirrored``
    ``transitionalItem`` -> ``transitional_item``
    and item stacks inside ``result``/``results``/``transitional_item`` must
    use ``id`` (ingredients in ``key``/``ingredient``/``ingredients`` keep
    ``item`` -- that is the ingredient codec, not the item-stack codec).

R3  Anything else is out of scope for this script.

Usage:
    python tools/fix_data_1211.py --check     # report only, no writes
    python tools/fix_data_1211.py             # apply
    python tools/fix_data_1211.py --selftest  # prove the rules
"""

import argparse
import json
import pathlib
import sys

DATA = pathlib.Path("src/main/resources/data")

COND_KEY = "conditions"
NEOFORGE_COND_KEY = "neoforge:conditions"


# --------------------------------------------------------------------------
# rule primitives
# --------------------------------------------------------------------------


def _is_neoforge_condition_array(value):
    """True when *value* is a non-empty list of NeoForge-style conditions.

    NeoForge conditions are ``{"type": "neoforge:mod_loaded", ...}``; vanilla
    loot conditions are ``{"condition": "minecraft:...", ...}``.  Requiring
    EVERY element to carry ``"type"`` (and no element to carry ``"condition"``)
    keeps the rule from ever touching loot conditions.
    """
    if not isinstance(value, list) or not value:
        return False
    for element in value:
        if not isinstance(element, dict):
            return False
        if "type" not in element or "condition" in element:
            return False
    return True


def rename_key_ordered(node, old, new):
    """Rename *old* to *new* in *node*, keeping the original key position.

    Tolerates a missing node so callers can pass an optional sub-object
    (e.g. ``node.get("result")``) without a guard at every call site.
    """
    if not isinstance(node, dict) or old not in node:
        return False
    rebuilt = {}
    for key, value in node.items():
        rebuilt[new if key == old else key] = value
    node.clear()
    node.update(rebuilt)
    return True


def item_to_id(node):
    """Rename an item-stack ``item`` key to ``id`` in place."""
    if isinstance(node, dict) and "item" in node and "id" not in node:
        rename_key_ordered(node, "item", "id")
        return True
    return False


# --------------------------------------------------------------------------
# R1: neoforge:conditions
# --------------------------------------------------------------------------


def fix_conditions(node, stats, depth=1):
    """Move TOP-LEVEL NeoForge condition lists onto the correct key.

    Depth 1 only, deliberately.  NeoForge's ``ConditionalOps`` wraps just the
    top-level datapack object -- it overrides no ``getMap``/``getMapValues``, so
    nested maps are never scanned.  A ``neoforge:conditions`` key nested deeper
    is therefore inert, and in at least one place actively wrong:
    ImmersiveEngineering's ``secondaries[]`` entries are
    ``StackWithChance(output, chance, conditions)`` and IE reads its OWN
    ``conditions`` key (``IERecipeSerializer$1.decode``); renaming it to
    ``neoforge:conditions`` makes IE silently drop the guard instead of
    applying it.  Nested occurrences are counted separately and never rewritten.
    """
    if isinstance(node, dict):
        # Iterate a snapshot and hold on to `value`: the key may be renamed
        # underneath us, so re-reading node[key] afterwards would raise.
        for key, value in list(node.items()):
            if key == COND_KEY and _is_neoforge_condition_array(value):
                if depth == 1:
                    rename_key_ordered(node, COND_KEY, NEOFORGE_COND_KEY)
                    stats["conditions"] += 1
                else:
                    stats["_nested_skipped"] = stats.get("_nested_skipped", 0) + 1
            fix_conditions(value, stats, depth + 1)
    elif isinstance(node, list):
        # Elements of a list sit at their owner's depth, matching the scan in
        # tools/scan_condition_depth.py.
        for element in node:
            fix_conditions(element, stats, depth)


# --------------------------------------------------------------------------
# R2: Create 6 field renames
# --------------------------------------------------------------------------


def fix_create(node, stats):
    """Repair Create mechanical-crafting and sequenced-assembly field names."""
    if not isinstance(node, dict):
        return
    recipe_type = node.get("type")

    if recipe_type == "create:mechanical_crafting":
        if rename_key_ordered(node, "acceptMirrored", "accept_mirrored"):
            stats["accept_mirrored"] += 1
        if item_to_id(node.get("result")):
            stats["result_id"] += 1

    elif recipe_type == "create:sequenced_assembly":
        # transitionalItem -> transitional_item (and its stack uses id)
        if rename_key_ordered(node, "transitionalItem", "transitional_item"):
            stats["transitional_item"] += 1
        if item_to_id(node.get("transitional_item")):
            stats["result_id"] += 1

        # Top-level results, plus each step's results.  ingredients keep `item`.
        holders = [node] + [s for s in node.get("sequence", []) if isinstance(s, dict)]
        for holder in holders:
            results = holder.get("results")
            if isinstance(results, list):
                for entry in results:
                    if item_to_id(entry):
                        stats["result_id"] += 1


def fix_ie_crusher(node, stats):
    """Repair ImmersiveEngineering 12.x crusher recipes.

    IE 12.4.2 renamed the sized-ingredient output field: `result` is a
    `TagOutput`, and its sized branch is `IngredientWithSize`
    (`basePredicate` + `count`, both required) -- `base_ingredient` does not
    exist anywhere in the mod.  Verified against the 106 crusher recipes IE
    ships in its own jar plus `javap` on `IngredientWithSize` /
    `CrusherRecipeSerializer`.

    Second, IE reads its OWN `conditions` key inside `secondaries[]`
    (`StackWithChance(output, chance, conditions)`, evaluated in
    `IERecipeSerializer$1.decode`).  NeoForge does not wrap nested maps, so a
    `neoforge:conditions` key there is silently ignored -- it must be
    `conditions`.  This reverts an over-broad rename made by an earlier
    revision of this script.
    """
    if not isinstance(node, dict) or node.get("type") != "immersiveengineering:crusher":
        return

    if rename_key_ordered(node.get("result"), "base_ingredient", "basePredicate"):
        stats["ie_base_predicate"] += 1

    secondaries = node.get("secondaries")
    if isinstance(secondaries, list):
        for entry in secondaries:
            if isinstance(entry, dict) and rename_key_ordered(entry, NEOFORGE_COND_KEY, COND_KEY):
                stats["ie_secondary_conditions"] += 1


# --------------------------------------------------------------------------
# driver
# --------------------------------------------------------------------------


def transform(document, stats):
    fix_conditions(document, stats)
    fix_create(document, stats)
    fix_ie_crusher(document, stats)
    return document


def load(path):
    return json.loads(path.read_text(encoding="utf-8"))


def run(check_only):
    if not DATA.exists():
        print("ERROR: %s not found; run from the repo root" % DATA)
        return 2

    files = sorted(DATA.rglob("*.json"))
    totals = {
        "conditions": 0,
        "accept_mirrored": 0,
        "transitional_item": 0,
        "result_id": 0,
        "ie_base_predicate": 0,
        "ie_secondary_conditions": 0,
    }
    touched = []

    for path in files:
        try:
            document = load(path)
        except Exception as exc:  # noqa: BLE001
            print("SKIP unparseable %s: %s" % (path, exc))
            continue

        stats = dict.fromkeys(totals, 0)
        transform(document, stats)
        changed = sum(v for k, v in stats.items() if not k.startswith("_"))
        if not changed:
            continue

        touched.append((str(path), {k: v for k, v in stats.items() if v and not k.startswith("_")}))
        for key in totals:
            totals[key] += stats[key]

        if not check_only:
            path.write_text(
                json.dumps(document, indent=2, ensure_ascii=False) + "\n",
                encoding="utf-8",
            )

    print("scanned %d json files under %s" % (len(files), DATA))
    print("")
    print("  conditions        -> neoforge:conditions : %4d" % totals["conditions"])
    print("  acceptMirrored    -> accept_mirrored     : %4d" % totals["accept_mirrored"])
    print("  transitionalItem  -> transitional_item   : %4d" % totals["transitional_item"])
    print("  stack item        -> id                  : %4d" % totals["result_id"])
    print("  IE base_ingredient-> basePredicate       : %4d" % totals["ie_base_predicate"])
    print("  IE secondary neoforge:conditions->conditions: %4d" % totals["ie_secondary_conditions"])
    print("")
    print("files %s: %d" % ("needing changes" if check_only else "rewritten", len(touched)))
    for name, stats in touched[:15]:
        print("    %-72s %s" % (name, {k: v for k, v in stats.items() if v}))
    if len(touched) > 15:
        print("    ... and %d more" % (len(touched) - 15))

    return 0


# --------------------------------------------------------------------------
# selftest
# --------------------------------------------------------------------------


def selftest():
    failures = []

    def check(label, got, want):
        if got != want:
            failures.append("%s: got %r want %r" % (label, got, want))

    def new_stats():
        return dict.fromkeys(
            [
                "conditions",
                "accept_mirrored",
                "transitional_item",
                "result_id",
                "ie_base_predicate",
                "ie_secondary_conditions",
            ],
            0,
        )

    # --- the discriminator must separate NeoForge from vanilla conditions ---
    check(
        "neoforge style detected",
        _is_neoforge_condition_array([{"type": "neoforge:mod_loaded", "modid": "create"}]),
        True,
    )
    check(
        "vanilla loot style rejected",
        _is_neoforge_condition_array([{"condition": "neoforge:loot_table_id"}]),
        False,
    )
    check("mixed styles rejected", _is_neoforge_condition_array([{"type": "x"}, {"condition": "y"}]), False)
    check("empty array rejected", _is_neoforge_condition_array([]), False)
    check("non-list rejected", _is_neoforge_condition_array({"type": "x"}), False)
    check("element without type rejected", _is_neoforge_condition_array([{"modid": "create"}]), False)

    # --- R1 must rename a recipe gate but leave a loot condition alone ---
    stats = new_stats()
    doc = {
        "type": "minecraft:crafting_shapeless",
        "conditions": [{"type": "neoforge:mod_loaded", "modid": "create"}],
        "ingredients": [{"item": "minecraft:stick"}],
        "result": {"id": "minecraft:torch"},
    }
    transform(doc, stats)
    check("R1 renamed", stats["conditions"], 1)
    check("R1 new key present", NEOFORGE_COND_KEY in doc, True)
    check("R1 old key gone", COND_KEY in doc, False)

    # Position must be preserved (the renamed key stays where it was).
    check("R1 key order preserved", list(doc.keys())[1], NEOFORGE_COND_KEY)
    # An ingredient must NOT be turned into an id.
    check("R1 ingredient untouched", doc["ingredients"][0], {"item": "minecraft:stick"})

    # A loot table's nested conditions must survive verbatim.
    stats = new_stats()
    loot = {
        "pools": [
            {
                "conditions": [{"condition": "minecraft:random_chance", "chance": 0.5}],
                "entries": [{"type": "minecraft:item", "name": "minecraft:stick"}],
            }
        ]
    }
    transform(loot, stats)
    check("R1 loot condition kept", stats["conditions"], 0)
    check("R1 loot key kept", COND_KEY in loot["pools"][0], True)

    # --- R2 mechanical crafting ---
    stats = new_stats()
    doc = {
        "type": "create:mechanical_crafting",
        "conditions": [{"type": "neoforge:mod_loaded", "modid": "create"}],
        "acceptMirrored": True,
        "key": {"Q": {"item": "scguns:gun_grip"}},
        "pattern": ["Q"],
        "result": {"id": "scguns:basker"},
    }
    transform(doc, stats)
    check("R2 accept_mirrored", doc.get("accept_mirrored"), True)
    check("R2 acceptMirrored gone", "acceptMirrored" in doc, False)
    check("R2 counted", stats["accept_mirrored"], 1)
    check("R2 key ingredient untouched", doc["key"]["Q"], {"item": "scguns:gun_grip"})
    check("R2 conditions counted", stats["conditions"], 1)

    # --- R2 sequenced assembly: nested results and transitional item ---
    stats = new_stats()
    doc = {
        "type": "create:sequenced_assembly",
        "ingredient": {"item": "scguns:large_brass_casing"},
        "results": [{"item": "scguns:krahg_round"}],
        "sequence": [
            {
                "type": "create:deploying",
                "ingredients": [{"item": "scguns:unfinished_krahg_round"}, [{"item": "scguns:nitro_powder_dust"}]],
                "results": [{"item": "scguns:unfinished_krahg_round"}],
            }
        ],
        "transitionalItem": {"item": "scguns:unfinished_krahg_round"},
    }
    transform(doc, stats)
    check("R2 transitional renamed", doc.get("transitional_item"), {"id": "scguns:unfinished_krahg_round"})
    check("R2 transitional gone", "transitionalItem" in doc, False)
    check("R2 top results id", doc["results"][0], {"id": "scguns:krahg_round"})
    check("R2 step results id", doc["sequence"][0]["results"][0], {"id": "scguns:unfinished_krahg_round"})
    check(
        "R2 step ingredients kept",
        doc["sequence"][0]["ingredients"],
        [{"item": "scguns:unfinished_krahg_round"}, [{"item": "scguns:nitro_powder_dust"}]],
    )
    check("R2 ingredient kept", doc["ingredient"], {"item": "scguns:large_brass_casing"})
    check("R2 id count", stats["result_id"], 3)

    # --- the depth rule: a NESTED type-style conditions list must be left alone ---
    # NeoForge only wraps the top-level object, and ImmersiveEngineering reads its
    # own `conditions` key inside secondaries[].  Renaming these was a real
    # regression in an earlier revision of this script.
    stats = new_stats()
    doc = {
        "type": "immersiveengineering:crusher",
        "conditions": [{"type": "neoforge:mod_loaded", "modid": "immersiveengineering"}],
        "secondaries": [{"chance": 0.1, "conditions": [{"type": "neoforge:true"}]}],
    }
    transform(doc, stats)
    check("nested conditions untouched", "conditions" in doc["secondaries"][0], True)
    check("nested not renamed", NEOFORGE_COND_KEY in doc["secondaries"][0], False)
    check("nested not counted", stats["conditions"], 1)
    check("nested skip recorded", stats.get("_nested_skipped", 0), 1)

    # --- R3: IE crusher schema ---
    stats = new_stats()
    doc = {
        "type": "immersiveengineering:crusher",
        "energy": 6000,
        "result": {"base_ingredient": {"item": "scguns:anthralite_dust"}, "count": 2},
        "secondaries": [
            {
                "chance": 0.1,
                NEOFORGE_COND_KEY: [{"type": "neoforge:true"}],
                "output": {"tag": "c:dusts/nickel"},
            }
        ],
    }
    transform(doc, stats)
    check("IE basePredicate", doc["result"], {"basePredicate": {"item": "scguns:anthralite_dust"}, "count": 2})
    check("IE base_ingredient gone", "base_ingredient" in doc["result"], False)
    check("IE basePredicate counted", stats["ie_base_predicate"], 1)
    check("IE count preserved", doc["result"]["count"], 2)
    check("IE secondary conditions reverted", COND_KEY in doc["secondaries"][0], True)
    check("IE secondary not neoforge", NEOFORGE_COND_KEY in doc["secondaries"][0], False)
    check("IE secondary counted", stats["ie_secondary_conditions"], 1)

    # An IE crusher already using basePredicate must be untouched.
    stats = new_stats()
    doc = {"type": "immersiveengineering:crusher", "result": {"basePredicate": {"item": "x"}, "count": 1}}
    transform(doc, stats)
    check("IE idempotent", stats["ie_base_predicate"], 0)
    check("IE value intact", doc["result"], {"basePredicate": {"item": "x"}, "count": 1})

    # A non-IE recipe must never be touched by the IE rule.
    stats = new_stats()
    doc = {"type": "minecraft:crafting_shapeless", "result": {"base_ingredient": {"item": "x"}, "count": 1}}
    transform(doc, stats)
    check("non-IE untouched", "base_ingredient" in doc["result"], True)

    # --- idempotence: a second pass must be a no-op ---
    before = json.dumps(doc, sort_keys=True)
    stats = new_stats()
    transform(doc, stats)
    check("idempotent", json.dumps(doc, sort_keys=True), before)
    check("idempotent total", sum(stats.values()), 0)

    # --- an already-correct stack must not gain a second id ---
    stats = new_stats()
    doc = {"type": "create:mechanical_crafting", "result": {"id": "x", "count": 2}}
    transform(doc, stats)
    check("no duplicate id", doc["result"], {"id": "x", "count": 2})
    check("no phantom count", stats["result_id"], 0)

    if failures:
        print("SELFTEST FAILED (%d)" % len(failures))
        for failure in failures:
            print("  - %s" % failure)
        return 1

    print("SELFTEST OK (all rules and the discriminator behave as specified)")
    return 0


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="report without writing")
    parser.add_argument("--selftest", action="store_true", help="prove the rewrite rules")
    args = parser.parse_args()

    if args.selftest:
        return selftest()
    return run(args.check)


if __name__ == "__main__":
    sys.exit(main())
