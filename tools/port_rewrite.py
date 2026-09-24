"""Mechanical Forge/MC 1.20.1 -> NeoForge/MC 1.21.1 source rewriting.

Re-runnable: every rule is idempotent, so the script can be extended and run
again over partially ported sources. Anything the rules cannot decide is left
alone for the compiler to flag.
"""
from __future__ import annotations

import os
import re
import sys

SRC = r"E:\mod\scgun-0.5.5-1.21.1-neoforge\src\main\java"
HELPER = "top.ribs.scguns.util.NbtHelper"

# --------------------------------------------------------------- text helpers

IDENT = re.compile(r"[A-Za-z_$][\w$]*")


def match_forward(text: str, i: int) -> int:
    """Given text[i] is an opening bracket, return index of its closing bracket.

    Uses a real bracket stack (mixing `(`/`{`/`[` with a single counter silently
    derails on nested code) and skips comments and literals, so an apostrophe
    inside a comment is never mistaken for a character literal.
    """
    pairs = {"(": ")", "[": "]", "{": "}"}
    stack: list[str] = []
    j = i
    n = len(text)
    while j < n:
        c = text[j]
        if c == "/" and j + 1 < n and text[j + 1] == "/":
            while j < n and text[j] != "\n":
                j += 1
            continue
        if c == "/" and j + 1 < n and text[j + 1] == "*":
            end = text.find("*/", j + 2)
            j = n if end < 0 else end + 2
            continue
        if c == '"':
            j += 1
            while j < n:
                if text[j] == "\\":
                    j += 2
                    continue
                if text[j] == '"':
                    break
                j += 1
        elif c == "'" and looks_like_char_literal(text, j):
            j += 1
            while j < n:
                if text[j] == "\\":
                    j += 2
                    continue
                if text[j] == "'":
                    break
                j += 1
        elif c in pairs:
            stack.append(pairs[c])
        elif c in ")]}":
            if not stack or stack[-1] != c:
                return -1
            stack.pop()
            if not stack:
                return j
        j += 1
    return -1


def looks_like_char_literal(text: str, i: int) -> bool:
    """True for 'a', '\\n', '\\u0041' - but not for a stray apostrophe."""
    j = i + 1
    n = len(text)
    if j < n and text[j] == "\\":
        j += 2
    else:
        j += 1
    return j < n and text[j] == "'"


def split_args(text: str) -> list[str]:
    """Split a top-level comma separated argument list."""
    out, depth, start = [], 0, 0
    i = 0
    while i < len(text):
        c = text[i]
        if c in "\"'":
            q = c
            i += 1
            while i < len(text):
                if text[i] == "\\":
                    i += 2
                    continue
                if text[i] == q:
                    break
                i += 1
        elif c in "([{":
            depth += 1
        elif c in ")]}":
            depth -= 1
        elif c == "," and depth == 0:
            out.append(text[start:i])
            start = i + 1
        i += 1
    out.append(text[start:])
    return out


# keywords that terminate a primary expression and are NOT part of it
BOUNDARY_WORDS = {
    "return", "throw", "case", "else", "do", "yield", "instanceof", "assert",
    "if", "while", "for", "switch", "synchronized", "try", "catch", "final",
    "break", "continue", "default", "extends", "implements", "package", "import",
    "public", "private", "protected", "static", "void", "class", "interface", "enum",
    "int", "long", "double", "float", "boolean", "byte", "short", "char",
    "var", "record", "sealed", "abstract", "native", "transient", "volatile",
    "true", "false", "null",
}
# keywords that can start a primary expression
EXPR_WORDS = {"new", "this", "super"}

BALANCED = {")": "(", "]": "[", "}": "{"}


