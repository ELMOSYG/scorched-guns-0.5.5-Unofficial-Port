"""Fifth-stage porting rules: Forge capabilities -> NeoForge capabilities.

Forge exposed capabilities through `ICapabilityProvider` + `LazyOptional`; NeoForge
1.21 registers them on `RegisterCapabilitiesEvent` and hands out a plain nullable
value. Rather than rewriting every block entity by hand:

  * block entity `getCapability(Capability<T>, Direction)` becomes a plain
    `getCapability(...) -> T` (nullable), and ModCapabilities registers one
    lambda per capability type that simply delegates to it;
  * item `initCapabilities` anonymous providers are replaced by a factory method
    plus a registration in ModCapabilities;
  * call sites `x.getCapability(Capabilities.ENERGY).map(...).orElse(d)` collapse
    into Caps helpers.

usage: python tools/port_rewrite5.py
"""
from __future__ import annotations

import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from port_rewrite import ensure_import, match_forward, receiver_start, rewrite_dot_calls  # noqa: E402

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")

GET_CAP = re.compile(r"public\s+<T>\s+LazyOptional<T>\s+getCapability\(\s*@?\w*\s*Capability<T>\s+cap\s*,")
LAZY_FIELD = re.compile(r"LazyOptional<([^<>]+)>\s+(\w+)\s*=\s*LazyOptional\.of\(\s*\(\)\s*->\s*([^;]+)\);")
LAZY_EMPTY = re.compile(r"LazyOptional<([^<>]+)>\s+(\w+)\s*=\s*LazyOptional\.empty\(\);")
LAZY_ASSIGN = re.compile(r"(\w+)\s*=\s*LazyOptional\.of\(\s*\(\)\s*->\s*([^;]+)\);")
INVALIDATE = re.compile(r"\n\s*(?:@Override\s+)?public\s+void\s+invalidateCaps\(\)\s*\{[^{}]*\}", re.S)

ENERGY_CHAIN = re.compile(
    r"\.getCapability\(\s*Capabilities\.EnergyStorage\.ITEM\s*\)"
    r"\s*\.map\(IEnergyStorage::(getEnergyStored|getMaxEnergyStored)\)"
    r"\s*\.orElse\(([^()]*)\)"
)
ORELSE_NULL = re.compile(r"\.getCapability\((Capabilities\.[\w.]+)\)\.orElse\(null\)")
ORELSE_THROW = re.compile(r"\.getCapability\((Capabilities\.[\w.]+)\)\.orElseThrow\([^()]*(?:\(\))?\)")


def cap_names(text: str, is_be: bool) -> str:
    if is_be:
        text = text.replace("Capabilities.ENERGY", "Capabilities.EnergyStorage.BLOCK")
        text = text.replace("Capabilities.ITEM_HANDLER", "Capabilities.ItemHandler.BLOCK")
    else:
        text = text.replace("Capabilities.ENERGY", "Capabilities.EnergyStorage.ITEM")
        text = text.replace("Capabilities.ITEM_HANDLER", "Capabilities.ItemHandler.ITEM")
    return text


def convert_be_capability(text: str, stats: dict) -> str:
    """Nullable getCapability + LazyOptional field flattening."""
    # repair the first pass, which produced `this.(T) handler`
    text, n = re.subn(r"\bthis\.\(T\)\s*(\w+)", r"((T) this.\1)", text)
    if n:
        stats["repair:cast receiver"] = n
    text, n = re.subn(r"\bsuper\.getCapability\(cap,\s*side\)", "null", text)
    if n:
        stats["repair:super call"] = n
    if "LazyOptional" not in text and not GET_CAP.search(text):
        return text
    # fields holding a handler directly
    text, n = LAZY_FIELD.subn(lambda m: "%s %s = %s;" % (m.group(1), m.group(2), m.group(3).strip()), text)
    stats["lazy field"] = stats.get("lazy field", 0) + n
    text, n = LAZY_EMPTY.subn(lambda m: "%s %s = null;" % (m.group(1), m.group(2)), text)
    stats["lazy empty"] = stats.get("lazy empty", 0) + n
    text, n = LAZY_ASSIGN.subn(lambda m: "%s = %s;" % (m.group(1), m.group(2).strip()), text)
    stats["lazy assign"] = stats.get("lazy assign", 0) + n
    # method signature + body. The capability parameter becomes a plain Object so
    # the original `cap == Capabilities.X` dispatch keeps compiling; the registry
    # only ever passes the constants registered in ModCapabilities.
    if GET_CAP.search(text):
        text = GET_CAP.sub("public <T> T getCapability(Object cap,", text)
        text = re.sub(r"\b((?:this\.)?[\w.]+?)\.cast\(\)", r"((T) \1)", text)
        text = re.sub(r"\bsuper\.getCapability\(cap,\s*side\)", "null", text)
        stats["be getCapability"] = stats.get("be getCapability", 0) + 1
    if INVALIDATE.search(text):
        text = INVALIDATE.sub("", text)
        stats["invalidateCaps"] = stats.get("invalidateCaps", 0) + 1
    # stale imports
    for imp in (
        "import net.neoforged.neoforge.common.capabilities.Capability;\n",
        "import net.neoforged.neoforge.common.capabilities.ICapabilityProvider;\n",
        "import net.neoforged.neoforge.common.util.LazyOptional;\n",
        "import net.minecraftforge.common.capabilities.Capability;\n",
        "import net.minecraftforge.common.util.LazyOptional;\n",
    ):
        text = text.replace(imp, "")
    return text


