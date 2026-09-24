"""Convert Scorched Guns 0.5.5 (MC 1.20.1) resources to MC 1.21.1 layout.

Source of truth: the 0.5.5 release jar (never the 0.4.7 GitHub source).
Output: src/main/resources of the NeoForge 1.21.1 project.

What changes 1.20.1 -> 1.21.1:
  paths   data/<ns>/recipes        -> data/<ns>/recipe
          data/<ns>/advancements   -> data/<ns>/advancement
          data/<ns>/loot_tables    -> data/<ns>/loot_table
          data/<ns>/structures     -> data/<ns>/structure      (template NBT)
          data/<ns>/tags/items     -> data/<ns>/tags/item
          data/<ns>/tags/blocks    -> data/<ns>/tags/block
          data/<ns>/tags/entity_types -> data/<ns>/tags/entity_type
          data/<ns>/forge          -> data/<ns>/neoforge       (biome modifiers)
          data/forge/tags          -> data/c/tags              (@c common tags)
          data/forge/loot_modifiers-> data/neoforge/loot_modifiers
  json    forge: -> neoforge: (conditions, biome modifiers, loot conditions)
          #forge:<tag> -> #c:<tag>
          recipe result: string/"item" -> {"id": ..., "count": n}
"""
from __future__ import annotations

import json
import os
import re
import shutil
import sys
import zipfile

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
JAR = os.path.join(ROOT, "需要移植的mod", "ScorchedGuns-0.5.5-1.20.1.jar")
OUT = os.path.join(ROOT, "src", "main", "resources")

# ---------------------------------------------------------------- path mapping

EXACT_PREFIX = [
    ("data/forge/tags/", "data/c/tags/"),
    ("data/forge/loot_modifiers/", "data/neoforge/loot_modifiers/"),
    ("data/forge/", "data/neoforge/"),
]

NS_PREFIX = [
    ("recipes/", "recipe/"),
    ("advancements/", "advancement/"),
    ("loot_tables/", "loot_table/"),
    ("structures/", "structure/"),
    ("predicates/", "predicate/"),
    ("item_modifiers/", "item_modifier/"),
    ("functions/", "function/"),
    ("tags/items/", "tags/item/"),
    ("tags/blocks/", "tags/block/"),
    ("tags/entity_types/", "tags/entity_type/"),
    ("tags/fluids/", "tags/fluid/"),
    ("tags/game_events/", "tags/game_event/"),
    ("forge/", "neoforge/"),
]


def map_path(name: str) -> str | None:
    """Return the 1.21.1 path for a jar entry, or None to skip it."""
    if name.startswith("top/") or name.startswith("META-INF/"):
        return None
    if name == "pack.mcmeta" or name == "scguns.mixins.json" or name == "scguns.refmap.json":
        return None
    parts = name.split("/")
    if parts[0] not in ("assets", "data") or len(parts) < 3:
        return None
    for a, b in EXACT_PREFIX:
        if name.startswith(a):
            return b + name[len(a):]
    ns = parts[1]
    rest = "/".join(parts[2:])
    for a, b in NS_PREFIX:
        if rest.startswith(a):
            rest = b + rest[len(a):]
            break
    return f"{parts[0]}/{ns}/{rest}"


# ------------------------------------------------------------ json transforms

FORGE_NS = re.compile(r'(:\s*"|")(forge):')
TAG_REF = re.compile(r'"#forge:')


def rewrite_ns(text: str) -> str:
    """forge: -> neoforge:, #forge:tag -> #c:tag."""
    text = TAG_REF.sub('"#c:', text)
    text = FORGE_NS.sub(r'\1neoforge:', text)
    return text


VANILLA_COOKING = {
    "minecraft:smelting",
    "minecraft:blasting",
    "minecraft:smoking",
    "minecraft:campfire_cooking",
    "minecraft:stonecutting",
}


def norm_result(obj):
    """Normalise one result entry to the 1.21 ItemStack form."""
    if isinstance(obj, str):
        return {"id": obj}
    if isinstance(obj, dict):
        if "item" in obj and "id" not in obj:
            out = {"id": obj["item"]}
            if "count" in obj:
                out["count"] = obj["count"]
            for k, v in obj.items():
                if k not in ("item", "count"):
                    out[k] = v
            return out
    return obj


def rewrite_recipe(obj: dict) -> bool:
    """Return True when the object was modified."""
    changed = False
    if "result" in obj:
        res = obj["result"]
        if isinstance(res, list):
            new = [norm_result(r) for r in res]
        else:
            new = norm_result(res)
        # cooking / stonecutting keep the count as a sibling field
        if obj.get("type") in VANILLA_COOKING and isinstance(new, dict):
            if "count" in obj:
                new.setdefault("count", obj.pop("count"))
        if new != res:
            obj["result"] = new
            changed = True
    if "results" in obj and isinstance(obj["results"], list):
        new = [norm_result(r) for r in obj["results"]]
        if new != obj["results"]:
            obj["results"] = new
            changed = True
    if "type" in obj and isinstance(obj["type"], str) and obj["type"].startswith("forge:"):
        obj["type"] = "neoforge:" + obj["type"][len("forge:"):]
        changed = True
    return changed


def convert_json(name: str, raw: bytes) -> bytes:
    text = raw.decode("utf-8")
    stripped = rewrite_ns(text)
    if "/recipe/" in name or name.endswith("/recipe") or "/recipes/" in name:
        try:
            obj = json.loads(stripped)
        except json.JSONDecodeError as exc:
            print(f"  !! bad json {name}: {exc}")
            return stripped.encode("utf-8")
        if isinstance(obj, list):
            changed = any(rewrite_recipe(o) for o in obj if isinstance(o, dict))
        elif isinstance(obj, dict):
            changed = rewrite_recipe(obj)
        else:
            changed = False
        if changed:
            stripped = json.dumps(obj, indent=2, ensure_ascii=False) + "\n"
    return stripped.encode("utf-8")


PACK_MCMETA = {
    "pack": {
        "description": "Resources for Scorched Guns",
        "pack_format": 34,
        "supported_formats": {"min_inclusive": 34, "max_inclusive": 48},
    }
}


def main() -> None:
    if os.path.isdir(OUT):
        shutil.rmtree(OUT)
    os.makedirs(OUT, exist_ok=True)
    z = zipfile.ZipFile(JAR)
    written = skipped = 0
    for info in z.infolist():
        if info.is_dir():
            continue
        target = map_path(info.filename)
        if target is None:
            skipped += 1
            continue
        raw = z.read(info)
        if target.endswith(".json"):
            raw = convert_json(target, raw)
        dest = os.path.join(OUT, *target.split("/"))
        os.makedirs(os.path.dirname(dest), exist_ok=True)
        with open(dest, "wb") as fh:
            fh.write(raw)
        written += 1

    with open(os.path.join(OUT, "pack.mcmeta"), "w", encoding="utf-8") as fh:
        json.dump(PACK_MCMETA, fh, indent=4)

    # the global loot modifier list belongs to the mod's own namespace
    old = os.path.join(OUT, "data", "neoforge", "loot_modifiers", "global_loot_modifiers.json")
    new = os.path.join(OUT, "data", "scguns", "loot_modifiers", "global_loot_modifiers.json")
    if os.path.isfile(old):
        os.makedirs(os.path.dirname(new), exist_ok=True)
        shutil.move(old, new)
        print("moved global_loot_modifiers.json -> data/scguns/loot_modifiers/")

    print(f"wrote {written} resource files ({skipped} jar entries handled separately)")
    print(f"target: {OUT}")


if __name__ == "__main__":
    sys.exit(main())
