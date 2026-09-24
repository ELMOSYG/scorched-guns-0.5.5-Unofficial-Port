"""Twelfth-stage rules.

  * 1.21.1 has no queryable fuel map (FuelValues arrived in 1.21.2), so
    CommonHooks.getBurnTime collapses into a ScFuels helper.
  * Removing `setCanceled` where the handler is a tick event: NeoForge only
    exposes setCanceled on cancellable events and a tick event never was one;
    the `return` that always follows it already did the work.
  * IEntityAdditionalSpawnData/NetworkHooks.getEntitySpawningPacket
    -> IEntityWithComplexSpawn + the standard spawn packet.
  * Ingredient.fromJson -> Ingredient.CODEC.parse.

usage: python tools/port_rewrite12.py
"""
from __future__ import annotations

import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from port_rewrite import ensure_import, match_forward, split_args  # noqa: E402

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")

AUTHORED = {
    "util/NbtHelper.java", "util/Caps.java", "util/DistHelper.java", "util/MobType.java",
    "util/ScEnchants.java", "util/ScFuels.java", "network/FrameworkMessageBridge.java",
    "network/PacketHandler.java", "init/ModCapabilities.java", "init/ModEnchantments.java",
    "enchantment/CorrodedEnchantment.java",
}

BURN = re.compile(r"(?:CommonHooks|EventHooks)\.getBurnTime\(\s*([^,()]+)\s*,\s*RecipeType\.\w+\s*\)")
SET_CANCELED = re.compile(r"^[ \t]*\w+\.setCanceled\((?:true|false)\);[ \t]*\r?\n", re.M)


def rewrite_from_json(text: str) -> tuple[str, int]:
    out = text
    pos = 0
    count = 0
    while True:
        m = re.search(r"Ingredient\.fromJson\(", out[pos:])
        if not m:
            break
        start = pos + m.start()
        open_paren = pos + m.end() - 1
        close = match_forward(out, open_paren)
        if close < 0:
            break
        args = out[open_paren + 1:close].strip()
        new = ("Ingredient.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, %s).getOrThrow()"
               % args)
        out = out[:start] + new + out[close + 1:]
        pos = start + len(new)
        count += 1
    return out, count


def drop_spawn_packet_override(text: str, stats: dict) -> str:
    if "NetworkHooks.getEntitySpawningPacket" not in text:
        return text
    out = text
    for m in list(re.finditer(r"public\s+Packet<ClientGamePacketListener>\s+getAddEntityPacket\(\)\s*\{", out)):
        open_brace = out.index("{", m.end() - 1)
        close = match_forward(out, open_brace)
        if close < 0:
            break
        line_start = out.rfind("\n", 0, m.start()) + 1
        prev = out.rfind("\n", 0, line_start - 1) + 1
        if "@Override" in out[prev:line_start]:
            line_start = prev
        out = out[:line_start] + out[close + 1:]
        stats["drop getAddEntityPacket"] = stats.get("drop getAddEntityPacket", 0) + 1
    out = out.replace("import net.neoforged.neoforge.network.NetworkHooks;\n", "")
    # declare the spawn data contract NeoForge understands
    if "readSpawnData(" in out and "implements IEntityWithComplexSpawn" not in out:
        m = re.search(r"(public|protected)\s+class\s+\w+[^{]*\{", out)
        if m:
            head = m.group(0)
            if " extends " in head and " implements " in head:
                new_head = head.replace(" implements ", " implements IEntityWithComplexSpawn, ", 1)
            elif " extends " in head:
                new_head = head.replace(" {", " implements IEntityWithComplexSpawn {", 1)
            else:
                new_head = head.replace(" {", " implements IEntityWithComplexSpawn {", 1)
            out = out.replace(head, new_head, 1)
            out = ensure_import(out, "net.neoforged.neoforge.entity.IEntityWithComplexSpawn")
            stats["IEntityWithComplexSpawn"] = stats.get("IEntityWithComplexSpawn", 0) + 1
    # the spawn data buffers are registry aware now
    out = out.replace("void writeSpawnData(FriendlyByteBuf buffer)", "void writeSpawnData(RegistryFriendlyByteBuf buffer)")
    out = out.replace("void readSpawnData(FriendlyByteBuf buffer)", "void readSpawnData(RegistryFriendlyByteBuf buffer)")
    if "RegistryFriendlyByteBuf" in out:
        out = ensure_import(out, "net.minecraft.network.RegistryFriendlyByteBuf")
    return out


def process(path: str, text: str) -> tuple[str, dict]:
    stats: dict = {}
    if "getBurnTime(" in text:
        text, n = BURN.subn(r"ScFuels.burnTime(\1)", text)
        if n:
            stats["getBurnTime"] = n
            text = ensure_import(text, "top.ribs.scguns.util.ScFuels")
    if "TickEvent.Pre" in text or "TickEvent.Post" in text:
        text, n = SET_CANCELED.subn("", text)
        if n:
            stats["drop setCanceled on tick"] = n
    if "NetworkHooks" in text:
        text = drop_spawn_packet_override(text, stats)
    if "Ingredient.fromJson(" in text:
        text, n = rewrite_from_json(text)
        if n:
            stats["Ingredient.fromJson"] = n
    return text, stats


def main() -> None:
    files = [os.path.join(d, f) for d, _, fs in os.walk(SRC) for f in fs if f.endswith(".java")]
    total: dict = {}
    changed = 0
    for path in files:
        if os.path.relpath(path, SRC).replace("\\", "/") in AUTHORED:
            continue
        text = open(path, encoding="utf-8", errors="replace").read()
        new, stats = process(path, text)
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
