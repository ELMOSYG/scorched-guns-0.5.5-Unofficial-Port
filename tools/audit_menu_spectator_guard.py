"""A menu that needs extra data must refuse to open for a spectator.

The player was disconnected with "Network protocol error" after right-clicking a machine in spectator
mode. The disconnect report and a probe on ServerPlayer#openMenu gave the exact call chain:

    at net.minecraft.server.level.ServerPlayer.openMenu(ServerPlayer.java:1116)      <- the one-arg overload
    at net.minecraft.server.level.ServerPlayerGameMode.useItemOn(ServerPlayerGameMode.java:348)
    at net.minecraft.server.network.ServerGamePacketListenerImpl.handleUseItemOn(...)

Vanilla opens a container block for a spectator itself, in ServerPlayerGameMode#useItemOn:

    MenuProvider provider = blockState.getMenuProvider(level, pos);
    if (provider != null) { player.openMenu(provider); return InteractionResult.SUCCESS; }

and that is the *one-argument* openMenu - no writer, so no extra data at all. The contract vanilla
expects is that the provider refuses, because ServerPlayer#openMenu handles a null menu by printing
"container.spectatorCantOpen" and sending nothing:

    AbstractContainerMenu menu = provider.createMenu(id, inventory, this);
    if (menu == null) { if (isSpectator()) displayClientMessage("container.spectatorCantOpen"); return empty; }

This mod's machine menus are registered through IMenuTypeExtension.create, so their constructors read the
buffer (MaceratorMenu.java:41, PoweredMechanicalPressMenu.java:38, ...). With a null one the client throws
a NullPointerException inside packet handling and the connection dies. Note that the block's own
useWithoutItem never runs in this case - vanilla's spectator branch replaces it - so guarding the block
does nothing; the guard belongs in the block entity's createMenu.

The rule: a createMenu that returns a menu class having an (int, Inventory, RegistryFriendlyByteBuf)
constructor must return null when the player is a spectator.

usage: python tools/audit_menu_spectator_guard.py
"""
from __future__ import annotations

import os
import re
import sys

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SOURCES = [
    os.path.join(ROOT, "src", "main", "java"),
    os.path.join(ROOT, "maid-compat", "src", "main", "java"),
]
SKIP_DIRS = {"build", ".git", "run", ".gradle", ".refs", "build-logs"}


def java_files():
    for root in SOURCES:
        for base, dirs, names in os.walk(root):
            dirs[:] = [d for d in dirs if d not in SKIP_DIRS]
            for name in names:
                if name.endswith(".java"):
                    yield os.path.join(base, name)


def menu_classes_needing_data() -> set:
    """Menu classes with a constructor that takes the extra-data buffer."""
    found = set()
    for path in java_files():
        text = open(path, encoding="utf-8", errors="replace").read()
        cls = re.search(r"public\s+(?:abstract\s+)?class\s+(\w+)", text)
        if not cls:
            continue
        if re.search(r"\b%s\s*\(\s*int\s+\w+\s*,\s*Inventory\s+\w+\s*,\s*RegistryFriendlyByteBuf\s+\w+\s*\)"
                     % cls.group(1), text):
            found.add(cls.group(1))
    return found


def main() -> int:
    menus = menu_classes_needing_data()
    problems = []
    checked = 0

    for path in java_files():
        rel = os.path.relpath(path, ROOT)
        text = open(path, encoding="utf-8", errors="replace").read()
        match = re.search(r"public\s+AbstractContainerMenu\s+createMenu\s*\(([^)]*)\)\s*\{", text)
        if not match:
            continue
        body_start = match.end()
        # the method body: up to the matching brace, roughly - the returned menu is named in the first
        # 2000 characters, which is far more than any of them take.
        body = text[body_start:body_start + 2000]
        returned = re.search(r"return\s+new\s+(\w+)\s*\(", body)
        if not returned or returned.group(1) not in menus:
            continue
        checked += 1
        if "isSpectator" not in body:
            problems.append("%s returns %s, which reads the extra-data buffer, but its createMenu does "
                            "not return null for a spectator - vanilla opens container blocks for "
                            "spectators through the one-argument openMenu, and the client then dies with "
                            "a NullPointerException and a network protocol error"
                            % (rel, returned.group(1)))

    if not checked:
        problems.append("no createMenu returning a buffer-reading menu was found, so this audit is not "
                        "measuring anything")

    for problem in problems:
        print("BROKEN %s" % problem)
    if problems:
        print("\n%d problem(s)" % len(problems))
        return 1
    print("0 problem(s): all %d menu providers that need extra data refuse spectators" % checked)
    return 0


if __name__ == "__main__":
    sys.exit(main())
