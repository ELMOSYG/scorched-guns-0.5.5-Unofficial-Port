"""Sixteenth-stage: villager trades and entity tick events.

  * 1.21 changed MerchantOffer's cost arguments from ItemStack to ItemCost, and
    the 6-argument 1.20.1 overload now needs an explicit Optional.empty().
  * Forge's LivingEvent.LivingTickEvent is gone; entities tick through
    EntityTickEvent.Pre/Post, whose getEntity() returns Entity.

usage: python tools/port_rewrite16.py
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
    "util/ScEnchants.java", "util/ScFuels.java", "util/ScEffects.java", "util/ScTrades.java",
    "network/FrameworkMessageBridge.java", "network/PacketHandler.java",
    "init/ModCapabilities.java", "init/ModEnchantments.java", "init/ModArmorMaterials.java",
    "init/ModJukeboxSongs.java", "enchantment/CorrodedEnchantment.java",
}


def to_cost(arg: str) -> str:
    arg = arg.strip()
    m = re.fullmatch(r"new ItemStack\((.*)\)", arg, re.S)
    if m:
        parts = [p.strip() for p in split_args(m.group(1))]
        if len(parts) == 1:
            return "new ItemCost(%s, 1)" % parts[0]
        if len(parts) == 2:
            return "new ItemCost(%s, %s)" % (parts[0], parts[1])
    if re.match(r"^[\w.()]+$", arg):
        return "ScTrades.cost(%s)" % arg
    return "ScTrades.cost(%s)" % arg


def rewrite_merchant_offers(text: str, stats: dict) -> str:
    out = text
    pos = 0
    while True:
        m = re.search(r"new MerchantOffer\(", out[pos:])
        if not m:
            break
        start = pos + m.start()
        open_paren = pos + m.end() - 1
        close = match_forward(out, open_paren)
        if close < 0:
            break
        args = [a.strip() for a in split_args(out[open_paren + 1:close])]
        new_args = list(args)
        if new_args:
            new_args[0] = to_cost(new_args[0])
        if len(args) == 6 and not args[1].startswith("Optional"):
            # 1.20.1 (cost, result, uses, xp, priceMul, ...) -> 1.21 wants the
            # optional second cost spelled out
            new_args = [new_args[0], "Optional.empty()"] + new_args[1:]
        out = out[:open_paren + 1] + ", ".join(new_args) + out[close:]
        pos = open_paren + 1 + sum(len(a) + 2 for a in new_args)
        stats["MerchantOffer"] = stats.get("MerchantOffer", 0) + 1
    if "ItemCost" in out:
        out = ensure_import(out, "net.minecraft.world.item.trading.ItemCost")
    if "ScTrades." in out:
        out = ensure_import(out, "top.ribs.scguns.util.ScTrades")
    if "Optional.empty()" in out:
        out = ensure_import(out, "java.util.Optional")
    return out


def rewrite_living_tick(text: str, stats: dict) -> str:
    if "LivingTickEvent" not in text:
        return text
    text = text.replace("import net.neoforged.neoforge.event.entity.living.LivingEvent.LivingTickEvent;",
                        "import net.neoforged.neoforge.event.tick.EntityTickEvent;")
    text = re.sub(r"\bLivingTickEvent\b", "EntityTickEvent.Post", text)
    # inside those handlers the entity is only known as Entity
    pattern = re.compile(r"void\s+\w+\(\s*EntityTickEvent\.Post\s+(\w+)\s*\)\s*\{")
    out = text
    pos = 0
    while True:
        m = pattern.search(out, pos)
        if not m:
            break
        brace = out.index("{", m.end() - 1)
        close = match_forward(out, brace)
        if close < 0:
            break
        param = m.group(1)
        body = out[brace + 1:close]
        new_body = body.replace("%s.getEntity()" % param, "((LivingEntity) %s.getEntity())" % param)
        out = out[:brace + 1] + new_body + out[close:]
        pos = brace + 1 + len(new_body)
        stats["living tick handler"] = stats.get("living tick handler", 0) + 1
    if "((LivingEntity)" in out:
        out = ensure_import(out, "net.minecraft.world.entity.LivingEntity")
    return out


def process(path: str, text: str) -> tuple[str, dict]:
    stats: dict = {}
    if "MerchantOffer(" in text:
        text = rewrite_merchant_offers(text, stats)
    if "LivingTickEvent" in text:
        text = rewrite_living_tick(text, stats)
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