def receiver_start(text: str, dot: int) -> int:
    """Return the start index of the primary expression ending at text[dot].

    Walks left over one chain token at a time (identifier, ``.``, call/index
    group) and remembers the leftmost position reached. The first construct that
    cannot belong to the expression (statement keyword, operator, ``;``, ``{``,
    ``}``…) ends the scan and the remembered position is the answer.
    """
    i = dot
    best = dot
    while i > 0:
        j = i - 1
        while j >= 0 and text[j].isspace():
            j -= 1
        if j < 0:
            break
        c = text[j]
        if c == ".":
            i = j
            continue
        if c in ")]}":
            if c == "}":
                break
            want = BALANCED[c]
            depth = 0
            k = j
            while k >= 0:
                if text[k] == c:
                    depth += 1
                elif text[k] == want:
                    depth -= 1
                    if depth == 0:
                        break
                k -= 1
            if k < 0:
                break
            m = k - 1
            while m >= 0 and text[m].isspace():
                m -= 1
            if m >= 0 and (text[m].isalnum() or text[m] in "_$"):
                w = m
                while w >= 0 and (text[w].isalnum() or text[w] in "_$"):
                    w -= 1
                if text[w + 1:m + 1] in BOUNDARY_WORDS:
                    break  # `for (`, `if (`, `while (` … are statements
                best = w + 1
                i = best
                continue
            if m >= 0 and text[m] in ")]}":
                i = m + 1
                continue
            best = k  # plain parenthesised expression
            i = best
            continue
        if c.isalnum() or c in "_$":
            w = j
            while w >= 0 and (text[w].isalnum() or text[w] in "_$"):
                w -= 1
            word = text[w + 1:j + 1]
            if word in BOUNDARY_WORDS and word not in EXPR_WORDS:
                break
            best = w + 1
            i = best
            continue
        if c in "\"'":
            quote = c
            w = j - 1
            while w >= 0 and text[w] != quote:
                w -= 1
            if w < 0:
                break
            best = w
            i = best
            continue
        break
    return best


def rewrite_dot_calls(text: str, method: str, template: str,
                      skip_prefix: str | None = None) -> tuple[str, int]:
    """Rewrite `<recv>.method(...)` into template % receiver, argument list.

    ``skip_prefix`` guards idempotency: calls that already read
    ``Helper.method(...)`` are left untouched.
    """
    pattern = re.compile(r"\." + method + r"\(")
    out = text
    count = 0
    pos = 0
    while True:
        m = pattern.search(out, pos)
        if not m:
            break
        if skip_prefix and out[max(0, m.start() - len(skip_prefix)):m.start()] == skip_prefix:
            pos = m.end()
            continue
        open_paren = m.end() - 1
        close = match_forward(out, open_paren)
        if close < 0:
            break
        args = out[open_paren + 1:close]
        start = receiver_start(out, m.start())
        while start < m.start() and out[start].isspace():
            start += 1
        recv = out[start:m.start()].strip()
        if not recv:
            pos = close
            continue
        new = template.replace("@R", recv).replace("@A", args)
        out = out[:start] + new + out[close + 1:]
        pos = start + len(new)
        count += 1
    return out, count


def rewrite_ctor(text: str, cls: str, fn) -> tuple[str, int]:
    """Rewrite `new <cls>(args)` through fn(arg_list) -> replacement or None."""
    pattern = re.compile(r"new\s+" + re.escape(cls) + r"\s*\(")
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
        args = out[open_paren + 1:close]
        new = fn(args)
        if new is None:
            pos = close
            continue
        out = out[:m.start()] + new + out[close + 1:]
        pos = m.start() + len(new)
        count += 1
    return out, count


def rewrite_generic_type(text: str, name: str, fn) -> tuple[str, int]:
    """Rewrite `Name<...>` allowing nesting; fn(inner) -> replacement."""
    pattern = re.compile(r"\b" + re.escape(name) + r"\s*<")
    out = text
    count = 0
    pos = 0
    while True:
        m = pattern.search(out, pos)
        if not m:
            break
        open_angle = out.index("<", m.start())
        depth = 0
        j = open_angle
        while j < len(out):
            if out[j] == "<":
                depth += 1
            elif out[j] == ">":
                depth -= 1
                if depth == 0:
                    break
            j += 1
        if j >= len(out):
            break
        inner = out[open_angle + 1:j]
        new = fn(inner)
        if new is None:
            pos = j
            continue
        out = out[:m.start()] + new + out[j + 1:]
        pos = m.start() + len(new)
        count += 1
    return out, count


