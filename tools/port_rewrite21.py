"""Twenty-first-stage: repair three non-idempotent rewrites from earlier passes.

  * the effect-map rule in stage 6 matched `Caps.ifPresent(` itself on a second
    run, producing `Caps.ifPresent(Caps, ...)`
  * the VertexConsumer accessor rename turned the PoseStack.Pose getter
    `pose.normal()` into `pose.setNormal()`
  * ForgeRegistries.Keys imports survived the registry constant mapping

usage: python tools/port_rewrite21.py
"""
from __future__ import annotations

import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from port_rewrite import ensure_import  # noqa: E402

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")


def process(path: str, text: str) -> tuple[str, dict]:
    stats: dict = {}
    if "Caps.ifPresent(Caps, " in text:
        stats["repair Caps.ifPresent"] = text.count("Caps.ifPresent(Caps, ")
        text = text.replace("Caps.ifPresent(Caps, ", "Caps.ifPresent(")
    if ".setNormal()" in text:
        # the Pose getter, not the VertexConsumer setter
        text, n = re.subn(r"\b(\w*[Pp]ose)\.setNormal\(\)", r"\1.normal()", text)
        if n:
            stats["repair pose.normal"] = n
    if "neoforge.registries.ForgeRegistries.Keys" in text:
        text = text.replace("import net.neoforged.neoforge.registries.ForgeRegistries.Keys;\n", "")
        text = text.replace("ForgeRegistries.Keys.", "Registries.")
        text = ensure_import(text, "net.minecraft.core.registries.Registries")
        stats["ForgeRegistries.Keys"] = 1
    if "ForgeRegistries" in text:
        text, n = re.subn(r"\bForgeRegistries\.Keys\.(\w+)", r"Registries.\1", text)
        if n:
            stats["Keys.X"] = n
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
