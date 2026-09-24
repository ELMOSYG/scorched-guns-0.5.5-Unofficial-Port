"""Forty-second-stage: the batch categories listed in HANDOFF.md section 8.

Everything here is a precise call-pattern rewrite (never a bare signature), and
every rule is idempotent, so the script can be re-run freely.

usage: python tools/port_rewrite42.py <error-log>
"""
from __future__ import annotations

import collections
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from port_rewrite import ensure_import, match_forward, receiver_start, split_args  # noqa: E402

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")
HELPER = "top.ribs.scguns.util.NbtHelper"
LIVING = "net.minecraft.world.entity.LivingEntity"

# how many arguments the 1.21 signature keeps
ARITY = {
    ".isValidBonemealTarget(": 3,
}

HURT = re.compile(r"\.hurtAndBreak\(")
BONEMEAL = re.compile(r"\.isValidBonemealTarget\(")
USED_HAND = re.compile(r"([\w.]+?)\.getUsedItemHand\(\)")
INTERACTION_HAND = re.compile(r"InteractionHand\.(?:MAIN_HAND|OFF_HAND)\Z")

# throwable entities / acid blocks: the 1.20.1 Forge overload took a
# Consumer<LivingEntity>; 1.21 wants the slot the stack lives in.
LAMBDA_SLOTS = [
    (re.compile(r"\.hurtAndBreak\((\w+),\s*(\w+),\s*\w+\s*->\s*\{\s*\}\)"),
     r".hurtAndBreak(\1, \2, \2.getEquipmentSlotForItem(stack))"),
    (re.compile(r"mainHand\.hurtAndBreak\((\w+),\s*(\w+),\s*\w+\s*->\s*\{\s*\}\)"),
     r"mainHand.hurtAndBreak(\1, \2, EquipmentSlot.MAINHAND)"),
    (re.compile(r"offHand\.hurtAndBreak\((\w+),\s*(\w+),\s*\w+\s*->\s*\{\s*\}\)"),
     r"offHand.hurtAndBreak(\1, \2, EquipmentSlot.OFFHAND)"),
    (re.compile(r"\.hurtAndBreak\((\w+),\s*(\w+),\s*\w+\s*->\s*\w+\.broadcastBreakEvent\(\w+\.getUsedItemHand\(\)\)\)"),
     r".hurtAndBreak(\1, \2, net.minecraft.world.entity.LivingEntity.getSlotForHand(\2.getUsedItemHand()))"),
]


def rewrite_calls(text: str, pattern: re.Pattern, fn) -> tuple[str, int]:
    """Rewrite the argument list of every `pattern(...)` call through fn(args)."""
    out = text
    count = 0
    pos = 0
    while True:
        m = pattern.search(out, pos)
        if not m:
            break
        open_paren = m.end() - 1
        close = match_forward(out, open_paren)
        if close < 0:
            break
        args = split_args(out[open_paren + 1:close])
        new = fn([a.strip() for a in args])
        if new is None:
            pos = open_paren + 1
            continue
        body = ", ".join(new)
        out = out[:open_paren + 1] + body + out[close:]
        pos = open_paren + 1 + len(body)
        count += 1
    return out, count


def hurt_slot(args: list[str]) -> list[str] | None:
    if len(args) != 3:
        return None
    slot = args[2]
    m = USED_HAND.fullmatch(slot)
    if m:
        return [args[0], args[1], "%s.getSlotForHand(%s.getUsedItemHand())" % (LIVING, m.group(1))]
    if INTERACTION_HAND.fullmatch(slot):
        return [args[0], args[1], "%s.getSlotForHand(%s)" % (LIVING, slot)]
    return None


def drop_tail(keep: int):
    def fn(args: list[str]) -> list[str] | None:
        if len(args) <= keep:
            return None
        return args[:keep]
    return fn