# ------------------------------------------------------------------- rule set

FORGED_IMPORTS = [
    ("net.minecraftforge.eventbus.api.", "net.neoforged.bus.api."),
    ("net.minecraftforge.api.distmarker.", "net.neoforged.api.distmarker."),
    ("net.minecraftforge.common.MinecraftForge", "net.neoforged.neoforge.common.NeoForge"),
    ("net.minecraftforge.common.ForgeConfigSpec", "net.neoforged.neoforge.common.ModConfigSpec"),
    ("net.minecraftforge.common.ForgeHooks", "net.neoforged.neoforge.common.CommonHooks"),
    ("net.minecraftforge.common.capabilities.", "net.neoforged.neoforge.capabilities."),
    ("net.minecraftforge.common.crafting.", "net.neoforged.neoforge.common.crafting."),
    ("net.minecraftforge.common.loot.", "net.neoforged.neoforge.common.loot."),
    ("net.minecraftforge.common.util.", "net.neoforged.neoforge.common.util."),
    ("net.minecraftforge.common.extensions.", "net.neoforged.neoforge.common.extensions."),
    ("net.minecraftforge.common.", "net.neoforged.neoforge.common."),
    ("net.minecraftforge.registries.", "net.neoforged.neoforge.registries."),
    ("net.minecraftforge.fml.", "net.neoforged.fml."),
    ("net.minecraftforge.event.", "net.neoforged.neoforge.event."),
    ("net.minecraftforge.client.", "net.neoforged.neoforge.client."),
    ("net.minecraftforge.items.", "net.neoforged.neoforge.items."),
    ("net.minecraftforge.fluids.", "net.neoforged.neoforge.fluids."),
    ("net.minecraftforge.energy.", "net.neoforged.neoforge.energy."),
    ("net.minecraftforge.network.", "net.neoforged.neoforge.network."),
    ("net.minecraftforge.entity.", "net.neoforged.neoforge.entity."),
    ("net.minecraftforge.data.", "net.neoforged.neoforge.data."),
    ("net.minecraftforge.", "net.neoforged.neoforge."),
]

