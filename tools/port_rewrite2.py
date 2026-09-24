"""Second-stage porting rules: 1.21 API shape changes that need structure aware
rewriting rather than plain textual substitution.

Handled here:
  * NeoForge tick events: TickEvent.{Client,Player,Level,Server}TickEvent became
    separate Pre/Post classes, so `if (event.phase == Phase.END)` guards are
    folded into the handler's parameter type.
  * event.player / event.entity -> event.getEntity().
  * DeferredHolder<R, T>: the register's registry type is the first parameter.
  * ItemStack.hurtAndBreak(int, LivingEntity, Consumer) -> (int, LivingEntity, EquipmentSlot).
  * NetworkHooks.openScreen(player, provider, pos) -> player.openMenu(provider, buf -> ...).
  * PlayerEvent player/entity field renames.

usage: python tools/port_rewrite2.py [path ...]
"""
from __future__ import annotations

import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from port_rewrite import match_forward, split_args  # noqa: E402

SRC = r"E:\mod\scgun-0.5.5-1.21.1-neoforge\src\main\java"

TICK_IMPORTS = {
    "net.neoforged.neoforge.event.TickEvent.ClientTickEvent":
        "net.neoforged.neoforge.client.event.ClientTickEvent",
    "net.neoforged.neoforge.event.TickEvent.PlayerTickEvent":
        "net.neoforged.neoforge.event.tick.PlayerTickEvent",
    "net.neoforged.neoforge.event.TickEvent.LevelTickEvent":
        "net.neoforged.neoforge.event.tick.LevelTickEvent",
    "net.neoforged.neoforge.event.TickEvent.ServerTickEvent":
        "net.neoforged.neoforge.event.tick.ServerTickEvent",
}

TICK_TYPES = ("ClientTickEvent", "PlayerTickEvent", "LevelTickEvent", "ServerTickEvent")

RETURN_TYPE = re.compile(
    r"(?P<sig>(?:public|private|protected)\s+(?:static\s+)?void\s+\w+\s*\(\s*(?:final\s+)?"
    r"(?P<type>" + "|".join(TICK_TYPES) + r")\s+(?P<param>\w+)\s*\)\s*\{)"
)


def phase_of(cond: str, param: str) -> str | None:
    """'pre' / 'post' when the condition pins a single tick phase."""
    m = re.search(r"\b" + param + r"\.phase\s*(==|!=)\s*Phase\.(START|END)", cond)
    if not m:
        return None
    op, phase = m.group(1), m.group(2)
    if op == "==":
        return "pre" if phase == "START" else "post"
    return "post" if phase == "START" else "pre"


def strip_phase_term(cond: str, param: str) -> str | None:
    """Remove the `x.phase == Phase.Y` term; None when it cannot be done safely."""
    m = re.search(r"\b" + param + r"\.phase\s*(==|!=)\s*Phase\.(START|END)", cond)
    if not m:
        return None
    # split on top-level && only: `||` mixes phases in ways we must not guess
    parts = [p.strip() for p in re.split(r"&&", cond)]
    kept = [p for p in parts if not re.search(r"\b" + param + r"\.phase\b", p)]
    if len(kept) == len(parts):
        return None
    return " && ".join(kept)


def rewrite_tick_handlers(text: str, stats: dict) -> str:
    out = text
    pos = 0
    while True:
        m = RETURN_TYPE.search(out, pos)
        if not m:
            break
        param = m.group("param")
        brace = out.index("{", m.end() - 1)
        # find the first `if (` inside the method body
        ifm = re.compile(r"if\s*\(").search(out, brace)
        if not ifm:
            pos = brace
            continue
        open_paren = out.index("(", ifm.start())
        close = match_forward(out, open_paren)
        if close < 0:
            break
        cond = out[open_paren + 1:close]
        phase = phase_of(cond, param)
        if phase is None:
            pos = ifm.end()
            continue
        rest = strip_phase_term(cond, param)
        if rest is None:
            pos = ifm.end()
            continue
        # find the block that the if guards
        body_open = out.index("{", close)
        body_close = match_forward(out, body_open)
        if body_close < 0:
            break
        new_param = param
        new_type = m.group("type") + ("." + ("Pre" if phase == "pre" else "Post"))
        if rest:
            new_if = "if (%s) {" % rest
        else:
            new_if = "{"
        new_text = (
            out[:m.start("sig")]
            + m.group("sig").replace(m.group("type"), new_type, 1)
            + out[m.end("sig"):ifm.start()]
            + new_if
            + out[body_open + 1:body_close]
            + "}"
            + out[body_close + 1:]
        )
        if new_text != out:
            stats["tick:" + m.group("type")] = stats.get("tick:" + m.group("type"), 0) + 1
        out = new_text
        pos = m.start("sig") + len(m.group("sig")) + 10
    return out


REG_DECL = re.compile(r"DeferredRegister<([^<>]*(?:<[^<>]*>)*)>\s+(\w+)\s*=")
HOLDER_FIELD = re.compile(
    r"DeferredHolder<\s*([^,<>]*(?:<[^<>]*>)*)\s*,\s*([^<>]*(?:<[^<>]*>)*)\s*>\s+(\w+)\s*=\s*([\w.]*?)\.register"
)


def registry_type_map(files: list[str]) -> dict[str, str]:
    """class-qualified and bare register field name -> registry type argument."""
    mapping: dict[str, str] = {}
    for path in files:
        text = open(path, encoding="utf-8", errors="replace").read()
        cls = os.path.basename(path)[:-5]
        for reg_type, name in REG_DECL.findall(text):
            mapping[f"{cls}.{name}"] = reg_type.strip()
            mapping.setdefault(name, reg_type.strip())
    return mapping