def tag_from_item(text: str, stats: dict) -> str:
    """`stack.save(new CompoundTag())` -> `NbtHelper.tagFromItem(stack)`."""
    pattern = re.compile(r"([\w.]+(?:\([^()]*\))?)\.save\(new CompoundTag\(\)\)")
    text, n = pattern.subn(lambda m: "%s.tagFromItem(%s)" % (HELPER, m.group(1)), text)
    if n:
        stats["save(new CompoundTag)"] = stats.get("save(new CompoundTag)", 0) + n
        text = ensure_import(text, HELPER)
    return text


def process(path: str, text: str) -> tuple[str, dict]:
    stats: dict = {}
    rel = os.path.relpath(path, SRC).replace("\\", "/")

    # ---- hurtAndBreak: 3rd argument must be an EquipmentSlot
    text, n = rewrite_calls(text, HURT, hurt_slot)
    if n:
        stats["hurtAndBreak slot"] = n
        text = ensure_import(text, LIVING)
    for rx, rep in LAMBDA_SLOTS:
        text, n = rx.subn(rep, text)
        if n:
            stats["hurtAndBreak lambda"] = stats.get("hurtAndBreak lambda", 0) + n
            text = ensure_import(text, LIVING)
            text = ensure_import(text, "net.minecraft.world.entity.EquipmentSlot")

    # ---- BonemealableBlock lost its `isClientSide` parameter
    text, n = rewrite_calls(text, BONEMEAL, drop_tail(3))
    if n:
        stats["isValidBonemealTarget arity"] = n

    # ---- EntityDimensions is a record now
    text, n = re.subn(r"(\.getDimensions\(\))\.(height|width)\b(?!\()", r"\1.\2()", text)
    if n:
        stats["EntityDimensions accessor"] = n

    # ---- ItemStack.save(CompoundTag) is gone
    text = tag_from_item(text, stats)

    # ---- Item.TooltipContext moved behind an import
    if "Item.TooltipContext" in text:
        text = ensure_import(text, "net.minecraft.world.item.Item")
        stats["Item import"] = 1

    # ---- getEquipmentSlotForItem() is an instance method
    text, n = re.subn(r"\bMob\.getEquipmentSlotForItem\(", "this.getEquipmentSlotForItem(", text)
    if n:
        stats["Mob.getEquipmentSlotForItem"] = n

    # ---- GUI overlay event was renamed
    if "RenderGuiOverlayEvent" in text:
        stats["RenderGuiLayerEvent"] = text.count("RenderGuiOverlayEvent")
        text = text.replace("RenderGuiOverlayEvent", "RenderGuiLayerEvent")

    # ---- render tick folded into the frame events
    if "TickEvent.RenderTickEvent" in text:
        text = text.replace("import net.neoforged.neoforge.event.TickEvent.RenderTickEvent;",
                            "import net.neoforged.neoforge.client.event.RenderFrameEvent;")
        text, n = re.subn(r"RenderTickEvent\s+(\w+)", r"RenderFrameEvent.Post \1", text)
        if n:
            stats["RenderFrameEvent"] = n
        text, n = re.subn(r"event\.phase\s*==\s*Phase\.END\s*&&\s*", "", text)
        if n:
            stats["phase END"] = n
        text, n = re.subn(r"event\.phase\s*==\s*Phase\.START\s*&&\s*", "", text)
        if n:
            stats["phase START"] = n
        text, n = re.subn(r"if\s*\(!event\.phase\.equals\(Phase\.START\)\)\s*\{", "if (true) {", text)
        if n:
            stats["phase guarded tick"] = n

    # ---- Minecraft#getDeltaFrameTime -> DeltaTracker
    text, n = re.subn(r"(\w+)\.getDeltaFrameTime\(\)",
                      r"\1.getTimer().getGameTimeDeltaPartialTick(false)", text)
    if n:
        stats["getDeltaFrameTime"] = n
        if rel == "client/handler/GunRenderingHandler.java":
            # the Pre frame event is the old START phase
            text = text.replace("public void onTick(RenderFrameEvent.Post event) {",
                                "public void onTick(RenderFrameEvent.Pre event) {")

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