REGISTRY_MAP = {
    "ForgeRegistries.ITEMS": "BuiltInRegistries.ITEM",
    "ForgeRegistries.BLOCKS": "BuiltInRegistries.BLOCK",
    "ForgeRegistries.ENTITY_TYPES": "BuiltInRegistries.ENTITY_TYPE",
    "ForgeRegistries.SOUND_EVENTS": "BuiltInRegistries.SOUND_EVENT",
    "ForgeRegistries.PARTICLE_TYPES": "BuiltInRegistries.PARTICLE_TYPE",
    "ForgeRegistries.MOB_EFFECTS": "BuiltInRegistries.MOB_EFFECT",
    "ForgeRegistries.POTIONS": "BuiltInRegistries.POTION",
    "ForgeRegistries.POTION_TYPES": "BuiltInRegistries.POTION",
    "ForgeRegistries.BLOCK_ENTITY_TYPES": "BuiltInRegistries.BLOCK_ENTITY_TYPE",
    "ForgeRegistries.MENU_TYPES": "BuiltInRegistries.MENU",
    "ForgeRegistries.RECIPE_SERIALIZERS": "BuiltInRegistries.RECIPE_SERIALIZER",
    "ForgeRegistries.RECIPE_TYPES": "BuiltInRegistries.RECIPE_TYPE",
    "ForgeRegistries.ATTRIBUTES": "BuiltInRegistries.ATTRIBUTE",
    "ForgeRegistries.FLUIDS": "BuiltInRegistries.FLUID",
    "ForgeRegistries.DAMAGE_TYPES": "BuiltInRegistries.DAMAGE_TYPE",
    "ForgeRegistries.STRUCTURE_TYPES": "BuiltInRegistries.STRUCTURE_TYPE",
    "ForgeRegistries.STRUCTURE_PIECE_TYPES": "BuiltInRegistries.STRUCTURE_PIECE",
    "ForgeRegistries.FEATURES": "BuiltInRegistries.FEATURE",
    "ForgeRegistries.PLACEMENT_MODIFIER_TYPES": "BuiltInRegistries.PLACEMENT_MODIFIER_TYPE",
    "ForgeRegistries.PROCESSOR_TYPES": "BuiltInRegistries.STRUCTURE_PROCESSOR",
    "ForgeRegistries.PAINTING_VARIANTS": "BuiltInRegistries.PAINTING_VARIANT",
    "ForgeRegistries.CREATIVE_MODE_TABS": "BuiltInRegistries.CREATIVE_MODE_TAB",
    "ForgeRegistries.ENCHANTMENTS": "BuiltInRegistries.ENCHANTMENT",
    "ForgeRegistries.ENCHANTMENT_EFFECT_COMPONENTS": "BuiltInRegistries.ENCHANTMENT_EFFECT_COMPONENT_TYPE",
    "ForgeRegistries.DATA_COMPONENT_TYPES": "BuiltInRegistries.DATA_COMPONENT_TYPE",
    "ForgeRegistries.VILLAGER_PROFESSIONS": "BuiltInRegistries.VILLAGER_PROFESSION",
    "ForgeRegistries.ACTIVITIES": "BuiltInRegistries.ACTIVITY",
    "ForgeRegistries.LOOT_MODIFIER_SERIALIZERS": "NeoForgeRegistries.LOOT_MODIFIER_SERIALIZERS",
    "ForgeRegistries.LOOT_CONDITION_TYPES": "NeoForgeRegistries.LOOT_CONDITION_TYPES",
    "ForgeRegistries.Keys.ITEMS": "Registries.ITEM",
    "ForgeRegistries.Keys.BLOCKS": "Registries.BLOCK",
    "ForgeRegistries.Keys.ENTITY_TYPES": "Registries.ENTITY_TYPE",
    "ForgeRegistries.Keys.ATTRIBUTES": "Registries.ATTRIBUTE",
    "ForgeRegistries.Keys.STRUCTURE_TYPES": "Registries.STRUCTURE_TYPE",
    "ForgeRegistries.Keys.DAMAGE_TYPES": "Registries.DAMAGE_TYPE",
    "ForgeRegistries.Keys.SOUND_EVENTS": "Registries.SOUND_EVENT",
    "ForgeRegistries.Keys.FLUIDS": "Registries.FLUID",
    "ForgeRegistries.Keys.FEATURES": "Registries.FEATURE",
}

FLAT_RENAMES = [
    ("AttributeModifier.Operation.ADDITION", "AttributeModifier.Operation.ADD_VALUE"),
    ("AttributeModifier.Operation.MULTIPLY_BASE", "AttributeModifier.Operation.ADD_MULTIPLIED_BASE"),
    ("AttributeModifier.Operation.MULTIPLY_TOTAL", "AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL"),
    ("MessageDirection.PLAY_SERVER_BOUND", "PacketFlow.SERVERBOUND"),
    ("MessageDirection.PLAY_CLIENT_BOUND", "PacketFlow.CLIENTBOUND"),
]

IDENT_RENAMES = [
    ("MinecraftForge", "NeoForge"),
    ("ForgeConfigSpec", "ModConfigSpec"),
    ("ForgeHooks", "CommonHooks"),
    ("ForgeEventFactory", "EventHooks"),
    ("ForgeCapabilities", "Capabilities"),
    ("ForgeMod", "NeoForgeMod"),
]

