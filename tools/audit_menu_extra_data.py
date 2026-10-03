"""Every menu opened on the server must carry the extra data its factory needs.

NeoForge only attaches extra data when the opener's writer produces bytes. From the 1.21.1 merged jar,
ServerPlayer#openMenu(MenuProvider, Consumer):

    AbstractContainerMenu menu = provider.createMenu(id, inventory, this);
    if (menu == null) { ...; return OptionalInt.empty(); }
    byte[] customData = FriendlyByteBufUtil.writeCustomData(buf -> {
        provider.writeClientSideData(menu, buf);
        if (writer != null) writer.accept(buf);
    }, this.registryAccess());
    if (customData.length != 0) {
        this.connection.send(new AdvancedOpenScreenPayload(menu.containerId, ..., customData));
    } else {
        this.connection.send(new ClientboundOpenScreenPacket(menu.containerId, menu.getType(), ...));
    }

So a menu opened with the one-argument `openMenu(provider)` - a null writer - always takes the plain
packet, and on the client MenuType#create runs the registered IContainerFactory with a *null* buffer.
Any factory that reads it dies:

    java.lang.NullPointerException: Cannot invoke "RegistryFriendlyByteBuf.readBlockPos()" because
    "extraData" is null
        at top.ribs.scguns.client.screen.MaceratorMenu.<init>(MaceratorMenu.java:41)
        at net.neoforged.neoforge.network.IContainerFactory.create(IContainerFactory.java:36)
        at net.minecraft.world.inventory.MenuType.create(MenuType.java:54)
        at net.minecraft.client.gui.screens.MenuScreens$ScreenConstructor.fromPacket(MenuScreens.java:125)

and the client disconnects with "Network protocol error" - which is what the player reported after
interacting with a workstation in spectator mode.

The rule, then: no `openMenu(provider)` with a single argument. Write the position (or the hand, for
item menus) so the packet always carries data. A call may be exempted by marking it on the spot:

    player.openMenu(menuProvider);   // AUDIT-OK(<why a null buffer is harmless here>)

The marker has to come with a reason, and it has to be true - it says the provider's factory never
touches the buffer (it builds a vanilla menu, or the type was registered as a plain MenuSupplier rather
than an IContainerFactory). Three call sites carry it today; each was read before it was marked.

usage: python tools/audit_menu_extra_data.py
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

# openMenu(...) with exactly one argument - the call that always produces an empty payload.
ONE_ARG = re.compile(r"\.openMenu\(\s*(?!.*?,\s*)[^(),]*(?:\([^()]*\))?[^(),]*\)\s*;")

# An exemption is either the AUDIT-OK(...) marker next to the call, or an entry here when the file
# belongs to somebody else and is not ours to annotate: (relative path, argument text) -> reason.
EXEMPT = {
    ("src/main/java/top/ribs/scguns/entity/monster/SupplyScampEntity.java",
     "new SupplyScampMenuProvider(this)"):
        "the supply scamp's registered factory ignores the buffer and builds a vanilla ChestMenu",
}


def strip_comments(src: str) -> str:
    out, i, n = [], 0, len(src)
    while i < n:
        c = src[i]
        if c == '"':
            out.append(c)
            i += 1
            while i < n and src[i] != '"':
                if src[i] == "\\":
                    out.append(src[i])
                    i += 1
                if i < n:
                    out.append(src[i])
                    i += 1
            if i < n:
                out.append('"')
                i += 1
        elif src.startswith("//", i):
            while i < n and src[i] != "\n":
                i += 1
        elif src.startswith("/*", i):
            i += 2
            while i < n and not src.startswith("*/", i):
                i += 1
            i += 2
        else:
            out.append(c)
            i += 1
    return "".join(out)


def calls(text: str):
    """Yield (line number, argument count, argument text, whole line) for every openMenu call."""
    for match in re.finditer(r"\.openMenu\(", text):
        start = match.end()
        depth, args_end, commas = 1, None, 0
        i = start
        while i < len(text):
            ch = text[i]
            if ch in "([":
                depth += 1
            elif ch in ")]":
                depth -= 1
                if depth == 0:
                    args_end = i
                    break
            elif ch == "," and depth == 1:
                commas += 1
            i += 1
        if args_end is None:
            continue
        line_no = text.count("\n", 0, match.start()) + 1
        line_start = text.rfind("\n", 0, match.start()) + 1
        line_end = text.find("\n", match.end())
        if line_end == -1:
            line_end = len(text)
        body = text[start:args_end].strip()
        yield line_no, (commas + 1 if body else 0), body, text[line_start:line_end]


SOURCE_MARKER = "AUDIT-OK("


def exemption(rel: str, body: str, marked: str) -> str:
    """Return the reason this call may skip the extra data, or "" if it may not."""
    key = (rel.replace("\\", "/"), " ".join(body.split()))
    if key in EXEMPT:
        return EXEMPT[key]
    if SOURCE_MARKER in marked:
        reason = marked.split(SOURCE_MARKER, 1)[1]
        reason = reason.split(")", 1)[0] if ")" in reason else reason
        reason = " ".join(reason.replace("//", " ").split())
        return reason if len(reason) >= 10 else ""
    return ""


def main() -> int:
    problems = []
    checked = 0
    exempt = 0
    for root in SOURCES:
        for base, dirs, names in os.walk(root):
            dirs[:] = [d for d in dirs if d not in SKIP_DIRS]
            for name in names:
                if not name.endswith(".java"):
                    continue
                path = os.path.join(base, name)
                rel = os.path.relpath(path, ROOT)
                raw = open(path, encoding="utf-8", errors="replace").read()
                text = strip_comments(raw)
                for line, argc, body, _ in calls(text):
                    if body.startswith("this.") or body.startswith("super."):
                        continue
                    checked += 1
                    if argc != 1:
                        continue
                    # The marker may sit on the call itself or in the // comment block directly above it,
                    # so a long reason stays readable. The block has to be contiguous: one blank line
                    # and the exemption no longer belongs to this call.
                    lines = raw.splitlines()
                    same_line = lines[line - 1] if line - 1 < len(lines) else ""
                    block = [same_line]
                    i = line - 2
                    while i >= 0 and lines[i].strip().startswith("//"):
                        block.insert(0, lines[i])
                        i -= 1
                    marked = "\n".join(block)
                    if exemption(rel, body, marked):
                        exempt += 1
                        continue
                    problems.append("%s:%d opens a menu with no extra data (%s) - if that menu's factory "
                                    "reads the buffer the client dies with a NullPointerException and "
                                    "kicks the player. Pass a writer, or mark it AUDIT-OK(<reason>)"
                                    % (rel, line, body[:60]))

    for problem in problems:
        print("BROKEN %s" % problem)
    if problems:
        print("\n%d problem(s)" % len(problems))
        return 1
    print("0 problem(s): all %d menu opens pass extra data (%d marked AUDIT-OK with a reason)"
          % (checked, exempt))
    return 0


if __name__ == "__main__":
    sys.exit(main())