def convert_item_capability(path: str, text: str, stats: dict) -> str:
    """initCapabilities(...) -> createXxxStorage(ItemStack) factory."""
    marker = re.search(r"public\s+ICapabilityProvider\s+initCapabilities\(", text)
    if not marker:
        return text
    open_paren = text.index("(", marker.start())
    close = match_forward(text, open_paren)
    body_open = text.index("{", close)
    body_close = match_forward(text, body_open)
    body = text[body_open + 1:body_close]
    # the storage expression inside LazyOptional.of(() -> ...)
    expr = re.search(r"LazyOptional\.of\(\s*\(\)\s*->\s*(.+?)\);", body, re.S)
    if not expr:
        return text
    storage = expr.group(1).strip()
    storage = storage.replace("EnergyGunItem.this.", "this.").replace("ExoSuitCoreItem.this.", "this.")
    storage = re.sub(r"(\w+)\.this\.", "this.", storage)
    factory = (
        "\n   /** Capability factory; registered in ModCapabilities. */\n"
        "   public IEnergyStorage createEnergyStorage(ItemStack stack) {\n"
        "      return %s;\n"
        "   }\n" % storage
    )
    text = text[:marker.start()] + factory + text[body_close + 1:]
    stats["item initCapabilities"] = stats.get("item initCapabilities", 0) + 1
    for imp in (
        "import net.neoforged.neoforge.common.capabilities.ICapabilityProvider;\n",
        "import net.neoforged.neoforge.common.util.LazyOptional;\n",
        "import net.neoforged.neoforge.common.capabilities.Capability;\n",
    ):
        text = text.replace(imp, "")
    if "IEnergyStorage" in text:
        text = ensure_import(text, "net.neoforged.neoforge.energy.IEnergyStorage")
    return text


def convert_call_sites(text: str, stats: dict) -> str:
    out = text
    # inline side-dependent providers: LazyOptional.of(() -> new X(...)).cast()
    out, n = re.subn(r"LazyOptional\.of\(\s*\(\)\s*->\s*(.*?)\)\.cast\(\)", r"((T) \1)", out, flags=re.S)
    if n:
        stats["inline lazy cast"] = n
    out, n = re.subn(r"LazyOptional\.empty\(\)", "null", out)
    if n:
        stats["LazyOptional.empty"] = n
    # an item stack inside a block entity file still uses the ITEM capability
    out, n = re.subn(r"(\b\w*[Ss]tack\w*\.getCapability\()Capabilities\.EnergyStorage\.BLOCK\)", r"\1Capabilities.EnergyStorage.ITEM)", out)
    if n:
        stats["item cap in be"] = n
    pos = 0
    while True:
        m = ENERGY_CHAIN.search(out, pos)
        if not m:
            break
        start = receiver_start(out, m.start())
        while start < m.start() and out[start].isspace():
            start += 1
        recv = out[start:m.start()].strip()
        helper = "energyStored" if m.group(1) == "getEnergyStored" else "maxEnergyStored"
        new = "Caps.%s(%s, %s)" % (helper, recv, m.group(2).strip())
        out = out[:start] + new + out[m.end():]
        pos = start + len(new)
        stats["energy chain"] = stats.get("energy chain", 0) + 1
    out, n = ORELSE_NULL.subn(r".getCapability(\1)", out)
    if n:
        stats["orElse(null)"] = n
    out, n = ORELSE_THROW.subn(
        lambda m: ".getCapability(%s)" % m.group(1), out)
    if n:
        stats["orElseThrow"] = n
    if "Caps." in out:
        out = ensure_import(out, "top.ribs.scguns.util.Caps")
    return out


def process(path: str, text: str) -> tuple[str, dict]:
    stats: dict = {}
    rel = os.path.relpath(path, SRC).replace("\\", "/")
    is_be = "/blockentity/" in "/" + rel
    text = cap_names(text, is_be)
    # drop the Forge capability-provider interface entirely
    if "ICapabilityProvider" in text:
        text, n = re.subn(r",\s*ICapabilityProvider\b", "", text)
        if n or "implements ICapabilityProvider" in text:
            text = re.sub(r"implements\s+ICapabilityProvider\b", "", text)
            stats["drop ICapabilityProvider"] = 1
        text = text.replace("import net.neoforged.neoforge.capabilities.ICapabilityProvider;\n", "")
    if is_be:
        text = convert_be_capability(text, stats)
    if "/item/" in "/" + rel:
        text = convert_item_capability(path, text, stats)
    text = convert_call_sites(text, stats)
    return text, stats


def main() -> None:
    authored = {
        "util/NbtHelper.java", "util/Caps.java", "util/DistHelper.java", "util/MobType.java",
        "network/FrameworkMessageBridge.java", "network/PacketHandler.java", "init/ModCapabilities.java",
    }
    files = [os.path.join(d, f) for d, _, fs in os.walk(SRC) for f in fs if f.endswith(".java")]
    total: dict = {}
    changed = 0
    for path in files:
        if os.path.relpath(path, SRC).replace("\\", "/") in authored:
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