def fix_deferred_holders(text: str, mapping: dict[str, str], stats: dict) -> str:
    def repl(m: re.Match) -> str:
        first, second, field, reg = m.group(1), m.group(2), m.group(3), m.group(4)
        reg_type = mapping.get(reg) or mapping.get(reg.split(".")[-1])
        if not reg_type or reg_type == first.strip():
            return m.group(0)
        stats["holder:" + reg_type] = stats.get("holder:" + reg_type, 0) + 1
        return f"DeferredHolder<{reg_type}, {second.strip()}> {field} = {reg}.register"

    return HOLDER_FIELD.sub(repl, text)


HURT_LAMBDA = re.compile(
    r"\.hurtAndBreak\(\s*([^,()]+)\s*,\s*([^,()]+)\s*,\s*"
    r"(?:\w+|\(\s*\w+\s*\))\s*->\s*(?:\w+\.broadcastBreakEvent\(([^()]+)\)|\(\s*\w+\.getUsedItemHand\(\)\s*\))\s*\)"
)


def fix_hurt_and_break(text: str, stats: dict) -> str:
    def repl(m: re.Match) -> str:
        amount, entity, slot = m.group(1).strip(), m.group(2).strip(), m.group(3)
        stats["hurtAndBreak"] = stats.get("hurtAndBreak", 0) + 1
        if slot is None:
            # 1.20.1 code broadcast on the entity's used hand
            return ".hurtAndBreak(%s, %s, %s.getUsedItemHand())" % (amount, entity, entity)
        return ".hurtAndBreak(%s, %s, %s)" % (amount, entity, slot.strip())

    text = HURT_LAMBDA.sub(repl, text)
    # `hurtAndBreak(n, entity, null)` -> explicit main hand slot (the old null
    # meant "damage, but do not broadcast a slot break")
    text, n = re.subn(
        r"\.hurtAndBreak\((\s*[^,()]+\s*,\s*[^,()]+\s*),\s*null\s*\)",
        r".hurtAndBreak(\1, net.minecraft.world.entity.EquipmentSlot.MAINHAND)",
        text,
    )
    if n:
        stats["hurtAndBreak:null"] = n
    return text


OPEN_SCREEN = re.compile(r"NetworkHooks\.openScreen\(")


def fix_open_screen(text: str, stats: dict) -> str:
    out = text
    pos = 0
    while True:
        m = OPEN_SCREEN.search(out, pos)
        if not m:
            break
        open_paren = m.end() - 1
        close = match_forward(out, open_paren)
        if close < 0:
            break
        args = [a.strip() for a in split_args(out[open_paren + 1:close])]
        player = args[0]
        if not re.fullmatch(r"[\w.]+", player):
            player = "(%s)" % player  # keep casts/expressions as the receiver
        provider = args[1] if len(args) > 1 else "null"
        if len(args) >= 3 and args[2] not in ("null",):
            writer = args[2]
            new = "%s.openMenu(%s, buf -> buf.writeBlockPos(%s))" % (player, provider, writer)
        else:
            new = "%s.openMenu(%s)" % (player, provider)
        out = out[:m.start()] + new + out[close + 1:]
        pos = m.start() + len(new)
        stats["openScreen"] = stats.get("openScreen", 0) + 1
    if "NetworkHooks" not in out:
        out = out.replace("import net.neoforged.neoforge.network.NetworkHooks;\n", "")
    return out


def process(path: str, text: str, mapping: dict[str, str]) -> tuple[str, dict]:
    stats: dict = {}
    # tick event imports
    for old, new in TICK_IMPORTS.items():
        if "import %s;" % old in text:
            text = text.replace("import %s;" % old, "import %s;" % new)
            stats["import:" + old.split(".")[-1]] = 1
    text = text.replace("import net.neoforged.neoforge.event.TickEvent.Phase;\n", "")
    # event fields (never inside an import path: `.event.entity.` must survive)
    text, n = re.subn(r"(?<![\w.])event\.player(?![\w])", "event.getEntity()", text)
    if n:
        stats["event.player"] = n
    text, n = re.subn(r"(?<![\w.])event\.entity(?![\w])", "event.getEntity()", text)
    if n:
        stats["event.entity"] = n
    if "event.getPlayer()" in text:
        stats["event.getPlayer()"] = text.count("event.getPlayer()")
        text = text.replace("event.getPlayer()", "event.getEntity()")
    # repair damage from earlier runs of the rule above
    if "event.getEntity()." in text:
        stats["repair:import"] = text.count("event.getEntity().")
        text = text.replace("import net.neoforged.neoforge.event.getEntity().",
                            "import net.neoforged.neoforge.event.entity.")
    # `(Cast)expr.openMenu(...)` produced by an earlier openScreen pass
    text, n = re.subn(r"\((\w+)\)([\w.]+)\.openMenu\(", r"((\1)\2).openMenu(", text)
    if n:
        stats["repair:cast"] = n
    text = rewrite_tick_handlers(text, stats)
    text = fix_deferred_holders(text, mapping, stats)
    text = fix_hurt_and_break(text, stats)
    text = fix_open_screen(text, stats)
    return text, stats


def main() -> None:
    targets = sys.argv[1:] or [SRC]
    files = [
        os.path.join(d, f)
        for target in targets
        for d, _, fs in ([(target, [], [os.path.basename(target)])] if os.path.isfile(target) else os.walk(target))
        for f in fs
        if f.endswith(".java")
    ]
    files = [f for f in files if os.path.isfile(f)]
    mapping = registry_type_map(
        [os.path.join(d, f) for d, _, fs in os.walk(SRC) for f in fs if f.endswith(".java")]
    )
    total: dict = {}
    changed = 0
    for path in files:
        text = open(path, encoding="utf-8", errors="replace").read()
        new, stats = process(path, text, mapping)
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
