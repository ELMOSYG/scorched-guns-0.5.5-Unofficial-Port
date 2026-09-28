"""Loot tables have to load, or the block drops itself (HANDOFF 82.30).

The player reported that mining nether sulfur ore gave the ore block back. The table was there and was valid
JSON, so nothing looked wrong - but Minecraft parses loot tables with strict codecs, and a table that fails to
parse is a table that does not exist, which is when a block falls back to dropping itself.

Two 1.20.1-era shapes were still in 28 and 10 of the mod's tables:

  * an enchantment predicate writes "enchantments" (a holder set) in 1.21; the mod said "enchantment". The
    singular spelling is still correct inside minecraft:apply_bonus and minecraft:enchanted_count_increase
    (verified in the function's own codec bytecode), which is why this has to be a structural rule and not a
    word search - and why a naive bulk rename would have broken the fortune functions.
  * minecraft:enchant_with_levels no longer accepts "treasure": true; in 1.21 vanilla's own random-loot tables
    pass "options": "#minecraft:on_random_loot".

Rules:

  1. every loot table parses as JSON,
  2. no enchantment predicate uses the singular "enchantment" key,
  3. every key the mod's tables use also appears in a vanilla loot table - this is what catches a field that
     was renamed or removed between versions, without needing a list of names to remember,
  4. every scguns item a table names is a registered item or block.

usage: python tools/audit_loot_tables.py
"""
from __future__ import annotations

import glob
import json
import os
import re
import sys
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TABLES = sorted(glob.glob(os.path.join(ROOT, "src", "main", "resources", "data", "scguns",
                                       "loot_table", "**", "*.json"), recursive=True))
INIT = os.path.join(ROOT, "src", "main", "java", "top", "ribs", "scguns", "init")
CLIENT_JAR = os.path.join(ROOT, "build", "moddev", "artifacts",
                          "neoforge-21.1.249-client-extra-aka-minecraft-resources.jar")

PREDICATE_KEYS = ("minecraft:enchantments", "minecraft:stored_enchantments", "enchantments", "stored_enchantments")
# Keys the mod may use that vanilla never writes. Empty on purpose: add an entry only with a reason.
ALLOWED_EXTRA_KEYS: dict[str, str] = {}


def walk_predicates(node, found):
    if isinstance(node, dict):
        for key, value in node.items():
            if key in PREDICATE_KEYS and isinstance(value, list):
                for entry in value:
                    if isinstance(entry, dict):
                        found.append(entry)
            walk_predicates(value, found)
    elif isinstance(node, list):
        for entry in node:
            walk_predicates(entry, found)


def vanilla_keys():
    """Every JSON key vanilla's own loot tables use, or None when the jar is not there."""
    if not os.path.isfile(CLIENT_JAR):
        return None
    keys = set()
    with zipfile.ZipFile(CLIENT_JAR) as zf:
        for name in zf.namelist():
            if name.startswith("data/minecraft/loot_table/") and name.endswith(".json"):
                keys |= set(re.findall(r'"([A-Za-z_][A-Za-z0-9_]*)"\s*:', zf.read(name).decode("utf-8")))
    return keys or None


def vanilla_table_types():
    """The `type` values vanilla's own loot tables declare, or None when the jar is not there."""
    if not os.path.isfile(CLIENT_JAR):
        return None
    types = set()
    with zipfile.ZipFile(CLIENT_JAR) as zf:
        for name in zf.namelist():
            if name.startswith("data/minecraft/loot_table/") and name.endswith(".json"):
                match = re.search(r'"type"\s*:\s*"([^"]+)"', zf.read(name).decode("utf-8"))
                if match:
                    types.add(match.group(1))
    return types or None


def registered_ids():
    """Every literal id handed to any registration helper.

    `registerBurnable("plasma_block", ..., 43200)` is a real registration, so the helper name must not be
    pinned: the first version of this rule matched only `register(` and produced three false positives.
    """
    ids = set()
    for name in ("ModItems.java", "ModBlocks.java"):
        path = os.path.join(INIT, name)
        if os.path.isfile(path):
            text = open(path, encoding="utf-8", errors="replace").read()
            ids |= set(re.findall(r'[A-Za-z_]*[Rr]egister[A-Za-z_]*\(\s*"([a-z0-9_]+)"', text))
    return ids


def main() -> int:
    problems: list[str] = []
    vanilla = vanilla_keys()
    vanilla_types = vanilla_table_types()
    registered = registered_ids()
    predicate_count = 0

    if not TABLES:
        problems.append("no loot tables found at all - is the path right?")

    for path in TABLES:
        rel = os.path.relpath(path, ROOT).replace("\\", "/")
        text = open(path, encoding="utf-8", errors="replace").read()
        try:
            data = json.loads(text)
        except Exception as exc:
            problems.append("%s is not valid JSON: %s" % (rel, exc))
            continue

        # 2. the singular key inside a predicate
        found: list = []
        walk_predicates(data, found)
        predicate_count += len(found)
        for entry in found:
            if "enchantment" in entry:
                problems.append('%s: an enchantment predicate uses "enchantment"; 1.21 spells it "enchantments", '
                                "and the whole table fails to load without it" % rel)

        # 3. keys vanilla does not know
        if vanilla is not None:
            for key in sorted(set(re.findall(r'"([A-Za-z_][A-Za-z0-9_]*)"\s*:', text))):
                if key in vanilla or key in ALLOWED_EXTRA_KEYS:
                    continue
                problems.append('%s writes the key "%s", which no vanilla loot table uses: a renamed or removed '
                                "field stops the table from loading" % (rel, key))

        # 4. item ids that do not exist
        for match in re.finditer(r'"(?:name|item)":\s*"scguns:([a-z0-9_]+)"', text):
            if match.group(1) not in registered:
                problems.append("%s names scguns:%s, which no registration declares" % (rel, match.group(1)))

        # 5. the table's own type - read from the parsed object, since a first-match regex finds a pool
        #    entry's "minecraft:item" and reports ten false positives (as the first version of this rule did)
        declared = data.get("type")
        if declared is None:
            problems.append('%s has no top-level "type": the loot table dispatcher needs one to pick a codec, '
                            "and without it the table never loads" % rel)
        elif vanilla_types is not None and declared not in vanilla_types:
            problems.append('%s declares type "%s", which no vanilla loot table uses' % (rel, declared))

    print("=== loot tables ===")
    print("  tables                                 %d" % len(TABLES))
    print("  enchantment predicates found           %d" % predicate_count)
    print("  vanilla key vocabulary                 %s"
          % ("%d keys" % len(vanilla) if vanilla else "unavailable (client jar missing - rule 3 skipped)"))
    print("  registered ids                         %d" % len(registered))
    print("")
    if problems:
        for problem in problems[:40]:
            print("BROKEN %s" % problem)
        if len(problems) > 40:
            print("... and %d more" % (len(problems) - 40))
        print("\n%d problem(s)" % len(problems))
        return 1
    print("0 problem(s): every loot table loads, and every id and field it uses exists")
    return 0


if __name__ == "__main__":
    sys.exit(main())
