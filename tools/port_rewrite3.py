"""Third-stage porting rules.

  * DeferredHolder<R, T>: resolve R per file (a bare `REGISTER` name differs
    between ModItems and ModBlocks, so a global name map is wrong).
  * BlockBehaviour.Properties.copy(Block) -> ofLegacyCopy(Block)  (1.21 split the
    old copy() into ofFullCopy/ofLegacyCopy; ofLegacyCopy keeps 1.20.1 rules).
  * Minecraft.getFrameTime() -> getTimer().getGameTimeDeltaPartialTick(false).
  * defineSynchedData() -> defineSynchedData(SynchedEntityData.Builder) with
    entityData.define(...) -> builder.define(...).
  * VertexConsumer/BufferBuilder vertex(...) -> addVertex(...).
  * Registry.getValue(id) -> Registry.get(id).
  * DeferredSpawnEggItem, igniteForSeconds, DistExecutor, IForgeMenuType, ...

usage: python tools/port_rewrite3.py
"""
from __future__ import annotations

import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from port_rewrite import ensure_import, match_forward  # noqa: E402

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")

REG_DECL = re.compile(r"DeferredRegister<([^<>]*(?:<[^<>]*>)*)>\s+(\w+)\s*=")
HOLDER_FIELD = re.compile(
    r"DeferredHolder<\s*([\w.$]*(?:<[^<>]*>)*)\s*,\s*([^<>]*(?:<[^<>]*>)*)\s*>\s+(\w+)\s*=\s*([\w.$]*?)\.register"
)

SIMPLE = [
    # (pattern, replacement, stat key)
    (r"\bProperties\.copy\(", "Properties.ofLegacyCopy(", "Properties.copy"),
    (r"\.getFrameTime\(\)", ".getTimer().getGameTimeDeltaPartialTick(false)", "getFrameTime"),
    (r"\.getFrameTime(?!\w)", ".getTimer().getGameTimeDeltaPartialTick(false)", "getFrameTime"),
    (r"\.vertex\(", ".addVertex(", "vertex->addVertex"),
    (r"\bForgeSpawnEggItem\b", "DeferredSpawnEggItem", "ForgeSpawnEggItem"),
    (r"\.setSecondsOnFire\(", ".igniteForSeconds(", "setSecondsOnFire"),
    (r"\bIForgeMenuType\.create\(", "IMenuTypeExtension.create(", "IForgeMenuType"),
    (r"\bBinaryBufferBuilder\b", "ByteBufferBuilder", "BinaryBufferBuilder"),
    (r"\bnew RenderType\.CompositeState\b", "new RenderType.CompositeState", "noop"),
]

GET_VALUE = re.compile(r"\b((?:BuiltInRegistries|NeoForgeRegistries|Registries)\.[A-Z_]+)\.getValue\(")


def registry_maps(files: list[str]) -> tuple[dict[str, str], dict[str, dict[str, str]]]:
    global_map: dict[str, str] = {}
    per_file: dict[str, dict[str, str]] = {}
    for path in files:
        text = open(path, encoding="utf-8", errors="replace").read()
        cls = os.path.basename(path)[:-5]
        own: dict[str, str] = {}
        for reg_type, name in REG_DECL.findall(text):
            own[name] = reg_type.strip()
            global_map[f"{cls}.{name}"] = reg_type.strip()
        per_file[path] = own
    return global_map, per_file


def fix_holders(text: str, own: dict[str, str], global_map: dict[str, str], stats: dict) -> str:
    def repl(m: re.Match) -> str:
        first, second, field, reg = m.group(1), m.group(2), m.group(3), m.group(4)
        reg_type = global_map.get(reg) if "." in reg else own.get(reg)
        if not reg_type:
            return m.group(0)
        if first.strip() == reg_type.strip():
            return m.group(0)
        stats["holder:" + reg_type] = stats.get("holder:" + reg_type, 0) + 1
        return f"DeferredHolder<{reg_type}, {second.strip()}> {field} = {reg}.register"

    return HOLDER_FIELD.sub(repl, text)


SYNC_DEFINE = re.compile(r"defineSynchedData\(\s*\)\s*\{")
ENTITY_DATA_DEFINE = re.compile(r"\bthis\.entityData\.define\(")


def fix_synched_data(text: str, stats: dict) -> str:
    """defineSynchedData() { this.entityData.define(...); } -> builder form."""
    out = text
    pos = 0
    while True:
        m = SYNC_DEFINE.search(out, pos)
        if not m:
            break
        brace = out.index("{", m.end() - 1)
        close = match_forward(out, brace)
        if close < 0:
            break
        body = out[brace + 1:close]
        new_body, n = ENTITY_DATA_DEFINE.subn("builder.define(", body)
        if not n:
            pos = close
            continue
        out = (
            out[:m.start()]
            + "defineSynchedData(SynchedEntityData.Builder builder) {"
            + new_body
            + "}"
            + out[close + 1:]
        )
        stats["defineSynchedData"] = stats.get("defineSynchedData", 0) + 1
        pos = m.start() + 20
    if stats.get("defineSynchedData"):
        out = ensure_import(out, "net.minecraft.network.syncher.SynchedEntityData")
    return out


def process(path: str, text: str, own: dict[str, str], global_map: dict[str, str]) -> tuple[str, dict]:
    stats: dict = {}
    text = fix_holders(text, own, global_map, stats)
    for pattern, repl, key in SIMPLE:
        text, n = re.subn(pattern, repl, text)
        if n and key != "noop":
            stats[key] = stats.get(key, 0) + n
    text, n = GET_VALUE.subn(r"\1.get(", text)
    if n:
        stats["registry.getValue"] = n
    text = fix_synched_data(text, stats)
    return text, stats


def main() -> None:
    files = [os.path.join(d, f) for d, _, fs in os.walk(SRC) for f in fs if f.endswith(".java")]
    global_map, per_file = registry_maps(files)
    total: dict = {}
    changed = 0
    for path in files:
        text = open(path, encoding="utf-8", errors="replace").read()
        new, stats = process(path, text, per_file.get(path, {}), global_map)
        if new != text:
            with open(path, "w", encoding="utf-8", newline="") as fh:
                fh.write(new)
            changed += 1
            for k, v in stats.items():
                total[k] = total.get(k, 0) + v
    print(f"rewrote {changed} files")
    for k, v in sorted(total.items(), key=lambda kv: -kv[1]):
        print(f"{v:6d}  {k}")


if __name__ == "__main__":
    main()
