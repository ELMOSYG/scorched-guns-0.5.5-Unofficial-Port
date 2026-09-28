"""The GUI item path is the per-frame path: a gun drawn in a JEI/EMI/inventory slot must cost almost nothing
(HANDOFF 82.24).

The inventory, the creative tabs, JEI and EMI draw every visible gun again on every frame - a search page can
hold several dozen. So whatever `AnimatedGunRenderer.renderByItem` does per call is multiplied by the number of
visible guns, every frame, and anything a GUI slot cannot use is a pure frame-time tax.

`renderByItem` used to resolve the item's baked model (including its item-overrides resolve), read the world
light level at the player's eye, parse the gun's tag and require a live `client.player`, and then the GUI branch
threw all of it away: a GUI slot is drawn with a fixed 12/12 light and no hand transforms, no muzzle flash and
no held-item animation. The method now returns through `renderGuiItem` before any of that work happens.

Rules:

  1. `renderByItem` must leave through the GUI branch before it touches the item model, the world light, the
     gun definition or the stack tag.
  2. That branch must still draw the gun, with the GUI's fixed light - it is a fast path, not a skip.
  3. The GeckoLib render hooks must carry `@Override`. `java.lang.Override` is SOURCE-retention, so javac
     proves an override only when it is written: 0.5.5's `renderRecursively` silently stopped overriding when
     GeckoLib changed `(float,float,float,float)` to `(int)` (HANDOFF 10.9.2) and the whole body became dead
     code with no warning. Mixins are exempt - they inject into a target and never annotate.

usage: python tools/audit_gui_render_cost.py
"""
from __future__ import annotations

import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "src", "main", "java", "top", "ribs", "scguns")
RENDERER = os.path.join(SRC, "client", "render", "gun", "animated", "AnimatedGunRenderer.java")

# Work that a GUI slot cannot use. Each is only worth doing in a hand/entity context.
GUI_FORBIDDEN = (
    "getModel(",
    "calculateBlockLight(",
    "getModifiedGun(",
    "NbtHelper.getTag(",
    "Objects.requireNonNull(",
)

# GeckoLib hooks whose signature drifted once already.
GECKO_HOOKS = ("renderByItem", "renderRecursively", "actuallyRender", "defaultRender",
               "preRender", "postRender", "addRenderData")


def strip_comments(text: str) -> str:
    """Blank out comments, aware of string literals.

    A `/*` inside a string is not a comment: this codebase has config comments naming file globs such as
    `data/scguns/entity/equipment/*.json`, and a stripper that ignores string literals treats that `/*` as a
    block comment start and deletes the code after it. That reads as "the thing does not exist" - a false
    negative, which is the one failure an audit must not have.
    """
    out = []
    i = 0
    n = len(text)
    while i < n:
        c = text[i]
        if c == '"':
            out.append(c)
            i += 1
            while i < n:
                if text[i] == "\\":
                    out.append(text[i:i + 2])
                    i += 2
                    continue
                out.append(text[i])
                if text[i] == '"':
                    i += 1
                    break
                i += 1
        elif c == "/" and i + 1 < n and text[i + 1] == "/":
            while i < n and text[i] != "\n":
                i += 1
        elif c == "/" and i + 1 < n and text[i + 1] == "*":
            i += 2
            while i + 1 < n and not (text[i] == "*" and text[i + 1] == "/"):
                if text[i] == "\n":
                    out.append("\n")
                i += 1
            i += 2
        else:
            out.append(c)
            i += 1
    return "".join(out)

def method_body(text: str, signature: str) -> tuple[str, int]:
    """(braces-balanced body of the first method whose declaration contains `signature`, its offset)."""
    at = text.find(signature)
    if at < 0:
        return "", -1
    start = text.find("{", at)
    if start < 0:
        return "", -1
    depth = 0
    for i in range(start, len(text)):
        if text[i] == "{":
            depth += 1
        elif text[i] == "}":
            depth -= 1
            if depth == 0:
                return text[start:i + 1], start
    return "", -1