RECIPE_RESULT_KEYS = ("result", "results", "output")


def ensure_import(text: str, fqcn: str) -> str:
    line = "import %s;" % fqcn
    if line in text:
        return text
    m = re.search(r"^package\s+[\w.]+;\s*$", text, re.M)
    if not m:
        return text
    return text[:m.end()] + "\n\n" + line + text[m.end():]


def process(path: str, text: str) -> tuple[str, dict]:
    stats = {}

    # ---- 1. package renames (imports + fully qualified uses)
    for old, new in FORGED_IMPORTS:
        if old in text:
            stats["import:" + old] = text.count(old)
            text = text.replace(old, new)

    # ---- 2. vanilla registry constants (after the namespace pass so usages match)
    had_forge_registries = "ForgeRegistries." in text or "registries.ForgeRegistries" in text
    for old, new in REGISTRY_MAP.items():
        if old in text:
            stats["reg:" + old] = text.count(old)
            text = text.replace(old, new)
    if had_forge_registries:
        text = text.replace("import net.neoforged.neoforge.registries.ForgeRegistries;\n", "")
        text = re.sub(r"^import net\.minecraftforge\.registries\.ForgeRegistries;\n", "", text, flags=re.M)
    if "BuiltInRegistries." in text:
        text = ensure_import(text, "net.minecraft.core.registries.BuiltInRegistries")
    if "NeoForgeRegistries." in text:
        text = ensure_import(text, "net.neoforged.neoforge.registries.NeoForgeRegistries")
    if re.search(r"(?<![A-Za-z0-9_$])Registries\.", text):
        text = ensure_import(text, "net.minecraft.core.registries.Registries")

    # ---- 3. flat renames
    for old, new in FLAT_RENAMES:
        if old != new and old in text:
            stats["flat:" + old] = text.count(old)
            text = text.replace(old, new)

    # ---- 3b. identifier renames (usages without the import prefix)
    for old, new in IDENT_RENAMES:
        rx = re.compile(r"\b" + old + r"\b")
        n = len(rx.findall(text))
        if n:
            stats["ident:" + old] = n
            text = rx.sub(new, text)

    # ---- 4. RegistryObject<T> -> DeferredHolder<T, T>
    if "RegistryObject" in text:
        text, n = rewrite_generic_type(
            text, "RegistryObject", lambda inner: "DeferredHolder<%s, %s>" % (inner.strip(), inner.strip())
        )
        text = re.sub(r"\bRegistryObject\b(?!\s*<)", "DeferredHolder", text)
        stats["RegistryObject"] = n
        text = text.replace(
            "import net.neoforged.neoforge.registries.RegistryObject;",
            "import net.neoforged.neoforge.registries.DeferredHolder;",
        )

    # ---- 5. ResourceLocation constructors
    if "new ResourceLocation(" in text:
        def rl(args: str) -> str:
            parts = [p.strip() for p in split_args(args)]
            if len(parts) == 1:
                return "ResourceLocation.parse(%s)" % parts[0]
            if len(parts) == 2:
                return "ResourceLocation.fromNamespaceAndPath(%s, %s)" % (parts[0], parts[1])
            return None

        text, n = rewrite_ctor(text, "ResourceLocation", rl)
        stats["ResourceLocation ctor"] = n

    # ---- 6. ItemStack NBT -> DataComponents backed helper
    for method, tpl in (
        ("getOrCreateTag", "NbtHelper.getOrCreateTag(@R)"),
        ("setTag", "NbtHelper.setTag(@R, @A)"),
        ("getTag", "NbtHelper.getTag(@R)"),
        ("hasTag", "NbtHelper.hasTag(@R)"),
    ):
        text, n = rewrite_dot_calls(text, method, tpl, skip_prefix="NbtHelper")
        if n:
            stats[method] = n
    if "NbtHelper." in text:
        text = ensure_import(text, HELPER)

    # ---- 7. NeoForge moved EventBusSubscriber out of @Mod; Bus.FORGE -> Bus.GAME
    if "EventBusSubscriber" in text:
        text = text.replace(
            "import net.neoforged.fml.common.Mod.EventBusSubscriber.Bus;",
            "import net.neoforged.fml.common.EventBusSubscriber.Bus;",
        )
        text = text.replace(
            "import net.neoforged.fml.common.Mod.EventBusSubscriber;",
            "import net.neoforged.fml.common.EventBusSubscriber;",
        )
        text, n = re.subn(r"\bMod\.EventBusSubscriber\b", "EventBusSubscriber", text)
        if n:
            stats["Mod.EventBusSubscriber"] = n
        text = ensure_import(text, "net.neoforged.fml.common.EventBusSubscriber")
    if "Bus.FORGE" in text:
        stats["Bus.FORGE"] = text.count("Bus.FORGE")
        text = text.replace("Bus.FORGE", "Bus.GAME")

    # ---- 8. Framework 0.13 dropped PlayMessage: messages become plain classes
    if "PlayMessage" in text:
        text, n = re.subn(r"\s+extends\s+PlayMessage<[^<>]*(?:<[^<>]*>)?[^<>]*>", "", text)
        stats["PlayMessage extends"] = n
        text = text.replace("import com.mrcrayfish.framework.api.network.message.PlayMessage;\n", "")

    return text, stats


