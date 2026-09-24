"""Thirty-ninth-stage: loose ends across recipes, mobs and client providers.

usage: python tools/port_rewrite39.py
"""
from __future__ import annotations

import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from port_rewrite import ensure_import, match_forward  # noqa: E402

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")


def fix_broken_codec(text: str, stats: dict) -> str:
    """`ItemStack.CODEC.parse(ops, ARG.getOrThrow(), true)` -> parse(ops, ARG).getOrThrow()"""
    out = text
    pos = 0
    count = 0
    while True:
        m = re.search(r"ItemStack\.CODEC\.parse\(", out[pos:])
        if not m:
            break
        open_paren = pos + m.end() - 1
        close = match_forward(out, open_paren)
        if close < 0:
            break
        inner = out[open_paren + 1:close]
        if ".getOrThrow()" in inner:
            fixed_inner = inner.replace(".getOrThrow()", "").strip()
            if fixed_inner.endswith(", true") or fixed_inner.endswith(", false"):
                fixed_inner = fixed_inner.rsplit(",", 1)[0].strip()
            new = "ItemStack.CODEC.parse(%s).getOrThrow()" % fixed_inner
            out = out[:pos + m.start()] + new + out[close + 1:]
            pos = pos + m.start() + len(new)
            count += 1
        else:
            pos = close
    if count:
        stats["repair codec parse"] = count
    return out


def process(path: str, text: str) -> tuple[str, dict]:
    stats: dict = {}
    if "ItemStack.CODEC.parse(" in text:
        text = fix_broken_codec(text, stats)
    # recipe input migration also covers Recipe<Container>
    if "implements Recipe<Container>" in text:
        text = text.replace("implements Recipe<Container>", "implements Recipe<ContainerRecipeInput>")
        text, n = re.subn(r"(matches|assemble)\(\s*Container\s+", r"\1(ContainerRecipeInput ", text)
        if n:
            stats["Recipe<Container>"] = n
        text = ensure_import(text, "top.ribs.scguns.common.recipe.ContainerRecipeInput")
    # finalizeSpawn lost its data tag parameter
    text, n = re.subn(r"(finalizeSpawn\([^;]*?MobSpawnType\.\w+\s*,\s*[^,;]+)\s*,\s*null\)", r"\1)", text)
    if n:
        stats["finalizeSpawn args"] = n
    # getExperienceReward now takes the level and killer
    text, n = re.subn(r"(\w+)\.getExperienceReward\(\)",
                      r"\1.getExperienceReward((net.minecraft.server.level.ServerLevel) this.level(), null)", text)
    if n:
        stats["getExperienceReward"] = n
    # client side code has no provider parameter: take it from the client level
    if "registries" in text and "deserializeNBT(registries" in text \
            and "HolderLookup.Provider registries" not in text:
        text = text.replace("deserializeNBT(registries", "deserializeNBT(net.minecraft.client.Minecraft.getInstance().level.registryAccess()")
        stats["client provider"] = 1
    return text, stats


def main() -> None:
    files = [os.path.join(d, f) for d, _, fs in os.walk(SRC) for f in fs if f.endswith(".java")]
    total: dict = {}
    changed = 0
    for path in files:
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
