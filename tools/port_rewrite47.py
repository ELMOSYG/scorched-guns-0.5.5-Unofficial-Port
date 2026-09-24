"""Forty-seventh-stage: drop bus registrations that NeoForge rejects outright.

Three ammo types (Beowulf +3, Gibbs round +4, turret Gibbs round +4) used to add a
flat looting bonus through Forge's `LootingLevelEvent`:

    @SubscribeEvent
    public static void onLootingLevel(LootingLevelEvent event) { ... }

NeoForge 21.1.249 has no `LootingLevelEvent`, and its `LivingDropsEvent` no longer
carries a modifiable looting level (the javadoc still mentions `#lootingLevel`, but
the field was removed), so the port dropped the handler and left the registration
behind. Registering a class with nothing to subscribe aborts mod loading with

    class X has no @SubscribeEvent methods, but register was called anyway.

Forge ignored that. The registration, its latch field, its helper and the now-unused
import are removed here; the lost bonus is recorded in HANDOFF.md as a known
deviation to restore with a global loot modifier.

Also removes `NeoForge.EVENT_BUS.register(TemporaryLightManager.class)`: that class
only has static helpers and never had a handler.

Idempotent; `--selftest` checks the shaped edits.

usage: python tools/port_rewrite47.py
"""
from __future__ import annotations

import os
import re
import sys

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")

TARGETS = [
    "top/ribs/scguns/entity/projectile/BeowulfProjectileEntity.java",
    "top/ribs/scguns/entity/projectile/GibbsRoundProjectileEntity.java",
    "top/ribs/scguns/entity/projectile/turret/TurretProjectileEntity.java",
]

NOTE = ("   // 0.5.5 registered this class on the game bus to add a flat looting bonus via\n"
        "   // Forge's LootingLevelEvent. NeoForge 21.1 has no such event and its\n"
        "   // LivingDropsEvent carries no modifiable looting level, so the handler could not\n"
        "   // be ported; registering a class with no @SubscribeEvent methods aborts mod\n"
        "   // loading (\"class ... has no @SubscribeEvent methods, but register was called\n"
        "   // anyway\"), so the registration is gone. See HANDOFF.md \"known deviations\".\n")


def strip_class(path: str) -> tuple[str, dict]:
    text = open(path, encoding="utf-8", errors="replace").read()
    stats: dict = {}
    cls = os.path.basename(path)[:-5]

    # decide on the import BEFORE the explanatory comment is inserted: the comment
    # mentions "@SubscribeEvent", which would otherwise mask the unused import
    imp = "import net.neoforged.bus.api.SubscribeEvent;\n"
    drop_import = imp in text and "@SubscribeEvent" not in text

    n = len(re.findall(r"^\s*registerLootingEventHandler\(\);\n", text, re.M))
    if n:
        text = re.sub(r"^\s*registerLootingEventHandler\(\);\n", "", text, flags=re.M)
        stats["removed call"] = n

    block = re.compile(
        r"   private static synchronized void registerLootingEventHandler\(\) \{\n"
        r"      if \(!eventRegistered\) \{\n"
        r"         NeoForge\.EVENT_BUS\.register\(" + re.escape(cls) + r"\.class\);\n"
        r"         eventRegistered = true;\n"
        r"      \}\n"
        r"   \}\n")
    if block.search(text):
        text = block.sub(NOTE, text)
        stats["removed helper"] = 1

    field = "   private static boolean eventRegistered = false;\n"
    if field in text:
        text = text.replace(field, "")
        stats["removed latch"] = 1

    if drop_import:
        text = text.replace(imp, "")
        stats["removed import"] = 1
    return text, stats


def main() -> None:
    total: dict = {}
    changed = 0
    for rel in TARGETS:
        path = os.path.join(SRC, rel.replace("/", os.sep))
        new, stats = strip_class(path)
        old = open(path, encoding="utf-8", errors="replace").read()
        if new != old:
            with open(path, "w", encoding="utf-8", newline="") as fh:
                fh.write(new)
            changed += 1
        for k, v in stats.items():
            total[k] = total.get(k, 0) + v

    # TemporaryLightManager: static helper class, nothing to subscribe
    if not os.environ.get("SKIP_LIGHT"):
        p = os.path.join(SRC, "top", "ribs", "scguns", "ScorchedGuns.java")
        text = open(p, encoding="utf-8", errors="replace").read()
        line = "            NeoForge.EVENT_BUS.register(TemporaryLightManager.class);\n"
        if line in text:
            text = text.replace(line, "")
            with open(p, "w", encoding="utf-8", newline="") as fh:
                fh.write(text)
            changed += 1
            total["TemporaryLightManager registration"] = 1

    print(f"rewrote {changed} files")
    for k, v in sorted(total.items(), key=lambda kv: -kv[1]):
        print(f"{v:6d}  {k}")


def selftest() -> None:
    import shutil
    import tempfile
    sample = ("   private static boolean eventRegistered = false;\n"
              "   public Foo() {\n      registerLootingEventHandler();\n   }\n"
              "   private static synchronized void registerLootingEventHandler() {\n"
              "      if (!eventRegistered) {\n"
              "         NeoForge.EVENT_BUS.register(Foo.class);\n"
              "         eventRegistered = true;\n"
              "      }\n   }\n"
              "import net.neoforged.bus.api.SubscribeEvent;\n")
    # the helper matches on the file's own class name, so the file must be Foo.java
    tmpdir = tempfile.mkdtemp()
    tmp = os.path.join(tmpdir, "Foo.java")
    with open(tmp, "w", encoding="utf-8") as fh:
        fh.write(sample)
    out, _ = strip_class(tmp)
    shutil.rmtree(tmpdir, ignore_errors=True)
    assert "registerLootingEventHandler" not in out, out
    assert "eventRegistered" not in out, out
    assert "import net.neoforged.bus.api.SubscribeEvent;" not in out, out
    assert out.count("0.5.5 registered this class") == 1, out
    print("ok   call, latch, helper and import removed; documented once")
    print("port_rewrite47 self-test passed")


if __name__ == "__main__":
    if "--selftest" in sys.argv:
        selftest()
    else:
        main()