def main() -> None:
    targets = sys.argv[1:] or [SRC]
    total = {}
    nfiles = 0
    for target in targets:
        walk = [target] if os.path.isfile(target) else [
            os.path.join(d, f)
            for d, _, fs in os.walk(target)
            for f in fs
            if f.endswith(".java")
        ]
        for path in walk:
            text = open(path, encoding="utf-8", errors="replace").read()
            new, stats = process(path, text)
            if new != text:
                with open(path, "w", encoding="utf-8", newline="") as fh:
                    fh.write(new)
                nfiles += 1
                for k, v in stats.items():
                    total[k] = total.get(k, 0) + v
    print(f"rewrote {nfiles} files")
    for k, v in sorted(total.items(), key=lambda kv: -kv[1]):
        print(f"{v:6d}  {k}")


SELFTEST_CASES = [
    ("return stack.getOrCreateTag().getInt(\"AmmoCount\");", "getOrCreateTag", "stack"),
    ("      return NbtHelper.getOrCreateTag(stack);", None, None),
    ("CompoundTag tag = this.stack.getTag();", "getTag", "this.stack"),
    ("for (ItemStack stack : contents) {\n   x;\n}\n\nammoPouch.getOrCreateTag().put(\"Items\", listTag);",
     "getOrCreateTag", "ammoPouch"),
    ("ItemStack s = forStack.getGunStack().getTag();", "getTag", "forStack.getGunStack()"),
    ("if (foo(a).getTag() != null) {}", "getTag", "foo(a)"),
    ("bar(new ItemStack(Items.AIR)).getTag();", "getTag", "bar(new ItemStack(Items.AIR))"),
    ("stack.getItem() instanceof GunItem ? gunStack.getTag() : null;", "getTag", "gunStack"),
    ("(a ? b : c).getTag();", "getTag", "(a ? b : c)"),
    ("arr[i].getTag();", "getTag", "arr[i]"),
]


def selftest() -> None:
    for text, method, expected in SELFTEST_CASES:
        if method is None:
            continue
        dot = text.index("." + method)
        got = text[receiver_start(text, dot):dot].strip()
        status = "ok  " if got == expected else "FAIL"
        print(f"{status} {got!r} (expected {expected!r})")
        assert got == expected, text
    print("receiver_start self-test passed")


if __name__ == "__main__":
    if "--selftest" in sys.argv:
        selftest()
        sys.exit(0)
    main()
