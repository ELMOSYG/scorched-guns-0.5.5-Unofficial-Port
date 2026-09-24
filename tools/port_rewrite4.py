"""Fourth-stage porting rules + compatibility shims.

  * VertexConsumer accessor renames (uv/color/uv2/overlayCoords/normal -> setX).
  * AttributeModifier.Operation constant renames.
  * DistExecutor -> DistHelper (Forge's DistExecutor is gone in NeoForge).
  * Framework sendToTracking -> sendToTrackingEntity.
  * LivingHurtEvent -> LivingIncomingDamageEvent.
  * MobType: 1.20.1 had a MobType enum, 1.21 replaced it with entity type tags;
    a local MobType shim reproduces the old semantics.
  * Drop now-unused NetworkHooks imports.

usage: python tools/port_rewrite4.py
"""
from __future__ import annotations

import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from port_rewrite import ensure_import  # noqa: E402

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")

VERTEX_ACCESSORS = [
    (r"\.overlayCoords\(", ".setOverlay("),
    (r"\.uv2\(", ".setLight("),
    (r"\.uv\(", ".setUv("),
    (r"\.color\(", ".setColor("),
    (r"\.normal\(", ".setNormal("),
]

OPERATIONS = [
    (r"\bOperation\.ADDITION\b", "Operation.ADD_VALUE"),
    (r"\bOperation\.MULTIPLY_BASE\b", "Operation.ADD_MULTIPLIED_BASE"),
    (r"\bOperation\.MULTIPLY_TOTAL\b", "Operation.ADD_MULTIPLIED_TOTAL"),
]

DIST_EXECUTOR = re.compile(
    r"DistExecutor\.(unsafeRunWhenOn|runWhenOn|unsafeCallWhenOn|callWhenOn)"
    r"\(\s*(Dist\.\w+)\s*,\s*\(\)\s*->\s*\(\)\s*->\s*"
)

MOB_TYPE_CALL = re.compile(r"\b([\w.()]+)\.getMobType\(\)")


def process(path: str, text: str) -> tuple[str, dict]:
    stats: dict = {}
    if "VertexConsumer" in text or "addVertex(" in text:
        for pattern, repl in VERTEX_ACCESSORS:
            text, n = re.subn(pattern, repl, text)
            if n:
                stats["vertex:" + repl] = stats.get("vertex:" + repl, 0) + n
    for pattern, repl in OPERATIONS:
        text, n = re.subn(pattern, repl, text)
        if n:
            stats["operation"] = stats.get("operation", 0) + n

    if "DistExecutor" in text:
        def dist_repl(m: re.Match) -> str:
            helper = "runWhenOn" if "Run" in m.group(1) else "callWhenOn"
            return "DistHelper.%s(%s, () -> " % (helper, m.group(2))

        text, n = DIST_EXECUTOR.subn(dist_repl, text)
        if n:
            stats["DistExecutor"] = n
            text = ensure_import(text, "top.ribs.scguns.util.DistHelper")

    if ".sendToTracking(" in text:
        stats["sendToTracking"] = text.count(".sendToTracking(")
        text = text.replace(".sendToTracking(", ".sendToTrackingEntity(")

    if "LivingHurtEvent" in text:
        stats["LivingHurtEvent"] = text.count("LivingHurtEvent")
        text = text.replace("LivingHurtEvent", "LivingIncomingDamageEvent")

    if "MobType" in text and "getMobType" in text:
        text, n = MOB_TYPE_CALL.subn(r"top.ribs.scguns.util.MobType.of(\1)", text)
        if n:
            stats["getMobType"] = n
    if re.search(r"\bMobType\b", text) and "world.entity.MobType" in text:
        text = text.replace("import net.minecraft.world.entity.MobType;",
                            "import top.ribs.scguns.util.MobType;")
        stats["MobType import"] = 1

    # drop an import that no longer has any usage in the body
    if "import net.neoforged.neoforge.network.NetworkHooks;" in text:
        body = text.replace("import net.neoforged.neoforge.network.NetworkHooks;", "")
        if "NetworkHooks." not in body:
            text = body
            stats["drop NetworkHooks import"] = 1

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