def java_files() -> list[str]:
    out = []
    for dirpath, _, names in os.walk(SRC):
        for name in names:
            if name.endswith(".java"):
                out.append(os.path.join(dirpath, name))
    return sorted(out)


def main() -> int:
    problems: list[str] = []

    # --- 1 + 2: the GUI fast path -----------------------------------------
    text = strip_comments(open(RENDERER, encoding="utf-8", errors="replace").read())
    body, _ = method_body(text, "public void renderByItem(")
    if not body:
        problems.append("AnimatedGunRenderer has no renderByItem")
    else:
        guard = body.find("ItemDisplayContext.GUI")
        if guard < 0:
            problems.append("renderByItem no longer branches on the GUI context")
        else:
            for needle in GUI_FORBIDDEN:
                at = body.find(needle)
                if 0 <= at < guard:
                    problems.append("renderByItem does %s before its GUI branch, so every gun in every "
                                    "inventory/JEI/EMI slot pays for work the GUI throws away" % needle)
            branch = body[guard:body.find("return;", guard) if body.find("return;", guard) > 0 else len(body)]
            if "renderGuiItem(" not in branch:
                problems.append("the GUI branch does not call renderGuiItem, so a GUI slot would draw nothing")

    gui_helper, _ = method_body(text, "private void renderGuiItem(")
    if not gui_helper:
        problems.append("renderGuiItem is gone: the GUI path is no longer a named, single-purpose fast path")
    else:
        if "super.renderByItem(" not in gui_helper:
            problems.append("renderGuiItem does not reach GeckoLib's renderer, so the gun would not be drawn")
        if "LightTexture.pack(12, 12)" not in gui_helper:
            problems.append("renderGuiItem no longer uses the GUI's fixed light")

    # --- 3: the hooks must carry @Override ---------------------------------
    for path in java_files():
        source = open(path, encoding="utf-8", errors="replace").read()
        if "@Mixin" in source or os.sep + "mixin" + os.sep in path:
            continue
        code = strip_comments(source)
        lines = code.splitlines()
        for index, line in enumerate(lines):
            match = re.search(r"\b(?:public|protected)\s+[\w<>\[\],.?\s]*\b(%s)\s*\(" % "|".join(GECKO_HOOKS), line)
            if not match:
                continue
            # walk back over annotations and blank lines
            annotation = None
            j = index - 1
            while j >= 0:
                stripped = lines[j].strip()
                if not stripped:
                    j -= 1
                    continue
                if stripped.startswith("@"):
                    if stripped.startswith("@Override"):
                        annotation = "@Override"
                    j -= 1
                    continue
                break
            if annotation is None:
                rel = os.path.relpath(path, ROOT).replace("\\", "/")
                problems.append("%s:%d declares %s without @Override: if GeckoLib changes that hook again it "
                                "stops overriding silently and the body becomes dead code"
                                % (rel, index + 1, match.group(1)))

    print("=== GUI item path ===")
    if body:
        guard = body.find("ItemDisplayContext.GUI")
        first = min([body.find(n) for n in GUI_FORBIDDEN if body.find(n) >= 0] or [len(body)])
        print("  GUI branch at %-6s first unusable work at %-6s %s"
              % (guard if guard >= 0 else "-", first if first < len(body) else "none",
                 "ok" if 0 <= guard <= first else "WRONG ORDER"))
    print("  GeckoLib hooks checked              %s" % ", ".join(GECKO_HOOKS))
    print("")
    if problems:
        for problem in problems:
            print("BROKEN %s" % problem)
        print("\n%d problem(s)" % len(problems))
        return 1
    print("0 problem(s): a GUI slot pays for the GUI draw and nothing else; every hook is an annotated override")
    return 0


if __name__ == "__main__":
    sys.exit(main())
